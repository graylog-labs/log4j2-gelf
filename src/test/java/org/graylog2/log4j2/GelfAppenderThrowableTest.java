package org.graylog2.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class GelfAppenderThrowableTest {

    private GelfTransport mockedGelfTransport;

    @Before
    public void setUp() {
        mockedGelfTransport = mock(GelfTransport.class);
    }

    @Test
    public void shouldNotAddExceptionToFullMessage() throws InterruptedException {
        // given
        final GelfAppender gelfAppender = createGelfAppender(true, true);
        final LogEvent event = createLogEventMock();
        given(event.getThrown()).willReturn(new RuntimeException("Outer Exception", new Exception("Inner Exception")));

        // when
        gelfAppender.append(event);

        // then
        ArgumentCaptor<GelfMessage> gelfMessageCaptor = ArgumentCaptor.forClass(GelfMessage.class);
        verify(mockedGelfTransport).send(gelfMessageCaptor.capture());
        final String fullMessage = gelfMessageCaptor.getValue().getFullMessage();
        assertThat(fullMessage, is("Some Message"));
    }

    @Test
    public void shouldAppendExceptionClassAndMessage() throws InterruptedException {
        // given
        final GelfAppender gelfAppender = createGelfAppender(true, true);
        final LogEvent event = createLogEventMock();
        given(event.getThrown()).willReturn(new RuntimeException("Outer Exception", new Exception("Inner Exception")));

        // when
        gelfAppender.append(event);

        // then
        ArgumentCaptor<GelfMessage> gelfMessageCaptor = ArgumentCaptor.forClass(GelfMessage.class);
        verify(mockedGelfTransport).send(gelfMessageCaptor.capture());
        final Object exceptionMessage = gelfMessageCaptor.getValue()
                .getAdditionalFields()
                .get("exceptionMessage");
        final Object exceptionClass = gelfMessageCaptor.getValue()
                .getAdditionalFields()
                .get("exceptionClass");
        assertThat(exceptionMessage, notNullValue());
        assertThat(exceptionMessage.toString(), containsString("Outer Exception"));
        assertThat(exceptionClass, notNullValue());
        assertThat(exceptionClass.toString(), containsString("java.lang.RuntimeException"));
    }

    @Test
    public void shouldNotAppendExceptionInformationIfNotRequested() throws InterruptedException {
        // given
        final GelfAppender gelfAppender = createGelfAppender(false, false);
        final LogEvent event = createLogEventMock();
        given(event.getThrown()).willReturn(new RuntimeException("Outer Exception", new Exception("Inner Exception")));

        // when
        gelfAppender.append(event);

        // then
        ArgumentCaptor<GelfMessage> gelfMessageCaptor = ArgumentCaptor.forClass(GelfMessage.class);
        verify(mockedGelfTransport).send(gelfMessageCaptor.capture());
        final Object exceptionMessage = gelfMessageCaptor.getValue()
                .getAdditionalFields()
                .get("exceptionMessage");
        final Object exceptionClass = gelfMessageCaptor.getValue()
                .getAdditionalFields()
                .get("exceptionClass");
        final Object exceptionStackTrace = gelfMessageCaptor.getValue()
                .getAdditionalFields()
                .get("exceptionStackTrace");
        assertThat(exceptionMessage, nullValue());
        assertThat(exceptionClass, nullValue());
        assertThat(exceptionStackTrace, nullValue());
    }

    @Test
    public void shouldNotFailIfNoExceptionAvailable() throws InterruptedException {
        // given
        final GelfAppender gelfAppender = createGelfAppender(false, false);
        final LogEvent event = createLogEventMock();
        given(event.getThrown()).willReturn(null);

        // when
        gelfAppender.append(event);

        // then
        ArgumentCaptor<GelfMessage> gelfMessageCaptor = ArgumentCaptor.forClass(GelfMessage.class);
        verify(mockedGelfTransport).send(gelfMessageCaptor.capture());
        final Object exceptionMessage = gelfMessageCaptor.getValue()
                .getAdditionalFields()
                .get("exceptionMessage");
        final Object exceptionClass = gelfMessageCaptor.getValue()
                .getAdditionalFields()
                .get("exceptionClass");
        final Object exceptionStackTrace = gelfMessageCaptor.getValue()
                .getAdditionalFields()
                .get("exceptionStackTrace");
        assertThat(exceptionMessage, nullValue());
        assertThat(exceptionClass, nullValue());
        assertThat(exceptionStackTrace, nullValue());
    }

    @Test
    public void shouldAppendStacktraceWithCauses() throws InterruptedException {
        // given
        final GelfAppender gelfAppender = createGelfAppender(true, true);
        final LogEvent event = createLogEventMock();
        given(event.getThrown()).willReturn(new RuntimeException("Outer Exception", new Exception("Inner Exception")));

        // when
        gelfAppender.append(event);

        // then
        ArgumentCaptor<GelfMessage> gelfMessageCaptor = ArgumentCaptor.forClass(GelfMessage.class);
        verify(mockedGelfTransport).send(gelfMessageCaptor.capture());
        final Object exceptionStackTrace = gelfMessageCaptor.getValue()
                .getAdditionalFields()
                .get("exceptionStackTrace");
        assertThat(exceptionStackTrace, notNullValue());
        assertThat(exceptionStackTrace.toString(), containsString("Caused by: java.lang.Exception: Inner Exception"));
    }

    @Test
    public void shouldAppendStacktraceWithoutCauses() throws InterruptedException {
        // given
        final GelfAppender gelfAppender = createGelfAppender(true, false);
        final LogEvent event = createLogEventMock();
        final RuntimeException exception = new RuntimeException("Outer Exception", new Exception("Inner Exception"));
        given(event.getThrown()).willReturn(exception);

        // when
        gelfAppender.append(event);

        // then
        ArgumentCaptor<GelfMessage> gelfMessageCaptor = ArgumentCaptor.forClass(GelfMessage.class);
        verify(mockedGelfTransport).send(gelfMessageCaptor.capture());
        final Object exceptionStackTrace = gelfMessageCaptor.getValue()
                .getAdditionalFields()
                .get("exceptionStackTrace");
        assertThat(exceptionStackTrace, notNullValue());

        assertThat(exceptionStackTrace.toString(), is(gelfAppender.getSimpleStacktraceAsString(exception)));
        assertThat(exceptionStackTrace.toString(), not(containsString("Caused by: java.lang.Exception: Inner Exception")));
    }

    private GelfAppender createGelfAppender(final boolean includeStackTrace, final boolean includeExceptionCause) {
        GelfAppender gelfAppender = new GelfAppender("appender", null, null, false, null, "host", false, false, includeStackTrace,
                null, includeExceptionCause);
        gelfAppender.setClient(mockedGelfTransport);
        return gelfAppender;
    }

    private LogEvent createLogEventMock() {
        final Message message = mock(Message.class);
        given(message.getFormattedMessage()).willReturn("Some Message");

        final LogEvent event = mock(LogEvent.class);
        given(event.getMessage()).willReturn(message);
        given(event.getLevel()).willReturn(Level.ALL);
        return event;
    }
}