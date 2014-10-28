package me.venuatu.commute

import java.io.File

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
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
import android.view.{Gravity, MotionEvent, View, ViewGroup}
import android.widget._
import com.google.android.gms.maps.model.LatLng
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.{Target, Picasso}
import macroid.FullDsl._
import macroid.contrib.LpTweaks._
import macroid.{Transformer, Tweak}
import me.venuatu.commute.web.{StreetView, Flickr}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class LocationListFragment extends views.BaseFragment() {

  val DATA = Seq[(String, LatLng)](
    "Home" ->     new LatLng(-34.410804, 150.880898),
    "Work" ->     new LatLng(-34.408460, 150.882353),
    "Deloitte" -> new LatLng(-33.862794, 151.207398)
  )

  var recycler = slot[RecyclerView]
  var columns: Int = 1

  override def onCreate(data: Bundle) {
    super.onCreate(data)
    val layout = l[FrameLayout](
      w[RecyclerView] <~ wire(recycler) <~ lp[FrameLayout](MATCH_PARENT, MATCH_PARENT)
    )
    view = getUi(layout)

    val metrics = new DisplayMetrics()
    ctx.getWindowManager.getDefaultDisplay.getMetrics(metrics)
    columns = Math.max(1, metrics.widthPixels / 480.dp)
    val manager = new StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)

    recycler.get.setLayoutManager(manager)
    recycler.get.setAdapter(new ListAdapter)

  }

  case class Holder(text: TextView, image: ImageView, card: RelativeLayout, view: View) extends ViewHolder(view)

  class ListAdapter extends RecyclerView.Adapter[Holder] {

    override def onCreateViewHolder(group: ViewGroup, viewType: Int): Holder = {
      var card = slot[RelativeLayout]
      var text = slot[TextView]
      var image = slot[ImageView]
      val view = getUi(
        l[RelativeLayout](
          w[ImageView] <~ wire(image) <~ lp[RelativeLayout](MATCH_PARENT, 120.dp),
          w[TextView] <~ wire(text) <~ textStyle(style.TextAppearance_AppCompat_Title)
            <~ Tweak[TextView] {_.setTextSize(14.dp)} <~ padding(top = 40.dp, bottom = 40.dp)
            <~ lp[RelativeLayout](MATCH_PARENT, MATCH_PARENT)
        ) <~ wire(card) <~ lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT)
          <~ Transformer {
          case t: TextView => t <~ gravity(Gravity.CENTER)
        }
      )

      val params = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
      if (columns > 1)
        params.setMargins(8 dp, 8 dp, 8 dp, 8 dp)
      else
        params.setMargins(8 dp, 4 dp, 8 dp, 4 dp)
      card.get.setLayoutParams(params)
//      text.get.setBackground(attrDrawable(R.attr.selectableItemBackground))
      image.get.setColorFilter(0xff888888, PorterDuff.Mode.MULTIPLY)
      image.get.setScaleType(ImageView.ScaleType.CENTER_CROP)
      Holder(text.get, image.get, card.get, view)
    }

    class ClickListener(h: Holder) extends OnClickListener with OnLongClickListener with OnTouchListener {
      val CLICKED_Z = 15
      var location: (Float, Float) = null

      override def onTouch(view: View, p2: MotionEvent): Boolean = {
        if (p2.getAction == MotionEvent.ACTION_DOWN) {
          println("down")
          if (isLollipop) {
            view.setTranslationZ(CLICKED_Z)
          } else {
//            view.asInstanceOf[CardView].setCardElevation(8)
          }
          if (location == null)
            location = (view.getY, 120.dp)
          val base = 120.dp
          val height = recycler.get.getHeight - 16.dp - base
          val yPosition = - view.getTop + recycler.get.getScrollY + 8.dp

          view.animate().translationY(yPosition).setUpdateListener(new AnimatorUpdateListener {
            override def onAnimationUpdate(anim: ValueAnimator): Unit = {
              val currheight = (anim.getAnimatedFraction * height).toInt
//              println(s"${anim.getAnimatedFraction}, $currheight, $height")
              val lp = new RelativeLayout.LayoutParams(MATCH_PARENT, currheight + base)
              h.image.setLayoutParams(lp)
              h.text.setLayoutParams(lp)
            }
          })
        } else if (p2.getAction == MotionEvent.ACTION_CANCEL || p2.getAction == MotionEvent.ACTION_UP) {
          println("up")
          reset(view)
        } else {
          println("MotionEvent", p2.getAction)
        }
        false
      }

      override def onClick(view: View) = reset(view)
      override def onLongClick(view: View): Boolean = false //{ reset(view, 800); true }

      def reset(view: View, delay: Int = 15) {
        println("click up")
        view.postDelayed(asRunnable {
          if (isLollipop) {
            view.animate().translationZ(0)
          } else {
//            view.asInstanceOf[CardView].setCardElevation(4)
          }
          if (location != null) {
            val base = 120.dp
            val height: Float = h.image.getHeight - base
            view.animate().translationY(0).setUpdateListener(new AnimatorUpdateListener {
              override def onAnimationUpdate(anim: ValueAnimator): Unit = {
                val currheight = ((1 - anim.getAnimatedFraction) * height).toInt
//                println(s"${anim.getAnimatedFraction}, $currheight, $base, $height")
                val lp = new RelativeLayout.LayoutParams(MATCH_PARENT, currheight + base)
                h.image.setLayoutParams(lp)
                h.text.setLayoutParams(lp)
              }
            })
          }
        }, 16 * delay)
      }
    }

    lazy implicit val picasso = Picasso.`with`(ctx)

    lazy val dimensions = s"${recycler.get.getHeight}x${recycler.get.getWidth}"

    override def onBindViewHolder(h: Holder, position: Int) {
      val item = DATA(position)

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
