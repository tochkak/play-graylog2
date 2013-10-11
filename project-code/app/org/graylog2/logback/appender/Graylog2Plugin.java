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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.slf4j.LoggerFactory;
import play.Application;
import play.Configuration;
import play.Plugin;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.graylog2.logback.appender.GelfTcpAppender.DONT_SEND_TO_GRAYLOG2;

@SuppressWarnings("unused")
public class Graylog2Plugin extends Plugin {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Graylog2Plugin.class);
    private final Long connectTimeout;
    private final Boolean isTcpNoDelay;
    private final Integer sendBufferSize;
    private String canonicalHostName;

    private ChannelFuture channelFuture;
    private GelfTcpAppender gelfAppender;
    private GelfAppenderHandler handler;

    private InetSocketAddress graylog2ServerAddress;
    private final Integer queueCapacity;
    private ClientBootstrap bootstrap;
    private Graylog2Plugin.ReconnectListener reconnectListener;
    private ExecutorService reconnectExecutor;
    private Graylog2Plugin.ReconnectRunnable reconnector;
    private final Long reconnectInterval;

    public Graylog2Plugin(Application app) {
        final Configuration config = app.configuration();

        queueCapacity = config.getInt("graylog2.appender.queue-size", 512);
        reconnectInterval = config.getMilliseconds("graylog2.appender.reconnect-interval", 500L);
        connectTimeout = config.getMilliseconds("graylog2.appender.connect-timeout", 1000L);
        isTcpNoDelay = config.getBoolean("graylog2.appender.tcp-nodelay", false);
        sendBufferSize = config.getInt("graylog2.appender.sendbuffersize", 0); // causes the socket default to be used
        try {
            canonicalHostName = config.getString("graylog2.appender.sourcehost", InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException e) {
            canonicalHostName = "localhost";
            log.error("Unable to resolve canonical localhost name. " +
                    "Please set it manually via graylog2.appender.sourcehost or fix your lookup service, falling back to {}", canonicalHostName);
        }
        // TODO make this a list and dynamically accessible from the application
        final String entry = config.getString("graylog2.appender.host", "127.0.0.1:12201");

        final Iterable<String> parts = Splitter.on(':').trimResults().omitEmptyStrings().limit(2).split(entry.toString());
        final Iterator<String> it = parts.iterator();
        try {
            String host = it.next();
            String port = it.next();
            graylog2ServerAddress = new InetSocketAddress(host, Integer.valueOf(port));
        } catch (IllegalArgumentException | NoSuchElementException e) {
            log.error("Malformed graylog2.appender.hosts entry {}. Please specify them as 'host:port'!", entry);
        }

        reconnectExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("graylog2-appender-reconnect-%d")
                        .build());
        reconnector = new ReconnectRunnable();
    }

    @Override
    public void onStart() {
        bootstrap = new ClientBootstrap(
                new OioClientSocketChannelFactory(
                        Executors.newCachedThreadPool()));

        reconnectListener = new ReconnectListener();

        handler = new GelfAppenderHandler(this, queueCapacity);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(handler);
            }
        });
        bootstrap.setOption("connectTimeoutMillis", connectTimeout);
        bootstrap.setOption("tcpNoDelay", isTcpNoDelay);
        if (sendBufferSize > 0) {
            bootstrap.setOption("sendBufferSize", sendBufferSize);
        }

        reconnectExecutor.execute(reconnector);
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);

        gelfAppender = new GelfTcpAppender(handler);
        gelfAppender.setContext(lc);
        gelfAppender.start();

        rootLogger.addAppender(gelfAppender);
    }

    public ChannelFuture reconnect() {
        if (log.isDebugEnabled()) {
            log.debug(DONT_SEND_TO_GRAYLOG2, "Reconnecting to graylog2 at {}", graylog2ServerAddress);
        }
        channelFuture = bootstrap.connect(graylog2ServerAddress);
        channelFuture.addListener(reconnectListener);
        return channelFuture;
    }

    @Override
    public void onStop() {
        if (gelfAppender != null) gelfAppender.stop();
        if (handler != null) handler.stop();
        if (channelFuture.getChannel()!= null) channelFuture.getChannel().close().awaitUninterruptibly();
    }

    public GelfAppenderHandler getGelfHandler() {
        return handler;
    }

    public String getLocalHostName() {
        return canonicalHostName;
    }

    private class ReconnectRunnable implements Runnable {
        // TODO implement back off strategy here.
        // this implementation simply sleeps for now
        @Override
        public void run() {
            try {
                Thread.sleep(reconnectInterval);
            } catch (InterruptedException e) {
                // ignore
            }
            channelFuture = reconnect();
        }
    }

    private class ReconnectListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                if (log.isDebugEnabled()) {
                    log.debug(DONT_SEND_TO_GRAYLOG2, "Could not connect. Retrying in {} ms. Exception {}", reconnectInterval ,future.getCause().getMessage());
                }
                reconnectExecutor.execute(reconnector);
            }
        }
    }
}
