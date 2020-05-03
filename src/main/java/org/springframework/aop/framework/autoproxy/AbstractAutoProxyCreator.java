package org.springframework.aop.framework.autoproxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * BeanPostProcessor implementation that wraps a group of beans with AOP proxies
 * that delegate to the given interceptors before invoking the bean itself.
 * Also supports additional specific interceptors per bean instance.
 *
 * <p>This is particularly useful if there's a large number of beans that need
 * to get wrapped with similar proxies, i.e. delegating to the same interceptors.
 * Instead of x repetitive proxy definitions for x target beans, you can register
 * one single such post processor with the bean factory to achieve the same effect.
 *
 * <p>Subclasses can apply any strategy to decide if a bean is to be proxied,
 * e.g. by type, by name, by definition details, etc. They can also return
 * additional interceptors that should just be applied to the specific bean
 * instance. The default concrete implementation is BeanNameAutoProxyCreator,
 * identifying the beans to be proxied via a list of bean names.
 * 
 * <p>Any number of TargetSourceCreator implementations can be used with
 * any subclass, to create a custom target source--for example, to pool prototype
 * objects. Autoproxying will occur even if there is no advice if a TargetSourceCreator
 * specifies a custom TargetSource.
 * If there are no TargetSourceCreators set, or if none matches, a SingletonTargetSource
 * will be used by default to wrap the bean to be autoproxied.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since October 13, 2003
 * @see #setInterceptors
 * @see BeanNameAutoProxyCreator
 * @version $Id: AbstractAutoProxyCreator.java,v 1.1 2003/12/12 16:50:43 johnsonr Exp $
 */
public abstract class AbstractAutoProxyCreator extends ProxyConfig implements BeanPostProcessor, BeanFactoryAware, Ordered {

	/**
	 * Convenience constant for subclasses: Return value for "do not proxy".
	 * @see #getInterceptorsAndAdvisorsForBean
	 */
	protected final Object[] DO_NOT_PROXY = null;

