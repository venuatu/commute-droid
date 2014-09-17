package me.venuatu.background_tracker

import android.content.IntentSender
import android.location.Location
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesClient.{ConnectionCallbacks, OnConnectionFailedListener}
import com.google.android.gms.location.{LocationClient, LocationListener, LocationRequest}
import com.google.android.gms.maps.model.LatLng
import macroid.ActivityContext
import me.venuatu.background_tracker.Tracker._
import spray.json.DefaultJsonProtocol._

object Tracker {
  sealed trait LocationEvent {}
  case class Connected() extends LocationEvent
  case class Disconnected() extends LocationEvent
  case class Location(lat: Double, lng: Double, accuracy: Double, altitude: Double, time: Long) extends LocationEvent
  case class Errored(e: Exception) extends LocationEvent

  implicit val LocationFormat = jsonFormat(Location, "lat", "lng", "accuracy", "altitude", "time")

  object LocationUtils {
    def fromAndroidLocation(loc: android.location.Location) = {
      Location(loc.getLatitude, loc.getLongitude, loc.getAccuracy, loc.getAltitude, loc.getTime)
    }
  }

  case class Priority(priority: String, interval: Int, minInterval: Int)
  def prio(str: String) = {
    str match {
      case "high" => LocationRequest.PRIORITY_HIGH_ACCURACY
      case "balanced" => LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
      case "low-power" => LocationRequest.PRIORITY_LOW_POWER
      case "no-power" => LocationRequest.PRIORITY_NO_POWER
      case _ =>
        throw new IllegalArgumentException("Priority should be one of: 'high', 'balanced', 'low-power', 'no-power'")
    }
  }

  val EARTH_POINT_DISTANCE: Double = 2 * Math.PI * (6378137d / 360)
  def badLatLngNear(lat1: Double, lat2: Double, lng1: Double, lng2: Double, distance: Double): Boolean = {
    val lng = Math.abs(lng2 - lng1) * Math.cos(lat2)
    val lat = Math.abs(lat2 - lat1)
    val dist = distance / EARTH_POINT_DISTANCE

    (lng * lng + lat * lat) < (dist * dist)
  }
}

class Tracker(priority: Priority, f: (LocationEvent) => Unit)(implicit ctx: ActivityContext) extends ConnectionCallbacks with OnConnectionFailedListener
    with LocationListener {
  def isConnected = connected

  private var connected = false
  val req = LocationRequest.create()

  req.setPriority(prio(priority.priority))
  req.setInterval(priority.interval)
  req.setFastestInterval(priority.minInterval)

  val client = new LocationClient(ctx.get, this, this)

  def start() {
    client.connect()
  }

  def stop() {
    if (connected) {
      client.removeLocationUpdates(this)
      client.disconnect()
      connected = false
    }
  }

  override def onConnected(data: Bundle) {
    connected = true
    client.requestLocationUpdates(req, this)
    f(Connected())
  }

  override def onDisconnected() {
    connected = false
    f(Disconnected())
  }

  override def onConnectionFailed(result: ConnectionResult) {
    if (result.hasResolution) {
      try {
        result.startResolutionForResult(ctx.get, 9000)
      } catch {
        case e: IntentSender.SendIntentException =>
          f(Errored(e))
      }
    } else {
      PlayServices.showErrorDialog(result.getErrorCode)
    }
  }

  override def onLocationChanged(loc: Location) {
    f(LocationUtils.fromAndroidLocation(loc))
  }
}
