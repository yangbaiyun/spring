/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.jdbc.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

/**
 * <b>This is the central class in the JDBC core package.</b>
 * It simplifies the use of JDBC and helps to avoid common errors. It executes
 * core JDBC workflow, leaving application code to provide SQL and extract results.
 * This class executes SQL queries or updates, initating iteration over
 * ResultSets and catching JDBC exceptions and translating them to
 * the generic, more informative, exception hierarchy defined in
 * the org.springframework.dao package.
 *
 * <p>Code using this class need only implement callback interfaces,
 * giving them a clearly defined contract. The PreparedStatementCreator callback
 * interface creates a prepared statement given a Connection provided by this class,
 * providing SQL and any necessary parameters. The RowCallbackHandler interface
 * extracts values from each row of a ResultSet.
 *
 * <p>Can be used within a service implementation via direct instantiation
 * with a DataSource reference, or get prepared in an application context
 * and given to services as bean reference. Note: The DataSource should
 * always be configured as a bean in the application context, in the first case
 * given to the service directly, in the second case to the prepared template.
 *
 * <p>The motivation and design of this class is discussed
 * in detail in
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 *
 * <p>Because this class is parameterizable by the callback interfaces and the
 * SQLExceptionTranslator interface, it isn't necessary to subclass it.
 * All SQL issued by this class is logged.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Yann Caroff
 * @author Thomas Risberg
 * @author Isabelle Muszynski
 * @version $Id: JdbcTemplate.java,v 1.19 2004/01/04 23:43:42 jhoeller Exp $
 * @since May 3, 2001
 * @see org.springframework.dao
 * @see org.springframework.jdbc.object
 * @see org.springframework.jdbc.datasource
 */
public class JdbcTemplate extends JdbcAccessor implements IJdbcTemplate, InitializingBean {

	/**
	 * Constant for use as a parameter to query methods to force use of a PreparedStatement
	 * rather than a Statement, even when there are no bind parameters.
	 * For example, query(sql, JdbcTemplate.PREPARE_STATEMENT, callbackHandler)
	 * will force the use of a JDBC PreparedStatement even if the SQL
	 * passed in has no bind parameters.
	 */
	public static final PreparedStatementSetter PREPARE_STATEMENT =
	    new PreparedStatementSetter() {
		    public void setValues(PreparedStatement ps) throws SQLException {
			    // do nothing
		    }
	    };

	protected final Log logger = LogFactory.getLog(getClass());

	/** Custom NativeJdbcExtractor */
	private NativeJdbcExtractor nativeJdbcExtractor;

	/** If this variable is false, we will throw exceptions on SQL warnings */
	private boolean ignoreWarnings = true;


	/**
	 * Construct a new JdbcTemplate for bean usage.
	 * Note: The DataSource has to be set before using the instance.
	 * This constructor can be used to prepare a JdbcTemplate via a BeanFactory,
	 * typically setting the DataSource via setDataSource.
	 * @see #setDataSource
	 */
	public JdbcTemplate() {
	}

	/**
	 * Construct a new JdbcTemplate, given a DataSource to obtain connections from.
	 * Note: This will trigger eager initialization of the exception translator.
	 * @param dataSource JDBC DataSource to obtain connections from
	 */
	public JdbcTemplate(DataSource dataSource) {
		setDataSource(dataSource);
		afterPropertiesSet();
	}

	/**
	 * Set a NativeJdbcExtractor to extract native JDBC objects from wrapped handles.
	 * Useful if native Statement and/or ResultSet handles are expected for casting
	 * to database-specific implementation classes, but a connection pool that wraps
	 * JDBC objects is used (note: <i>any</i> pool will return wrapped Connections).
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor extractor) {
		this.nativeJdbcExtractor = extractor;
	}
	
	/**
	 * Return the current NativeJdbcExtractor implementation.
	 */
	public NativeJdbcExtractor getNativeJdbcExtractor() {
		return this.nativeJdbcExtractor;
	}

