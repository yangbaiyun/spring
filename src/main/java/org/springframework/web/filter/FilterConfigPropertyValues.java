package org.springframework.web.filter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.util.StringUtils;

/**
 * PropertyValues implementation created from ServetConfig parameters.
 * This class is immutable once initialized.
 * @author Juergen Hoeller
 */
class FilterConfigPropertyValues implements PropertyValues {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * PropertyValues delegate. We use delegation rather than simply subclass
	 * MutablePropertyValues as we don't want to expose MutablePropertyValues's
	 * update methods. This class is immutable once initialized.
	 */
	private MutablePropertyValues mutablePropertyValues;

	/**
	 * Create a new FilterConfigPropertyValues object.
	 * @param config ServletConfig we'll use to take PropertyValues from
	 * @throws ServletException should never be thrown from this method
	 */
	public FilterConfigPropertyValues(FilterConfig config) throws ServletException {
		this(config, null);
	}

	/**
	 * Creates new FilterConfigPropertyValues object.
	 * @param config ServletConfig we'll use to take PropertyValues from
	 * @param requiredProperties array of property names we need, where
	 * we can't accept default values
	 * @throws ServletException if any required properties are missing
	 */
	public FilterConfigPropertyValues(FilterConfig config, List requiredProperties) throws ServletException {
		// ensure we have a deep copy
		List missingProps = (requiredProperties == null) ? new ArrayList(0) : new ArrayList(requiredProperties);

		this.mutablePropertyValues = new MutablePropertyValues();
		Enumeration enum1 = config.getInitParameterNames();
		while (enum1.hasMoreElements()) {
			String property = (String) enum1.nextElement();
			Object value = config.getInitParameter(property);
			this.mutablePropertyValues.addPropertyValue(new PropertyValue(property, value));
			missingProps.remove(property);
		}

		// fail if we are still missing properties
		if (missingProps.size() > 0) {
			throw new ServletException("Initialization from ServletConfig for servlet '" + config.getFilterName() +
																 "' failed: the following required properties were missing -- (" +
			                           StringUtils.collectionToDelimitedString(missingProps, ", ") + ")");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found PropertyValues in ServletConfig: " + this.mutablePropertyValues);
		}
	}

	public PropertyValue[] getPropertyValues() {
		return this.mutablePropertyValues.getPropertyValues();
	}

	public boolean contains(String propertyName) {
		return this.mutablePropertyValues.contains(propertyName);
	}

	public PropertyValue getPropertyValue(String propertyName) {
		return this.mutablePropertyValues.getPropertyValue(propertyName);
	}

	public PropertyValues changesSince(PropertyValues old) {
		return this.mutablePropertyValues.changesSince(old);
	}

}
