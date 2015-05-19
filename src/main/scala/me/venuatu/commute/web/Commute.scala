package me.venuatu.commute.web

import android.net.Uri
import android.util.{Base64, Log}
import com.google.android.gms.maps.model.LatLng
import com.squareup.okhttp.{MediaType, RequestBody, Response, Request}
import macroid.AppContext
import spray.json._, DefaultJsonProtocol._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Commute {
//  val URL = "https://staging.ilun.io/api/"
  val URL = "https://ilun.io/api/"

  case class Configuration(cacheUntil: Long, location: JsObject)
  implicit val ConfigurationFmt = jsonFormat(Configuration, "cacheUntil", "location")

  def pullConfiguration()(implicit ctx: AppContext): Future[Configuration] = {
    //TODO: actually caching this
    val request = new Request.Builder()
      .url(URL + "config")
    doAuthedRequest(request) map { resp =>
      if ((resp.code() / 100) == 2) {
        val str = resp.body().string()
        str.parseJson.convertTo[Configuration]
      } else {
        throw new Exception(s"commute api gave a ${resp.code()} ${resp.body().string()}")
      }
    }
  }

  // See: https://github.com/venuatu/commute-web/blob/master/models/Trip.js
  case class StopLocation(name: String, id: Option[String], platform: Option[String], stopIndex: Option[Int],
                          lat: Float, lng: Float)
  case class TripStep(streetName: Option[String], bogusName: Boolean, relativeDirection: Option[String],
                      absoluteDirection: Option[String], distance: Float, lat: Float, lng: Float, path: Option[String])
  case class TripLegAlert(text: String, upstream: String, start: Option[String], end: Option[String])
  case class TripRoadSegments(id: String, path: String, lastStep: Int, distance: Float, quickDuration: Option[Float],
                              minDuration: Float, maxDuration: Float, congestion: Int)
  case class TripLeg(from: StopLocation, to: StopLocation, transport: String, startTime: Long, endTime: Long,
                      duration: Float, distance: Float, path: String, realTime: Boolean, route: Option[String],
                      routeId: Option[String], steps: Seq[TripStep], alerts: Seq[TripLegAlert], headsign: Option[String],
                      roadSegments: Seq[TripRoadSegments])
  case class Trip(from: StopLocation, to: StopLocation, startTime: Long, endTime: Long, duration: Float,
                   distance: Float, transfers: Option[Int], mainTransport: String, walkDistance: Option[Float],
                   walkTime: Option[Float], transitTime: Option[Float], waitingTime: Option[Float],
                   legs: Seq[TripLeg])
  case class TripResult(trips: Seq[Trip])

  implicit val StopLocationFmt = jsonFormat(StopLocation, "name", "id", "platform", "stopIndex", "lat", "lng")
  implicit val TripStepFmt = jsonFormat(TripStep, "streetName", "bogusName", "relativeDirection", "absoluteDirection",
                                        "distance", "lat", "lng", "path")
  implicit val TripLegAlertFmt = jsonFormat(TripLegAlert, "text", "upstream", "start", "end")
  implicit val TripRoadSegmentsFmt = jsonFormat(TripRoadSegments, "_id", "path", "lastStep", "distance",
                                                "quickDuration", "minDuration", "maxDuration", "congestion")
  implicit val TripLegFmt = jsonFormat(TripLeg, "from", "to", "transport", "startTime", "endTime", "duration",
                                        "distance", "path", "realTime", "route", "routeId", "steps", "alerts", "headline", "roadSegments")
  implicit val TripFmt = jsonFormat(Trip, "from", "to", "startTime", "endTime", "duration", "distance", "transfers",
                                    "mainTransport", "walkDistance", "walkTime", "transitTime", "waitingTime", "legs")
  implicit val TripResultFmt = jsonFormat(TripResult, "trips")


  private def serializeLocation(loc: LatLng) =
    s"${loc.latitude},${loc.longitude}"

  def planTrips(from: LatLng, to: LatLng, departing: Boolean)(implicit ctx: AppContext): Future[Seq[Trip]] = {
    val request = new Request.Builder()
      .url(Uri.parse(URL + "route").buildUpon()
        .appendQueryParameter("from", serializeLocation(from))
        .appendQueryParameter("to", serializeLocation(to))
//        .appendQueryParameter("time", )
        .appendQueryParameter("departing", if (departing) "true" else "false")
        .build().toString
      )
    Log.d("commuteurl", request.toString)
    doAuthedRequest(request) map {resp =>
      if ((resp.code() / 100) != 2)
        throw new Exception(s"commute api gave a ${resp.code()} ${resp.body().string()}")
      val str = resp.body().string()
      str.parseJson.convertTo[TripResult].trips
    }
  }
  
  def pullTrip(_id: String)(implicit ctx: AppContext): Future[Option[Trip]] = {
    val request = new Request.Builder()
      .url(URL + "trip/" + _id)
    doAuthedRequest(request) map {resp =>
      if (resp.code() == 404) {
        None
      } else if ((resp.code() / 100) == 2) {
        val str = resp.body().string()
        Some(str.parseJson.convertTo[Trip])
      } else {
        throw new Exception(s"commute api gave a ${resp.code()} ${resp.body().string()}")
      }
    }
  }

  lazy val MEDIA_TYPE_JSON = MediaType.parse("application/json")

  case class ProgressResult(status: String, minDuration: Double, maxDuration: Double)

  implicit val ProgressResultFmt = jsonFormat(ProgressResult, "status", "minDuration", "maxDuration")

  case class Reading(timestamp: Long, lat: Double, lng: Double, bearing: Int, speed: Int)
  case class Sample(readings: Seq[Reading])
  case class ProgressReq(samples: Seq[Sample])

  implicit val ReadingFmt = jsonFormat(Reading, "timestamp", "lat", "lng", "bearing", "speed")
  implicit val SampleFmt = jsonFormat(Sample, "readings")
  implicit val ProgressReqFmt = jsonFormat(ProgressReq, "samples")

  def sendProgress(_id: String, samples: Seq[Sample])(implicit ctx: AppContext): Future[ProgressResult] = {
    val request = new Request.Builder()
      .url(URL + "trip/" + _id + "/progress")
      .post(RequestBody.create(MEDIA_TYPE_JSON, samples.toJson.toString()))
    doAuthedRequest(request) map {resp =>
      if ((resp.code() / 100) == 2) {
        val str = resp.body().string()
        str.parseJson.convertTo[ProgressResult]
      } else {
        throw new Exception(s"commute api gave a ${resp.code()} ${resp.body().string()}")
      }
    }
  }

  // auth token things

  private def doAuthedRequest(request: Request.Builder, tries: Int = 5)(implicit ctx: AppContext): Future[Response] = {
    getToken().flatMap { token =>
      WebRequest.doRequest(
        request.header("Authorization", token.toHeader)
        .build()
      )
    }.flatMap { resp =>
      if (resp.code() == 401 && tries > 0) {// clear and pull a new token when needed
        clearToken()
        doAuthedRequest(request, tries -1)
      } else {
        Future { resp }
      }
    }
  }

  case class Token(username: String, password: String) {
    def toHeader: String = {
      "Basic " + Base64.encodeToString(s"$username:$password".getBytes("UTF-8"), Base64.DEFAULT)
    }
  }
  implicit val TokenFmt = jsonFormat(Token, "username", "password")

  private def getToken()(implicit ctx: AppContext): Future[Token] = {
    val token = ctx.get.getSharedPreferences("commute_token", 0).getString("auth_token", null)
    if (token == null) {
      WebRequest.doRequest(
        new Request.Builder()
          .url(URL + "user")
          .post(RequestBody.create(MEDIA_TYPE_JSON, "{}"))
          .build()
      ) map { resp =>
        val bits = resp.body().string().parseJson.asJsObject
          .getFields("token").head.convertTo[String].split(":")
        val token = Token(bits(0), bits(1))
        saveToken(token)
        token
      }
    } else {
      Future { token.parseJson.convertTo[Token] }
    }
  }

  private def saveToken(token: Token)(implicit ctx: AppContext): Unit = {
    Log.d("commuteapi", s"saving authtoken: $token '${token.toHeader}'")
    ctx.get.getSharedPreferences("commute_token", 0)
      .edit()
      .putString("auth_token", token.toJson.toString())
      .commit()
  }

  private def clearToken()(implicit ctx: AppContext): Unit = {
    ctx.get.getSharedPreferences("commute_token", 0)
      .edit()
      .remove("auth_token")
      .commit()
  }
}
