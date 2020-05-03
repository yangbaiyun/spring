/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.beans.TestBean;

/**
 * Simple factory to allow testing of FactoryBean 
 * support in AbstractBeanFactory. Depending on whether its
 * singleton property is set, it will return a singleton
 * or a prototype instance.
 * Implements InitializingBean interface, so we can check that
 * factories get this lifecycle callback if they want.
 * @author Rod Johnson
 * @since 10-Mar-2003
 * version $Id: DummyFactory.java,v 1.7 2003/12/04 18:45:15 jhoeller Exp $
 */
public class DummyFactory implements FactoryBean, BeanNameAware, BeanFactoryAware, InitializingBean {
	
	public static final String SINGLETON_NAME = "Factory singleton";

	/**
	 * Default is for factories to return a singleton instance.
	 */
	private boolean singleton = true;

	private String beanName;

	private AutowireCapableBeanFactory beanFactory;

	private boolean postProcessed;

	private boolean isInitialized;

	private static boolean prototypeCreated;

	private TestBean testBean;

	private TestBean otherTestBean;
	
	/**
	 * Clear static state
	 *
	 */
	public static void reset() {
		prototypeCreated = false;
	}

	public DummyFactory() {
		this.testBean = new TestBean();
		this.testBean.setName(SINGLETON_NAME);
		this.testBean.setAge(25);
	}

	/**
	 * Return if the bean managed by this factory is a singleton.
	 * @see FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return this.singleton;
	}

	/**
	 * Set if the bean managed by this factory is a singleton.
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
		this.beanFactory.applyBeanPostProcessors(this.testBean, this.beanName);
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setPostProcessed(boolean postProcessed) {
		this.postProcessed = postProcessed;
	}

	public boolean isPostProcessed() {
		return postProcessed;
	}

	public void setOtherTestBean(TestBean otherTestBean) {
		this.otherTestBean = otherTestBean;
	}

	public TestBean getOtherTestBean() {
		return otherTestBean;
	}

	public void afterPropertiesSet() {
		if (isInitialized)
			throw new RuntimeException("Cannot call afterPropertiesSet twice on the one bean");
		this.isInitialized = true;
	}
	
	/**
	 * Was this initialized by invocation of the
	 * afterPropertiesSet() method from the InitializingBean interface?
	 */
	public boolean wasInitialized() {
		return this.isInitialized;
	}

	public static boolean wasPrototypeCreated() {
		return prototypeCreated;
	}

	/**
	 * Return the managed object, supporting both singleton
	 * and prototype mode.
	 * @see FactoryBean#getObject()
	 */
	public Object getObject() throws BeansException {
		if (isSingleton()) {
			//System.out.println("DummyFactory returned new SINGLETON");
			return this.testBean;
		}
		else {
			//System.out.println("DummyFactory created new PROTOTYPE");
			TestBean prototype = new TestBean("prototype created at " + System.currentTimeMillis(), 11);
			//System.out.println("prot name is " + prototype.getName());
			if (this.beanFactory != null) {
				this.beanFactory.applyBeanPostProcessors(prototype, this.beanName);
			}
			prototypeCreated = true;
			return prototype;
		}
	}

	public Class getObjectType() {
		return TestBean.class;
	}

}
