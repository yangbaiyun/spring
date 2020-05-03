/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.aop.support;

import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

/**
 * Static methods useful for manipulating pointcuts.
 * @author Rod Johnson
 * @version $Id: Pointcuts.java,v 1.2 2003/11/21 22:45:29 jhoeller Exp $
 */
public abstract class Pointcuts {
	
	public static Pointcut union(Pointcut a, Pointcut b) {
		return new ComposablePointcut(a.getClassFilter(), a.getMethodMatcher()).union(b);
	}
	
	public static Pointcut intersection(Pointcut a, Pointcut b) {
		return new ComposablePointcut(a.getClassFilter(), a.getMethodMatcher()).intersection(b);
	}
	
	/**
	 * Perform the least expense check for a match.
	 */
	public static boolean matches(Pointcut pc, Method m, Class targetClass, Object[] arguments) {
		if (pc == Pointcut.TRUE) {
			return true;
		}
			
		if (pc.getClassFilter().matches(targetClass)) {
			// Only check if it gets past first hurdle
			MethodMatcher mm = pc.getMethodMatcher();
			if (mm.matches(m, targetClass)) { 
				// We may need additional runtime (argument) check
				return  mm.isRuntime() ? mm.matches(m, targetClass, arguments) : true;
			}
		}
		return false;
	}

	public static boolean equals(Pointcut a, Pointcut b) {
		return a.getClassFilter() == b.getClassFilter() &&
				a.getMethodMatcher() == b.getMethodMatcher();
	}

}