	/**
	 * Set whether or not we want to ignore SQLWarnings.
	 * Default is true.
	 */
	public void setIgnoreWarnings(boolean ignoreWarnings) {
		this.ignoreWarnings = ignoreWarnings;
	}

	/**
	 * Return whether or not we ignore SQLWarnings.
	 * Default is true.
	 */
	public boolean getIgnoreWarnings() {
		return ignoreWarnings;
	}


	//-------------------------------------------------------------------------
	// Public methods
	//-------------------------------------------------------------------------

	/**
	 * Execute a query given static SQL.
	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to execute
	 * a static query with a PreparedStatement, use the overloaded query method
	 * with the PREPARE_STATEMENT constant as PreparedStatementSetter argument.
	 * <p>In most cases the query() method should be preferred to the parallel
	 * doWithResultSetXXXX() method. The doWithResultSetXXXX() methods are
	 * included to allow full control over the extraction of data from ResultSets
	 * and to facilitate integration with third-party software.
	 * @param sql SQL query to execute
	 * @param callbackHandler object that will extract results
	 * @throws DataAccessException if there is any problem executing the query
	 * @see #query(String, PreparedStatementSetter, RowCallbackHandler)
	 * @see #PREPARE_STATEMENT
	 */
	public void query(String sql, RowCallbackHandler callbackHandler) throws DataAccessException {
		doWithResultSetFromStaticQuery(sql,
		    new RowCallbackHandlerResultSetExtractor(callbackHandler));
	}

