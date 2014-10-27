package me.venuatu.commute

import java.io.File

import android.graphics.Color
import android.os.Bundle
import android.util.{Log, TypedValue}
import android.widget.FrameLayout
import com.google.android.gms.maps.model._
import com.google.android.gms.maps.{CameraUpdateFactory, GoogleMapOptions, SupportMapFragment}
import macroid.FullDsl.{l, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object MapFragment {
  // So that the camera location survives a screen rotation
  var camera: CameraPosition = CameraPosition.fromLatLngZoom(new LatLng(-34.41339665061414,150.88401697576046), 13.91082f)
}

class MapFragment extends views.BaseFragment() {
  val CAMERA_LOCATION_STR = "cameralocation"

  var mapFragment: SupportMapFragment = null
  def map = mapFragment.getMap

  val mapOptions = new GoogleMapOptions().useViewLifecycleInFragment(true)
    .camera(MapFragment.camera).compassEnabled(false).tiltGesturesEnabled(false)

  override def onCreate(data: Bundle) {
    super.onCreate(data)
    val layout = l[FrameLayout]() <~ id(Id.content_view)
    view = getUi(layout)

    if (!misc.PlayServices.isAvailable) return
    mapFragment = SupportMapFragment.newInstance(mapOptions)
    getChildFragmentManager.beginTransaction()
      .replace(Id.content_view, mapFragment)
      .commit()
    if (data != null && data.containsKey(CAMERA_LOCATION_STR)) {
      map.animateCamera(CameraUpdateFactory.newCameraPosition(data.getParcelable(CAMERA_LOCATION_STR)))
    }
  }

  override def onSaveInstanceState(data: Bundle) {
    super.onSaveInstanceState(data)
    data.putParcelable(CAMERA_LOCATION_STR, map.getCameraPosition)
  }

  override def onResume() {
    super.onResume()
    if (mapFragment == null) {
      if (!misc.PlayServices.isAvailable) return
      getActivity.recreate()
    }
    map.setBuildingsEnabled(false)
    map.setIndoorEnabled(false)
//    map.setMyLocationEnabled(true)
    pullData()
  }

  override def onPause() {
    super.onPause()
    if (mapFragment == null) return
  }

  lazy val lineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, ctx.getResources.getDisplayMetrics)

  case class SimpleLocation(time: Long, lat: Double, lng: Double, accuracy: Double) {
    def toTrackerLocation = {
      misc.Tracker.Location(lat, lng, accuracy, 0, time)
    }
  }

  def pullData() = {
    Log.d("background_tracker", s"reading data from file main")

    Future {
      val file = new File(ctx.getFilesDir, "locations.txt")
      if (file.exists()) {
        val start = System.currentTimeMillis()
        Log.d("background_tracker", s"reading data from file")
        val ret = io.Source.fromFile(file).getLines().filter{_.startsWith("l,")}.map {line =>
          val data = line.split(',')
          SimpleLocation(data(1).toLong, data(2).toDouble, data(3).toDouble, data(4).toDouble)
        }.toSeq
        Log.d("background_tracker", s"read ${ret.length} lines in: " + (System.currentTimeMillis() - start))
        ret
      } else {
        Seq()
      }
    } onCompleteUi {
      case Success(data) =>
        if (data.size > 0) {
          val start = System.currentTimeMillis()
          map.clear()
          var bounds: LatLngBounds = null
          val polyOptions = new PolylineOptions().color(Color.BLUE).width(3)
          var lastLoc: SimpleLocation = null
          data.foreach { loc =>
            if (lastLoc == null || (!misc.Tracker.badLatLngNear(lastLoc.lat, loc.lat, lastLoc.lng, loc.lng, 50))) {

              val latlng = new LatLng(loc.lat, loc.lng)
              map.addCircle(new CircleOptions().center(latlng).radius(loc.accuracy)
                .fillColor(Color.argb(70, 100, 149, 237)).strokeWidth(0).zIndex(0))
              polyOptions.add(latlng)

              bounds = if (bounds == null) new LatLngBounds(latlng, latlng) else bounds.including(latlng)
            }
            lastLoc = loc
          }
          map.addPolyline(polyOptions)
          map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20))
          Log.d("background_tracker", "pushed onto map in: " + (System.currentTimeMillis() - start))
        }
      case Failure(e) =>
        e.printStackTrace()
    }
  }
}