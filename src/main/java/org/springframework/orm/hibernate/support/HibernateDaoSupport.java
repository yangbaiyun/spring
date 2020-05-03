package org.springframework.orm.hibernate.support;

import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.CleanupFailureDataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate.HibernateTemplate;
import org.springframework.orm.hibernate.SessionFactoryUtils;

/**
 * Convenient super class for Hibernate data access objects.
 *
 * <p>Requires a SessionFactory to be set, providing a HibernateTemplate
 * based on it to subclasses. Can alternatively be initialized directly via a
 * HibernateTemplate, to reuse the latter's settings like SessionFactory,
 * flush mode, exception translator, etc.
 *
 * <p>This base class is mainly intended for HibernateTemplate usage
 * but can also be used when working with SessionFactoryUtils directly,
 * e.g. in combination with HibernateInterceptor-managed Sessions.
 * Convenience getSession and closeSessionIfNecessary methods are provided
 * for that usage.
 *
 * @author Juergen Hoeller
 * @since 28.07.2003
 * @see #setSessionFactory
 * @see #setHibernateTemplate
 * @see org.springframework.orm.hibernate.HibernateTemplate
 * @see org.springframework.orm.hibernate.HibernateInterceptor
 */
public abstract class HibernateDaoSupport implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private HibernateTemplate hibernateTemplate;

	/**
	 * Set the Hibernate SessionFactory to be used by this DAO.
	 */
	public final void setSessionFactory(SessionFactory sessionFactory) {
	  this.hibernateTemplate = new HibernateTemplate(sessionFactory);
	}

	/**
	 * Return the Hibernate SessionFactory used by this DAO.
	 */
	protected final SessionFactory getSessionFactory() {
		return hibernateTemplate.getSessionFactory();
	}

	/**
	 * Set the HibernateTemplate for this DAO explicitly,
	 * as an alternative to specifying a SessionFactory.
	 */
	public final void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * Return the HibernateTemplate for this DAO,
	 * pre-initialized with the SessionFactory or set explicitly.
	 */
	protected final HibernateTemplate getHibernateTemplate() {
	  return hibernateTemplate;
	}

	public final void afterPropertiesSet() throws Exception {
		if (this.hibernateTemplate == null) {
			throw new IllegalArgumentException("sessionFactory or hibernateTemplate is required");
		}
		initDao();
	}

	/**
	 * Subclasses can override this for custom initialization behavior.
	 * Gets called after population of this instance's bean properties.
	 * @throws Exception if initialization fails
	 */
	protected void initDao() throws Exception {
	}

	/**
	 * Get a Hibernate Session, either from the current transaction or
	 * a new one. The latter is only allowed if the "allowCreate" setting
	 * of this bean's HibernateTemplate is true.
	 * @return the Hibernate Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and allowCreate false
	 * @see org.springframework.orm.hibernate.HibernateTemplate
	 */
	protected final Session getSession()
	    throws DataAccessResourceFailureException, IllegalStateException {
		return getSession(this.hibernateTemplate.isAllowCreate());
	}

	/**
	 * Get a Hibernate Session, either from the current transaction or
	 * a new one. The latter is only allowed if "allowCreate" is true.
	 * @param allowCreate if a new Session should be created if no thread-bound found
	 * @return the Hibernate Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and allowCreate false
	 * @see org.springframework.orm.hibernate.SessionFactoryUtils#getSession(SessionFactory, boolean)
	 */
	protected final Session getSession(boolean allowCreate)
	    throws DataAccessResourceFailureException, IllegalStateException {
		return (!allowCreate ?
		    SessionFactoryUtils.getSession(getSessionFactory(), false) :
				SessionFactoryUtils.getSession(getSessionFactory(),
				                               this.hibernateTemplate.getEntityInterceptor(),
																			 this.hibernateTemplate.getJdbcExceptionTranslator()));
	}

	/**
	 * Close the given Hibernate Session if necessary, created via this bean's
	 * SessionFactory, if it isn't bound to the thread.
	 * @param session Session to close
	 * @throws DataAccessResourceFailureException if the Session couldn't be closed
	 * @see org.springframework.orm.hibernate.SessionFactoryUtils#closeSessionIfNecessary
	 */
	protected final void closeSessionIfNecessary(Session session)
	    throws CleanupFailureDataAccessException {
		SessionFactoryUtils.closeSessionIfNecessary(session, getSessionFactory());
	}

}
