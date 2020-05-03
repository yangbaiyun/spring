package org.springframework.jdbc.support;

import java.sql.SQLException;

import junit.framework.TestCase;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;

/**
 * 
 * @author Rod Johnson
 * @since 13-Jan-03
 */
public class SQLStateExceptionTranslatorTests extends TestCase {
	
	private SQLStateSQLExceptionTranslator trans = new SQLStateSQLExceptionTranslator();

	// ALSO CHECK CHAIN of SQLExceptions!?
	// also allow chain of translators? default if can't do specific?

	public void testBadSqlGrammar() {
		String sql = "SELECT FOO FROM BAR";
		SQLException sex = new SQLException("Message", "42001", 1);
		try {
			throw this.trans.translate("task", sql, sex); 
		}
		catch (BadSqlGrammarException ex) {
			// OK
			assertTrue("SQL is correct", sql.equals(ex.getSql()));
			assertTrue("Exception matches", sex.equals(ex.getSQLException()));
		}
	}
	
	public void testInvalidSqlStateCode() {
		String sql = "SELECT FOO FROM BAR";
		SQLException sex = new SQLException("Message", "NO SUCH CODE", 1);
		try {
			throw this.trans.translate("task", sql, sex);
		}
		catch (UncategorizedSQLException ex) {
			// OK
			assertTrue("SQL is correct", sql.equals(ex.getSql()));
			assertTrue("Exception matches", sex.equals(ex.getSQLException()));
		}
	}
	
	/**
	 * PostgreSQL can return null
	 * SAP DB can apparently return empty SQL code
	 * Bug 729170 
	 */
	public void testMalformedSqlStateCodes() {
		String sql = "SELECT FOO FROM BAR";
		SQLException sex = new SQLException("Message", null, 1);
		testMalformedSqlStateCode(sex);
		
		sex = new SQLException("Message", "", 1);
		testMalformedSqlStateCode(sex);
				
		// One char's not allowed
		sex = new SQLException("Message", "I", 1);
		testMalformedSqlStateCode(sex);
	}
	
	
	private void testMalformedSqlStateCode(SQLException sex) {
		String sql = "SELECT FOO FROM BAR";
		try {
			throw this.trans.translate("task", sql, sex);
		}
		catch (UncategorizedSQLException ex) {
			// OK
			assertTrue("SQL is correct", sql.equals(ex.getSql()));
			assertTrue("Exception matches", sex.equals(ex.getSQLException()));
		}
	}

}
