package ru.tochkak.logback.graylog2

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.ImplementedBy

import scala.collection.JavaConverters

class GrayLog2ParamsExtractorDefault extends GrayLog2ParamsExtractor {
  def extract(requestHeader: RequestHeader, response: Result): Map[String, String] = {
    Map[String, String]()
  }
}

@ImplementedBy(classOf[GrayLog2ParamsExtractorDefault])
trait GrayLog2ParamsExtractor {
  def extract(requestHeader: RequestHeader, response: Result): Map[String, String]
}

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
  graylog2: Graylog2Component,
  extractor: GrayLog2ParamsExtractor
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

          graylogAppender.append(
            shortMessage, accessLogMessage, JavaConverters.mapAsJavaMap(extractor.extract(requestHeader, result))
          )
          result
        }
      }
    } else {
      nextFilter(requestHeader)
    }
  }

}
