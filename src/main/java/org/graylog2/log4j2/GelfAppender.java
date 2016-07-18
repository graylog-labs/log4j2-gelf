package org.graylog2.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.status.StatusLogger;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Plugin(name = "GELF", category = "Core", elementType = "appender", printObject = true)
public class GelfAppender extends AbstractAppender {
    private static final long serialVersionUID = 4796033328540158817L;

    private static final Logger LOG = StatusLogger.getLogger();

    private final GelfConfiguration gelfConfiguration;
    private final String hostName;
    private final boolean includeSource;
    private final boolean includeThreadContext;
    private final boolean includeStackTrace;
    private final boolean includeExceptionCause;
    private final Map<String, Object> additionalFields;

    private GelfTransport client;

    protected GelfAppender(final String name,
                           final Layout<? extends Serializable> layout,
                           final Filter filter,
                           final boolean ignoreExceptions,
                           final GelfConfiguration gelfConfiguration,
                           final String hostName,
                           final boolean includeSource,
                           final boolean includeThreadContext,
                           final boolean includeStackTrace,
                           String additionalFields,
                           final boolean includeExceptionCause) {
        super(name, filter, layout, ignoreExceptions);
        this.gelfConfiguration = gelfConfiguration;
        this.hostName = hostName;
        this.includeSource = includeSource;
        this.includeThreadContext = includeThreadContext;
        this.includeStackTrace = includeStackTrace;
        this.includeExceptionCause = includeExceptionCause;

        if (null != additionalFields && !additionalFields.isEmpty()) {
            this.additionalFields = new HashMap<>();

            try {
                String[] values = additionalFields.split(",");
                for (String s : values) {
                    String[] nvp = s.split("=");
                    this.additionalFields.put(nvp[0], nvp[1]);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to read additional fields.", e);
            }
        } else {
            this.additionalFields = Collections.emptyMap();
        }
    }

    @Override
    public void append(LogEvent event) {
        final GelfMessageBuilder builder = new GelfMessageBuilder(event.getMessage().getFormattedMessage(), hostName)
                .timestamp(event.getTimeMillis() / 1000d)
                .level(GelfMessageLevel.fromNumericLevel(Severity.getSeverity(event.getLevel()).getCode()))
                .additionalField("loggerName", event.getLoggerName())
                .additionalField("threadName", event.getThreadName());

        final Marker marker = event.getMarker();
        if (marker != null) {
            builder.additionalField("marker", marker.getName());
        }

        if (includeThreadContext) {
            for (Map.Entry<String, String> entry : event.getContextMap().entrySet()) {
                builder.additionalField(entry.getKey(), entry.getValue());
            }

            final List<String> contextStack = event.getContextStack().asList();
            if (contextStack != null && !contextStack.isEmpty()) {
                builder.additionalField("contextStack", contextStack.toString());
            }
        }

        final StackTraceElement source = event.getSource();
        if (includeSource && source != null) {
            builder.additionalField("sourceFileName", source.getFileName());
            builder.additionalField("sourceMethodName", source.getMethodName());
            builder.additionalField("sourceClassName", source.getClassName());
            builder.additionalField("sourceLineNumber", source.getLineNumber());
        }

        @SuppressWarnings("all")
        final Throwable thrown = event.getThrown();
        if (includeStackTrace && thrown != null) {
            String stackTrace;
            if (includeExceptionCause) {
                final StringWriter stringWriter = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(stringWriter);
                thrown.printStackTrace(printWriter);
                stackTrace = stringWriter.toString();
            } else {
                stackTrace = getSimpleStacktraceAsString(thrown);
            }

            builder.additionalField("exceptionClass", thrown.getClass().getCanonicalName());
            builder.additionalField("exceptionMessage", thrown.getMessage());
            builder.additionalField("exceptionStackTrace", stackTrace);

            builder.fullMessage(event.getMessage().getFormattedMessage());
        }

        if (!additionalFields.isEmpty()) {
            builder.additionalFields(additionalFields);
        }

        final GelfMessage gelfMessage = builder.build();
        try {
            final boolean sent = client.trySend(gelfMessage);
            if (!sent) {
                LOG.debug("Couldn't send message: {}", gelfMessage);
            }
        } catch (Exception e) {
            throw new AppenderLoggingException("failed to write log event to GELF server: " + e.getMessage(), e);
        }
    }

    protected String getSimpleStacktraceAsString(final Throwable thrown) {
        final StringBuilder stackTraceBuilder = new StringBuilder();
        for (StackTraceElement stackTraceElement : thrown.getStackTrace()) {
            new Formatter(stackTraceBuilder).format("%s.%s(%s:%d)%n",
                    stackTraceElement.getClassName(),
                    stackTraceElement.getMethodName(),
                    stackTraceElement.getFileName(),
                    stackTraceElement.getLineNumber());
        }
        return stackTraceBuilder.toString();
    }

    protected void setClient(GelfTransport client) {
        this.client = requireNonNull(client);
    }

    @Override
    public void start() {
        super.start();
        client = GelfTransports.create(gelfConfiguration);
    }

    @Override
    public void stop() {
        super.stop();
        if (client != null) {
            client.stop();
        }
    }

    @Override
    public String toString() {
        return GelfAppender.class.getSimpleName() + "{"
                + "name=" + getName()
                + ",server=" + gelfConfiguration.getRemoteAddress().getHostName()
                + ",port=" + gelfConfiguration.getRemoteAddress().getPort()
                + ",protocol=" + gelfConfiguration.getTransport().toString()
                + ",hostName=" + hostName
                + ",queueSize=" + gelfConfiguration.getQueueSize()
                + ",connectTimeout=" + gelfConfiguration.getConnectTimeout()
                + ",reconnectDelay=" + gelfConfiguration.getReconnectDelay()
                + ",sendBufferSize=" + gelfConfiguration.getSendBufferSize()
                + ",tcpNoDelay=" + gelfConfiguration.isTcpNoDelay()
                + ",tcpKeepAlive=" + gelfConfiguration.isTcpKeepAlive()
                + ",tlsEnabled=" + gelfConfiguration.isTlsEnabled()
                + ",tlsCertVerificationEnabled=" + gelfConfiguration.isTlsCertVerificationEnabled()
                + ",tlsTrustCertChainFilename=" + (gelfConfiguration.getTlsTrustCertChainFile() != null ?
                gelfConfiguration.getTlsTrustCertChainFile().getPath() : "null")
                + "}";
    }

    /**
     * Factory method for creating a {@link GelfTransport} provider within the plugin manager.
     *
     * @param name                             The name of the Appender.
     * @param filter                           A Filter to determine if the event should be handled by this Appender.
     * @param layout                           The Layout to use to format the LogEvent defaults to {@code "%m%n"}.
     * @param ignoreExceptions                 The default is {@code true}, causing exceptions encountered while appending events
     *                                         to be internally logged and then ignored. When set to {@code false} exceptions will
     *                                         be propagated to the caller, instead. Must be set to {@code false} when wrapping this
     *                                         Appender in a {@link org.apache.logging.log4j.core.appender.FailoverAppender}.
     * @param server                           The server name of the GELF server, defaults to {@code localhost}.
     * @param port                             The port the GELF server is listening on, defaults to {@code 12201}.
     * @param hostName                         The host name of the machine generating the logs, defaults to local host name
     *                                         or {@code localhost} if it couldn't be detected.
     * @param protocol                         The transport protocol to use, defaults to {@code UDP}.
     * @param tlsEnabled                       Whether TLS should be enabled, defaults to {@code false}.
     * @param tlsEnableCertificateVerification Whether TLS certificate chain should be checked, defaults to {@code true}.
     * @param tlsTrustCertChainFilename        A X.509 certificate chain file in PEM format for certificate verification, defaults to {@code null}
     * @param queueSize                        The size of the internally used queue, defaults to {@code 512}.
     * @param connectTimeout                   The connection timeout for TCP connections in milliseconds, defaults to {@code 1000}.
     * @param reconnectDelay                   The time to wait between reconnects in milliseconds, defaults to {@code 500}.
     * @param sendBufferSize                   The size of the socket send buffer in bytes, defaults to {@code -1} (deactivate).
     * @param tcpNoDelay                       Whether Nagle's algorithm should be used for TCP connections, defaults to {@code false}.
     * @param tcpKeepAlive                     Whether to try keeping alive TCP connections, defaults to {@code false}.
     * @param includeSource                    Whether the source of the log message should be included, defaults to {@code true}.
     * @param includeThreadContext             Whether the contents of the {@link org.apache.logging.log4j.ThreadContext} should be included, defaults to {@code true}.
     * @param includeStackTrace                Whether a full stack trace should be included, defaults to {@code true}.
     * @param includeExceptionCause            Whether the included stack trace should contain causing exceptions, defaults to {@code false}.
     * @param additionalFields                 Additional static comma-delimited key=value pairs that will be added to every log message.
     * @return a new GELF provider
     */
    @PluginFactory
    public static GelfAppender createGelfAppender(@PluginElement("Filter") Filter filter,
                                                  @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                  @PluginAttribute(value = "name") String name,
                                                  @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) Boolean ignoreExceptions,
                                                  @PluginAttribute(value = "server", defaultString = "localhost") String server,
                                                  @PluginAttribute(value = "port", defaultInt = 12201) Integer port,
                                                  @PluginAttribute(value = "protocol", defaultString = "UDP") String protocol,
                                                  @PluginAttribute(value = "hostName") String hostName,
                                                  @PluginAttribute(value = "queueSize", defaultInt = 512) Integer queueSize,
                                                  @PluginAttribute(value = "connectTimeout", defaultInt = 1000) Integer connectTimeout,
                                                  @PluginAttribute(value = "reconnectDelay", defaultInt = 500) Integer reconnectDelay,
                                                  @PluginAttribute(value = "sendBufferSize", defaultInt = -1) Integer sendBufferSize,
                                                  @PluginAttribute(value = "tcpNoDelay", defaultBoolean = false) Boolean tcpNoDelay,
                                                  @PluginAttribute(value = "tcpKeepAlive", defaultBoolean = false) Boolean tcpKeepAlive,
                                                  @PluginAttribute(value = "includeSource", defaultBoolean = true) Boolean includeSource,
                                                  @PluginAttribute(value = "includeThreadContext", defaultBoolean = true) Boolean includeThreadContext,
                                                  @PluginAttribute(value = "includeStackTrace", defaultBoolean = true) Boolean includeStackTrace,
                                                  @PluginAttribute(value = "includeExceptionCause", defaultBoolean = false) Boolean includeExceptionCause,
                                                  @PluginAttribute(value = "additionalFields") String additionalFields,
                                                  @PluginAttribute(value = "tlsEnabled", defaultBoolean = false) Boolean tlsEnabled,
                                                  @PluginAttribute(value = "tlsEnableCertificateVerification", defaultBoolean = true) Boolean tlsEnableCertificateVerification,
                                                  @PluginAttribute(value = "tlsTrustCertChainFilename") String tlsTrustCertChainFilename) {
        if (name == null) {
            LOGGER.error("No name provided for ConsoleAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        if (!"UDP".equalsIgnoreCase(protocol) && !"TCP".equalsIgnoreCase(protocol)) {
            LOG.warn("Invalid protocol {}, falling back to UDP", protocol);
            protocol = "UDP";
        }
        if (hostName == null || hostName.trim().isEmpty()) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOG.warn("Couldn't detect local host name, falling back to \"localhost\"");
                hostName = "localhost";
            }
        }

