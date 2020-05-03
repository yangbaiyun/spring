/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.jdbc;

import java.sql.SQLException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Exception thrown when SQL specified is invalid. Such exceptions always have
 * a java.sql.SQLException root cause.
 *
 * <p>It would be possible to have subclasses for no such table, no such column etc.
 * A custom SQLExceptionTranslator could create such more specific exceptions,
 * without affecting code using this class.
 *
 * @author Rod Johnson
 * @version $Id: BadSqlGrammarException.java,v 1.1 2003/12/05 17:02:36 jhoeller Exp $
 */
public class BadSqlGrammarException extends InvalidDataAccessResourceUsageException {
	
	/** Root cause: underlying JDBC exception. */ 
	private final SQLException ex;
	
	/** The offending SQL. */
	private final String sql;

	/**
	 * Constructor for BadSqlGrammarException.
	 * @param task name of current task (may be null)
	 * @param sql the offending SQL statement
	 * @param ex the root cause
	 */
	public BadSqlGrammarException(String task, String sql, SQLException ex) {
		super("Bad SQL grammar [" + sql + "]" + (task != null ? " in task '" + task + "'" : ""), ex);
		this.ex = ex;
		this.sql = sql;
	}
	
	/**
	 * Return the wrapped SQLException.
	 * @return the wrapped SQLException
	 */
	public SQLException getSQLException() {
		return ex;
	}
	
	/**
	 * Return the SQL that caused the problem.
	 * @return the offdending SQL
	 */
	public String getSql() {
		return sql;
	}

}
