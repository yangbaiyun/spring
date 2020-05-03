/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.aop.support;

import javax.servlet.ServletException;

import junit.framework.TestCase;


/**
 * 
 * @author Rod Johnson
 * @since 23-Jul-2003
 * @version $Id: RegexpMethodPointcutTests.java,v 1.1 2003/11/16 12:54:57 johnsonr Exp $
 */
public class RegexpMethodPointcutTests extends TestCase {

	/**
	 * Constructor for RegexpMethodPointcutTests.
	 * @param arg0
	 */
	public RegexpMethodPointcutTests(String arg0) {
		super(arg0);
	}
	
	public void testExactMatch() throws Exception {
		RegexpMethodPointcut rpc = new RegexpMethodPointcut();
		rpc.setPattern("java.lang.Object.hashCode");
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", null), String.class));
	}
	
	public void testWildcard() throws Exception {
		RegexpMethodPointcut rpc = new RegexpMethodPointcut();
		rpc.setPattern(".*Object.hashCode");
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", null), Object.class));
		assertFalse(rpc.matches(Object.class.getMethod("wait", null), Object.class));
	}
	
	public void testWildcardForOneClass() throws Exception {
		RegexpMethodPointcut rpc = new RegexpMethodPointcut();
		rpc.setPattern("java.lang.Object.*");
		assertTrue(rpc.matches(Object.class.getMethod("hashCode", null), String.class));
		assertTrue(rpc.matches(Object.class.getMethod("wait", null), String.class));
	}
	
	public void testMatchesObjectClass() throws Exception {
		RegexpMethodPointcut rpc = new RegexpMethodPointcut();
		rpc.setPattern("java.lang.Object.*");
		assertTrue(rpc.matches(Exception.class.getMethod("hashCode", null), ServletException.class));
		// Doesn't match a method from Throwable
		assertFalse(rpc.matches(Exception.class.getMethod("getMessage", null), Exception.class));
	}

}
