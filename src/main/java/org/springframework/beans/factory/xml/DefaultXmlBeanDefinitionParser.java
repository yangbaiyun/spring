package org.springframework.beans.factory.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.ConstructorArgumentValues;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.RuntimeBeanReference;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the XmlBeanDefinitionParser interface.
 * Parses bean definitions according to the "spring-beans" DTD. 
 * @author Juergen Hoeller
 * @since 18.12.2003
 */
public class DefaultXmlBeanDefinitionParser implements XmlBeanDefinitionParser {

	public static final String BEAN_NAME_DELIMITERS = ",; ";

	/**
	 * Value of a T/F attribute that represents true.
	 * Anything else represents false. Case seNsItive.
	 */
	private static final String TRUE_VALUE = "true";
	private static final String DEFAULT_VALUE = "default";

	private static final String DEFAULT_LAZY_INIT_ATTRIBUTE = "default-lazy-init";
	private static final String DEFAULT_DEPENDENCY_CHECK_ATTRIBUTE = "default-dependency-check";
	private static final String DEFAULT_AUTOWIRE_ATTRIBUTE = "default-autowire";

	private static final String BEAN_ELEMENT = "bean";
	private static final String DESCRIPTION_ELEMENT = "description";
	private static final String CLASS_ATTRIBUTE = "class";
	private static final String PARENT_ATTRIBUTE = "parent";
	private static final String ID_ATTRIBUTE = "id";
	private static final String NAME_ATTRIBUTE = "name";
	private static final String SINGLETON_ATTRIBUTE = "singleton";
	private static final String DEPENDS_ON_ATTRIBUTE = "depends-on";
	private static final String INIT_METHOD_ATTRIBUTE = "init-method";
	private static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";
	private static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";
	private static final String INDEX_ATTRIBUTE = "index";
	private static final String PROPERTY_ELEMENT = "property";
	private static final String REF_ELEMENT = "ref";
	private static final String LIST_ELEMENT = "list";
	private static final String MAP_ELEMENT = "map";
	private static final String KEY_ATTRIBUTE = "key";
	private static final String ENTRY_ELEMENT = "entry";
	private static final String BEAN_REF_ATTRIBUTE = "bean";
	private static final String LOCAL_REF_ATTRIBUTE = "local";
	private static final String EXTERNAL_REF_ATTRIBUTE = "external";
	private static final String VALUE_ELEMENT = "value";
	private static final String PROPS_ELEMENT = "props";
	private static final String PROP_ELEMENT = "prop";

	private static final String LAZY_INIT_ATTRIBUTE = "lazy-init";

	private static final String DEPENDENCY_CHECK_ATTRIBUTE = "dependency-check";
	private static final String DEPENDENCY_CHECK_ALL_ATTRIBUTE_VALUE = "all";
	private static final String DEPENDENCY_CHECK_SIMPLE_ATTRIBUTE_VALUE = "simple";
	private static final String DEPENDENCY_CHECK_OBJECTS_ATTRIBUTE_VALUE = "objects";

	private static final String AUTOWIRE_ATTRIBUTE = "autowire";
	private static final String AUTOWIRE_BY_NAME_VALUE = "byName";
	private static final String AUTOWIRE_BY_TYPE_VALUE = "byType";
	private static final String AUTOWIRE_CONSTRUCTOR_VALUE = "constructor";


	protected final Log logger = LogFactory.getLog(getClass());

	private BeanDefinitionRegistry beanFactory;

	private ClassLoader beanClassLoader;

	private String defaultLazyInit;

	private String defaultDependencyCheck;

	private String defaultAutowire;


	public void registerBeanDefinitions(BeanDefinitionRegistry beanFactory, ClassLoader beanClassLoader,
	                                Document doc, Resource resource) {
		this.beanFactory = beanFactory;
		this.beanClassLoader = beanClassLoader;

		logger.debug("Loading bean definitions");
		Element root = doc.getDocumentElement();

		this.defaultLazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE);
		logger.debug("Default lazy init '" + this.defaultLazyInit + "'");
		this.defaultDependencyCheck = root.getAttribute(DEFAULT_DEPENDENCY_CHECK_ATTRIBUTE);
		logger.debug("Default dependency check '" + this.defaultDependencyCheck + "'");
		this.defaultAutowire = root.getAttribute(DEFAULT_AUTOWIRE_ATTRIBUTE);
		logger.debug("Default autowire '" + this.defaultAutowire + "'");