	/**
	 * Execute a query given static SQL.
	 * Uses a JDBC Statement, not a PreparedStatement. If you want to execute
	 * a static query with a PreparedStatement, use the overloaded query method
	 * with a NOP PreparedStatement setter as a parameter.
	 * @param sql SQL query to execute
	 * @param rse object that will extract all rows of results
	 * @throws DataAccessException if there is any problem executing
	 * the query
	 */
	public void doWithResultSetFromStaticQuery(String sql, ResultSetExtractor rse) throws DataAccessException {
		if (sql == null) {
			throw new InvalidDataAccessApiUsageException("SQL may not be null");
		}
		if (containsBindVariables(sql)) {
			throw new InvalidDataAccessApiUsageException(
			    "Cannot execute [" + sql + "] as a static query: it contains bind variables");
		}
		Connection con = DataSourceUtils.getConnection(getDataSource());
		Statement stmt = null;
		ResultSet rs = null;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Executing static SQL query [" + sql + "] using a java.sql.Statement");
			}
			Connection conToUse = con;
			if (this.nativeJdbcExtractor != null &&
			    this.nativeJdbcExtractor.isNativeConnectionNecessaryForNativeStatements()) {
				conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
			}
			stmt = conToUse.createStatement();
			DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
			Statement stmtToUse = stmt;
			if (this.nativeJdbcExtractor != null) {
				stmtToUse = this.nativeJdbcExtractor.getNativeStatement(stmt);
			}
			rs = stmtToUse.executeQuery(sql);
			ResultSet rsToUse = rs;
			if (this.nativeJdbcExtractor != null) {
				rsToUse = this.nativeJdbcExtractor.getNativeResultSet(rs);
			}
			rse.extractData(rsToUse);
			SQLWarning warning = stmt.getWarnings();
			throwExceptionOnWarningIfNotIgnoringWarnings(warning);
		}
		catch (SQLException ex) {
			throw getExceptionTranslator().translate("JdbcTemplate.query(sql)", sql, ex);
		}
		finally {
			if (rs != null) {
				try {
					rs.close();
				}
				catch (SQLException ignore) {
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				}
				catch (SQLException ignore) {
				}
			}
			DataSourceUtils.closeConnectionIfNecessary(con, getDataSource());
		}
	}

	/**
	 * Query using a prepared statement.
	 * @param psc Callback handler that can create a PreparedStatement
	 * given a Connection
	 * @param callbackHandler object that will extract results,
	 * one row at a time
	 * @throws DataAccessException if there is any problem
	 */
	public void query(PreparedStatementCreator psc, RowCallbackHandler callbackHandler)
	    throws DataAccessException {
		doWithResultSetFromPreparedQuery(psc,
		    new RowCallbackHandlerResultSetExtractor(callbackHandler));
	}

	/**
	 * Query using a prepared statement. Most other query methods use this method.
	 * @param psc Callback handler that can create a PreparedStatement given a
	 * Connection
	 * @param rse object that will extract results.
	 * @throws DataAccessException if there is any problem
	 */
	public void doWithResultSetFromPreparedQuery(PreparedStatementCreator psc, ResultSetExtractor rse)
	    throws DataAccessException {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Connection conToUse = con;
			if (this.nativeJdbcExtractor != null &&
			    this.nativeJdbcExtractor.isNativeConnectionNecessaryForNativeStatements()) {
				conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
			}
			ps = psc.createPreparedStatement(conToUse);
			if (logger.isDebugEnabled()) {
				logger.debug("Executing SQL query using PreparedStatement [" + psc + "]");
			}
			PreparedStatement psToUse = ps;
			if (this.nativeJdbcExtractor != null) {
				psToUse = this.nativeJdbcExtractor.getNativePreparedStatement(ps);
			}
			rs = psToUse.executeQuery();
			ResultSet rsToUse = rs;
			if (this.nativeJdbcExtractor != null) {
				rsToUse = this.nativeJdbcExtractor.getNativeResultSet(rs);
			}
			rse.extractData(rsToUse);
			SQLWarning warning = ps.getWarnings();
			throwExceptionOnWarningIfNotIgnoringWarnings(warning);
		}
		catch (SQLException ex) {
			throw getExceptionTranslator().translate(
			    "JdbcTemplate.query with PreparedStatementCreator [" + psc + "]",
			    null, ex);
		}
		finally {
			if (rs != null) {
				try {
					rs.close();
				}
				catch (SQLException ignore) {
				}
			}
			if (ps != null) {
				try {
					ps.close();
				}
				catch (SQLException ignore) {
				}
			}
			DataSourceUtils.closeConnectionIfNecessary(con, getDataSource());
		}
	}

	/**
	 * Query given SQL to create a prepared statement from SQL and a
	 * PreparedStatementSetter implementation that knows how to bind values
	 * to the query.
	 * @param sql SQL to execute
	 * @param pss object that knows how to set values on the prepared statement.
	 * If this is null, the SQL will be assumed to contain no bind parameters.
	 * Even if there are no bind parameters, this object may be used to
	 * set fetch size and other performance options.
	 * @param callbackHandler object that will extract results
	 * @throws DataAccessException if the query fails
	 */
	public void query(final String sql, final PreparedStatementSetter pss, RowCallbackHandler callbackHandler)
	    throws DataAccessException {
		if (sql == null) {
			throw new InvalidDataAccessApiUsageException("SQL may not be null");
		}
		if (pss == null) {
			// Check there are no bind parameters, in which case pss could not be null
			if (containsBindVariables(sql))
				throw new InvalidDataAccessApiUsageException(
				    "SQL [" + sql + "] requires at least one bind variable, but PreparedStatementSetter parameter was null");
			query(sql, callbackHandler);
		}
		else {
			// Wrap it in a new PreparedStatementCreator
			query(new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					PreparedStatement ps = con.prepareStatement(sql);
					DataSourceUtils.applyTransactionTimeout(ps, getDataSource());
					PreparedStatement psToUse = ps;
					if (nativeJdbcExtractor != null) {
						psToUse = nativeJdbcExtractor.getNativePreparedStatement(ps);
					}
					pss.setValues(psToUse);
					return psToUse;
				}
			}, callbackHandler);
		}
	}

	/**
	 * Return whether the given SQL String contains bind variables
	 */
	private boolean containsBindVariables(String sql) {
		return sql.indexOf("?") != -1;
	}

	/**
	 * Issue a single SQL update.
	 * @param sql static SQL to execute
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem.
	 */
	public int update(final String sql) throws DataAccessException {
		if (logger.isDebugEnabled()) {
			logger.debug("Running SQL update [" + sql + "]");
		}
		return update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(sql);
				DataSourceUtils.applyTransactionTimeout(ps, getDataSource());
				if (nativeJdbcExtractor != null) {
					return nativeJdbcExtractor.getNativePreparedStatement(ps);
				}
				return ps;
			}
		});
	}

	/**
	 * Issue an update using a PreparedStatementCreator to provide SQL and any
	 * required parameters.
	 * @param psc callback object that provides SQL and any necessary parameters
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	public int update(PreparedStatementCreator psc) throws DataAccessException {
		return update(new PreparedStatementCreator[]{psc})[0];
	}

	/**
	 * Issue multiple updates using multiple PreparedStatementCreators to provide
	 * SQL and any required parameters.
	 * @param pscs array of callback objects that provide SQL and any necessary parameters
	 * @return an array of the number of rows affected by each statement
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	public int[] update(PreparedStatementCreator[] pscs) throws DataAccessException {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		PreparedStatement ps = null;
		int index = 0;
		try {
			Connection conToUse = con;
			if (this.nativeJdbcExtractor != null &&
			    this.nativeJdbcExtractor.isNativeConnectionNecessaryForNativeStatements()) {
				conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
			}
			int[] retVals = new int[pscs.length];
			for (index = 0; index < retVals.length; index++) {
				ps = pscs[index].createPreparedStatement(conToUse);
				if (logger.isDebugEnabled()) {
					logger.debug("Executing SQL update using PreparedStatement [" + pscs[index] + "]");
				}
				retVals[index] = ps.executeUpdate();
				if (logger.isDebugEnabled()) {
					logger.debug("SQL update affected " + retVals[index] + " rows");
				}
				ps.close();
			}

			// Don't worry about warnings, as we're more likely to get exception on updates
			// (for example on data truncation)
			return retVals;
		}
		catch (SQLException ex) {
			if (ps != null) {
				try {
					ps.close();
				}
				catch (SQLException ignore) {
				}
			}
			throw getExceptionTranslator().translate(
			    "processing update " + (index + 1) + " of " + pscs.length + "; update was [" + pscs[index] + "]",
			    null, ex);
		}
		finally {
			DataSourceUtils.closeConnectionIfNecessary(con, getDataSource());
		}
	}

	/**
	 * Issue an update using a PreparedStatementSetter to set bind parameters,
	 * with given SQL. Simpler than using a PreparedStatementCreator as this
	 * method will create the PreparedStatement: The PreparedStatementSetter
	 * just needs to set parameters.
	 * @param sql SQL, containing bind parameters
	 * @param pss helper that sets bind parameters. If this is null
	 * we run an update with static SQL.
	 * @return the number of rows affected
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	public int update(final String sql, final PreparedStatementSetter pss) throws DataAccessException {
		if (pss == null) {
			return update(sql);
		}
		return update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(sql);
				DataSourceUtils.applyTransactionTimeout(ps, getDataSource());
				PreparedStatement psToUse = ps;
				if (nativeJdbcExtractor != null) {
					psToUse = nativeJdbcExtractor.getNativePreparedStatement(ps);
				}
				pss.setValues(psToUse);
				return ps;
			}
		});
	}

	/**
	 * Issue multiple updates using JDBC 2.0 batch updates and PreparedStatementSetters to
	 * set values on a PreparedStatement created by this method
	 * @param sql defining PreparedStatement that will be reused.
	 * All statements in the batch will use the same SQL.
	 * @param pss object to set parameters on the
	 * PreparedStatement created by this method
	 * @return an array of the number of rows affected by each statement
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	public int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		PreparedStatement ps = null;
		try {
			Connection conToUse = con;
			if (this.nativeJdbcExtractor != null &&
			    this.nativeJdbcExtractor.isNativeConnectionNecessaryForNativeStatements()) {
				conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
			}
			ps = conToUse.prepareStatement(sql);
			DataSourceUtils.applyTransactionTimeout(ps, getDataSource());
			PreparedStatement psToUse = ps;
			if (this.nativeJdbcExtractor != null) {
				psToUse = this.nativeJdbcExtractor.getNativePreparedStatement(ps);
			}
			int batchSize = pss.getBatchSize();
			for (int i = 0; i < batchSize; i++) {
				pss.setValues(psToUse, i);
				ps.addBatch();
			}
			int[] retVals = ps.executeBatch();
			ps.close();
			return retVals;
		}
		catch (SQLException ex) {
			if (ps != null) {
				try {
					ps.close();
				}
				catch (SQLException ignore) {
				}
			}
			throw getExceptionTranslator().translate(
			    "processing batch update with size=" + pss.getBatchSize() + "; update was [" + sql + "]",
			    sql, ex);
		}
		finally {
			DataSourceUtils.closeConnectionIfNecessary(con, getDataSource());
		}
	}

	/**
	 * Execute an Sql call using a CallableStatementCreator to provide SQL and any required
	 * parameters.
	 * @param csc callback object that provides SQL and any necessary parameters
	 * @return Map of extracted out parameters
	 * @throws DataAccessException if there is any problem issuing the update
	 */
	public Map execute(CallableStatementCreator csc, List declaredParameters) throws DataAccessException {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		CallableStatement cs = null;
		try {
			Connection conToUse = con;
			if (this.nativeJdbcExtractor != null &&
			    this.nativeJdbcExtractor.isNativeConnectionNecessaryForNativeStatements()) {
				conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
			}
			cs = csc.createCallableStatement(conToUse);
			CallableStatement csToUse = cs;
			if (this.nativeJdbcExtractor != null) {
				csToUse = this.nativeJdbcExtractor.getNativeCallableStatement(cs);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Executing call using CallableStatement [" + cs + "]");
			}
			boolean retVal = csToUse.execute();
			if (logger.isDebugEnabled()) {
				logger.debug("CallableStatement.execute returned [" + retVal + "]");
			}
			Map retMap = new HashMap();
			if (retVal) {
				retMap.putAll(extractReturnedResultSets(csToUse, declaredParameters));
			}
			retMap.putAll(extractOutputParameters(csToUse, declaredParameters));
			return retMap;
		}
		catch (SQLException ex) {
			throw getExceptionTranslator().translate(
			    "JdbcTemplate.execute()",
			    csc.toString(),
			    ex);
		}
		finally {
			if (cs != null) {
				try {
					cs.close();
				}
				catch (SQLException ignore) {
				}
			}
			DataSourceUtils.closeConnectionIfNecessary(con, getDataSource());
		}
	}

	/**
	 * Extract output parameters from the completed stored procedure.
	 * @param cs JDBC wrapper for the stored procedure
	 * @param parameters parameter list for the stored procedure
	 * @return parameters to the stored procedure
	 */
	private Map extractOutputParameters(CallableStatement cs, List parameters) throws SQLException {
		Map outParams = new HashMap();
		int sqlColIndx = 1;
		for (int i = 0; i < parameters.size(); i++) {
			SqlParameter p = (SqlParameter) parameters.get(i);
			if (p instanceof SqlOutParameter) {
				Object out = null;
				out = cs.getObject(sqlColIndx);
				if (out instanceof ResultSet) {
					// We can't pass back a resultset since the connection will be closed - we must process it
					try {
						if (((SqlOutParameter) p).isResultSetSupported()) {
							ResultSetExtractor rse = null;
							if (((SqlOutParameter) p).isRowMapperSupported())
								rse = new RowCallbackHandlerResultSetExtractor(((SqlOutParameter) p).newResultReader());
							else
								rse = new RowCallbackHandlerResultSetExtractor(((SqlOutParameter) p).getRowCallbackHandler());
							rse.extractData((ResultSet) out);
							logger.debug("ResultSet returned from stored procedure was processed");
							if (((SqlOutParameter) p).isRowMapperSupported()) {
								outParams.put(p.getName(), ((ResultReader) ((RowCallbackHandlerResultSetExtractor) rse).getCallbackHandler()).getResults() );
							}
							else {
								outParams.put(p.getName(), "ResultSet processed.");
							}
						}
						else {
							logger.warn("ResultSet returned from stored procedure but a corresponding SqlOutParameter with a RowCallbackHandler was not declared");
							outParams.put(p.getName(), "ResultSet was returned but not processed.");
						}
					}
					catch (SQLException se) {
						throw se;
					}
					finally {
						try {
							((ResultSet) out).close();
						}
						catch (SQLException ignore) {
						}
					}
				}
				else {
					outParams.put(p.getName(), out);
				}
			}
			if (!(p instanceof SqlReturnResultSet)) {
				sqlColIndx++;
			}
		}
		return outParams;
	}

	/**
	 * Extract returned resultsets from the completed stored procedure.
	 * @param cs JDBC wrapper for the stored procedure
	 * @param parameters Parameter list for the stored procedure
	 */
	private Map extractReturnedResultSets(CallableStatement cs, List parameters) throws SQLException {
		Map returnedResults = new HashMap();
		int rsIndx = 0;
		do {
			SqlParameter p = null;
			if (parameters != null && parameters.size() > rsIndx) {
				p = (SqlParameter) parameters.get(rsIndx);
			}
			if (p != null && p instanceof SqlReturnResultSet) {
				ResultSet rs = null;
				rs = cs.getResultSet();
				try {
					ResultSet rsToUse = rs;
					if (this.nativeJdbcExtractor != null) {
						rsToUse = this.nativeJdbcExtractor.getNativeResultSet(rs);
					}
					ResultSetExtractor rse = null;
					if (((SqlReturnResultSet) p).isRowMapperSupported()) {
						rse = new RowCallbackHandlerResultSetExtractor(((SqlReturnResultSet) p).newResultReader());
					}
					else {
						rse = new RowCallbackHandlerResultSetExtractor(((SqlReturnResultSet) p).getRowCallbackHandler());
					}
					rse.extractData(rsToUse);
					if (((SqlReturnResultSet) p).isRowMapperSupported()) {
						returnedResults.put(p.getName(), ((ResultReader)((RowCallbackHandlerResultSetExtractor) rse).getCallbackHandler()).getResults());
					}
					else {
						returnedResults.put(p.getName(), "ResultSet returned from stored procedure was processed");
					}
				}
				catch (SQLException se) {
					throw se;
				}
				finally {
					try {
						rs.close();
					}
					catch (SQLException ignore) {
					}
				}
			}
			else {
				logger.warn("ResultSet returned from stored procedure but a corresponding SqlReturnResultSet parameter was not declared");
			}
			rsIndx++;
		} while (cs.getMoreResults());
		
		return returnedResults;
	}

	/**
	 * Convenience method to throw an SQLWarningException if we're
	 * not ignoring warnings.
	 * @param warning warning from current statement. May be null,
	 * in which case this method does nothing.
	 */
	private void throwExceptionOnWarningIfNotIgnoringWarnings(SQLWarning warning)
	    throws SQLWarningException {
		if (warning != null) {
			if (this.ignoreWarnings) {
				logger.warn("SQLWarning ignored: " + warning);
			}
			else {
				throw new SQLWarningException("Warning not ignored", warning);
			}
		}
	}


	/**
	 * Adapter to enable use of a RowCallbackHandler inside a
	 * ResultSetExtractor. Uses a  regular ResultSet, so we have
	 * to be careful when using it, so we don't use it for navigating
	 * since this could lead to unpreditable consequences.
	 */
	private static final class RowCallbackHandlerResultSetExtractor implements ResultSetExtractor {

		/** RowCallbackHandler to use to extract data */
		private RowCallbackHandler callbackHandler;

		/**
		 * Construct a new ResultSetExtractor that will use the given
		 * RowCallbackHandler to process each row.
		 */
		private RowCallbackHandlerResultSetExtractor(RowCallbackHandler callbackHandler) {
			this.callbackHandler = callbackHandler;
		}

		public RowCallbackHandler getCallbackHandler() {
			return callbackHandler;
		}

		public void extractData(ResultSet rs) throws SQLException {
			while (rs.next()) {
				this.callbackHandler.processRow(rs);
			}
		}
	}

}
