package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.context.config.ConfigurableApplicationContext;

/**
 * Interface to be implemented by configurable web application contexts.
 * Expected by ContextLoader and FrameworkServlet.
 *
 * <p>Note: The setters of this interface need to be called before an invocation
 * of the refresh method inherited from ConfigurableApplicationContext.
 * They do not cause an initialization of the context on their own.
 *
 * @author Juergen Hoeller
 * @since 05.12.2003
 * @see #refresh
 * @see ContextLoader#createWebApplicationContext
 * @see org.springframework.web.servlet.FrameworkServlet#createWebApplicationContext
 */
public interface ConfigurableWebApplicationContext extends WebApplicationContext, ConfigurableApplicationContext {

	/**
	 * Any number of these characters are considered delimiters
	 * between multiple context paths in a single-String config location.
	 * @see ContextLoader#CONFIG_LOCATION_PARAM
	 * @see org.springframework.web.servlet.FrameworkServlet#setContextConfigLocation
	 */
	String CONFIG_LOCATION_DELIMITERS = ",; ";


	/**
	 * Set the ServletContext for this web application context.
	 * <p>Does not cause an initialization of the context: refresh needs to be
	 * called after the setting of all configuration properties.
	 * @see #refresh
	 */
	void setServletContext(ServletContext servletContext);

	/**
	 * Set the namespace for this web application context,
	 * to be used for building a default context config location.
	 * The root web application context does not have a namespace.
	 */
	void setNamespace(String namespace);

	/**
	 * Set the config locations for this web application context.
	 * If not set, the implementation is supposed to use a default for the
	 * given namespace respectively the root web application context.
	 */
	void setConfigLocations(String[] configLocations);

}
