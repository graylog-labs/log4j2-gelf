
package org.graylog2.log4j2;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.Test;


public class GelfAppenderTest {
    @Test
    public void testLog() {
        final Logger logger = LogManager.getLogger("test");
        logger.info("Hello World");
    }


    @Test
    public void testMarker() {
        final Logger logger = LogManager.getLogger("test");
        final Marker parent = MarkerManager.getMarker("PARENT");
        final Marker marker = MarkerManager.getMarker("TEST").addParents(parent);
        logger.info(marker, "Hello World");
    }


    @Test
    public void testException() {
        final Logger logger = LogManager.getLogger("test");

        try {
            throw new Exception("Test");
        } catch (Exception e) {
            e.fillInStackTrace();
            logger.error("Hello World", e);
        }
    }


    @Test
    public void testThreadContext() {
        final Logger logger = LogManager.getLogger("test");

        ThreadContext.push("Message only");
        ThreadContext.push("int", 1);
        ThreadContext.push("int-long-string", 1, 2l, "3");
        ThreadContext.put("key", "value");

        logger.info("Hello World");

        ThreadContext.clearAll();

    }


    @AfterClass
    public static void shutdown() throws InterruptedException {
        //need to wait to hope the underlying gelf client pushes the messages.
        Thread.sleep(500);
    }
}
