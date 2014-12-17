package me.venuatu.commute

import java.util.Calendar

import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.appcompat.R.style
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.support.v7.widget.RecyclerView.ViewHolder
import android.text.format.DateUtils
import android.util.{TypedValue, Log}
import android.view.ViewGroup.LayoutParams._
import android.view.{LayoutInflater, Gravity, ViewGroup, View}
import android.widget._
import com.google.android.gms.maps.model.LatLng
import macroid.{Transformer, Tweak}
import me.venuatu.commute.misc.Tracker
import me.venuatu.commute.views.{TripView, TripStopView, BaseFragment}
import me.venuatu.commute.web.Commute
import me.venuatu.commute.web.Commute.Trip
import macroid.FullDsl._
import macroid.contrib.LpTweaks._
import spray.json._, DefaultJsonProtocol._

object TripFragment {
  def fromTrip(trip: Trip): TripFragment = {
    val frag = new TripFragment
    val bundle = new Bundle
    bundle.putSerializable("trip", trip)
    frag.setArguments(bundle)
    frag
  }
}

class TripFragment extends BaseFragment() {

  var trip: Trip = null
  var scrollView = slot[ScrollView]
  var layout = slot[RelativeLayout]
  var location: Tracker.Location = null

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, bundle: Bundle) = {
    super.onCreateView(inflater, container, bundle)
    if (bundle != null) {
      trip = bundle.getSerializable("trip").asInstanceOf[Trip]
    } else {
      trip = getArguments.getSerializable("trip").asInstanceOf[Trip]
    }

    val view = getUi(
      l[ScrollView](
        l[RelativeLayout](
//          w[TripView] <~ wire(tripView) <~ lp[RelativeLayout](48.dp, MATCH_PARENT)
        ) <~ wire(layout)
      ) <~ wire(scrollView)
    )

    val MIN_DISTANCE: Int = (48.dp * 1.6).toInt
    val MAX_DISTANCE: Int = (48.dp * 9 / 3.0 * 2).toInt
    val LENGTH_PER_SECOND = 48.dp * 11 / 3.0 * 2/ 60.0 / 60.0
    val length: Double = trip.duration * LENGTH_PER_SECOND
    var cumulativeLength: Double = 0
    var previousDifference: Double = 0
    var topOffset = -1
    val TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME

    trip.legs.map{leg =>
      // Create a sidebar view and a text view for each trip.leg
      var text = slot[TextView]
      val view = getUi(
        l[LinearLayout](
          w[TextView] <~ wire(text)// <~ textStyle(style.TextAppearance_AppCompat_Small)
            <~ padding(bottom = 8.dp)
            <~ lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT)
        ) <~ lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT) <~ horizontal
      )

      text.get.setTextSize(8.sp)
      text.get.setTextColor(0xff000000)
      val route = leg.route.getOrElse("")

      text.get.setText(Seq(
        leg.transport + " " + route + " " +
          DateUtils.formatDateTime(ctx, leg.startTime, TIME_FLAGS),// + " " + DateUtils.getRelativeTimeSpanString(leg.startTime),
        leg.from.name,// + " " + offset,
        leg.distance + "m " + (leg.duration / 60) + " minutes"
      ).mkString("\n"))

      val difference = Math.min(Math.max(previousDifference, MIN_DISTANCE), MAX_DISTANCE)

      val params = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
      params.leftMargin = 72.dp
      params.topMargin = (length - (cumulativeLength + difference)).toInt

      cumulativeLength = cumulativeLength + difference
      previousDifference = (leg.duration * LENGTH_PER_SECOND).toInt

      (leg, view, params, previousDifference)
    }.sortBy(_._3.topMargin).foreach{case (leg, textview, params, difference) =>

      // Compact the above layout positions for perfection and probably bad math
      val diff = Math.min(Math.max(difference.toInt, MIN_DISTANCE), MAX_DISTANCE)
      val offset = 54.dp

      val icon = new TripStopView(ctx)
      icon.setLeg(leg)
      val iconParams = new RelativeLayout.LayoutParams(72.dp, diff.toInt + icon.offset.toInt)
      iconParams.topMargin = params.topMargin - topOffset - diff.toInt + offset

      if (topOffset == -1) {
        topOffset = params.topMargin - diff.toInt + offset
        iconParams.topMargin = 0
      }

      params.topMargin = params.topMargin - topOffset

      layout.get.addView(textview, params)
      layout.get.addView(icon, iconParams)
    }

    val back = new ColorDrawable(0xDDE9E9E9)
    scrollView.get.setBackground(back)
    scrollView.get.postDelayed(asRunnable{
      scrollView.get.scrollTo(0, scrollView.get.getBottom)
    }, 100)

    view
  }

  override def onResume() = {
    super.onResume()
  }

  override def onSaveInstanceState(bundle: Bundle) {
    super.onSaveInstanceState(bundle)
    bundle.putSerializable("trip", trip)
  }


}
