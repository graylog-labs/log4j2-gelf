# GELF Appender for Apache Log4j 2

This appender for [Apache Log4j 2](https://logging.apache.org/log4j/2.x/) logs messages to a GELF server like [Graylog2](http://www.graylog2.org) or [logstash](http://logstash.net).

It's using the official [GELF Java client](https://graylog2.github.io/gelfclient/) to connect to a remote server.

You can specify the following parameters for the GELF appender in the `log4j2.xml` configuration file:

`name` The reference name of the Appender

`server` The host name or IP address of the GELF server (default: `localhost`)

`port` The port the GELF server is listening on (default: `12201`)

`hostName` The host name of the machine generating the logs (default: the local host name or `localhost` if it couldn't be detected)

`protocol` The transport protocol to use (default: `UDP`)

`includeSource` Whether the source of the log message should be included (default: `true`)

`includeThreadContext` Whether the contents of the [ThreadContext](https://logging.apache.org/log4j/2.x/manual/thread-context.html) should be included (default: `true`)

`includeStackTrace` Whether a full stack trace should be included (default: `true`)

`queueSize` The size of the internally used queue (default: `512`)

`connectTimeout` The connection timeout for TCP connections in milliseconds (default: `1000`)

`reconnectDelay` The time to wait between reconnects in milliseconds (default: `500`)

`sendBufferSize` The size of the socket send buffer in bytes. A size of -1 deactivates the send buffer (default: `-1`)

`tcpNoDelay` Whether Nagle's algorithm should be used for TCP connections (default: `false`)

`filter` A [Filter](https://logging.apache.org/log4j/2.x/manual/filters.html) to determine if the event should be handled by this Appender

`layout` The [Layout](https://logging.apache.org/log4j/2.x/manual/layouts.html) to use to format the LogEvent (default: `"%m%n"`)

`ignoreExceptions` The default is `true`, causing exceptions encountered while appending events to be internally logged and then ignored. When set to `false` exceptions will be propagated to the caller, instead. Must be set to `false` when wrapping this Appender in a `FailoverAppender`.


## Log4j2.xml example

    <configuration status="OFF">
        <appenders>
            <GELF name="gelfAppender" server="graylog2.example.com" port="12201" hostName="appserver01.example.com"/>
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


# Versions

| GELF Appender for Apache Log4j 2 | Release date |
| -------------------------------- | ------------ |
| 1.0.0                            |              |

This appender uses GELF Java client 1.0.0.


# Installation

Maven coordinates
    
    <dependencies>
        <dependency>
            <groupId>org.graylog2.log4j2</groupId>
            <artifactId>log4j2-gelf</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>


# Project docs

The Maven project site is available at [Github](https://graylog2.github.io/gelf-log4j2).


# License

GELF Appender for Apache Log4j 2

Copyright (C) 2014 TORCH GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
