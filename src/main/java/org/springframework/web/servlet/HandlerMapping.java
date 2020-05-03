/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface to be implemented by objects that define a mapping between
 * requests and handler objects.
 *
 * <p>This class can be implemented by application developers, although this
 * is not necessary, as BeanNameUrlHandlerMapping and SimpleUrlHandlerMapping
 * are included in the framework. The former is the default if no
 * HandlerMapping bean is registered in the application context.
 *
 * <p>HandlerMapping implementations can support mapped interceptors but do
 * not have to. A handler will always be wrapped in a HandlerExecutionChain
 * instance, optionally accompanied by some HandlerInterceptor instances.
 * The DispatcherServlet will first call each HandlerInterceptor's preHandle
 * method in the given order, finally invoking the handler itself if all
 * preHandle method have returned "true".
 *
 * <p>The ability to parameterize this mapping is a powerful and unusual
 * capability of this MVC framework. For example, it is possible to write
 * a custom mapping based on session state, cookie state or many other
 * variables. No other MVC framework seems to be equally flexible.
 *
 * <p>Note: Implementations can implement the Ordered interface to be able
 * to specify a sorting order and thus a priority for getting applied by
 * DispatcherServlet. Non-Ordered instances get treated as lowest priority.
 *
 * @author Rod Johnson
 * @see org.springframework.core.Ordered
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping
 * @see org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping
 * @see org.springframework.web.servlet.handler.SimpleUrlHandlerMapping
 */
public interface HandlerMapping {
	
	/**
	 * Return a handler and any interceptors for this request. The choice may be made
	 * on request URL, session state, or any factor the implementing class chooses.
	 * <p>This returns Object, rather than even a tag interface, so that handlers
	 * are not constrained in any way. For example, a HandlerAdapter could be
	 * written to allow another framework's handler objects to be used.
	 * <p>Returns null if no match was found. This is not an error. The
	 * DispatcherServlet will query all registered HandlerMapping beans to find
	 * a match, and only decide there is an error if none can find a handler.
	 * @param request current HTTP request
	 * @return a HandlerExecutionChain instance containing handler object and
	 * any interceptors, or null if no mapping found
	 * @throws Exception if there is an internal error
	 */
	HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;

}
