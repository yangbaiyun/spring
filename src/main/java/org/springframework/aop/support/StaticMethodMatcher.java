/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.aop.support;

import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;

/**
 * Convenient abstract superclas for static method matchers, which don't care
 * about arguments at runtime. 
 */
public abstract class StaticMethodMatcher implements MethodMatcher {

	public final boolean isRuntime() {
		return false;
	}

	public final boolean matches(Method m, Class targetClass, Object[] args) {
		// Should never be invoked because isRuntime() returns false
		throw new UnsupportedOperationException("Illegal MethodMatcher usage");
	}

}
