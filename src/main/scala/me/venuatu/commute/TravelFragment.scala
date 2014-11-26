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
import android.support.v7.appcompat.R.style
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
import me.venuatu.commute.views.Transition
import me.venuatu.commute.web.Commute.Trip
import me.venuatu.commute.web.{Commute, StreetView, Flickr}
import spray.json._, DefaultJsonProtocol._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object TravelFragment {
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

class TravelFragment extends views.BaseFragment() {

  var name: String = null
  var destination: LatLng = null
  lazy val streetView = new StreetView
  var image = slot[ImageView]
  var pager = slot[ViewPager]
  var tabStrip = slot[PagerTabStrip]
  lazy val pagerAdapter = new Pager(getChildFragmentManager)
  val trips = collection.mutable.ArrayBuffer[Trip]()
  var tracker: Tracker = null

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, bundle: Bundle) = {
    super.onCreateView(inflater, container, bundle)
    val layout = l[RelativeLayout](
      w[ImageView] <~ lp[RelativeLayout](MATCH_PARENT, MATCH_PARENT) <~ wire(image),
      l[LinearLayout](
        l[ViewPager]() <~ lp[RelativeLayout](MATCH_PARENT, MATCH_PARENT) <~ id(Id.pager) <~ wire(pager)
      ) <~ lp[LinearLayout](MATCH_PARENT, MATCH_PARENT) <~ vertical
    ) <~ lp[LinearLayout](MATCH_PARENT, MATCH_PARENT)
    val view = getUi(layout)
    val data = if (bundle == null) getArguments else bundle
    if (data != null) {
      name = data.getString("name")
      destination = data.getParcelable("destination")
    }
    image.get.setColorFilter(0xffaaaaaa, PorterDuff.Mode.MULTIPLY)
    image.get.setScaleType(ImageView.ScaleType.CENTER_CROP)
    implicit val picasso = Picasso.`with`(ctx)
    streetView.getBlurredImage(destination) onCompleteUi {
      case Success(file: File) => picasso.load(file).into(image.get)
      case Failure(e) => e.printStackTrace()
    }

    tracker = new Tracker(Tracker.Priority("high", 1, 1), {
      case loc: Tracker.Location =>
        updateLocation(loc)
        Log.d("travel.tracker", loc.toString)
      case e =>
        Log.d("travel.tracker", e.toString)
    })

    pager.get.setAdapter(pagerAdapter)
    tabStrip = Some(new PagerTabStrip(ctx))
    val tlp = new ViewPager.LayoutParams()
    tlp.height = WRAP_CONTENT
    tlp.width = MATCH_PARENT
    tlp.gravity = Gravity.TOP
    tabStrip.get.setLayoutParams(tlp)
    pager.get.addView(tabStrip.get, tlp)

    view
  }

  override def onResume() {
    super.onResume()
    ctx.setTitle(name)
  }

  override def onStop() {
    super.onStop()
    tracker.stop()
  }

  def updateLocation(loc: Tracker.Location) = {
    Toast.makeText(ctx, loc.toJson.toString(), Toast.LENGTH_SHORT).show()
    if (loc.accuracy < 50/*m*/) {
      tracker.stop()
      ctx.setProgressBarIndeterminateVisibility(true)
      Commute.pullTrip(new LatLng(loc.lat, loc.lng), destination, departing = true) onCompleteUi {
        case Success(pulled) =>
          pulled.foreach {trip => trips += trip}
          ctx.setProgressBarIndeterminateVisibility(false)
          pagerAdapter.notifyDataSetChanged()
        case Failure(e) =>
          e.printStackTrace()
          Toast.makeText(ctx, e.toString, Toast.LENGTH_LONG).show()
          ctx.onBackPressed()
      }
    }
  }


  class Pager(fm: FragmentManager) extends FragmentPagerAdapter(fm) {
    override def getItem(position: Int): Fragment = {
      TripFragment.fromTrip(trips(position))
    }

    override def getCount: Int = trips.length

    override def getPageTitle(position: Int): CharSequence = {
      trips(position).mainTransport
    }
  }

  override def onSaveInstanceState(data: Bundle) {
    super.onSaveInstanceState(data)
    data.putString("name", name)
    data.putParcelable("destination", destination)
  }

}
