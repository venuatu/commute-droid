package me.venuatu.commute.misc

import java.io.{File, FileWriter}

import android.app.{AlarmManager, PendingIntent}
import android.content.{BroadcastReceiver, Context, Intent}
import android.location.Location
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesClient.{ConnectionCallbacks, OnConnectionFailedListener}
import com.google.android.gms.location._

object LocationSaver {
  var lastLocation: Tracker.Location = null
  var req: LocationRequest = null
  var client: LocationClient = null
}

class LocationSaver extends BroadcastReceiver with ConnectionCallbacks with OnConnectionFailedListener with LocationListener {
  var ctx: Context = null
  lazy val pi = PendingIntent.getBroadcast(ctx, 0, new Intent(ctx, this.getClass), PendingIntent.FLAG_CANCEL_CURRENT)

  override def onReceive(context: Context, intent: Intent) {
    ctx = context.getApplicationContext
    if (Seq("android.intent.action.BOOT_COMPLETED", "android.intent.action.MY_PACKAGE_REPLACED").contains(intent.getAction)) {
      val alarm = ctx.getSystemService(Context.ALARM_SERVICE).asInstanceOf[AlarmManager]
      alarm.cancel(pi)
      alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_HALF_DAY, AlarmManager.INTERVAL_HALF_DAY, pi)
      writeToFile(s"s,alarmRegistered,${intent.getAction}")
    }
    start()
    if (intent.hasExtra(LocationClient.KEY_LOCATION_CHANGED)) {
      val loc = intent.getExtras.get(LocationClient.KEY_LOCATION_CHANGED).asInstanceOf[Location]
      onLocationChanged(loc)
    }
  }

  val INTERVAL = 60 * 60 * 1000
  val MIN_INTERVAL = 5 * 1000

  def start() {
    if (LocationSaver.req == null) {
      LocationSaver.req = LocationRequest.create()
      LocationSaver.req.setPriority(LocationRequest.PRIORITY_NO_POWER)
      LocationSaver.req.setInterval(INTERVAL)
      LocationSaver.req.setFastestInterval(MIN_INTERVAL)
      LocationSaver.client = new LocationClient(ctx, this, this)
    }
    if (!(LocationSaver.client.isConnected || LocationSaver.client.isConnecting)) {
      LocationSaver.client.connect()
      writeToFile("s,start")
    }
  }

  def stop() {
    if (LocationSaver.client.isConnected) {
      LocationSaver.client.removeLocationUpdates(pi)
      LocationSaver.client.disconnect()
      writeToFile("s,stop")
    }
  }

  override def finalize() {
    //writeToFile("status,finalize")
  }

  override def onConnected(data: Bundle) {
    LocationSaver.client.requestLocationUpdates(LocationSaver.req, pi)
    writeToFile("s,onConnected")
  }

  override def onDisconnected() {
    LocationSaver.client.removeLocationUpdates(this)
    writeToFile("s,onDisconnected")
  }

  override def onConnectionFailed(result: ConnectionResult) {
    writeToFile(s"s,onConnectionFailed,$result")
  }

  def writeToFile(str: String) {
    val file = new File(ctx.getFilesDir, "locations.txt")
    val writer = new FileWriter(file, true)
    writer.append(str + "\n")
    writer.close()
  }

  def onLocationChanged(loc: Location) {
    val now = System.currentTimeMillis()
    val lastLoc = LocationSaver.lastLocation
    val location = Tracker.LocationUtils.fromAndroidLocation(loc)
    if ((lastLoc == null || (loc.getTime - lastLoc.time > MIN_INTERVAL &&
        !Tracker.badLatLngNear(lastLoc.lat, location.lat, lastLoc.lng, location.lng, 30)))) {
      writeToFile(s"l,${location.time},${location.lat},${location.lng},${location.accuracy}")
      LocationSaver.lastLocation = location
    }
  }
}
