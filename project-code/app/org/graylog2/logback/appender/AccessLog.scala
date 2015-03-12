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

import org.graylog2.gelfclient.GelfMessage
import play.api.mvc.{Result, RequestHeader, Filter}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Play
import play.api.libs.json.{JsNumber, JsString, JsObject}
import ExecutionContext.Implicits.global
import play.api.libs.iteratee.{Enumeratee, Enumerator}

class AccessLog extends Filter {
  def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = ScalaAccessLog.apply(f)(rh)
}

object ScalaAccessLog extends Filter {
  // this will exist, because otherwise we can't use this class anyway
  lazy val plugin: Graylog2Plugin = Play.current.plugin(classOf[Graylog2Plugin]).get
  lazy val logger = plugin.getGelfAppender

  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (plugin.isAccessLogEnabled) {
      val startTime: Long = System.currentTimeMillis()
      next(rh).map(result => logRequest(startTime, rh, result))
    } else {
      next(rh)
    }
  }


  def logRequest(startTime: Long, request: RequestHeader, response: Result): Result = {
    val endTime: Long = System.currentTimeMillis()

    // adapt the original response.body Enumerator and then return new SimpleResult.
    // there seems to be no way of doing parallel iteration or I'm just too dumb for it.
    // all other proposed solutions either broke assets completely, or just didn't finish collecting.
    var responseLength: Int = 0
    var chunks: Int = 0
    val enumerator: Enumerator[Array[Byte]] = response.body through Enumeratee.map[Array[Byte]] {
      chunk =>
        responseLength = responseLength + chunk.size
        chunks += 1
        chunk
    }
    val doner: Enumerator[Array[Byte]] = enumerator.onDoneEnumerating {
      // TODO add apache log format parser to have the fields configurable
      val fields : Map[String, Any] = Map(
        // standard gelf fields
        "host" -> plugin.getLocalHostName,
        "short_message" -> (request.method + " " + request.uri),
        "timestamp" -> startTime / 1000D,
        // request related fields
        "method" -> request.method,
        "url" -> request.uri,
        "version" -> request.version,
        "remote_host" -> request.remoteAddress,
        "referer" -> request.headers.get("Referer").getOrElse(""),
        "user_agent" -> request.headers.get("User-Agent").getOrElse(""),
        // response related fields
        "status" -> response.header.status,
        "response_bytes" -> responseLength,
        "duration" -> (endTime - startTime),
        "chunks" -> chunks
      )

      val gelfMessage : GelfMessage = new GelfMessage(request.method + " " + request.uri, plugin.getLocalHostName)

      fields.foreach(entry => gelfMessage.addAdditionalField(entry._1, entry._2))

      logger.append(gelfMessage)
    }

    Result(response.header, doner, response.connection)
  }
}
