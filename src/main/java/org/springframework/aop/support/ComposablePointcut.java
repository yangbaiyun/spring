/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.aop.support;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

/**
 * Convenient class for building up pointcuts.
 * @author Rod Johnson
 * @since 11-Nov-2003
 * @version $Id: ComposablePointcut.java,v 1.2 2003/11/21 22:45:29 jhoeller Exp $
 */
public class ComposablePointcut implements Pointcut {
	
	private ClassFilter classFilter;
	
	private MethodMatcher methodMatcher;
	
	public ComposablePointcut() {
		this.classFilter =  ClassFilter.TRUE;
		this.methodMatcher = MethodMatcher.TRUE;
	}
	
	public ComposablePointcut(ClassFilter classFilter, MethodMatcher methodMatcher) {
		this.classFilter = classFilter;
		this.methodMatcher = methodMatcher;
	}
	
	public ComposablePointcut union(ClassFilter filter) {
		this.classFilter = ClassFilters.union(this.classFilter, filter);
		return this;
	}
	
	public ComposablePointcut intersection(ClassFilter filter) {
		this.classFilter = ClassFilters.intersection(this.classFilter, filter);
		return this;
	}

	public ComposablePointcut union(MethodMatcher mm) {
		this.methodMatcher = MethodMatchers.union(this.methodMatcher, mm);
		return this;
	}

	public ComposablePointcut intersection(MethodMatcher mm) {
		this.methodMatcher = MethodMatchers.intersection(this.methodMatcher, mm);
		return this;
	}
	
	public ComposablePointcut union(Pointcut other) {
		this.classFilter = ClassFilters.union(this.classFilter, other.getClassFilter());
		this.methodMatcher = MethodMatchers.union(this.methodMatcher, other.getMethodMatcher());
		return this;
	}
	
	public ComposablePointcut intersection(Pointcut other) {
		// TODO vertical composition issue
		this.classFilter = ClassFilters.intersection(this.classFilter, other.getClassFilter());
		this.methodMatcher = MethodMatchers.intersection(this.methodMatcher, other.getMethodMatcher());
		return this;
	}

	public ClassFilter getClassFilter() {
		return this.classFilter;
	}

	public MethodMatcher getMethodMatcher() {
		return this.methodMatcher;
	}

}
