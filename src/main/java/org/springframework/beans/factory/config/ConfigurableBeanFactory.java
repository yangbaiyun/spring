package org.springframework.beans.factory.config;

import java.beans.PropertyEditor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * SPI interface to be implemented by most if not all bean factories.
 * Provides means to configure a bean factory in addition to the bean
 * factory client methods in the BeanFactory interface.
 *
 * <p>Allows for framework-internal plug'n'play even when needing access
 * to bean factory configuration methods.
 *
 * @author Juergen Hoeller
 * @since 03.11.2003
 * @see ConfigurableListableBeanFactory
 * @version $Id: ConfigurableBeanFactory.java,v 1.7 2003/12/24 14:48:11 johnsonr Exp $
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory {

	/**
	 * Set the parent of this bean factory.
	 * <p>Note that the parent shouldn't be changed: It should only be set outside
	 * a constructor if it isn't available when an object of this class is created.
	 * @param parentBeanFactory the parent bean factory
	 */
	void setParentBeanFactory(BeanFactory parentBeanFactory);

	/**
	 * Register the given custom property editor for all properties
	 * of the given type.
	 * @param requiredType type of the property
	 * @param propertyEditor editor to register
	 */
	void registerCustomEditor(Class requiredType, PropertyEditor propertyEditor);

	/**
	 * Add a new BeanPostPrcoessor that will get applied to beans
	 * created with this factory.
	 */
	void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

	/**
	 * Ignore the given dependency type for autowiring.
	 * <p>This will typically be used for dependencies that are resolved
	 * in other ways, like BeanFactory through BeanFactoryAware or
	 * ApplicationContext through ApplicationContextAware.
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.ApplicationContextAware
	 */
	void ignoreDependencyType(Class type);

	/**
	 * Return the registered PropertyValues for the given bean.
	 * @param beanName name of the bean
	 * @return the registered PropertyValues
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 */
	PropertyValues getPropertyValues(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Register property value for a specific bean, overriding an existing value.
	 * If no previous value exists, a new one will be added.
	 * <p>This is intended for bean factory post processing, i.e. overriding
	 * certain property values after parsing the original bean definitions.
	 * @param beanName name of the bean
	 * @param pv property name and value
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @throws BeansException if the property values of the specified bean are immutable
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
	 */
	void overridePropertyValue(String beanName, PropertyValue pv) throws BeansException;

	/**
	 * Given a bean name, create an alias. We typically use this method to
	 * support names that are illegal within XML ids (used for bean names).
	 * @param beanName name of the bean
	 * @param alias alias that will behave the same as the bean name
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @throws BeansException if the alias is already in use
	 */
	void registerAlias(String beanName, String alias) throws BeansException;

	/**
	 * Register the given existing object as singleton in the bean factory,
	 * under the given bean name.
	 * @param beanName name of the bean
	 * @param singletonObject the existing object
	 * @throws BeansException if the singleton could not be registered
	 */
	void registerSingleton(String beanName, Object singletonObject) throws BeansException;

	/**
	 * Destroy all cached singletons in this factory.
	 * To be called on shutdown of a factory.
	 */
	void destroySingletons();

}
