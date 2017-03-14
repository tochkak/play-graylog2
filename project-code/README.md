This modules provides a logback appender that writes to Graylog2 via GELF over TCP for Play 2.5+.

To use this module, add the following dependency to your build.sbt file:

    ("ru.tochkak" %% "play2-graylog2" % "1.0.0").excludeAll(ExclusionRule(organization = "io.netty"))

In your application.conf you can set a couple of entries to configure the behavior of the appender:

    graylog2.appender.host="127.0.0.1:12201"        # the graylog2 server to send logs to (its GELF TCP input)
    graylog2.appender.sourcehost="example.org"      # string to use as "source" field of the GELF messages
    graylog2.appender.queue-size=512                # the number of logged, but unsent log messages, stored in memory
    graylog2.appender.reconnect-interval=500ms      # if the connection graylog2 is lost, wait this long until trying a reconnect
    graylog2.appender.connect-timeout=1s            # the TCP connect timeout
    graylog2.appender.tcp-nodelay=false             # optionally disable Nagle's algorithm, improves throughput if you log small messages a lot
    graylog2.appender.sendbuffersize                # if unset, it uses the systems default TCP send buffer size, override to use a different value
    graylog2.appender.access-log=true               # if set, send access log

To make use of the AccessLog that sends a structured access log to the configured graylog2 servers, include the Filter in your Global object:

For Scala:

    import javax.inject.Inject

    import play.api.http.HttpFilters
    import ru.tochkak.logback.graylog2.Graylog2Filter

    class Filters @Inject()(
      graylog2: Graylog2Filter
    ) extends HttpFilters {
      val filters = Seq(graylog2)
    }

Finally, enable the module in your `conf/application.conf`:

    play.modules.enabled += "ru.tochkak.logback.graylog2.Graylog2Module"

Also consult the Play documentation at https://www.playframework.com/documentation/2.5.x/ScalaHttpFilters if you would like to know more about Filters.


License
-------

Copyright 2013-2014 TORCH GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
