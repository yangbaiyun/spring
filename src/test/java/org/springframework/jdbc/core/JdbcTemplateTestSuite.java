package org.springframework.jdbc.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.easymock.MockControl;

import org.springframework.dao.CleanupFailureDataAccessException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.JdbcTestCase;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.datasource.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

/** 
 * Mock object based tests for JdbcTemplate.
 * @author Rod Johnson
 * @version $Id: JdbcTemplateTestSuite.java,v 1.10 2003/12/18 18:51:48 jhoeller Exp $
 */
public class JdbcTemplateTestSuite extends JdbcTestCase {

	public JdbcTemplateTestSuite(String name) {
		super(name);
	}

	public void testBeanProperties() throws Exception {
		replay();

		JdbcTemplate t = new JdbcTemplate(mockDataSource);
		assertTrue("datasource ok", t.getDataSource() == mockDataSource);
		assertTrue("ignores warnings by default", t.getIgnoreWarnings());
		t.setIgnoreWarnings(false);
		assertTrue("can set NOT to ignore warnings", !t.getIgnoreWarnings());
	}

	public void testCannotRunStaticSqlWithBindParameters() throws Exception {
		final String sql = "UPDATE FOO SET NAME='tony' WHERE ID > ?";

		replay();

		JdbcTemplate t = new JdbcTemplate(mockDataSource);
		try {
			t.query(sql, new RowCountCallbackHandler());
			fail("Should have objected to bind variables");
		} catch (InvalidDataAccessApiUsageException ex) {
			// Ok 
		}
	}

	public void testUpdateCount() throws Exception {
		final String sql =
			"UPDATE INVOICE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		int idParam = 11111;

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.setInt(1, idParam);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(1);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		Dispatcher d = new Dispatcher(idParam, sql);
		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		int rowsAffected = template.update(d);
		assertTrue("1 update affected 1 row", rowsAffected == 1);

		/*
		d = new Dispatcher(idParam);
		rowsAffected = template.update(d);
		assertTrue("bogus update affected 0 rows", rowsAffected == 0);
		*/

		ctrlPreparedStatement.verify();
	}

	public void testBogusUpdate() throws Exception {
		final String sql =
			"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int idParam = 6666;

		// It's because Integers aren't canonical
		SQLException sex = new SQLException("bad update");

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.setInt(1, idParam);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setThrowable(sex);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		Dispatcher d = new Dispatcher(idParam, sql);
		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		try {
			int rowsAffected = template.update(d);
			fail("Bogus update should throw exception");
		} catch (UncategorizedDataAccessException ex) {
			// pass
			assertTrue(
				"Correct exception",
				ex instanceof UncategorizedSQLException);
			assertTrue("Root cause is correct", ex.getRootCause() == sex);
			//assertTrue("no update occurred", !je.getDataWasUpdated());
		}

		ctrlPreparedStatement.verify();
	}

