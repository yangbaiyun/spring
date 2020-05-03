package org.springframework.orm.jdo;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.JDOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;

/**
 * Base class for JdoTemplate and JdoInterceptor, defining common
 * properties like flushing behavior.
 *
 * <p>Note: With JDO, modifications to persistent objects are just possible
 * within a transaction (in contrast to Hibernate). Therefore, eager flushing
 * will just get applied when in a transaction. Furthermore, there is explicit
 * notion of flushing never, as this would not imply a performance gain due to
 * JDO's field interception mechanism that doesn't involve snapshot comparison.
 *
 * <p>Eager flushing is just available for specific JDO implementations.
 * You need to a corresponding JdoDialect to make eager flushing work.
 *
 * <p>Not intended to be used directly. See JdoTemplate and JdoInterceptor.
 *
 * @author Juergen Hoeller
 * @since 02.11.2003
 * @see JdoTemplate
 * @see JdoInterceptor
 * @see #setFlushEager
 */
public class JdoAccessor implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private PersistenceManagerFactory persistenceManagerFactory;

	private JdoDialect jdoDialect;

	private boolean flushEager = false;

	/**
	 * Set the JDO PersistenceManagerFactory that should be used to create
	 * PersistenceManagers.
	 */
	public void setPersistenceManagerFactory(PersistenceManagerFactory pmf) {
		this.persistenceManagerFactory = pmf;
	}

	/**
	 * Return the JDO PersistenceManagerFactory that should be used to create
	 * PersistenceManagers.
	 */
	public PersistenceManagerFactory getPersistenceManagerFactory() {
		return persistenceManagerFactory;
	}

	/**
	 * Set the JDO dialect to use for this accessor.
	 * <p>The dialect object can be used to retrieve the underlying JDBC
	 * connection or to eagerly flush changes to the database.
	 */
	public void setJdoDialect(JdoDialect jdoDialect) {
		this.jdoDialect = jdoDialect;
	}

	/**
	 * Return the JDO dialect to use for this accessor.
	 */
	public JdoDialect getJdoDialect() {
		return jdoDialect;
	}

	/**
	 * Set if this accessor should flush changes to the database eagerly.
	 * <p>Eager flushing leads to immediate synchronization with the database,
	 * even if in a transaction. This causes inconsistencies to show up and throw
	 * a respective exception immediately, and JDBC access code that participates
	 * in the same transaction will see the changes as the database is already
	 * aware of them then. But the drawbacks are:
	 * <ul>
	 * <li>additional communication roundtrips with the database, instead of a
	 * single batch at transaction commit;
	 * <li>the fact that an actual database rollback is needed if the Hibernate
	 * transaction rolls back (due to already submitted SQL statements).
	 * </ul>
	 */
	public void setFlushEager(boolean flushEager) {
		this.flushEager = flushEager;
	}

	/**
	 * Return if this accessor should flush changes to the database eagerly.
	 */
	public boolean isFlushEager() {
		return flushEager;
	}

	public void afterPropertiesSet() {
		if (this.persistenceManagerFactory == null) {
			throw new IllegalArgumentException("persistenceManagerFactory is required");
		}
		if (this.flushEager && this.jdoDialect == null) {
			throw new IllegalArgumentException("Cannot flush eagerly without a jdoDialect setting");
		}
	}

	/**
	 * Flush the given JDO persistence manager if necessary.
	 * @param pm the current JDO PersistenceManage
	 * @param existingTransaction if executing within an existing transaction
	 * @throws JDOException in case of JDO flushing errors
	 */
	protected void flushIfNecessary(PersistenceManager pm, boolean existingTransaction) throws JDOException {
		if (this.flushEager && this.jdoDialect != null) {
			logger.debug("Eagerly flushing JDO persistence manager");
			this.jdoDialect.flush(pm.currentTransaction());
		}
	}

}
