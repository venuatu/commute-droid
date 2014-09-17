package me.venuatu.background_tracker

import java.io.{BufferedReader, InputStreamReader, FileInputStream, File}

import android.graphics.Color
import android.os.Bundle
import android.util.{Log, TypedValue}
import android.widget.{Toast, FrameLayout}
import com.google.android.gms.maps.{CameraUpdateFactory, GoogleMapOptions}
import com.google.android.gms.maps.model._
import macroid.FullDsl._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object MapFragment {
  // So that the camera location survives a screen rotation
  var camera: CameraPosition = CameraPosition.fromLatLngZoom(new LatLng(-34.41339665061414,150.88401697576046), 13.91082f)
}

class MapFragment extends BaseFragment() {
  var mapFragment: com.google.android.gms.maps.MapFragment = null
  def map = mapFragment.getMap

  val mapOptions = new GoogleMapOptions().useViewLifecycleInFragment(true)
    .camera(MapFragment.camera).compassEnabled(false).tiltGesturesEnabled(false)

  override def onCreate(instanceState: Bundle) {
    super.onCreate(instanceState)
    val layout = l[FrameLayout]() <~ id(Id.content_view)
    view = getUi(layout)

    if (!PlayServices.isAvailable) return
    mapFragment = com.google.android.gms.maps.MapFragment.newInstance(mapOptions)
    getChildFragmentManager.beginTransaction()
      .replace(Id.content_view, mapFragment)
      .commit()
  }

  override def onResume() {
    super.onResume()
    if (mapFragment == null) {
      if (!PlayServices.isAvailable) return
      getActivity.recreate()
    }
    map.setBuildingsEnabled(false)
    map.setIndoorEnabled(false)
    pullData()
  }

  override def onPause() {
    super.onPause()
    if (mapFragment == null) return
  }

  lazy val lineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, ctx.getResources.getDisplayMetrics)

  case class SimpleLocation(time: Long, lat: Double, lng: Double, accuracy: Double) {
    def toTrackerLocation = {
      Tracker.Location(lat, lng, accuracy, 0, time)
    }
  }

  def pullData() = {
    Future {
      val file = new File(ctx.getFilesDir, "locations.json")
      if (file.exists()) {
        val start = System.currentTimeMillis()
        val ret = io.Source.fromFile(file).getLines().filter{_.startsWith("l,")}.map {line =>
          val data = line.split(',')
          SimpleLocation(data(1).toLong, data(2).toDouble, data(3).toDouble, data(4).toDouble)
        }.filter {_.accuracy < 500}.toSeq
        Log.d("background_tracker", s"read ${ret.length} lines in: " + (System.currentTimeMillis() - start))
        ret
      } else {
        Seq()
      }
    } onComplete {
      case Success(data) => onUiThread {
        if (data.size > 0) {
          val start = System.currentTimeMillis()
          map.clear()
          var bounds: LatLngBounds = null
          val polyOptions = new PolylineOptions().color(Color.BLUE).width(3)
          var lastLoc: SimpleLocation = null
          data.foreach { loc =>
            if (lastLoc == null || (!Tracker.badLatLngNear(lastLoc.lat, loc.lat, lastLoc.lng, loc.lng, 50))) {

              val latlng = new LatLng(loc.lat, loc.lng)
              map.addCircle(new CircleOptions().center(latlng).radius(loc.accuracy)
                .fillColor(Color.argb(30, 100, 149, 237)).strokeWidth(0).zIndex(0))
              polyOptions.add(latlng)

              bounds = if (bounds == null) new LatLngBounds(latlng, latlng) else bounds.including(latlng)
            }
            lastLoc = loc
          }
          map.addPolyline(polyOptions)
          map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20))
          Log.d("background_tracker", "pushed onto map in: " + (System.currentTimeMillis() - start))
        }
      }
      case Failure(e) =>
        e.printStackTrace()
    }
  }
}