/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
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
