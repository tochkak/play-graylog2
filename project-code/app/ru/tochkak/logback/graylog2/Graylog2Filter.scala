package ru.tochkak.logback.graylog2

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

case class GelfMessage(
  timestamp: Int,
  method: String,
  url: String,
  remoteAddr: String,
  httpStatus: Int,
  responseLength: Long,
  responseTime: Long
)

class Graylog2Filter @Inject()(
  implicit val mat: Materializer,
  ec: ExecutionContext,
  graylog2: Graylog2Component
) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])
    (requestHeader: RequestHeader): Future[Result] = {

    if (graylog2.isAccessLogEnabled) {
      val startTime = System.currentTimeMillis

      val graylogAppender = graylog2.getGelfClientAppender

      nextFilter(requestHeader).map {
        result => {
          val requestTime = System.currentTimeMillis() - startTime

          val shortMessage = s"${requestHeader.method} ${requestHeader.uri} took ${requestTime}ms and returned ${result.header.status}"

          val accessLogMessage = new AccessLogMessage(
            (startTime / 1000d).toInt,
            requestHeader.method,
            requestHeader.uri,
            requestHeader.remoteAddress,
            result.header.status,
            result.body.contentLength.getOrElse[Long](0),
            requestTime
          )

          graylogAppender.append(shortMessage, accessLogMessage)
          result
        }
      }
    } else {
      nextFilter(requestHeader)
    }
  }

}