		NodeList nl = root.getChildNodes();
		int beanDefinitionCounter = 0;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && BEAN_ELEMENT.equals(node.getNodeName())) {
				beanDefinitionCounter++;
				loadBeanDefinition((Element) node);
			}
		}
		logger.debug("Found " + beanDefinitionCounter + " <" + BEAN_ELEMENT + "> elements defining beans");
	}

	protected BeanDefinitionRegistry getBeanFactory() {
		return beanFactory;
	}

	protected ClassLoader getBeanClassLoader() {
		return beanClassLoader;
	}

	protected String getDefaultLazyInit() {
		return defaultLazyInit;
	}

	protected String getDefaultDependencyCheck() {
		return defaultDependencyCheck;
	}

	protected String getDefaultAutowire() {
		return defaultAutowire;
	}


	/**
	 * Parse an element definition: We know this is a BEAN element.
	 * Bean elements specify their canonical name as id attribute
	 * and their aliases as a delimited name attribute.
	 * If no id specified, use the first name in the name attribute as
	 * canonical name, registering all others as aliases.
	 */
	protected void loadBeanDefinition(Element ele) {
		String id = ele.getAttribute(ID_ATTRIBUTE);
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		List aliases = new ArrayList();
		if (nameAttr != null && !"".equals(nameAttr)) {
			aliases.addAll(Arrays.asList(StringUtils.tokenizeToStringArray(nameAttr, BEAN_NAME_DELIMITERS, true, true)));
		}
		AbstractBeanDefinition beanDefinition = parseBeanDefinition(ele, id);

		if (id == null || "".equals(id)) {
			if (!aliases.isEmpty()) {
				id = (String) aliases.remove(0);
				logger.debug("No XML id specified - using '" + id + "' as id and " + aliases + " as aliases");
			}
			else if (beanDefinition instanceof RootBeanDefinition) {
				id = ((RootBeanDefinition) beanDefinition).getBeanClass().getName();
				logger.debug("Neither XML id nor name specified - using bean class name [" + id + "] as id");
			}
			else {
				throw new BeanDefinitionStoreException(beanDefinition + " has neither id nor name nor bean class");
			}
		}

		logger.debug("Registering bean definition with id '" + id + "'");
		this.beanFactory.registerBeanDefinition(id, beanDefinition);
		for (Iterator it = aliases.iterator(); it.hasNext();) {
			this.beanFactory.registerAlias(id, (String) it.next());
		}
	}

	/**
	 * Parse a standard bean definition.
	 */
	protected AbstractBeanDefinition parseBeanDefinition(Element ele, String beanName) {
		String className = null;
		try {
			if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
				className = ele.getAttribute(CLASS_ATTRIBUTE);
			}
			String parent = null;
			if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
				parent = ele.getAttribute(PARENT_ATTRIBUTE);
			}
			if (className == null && parent == null) {
				throw new FatalBeanException("No class or parent in bean definition '" + beanName + "'", null);
			}

			AbstractBeanDefinition bd = null;
			PropertyValues pvs = getPropertyValueSubElements(ele);

			if (className != null) {
				Class clazz = Class.forName(className, true, this.beanClassLoader);
				ConstructorArgumentValues cargs = getConstructorArgSubElements(ele);
				RootBeanDefinition rbd = new RootBeanDefinition(clazz, cargs, pvs);

				if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
					String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
					rbd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, BEAN_NAME_DELIMITERS, true, true));
				}

				String dependencyCheck = ele.getAttribute(DEPENDENCY_CHECK_ATTRIBUTE);
				if (DEFAULT_VALUE.equals(dependencyCheck)) {
					dependencyCheck = this.defaultDependencyCheck;
				}
				rbd.setDependencyCheck(getDependencyCheck(dependencyCheck));

				String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
				if (DEFAULT_VALUE.equals(autowire)) {
					autowire = this.defaultAutowire;
				}
				rbd.setAutowire(getAutowire(autowire));

				String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
				if (!initMethodName.equals("")) {
					rbd.setInitMethodName(initMethodName);
				}
				String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
				if (!destroyMethodName.equals("")) {
					rbd.setDestroyMethodName(destroyMethodName);
				}

				bd = rbd;
			}
			else {
				bd = new ChildBeanDefinition(parent, pvs);
			}

			if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
				bd.setSingleton(TRUE_VALUE.equals(ele.getAttribute(SINGLETON_ATTRIBUTE)));
			}

			String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
			if (DEFAULT_VALUE.equals(lazyInit)) {
				lazyInit = this.defaultLazyInit;
			}
			bd.setLazyInit(TRUE_VALUE.equals(lazyInit));

			return bd;
		}
		catch (ClassNotFoundException ex) {
			throw new FatalBeanException("Error creating bean with name '" + beanName + "'; " +
					"classname was '" + className + "'", ex);
		}
	}

	/**
	 * Parse constructor argument subelements of the given bean element.
	 */
	protected ConstructorArgumentValues getConstructorArgSubElements(Element beanEle) {
		NodeList nl = beanEle.getElementsByTagName(CONSTRUCTOR_ARG_ELEMENT);
		ConstructorArgumentValues cargs = new ConstructorArgumentValues();
		for (int i = 0; i < nl.getLength(); i++) {
			Element cargEle = (Element) nl.item(i);
			parseConstructorArgElement(cargs, cargEle);
		}
		return cargs;
	}

	/**
	 * Parse property value subelements of the given bean element.
	 */
	protected PropertyValues getPropertyValueSubElements(Element beanEle) {
		NodeList nl = beanEle.getChildNodes();
		MutablePropertyValues pvs = new MutablePropertyValues();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && PROPERTY_ELEMENT.equals(node.getNodeName())) {
				parsePropertyElement(pvs, (Element) node);
			}
		}
		return pvs;
	}

	/**
	 * Parse a constructor-arg element.
	 */
	protected void parseConstructorArgElement(ConstructorArgumentValues cargs, Element ele) throws DOMException {
		Object val = getPropertyValue(ele);
		String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
		if (!"".equals(indexAttr)) {
			try {
				int index = Integer.parseInt(indexAttr);
				if (index < 0) {
					throw new FatalBeanException("'index' cannot be lower than 0");
				}
				cargs.addIndexedArgumentValue(index, val);
			}
			catch (NumberFormatException ex) {
				throw new FatalBeanException("Attribute 'index' of tag 'constructor-arg' must be an integer");
			}
		}
		else {
			cargs.addGenericArgumentValue(val);
		}
	}

	/**
	 * Parse a property element.
	 */
	protected void parsePropertyElement(MutablePropertyValues pvs, Element ele) throws DOMException {
		String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
		if ("".equals(propertyName)) {
			throw new BeanDefinitionStoreException("Tag 'property' must have a 'name' attribute");
		}
		Object val = getPropertyValue(ele);
		pvs.addPropertyValue(new PropertyValue(propertyName, val));
	}

	/**
	 * Get the value of a property element. May be a list.
	 * @param ele property element
	 */
	protected Object getPropertyValue(Element ele) {
		// should only have one element child: value, ref, collection
		NodeList nl = ele.getChildNodes();
		Element valueRefOrCollectionElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i) instanceof Element) {
				Element candidateEle = (Element) nl.item(i);
				if (DESCRIPTION_ELEMENT.equals(candidateEle.getTagName())) {
					// keep going: we don't use this value for now
				}
				else {
					// child element is what we're looking for
					valueRefOrCollectionElement = candidateEle;
				}
			}
		}
		if (valueRefOrCollectionElement == null) {
			throw new BeanDefinitionStoreException("<property> element must have a value, ref, or collection subelement");
		}
		return parsePropertySubelement(valueRefOrCollectionElement);
	}

	/**
	 * Parse a value, ref or collection subelement of a property element
	 * @param ele subelement of property element; we don't know which yet
	 */
	protected Object parsePropertySubelement(Element ele) {
		if (ele.getTagName().equals(BEAN_ELEMENT)) {
			return parseBeanDefinition(ele, "(inner bean definition)");
		}
		else if (ele.getTagName().equals(REF_ELEMENT)) {
			// a generic reference to any name of any bean
			String beanName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
			if ("".equals(beanName)) {
				// a reference to the id of another bean in the same XML file
				beanName = ele.getAttribute(LOCAL_REF_ATTRIBUTE);
				if ("".equals(beanName)) {
					// a reference to a bean in a different XML file
					beanName = ele.getAttribute(EXTERNAL_REF_ATTRIBUTE);
					if ("".equals(beanName)) {
						throw new FatalBeanException("Either 'bean' or 'local' or 'external' is required for a reference");
					}
				}
			}
			return new RuntimeBeanReference(beanName);
		}
		else if (ele.getTagName().equals(VALUE_ELEMENT)) {
			// it's a literal value
			return getTextValue(ele);
		}
		else if (ele.getTagName().equals(LIST_ELEMENT)) {
			return getList(ele);
		}
		else if (ele.getTagName().equals(MAP_ELEMENT)) {
			return getMap(ele);
		}
		else if (ele.getTagName().equals(PROPS_ELEMENT)) {
			return getProps(ele);
		}
		throw new BeanDefinitionStoreException("Unknown subelement of <property>: <" + ele.getTagName() + ">", null);
	}

	/**
	 * Return list of collection.
	 */
	protected List getList(Element collectionEle) {
		NodeList nl = collectionEle.getChildNodes();
		ManagedList l = new ManagedList();
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i) instanceof Element) {
				Element ele = (Element) nl.item(i);
				l.add(parsePropertySubelement(ele));
			}
		}
		return l;
	}

	protected Map getMap(Element mapEle) {
		ManagedMap m = new ManagedMap();
		List l = getChildElementsByTagName(mapEle, ENTRY_ELEMENT);
		for (int i = 0; i < l.size(); i++) {
			Element entryEle = (Element) l.get(i);
			String key = entryEle.getAttribute(KEY_ATTRIBUTE);
			// TODO hack: make more robust
			NodeList subEles = entryEle.getElementsByTagName("*");
			m.put(key, parsePropertySubelement((Element) subEles.item(0)));
		}
		return m;
	}

	/**
	 * Don't use the horrible DOM API to get child elements:
	 * Get an element's children with a given element name
	 */
	protected List getChildElementsByTagName(Element mapEle, String elementName) {
		NodeList nl = mapEle.getChildNodes();
		List nodes = new ArrayList();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n instanceof Element && elementName.equals(n.getNodeName())) {
				nodes.add(n);
			}
		}
		return nodes;
	}

	protected Properties getProps(Element propsEle) {
		Properties p = new Properties();
		NodeList nl = propsEle.getElementsByTagName(PROP_ELEMENT);
		for (int i = 0; i < nl.getLength(); i++) {
			Element propEle = (Element) nl.item(i);
			String key = propEle.getAttribute(KEY_ATTRIBUTE);
			String value = getTextValue(propEle);
			p.setProperty(key, value);
		}
		return p;
	}

	/**
	 * Make the horrible DOM API slightly more bearable:
	 * get the text value we know this element contains
	 */
	protected String getTextValue(Element ele) {
		NodeList nl = ele.getChildNodes();
		if (nl.item(0) == null) {
			// treat empty value as empty String
			return "";
		}
		if (nl.getLength() != 1 || !(nl.item(0) instanceof Text)) {
			throw new FatalBeanException("Unexpected element or type mismatch: expected single node of " +
																	 nl.item(0).getClass() + " to be of type Text: " + "found " + ele, null);
		}
		Text t = (Text) nl.item(0);
		// This will be a String
		return t.getData();
	}

	protected int getDependencyCheck(String att) {
		int dependencyCheckCode = RootBeanDefinition.DEPENDENCY_CHECK_NONE;
		if (DEPENDENCY_CHECK_ALL_ATTRIBUTE_VALUE.equals(att)) {
			dependencyCheckCode = RootBeanDefinition.DEPENDENCY_CHECK_ALL;
		}
		else if (DEPENDENCY_CHECK_SIMPLE_ATTRIBUTE_VALUE.equals(att)) {
			dependencyCheckCode = RootBeanDefinition.DEPENDENCY_CHECK_SIMPLE;
		}
		else if (DEPENDENCY_CHECK_OBJECTS_ATTRIBUTE_VALUE.equals(att)) {
			dependencyCheckCode = RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS;
		}
		// else leave default value
		return dependencyCheckCode;
	}

	protected int getAutowire(String att) {
		int autowire = RootBeanDefinition.AUTOWIRE_NO;
		if (AUTOWIRE_BY_NAME_VALUE.equals(att)) {
			autowire = RootBeanDefinition.AUTOWIRE_BY_NAME;
		}
		else if (AUTOWIRE_BY_TYPE_VALUE.equals(att)) {
			autowire = RootBeanDefinition.AUTOWIRE_BY_TYPE;
		}
		else if (AUTOWIRE_CONSTRUCTOR_VALUE.equals(att)) {
			autowire = RootBeanDefinition.AUTOWIRE_CONSTRUCTOR;
		}
		// else leave default value
		return autowire;
	}

}
