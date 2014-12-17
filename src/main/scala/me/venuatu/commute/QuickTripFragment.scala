package me.venuatu.commute

import java.io.File

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.app.Activity
import android.graphics.{Color, PorterDuff, Bitmap}
import android.graphics.drawable.{BitmapDrawable, Drawable}
import android.os.{Build, Bundle}
import android.renderscript._
import android.support.v4.app.{FragmentPagerAdapter, Fragment, FragmentManager, FragmentStatePagerAdapter}
import android.support.v4.view.{PagerTabStrip, ViewPager}
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.{LinearLayoutManager, CardView, RecyclerView, StaggeredGridLayoutManager}
import android.util.{Log, DisplayMetrics}
import android.view.View.{OnClickListener, OnLongClickListener, OnTouchListener}
import android.view.ViewGroup.LayoutParams._
import android.view.animation.AccelerateDecelerateInterpolator
import android.view._
import android.widget._
import com.google.android.gms.maps.model.LatLng
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.{Target, Picasso}
import macroid.FullDsl._
import macroid.contrib.LpTweaks._
import macroid.{Transformer, Tweak}
import me.venuatu.commute.misc.Tracker
import me.venuatu.commute.views.{TripStopView, Transition}
import me.venuatu.commute.web.Commute.Trip
import me.venuatu.commute.web.{Commute, StreetView, Flickr}
import spray.json._, DefaultJsonProtocol._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object QuickTripFragment {
  def towards(location: (String, LatLng), from: LatLng = null): TravelFragment = {
    val frag = new TravelFragment
    val data = new Bundle()
    data.putString("name", location._1)
    data.putParcelable("destination", location._2)
    if (from != null)
      data.putParcelable("from", from)

    frag.setArguments(data)
    frag
  }
}

class QuickTripFragment extends views.BaseFragment() {
//
//  var name: String = null
//  var destination: LatLng = null
//  lazy val streetView = new StreetView
//  var image = slot[ImageView]
//  var recycler = slot[RecyclerView]
//  var tabStrip = slot[PagerTabStrip]
//  val trips = collection.mutable.ArrayBuffer[Trip]()
//  lazy val adapter = new ListAdapter
//
//  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, bundle: Bundle) = {
//    super.onCreateView(inflater, container, bundle)
//    val layout = l[RelativeLayout](
//      w[ImageView] <~ lp[RelativeLayout](MATCH_PARENT, MATCH_PARENT) <~ wire(image),
//      w[RecyclerView] <~ lp[RelativeLayout](MATCH_PARENT, MATCH_PARENT) <~ wire(recycler)
//    ) <~ lp[LinearLayout](MATCH_PARENT, MATCH_PARENT)
//    val view = getUi(layout)
//    val data = if (bundle == null) getArguments else bundle
//    if (data != null) {
//      name = data.getString("name")
//      destination = data.getParcelable("destination")
//    }
//    image.get.setColorFilter(0xffaaaaaa, PorterDuff.Mode.MULTIPLY)
//    image.get.setScaleType(ImageView.ScaleType.CENTER_CROP)
//    implicit val picasso = Picasso.`with`(ctx)
//    streetView.getBlurredImage(destination) onCompleteUi {
//      case Success(file: File) => picasso.load(file).into(image.get)
//      case Failure(e) => e.printStackTrace()
//    }
//
//    ctx.asInstanceOf[MainActivity].requestLocationUpdate()
//
//    val lm = new LinearLayoutManager(ctx)
//    lm.setOrientation(0)
//    recycler.get.setLayoutManager(lm)
//    recycler.get.setAdapter(adapter)
//
//    view
//  }
//
//  override def onResume() {
//    super.onResume()
//    ctx.setTitle(name)
//  }
//
//  case class Holder(text: TextView, trip: TripStopView, view: View) extends ViewHolder(view)
//
//  class ListAdapter extends RecyclerView.Adapter[Holder] {
//    override def onCreateViewHolder(viewGroup: ViewGroup, i: Int): Holder = {
//      var text = slot[TextView]
//      var image = slot[ImageView]
//      var line = slot[Bitmap]
//
//      Holder(text.get, trip.get, view)
//    }
//
//    override def getItemCount: Int =
//      trips.length
//
//    override def onBindViewHolder(vh: Holder, i: Int): Unit = ???
//  }
//
//  def updateLocation(loc: Tracker.Location) = {
//    ctx.setProgressBarIndeterminateVisibility(true)
//    Commute.pullTrip(new LatLng(loc.lat, loc.lng), destination, departing = true) onCompleteUi {
//      case Success(pulled) =>
//        pulled.foreach {trip => trips += trip}
//        ctx.setProgressBarIndeterminateVisibility(false)
//        pagerAdapter.notifyDataSetChanged()
//      case Failure(e) =>
//        e.printStackTrace()
//        Toast.makeText(ctx, e.toString, Toast.LENGTH_LONG).show()
//        ctx.onBackPressed()
//    }
//  }
//
//  override def onSaveInstanceState(data: Bundle) {
//    super.onSaveInstanceState(data)
//    data.putString("name", name)
//    data.putParcelable("destination", destination)
//  }

}
