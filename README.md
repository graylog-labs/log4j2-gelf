# GELF Appender for Apache Log4j 2
[![Build Status](https://travis-ci.org/Graylog2/log4j2-gelf.svg?branch=master)](https://travis-ci.org/Graylog2/log4j2-gelf)

This appender for [Apache Log4j 2](https://logging.apache.org/log4j/2.x/) logs messages to a GELF server like [Graylog2](http://www.graylog2.org) or [logstash](http://logstash.net).

It's using the official [GELF Java client](https://graylog2.github.io/gelfclient/) to connect to a remote server.

You can specify the following parameters for the GELF appender in the `log4j2.xml` configuration file:

* `name`
  * The reference name of the Appender
* `server` (default: `localhost`)
  * The host name or IP address of the GELF server
* `port` (default: `12201`)
  * The port the GELF server is listening on
* `hostName` (default: the local host name or `localhost` if it couldn't be detected)
  * The host name of the machine generating the logs
* `protocol` (default: `UDP`)
  * The transport protocol to use
* `tlsEnabled` (default: `false`)
  * Whether TLS should be enabled
* `tlsEnableCertificateVerification` (default: `true`)
  * Whether the TLS certificate chain should be checked
* `tlsTrustCertChainFilename`  (default: empty)
  * A X.509 certificate chain file in PEM format for certificate verification
* `includeSource` (default: `true`)
  * Whether the source of the log message should be included
* `includeThreadContext` (default: `true`)
  * Whether the contents of the [ThreadContext](https://logging.apache.org/log4j/2.x/manual/thread-context.html) should be included
* `includeStackTrace` (default: `true`)
  * Whether a full stack trace should be included
* `includeExceptionCause` (default: `false`)
  * Whether the included stack trace should contain causing exceptions
* `queueSize` (default: `512`)
  * The size of the internally used queue
* `connectTimeout` (default: `1000`)
  * The connection timeout for TCP connections in milliseconds
* `reconnectDelay` (default: `500`)
  * The time to wait between reconnects in milliseconds
* `sendBufferSize` (default: `-1`)
  * The size of the socket send buffer in bytes. A size of -1 deactivates the send buffer
* `tcpNoDelay` (default: `false`)
  * Whether Nagle's algorithm should be used for TCP connections
* `tcpKeepAlive` (default: `false`)
  * Whether to try keeping alive TCP connections.
* `filter`
  * A [Filter](https://logging.apache.org/log4j/2.x/manual/filters.html) to determine if the event should be handled by this Appender
* `layout` (default: `"%m%n"`)
  * The [Layout](https://logging.apache.org/log4j/2.x/manual/layouts.html) to use to format the LogEvent
* `ignoreExceptions`
  * The default is `true`, causing exceptions encountered while appending events to be internally logged and then ignored. When set to `false` exceptions will be propagated to the caller, instead. Must be set to `false` when wrapping this Appender in a `FailoverAppender`.
* `additionalFields`
  * Comma-delimited list of key=value pairs to be included in every message

## Log4j2.xml example

    <configuration status="OFF" packages="org.graylog2.log4j2">
        <appenders>
            <GELF name="gelfAppender" server="graylog2.example.com" port="12201" hostName="appserver01.example.com" additionalFields="foo=bar"/>
        </appenders>
        <loggers>
            <root level="info">
                <appender-ref ref="gelfAppender"/>
            </root>
        </loggers>
    </configuration>


## Java code example

    Logger logger = LogManager.getLogger("test");
    ThreadContext.put("userId", "testUser");
    logger.info("Hello World");

## Using variables in the additionalFields

The `additionalFields` attribute can contain references to variables. 
In order for Log4j 2.x to resolve the variable's value, the variable name must have a certain prefix depending on how the variable is provided.
Internally we're making use of Log4j's [StrSubstitutor](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/StrSubstitutor.html) to resolve the variable's value. 
This in turn is utilizing the following [Log4j Lookups](https://logging.apache.org/log4j/2.x/manual/lookups.html) with the prefixes in the following list:

| Prefix       | Documentation                                                                                                                                                      |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `date`       | [DateLookup](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/DateLookup.html)                                         |
| `sd`         | [StructuredDataLookup](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/StructuredDataLookup.html)                     |
| `java`       | [SystemPropertiesLookup](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/SystemPropertiesLookup.html)                 |
| `ctx`        | [ContextMapLookup](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/ContextMapLookup.html)                             |
| `jndi`       | [JndiLookup](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/JndiLookup.html)                                         |
| `jvmrunargs` | [JmxRuntimeInputArgumentsLookup](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/JmxRuntimeInputArgumentsLookup.html) |
| `env`        | [EnvironmentLookup](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/EnvironmentLookup.html)                           |
| `sys`        | [SystemPropertiesLookup](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/SystemPropertiesLookup.html)                 |
| `map`        | [MapLookup](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/MapLookup.html)                                           |
| `bundle`     | [ResourceBundleLookup](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/lookup/ResourceBundleLookup.html)                     |

Please read up on the different variable handling in the linked Javadocs.

### Example configuration with variables

    <GELF name="gelfAppender" 
      server="graylog2.example.com" 
      port="12201" 
      hostName="appserver01.example.com" 
      additionalFields="user=${env:USER},CLIargument=${sys:cliargument},jvm=${java:vm},fileEncoding=${sys:file.encoding}"/>


# Versions

| GELF Appender for Apache Log4j 2 | Release date |
| -------------------------------- | ------------ |
| 1.0.0                            | 29-Sep-2014  |
| 1.0.1                            | 21-Oct-2014  |
| 1.0.2                            | 09-Feb-2015  |
| 1.0.3                            | 11-Feb-2015  |
| 1.1.0                            | 16-Jul-2015  |

This appender uses GELF Java client 1.3.0.


# Installation

Maven coordinates

    <dependencies>
        <dependency>
            <groupId>org.graylog2.log4j2</groupId>
            <artifactId>log4j2-gelf</artifactId>
            <version>1.1.0</version>
        </dependency>
    </dependencies>


# License

GELF Appender for Apache Log4j 2

Copyright (C) 2014 TORCH GmbH; 2015 Graylog, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
