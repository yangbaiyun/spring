/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.transaction;

import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * 
 * @author Rod Johnson
 * @version $Id: CountingTxManager.java,v 1.1 2003/12/02 16:27:31 johnsonr Exp $
 */
public class CountingTxManager extends AbstractPlatformTransactionManager {
	
	public int commits;
	public int rollbacks;
	public int inflight;
	private boolean rollbackOnly;

	/**
	 * @see AbstractPlatformTransactionManager#doGetTransaction()
	 */
	protected Object doGetTransaction() throws TransactionException {
		return new Object();
	}

	/**
	 * @see AbstractPlatformTransactionManager#isExistingTransaction(Object)
	 */
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		return false;
	}

	/**
	 * @see AbstractPlatformTransactionManager#doBegin(Object, TransactionDefinition)
	 */
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		++inflight;
	}

	/**
	 * @see AbstractPlatformTransactionManager#doCommit(TransactionStatus)
	 */
	protected void doCommit(TransactionStatus status) throws TransactionException {
		++commits;
		--inflight;
	}

	/**
	 * @see AbstractPlatformTransactionManager#doRollback(TransactionStatus)
	 */
	protected void doRollback(TransactionStatus status) throws TransactionException {
		++rollbacks;
		--inflight;
	}

	/**
	 * @see AbstractPlatformTransactionManager#doSetRollbackOnly(TransactionStatus)
	 */
	protected void doSetRollbackOnly(TransactionStatus status) throws TransactionException {
		rollbackOnly = true;
	}

}
