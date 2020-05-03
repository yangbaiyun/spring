package org.springframework.orm.hibernate;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.sql.SQLException;

import junit.framework.TestCase;
import net.sf.hibernate.FlushMode;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.JDBCException;

import org.aopalliance.intercept.AttributeRegistry;
import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.Invocation;
import org.aopalliance.intercept.MethodInvocation;
import org.easymock.MockControl;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * @author Juergen Hoeller
 */
public class HibernateInterceptorTests extends TestCase {

	public void testInterceptorWithNewSession() throws HibernateException {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sf);
		try {
			interceptor.invoke(new TestInvocation(sf));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		sfControl.verify();
		sessionControl.verify();
	}

	public void testInterceptorWithNewSessionAndFlushNever() throws HibernateException {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.setFlushMode(FlushMode.NEVER);
		sessionControl.setVoidCallable(1);
		session.close();
			sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setFlushModeName("FLUSH_NEVER");
		interceptor.setSessionFactory(sf);
		try {
			interceptor.invoke(new TestInvocation(sf));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		sfControl.verify();
		sessionControl.verify();
	}

	public void testInterceptorWithPrebound() {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		sfControl.replay();
		sessionControl.replay();

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sf);
		try {
			interceptor.invoke(new TestInvocation(sf));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}

		sfControl.verify();
		sessionControl.verify();
	}

	public void testInterceptorWithPreboundAndFlushEager() throws HibernateException {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		session.flush();
		sessionControl.setVoidCallable(1);
		sfControl.replay();
		sessionControl.replay();

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setFlushMode(HibernateInterceptor.FLUSH_EAGER);
		interceptor.setSessionFactory(sf);
		try {
			interceptor.invoke(new TestInvocation(sf));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}

		sfControl.verify();
		sessionControl.verify();
	}

	public void testInterceptorWithFlushingFailure() throws Throwable {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		SQLException sqlex = new SQLException("argh", "27");
		session.flush();
		sessionControl.setThrowable(new JDBCException(sqlex), 1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sf);
		try {
			interceptor.invoke(new TestInvocation(sf));
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException ex) {
			// expected
			assertEquals(sqlex, ex.getRootCause());
		}

		sfControl.verify();
		sessionControl.verify();
	}

	public void testInterceptorWithEntityInterceptor() throws HibernateException {
		MockControl interceptorControl = MockControl.createControl(net.sf.hibernate.Interceptor.class);
		net.sf.hibernate.Interceptor entityInterceptor = (net.sf.hibernate.Interceptor) interceptorControl.getMock();
		interceptorControl.replay();
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		sf.openSession(entityInterceptor);
		sfControl.setReturnValue(session, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sf);
		interceptor.setEntityInterceptor(entityInterceptor);
		try {
			interceptor.invoke(new TestInvocation(sf));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		sfControl.verify();
		sessionControl.verify();
	}


	private static class TestInvocation implements MethodInvocation {

		private SessionFactory sessionFactory;

		public TestInvocation(SessionFactory sessionFactory) {
			this.sessionFactory = sessionFactory;
		}

		public Object proceed() throws Throwable {
			if (!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
				throw new IllegalStateException("Session not bound");
			}
			return null;
		}


		public int getCurrentInterceptorIndex() {
			return 0;
		}

		public int getNumberOfInterceptors() {
			return 0;
		}

		public Interceptor getInterceptor(int i) {
			return null;
		}

		public Method getMethod() {
			return null;
		}

		public AccessibleObject getStaticPart() {
			return null;
		}

		public Object getArgument(int i) {
			return null;
		}

		public Object[] getArguments() {
			return null;
		}

		public void setArgument(int i, Object handler) {
		}

		public int getArgumentCount() {
			return 0;
		}

		public Object getThis() {
			return null;
		}

		public Object getProxy() {
			return null;
		}

		public Object addAttachment(String msg, Object handler) {
			return null;
		}

		public Object getAttachment(String msg) {
			return null;
		}

		public Invocation cloneInstance() {
			return null;
		}

		public AttributeRegistry getAttributeRegistry() {
			return null;
		}
	}

}
