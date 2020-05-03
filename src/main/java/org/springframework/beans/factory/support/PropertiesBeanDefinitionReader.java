package org.springframework.beans.factory.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.Resource;

/**
 * Bean definition reader for a simple properties format.
 * Provides bean definition registration methods for Map/Properties and
 * ResourceBundle. Typically applied to a DefaultListableBeanFactory.
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 26.11.2003
 * @see DefaultListableBeanFactory
 */
public class PropertiesBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * Value of a T/F attribute that represents true.
	 * Anything else represents false. Case seNsItive.
	 */
	public static final String TRUE_VALUE = "true";

	/**
	 * Separator between bean name and property name.
	 * We follow normal Java conventions.
	 */
	public static final String SEPARATOR = ".";

	/**
	 * Prefix for the class property of a root bean definition.
	 */
	public static final String CLASS_KEY = "class";

	/**
	 * Special string added to distinguish owner.(singleton)=true
	 * Default is true.
	 */
	public static final String SINGLETON_KEY = "(singleton)";

	/**
	 * Special string added to distinguish owner.(lazy-init)=true
	 * Default is false.
	 */
	public static final String LAZY_INIT_KEY = "(lazy-init)";

	/**
	 * Reserved "property" to indicate the parent of a child bean definition.
	 */
	public static final String PARENT_KEY = "parent";

	/**
	 * Property suffix for references to other beans in the current
	 * BeanFactory: e.g. owner.dog(ref)=fido.
	 * Whether this is a reference to a singleton or a prototype
	 * will depend on the definition of the target bean.
	 */
	public static final String REF_SUFFIX = "(ref)";

	/**
	 * Prefix before values referencing other beans.
	 */
	public static final String REF_PREFIX = "*";


	protected final Log logger = LogFactory.getLog(getClass());

	/** Name of default parent bean */
	private String defaultParentBean;


	/**
	 * Create new PropertiesBeanDefinitionReader for the given bean factory.
	 */
	public PropertiesBeanDefinitionReader(BeanDefinitionRegistry beanFactory) {
		super(beanFactory);
	}

	/**
	 * Set the default parent bean for this bean factory.
	 * If a child bean definition (i.e. a definition without class
	 * attribute) handled by this factory doesn't provide a parent
	 * attribute, this default value gets used.
	 * <p>Can be used e.g. for view definition files, to define a
	 * parent with common attributes for all views.
	 */
	public void setDefaultParentBean(String defaultParentBean) {
		this.defaultParentBean = defaultParentBean;
	}

	/**
	 * Return the default parent bean for this bean factory.
	 */
	public String getDefaultParentBean() {
		return defaultParentBean;
	}


	/**
	 * Load bean definitions from the specified properties file,
	 * using all property keys (i.e. not filtering by prefix).
	 * @param resource the resource descriptor for the properties file
	 * @return the number of bean definitions found
	 * @throws BeansException in case of loading or parsing errors
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource, String)
	 */
	public int loadBeanDefinitions(Resource resource) {
		return loadBeanDefinitions(resource, null);
	}

	/**
	 * Load bean definitions from the specified properties file.
	 * @param resource the resource descriptor for the properties file
	 * @return the number of bean definitions found
	 * @throws BeansException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(Resource resource, String prefix) {
		Properties props = new Properties();
		try {
			InputStream is = resource.getInputStream();
			try {
				props.load(is);
			}
			finally {
				is.close();
			}
			return registerBeanDefinitions(props, prefix);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("IOException parsing properties from " + resource, ex);
		}
	}

	/**
	 * Register bean definitions contained in a resource bundle,
	 * using all property keys (i.e. not filtering by prefix).
	 * @return the number of bean definitions found
	 * @throws BeansException in case of loading or parsing errors
	 * @see #registerBeanDefinitions(java.util.ResourceBundle, String)
	 */
	public int registerBeanDefinitions(ResourceBundle rb) throws BeanDefinitionStoreException {
		return registerBeanDefinitions(rb, null);
	}

	/**
	 * Register bean definitions contained in a ResourceBundle.
	 * <p>Similar syntax as for a Map. This method is useful to enable
	 * standard Java internationalization support.
	 * @return the number of bean definitions found
	 * @throws BeansException in case of loading or parsing errors
	 */
	public int registerBeanDefinitions(ResourceBundle rb, String prefix) throws BeanDefinitionStoreException {
		// Simply create a map and call overloaded method
		Map m = new HashMap();
		Enumeration keys = rb.getKeys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			m.put(key, rb.getObject(key));
		}
		return registerBeanDefinitions(m, prefix);
	}

	/**
	 * Register bean definitions contained in a Map,
	 * using all property keys (i.e. not filtering by prefix).
	 * @return the number of bean definitions found
	 * @throws BeansException in case of loading or parsing errors
	 * @see #registerBeanDefinitions(java.util.Map, String)
	 */
	public int registerBeanDefinitions(Map m) throws BeansException {
		return registerBeanDefinitions(m, null);
	}

	/**
	 * Register bean definitions contained in a Map.
	 * Ignore ineligible properties.
	 * @param m Map name -> property (String or Object). Property values
	 * will be strings if coming from a Properties file etc. Property names
	 * (keys) <b>must</b> be strings. Class keys must be Strings.
	 * <code>
	 * employee.class=MyClass              // special property
	 * //employee.abstract=true              // this prototype can't be instantiated directly
	 * employee.group=Insurance Services   // real property
	 * employee.usesDialUp=false           // default unless overriden
	 *
	 * employee.manager(ref)=tony		   // reference to another prototype defined in the same file
	 *									   // cyclic and unresolved references will be detected
	 * salesrep.parent=employee
	 * salesrep.department=Sales and Marketing
	 *
	 * techie.parent=employee
	 * techie.department=Software Engineering
	 * techie.usesDialUp=true              // overridden property
	 * </code>
	 * @param prefix The match or filter within the keys in the map: e.g. 'beans.'
	 * @return the number of bean definitions found
	 * @throws BeansException in case of loading or parsing errors
	 */
	public int registerBeanDefinitions(Map m, String prefix) throws BeansException {
		if (prefix == null) {
			prefix = "";
		}
		int beanCount = 0;

		Set keys = m.keySet();
		Iterator itr = keys.iterator();
		while (itr.hasNext()) {
			String key = (String) itr.next();
			if (key.startsWith(prefix)) {
				// Key is of form prefix<name>.property
				String nameAndProperty = key.substring(prefix.length());
				int sepIndx = nameAndProperty.indexOf(SEPARATOR);
				if (sepIndx != -1) {
					String beanName = nameAndProperty.substring(0, sepIndx);
					logger.debug("Found bean name '" + beanName + "'");
					if (!getBeanFactory().containsBeanDefinition(beanName)) {
						// If we haven't already registered it...
						registerBeanDefinition(beanName, m, prefix + beanName);
						++beanCount;
					}
				}
				else {
					// Ignore it: it wasn't a valid bean name and property,
					// although it did start with the required prefix
					logger.debug("Invalid bean name and property [" + nameAndProperty + "]");
				}
			}	// if the key started with the prefix we're looking for
		}	// while there are more keys

		return beanCount;
	}

	/**
	 * Get all property values, given a prefix (which will be stripped)
	 * and add the bean they define to the factory with the given name
	 * @param beanName name of the bean to define
	 * @param m Map containing string pairs
	 * @param prefix prefix of each entry, which will be stripped
	 * @throws org.springframework.beans.factory.BeanDefinitionStoreException in the bean definition is invalid
	 */
	private void registerBeanDefinition(String beanName, Map m, String prefix) throws BeansException {
		String className = null;
		String parent = null;
		boolean singleton = true;
		boolean lazyInit = false;

		MutablePropertyValues pvs = new MutablePropertyValues();
		Set keys = m.keySet();
		Iterator itr = keys.iterator();
		while (itr.hasNext()) {
			String key = (String) itr.next();
			if (key.startsWith(prefix + SEPARATOR)) {
				String property = key.substring(prefix.length() + SEPARATOR.length());
				if (property.equals(CLASS_KEY)) {
					className = (String) m.get(key);
				}
				else if (property.equals(SINGLETON_KEY)) {
					String val = (String) m.get(key);
					singleton = (val == null) || val.equals(TRUE_VALUE);
				}
				else if (property.equals(LAZY_INIT_KEY)) {
					String val = (String) m.get(key);
					lazyInit = val.equals(TRUE_VALUE);
				}
				else if (property.equals(PARENT_KEY)) {
					parent = (String) m.get(key);
				}
				else if (property.endsWith(REF_SUFFIX)) {
					// This isn't a real property, but a reference to another prototype
					// Extract property name: property is of form dog(ref)
					property = property.substring(0, property.length() - REF_SUFFIX.length());
					String ref = (String) m.get(key);

					// It doesn't matter if the referenced bean hasn't yet been registered:
					// this will ensure that the reference is resolved at rungime
					// Default is not to use singleton
					Object val = new RuntimeBeanReference(ref);
					pvs.addPropertyValue(new PropertyValue(property, val));
				}
				else{
					// Normal bean property
					Object val = m.get(key);
					if (val instanceof String) {
						String sval = (String) val;
						// If it starts with unescaped prefix...
						if (sval.startsWith(REF_PREFIX)) {
							// Expand reference
							String targetName = ((String) val).substring(1);
							if (sval.startsWith("**")) {
								val = targetName;
							}
							else {
								val = new RuntimeBeanReference(targetName);
							}
						}
					}

					pvs.addPropertyValue(new PropertyValue(property, val));
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(pvs.toString());
		}

		if (parent == null) {
			parent = this.defaultParentBean;
		}

		if (className == null && parent == null)
			throw new FatalBeanException("Invalid bean definition. class or parent must be supplied for bean with name '" +
			                             beanName + "'");

		try {
			AbstractBeanDefinition beanDefinition = null;
			if (className != null) {
				// Load the class using a special class loader if one is available.
				// Otherwise rely on the thread context classloader.
				Class clazz = Class.forName(className, true, getBeanClassLoader());
				beanDefinition = new RootBeanDefinition(clazz, pvs);
			}
			else {
				beanDefinition = new ChildBeanDefinition(parent, pvs);
			}

			beanDefinition.setSingleton(singleton);
			beanDefinition.setLazyInit(lazyInit);
			getBeanFactory().registerBeanDefinition(beanName, beanDefinition);
		}
		catch (ClassNotFoundException ex) {
			throw new FatalBeanException("Cannot find class [" + className + "] for bean with name '" + beanName + "'", ex);
		}
	}

}