        final InetSocketAddress serverAddress = new InetSocketAddress(server, port);
        final GelfTransports gelfProtocol = GelfTransports.valueOf(protocol.toUpperCase());
        final GelfConfiguration gelfConfiguration = new GelfConfiguration(serverAddress)
                .transport(gelfProtocol)
                .queueSize(queueSize)
                .connectTimeout(connectTimeout)
                .reconnectDelay(reconnectDelay)
                .sendBufferSize(sendBufferSize)
                .tcpNoDelay(tcpNoDelay)
                .tcpKeepAlive(tcpKeepAlive);

        if (tlsEnabled) {
            if (gelfProtocol.equals(GelfTransports.TCP)) {
                gelfConfiguration.enableTls();
                if (!tlsEnableCertificateVerification) {
                    LOG.warn("TLS certificate validation is disabled. This is unsecure!");
                    gelfConfiguration.disableTlsCertVerification();
                }
                if (tlsEnableCertificateVerification && tlsTrustCertChainFilename != null) {
                    gelfConfiguration.tlsTrustCertChainFile(new File(tlsTrustCertChainFilename));
                }
            } else {
                LOG.warn("Enabling of TLS is invalid for UDP Transport");
            }
        }

        return new GelfAppender(name, layout, filter, ignoreExceptions, gelfConfiguration, hostName, includeSource,
                includeThreadContext, includeStackTrace, additionalFields, includeExceptionCause);
    }
}
