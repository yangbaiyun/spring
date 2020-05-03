/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.ejb.access;

import java.lang.reflect.Proxy;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.naming.NamingException;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jndi.JndiTemplate;
import org.springframework.remoting.RemoteAccessException;

/**
 * Tests Business Methods pattern
 * @author Rod Johnson
 * @since 21-May-2003
 * @version $Id: SimpleRemoteStatelessSessionProxyFactoryBeanTests.java,v 1.5 2003/12/30 01:11:54 jhoeller Exp $
 */
public class SimpleRemoteStatelessSessionProxyFactoryBeanTests extends TestCase {

	public void testInvokesMethod() throws Exception {
		final int value = 11;
		final String jndiName = "foo";
		
		MockControl ec = MockControl.createControl(MyEjb.class);
		MyEjb myEjb = (MyEjb) ec.getMock();
		myEjb.getValue();
		ec.setReturnValue(value, 1);
		ec.replay();
		
		MockControl mc = MockControl.createControl(MyHome.class);
		final MyHome home = (MyHome) mc.getMock();
		home.create();
		mc.setReturnValue(myEjb, 1);
		mc.replay();
		
		JndiTemplate jt = new JndiTemplate() {
			public Object lookup(String name) throws NamingException {
				// parameterize
				assertTrue(name.equals("java:comp/env/" + jndiName));
				return home;
			}
		};
		
		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setBusinessInterface(MyBusinessMethods.class);
		fb.setJndiTemplate(jt);
		fb.setInContainer(true);

		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyBusinessMethods mbm = (MyBusinessMethods) fb.getObject();
		assertTrue(Proxy.isProxyClass(mbm.getClass()));
		assertTrue(mbm.getValue() == value);
		mc.verify();	
		ec.verify();	
	}
	
	public void testRemoteException() throws Exception {
		final RemoteException rex = new RemoteException();
		final String jndiName = "foo";
	
		MockControl ec = MockControl.createControl(MyEjb.class);
		MyEjb myEjb = (MyEjb) ec.getMock();
		myEjb.getValue();
		ec.setThrowable(rex);
		ec.replay();
	
		MockControl mc = MockControl.createControl(MyHome.class);
		final MyHome home = (MyHome) mc.getMock();
		home.create();
		mc.setReturnValue(myEjb, 1);
		mc.replay();
	
		JndiTemplate jt = new JndiTemplate() {
			public Object lookup(String name) throws NamingException {
				// parameterize
				assertTrue(name.equals("java:comp/env/" + jndiName));
				return home;
			}
		};
	
		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setBusinessInterface(MyBusinessMethods.class);
		fb.setJndiTemplate(jt);
		fb.setInContainer(true);
	
		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyBusinessMethods mbm = (MyBusinessMethods) fb.getObject();
		assertTrue(Proxy.isProxyClass(mbm.getClass()));
		try {
			mbm.getValue();
			fail("Should've thrown remote exception");
		}
		catch (RemoteException ex) {
			assertTrue(ex == rex);
		}
		mc.verify();	
		ec.verify();	
	}
	
	public void testCreateException() throws Exception {
		final String jndiName = "foo";
	
		final CreateException cex = new CreateException();
		MockControl mc = MockControl.createControl(MyHome.class);
		final MyHome home = (MyHome) mc.getMock();
		home.create();
		mc.setThrowable(cex);
		mc.replay();
	
		JndiTemplate jt = new JndiTemplate() {
			public Object lookup(String name) throws NamingException {
				// parameterize
				assertTrue(name.equals(jndiName));
				return home;
			}
		};
	
		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setInContainer(false);	// no java:comp/env prefix
		fb.setBusinessInterface(MyBusinessMethods.class);
		assertEquals(fb.getBusinessInterface(), MyBusinessMethods.class);
		fb.setJndiTemplate(jt);
	
		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyBusinessMethods mbm = (MyBusinessMethods) fb.getObject();
		assertTrue(Proxy.isProxyClass(mbm.getClass()));
		
		try {
			mbm.getValue();
			fail("Should have failed to create EJB");
		}
		catch (RemoteException ex) {
			assertTrue(ex.getCause() == cex);
		}
		
		mc.verify();	
	}
	
	public void testCreateExceptionWithLocalBusinessInterface() throws Exception {
		final String jndiName = "foo";

		final CreateException cex = new CreateException();
		MockControl mc = MockControl.createControl(MyHome.class);
		final MyHome home = (MyHome) mc.getMock();
		home.create();
		mc.setThrowable(cex);
		mc.replay();

		JndiTemplate jt = new JndiTemplate() {
			public Object lookup(String name) throws NamingException {
				// parameterize
				assertTrue(name.equals(jndiName));
				return home;
			}
		};

		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setInContainer(false);	// no java:comp/env prefix
		fb.setBusinessInterface(MyLocalBusinessMethods.class);
		assertEquals(fb.getBusinessInterface(), MyLocalBusinessMethods.class);
		fb.setJndiTemplate(jt);

		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyLocalBusinessMethods mbm = (MyLocalBusinessMethods) fb.getObject();
		assertTrue(Proxy.isProxyClass(mbm.getClass()));

		try {
			mbm.getValue();
			fail("Should have failed to create EJB");
		}
		catch (RemoteAccessException ex) {
			assertTrue(ex.getRootCause() == cex);
		}

		mc.verify();
	}

	public void testNoBusinessInterfaceSpecified() throws Exception {
		// Will do JNDI lookup to get home but won't call create
		// Could actually try to figure out interface from create?
		final String jndiName = "foo";

		MockControl mc = MockControl.createControl(MyHome.class);
		final MyHome home = (MyHome) mc.getMock();
		mc.replay();

		JndiTemplate jt = new JndiTemplate() {
			public Object lookup(String name) throws NamingException {
				// parameterize
				assertTrue(name.equals(jndiName));
				return home;
			}
		};

		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setInContainer(false);	// no java:comp/env prefix
		// Don't set business interface
		fb.setJndiTemplate(jt);
		
		// Check it's a singleton
		assertTrue(fb.isSingleton());

		try {
			fb.afterPropertiesSet();
			fail("Should have failed to create EJB");
		}
		catch (IllegalArgumentException ex) {
			// TODO more appropriate exception?
			assertTrue(ex.getMessage().indexOf("businessInterface") != 1);
		}
	
		// Expect no methods on home
		mc.verify();	
	}
	
	
	protected static interface MyHome extends EJBHome {

		MyBusinessMethods create() throws CreateException, RemoteException;
	}


	protected static interface MyBusinessMethods  {

		int getValue() throws RemoteException;
	}


	protected static interface MyLocalBusinessMethods  {

		int getValue();
	}


	protected static interface MyEjb extends EJBObject, MyBusinessMethods {
		
	}

}
