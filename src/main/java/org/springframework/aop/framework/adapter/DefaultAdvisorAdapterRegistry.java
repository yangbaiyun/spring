/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.aop.framework.adapter;

import java.util.LinkedList;
import java.util.List;

import org.aopalliance.intercept.Interceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.InterceptionAroundAdvisor;
import org.springframework.aop.support.DefaultInterceptionAroundAdvisor;

/**
 * @author Rod Johnson
 * @version $Id: DefaultAdvisorAdapterRegistry.java,v 1.2 2003/12/30 01:07:11 jhoeller Exp $
 */
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry {
	
	private List adapters = new LinkedList();
	
	public DefaultAdvisorAdapterRegistry() {
		// Register well-known adapters
		registerAdvisorAdapter(new BeforeAdviceAdapter());
		registerAdvisorAdapter(new ThrowsAdviceAdapter());
	}

	public Advisor wrap(Object advice) throws UnknownAdviceTypeException {
		if (advice instanceof Advisor)
			return (Advisor) advice;
		if (advice instanceof Interceptor)
			return new DefaultInterceptionAroundAdvisor((Interceptor) advice);
		
		for (int i = 0; i < adapters.size(); i++) {
			AdvisorAdapter adapter = (AdvisorAdapter) adapters.get(i);
			if (adapter.supportsAdvice(advice)) {
				return adapter.wrap(advice);
			}
		}
		throw new UnknownAdviceTypeException(advice);
	}

	public Interceptor getInterceptor(Advisor advisor) throws UnknownAdviceTypeException {
		if (advisor instanceof InterceptionAroundAdvisor)
			return ((InterceptionAroundAdvisor) advisor).getInterceptor();
		
		for (int i = 0; i < adapters.size(); i++) {
			AdvisorAdapter adapter = (AdvisorAdapter) adapters.get(i);
			if (adapter.supportsAdvisor(advisor)) {
				return adapter.getInterceptor(advisor);
			}
		}
		throw new UnknownAdviceTypeException(advisor);
	}

	public void registerAdvisorAdapter(AdvisorAdapter adapter) {
		adapters.add(adapter);
	}

}
