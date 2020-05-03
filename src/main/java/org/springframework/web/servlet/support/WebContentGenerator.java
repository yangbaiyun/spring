package org.springframework.web.servlet.support;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.context.support.WebApplicationObjectSupport;

/**
 * Convenient superclass for any kind of web content generator,
 * like AbstractController and WebContentInterceptor.
 *
 * <p>Supports HTTP cache control options. The usage of respective
 * HTTP headers can be determined via the "useExpiresHeader" and
 * "userCacheControlHeader" properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setUseExpiresHeader
 * @see #setUseCacheControlHeader
 */
public class WebContentGenerator extends WebApplicationObjectSupport {

	public static final String METHOD_GET = "GET";

	public static final String METHOD_POST = "POST";

	public static final String HEADER_PRAGMA = "Pragma";

	public static final String HEADER_EXPIRES = "Expires";

	public static final String HEADER_CACHE_CONTROL = "Cache-Control";


	/** Set of supported methods (GET, POST, etc). GET and POST by default. */
	private Set	supportedMethods;

	private boolean requireSession = false;

	/** Use HTTP 1.0 expires header? */
	private boolean useExpiresHeader = true;

	/** Use HTTP 1.1 cache-control header? */
	private boolean useCacheControlHeader = true;

	private int cacheSeconds = -1;


	/**
	 * Create a new Controller supporting GET and POST methods.
	 */
	public WebContentGenerator() {
		this.supportedMethods = new HashSet();
		this.supportedMethods.add(METHOD_GET);
		this.supportedMethods.add(METHOD_POST);
	}

	/**
	 * Set the HTTP methods that this controller should support.
	 * Default is GET and POST.
	 */
	public final void setSupportedMethods(String[] supportedMethodsArray) {
		if (supportedMethodsArray == null || supportedMethodsArray.length == 0) {
			throw new IllegalArgumentException("supportedMethods must not be empty");
		}
		this.supportedMethods.clear();
		for (int i = 0; i < supportedMethodsArray.length; i++) {
			this.supportedMethods.add(supportedMethodsArray[i]);
		}
	}

	/**
	 * Set if a session should be required to handle requests.
	 */
	public final void setRequireSession(boolean requireSession) {
		this.requireSession = requireSession;
	}

	/**
	 * Set whether to use the HTTP 1.0 expires header. Default is true.
	 * <p>Note: Cache headers will only get applied if caching is enabled
	 * for the current request.
	 */
	public final void setUseExpiresHeader(boolean useExpiresHeader) {
		this.useExpiresHeader = useExpiresHeader;
	}

	/**
	 * Set whether to use the HTTP 1.1 cache-control header. Default is true.
	 * <p>Note: Cache headers will only get applied if caching is enabled
	 * for the current request.
	 */
	public final void setUseCacheControlHeader(boolean useCacheControlHeader) {
		this.useCacheControlHeader = useCacheControlHeader;
	}

	/**
	 * If 0 disable caching, default is no caching header generation.
	 * Only if this is set to 0 (no cache) or a positive value (cache for this many
	 * seconds) will this class generate cache headers.
	 * They can be overwritten by subclasses anyway, before content is generated.
	 */
	public final void setCacheSeconds(int seconds) {
		this.cacheSeconds = seconds;
	}


	/**
	 * Check and prepare the given request and response according to the settings
	 * of this generator. Checks for supported methods and a required session,
	 * and applies the specified number of cache seconds.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param lastModified if the mapped handler provides Last-Modified support
	 * @throws ServletException if the request cannot be handled because a check failed
	 */
	protected final void checkAndPrepare(HttpServletRequest request, HttpServletResponse response,
	                                     boolean lastModified)
	    throws ServletException {

		// check whether we should support the request method
		String method = request.getMethod();
		if (!this.supportedMethods.contains(method)) {
			logger.info("Disallowed '" + method + "' request");
			throw new ServletException("This resource does not support request method '" + method + "'");
		}

		// check whether session was required
		HttpSession session = request.getSession(false);
		if (this.requireSession && session == null) {
			throw new ServletException("This resource requires a pre-existing HttpSession: none was found");
		}

		// do declarative cache control
		// revalidate if the controller supports last-modified
		applyCacheSeconds(response, this.cacheSeconds, lastModified);
	}

	/**
	 * Prevent the response being cached.
	 * See www.mnot.net.cache docs.
	 */
	protected final void preventCaching(HttpServletResponse response) {
		response.setHeader(HEADER_PRAGMA, "No-cache");
		if (this.useExpiresHeader) {
			// HTTP 1.0 header
			response.setDateHeader(HEADER_EXPIRES, 1L);
		}
		if (this.useCacheControlHeader) {
			// HTTP 1.1 header
			response.setHeader(HEADER_CACHE_CONTROL, "no-cache");
		}
	}

	/**
	 * Set HTTP headers to allow caching for the given number of seconds.
	 * Does not tell the browser to revalidate the resource.
	 * @param response current HTTP response
	 * @param seconds number of seconds into the future that the response
	 * should be cacheable for
	 * @see #cacheForSeconds(javax.servlet.http.HttpServletResponse, int, boolean)
	 */
	protected final void cacheForSeconds(HttpServletResponse response, int seconds) {
		cacheForSeconds(response, seconds, false);
	}

	/**
	 * Set HTTP headers to allow caching for the given number of seconds.
	 * Tells the browser to revalidate the resource if mustRevalidate is true.
	 * @param response current HTTP response
	 * @param seconds number of seconds into the future that the response
	 * should be cacheable for
	 * @param mustRevalidate whether the client should revalidate the resource
	 * (typically only necessary for controllers with last-modified support)
	 */
	protected final void cacheForSeconds(HttpServletResponse response, int seconds, boolean mustRevalidate) {
		if (this.useExpiresHeader) {
			// HTTP 1.0 header
			response.setDateHeader(HEADER_EXPIRES, System.currentTimeMillis() + seconds * 1000L);
		}
		if (this.useCacheControlHeader) {
			// HTTP 1.1 header
			String hval = "max-age=" + seconds;
			if (mustRevalidate) {
				hval += ", must-revalidate";
			}
			response.setHeader(HEADER_CACHE_CONTROL, hval);
		}
	}

	/**
	 * Apply the given cache seconds and generate respective HTTP headers,
	 * i.e. allow caching for the given number of seconds in case of a positive
	 * value, prevent caching if given a 0 value, do nothing else.
	 * Does not tell the browser to revalidate the resource.
	 * @param response current HTTP response
	 * @param seconds positive number of seconds into the future that the
	 * response should be cacheable for, 0 to prevent caching,
	 * @see #cacheForSeconds(javax.servlet.http.HttpServletResponse, int, boolean)
	 */
	protected final void applyCacheSeconds(HttpServletResponse response, int seconds) {
		applyCacheSeconds(response, seconds, false);
	}

	/**
	 * Apply the given cache seconds and generate respective HTTP headers,
	 * i.e. allow caching for the given number of seconds in case of a positive
	 * value, prevent caching if given a 0 value, do nothing else.
	 * @param response current HTTP response
	 * @param seconds number of seconds into the future that the response
	 * should be cacheable for
	 * @param mustRevalidate whether the client should revalidate the resource
	 * (typically only necessary for controllers with last-modified support)
	 */
	protected final void applyCacheSeconds(HttpServletResponse response, int seconds, boolean mustRevalidate) {
		if (seconds > 0) {
			cacheForSeconds(response, seconds, mustRevalidate);
		}
		else if (seconds == 0) {
			preventCaching(response);
		}
		// leave caching to the client otherwise
	}

}
