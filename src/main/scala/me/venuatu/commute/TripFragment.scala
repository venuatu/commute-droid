package me.venuatu.commute

import java.util.Calendar

import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v7.appcompat.R.style
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.support.v7.widget.RecyclerView.ViewHolder
import android.util.Log
import android.view.ViewGroup.LayoutParams._
import android.view.{LayoutInflater, Gravity, ViewGroup, View}
import android.widget.{RelativeLayout, ImageView, TextView, LinearLayout}
import com.google.android.gms.maps.model.LatLng
import macroid.{Transformer, Tweak}
import me.venuatu.commute.misc.Tracker
import me.venuatu.commute.views.{TripStopView, BaseFragment}
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
  var recycler = slot[RecyclerView]
  var location: Tracker.Location = null

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, bundle: Bundle) = {
    super.onCreateView(inflater, container, bundle)
    if (bundle != null) {
      trip = bundle.getSerializable("trip").asInstanceOf[Trip]
    } else {
      trip = getArguments.getSerializable("trip").asInstanceOf[Trip]
    }

    val view = getUi(
      w[RecyclerView]() <~ wire(recycler)
    )
    val lm = new LinearLayoutManager(ctx)
    lm.setOrientation(1)
    recycler.get.setLayoutManager(lm)
    recycler.get.setAdapter(new ListAdapter)

    view
  }

//  override def onRestoreInstanceState(bundle: Bundle) {
//    super.onRestoreInstanceState(bundle)
//    trip = bundle.getSerializable("trip").asInstanceOf[Trip]
//  }

  override def onSaveInstanceState(bundle: Bundle) {
    super.onSaveInstanceState(bundle)
    bundle.putSerializable("trip", trip)
  }

  case class Holder(text: TextView, trip: TripStopView, view: View) extends ViewHolder(view)

  class ListAdapter extends RecyclerView.Adapter[Holder] {
    lazy val offset = {
      Calendar.getInstance().getTimeZone.getOffset(0)
    }

    override def onCreateViewHolder(group: ViewGroup, viewType: Int): Holder = {
      var text = slot[TextView]
      var tripview = slot[TripStopView]
      val view = getUi(
        l[LinearLayout](
          w[TripStopView] <~ wire(tripview) <~ lp[LinearLayout](64.dp, MATCH_PARENT),
          w[TextView] <~ wire(text) <~ textStyle(style.TextAppearance_AppCompat_Body1)
            <~ padding(bottom = 8.dp)
            <~ lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT)
        ) <~ lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT) <~ horizontal
      )

      //      text.get.setBackground(attrDrawable(R.attr.selectableItemBackground))
      Holder(text.get, tripview.get, view)
    }

    override def onBindViewHolder(p1: Holder, p2: Int) {
      if (p2 < trip.legs.length) {
        val leg = trip.legs(p2)
        p1.text.setText(Seq(
          leg.transport + " " + leg.routeId.toJson.prettyPrint.replace("null", "") + " " +
              (System.currentTimeMillis() - offset - leg.startTime),
          leg.from.name + " " + offset,
          leg.distance + "m " + leg.duration + "s"
        ).mkString("\n"))
        p1.trip.setLeg(leg)
      } else {
        val leg = trip.legs(p2 -1)
        p1.text.setText(Seq(
          leg.to.name,
          (System.currentTimeMillis() - offset - leg.endTime) + "s"
        ).mkString("\n"))
        p1.trip.setLeg(null)
      }
    }

    override def getItemCount: Int = {
      trip.legs.length +1
    }
  }
}