	public void testStrings() throws Exception {
		String sql = "SELECT FORENAME FROM CUSTMR";
		String[] results = { "rod", "gary", " portia" };

		class StringHandler implements RowCallbackHandler {
			private List l = new LinkedList();

			public void processRow(ResultSet rs) throws SQLException {
				l.add(rs.getString(1));
			}

			public String[] getStrings() {
				return (String[]) l.toArray(new String[l.size()]);
			}
		}
		StringHandler sh = new StringHandler();

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(results[0]);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(results[1]);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(results[2]);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement =
			(PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.getWarnings();
		ctrlStatement.setReturnValue(null);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		template.query(sql, sh);

		// Match
		String[] forenames = sh.getStrings();
		assertTrue("same length", forenames.length == results.length);
		for (int i = 0; i < forenames.length; i++) {
			assertTrue(
				"Row " + i + " matches",
				forenames[i].equals(results[i]));
		}

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	/*	
		// REFACTOR!?
		public void testStringsWithPreparedStatement() throws Exception {
			class StringHandler implements RowCallbackHandler {
				private List l = new LinkedList();
	
				public void processRow(ResultSet rs) throws SQLException {
					l.add(rs.getString(1));
				}
	
				public String[] getStrings() {
					return (String[]) l.toArray(new String[l.size()]);
				}
			}
			StringHandler sh = new StringHandler();
		
			final String sql  = "SELECT FORENAME FROM CUSTMR WHERE ID>?";
			String[][] results = {
				{ "rod" },
				{ "gary" },
				{" portia" }
			};
		
			final MockConnection con = MockConnectionFactory.preparedStatement(sql, new Integer[] { new Integer(1) }, results, true);
			con.setExpectedCloseCalls(2);
		
			MockControl dsControl = MockControl.createControl(DataSource.class);
			DataSource ds = (DataSource) dsControl.getMock();
			ds.getConnection();
			dsControl.setReturnValue(con, MockControl.ONE_OR_MORE);
			dsControl.replay();
		
			JdbcTemplate template = new JdbcTemplate(ds);
		
			PreparedStatementCreator psc = new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
					assertTrue("Conn is correct", conn == con);
					PreparedStatement ps = conn.prepareStatement(sql);
				
					// FIX!>
					ps.setInt(1, 1);
					return ps;
				}
			};
		
			template.query(psc, sh);
		
			// Match
			String[] forenames = sh.getStrings();
			assertTrue("same length", forenames.length == results.length);
			for (int i = 0; i < forenames.length; i++) {
				assertTrue("Row " + i + " matches", forenames[i].equals(results[i][0]));
			}
		
			dsControl.verify();
		}
	*/

	public void testLeaveConnOpenOnRequest() throws Exception {
		String sql = "SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3";

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		ctrlResultSet = MockControl.createControl(ResultSet.class);
		mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement =
			(PreparedStatement) ctrlStatement.getMock();
		ctrlStatement = MockControl.createControl(PreparedStatement.class);
		mockStatement = (PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.getWarnings();
		ctrlStatement.setReturnValue(null);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.isClosed();
		ctrlConnection.setReturnValue(false, 2);
		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);
		// if close is called entire test will fail
		mockConnection.close();
		ctrlConnection.setDefaultThrowable(new RuntimeException());

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		SingleConnectionDataSource scf =
			new SingleConnectionDataSource(
				mockDataSource.getConnection(),
				false);
		JdbcTemplate template2 = new JdbcTemplate(scf);
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		template2.query(sql, rcch);

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	public void testCloseConnOnRequest() throws Exception {
		String sql = "SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3";

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement =
			(PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.getWarnings();
		ctrlStatement.setReturnValue(null);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		JdbcTemplate template2 = new JdbcTemplate(mockDataSource);
		RowCountCallbackHandler rcch = new RowCountCallbackHandler();
		template2.query(sql, rcch);

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	//	This really tests the translator: shouldn't the SQL translator have its own tests?
	//	We just need to check that the translator is invoked and that it's exception is correctly used
	/*
		 public void testSQLExceptionIsTranslated() throws Exception {
			
			 class TestSqlExceptionTranslator implements SQLExceptionTranslator {
				 private int invoked;
				 public DataAccessException translate(String task, String sql, SQLException sqlex) {
					 // Any subclass will do: can't really check all, can we?
					 // YES, we can: loop throuigh with exception as parameter
					 System.out.println("Our translator");
					 ++invoked;
					 return new BadSqlGrammarException("Exception doing " + task,sql, sqlex);
				 }
			 }
			
			 TestSqlExceptionTranslator trans = new TestSqlExceptionTranslator();
			
			 String sql = "SELECT"; // doesn't really matter what this is
			
			 SQLException sex = new SQLException("Any type of SQLException");
			 //MockConnection con = MockConnectionFactory.preparedStatement(sql, null, new Object[0][0], false, sex, null);
			
			 MockControl conControl = MockControl.createControl(Connection.class);
			 Connection con = (Connection) conControl.getMock();
			 con.prepareStatement(sql);
			
			 MockControl psControl = MockControl.createControl(PreparedStatement.class);
			 PreparedStatement ps = (PreparedStatement) psControl.getMock();
			 ps.executeQuery();
			 MockSingleRowResultSet rs = new MockSingleRowResultSet();
			 rs.setExpectedCloseCalls(2);
			 //rs.setupMetaData()
			
			 psControl.setReturnValue(rs);
			 psControl.replay();
			 conControl.setReturnValue(ps);
			 con.close();
			 conControl.replay();
	
			 MockControl dsControl = MockControl.createControl(DataSource.class);
			 DataSource ds = (DataSource) dsControl.getMock();
			 ds.getConnection();
			 dsControl.setReturnValue(con);
			 dsControl.replay();
	
			 JdbcTemplate template = new JdbcTemplate(ds);
			 template.setExceptionTranslator(trans);
			 RowCountCallbackHandler rcch = new RowCountCallbackHandler();
			 try {
				 template.query(PreparedStatementCreatorFactory.newPreparedStatementCreator(sql), rcch);
				 fail("Exceptioo should be translated");
			 }
			 catch (DataAccessException ex) {
				 SQLException se2 = (SQLException) ex.getRootCause();
				 assertTrue("Found SQL exception", se2 == sex);
				 //System.out.println("VENDOR CODE IS " + sex.getErrorCode());
				 //System.out.println("SQLSTATE IS " + sex.getSQLState());
			 }
			 dsControl.verify();
			 conControl.verify();
		 }
		 */

	/**
	 * Test that we see a runtime exception back
	 */
	public void testExceptionComesBack() throws Exception {
		final String sql = "SELECT ID FROM CUSTMR";
		final RuntimeException rex = new RuntimeException("What I want to see");
		Object[][] results = new Object[][] { { new Integer(1)}, {
				new Integer(2)
				}
		};

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement =
			(PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.getWarnings();
		ctrlStatement.setReturnValue(null);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		try {
			template.query(sql, new RowCallbackHandler() {
				public void processRow(ResultSet rs)
					throws SQLException {
					throw rex;
				}
			});
			fail("Should have thrown exception");
		} catch (RuntimeException ex) {
			assertTrue("Wanted same exception back, not " + ex, ex == rex);
		}
	}

	/**
	 * Test update with static SQL
	 */
	public void testSqlUpdateEncountersSqlException() throws Exception {
		SQLException sex = new SQLException("bad update");
		final String sql =
			"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setThrowable(sex);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		try {
			template.update(sql);
		} catch (DataAccessException ex) {
			assertTrue("root cause is correct", ex.getRootCause() == sex);
		}

		ctrlPreparedStatement.verify();
	}

	public void testSqlUpdate() throws Exception {
		final String sql =
			"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";
		int rowsAffected = 33;

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(rowsAffected);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		int actualRowsAffected = template.update(sql);
		assertTrue(
			"Actual rows affected is correct",
			actualRowsAffected == rowsAffected);

		ctrlPreparedStatement.verify();
	}

	public void testSqlUpdateWithThreadConnection() throws Exception {
		final String sql =
			"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = 4";
		int rowsAffected = 33;

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(rowsAffected);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		int actualRowsAffected = template.update(sql);
		assertTrue(
			"Actual rows affected is correct",
			actualRowsAffected == rowsAffected);

		ctrlPreparedStatement.verify();
	}

	/**
	 * Check that a successful bulk update works
	 * @throws Exception
	 */
	public void testBatchUpdate() throws Exception {
		final String sql =
			"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.setInt(1, ids[0]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setInt(1, ids[1]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeBatch();
		ctrlPreparedStatement.setReturnValue(ids);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		BatchPreparedStatementSetter setter =
			new BatchPreparedStatementSetter() {
			public void setValues(PreparedStatement ps, int i)
				throws SQLException {
				ps.setInt(1, ids[i]);
			}
			public int getBatchSize() {
				return ids.length;
			}
		};

		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		int[] actualRowsAffected = template.batchUpdate(sql, setter);
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);

		ctrlPreparedStatement.verify();
	}

	/**
	 * Test case where a batch update fails
	 * @throws Exception
	 */
	public void testBatchUpdateFails() throws Exception {
		final String sql =
			"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		SQLException sex = new SQLException();

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.setInt(1, ids[0]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setInt(1, ids[1]);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeBatch();
		ctrlPreparedStatement.setThrowable(sex);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		BatchPreparedStatementSetter setter =
			new BatchPreparedStatementSetter() {
			public void setValues(PreparedStatement ps, int i)
				throws SQLException {
				ps.setInt(1, ids[i]);
			}
			public int getBatchSize() {
				return ids.length;
			}
		};

		try {
			JdbcTemplate template = new JdbcTemplate(mockDataSource);
			template.batchUpdate(sql, setter);
			fail("Should have failed because of SQLException in bulk update");
		} catch (DataAccessException ex) {
			assertTrue("Root cause is SQLException", ex.getRootCause() == sex);
		}

		ctrlPreparedStatement.verify();
	}

	public void testCouldntGetConnectionInOperationOrLazilyInstantiatedExceptionTranslator() throws SQLException {
		SQLException sex = new SQLException("foo", "07xxx");

		// Change behaviour in setUp() because we only expect one call to getConnection():
		// none is necessary to get metadata for exception translator
		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		// Expect two calls: make get Metadata fail also
		ctrlDataSource.setThrowable(sex, 2);
		ctrlDataSource.replay();

		try {
			JdbcTemplate template2 = new JdbcTemplate(mockDataSource);
			RowCountCallbackHandler rcch = new RowCountCallbackHandler();
			template2.query(
				"SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3",
				rcch);
			fail("Shouldn't have executed query without a connection");
		} 
		catch (CannotGetJdbcConnectionException ex) {
			// pass
			assertTrue("Check root cause", ex.getRootCause() == sex);
		}
		
		ctrlDataSource.verify();
	}
	
	
	/**
	 * Verify that afterPropertiesSet invokes exceception translator
	 * @throws SQLException
	 */
	public void testCouldntGetConnectionInOperationWithExceptionTranslatorInitialized() throws SQLException {
		SQLException sex = new SQLException("foo", "07xxx");

		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(null, 1);
		mockConnection.close();
		ctrlConnection.setVoidCallable(1);
		ctrlConnection.replay();
		
		// Change behaviour in setUp() because we only expect one call to getConnection():
		// none is necessary to get metadata for exception translator
		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		// Upfront call for metadata
		ctrlDataSource.setReturnValue(mockConnection, 1);
		// One call for operation
		mockDataSource.getConnection();
		ctrlDataSource.setThrowable(sex, 1);
		ctrlDataSource.replay();

		try {
			JdbcTemplate template2 = new JdbcTemplate(mockDataSource);
			template2.afterPropertiesSet();
			RowCountCallbackHandler rcch = new RowCountCallbackHandler();
			template2.query(
				"SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3",
				rcch);
			fail("Shouldn't have executed query without a connection");
		} 
		catch (CannotGetJdbcConnectionException ex) {
			// pass
			assertTrue("Check root cause", ex.getRootCause() == sex);
		}
	
		ctrlDataSource.verify();
		ctrlConnection.verify();
	}
	
	public void testCouldntGetConnectionInOperationWithExceptionTranslatorInitializedViaBeanProperty() throws Exception {
		testCouldntGetConnectionInOperationWithExceptionTranslatorInitialized(true);
	}
	
	public void testCouldntGetConnectionInOperationWithExceptionTranslatorInitializedInAfterPropertiesSet() throws Exception {
		testCouldntGetConnectionInOperationWithExceptionTranslatorInitialized(false);
	}
	
	/**
	 * If beanProperty is true, initialize via exception translator bean property
	 * if false, use afterPropertiesSet()
	 * @param beanProperty
	 * @throws SQLException
	 */
	private void testCouldntGetConnectionInOperationWithExceptionTranslatorInitialized(boolean beanProperty) throws SQLException {
		SQLException sex = new SQLException("foo", "07xxx");

		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(null, 1);
		mockConnection.close();
		ctrlConnection.setVoidCallable(1);
		ctrlConnection.replay();
	
		// Change behaviour in setUp() because we only expect one call to getConnection():
		// none is necessary to get metadata for exception translator
		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		// Upfront call for metadata
		ctrlDataSource.setReturnValue(mockConnection, 1);
		// One call for operation
		mockDataSource.getConnection();
		ctrlDataSource.setThrowable(sex, 1);
		ctrlDataSource.replay();

		try {
			JdbcTemplate template2 = new JdbcTemplate();
			template2.setDataSource(mockDataSource);
			if (beanProperty) {
				// This will get a connection
				template2.setExceptionTranslator(new SQLErrorCodeSQLExceptionTranslator(mockDataSource));
			}
			else {
				// This will cause creation of default SQL translator.
				// Note that only call should be effective
				template2.afterPropertiesSet();
				template2.afterPropertiesSet();
			}
			RowCountCallbackHandler rcch = new RowCountCallbackHandler();
			template2.query(
				"SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3",
				rcch);
			fail("Shouldn't have executed query without a connection");
		} 
		catch (CannotGetJdbcConnectionException ex) {
			// pass
			assertTrue("Check root cause", ex.getRootCause() == sex);
		}

		ctrlDataSource.verify();
		ctrlConnection.verify();
	}


	public void testPreparedStatementSetterQueryWithNullArg()
		throws Exception {
		final String sql = "SELECT * FROM FOO WHERE ID > 1";

		replay();

		class MockJdbcTemplate extends JdbcTemplate {
			private boolean valid = false;
			public MockJdbcTemplate(DataSource ds) {
				super(ds);
			}
			// Override this so we don't need to get connection
			public void query(String sql, RowCallbackHandler rch) {
				valid = true;
			}
		}

		MockJdbcTemplate mockTemplate = new MockJdbcTemplate(mockDataSource);
		mockTemplate.query(sql, null, null);
		assertTrue("invoked no arg query", mockTemplate.valid);
	}

	public void testPreparedStatementSetterSucceeds() throws Exception {
		final String sql = "UPDATE FOO SET NAME=? WHERE ID = 1";
		final String name = "Gary";
		int expectedRowsUpdated = 1;

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.setString(1, name);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(expectedRowsUpdated);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		PreparedStatementSetter pss = new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setString(1, name);
			}
		};
		int actualRowsUpdated =
			new JdbcTemplate(mockDataSource).update(sql, pss);
		assertTrue(
			"updated correct # of rows",
			actualRowsUpdated == expectedRowsUpdated);

		ctrlPreparedStatement.verify();
	}

	public void testPreparedStatementSetterFails() throws Exception {
		final String sql = "UPDATE FOO SET NAME=? WHERE ID = 1";
		final String name = "Gary";
		SQLException sex = new SQLException();

		MockControl ctrlPreparedStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement =
			(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.setString(1, name);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setThrowable(sex);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		ctrlPreparedStatement.replay();
		replay();

		PreparedStatementSetter pss = new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setString(1, name);
			}
		};
		try {
			new JdbcTemplate(mockDataSource).update(sql, pss);
			fail("Should have failed with SQLException");
		} catch (DataAccessException ex) {
			assertTrue("root cause was preserved", ex.getRootCause() == sex);
		}

		ctrlPreparedStatement.verify();
	}

	public void testCouldntClose() throws Exception {
		SQLException sex = new SQLException("bar");
		mockConnection.close();
		ctrlConnection.setThrowable(sex);

		replay();

		try {
			JdbcTemplate template2 = new JdbcTemplate(mockDataSource);
			RowCountCallbackHandler rcch = new RowCountCallbackHandler();
			template2.query(
				"SELECT ID, FORENAME FROM CUSTMR WHERE ID < 3",
				rcch);
			fail("Should throw exception on failure to close");
		} catch (CleanupFailureDataAccessException ex) {
			// pass
			assertTrue("Check root cause", ex.getRootCause() == sex);
		}
	}

	/**
	 * Mock objects allow us to produce warnings at will
	 */

	public void testFatalWarning() throws Exception {
		String sql = "SELECT forename from custmr";
		SQLWarning warnings = new SQLWarning("My warning");

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement =
			(PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.getWarnings();
		ctrlStatement.setReturnValue(warnings);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		JdbcTemplate t = new JdbcTemplate(mockDataSource);
		t.setIgnoreWarnings(false);
		try {
			t.query(sql, new RowCallbackHandler() {
				public void processRow(ResultSet rs)
					throws SQLException {
					rs.getByte(1);
				}
			});
			fail("Should have thrown exception on warning");
		} catch (SQLWarningException ex) {
			// Pass
			assertTrue(
				"Root cause of warning was correct",
				ex.getRootCause() == warnings);
		}

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	public void testIgnoredWarning() throws Exception {
		String sql = "SELECT forename from custmr";
		SQLWarning warnings = new SQLWarning("My warning");

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement =
			(PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.getWarnings();
		ctrlStatement.setReturnValue(warnings);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		// Too long: truncation
		JdbcTemplate t = new JdbcTemplate(mockDataSource);
		t.setIgnoreWarnings(true);
		t.query(sql, new RowCallbackHandler() {
			public void processRow(ResultSet rs)
				throws SQLException {
				rs.getByte(1);
			}
		});

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

	/**
	 * Test that we see an SQLException translated using Error Code
	 */
	public void testSQLErrorCodeTranslation() throws Exception {
		final SQLException sex =
			new SQLException("I have a known problem", "99999", 1054);
		final String sql = "SELECT ID FROM CUSTOMER";
		Object[][] results = new Object[][] { { new Integer(1)}, {
				new Integer(2)
				}
		};

		MockControl ctrlDatabaseMetaData =
			MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData =
			(DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement =
			(PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData);

		ctrlDatabaseMetaData.replay();
		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		try {
			template.query(sql, new RowCallbackHandler() {
				public void processRow(ResultSet rs)
					throws SQLException {
					throw sex;
				}
			});
			fail("Should have thrown exception");
		} catch (BadSqlGrammarException ex) {
			assertTrue(
				"Wanted same exception back, not " + ex,
				sex == ex.getRootCause());
		} catch (Exception ex) {
			fail(
				"Should have thrown BadSqlGrammarException exception, not "
					+ ex);
		}

		ctrlDatabaseMetaData.verify();
		ctrlResultSet.verify();
		ctrlStatement.verify();
	}
	
	
	/**
	 * Test that we see an SQLException translated using Error Code.
	 * If we provide the SQLExceptionTranslator, we shouldn't use a connection
	 * to get the metadata
	 */
	public void testUseCustomSQLErrorCodeTranslator() throws Exception {
		// Bad SQL state
		final SQLException sex =
			new SQLException("I have a known problem", "07000", 1054);
		final String sql = "SELECT ID FROM CUSTOMER";
		Object[][] results = new Object[][] { { new Integer(1)}, {
				new Integer(2)
				}
		};

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement =
			MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockStatement =
			(PreparedStatement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);
		
		// Change behaviour in setUp() because we only expect one call to getConnection():
		// none is necessary to get metadata for exception translator
		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement, 1);
		mockConnection.close();
		ctrlConnection.setVoidCallable(1);
		ctrlConnection.replay();

		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		ctrlDataSource.setReturnValue(mockConnection, 1);
		ctrlDataSource.replay();
		///// end changed behaviour

		ctrlResultSet.replay();
		ctrlStatement.replay();
	

		JdbcTemplate template = new JdbcTemplate();
		template.setDataSource(mockDataSource);
		// Set custom exception translator
		template.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		template.afterPropertiesSet();
		try {
			template.query(sql, new RowCallbackHandler() {
				public void processRow(ResultSet rs)
					throws SQLException {
					throw sex;
				}
			});
			fail("Should have thrown exception");
		} catch (BadSqlGrammarException ex) {
			assertTrue(
				"Wanted same exception back, not " + ex,
				sex == ex.getRootCause());
		} 

		ctrlResultSet.verify();
		ctrlStatement.verify();
		
		// We didn't call superclass replay() so we need to check these ourselves
		ctrlDataSource.verify();
		ctrlConnection.verify();
	}

	public void testNativeJdbcExtractorInvoked() throws Exception {
		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		final ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.close();
		ctrlResultSet.setVoidCallable(2);

		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		final Statement mockStatement = (Statement) ctrlStatement.getMock();
		mockStatement.getWarnings();
		ctrlStatement.setReturnValue(null);
		mockStatement.close();
		ctrlStatement.setVoidCallable();
		MockControl ctrlStatement2 = MockControl.createControl(Statement.class);
		final Statement mockStatement2 = (Statement) ctrlStatement2.getMock();
		mockStatement2.executeQuery("my query");
		ctrlStatement2.setReturnValue(mockResultSet, 1);

		MockControl ctrlPreparedStatement =	MockControl.createControl(PreparedStatement.class);
		final PreparedStatement mockPreparedStatement =	(PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setReturnValue(null);
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();
		MockControl ctrlPreparedStatement2 =	MockControl.createControl(PreparedStatement.class);
		final PreparedStatement mockPreparedStatement2 =	(PreparedStatement) ctrlPreparedStatement2.getMock();
		mockPreparedStatement2.executeQuery();
		ctrlPreparedStatement2.setReturnValue(mockResultSet, 1);

		MockControl ctrlCallableStatement =	MockControl.createControl(CallableStatement.class);
		final CallableStatement mockCallableStatement =	(CallableStatement) ctrlCallableStatement.getMock();
		mockCallableStatement.close();
		ctrlCallableStatement.setVoidCallable();
		MockControl ctrlCallableStatement2 =	MockControl.createControl(CallableStatement.class);
		final CallableStatement mockCallableStatement2 =	(CallableStatement) ctrlCallableStatement2.getMock();
		mockCallableStatement2.execute();
		ctrlCallableStatement2.setReturnValue(true);
		mockCallableStatement2.getMoreResults();
		ctrlCallableStatement2.setReturnValue(false);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		ctrlStatement2.replay();
		ctrlPreparedStatement.replay();
		ctrlPreparedStatement2.replay();
		ctrlCallableStatement.replay();
		ctrlCallableStatement2.replay();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement, 1);
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		template.setNativeJdbcExtractor(new NativeJdbcExtractor() {
			public boolean isNativeConnectionNecessaryForNativeStatements() {
				return false;
			}
			public Connection getNativeConnection(Connection con) {
				return con;
			}
			public Connection getNativeConnectionFromStatement(Statement stmt) throws SQLException {
				return stmt.getConnection();
			}
			public Statement getNativeStatement(Statement stmt) {
				assertTrue(stmt == mockStatement);
				return mockStatement2;
			}
			public PreparedStatement getNativePreparedStatement(PreparedStatement ps) {
				assertTrue(ps == mockPreparedStatement);
				return mockPreparedStatement2;
			}
			public CallableStatement getNativeCallableStatement(CallableStatement cs) {
				assertTrue(cs == mockCallableStatement);
				return mockCallableStatement2;
			}
			public ResultSet getNativeResultSet(ResultSet rs) throws SQLException {
				return rs;
			}
		});

		template.doWithResultSetFromStaticQuery("my query",	new ResultSetExtractor() {
			public void extractData(ResultSet rs2) throws SQLException {
				assertEquals(mockResultSet, rs2);
			}
		});

		template.doWithResultSetFromPreparedQuery(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
				return mockPreparedStatement;
			}
		}, new ResultSetExtractor() {
			public void extractData(ResultSet rs2) throws SQLException {
				assertEquals(mockResultSet, rs2);
			}
		});

		template.execute(new CallableStatementCreator() {
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				return mockCallableStatement;
			}
		},
		new ArrayList());

		ctrlStatement.verify();
		ctrlStatement2.verify();
		ctrlPreparedStatement.verify();
		ctrlPreparedStatement2.verify();
		ctrlCallableStatement.verify();
		ctrlCallableStatement2.verify();
	}

	public void testStaticResultSetClosed() throws Exception {
		MockControl ctrlResultSet;
		ResultSet mockResultSet;
		MockControl ctrlStatement;
		Statement mockStatement;
		MockControl ctrlResultSet2;
		ResultSet mockResultSet2;
		MockControl ctrlPreparedStatement;
		PreparedStatement mockPreparedStatement;

		try {
			ctrlResultSet = MockControl.createControl(ResultSet.class);
			mockResultSet = (ResultSet) ctrlResultSet.getMock();
			mockResultSet.close();
			ctrlResultSet.setVoidCallable();

			ctrlStatement = MockControl.createControl(Statement.class);
			mockStatement = (Statement) ctrlStatement.getMock();
			mockStatement.executeQuery("my query");
			ctrlStatement.setReturnValue(mockResultSet);
			mockStatement.close();
			ctrlStatement.setVoidCallable();

			ctrlResultSet2 = MockControl.createControl(ResultSet.class);
			mockResultSet2 = (ResultSet) ctrlResultSet2.getMock();
			mockResultSet2.close();
			ctrlResultSet2.setVoidCallable();

			ctrlPreparedStatement =	MockControl.createControl(PreparedStatement.class);
			mockPreparedStatement =	(PreparedStatement) ctrlPreparedStatement.getMock();
			mockPreparedStatement.executeQuery();
			ctrlPreparedStatement.setReturnValue(mockResultSet2);
			mockPreparedStatement.close();
			ctrlPreparedStatement.setVoidCallable();

			mockConnection.createStatement();
			ctrlConnection.setReturnValue(mockStatement);
			mockConnection.prepareStatement("my query");
			ctrlConnection.setReturnValue(mockPreparedStatement);
		}
		catch (SQLException sex) {
			throw new RuntimeException("EasyMock initialization of jdbc objects failed");
		}

		ctrlResultSet.replay();
		ctrlStatement.replay();
		ctrlResultSet2.replay();
		ctrlPreparedStatement.replay();
		replay();

		JdbcTemplate template = new JdbcTemplate(mockDataSource);

		try {
			template.doWithResultSetFromStaticQuery("my query",	new ResultSetExtractor() {
				public void extractData(ResultSet rs) throws SQLException {
					throw new InvalidDataAccessApiUsageException("");
				}
			});
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// ok
		}

		try {
			template.doWithResultSetFromPreparedQuery(new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					return con.prepareStatement("my query");
				}
			}, new ResultSetExtractor() {
				public void extractData(ResultSet rs2) throws SQLException {
					throw new InvalidDataAccessApiUsageException("");
				}
			});
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// ok
		}

		// verify confirms if test is successful by checking if close() called
		ctrlResultSet.verify();
		ctrlStatement.verify();
		ctrlResultSet2.verify();
		ctrlPreparedStatement.verify();
	}

	public void testExecuteClosed() throws Exception {
		MockControl ctrlResultSet;
		ResultSet mockResultSet;
		MockControl ctrlCallable;
		CallableStatement mockCallable;

		try {
			ctrlResultSet = MockControl.createControl(ResultSet.class);
			mockResultSet = (ResultSet) ctrlResultSet.getMock();
			mockResultSet.next();
			ctrlResultSet.setReturnValue(true);
			mockResultSet.close();
			ctrlResultSet.setVoidCallable();

			ctrlCallable = MockControl.createControl(CallableStatement.class);
			mockCallable = (CallableStatement) ctrlCallable.getMock();
			mockCallable.execute();
			ctrlCallable.setReturnValue(true);
			mockCallable.getResultSet();
			ctrlCallable.setReturnValue(mockResultSet);
			mockCallable.close();
			ctrlCallable.setVoidCallable();

			mockConnection.prepareCall("my query");
			ctrlConnection.setReturnValue(mockCallable);

		} catch (SQLException sex) {
			throw new RuntimeException("EasyMock initialization of jdbc objects failed");
		}

		ctrlResultSet.replay();
		ctrlCallable.replay();
		replay();

		List params = new ArrayList();
		params.add(new SqlReturnResultSet("", new RowCallbackHandler() {
			public void processRow(ResultSet rs) throws SQLException {
				throw new InvalidDataAccessApiUsageException("");
			}

		}));

		JdbcTemplate template = new JdbcTemplate(mockDataSource);
		try {
			template.execute(new CallableStatementCreator() {
				public CallableStatement createCallableStatement(Connection conn)
					throws SQLException {
					return conn.prepareCall("my query");
				}
			}, params);
		} catch (InvalidDataAccessApiUsageException idaauex) {
			// ok
		}

		// verify confirms if test is successful by checking if close() called
		ctrlResultSet.verify();
		ctrlCallable.verify();
	}

	class Dispatcher implements PreparedStatementCreator {
		private int id;
		private String sql;

		public Dispatcher(int id, String sql) {
			this.id = id;
			this.sql = sql;
		}
		public PreparedStatement createPreparedStatement(Connection conn)
			throws SQLException {
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setInt(1, id);
			return ps;
		}
	};

}
