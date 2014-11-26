package me.venuatu.commute

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.ViewGroup.LayoutParams
import android.view.Window
import android.widget.{Toast, FrameLayout, LinearLayout}
import macroid.FullDsl._
import macroid._
import me.venuatu.commute.misc.Tracker
import spray.json._, DefaultJsonProtocol._

class MainActivity extends views.BaseActivity {
  var toolbar: Toolbar = null
  var location: Tracker.Location = null
  var tracker: Tracker = null

  override def onCreate(savedInstanceState: Bundle) = {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
    super.onCreate(savedInstanceState)
    toolbar = new Toolbar(ctx.ctx)
    toolbar.setBackground(new ColorDrawable(getResources.getColor(R.color.primary)))
//    toolbar.setMinimumHeight(getResources.getDimensionPixelSize(R.attr.actionBarSize))
    setSupportActionBar(toolbar)
    setProgressBarIndeterminate(true)
    val view = getUi(
      l[LinearLayout](
        Ui(toolbar) <~ lp[FrameLayout](LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        l[FrameLayout]() <~ id(Id.content_view) <~ lp[FrameLayout](LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
      ) <~ vertical
    )
    setContentView(view)

    getSupportFragmentManager.beginTransaction()
      .replace(Id.content_view, new LocationListFragment)
      .commit()
  }

  def requestLocationUpdate() {
    if (tracker == null)
      tracker = new Tracker(Tracker.Priority("high", 1, 1), {
        case loc: Tracker.Location =>
          Toast.makeText(ctx, loc.toJson.toString(), Toast.LENGTH_SHORT).show()
          if (loc.accuracy < 50) {
            location = loc
            pushLocation()
            tracker.stop()
          }
        case event =>
          Log.d("commute.tracker", event.toString)
      })
    if (location != null && location.time + 1 * 60 * 1000 > System.currentTimeMillis()) {
      pushLocation()
      tracker.stop()
    } else {
      tracker.start()
    }
  }

  private def pushLocation() = {
    getSupportFragmentManager.findFragmentById(Id.content_view) match {
      case view: TravelFragment =>
        view.updateLocation(location)
      case _ =>
    }
  }
}
