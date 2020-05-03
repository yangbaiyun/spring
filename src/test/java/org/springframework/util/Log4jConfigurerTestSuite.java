package org.springframework.util;

import java.io.FileNotFoundException;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Alef Arendsen
 */
public class Log4jConfigurerTestSuite extends TestCase {

	/*
	 * Class to test for void initLogging(String)
	 */
	public void testInitLoggingString() 
	throws FileNotFoundException {		
		try {
			Log4jConfigurer.initLogging("test/org/springframework/util/bla.properties");
			fail("Exception should have been thrown, file does not exist!");
		} catch (FileNotFoundException e) {
			// ok
		}
		
		Log4jConfigurer.initLogging("test/org/springframework/util/testlog4j.properties");
		
		Log log = LogFactory.getLog(this.getClass());
		log.debug("debug");
		log.info("info");
		log.warn("warn");
		log.error("error");
		log.fatal("fatal");
		
		assertTrue(MockLog4jAppender.loggingStrings.contains("debug"));
		assertTrue(MockLog4jAppender.loggingStrings.contains("info"));
		assertTrue(MockLog4jAppender.loggingStrings.contains("warn"));
		assertTrue(MockLog4jAppender.loggingStrings.contains("error"));
		assertTrue(MockLog4jAppender.loggingStrings.contains("fatal"));
	}

	public void testInitLoggingStringlong() {
		//TODO Implement initLogging() with time to wait before reloading!
	}

	public void testShutdownLogging() 
	throws FileNotFoundException {
		Log4jConfigurer.initLogging("test/org/springframework/util/testlog4j.properties");
		Log4jConfigurer.shutdownLogging();
		assertTrue(MockLog4jAppender.closeCalled);
	}

	public void testSetWorkingDirSystemProperty() {
		//TODO Implement setWorkingDirSystemProperty().
	}

}
