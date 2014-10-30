package me.venuatu.commute.web

import spray.json._, DefaultJsonProtocol._

object Commute {

  // See: https://github.com/venuatu/commute-web/blob/master/models/Trip.js
  case class StopLocation(name: String, id: Option[String], platform: Option[String], stopIndex: Option[Int],
                          lat: Float, lng: Float)
  case class TripStep(streetName: Option[String], bogusName: Boolean, relativeDirection: Option[String],
                      absoluteDirection: Option[String], distance: Float, lat: Float, lng: Float, path: Option[String])
  case class TripLegAlert(text: String, upstream: String, start: String, end: String)
  case class TripRoadSegments(id: String, path: String, lastStep: Int, distance: Float, quickDuration: Float,
                              minDuration: Float, maxDuration: Float, congestion: Int)
  case class TripLeg(from: StopLocation, to: StopLocation, transport: String, startTime: Long, endTime: Long,
                      duration: Float, distance: Float, path: String, realTime: Boolean, routeId: Option[String],
                      steps: Seq[TripStep], alerts: Seq[TripLegAlert], roadSegments: Seq[TripRoadSegments])
  case class Trip(from: StopLocation, to: StopLocation, startTime: Long, endTime: Long, duration: Float,
                   distance: Float, transfers: Int, mainTransport: String, walkDistance: Option[Float],
                   walkTime: Option[Float], transitTime: Option[Float], waitingTime: Option[Float],
                   legs: Seq[TripLeg])

  implicit val StopLocationFmt = jsonFormat(StopLocation, "name", "id", "platform", "stopIndex", "lat", "lng")
  implicit val TripStepFmt = jsonFormat(TripStep, "streetName", "bogusName", "relativeDirection", "absoluteDirection",
                                        "distance", "lat", "lng", "path")
  implicit val TripLegAlertFmt = jsonFormat(TripLegAlert, "text", "upstream", "start", "end")
  implicit val TripRoadSegmentsFmt = jsonFormat(TripRoadSegments, "id", "path", "lastStep", "distance",
                                                "quickDuration", "minDuration", "maxDuration", "congestion")
  implicit val TripLegFmt = jsonFormat(TripLeg, "from", "to", "transport", "startTime", "endTime", "duration",
                                        "distance", "path", "realtime", "routeId", "steps", "alerts", "roadSegments")
  implicit val TripFmt = jsonFormat(Trip, "from", "to", "startTime", "endTime", "duration", "distance", "transfers",
                                    "mainTransport", "walkDistance", "walkTime", "transitTime", "waitingTime", "legs")


  
}
