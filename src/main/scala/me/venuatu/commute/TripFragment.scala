package me.venuatu.commute

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
import me.venuatu.commute.views.BaseFragment
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

  case class Holder(text: TextView, view: View) extends ViewHolder(view)

  class ListAdapter extends RecyclerView.Adapter[Holder] {

    override def onCreateViewHolder(group: ViewGroup, viewType: Int): Holder = {
      var text = slot[TextView]
      val view = getUi(
        l[LinearLayout](
          w[TextView] <~ wire(text) <~ textStyle(style.TextAppearance_AppCompat_Body1)
            <~ lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT)
        ) <~ lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT)
      )

      //      text.get.setBackground(attrDrawable(R.attr.selectableItemBackground))
      Holder(text.get, view)
    }

    override def onBindViewHolder(p1: Holder, p2: Int) {
      p1.text.setText(
        trip.legs(p2).toJson.prettyPrint
      )
    }

    override def getItemCount: Int = {
      trip.legs.length
    }
  }
}
