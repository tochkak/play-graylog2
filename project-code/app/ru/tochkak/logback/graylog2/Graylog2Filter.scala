package ru.tochkak.logback.graylog2

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

class Graylog2Filter @Inject() (
  implicit val mat: Materializer,
  ec: ExecutionContext,
  graylog2: Graylog2Component
) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])
    (requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map {
      result => {
        println("Hello")
        result
      }
    }
  }


}
