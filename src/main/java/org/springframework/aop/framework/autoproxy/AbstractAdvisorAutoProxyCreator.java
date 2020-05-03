/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.aop.framework.autoproxy;

import java.util.LinkedList;
import java.util.List;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.ControlFlow;
import org.springframework.util.ControlFlowFactory;

/**
 * Abstract BeanPostProcessor implementation that creates AOP proxies. 
 * This class is completely generic; it contains
 * no special code to handle any particular aspects, such as pooling aspects.
 * <br>
 * Subclasses must implement the abstract findCandidateAdvisors() method to
 * return a list of Advisors applying to any object. Subclasses can also
 * override the inherited shouldSkip() method to exclude certain objects
 * from autoproxying, but they must be careful to invoke the shouldSkip()
 * method of this class, which tries to avoid circular reference problems
 * and infinite loops.
 * @author Rod Johnson
 * @version $Id: AbstractAdvisorAutoProxyCreator.java,v 1.3 2003/12/30 01:07:11 jhoeller Exp $
 */
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

	public final static int UNORDERED = 100;
	
	/**
	 * Find all candidate advices to use in auto proxying.
	 * @return list of Advisor
	 */
	protected abstract List findCandidateAdvisors();

	/**
	 * Find all eligible advices and for autoproxying this class.
	 * @return the empty list, not null, if there are no pointcuts or interceptors
	 */
	protected List findEligibleAdvisors(Class clazz) {
		List candidateAdvice = findCandidateAdvisors();
		List eligibleAdvice = new LinkedList();
		
		for (int i = 0; i < candidateAdvice.size(); i++) {
			// Sun, give me generics, please!
			Advisor candidate = (Advisor) candidateAdvice.get(i);
			if (AopUtils.canApply(candidate, clazz, null)) {
				eligibleAdvice.add(candidate);
				logger.info("Candidate Advice [" + candidate + "] ACCEPTED for class '" + clazz.getName() + "'");
			}
			else {
				logger.info("Candidate Advice [" + candidate + "] REJECTED for class '" + clazz.getName() + "'");
			}
		}
		
		return eligibleAdvice;
	}

	protected Object[] getInterceptorsAndAdvisorsForBean(Object bean, String name) {
		//if (logger.isDebugEnabled())
		//	logger.debug("getInterceptorsAndAdvicesForBean with name '" + name + "'; singleton=" + this.owningFactory.isSingleton(name));
		List advices = findEligibleAdvisors(bean.getClass());
		if (advices.isEmpty()) {
			return DO_NOT_PROXY;
		}
		advices = sortAdvisors(advices);
		return advices.toArray();
	}

	/**
	 * Sort based on ordering.
	 */
	protected List sortAdvisors(List l) {
		// TODO implement this
		//		Ordering:
		// PERFMON?
		// 1. tx interceptor - or weightings for pointcuts?
		// 2. tx 
		// 3. custom

		return l;
	}

	/**
	 * We override this method to ensure that all candidate advisors are materialized
	 * under a stack trace including this bean. Otherwise, the dependencies won't
	 * be apparent to the circular-reference prevention strategy in AbstractBeanFactory.
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		findCandidateAdvisors();
	}

	/**
	 * We override this to ensure that we don't get into circular reference hell
	 * when our own infrastructure (such as this class) depends on advisors that depend
	 * on beans... We use a ControlFlow object to check that we didn't arrived at this
	 * call via this classes findCandidateAdvisors() method.
	 * @see org.springframework.util.ControlFlow
	 */
	protected boolean shouldSkip(Object bean, String name) {
		// TODO consider pulling this test into AbstractBeanFactory.applyPostProcessors(),
		// to protect all PostProcessors
		ControlFlow cflow = ControlFlowFactory.getInstance().createControlFlow();
		if (cflow.under(getClass(), "findCandidateAdvisors")) {
			//System.err.println("Skipping " + name + ": " + cflow);
			return true;
		}
		else {
			return false;
		}
	}

}
