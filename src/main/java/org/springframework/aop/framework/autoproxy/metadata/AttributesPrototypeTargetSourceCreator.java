/*
 * The Spring Framework is published under the terms of the Apache Software License.
 */

package org.springframework.aop.framework.autoproxy.metadata;

import java.util.Collection;

import org.springframework.aop.framework.autoproxy.target.AbstractPrototypeTargetSourceCreator;
import org.springframework.aop.target.AbstractPrototypeTargetSource;
import org.springframework.aop.target.PrototypeTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.metadata.Attributes;

/**
 * PrototypeTargetSourceCreator driven by metadata.
 * Creates a prototype
 * only if there's a PrototypeAttribute associated with the class.
 * @author Rod Johnson
 * @version $Id: AttributesPrototypeTargetSourceCreator.java,v 1.2 2003/12/15 17:14:34 johnsonr Exp $
 */
public class AttributesPrototypeTargetSourceCreator extends AbstractPrototypeTargetSourceCreator {

	private final Attributes attributes;

	public AttributesPrototypeTargetSourceCreator(Attributes attributes) {
		this.attributes = attributes;
	}

	protected AbstractPrototypeTargetSource createPrototypeTargetSource(Object bean, String beanName, BeanFactory bf) {
		Class beanClass = bean.getClass();
		// See if there's a pooling attribute
		Collection atts = attributes.getAttributes(beanClass, PrototypeAttribute.class);
		if (atts.isEmpty()) {
			// No pooling attribute: don't create a custom TargetSource
			return null;
		}
		else {
			return new PrototypeTargetSource();
		}
	}

}