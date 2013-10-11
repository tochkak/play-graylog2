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
package org.graylog2.logback.appender

import play.api.mvc.{SimpleResult, RequestHeader, Filter}
import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.Play
import play.api.libs.json.{JsNumber, JsString, JsObject}
import ExecutionContext.Implicits.global
import java.net.{InetAddress, Inet4Address}
import play.api.libs.iteratee.{Iteratee, Enumeratee}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

class AccessLog extends Filter {
  // this will exist, because otherwise we can't use this class anyway
  val plugin: Graylog2Plugin = Play.current.plugin(classOf[Graylog2Plugin]).get
  val logger: GelfAppenderHandler = plugin.getGelfHandler
  def apply(next: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    val startTime: Long = System.currentTimeMillis()
    next(rh).map(result => logRequest(startTime, rh, result))
  }


  def logRequest(startTime: Long, request: RequestHeader, response: SimpleResult): SimpleResult = {
    val endTime: Long = System.currentTimeMillis()

    // TODO this can't be right.
    val bytes: Array[Byte] = Await.result(response.body |>>> Iteratee.consume[Array[Byte]](), Duration.Inf)
    val responseLength: Int = bytes.length

    // TODO add apache log format parser to have the fields configurable
    val gelfString = JsObject(List(
      // standard gelf fields
      "host" -> JsString(plugin.getLocalHostName),
      "short_message" -> JsString(request.method + " " + request.uri),
      "timestamp" -> JsNumber(startTime / 1000D),
      // request related fields
      "method" -> JsString(request.method),
      "url" -> JsString(request.uri),
      "version" -> JsString(request.version),
      "remote_host" -> JsString(request.remoteAddress),
      "referer" -> JsString(request.headers.get("Referer").getOrElse("")),
      "user_agent" -> JsString(request.headers.get("User-Agent").getOrElse("")),
      // response related fields
      "status" -> JsNumber(response.header.status),
      "response_bytes" -> JsNumber(responseLength),
      "duration" -> JsNumber(endTime - startTime)
    )).toString()

    logger.offer(gelfString)

    response
  }
}
