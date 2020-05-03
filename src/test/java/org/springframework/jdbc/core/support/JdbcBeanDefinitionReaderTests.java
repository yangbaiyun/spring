package org.springframework.jdbc.core.support;

import java.sql.ResultSet;
import java.sql.Statement;

import org.easymock.MockControl;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.JdbcTestCase;

/**
 * @author Rod Johnson
 */
public class JdbcBeanDefinitionReaderTests extends JdbcTestCase {

	public void testValid() throws Exception {
		String sql =
			"SELECT NAME AS NAME, PROPERTY AS PROPERTY, VALUE AS VALUE FROM T";

		String[][] results =
			{ { "one", "class", "org.springframework.beans.TestBean" }, {
				"one", "age", "53" }, };

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true, 2);
		ctrlResultSet.setReturnValue(false, 1);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(results[0][0]);
		mockResultSet.getString(2);
		ctrlResultSet.setReturnValue(results[0][1]);
		mockResultSet.getString(3);
		ctrlResultSet.setReturnValue(results[0][2]);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(results[1][0]);
		mockResultSet.getString(2);
		ctrlResultSet.setReturnValue(results[1][1]);
		mockResultSet.getString(3);
		ctrlResultSet.setReturnValue(results[1][2]);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		Statement mockStatement = (Statement) ctrlStatement.getMock();
		mockStatement.executeQuery(sql);
		ctrlStatement.setReturnValue(mockResultSet);
		mockStatement.getWarnings();
		ctrlStatement.setReturnValue(null);
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		JdbcBeanDefinitionReader reader = new JdbcBeanDefinitionReader(bf);
		reader.setDataSource(mockDataSource);
		reader.loadBeanDefinitions(sql);
		assertTrue(bf.getBeanDefinitionCount() == 1);
		TestBean tb = (TestBean) bf.getBean("one");
		assertTrue(tb.getAge() == 53);

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

}
