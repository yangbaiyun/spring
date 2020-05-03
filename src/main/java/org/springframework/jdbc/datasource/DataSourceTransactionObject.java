package org.springframework.jdbc.datasource;

/**
 * DataSource transaction object, representing a ConnectionHolder.
 * Used as transaction object by DataSourceTransactionManager.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Juergen Hoeller
 * @since 02.05.2003
 * @see DataSourceTransactionManager
 * @see ConnectionHolder
 * @version $Id: DataSourceTransactionObject.java,v 1.4 2003/12/30 01:02:44 jhoeller Exp $
 */
public class DataSourceTransactionObject {

	private ConnectionHolder connectionHolder;

	private Integer previousIsolationLevel;
	
	private boolean mustRestoreAutoCommit;

	/**
	 * Create DataSourceTransactionObject for new ConnectionHolder.
	 */
	public DataSourceTransactionObject() {
	}

	/**
	 * Create DataSourceTransactionObject for existing ConnectionHolder.
	 */
	protected DataSourceTransactionObject(ConnectionHolder connectionHolder) {
		this.connectionHolder = connectionHolder;
	}

	/**
	 * Set new ConnectionHolder.
	 */
	protected void setConnectionHolder(ConnectionHolder connectionHolder) {
		this.connectionHolder = connectionHolder;
	}

	public ConnectionHolder getConnectionHolder() {
		return connectionHolder;
	}

	protected void setPreviousIsolationLevel(Integer previousIsolationLevel) {
		this.previousIsolationLevel = previousIsolationLevel;
	}

	public Integer getPreviousIsolationLevel() {
		return previousIsolationLevel;
	}

	public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
		this.mustRestoreAutoCommit = mustRestoreAutoCommit;
	}

	public boolean getMustRestoreAutoCommit() {
		return mustRestoreAutoCommit;
	}

}
