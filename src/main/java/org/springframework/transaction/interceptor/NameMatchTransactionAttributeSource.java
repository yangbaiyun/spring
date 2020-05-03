package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Simple implementation of TransactionAttributeSource that
 * allows attributes to be matched by registered name.
 * @author Juergen Hoeller
 * @since 21.08.2003
 * @see #isMatch
 */
public class NameMatchTransactionAttributeSource extends AbstractTransactionAttributeSource {

	private Map nameMap = new HashMap();

	/**
	 * Set a name/attribute map, consisting of method names
	 * (e.g. "myMethod") and TransactionAttribute instances.
	 * @see TransactionAttribute
	 */
	public void setNameMap(Map nameMap) {
		this.nameMap = nameMap;
	}

	/**
	 * Parses the given properties into a name/attribute map.
	 * Expects method names as keys and String attributes definitions as values,
	 * parsable into TransactionAttribute instances via TransactionAttributeEditor.
	 * @see #setNameMap
	 * @see TransactionAttributeEditor
	 */
	public void setProperties(Properties transactionAttributes) {
		TransactionAttributeEditor tae = new TransactionAttributeEditor();
		for (Iterator it = transactionAttributes.keySet().iterator(); it.hasNext(); ) {
			String methodName = (String) it.next();
			String value = transactionAttributes.getProperty(methodName);
			tae.setAsText(value);
			TransactionAttribute attr = (TransactionAttribute) tae.getValue();
			addTransactionalMethod(methodName, attr);
		}
	}

	/**
	 * Add an attribute for a transactional method.
	 * Method names can end with "*" for matching multiple methods.
	 * @param methodName the name of the method
	 * @param attr attribute associated with the method
	 */
	public void addTransactionalMethod(String methodName, TransactionAttribute attr) {
		logger.debug("Adding transactional method [" + methodName + "] with attribute [" + attr + "]");
		this.nameMap.put(methodName, attr);
	}

	public TransactionAttribute getTransactionAttribute(Method method, Class targetClass) {
		String methodName = method.getName();
		TransactionAttribute attr = (TransactionAttribute) this.nameMap.get(methodName);
		if (attr != null) {
			return attr;
		}
		else {
			// look up most specific name match
			String bestNameMatch = null;
			for (Iterator it = this.nameMap.keySet().iterator(); it.hasNext();) {
				String mappedName = (String) it.next();
				if (isMatch(methodName, mappedName) &&
						(bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
					attr = (TransactionAttribute) this.nameMap.get(mappedName);
					bestNameMatch = mappedName;
				}
			}
			return attr;
		}
	}

}
