package org.springframework.orm.jdo;

import javax.jdo.JDOException;
import javax.jdo.JDOFatalUserException;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.CleanupFailureDataAccessException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Helper class featuring methods for JDO PersistenceManager handling,
 * allowing for reuse of PersistenceManager instances within transactions.
 *
 * <p>Used by JdoTemplate, JdoInterceptor, and JdoTransactionManager.
 * Can also be used directly in application code, e.g. in combination
 * with JdoInterceptor.
 *
 * @author Juergen Hoeller
 * @since 03.06.2003
 * @see JdoTemplate
 * @see JdoInterceptor
 * @see JdoTransactionManager
 */
public abstract class PersistenceManagerFactoryUtils {

	private static final Log logger = LogFactory.getLog(PersistenceManagerFactoryUtils.class);

	/**
	 * Get a JDO PersistenceManager via the given factory.
	 * Is aware of a respective PersistenceManager bound to the current thread,
	 * for example when using JdoTransactionManager.
	 * Will create a new PersistenceManager else, if allowCreate is true.
	 * @param pmf PersistenceManagerFactory to create the session with
	 * @param allowCreate if a new PersistenceManager should be created if no thread-bound found
	 * @return the PersistenceManager
	 * @throws DataAccessResourceFailureException if the PersistenceManager couldn't be created
	 * @throws IllegalStateException if no thread-bound PersistenceManager found and allowCreate false
	 */
	public static PersistenceManager getPersistenceManager(PersistenceManagerFactory pmf, boolean allowCreate)
	    throws DataAccessResourceFailureException {
		PersistenceManagerHolder pmHolder = (PersistenceManagerHolder) TransactionSynchronizationManager.getResource(pmf);
		if (pmHolder != null) {
			return pmHolder.getPersistenceManager();
		}
		if (!allowCreate) {
			throw new IllegalStateException("Not allowed to create new persistence manager");
		}
		logger.debug("Opening JDO persistence manager");
		try {
			PersistenceManager pm = pmf.getPersistenceManager();
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				logger.debug("Registering transaction synchronization for JDO persistence manager");
				// use same PersistenceManager for further JDO actions within the transaction
				// thread object will get removed by synchronization at transaction completion
				TransactionSynchronizationManager.bindResource(pmf, new PersistenceManagerHolder(pm));
				TransactionSynchronizationManager.registerSynchronization(new PersistenceManagerSynchronization(pm, pmf));
			}
			return pm;
		}
		catch (JDOException ex) {
			throw new DataAccessResourceFailureException("Cannot get JDO persistence manager", ex);
		}
	}

	/**
	 * Convert the given JDOException to an appropriate exception from the
	 * org.springframework.dao hierarchy.
	 * <p>Unfortunately, JDO's JDOUserException covers a lot of distinct causes
	 * like unparsable query, optimistic locking failure, etc. Thus, we are not able
	 * to convert to Spring's DataAccessException hierarchy in a fine-granular way.
	 * @param ex JDOException that occured
	 * @return the corresponding DataAccessException instance
	 */
	public static DataAccessException convertJdoAccessException(JDOException ex) {
		if (ex instanceof JDOUserException || ex instanceof JDOFatalUserException) {
			return new JdoUsageException(ex);
		}
		else {
			// fallback
			return new JdoSystemException(ex);
		}
	}

	/**
	 * Close the given PersistenceManager, created via the given factory,
	 * if it isn't bound to the thread.
	 * @param pm PersistenceManager to close
	 * @param pmf PersistenceManagerFactory that the PersistenceManager was created with
	 * @throws DataAccessResourceFailureException if the PersistenceManager couldn't be closed
	 */
	public static void closePersistenceManagerIfNecessary(PersistenceManager pm, PersistenceManagerFactory pmf)
	    throws CleanupFailureDataAccessException {
		if (pm == null || TransactionSynchronizationManager.hasResource(pmf)) {
			return;
		}
		logger.debug("Closing JDO persistence manager");
		try {
			pm.close();
		}
		catch (JDOException ex) {
			throw new CleanupFailureDataAccessException("Cannot close JDO persistence manager", ex);
		}
	}


	/**
	 * Callback for resource cleanup at the end of a non-JDO transaction
	 * (e.g. when participating in a JTA transaction).
	 */
	private static class PersistenceManagerSynchronization implements TransactionSynchronization {

		private PersistenceManager persistenceManager;

		private PersistenceManagerFactory persistenceManagerFactory;

		public PersistenceManagerSynchronization(PersistenceManager pm, PersistenceManagerFactory pmf) {
			this.persistenceManager = pm;
			this.persistenceManagerFactory = pmf;
		}

		public void beforeCommit() {
		}

		public void beforeCompletion() throws CleanupFailureDataAccessException {
			TransactionSynchronizationManager.unbindResource(this.persistenceManagerFactory);
			closePersistenceManagerIfNecessary(this.persistenceManager, this.persistenceManagerFactory);
		}

		public void afterCompletion(int status) {
		}
	}

}
