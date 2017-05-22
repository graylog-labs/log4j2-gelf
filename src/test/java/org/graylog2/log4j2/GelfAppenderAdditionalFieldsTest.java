package org.graylog2.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GelfAppenderAdditionalFieldsTest {

    private static final String KEY = "non-relevant-key";
    private static final String CONTEXT_KEY = "non-relevant-contextKey";
    private static final Object VALUE = "non-relevant-value";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private GelfTransport mockedGelfTransport;

    @Before
    public void setUp() {
        when(mockedGelfTransport.trySend(any(GelfMessage.class))).thenReturn(true);
    }

    @Test
    public void shouldResolveVariablesForAdditionalFields() {
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put(CONTEXT_KEY, VALUE.toString());
        KeyValuePair[] keyValuePairs = {new KeyValuePair(KEY, format("${ctx:%s:-}", CONTEXT_KEY))};

        final GelfAppender gelfAppender = createGelfAppender(true, false, keyValuePairs);
        final LogEvent event = createLogEventMock(contextMap);

        // when
        gelfAppender.append(event);

        // then
        ArgumentCaptor<GelfMessage> gelfMessageCaptor = ArgumentCaptor.forClass(GelfMessage.class);
        verify(mockedGelfTransport).trySend(gelfMessageCaptor.capture());

        GelfMessage message = gelfMessageCaptor.getValue();
        assertThat(message.getAdditionalFields(), hasEntry(KEY, VALUE));
    }

    private GelfAppender createGelfAppender(final boolean includeStackTrace, final boolean includeExceptionCause, KeyValuePair[] additionalFields) {
        GelfAppender gelfAppender = new GelfAppender("appender", null, null, false, null, "host", false, false, includeStackTrace,
                additionalFields, includeExceptionCause);
        gelfAppender.setClient(mockedGelfTransport);

        return gelfAppender;
    }

    private LogEvent createLogEventMock(Map<String, String> contextMap) {
        final Message message = mock(Message.class);
        given(message.getFormattedMessage()).willReturn("Some Message");
        final LogEvent event = mock(LogEvent.class);
        given(event.getContextMap()).willReturn(contextMap);
        given(event.getMessage()).willReturn(message);
        given(event.getLevel()).willReturn(Level.ALL);
        return event;
    }
}
