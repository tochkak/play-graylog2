import org.graylog2.logback.appender.ScalaAccessLog
import play.api.mvc.WithFilters

object Global extends WithFilters(ScalaAccessLog) {
}