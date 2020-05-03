/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.jdbc.support;

import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;

/**
 * Implementation of SQLExceptionTranslator that uses specific vendor codes.
 * More precise than SQLState implementation, but vendor specific.
 *
 * <p>The JdbcTemplate class enables error handling to be parameterized without
 * making application's dependent on a particular RDBMS.
 *
 * <p>This class applies the following matching rules:
 * <ul>
 * <li>Try custom translation implemented by any subclass. Note that this class is
 * concrete and is typically used itself, in which case this rule doesn't apply.
 * <li>Apply error code matching. Error codes are obtained from the SQLErrorCodesFactory
 * by default. This looks up error codes from the classpath and keys into them from the
 * database name from the database metadata.
 * <li>Fallback to fallback translator. SQLStateSQLExceptionTranslator is the default
 * fallback translator.
 * </ul>
 * 
 * @author Rod Johnson
 * @author Thomas Risberg
 * @see org.springframework.jdbc.support.SQLErrorCodesFactory
 * @version $Id: SQLErrorCodeSQLExceptionTranslator.java,v 1.1 2003/12/05 17:03:14 jhoeller Exp $
 */
public class SQLErrorCodeSQLExceptionTranslator implements SQLExceptionTranslator {

	protected final Log logger = LogFactory.getLog(getClass());

	/** Error codes available to subclasses */
	protected SQLErrorCodes sqlErrorCodes;
	
	/** Fallback translator to use if SQLError code matching doesn't work */
	private SQLExceptionTranslator fallback = new SQLStateSQLExceptionTranslator();
	
	/**
	 * Constructor for use as a JavaBean.
	 * The SqlErrorCodes or DataSource property must be set.
	 */
	public SQLErrorCodeSQLExceptionTranslator() {
	}


	/**
	 * Create a SQLErrorCode translator given these error codes.
	 * Does not require a database metadata lookup to be performed using a connection.
	 * @param sec error codes
	 */
	public SQLErrorCodeSQLExceptionTranslator(SQLErrorCodes sec) {
		this.sqlErrorCodes = sec;		
	}
	
	/**
	 * Create a SQLErrorCode translator for the given DataSource.
	 * Invoking this constructor will cause a connection to be obtained from the 
	 * DataSource to get the metadata
	 * @param ds DataSource to use to find metadata and establish which error
	 * codes are usable
	 */
	public SQLErrorCodeSQLExceptionTranslator(DataSource ds) {
		setDataSource(ds);
	}
	
	/**
	 * Set the DataSource.
	 * Setting this property will cause a connection to be obtained from the 
	 * DataSource to get the metadata
	 * @param ds DataSource to use to find metadata and establish which error
	 * codes are usable
	 */
	public void setDataSource(DataSource ds) {
		this.sqlErrorCodes = SQLErrorCodesFactory.getInstance().getErrorCodes(ds);
	}
	
	/**
	 * Override the default SQLState fallback translator
	 * @param fallback custom fallback exception translator to use if error code
	 * translation fails
	 */
	public void setFallbackTranslator(SQLExceptionTranslator fallback) {
		this.fallback = fallback;
	}

	/**
	 * Set custom error codes to be used for translation
	 * @param sec custom error codes to use
	 */
	public void setSqlErrorCodes(SQLErrorCodes sec) {
		this.sqlErrorCodes = sec;		
	}

	/**
	 * @see org.springframework.jdbc.support.SQLExceptionTranslator#translate(java.lang.String, java.lang.String, java.sql.SQLException)
	 */
	public DataAccessException translate(String task, String sql, SQLException sqlex) {

		// First, try custom translation
		DataAccessException dex = customTranslate(task, sql, sqlex);
		if (dex != null) {
			return dex;
		}
		
		// Now try error codes
		String errorCode = Integer.toString(sqlex.getErrorCode());
		if (errorCode != null) {
			if (Arrays.binarySearch(sqlErrorCodes.getBadSqlGrammarCodes(), errorCode) >= 0) {
				logTranslation(task, sql, sqlex);
				return new BadSqlGrammarException(task, sql, sqlex);
			}
			else if (Arrays.binarySearch(sqlErrorCodes.getDataIntegrityViolationCodes() , errorCode) >= 0) {
				logTranslation(task, sql, sqlex);
				return new DataIntegrityViolationException(task + ": " + sqlex.getMessage(), sqlex);
			}
		}

		// We couldn't identify it more precisely - let's hand it over to the SQLState fallback translator.
		logger.warn("Unable to translate SQLException with errorCode '" + sqlex.getErrorCode() +
						"', will now try the fallback translator");
		return this.fallback.translate(task, sql, sqlex);
	}
	
	
	/**
	 * Subclasses can override this method to attempt a custom mapping from SQLException to DataAccessException
	 * @param task task being attempted
	 * @param sql SQL that caused the problem
	 * @param sqlex offending SQLException
	 * @return null if no custom translation was possible, otherwise a DataAccessException
	 * resulting from custom translation. This exception should include the sqlex parameter
	 * as a nested root cause. This implementation always returns null, meaning that
	 * the translator always falls back to the default error codes.
	 */
	protected DataAccessException customTranslate(String task, String sql, SQLException sqlex) {
		return null;
	}

	private void logTranslation(String task, String sql, SQLException sqlex) {
		logger.warn("Translating SQLException with SQLState '" + sqlex.getSQLState() + "' and errorCode '" + sqlex.getErrorCode() +
						"' and message [" + sqlex.getMessage() + "]; SQL was [" + sql + "] for task [" + task + "]");
	}

}
