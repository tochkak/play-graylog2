package ru.tochkak.logback.graylog2;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.net.HostAndPort;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.LoggerFactory;
import play.Configuration;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SuppressWarnings("unused")
public interface Graylog2Component {
    GelfClientAppender getGelfClientAppender();
    Boolean isAccessLogEnabled();
}

class Graylog2Impl implements Graylog2Component {

    private GelfClientAppender gelfClientAppender;

    private Boolean accessLogEnabled;

    private GelfConfiguration getGelfConfiguration(Configuration config) {
        final Integer queueCapacity = config.getInt("graylog2.appender.queue-size", 512);
        final Long reconnectInterval = config.getMilliseconds("graylog2.appender.reconnect-interval", 500L);
        final Long connectTimeout = config.getMilliseconds("graylog2.appender.connect-timeout", 1000L);
        final Boolean isTcpNoDelay = config.getBoolean("graylog2.appender.tcp-nodelay", false);
        final String hostString = config.getString("graylog2.appender.host", "127.0.0.1:12201");
        final String protocol = config.getString("graylog2.appender.protocol", "udp");

        final HostAndPort hostAndPort = HostAndPort.fromString(hostString);

        GelfTransports gelfTransport = GelfTransports.valueOf(protocol.toUpperCase());

        final Integer sendBufferSize = config.getInt("graylog2.appender.sendbuffersize", 0); // causes the socket default to be used

        return new GelfConfiguration(hostAndPort.getHostText(), hostAndPort.getPort())
                .transport(gelfTransport)
                .reconnectDelay(reconnectInterval.intValue())
                .queueSize(queueCapacity)
                .connectTimeout(connectTimeout.intValue())
                .tcpNoDelay(isTcpNoDelay)
                .sendBufferSize(sendBufferSize);
    }

    @Inject
    public Graylog2Impl(Configuration config) {
        String canonicalHostName;
        try {
            canonicalHostName = config.getString(
                    "graylog2.appender.sourcehost",
                    InetAddress.getLocalHost().getCanonicalHostName()
            );
        } catch (UnknownHostException e) {
            canonicalHostName = "unknown";
        }

        accessLogEnabled = config.getBoolean("graylog2.appender.access-log", false);

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
