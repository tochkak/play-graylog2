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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.graylog2.logback.appender.GelfTcpAppender.DONT_SEND_TO_GRAYLOG2;

public class GelfAppenderHandler extends SimpleChannelUpstreamHandler {
    private static final Logger log = LoggerFactory.getLogger(GelfAppenderHandler.class);

    private final Graylog2Plugin graylog2Plugin;
    private BlockingQueue<ILoggingEvent> queue;
    private Channel channel;
    private AtomicBoolean keepRunning = new AtomicBoolean(true);
    private final Thread senderThread;
    private ReentrantLock lock;
    private Condition connectedCond;

    public GelfAppenderHandler(Graylog2Plugin graylog2Plugin, int capacity) {
        this.graylog2Plugin = graylog2Plugin;
        this.queue = new LinkedBlockingQueue<>(capacity);
        lock = new ReentrantLock();
        connectedCond = lock.newCondition();

        senderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ILoggingEvent event = null;
                while (keepRunning.get()) {
                    // wait until we are connected to the graylog2 server before polling log events from the queue
                    lock.lock();
                    try {
                        while (channel == null || !channel.isConnected()) {
                            try {
                                connectedCond.await();
                            } catch (InterruptedException e) {
                                if (!keepRunning.get()) {
                                    // bail out if we are awoken because the application is stopping
                                    break;
                                }
                            }
                        }
                        // we are connected, let's start sending logs
                        try {
                            // if we have a lingering event already, try to send that instead of polling a new one.
                            if (event == null) {
                                event = queue.poll(100, TimeUnit.MILLISECONDS);
                            }
                            // if we are still connected, convert LoggingEvent to GELF and send it
                            // but if we aren't connected anymore, we'll have already pulled an event from the queue,
                            // which we keep hanging around in this thread and in the next loop iteration will block until we are connected again.
                            if (event != null && channel != null && channel.isConnected()) {
                                final ObjectNode gelf = Json.newObject();
                                InetSocketAddress localSock = (InetSocketAddress) channel.getLocalAddress();
                                gelf.put("short_message", event.getFormattedMessage());
                                gelf.put("host", localSock.getHostName());
                                gelf.put("timestamp", event.getTimeStamp() / 1000d);
                                gelf.put("threadname", event.getThreadName());
                                gelf.put("logger", event.getLoggerName());
                                gelf.put("loglevel", event.getLevel().toString());
                                // we send nul byte delimited gelf
                                final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(
                                        ChannelBuffers.wrappedBuffer(gelf.toString().getBytes(Charsets.UTF_8)),
                                        Delimiters.nulDelimiter()[0]);
                                channel.write(buffer);
                                event = null;
                            }
                        } catch (InterruptedException e) {
                            // ignore, when stopping keepRunning will be set to false outside
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            }
        });

        senderThread.start();
    }

    public boolean offer(ILoggingEvent event) {
        if (log.isTraceEnabled()) {
            log.trace(DONT_SEND_TO_GRAYLOG2, "Remaining capacity in GELF queue: {} elements.", queue.remainingCapacity());
        }
        return queue.offer(event);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        lock.lock();
        try {
            if (log.isDebugEnabled()) {
                log.debug(DONT_SEND_TO_GRAYLOG2, "Connection to graylog2 server at {} established.", ctx.getChannel().getRemoteAddress());
            }
            channel = e.getChannel();
            connectedCond.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (log.isDebugEnabled() && !(e.getCause() instanceof IOException))
            log.debug(DONT_SEND_TO_GRAYLOG2, "Caught exception during sending log event to graylog2 " + e);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug(DONT_SEND_TO_GRAYLOG2, "Connection lost to graylog2 server.");
        }
        lock.lock();
        // reconnect, TODO implement multiple connections so we can retry a different server
        try {
            channel = null;
            // only reconnect if we weren't stopped.
            if (keepRunning.get()) {
                graylog2Plugin.reconnect();
            }
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        keepRunning.set(false);
        // poke the sender thread to stop itself
        senderThread.interrupt();
    }
}
