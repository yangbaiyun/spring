/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.web.servlet.handler.metadata;

import java.util.Collection;

import org.apache.commons.attributes.AttributeIndex;
import org.apache.commons.attributes.Attributes;
import org.springframework.context.ApplicationContextException;

/**
 * Subclass of AbstractPathMapHandlerMapping that recognizes Commons Attributes
 * metadata attributes of type PathMap on application Controllers and automatically
 * wires them into the current servlet's WebApplicationContext.
 * <p>
 * Controllers must have class attributes of the form:
 * <code>
 * &64;org.springframework.web.servlet.handler.commonsattributes.PathMap("/path.cgi")
 * </code>
 * <br>The path must be mapped to the relevant Spring DispatcherServlet in /WEB-INF/web.xml.
 * It's possible to have multiple PathMap attributes on the one controller class.
 * <p>To use this feature, you must compile application classes with Commons Attributes,
 * and run the Commons Attributes indexer tool on your application classes, which must
 * be in a Jar rather than in WEB-INF/classes.
 * <p>Controllers instantiated by this class may have dependencies on middle tier
 * objects, expressed via JavaBean properties or constructor arguments. These will
 * be resolved automatically.
 * <p>You will normally use this HandlerMapping with at most one DispatcherServlet in your web 
 * application. Otherwise you'll end with one instance of the mapped controller for
 * each DispatcherServlet's context. You <i>might</i> want this--for example, if
 * one's using a .pdf mapping and a PDF view, and another a JSP view, or if
 * using different middle tier objects, but should understand the implications. All
 * Controllers with attributes will be picked up by each DispatcherServlet's context.
 * @author Rod Johnson
 * @version $Id: CommonsPathMapHandlerMapping.java,v 1.1 2003/12/25 08:56:12 johnsonr Exp $
 */
public class CommonsPathMapHandlerMapping extends AbstractPathMapHandlerMapping {
	
	/**
	 * Use Commons Attributes AttributeIndex to get a Collection of FQNs of
	 * classes with the required PathMap attribute. Protected so that it can
	 * be overridden during testing.
	 */
	protected Collection getClassNamesWithPathMapAttributes() {
		try {
			AttributeIndex ai = new AttributeIndex(getClass().getClassLoader());
			return ai.getClassesWithAttribute(PathMap.class);
		}
		catch (Exception ex) {
			throw new ApplicationContextException("Failed to load Commons Attributes attribute index", ex);
		}
	}
	
	/**
	 * Use Commons Attributes to find PathMap attributes for the given class.
	 * We know there's at least one, as the getClassNamesWithPathMapAttributes
	 * method return this class name.
	 */
	protected PathMap[] getPathMapAttributes(Class handlerClass) {
		Collection atts =  Attributes.getAttributes(handlerClass, PathMap.class);
		return (PathMap[]) atts.toArray(new PathMap[atts.size()]);
	}

}