	/**
	 * Convenience constant for subclasses: Return value for
	 * "proxy without additional interceptors, just the common ones".
	 * @see #getInterceptorsAndAdvisorsForBean
	 */
	protected final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];

	protected final Log logger = LogFactory.getLog(getClass());

	private int order = Integer.MAX_VALUE;  // default: same as non-Ordered

	private Object[] interceptors;

	private boolean applyCommonInterceptorsFirst = true;
	
	private List targetSourceCreators = Collections.EMPTY_LIST;
	
	private BeanFactory owningBeanFactory;
	

	public final void setOrder(int order) {
	  this.order = order;
	}

	public final int getOrder() {
	  return order;
	}
	
	/**
	 * Set custom TargetSourceCreators, that will be applied in this order.
	 * If the list is empty, or they all return null, a SingletonTargetSource
	 * will be created.<br>
	 * TargetSourceCreators can only be invoked if this post processor is used in
	 * a BeanFactory, and its BeanFactoryAware callback is used.
	 * @param l list of TargetSourceCreator. Ordering is significant: the TargetSource
	 * returned from the first matching TargetSourceCreator (that is, the first that returns non-null)
	 * will be used.
	 */
	public void setCustomTargetSourceCreators(List l) {
		this.targetSourceCreators = l;
	}

	/**
	 * Set the MethodInterceptors, MethodBeforeAdvices and pointcuts that the automatic proxies
	 * should delegate to before invoking the bean itself.
	 */
	public void setInterceptors(Object[] interceptors) {
		this.interceptors = interceptors;
	}
	

	/**
	 * Set whether the common interceptors should be applied before
	 * bean-specific ones. Default is true; else, bean-specific
	 * interceptors will get applied first.
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}
	
	/**
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		this.owningBeanFactory = beanFactory;
	}
	
	/**
	 * Return the owning BeanFactory
	 * May be null, as this object doesn't need to belong to a bean factory
	 */
	protected BeanFactory getBeanFactory() {
		return this.owningBeanFactory;
	}


	/**
	 * Create a proxy with the configured interceptors if the bean is
	 * identified as one to proxy by the subclass.
	 * @see #getInterceptorsAndAdvisorsForBean
	 */
	public Object postProcessBean(Object bean, String name) throws BeansException {
		
		// Check for special cases. We don't want to try to autoproxy a part of the autoproxying
		// infrastructure, lest we get a stack overflow.
		if (isInfrastructureClass(bean, name) || shouldSkip(bean, name)) {
			logger.debug("Did not attempt to autoproxy infrastructure class '" + bean.getClass() + "'");
			return bean;
		}
		
		TargetSource targetSource = getTargetSource(bean, name);
		
		Object[] specificInterceptors = getInterceptorsAndAdvisorsForBean(bean, name);
		
		// Proxy if we have advice or if a TargetSourceCreator wants to do some
		// fancy stuff such as pooling
		if (specificInterceptors != null || !(targetSource instanceof SingletonTargetSource)) {
			List allInterceptors = new ArrayList();
			if (specificInterceptors != null) {
				allInterceptors.addAll(Arrays.asList(specificInterceptors));
				if (this.interceptors != null) {
					if (this.applyCommonInterceptorsFirst) {
						allInterceptors.addAll(0, Arrays.asList(this.interceptors));
					}
					else {
						allInterceptors.addAll(Arrays.asList(this.interceptors));
					}
				}
			}
			if (logger.isInfoEnabled()) {
				int nrOfCommonInterceptors = this.interceptors != null ? this.interceptors.length : 0;
				logger.info("Creating implicit proxy for bean '" +  name + "' with " + nrOfCommonInterceptors +
										" common interceptors and " + specificInterceptors.length + " specific interceptors");
			}
			ProxyFactory proxyFactory = new ProxyFactory();
			// Copy our properties (proxyTargetClass) inherited from ProxyConfig
			proxyFactory.copyFrom(this);
			
			if (!getProxyTargetClass()) {
				// Must allow for introductions; can't just set interfaces to
				// the target's interfaces only
				Class[] targetsInterfaces = AopUtils.getAllInterfaces(bean);
				for (int i = 0; i < targetsInterfaces.length; i++) {
					proxyFactory.addInterface(targetsInterfaces[i]);
				}
			}
			
			for (Iterator it = allInterceptors.iterator(); it.hasNext();) {
				Advisor advisor = GlobalAdvisorAdapterRegistry.getInstance().wrap(it.next());
				proxyFactory.addAdvisor(advisor);
			}
			proxyFactory.setTargetSource(getTargetSource(bean, name));
			
			//System.err.print(proxyFactory);
			return proxyFactory.getProxy();
		}
		else {
			return bean;
		}
	}
	
	protected boolean isInfrastructureClass(Object bean, String name) {
		return Advisor.class.isAssignableFrom(bean.getClass()) ||
			MethodInterceptor.class.isAssignableFrom(bean.getClass()) ||
			AbstractAutoProxyCreator.class.isAssignableFrom(bean.getClass());
	}
	
	/**
	 * Subclasses should override this method to return true if this
	 * bean should not be considered for autoproxying by this post processor. 
	 * Sometimes we need to be able to avoid this happening if it will lead to
	 * a circular reference. This implementation returns true.
	 */
	protected boolean shouldSkip(Object bean, String name) {
		return false;
	}

	/**
	 * Create a target source to source instances.
	 * Uses any TargetSourceCreators if set.
	 * @param bean bean to intercept
	 * @param beanName name of the bean
	 * @return an invoker interceptor wrapping this bean.
	 * This implementation returns a straight reflection InvokerInterceptor
	 */
	private TargetSource getTargetSource(Object bean, String beanName) {
		
		// We can't create fancy target sources for singletons
		if (this.owningBeanFactory != null && !owningBeanFactory.isSingleton(beanName)) {
			logger.info("Checking for custom TargetSource for bean with name '" + beanName + "'");
			for (int i = 0; i < targetSourceCreators.size(); i++) {
				TargetSourceCreator tsc = (TargetSourceCreator) targetSourceCreators.get(i);
				TargetSource ts = tsc.getTargetSource(bean, beanName, owningBeanFactory);
				if (ts != null) {
					// Found a match
					logger.info("TargetSourceCreator [" + tsc + " found custom TargetSource for bean with name '" + beanName + "'");
					return ts;
				}
			}
		}
		// Default is a simple, default target source
		return new SingletonTargetSource(bean);
	}

	/**
	 * Return whether the given bean is to be proxied,
	 * and what additional interceptors and pointcuts to apply.
	 * @param bean the new bean instance
	 * @param beanName the beanName of the bean
	 * @return an array of additional interceptors for the particular bean;
	 * or an empty array if no additional interceptors but just the common ones;
	 * or null if no proxy at all, not even with the common interceptors.
	 * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessBean
	 * @see #DO_NOT_PROXY
	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 */
	protected abstract Object[] getInterceptorsAndAdvisorsForBean(Object bean, String beanName)
	    throws BeansException;

}
