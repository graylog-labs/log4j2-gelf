package org.graylog2.log4j2;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class GelfAppenderTest {
    private Logger logger;

    @Before
    public void setUp() {
        logger = LogManager.getLogger("test");
    }

    @Test
    public void testLog() {
        logger.info("Hello World");
    }

    @Test
    public void testMarker() {
        final Marker parent = MarkerManager.getMarker("PARENT");
        final Marker marker = MarkerManager.getMarker("TEST").addParents(parent);
        logger.info(marker, "Hello World");
    }

    @Test
    public void testException() {
        try {
            throw new Exception("Test", new Exception("Cause", new RuntimeException("Inner Cause")));
        } catch (Exception e) {
            e.fillInStackTrace();
            logger.error("Hello World", e);
        }
    }

    @Test
    public void testThreadContext() {
        ThreadContext.push("Message only");
        ThreadContext.push("int", 1);
        ThreadContext.push("int-long-string", 1, 2L, "3");
        ThreadContext.put("key", "value");

        logger.info("Hello World");

        ThreadContext.clearAll();
    }

    @Test
    public void testIsFqdn() {
        assertThat(GelfAppender.isFQDN("host"), equalTo(false));
        assertThat(GelfAppender.isFQDN("123.123.56.53"), equalTo(false));
        assertThat(GelfAppender.isFQDN("::1"), equalTo(false));
        assertThat(GelfAppender.isFQDN("1080:0:0:0:8:800:200C:417A"), equalTo(false));
        assertThat(GelfAppender.isFQDN("2001:cdba::3257:9652"), equalTo(false));
        assertThat(GelfAppender.isFQDN("::ffff:0:10.0.0.3"), equalTo(false));
        assertThat(GelfAppender.isFQDN("2001:db8:122:344::192.0.2.33"), equalTo(false));
        assertThat(GelfAppender.isFQDN("host.example.com"), equalTo(true));
    }

    @AfterClass
    public static void shutdown() throws InterruptedException {
        //need to wait to hope the underlying gelf client pushes the messages.
        Thread.sleep(500);
    }
}
