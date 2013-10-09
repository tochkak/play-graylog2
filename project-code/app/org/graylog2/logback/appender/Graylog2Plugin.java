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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.base.Splitter;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.slf4j.LoggerFactory;
import play.Application;
import play.Configuration;
import play.Plugin;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.graylog2.logback.appender.GelfTcpAppender.DONT_SEND_TO_GRAYLOG2;

@SuppressWarnings("unused")
public class Graylog2Plugin extends Plugin {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Graylog2Plugin.class);

    private ChannelFuture channelFuture;
    private GelfTcpAppender gelfAppender;
    private GelfAppenderHandler handler;

    private InetSocketAddress graylog2ServerAddress;
    private final Integer queueCapacity;
    private ClientBootstrap bootstrap;
    private Graylog2Plugin.ReconnectListener reconnectListener;

    public Graylog2Plugin(Application app) {
        final Configuration config = app.configuration();
        queueCapacity = config.getInt("graylog2.appender.queue-size", 512);

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                channelFuture = reconnect();
            }
        }).start();
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

    private class ReconnectListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                if (log.isDebugEnabled()) {
                    log.debug(DONT_SEND_TO_GRAYLOG2, "Could not connect. Retrying in 500 ms. Exception {}", future.getCause().getMessage());
                }
                // TODO implement back-off
                Thread.sleep(500);
                // could not connect, let's retry
                reconnect();
            }
        }
    }
}
