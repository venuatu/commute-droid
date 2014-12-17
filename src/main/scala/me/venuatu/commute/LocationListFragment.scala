package me.venuatu.commute

import java.io.File

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.app.Activity
import android.graphics.{Color, PorterDuff, Bitmap}
import android.graphics.drawable.{BitmapDrawable, Drawable}
import android.os.{Build, Bundle}
import android.renderscript._
import android.support.v7.appcompat.R.style
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.{CardView, RecyclerView, StaggeredGridLayoutManager}
import android.util.DisplayMetrics
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
import me.venuatu.commute.views.Transition
import me.venuatu.commute.web.{StreetView, Flickr}
import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class LocationListFragment extends views.BaseFragment() {

  val DATA = Seq[(String, LatLng)](
    "Home" ->                           new LatLng(-34.410804, 150.880898),
    "Work" ->                           new LatLng(-34.408460, 150.882353),
    "Coles Fairy Meadow" ->             new LatLng(-34.394601, 150.893009),
    "Deloitte" ->                       new LatLng(-33.862794, 151.207398),
    "Transport for New South Wales" ->  new LatLng(-33.884096, 151.203697),
    "Google Pyrmont" ->                 new LatLng(-33.867095, 151.195876),
    "Palmer St, Artarmon" ->            new LatLng(-33.806742, 151.181688),
    "Bossley Park Public School" ->     new LatLng(-33.858736, 150.882481)
  )

  var recycler = slot[RecyclerView]
  var columns: Int = 1

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, bundle: Bundle) = {
    super.onCreateView(inflater, container, bundle)
    val layout = l[FrameLayout](
      w[RecyclerView] <~ wire(recycler) <~ lp[FrameLayout](MATCH_PARENT, MATCH_PARENT)
    )
    val view = getUi(layout)

    val metrics = new DisplayMetrics()
    ctx.getWindowManager.getDefaultDisplay.getMetrics(metrics)
    columns = Math.max(1, metrics.widthPixels / 480.dp)
    val manager = new StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)

    recycler.get.setLayoutManager(manager)
    recycler.get.setAdapter(new ListAdapter)

    view
  }

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    ctx.setTitle("Commute")
  }

  override def onResume() {
    super.onResume()
    ctx.setTitle("Commute")
  }

  case class Holder(text: TextView, image: ImageView, card: RelativeLayout, view: View, var pos: Int) extends ViewHolder(view)

  class ListAdapter extends RecyclerView.Adapter[Holder] {
    val BASE_HEIGHT = 120.dp
    val interpolator = new AccelerateDecelerateInterpolator()
    val animate = Transition(interpolator)

    override def onCreateViewHolder(group: ViewGroup, viewType: Int): Holder = {
      var card = slot[RelativeLayout]
      var text = slot[TextView]
      var image = slot[ImageView]
      val view = getUi(
        l[RelativeLayout](
          w[ImageView] <~ wire(image) <~ lp[RelativeLayout](MATCH_PARENT, BASE_HEIGHT),
          w[TextView] <~ wire(text) <~ textStyle(style.TextAppearance_AppCompat_Title)
            <~ Tweak[TextView] {_.setTextSize(24)} <~ padding(top = 40.dp, bottom = 40.dp)
            <~ lp[RelativeLayout](MATCH_PARENT, BASE_HEIGHT)
        ) <~ wire(card) <~ lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT)
          <~ Transformer {
          case t: TextView => t <~ gravity(Gravity.CENTER)
        }
      )

      val params = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
      if (columns > 1)
        params.setMargins(1, 1, 1, 1)
      else
        params.setMargins(0, 1, 0, 1)
      card.get.setLayoutParams(params)
//      text.get.setBackground(attrDrawable(R.attr.selectableItemBackground))
      image.get.setColorFilter(0xffaaaaaa, PorterDuff.Mode.MULTIPLY)
      image.get.setScaleType(ImageView.ScaleType.CENTER_CROP)
      Holder(text.get, image.get, card.get, view, 0)
    }

    class ClickListener(h: Holder) extends OnClickListener with OnLongClickListener with OnTouchListener {
      val CLICKED_Z = 15
      var animation: Promise[Unit] = null

      def translationX(value: Float) = Tweak[View] {_.setTranslationX(value)}
      def translationY(value: Float) = Tweak[View] {_.setTranslationY(value)}

      override def onTouch(view: View, p2: MotionEvent): Boolean = {
        if (p2.getAction == MotionEvent.ACTION_DOWN) {
          println("down")
          if (isLollipop) {
            view.setTranslationZ(CLICKED_Z)
          } else {
//            view.asInstanceOf[CardView].setCardElevation(8)
          }
          val height = recycler.get.getHeight
          val yPosition = - view.getTop + recycler.get.getScrollY
          val yRange = Transition.MultipliableRange(h.card.getTranslationY.toInt until yPosition)
          val heightRange = Transition.MultipliableRange(h.image.getHeight until height)

          if (animation != null && !animation.isCompleted) {
            animation.failure(Transition.CancelAnimation)
          }
          animation = animate(250.millis,
            v => view    <~ translationY(yRange * v),
            v => h.image <~ lp[RelativeLayout](MATCH_PARENT, heightRange *| v),
            v => h.text  <~ lp[RelativeLayout](MATCH_PARENT, heightRange *| v)
          )
          animation.future onSuccessUi {
            case _ =>
            getFragmentManager.beginTransaction()
              .replace(Id.content_view, TravelFragment.towards(DATA(h.pos)))
              .addToBackStack(null)
              .commit()
          }
        } else if (p2.getAction == MotionEvent.ACTION_CANCEL){// || p2.getAction == MotionEvent.ACTION_UP) {
          println("cancel")
          reset(view)
        } else {
          println("MotionEvent", p2.getAction)
        }
        false
      }

      override def onClick(view: View) = {
        println("click")
        reset(view)
      }
      override def onLongClick(view: View): Boolean = false //{ reset(view, 800); true }

      def reset(view: View, delay: Int = 8) {
        if (isLollipop) {
          view.animate().translationZ(0)
        } else {
//          view.asInstanceOf[CardView].setCardElevation(4)
        }
        val yRange = Transition.MultipliableRange(h.card.getTranslationY.toInt until 0)
        val heightRange = Transition.MultipliableRange(h.image.getHeight until BASE_HEIGHT)

        if (animation != null && !animation.isCompleted) {
          animation.failure(Transition.CancelAnimation)
        }
        animation = animate(250.millis,
          v => view    <~ translationY(yRange * v),
          v => h.image <~ lp[RelativeLayout](MATCH_PARENT, heightRange *| v),
          v => h.text  <~ lp[RelativeLayout](MATCH_PARENT, heightRange *| v)
        )
      }
    }

    lazy implicit val picasso = Picasso.`with`(ctx)

    lazy val dimensions = s"${recycler.get.getHeight}x${recycler.get.getWidth}"

    override def onBindViewHolder(h: Holder, position: Int) {
      val item = DATA(position)
      h.pos = position

      h.text.setText(item._1)
//      h.location.setText(s"${item._2.latitude} ${item._2.longitude}")
//      holder.card.setRadius(16 dp)
      val clicker = new ClickListener(h)
      h.card.setOnClickListener(clicker)
      h.card.setOnLongClickListener(clicker)
      h.card.setOnTouchListener(clicker)
      val start = System.currentTimeMillis()
      streetView.getBlurredImage(item._2) onCompleteUi {
        case Success(file: File) =>
          picasso.load(file).into(h.image)
          println("load file took: ", System.currentTimeMillis() - start)
        case Failure(e) =>
          e.printStackTrace()
      }
    }

    override def getItemCount: Int = {
      DATA.length
    }
  }

  lazy val streetView = new StreetView
}
