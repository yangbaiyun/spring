/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.aop.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenience superclass for configuration used in creating proxies,
 * to ensure that all proxy creators have consistent properties.
 * <br>
 * Note that it is now longer possible to configure subclasses to 
 * expose the MethodInvocation. Interceptors should normally manage their own
 * ThreadLocals if they need to make resources available to advised objects.
 * If it's absolutely necessary to expose the MethodInvocation, use an
 * interceptor to do so.
 * @author Rod Johnson
 * @version $Id: ProxyConfig.java,v 1.6 2003/12/19 10:22:02 johnsonr Exp $
 */
public class ProxyConfig {
	
	/*
	 * Note that some of the instance variables in this class and AdvisedSupport
	 * are protected, rather than private, as is usually preferred in Spring
	 * (following "Expert One-on-One J2EE Design and Development", Chapter 4).
	 * This allows direct field access in the AopProxy implementations, which
	 * produces a 10-20% reduction in AOP performance overhead compared with method
	 * access. - RJ, December 10, 2003.
	 */
	
	protected final Log logger = LogFactory.getLog(getClass());

	private boolean proxyTargetClass;
	
	private boolean optimize;
	
	/**
	 * Should proxies obtained from this configuration expose
	 * the AOP proxy for the AopContext class to retrieve for targets?
	 * The default is false, as enabling this property may
	 * impair performance.
	 */
	protected boolean exposeProxy;

	
	public ProxyConfig() {
	}

	public void copyFrom(ProxyConfig other) {
		this.optimize = other.getOptimize();
		this.proxyTargetClass = other.proxyTargetClass;
		this.exposeProxy = other.exposeProxy;
	}

	public boolean getProxyTargetClass() {
		return this.proxyTargetClass;
	}

	/**
	 * Set whether to proxy the target class directly as well as any interfaces.
	 * We can set this to true to force CGLIB proxying. Default is false
	 * @param proxyTargetClass whether to proxy the target class directly as well as any interfaces
	 */
	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}
	
	/**
	 * @return whether proxies should perform agressive optimizations.
	 */
	public boolean getOptimize() {
		return this.optimize;
	}

	/**
	 * Set whether proxies should perform agressive optimizations.
	 * The exact meaning of "agressive optimizations" will differ
	 * between proxies, but there is usually some tradeoff. 
	 * For example, optimization will usually mean that advice changes won't take
	 * effect after a proxy has been created. For this reason, optimization
	 * is disabled by default. An optimize value of true may be ignored
	 * if other settings preclude optimization: for example, if exposeProxy
	 * is set to true and that's not compatible with the optimization.
	 * <br>For example, CGLIB-enhanced proxies may optimize out.
	 * overriding methods with no advice chain. This can produce 2.5x performance
	 * improvement for methods with no advice. 
	 * <br><b>Warning:</b> Setting this to true can produce large performance
	 * gains when using CGLIB (also set proxyTargetClass to true), so it's
	 * a good setting for performance-critical proxies. However, enabling this
	 * will mean that advice cannot be changed after a proxy has been obtained
	 * from this factory.
	 * @param optimize whether to enable agressive optimizations. 
	 * Default is false.
	 */
	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}


	/**
	 * @return whether the AOP proxy will expose the AOP proxy for
	 * each invocation.
	 */
	public final boolean getExposeProxy() {
		return this.exposeProxy;
	}

	/**
	 * Set whether the proxy should be exposed by the AOP framework as a ThreadLocal for
	 * retrieval via the AopContext class. This is useful if an advised object needs
	 * to call another advised method on itself. (If it uses <code>this</code>, the invocation
	 * will not be advised).
	 * @param exposeProxy whether the proxy should be exposed. Default
	 * is false, for optimal pe3rformance.
	 */
	public final void setExposeProxy(boolean exposeProxy) {
		this.exposeProxy = exposeProxy;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("exposeProxy=" + exposeProxy + "; ");
		sb.append("enableCglibSubclassOptimizations=" + optimize + "; ");
		return sb.toString();
	}

}
