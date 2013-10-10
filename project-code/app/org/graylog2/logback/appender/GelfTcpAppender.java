/*
 * Copyright 2013 TORCH UG
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.graylog2.logback.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.BasicMarkerFactory;

public class GelfTcpAppender extends AppenderBase<ILoggingEvent> {
    private static final Logger log = LoggerFactory.getLogger(GelfAppenderHandler.class);

    private final GelfAppenderHandler handler;

    public static final Marker DONT_SEND_TO_GRAYLOG2 = new BasicMarkerFactory().getMarker("GRAYLOG2_IGNORE_MARKER");

    public GelfTcpAppender(GelfAppenderHandler handler) {
        this.handler = handler;
        setName("Graylog2-TCP");
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (event == null || !isStarted()) return;
        // skip our own log events, to avoid infinite loops
        if (event.getMarker() != null && event.getMarker().contains(DONT_SEND_TO_GRAYLOG2)) return;

        final boolean added = handler.offer(event);
        if (!added) {
            log.warn(DONT_SEND_TO_GRAYLOG2, "Graylog2 appender queue ran out of capacity, dropping log message {}", event);
        }
    }


}
