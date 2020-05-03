/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;

import javax.servlet.ServletException;

import junit.framework.TestCase;

import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.metadata.support.MapAttributes;


/**
 * 
 * @author Rod Johnson
 * @version $Id: AttributesTransactionAttributeSourceTests.java,v 1.2 2003/12/17 09:54:16 johnsonr Exp $
 */
public class AttributesTransactionAttributeSourceTests extends TestCase {

	/**
	 * Constructor for AttributeRegistryTransactionAttributeSourceTests.
	 * @param arg0
	 */
	public AttributesTransactionAttributeSourceTests(String arg0) {
		super(arg0);
	}
	
	protected void setUp() {
		//Logger.getLogger(RuleBasedTransactionAttribute.class.getName()).setLevel(Level.DEBUG);
	}
	
	public void testNullOrEmpty() throws Exception {
		Method method = ITestBean.class.getMethod("getAge", null);
		
		MapAttributes mar = new MapAttributes();
		mar.register(method, null);
		AttributesTransactionAttributeSource atas = new AttributesTransactionAttributeSource(mar);
		assertNull(atas.getTransactionAttribute(method, null));
		
		mar.register(method, new Object[0]);
		assertNull(atas.getTransactionAttribute(method, null));
	}
	
	public void testSingleTransactionAttribute() throws Exception {
		Method method = ITestBean.class.getMethod("getAge", null);
	
		TransactionAttribute txAtt = new DefaultTransactionAttribute();
		
		MapAttributes ma = new MapAttributes();
		ma.register(method, new Object[] { txAtt });
		AttributesTransactionAttributeSource atas = new AttributesTransactionAttributeSource(ma);
		TransactionAttribute actual = atas.getTransactionAttribute(method, method.getDeclaringClass());
		assertEquals(txAtt, actual);
	}
	
	
	public void testTransactionAttributeAmongOthers() throws Exception {
		Method method = TestBean.class.getMethod("getAge", null);

		TransactionAttribute txAtt = new DefaultTransactionAttribute();
	
		MapAttributes ma = new MapAttributes();
		AttributesTransactionAttributeSource atas = new AttributesTransactionAttributeSource(ma);
		ma.register(method, new Object[] { new Object(), "", txAtt, "er" });
		TransactionAttribute actual = atas.getTransactionAttribute(method, method.getDeclaringClass());
		assertEquals(txAtt, actual);
	}
	
	/**
	 * Test the important case where the invocation is on a proxied interface method, but
	 * the attribute is defined on the target class
	 * @throws Exception
	 */
	public void testTransactionAttributeDeclaredOnClassMethod() throws Exception {
		Method classMethod = TestBean.class.getMethod("getAge", null);
		Method interfaceMethod = ITestBean.class.getMethod("getAge", null);

		TransactionAttribute txAtt = new DefaultTransactionAttribute();

		MapAttributes ma = new MapAttributes();
		AttributesTransactionAttributeSource atas = new AttributesTransactionAttributeSource(ma);
		ma.register(classMethod, new Object[] { new Object(), "", txAtt, "er" });
		// Target class implements ITestBean
		TransactionAttribute actual = atas.getTransactionAttribute(interfaceMethod, TestBean.class);
		assertEquals(txAtt, actual);
	}
	
	public void testTransactionAttributeDeclaredOnInterfaceMethodOnly() throws Exception {
		Method interfaceMethod = ITestBean.class.getMethod("getAge", null);

		TransactionAttribute txAtt = new DefaultTransactionAttribute();

		MapAttributes ma = new MapAttributes();
		AttributesTransactionAttributeSource atas = new AttributesTransactionAttributeSource(ma);
		ma.register(interfaceMethod, new Object[] { new Object(), "", txAtt, "er" });
		// Target class implements ITestBean
		TransactionAttribute actual = atas.getTransactionAttribute(interfaceMethod, TestBean.class);
		assertEquals(txAtt, actual);
	}
	
	public void testTransactionAttributeDeclaredOnTargetClassMethodTakesPrecedenceOverAttributeDeclaredOnInterfaceMethod() throws Exception {
		Method classMethod = TestBean.class.getMethod("getAge", null);
		Method interfaceMethod = ITestBean.class.getMethod("getAge", null);

		TransactionAttribute interfaceAtt = new DefaultTransactionAttribute();
		TransactionAttribute classAtt = new DefaultTransactionAttribute();

		MapAttributes ma = new MapAttributes();
		AttributesTransactionAttributeSource atas = new AttributesTransactionAttributeSource(ma);
		ma.register(interfaceMethod, new Object[] { new Object(), "", interfaceAtt, "er" });
		ma.register(classMethod, new Object[] { new Object(), "", classAtt, "er" });
		// Target class implements ITestBean
		TransactionAttribute actual = atas.getTransactionAttribute(interfaceMethod, TestBean.class);
		assertEquals(classAtt, actual);
	}
	
	public void testRollbackRulesAreApplied() throws Exception {
		Method method = TestBean.class.getMethod("getAge", null);

		MapAttributes ma = new MapAttributes();
		TransactionAttribute txAtt = new RuleBasedTransactionAttribute();
		RollbackRuleAttribute rr = new RollbackRuleAttribute("java.lang.Exception");
		RollbackRuleAttribute nrr = new NoRollbackRuleAttribute("ServletException");

		AttributesTransactionAttributeSource atas = new AttributesTransactionAttributeSource(ma);
		
		ma.register(method, new Object[] { new Object(), "", txAtt, rr, nrr, "er" });
		TransactionAttribute actual = atas.getTransactionAttribute(method, method.getDeclaringClass());
		assertEquals(txAtt, actual);
		assertTrue(txAtt.rollbackOn(new Exception()));
		assertFalse(txAtt.rollbackOn(new ServletException()));
	}
	
	/**
	 * Test that transaction attribute is inherited from class
	 * if not specified on method
	 * @throws Exception
	 */
	public void testDefaultsToClassTransactionAttribute() throws Exception {
		Method method = TestBean.class.getMethod("getAge", null);

		TransactionAttribute txAtt = new DefaultTransactionAttribute();
		MapAttributes ma = new MapAttributes();
		AttributesTransactionAttributeSource atas = new AttributesTransactionAttributeSource(ma);
		ma.register(TestBean.class, new Object[] { new Object(), "", txAtt, "er" });
		TransactionAttribute actual = atas.getTransactionAttribute(method, null);
		assertEquals(txAtt, actual);
	}

}
