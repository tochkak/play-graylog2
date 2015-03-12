package org.graylog2.logback.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.transport.GelfTransport;

public class GelfclientAppender extends AppenderBase<ILoggingEvent> {
    private final GelfTransport transport;
    private final String hostname;

    public GelfclientAppender(GelfTransport transport, String hostname) {
        this.transport = transport;
        this.hostname = hostname;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        append(convertToGelfMessage(eventObject));
    }

    public void append(GelfMessage gelfMessage) {
        try {
            transport.send(gelfMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private GelfMessage convertToGelfMessage(ILoggingEvent event) {
        return new GelfMessageBuilder(event.getFormattedMessage(), hostname)
                .timestamp(event.getTimeStamp() / 1000d)
                .level(toGelfMessageLevel(event.getLevel()))
                .additionalField("threadname", event.getThreadName())
                .additionalField("logger", event.getLoggerName())
                .build();
    }

    private GelfMessageLevel toGelfMessageLevel(Level level) {
        switch(level.toInt()) {
            case Level.ERROR_INT:
                return GelfMessageLevel.ERROR;
            case Level.WARN_INT:
                return GelfMessageLevel.WARNING;
            case Level.DEBUG_INT:
                return GelfMessageLevel.DEBUG;
            default:
                return GelfMessageLevel.INFO;
        }
    }
}
