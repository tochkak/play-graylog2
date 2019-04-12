package ru.tochkak.logback.graylog2;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public interface Graylog2Component {
    GelfClientAppender getGelfClientAppender();
    Boolean isAccessLogEnabled();
}

class Graylog2Impl implements Graylog2Component {

    private GelfClientAppender gelfClientAppender;

    private Boolean accessLogEnabled;

    private GelfConfiguration getGelfConfiguration(Config config) {
        final int queueCapacity = config.getInt("graylog2.appender.queue-size");
        final Long reconnectInterval = config.getDuration("graylog2.appender.reconnect-interval", TimeUnit.MILLISECONDS);
        final Long connectTimeout = config.getDuration("graylog2.appender.connect-timeout", TimeUnit.MILLISECONDS);
        final boolean isTcpNoDelay = config.getBoolean("graylog2.appender.tcp-nodelay");
        final String hostString = config.getString("graylog2.appender.host");
        final String protocol = config.getString("graylog2.appender.protocol");

        final HostAndPort hostAndPort = HostAndPort.fromString(hostString);

        GelfTransports gelfTransport = GelfTransports.valueOf(protocol.toUpperCase());

        final Integer sendBufferSize = config.getInt("graylog2.appender.sendbuffersize"); // causes the socket default to be used

        return new GelfConfiguration(hostAndPort.getHost(), hostAndPort.getPort())
                .transport(gelfTransport)
                .reconnectDelay(reconnectInterval.intValue())
                .queueSize(queueCapacity)
                .connectTimeout(connectTimeout.intValue())
                .tcpNoDelay(isTcpNoDelay)
                .sendBufferSize(sendBufferSize);
    }

    @Inject
    public Graylog2Impl(Config config) {
        String canonicalHostName;
        try {
            canonicalHostName = config.getString("graylog2.appender.sourcehost");
        } catch (ConfigException.Missing e) {
            canonicalHostName = "unknown";
        }

        accessLogEnabled = config.getBoolean("graylog2.appender.access-log");

        final GelfConfiguration gelfConfiguration = getGelfConfiguration(config);

        GelfTransport transport = GelfTransports.create(gelfConfiguration);

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);

        gelfClientAppender = new GelfClientAppender(transport, canonicalHostName);

        gelfClientAppender.setContext(lc);

        gelfClientAppender.start();

        rootLogger.addAppender(gelfClientAppender);
    }

    public Boolean isAccessLogEnabled() {
        return accessLogEnabled;
    }
    @SuppressWarnings("unused")
    public GelfClientAppender getGelfClientAppender() {
        return gelfClientAppender;
    }
}
