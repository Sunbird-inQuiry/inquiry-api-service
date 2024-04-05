package filters

import akka.util.ByteString
import org.apache.commons.lang3.StringUtils
import org.sunbird.telemetry.logger.TelemetryManager

import javax.inject.Inject
import org.sunbird.telemetry.util.TelemetryAccessEventUtil
import play.api.Logging
import play.api.libs.streams.Accumulator
import play.api.mvc._

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

class AccessLogFilter @Inject() (implicit ec: ExecutionContext) extends EssentialFilter with Logging {

    val xHeaderNames = Map("x-session-id" -> "X-Session-ID", "X-Consumer-ID" -> "x-consumer-id", "x-device-id" -> "X-Device-ID", "x-app-id" -> "APP_ID", "x-authenticated-userid" -> "X-Authenticated-Userid", "x-channel-id" -> "X-Channel-Id")

    def apply(nextFilter: EssentialAction) = new EssentialAction {
      def apply(requestHeader: RequestHeader) = {

        val startTime = System.currentTimeMillis
        val defaultReqId = UUID.randomUUID().toString
        val accumulator: Accumulator[ByteString, Result] = nextFilter(requestHeader)

        if (requestHeader.uri.contains("/publish")) {
          val requestId = requestHeader.headers.get("X-Request-ID").getOrElse("")
          if(StringUtils.isEmpty(requestId))
            requestHeader.headers.add(("X-Request-Id" -> defaultReqId))
          TelemetryManager.info(s"ENTRY:assessment: Request URL: ${requestHeader.uri} : Request Received For Publish.", Map("requestId" -> requestId).asJava.asInstanceOf[java.util.Map[String, AnyRef]])
        }

        accumulator.map { result =>
          val endTime     = System.currentTimeMillis
          val requestTime = endTime - startTime

          val path = requestHeader.uri
          if(!path.contains("/health")){
            val headers = requestHeader.headers.headers.groupBy(_._1).mapValues(_.map(_._2))
            val appHeaders = headers.filter(header => xHeaderNames.keySet.contains(header._1.toLowerCase))
                .map(entry => (xHeaderNames.get(entry._1.toLowerCase()).get, entry._2.head))
            val otherDetails = Map[String, Any]("StartTime" -> startTime, "env" -> "assessment",
                "RemoteAddress" -> requestHeader.remoteAddress,
                "ContentLength" -> result.body.contentLength.getOrElse(0),
                "Status" -> result.header.status, "Protocol" -> "http",
                "path" -> path,
                "Method" -> requestHeader.method.toString)
            TelemetryAccessEventUtil.writeTelemetryEventLog((otherDetails ++ appHeaders).asInstanceOf[Map[String, AnyRef]].asJava)
          }
          if (requestHeader.uri.contains("/publish")) {
            val requestId = requestHeader.headers.get("X-Request-ID").getOrElse("")
            val params = Map("requestId" -> requestId, "Status" -> result.header.status).asJava.asInstanceOf[java.util.Map[String, AnyRef]]
            TelemetryManager.info(s"EXIT:assessment: Request URL: ${requestHeader.uri} : Response Provided.", params)
          }
          result.withHeaders("Request-Time" -> requestTime.toString)
        }
      }
    }

  }