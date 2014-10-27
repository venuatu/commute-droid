package me.venuatu.commute

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
import android.view.ViewGroup.LayoutParams
import android.view.{Gravity, MotionEvent, View, ViewGroup}
import android.widget.{FrameLayout, LinearLayout, TextView}
import com.google.android.gms.maps.model.LatLng
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.{Target, Picasso}
import macroid.FullDsl._
import macroid.contrib.LpTweaks._
import macroid.{Transformer, Tweak}
import me.venuatu.commute.web.Flickr

import scala.util.{Failure, Success}


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
      w[RecyclerView] <~ wire(recycler) <~ lp[FrameLayout](LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    )
    view = getUi(layout)

    val metrics = new DisplayMetrics()
    ctx.getWindowManager.getDefaultDisplay.getMetrics(metrics)
    columns = Math.max(1, metrics.widthPixels / 480.dp)
    val manager = new StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)

    recycler.get.setLayoutManager(manager)
    recycler.get.setAdapter(new ListAdapter)

  }

  case class Holder(text: TextView, location: TextView, card: LinearLayout, view: View) extends ViewHolder(view)

  class ListAdapter extends RecyclerView.Adapter[Holder] {

    override def onCreateViewHolder(group: ViewGroup, viewType: Int): Holder = {
      var card = slot[LinearLayout]
      var text = slot[TextView]
      var location = slot[TextView]
      val view = getUi(
        l[LinearLayout](
          l[LinearLayout](
            w[TextView] <~ wire(text) <~ wire(location) <~ textStyle(style.TextAppearance_AppCompat_Title)
//            w[TextView] <~ wire(location) <~ hide
          ) <~ vertical <~ matchWidth <~
            lp[LinearLayout](LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        ) <~ padding(top = 16 dp, bottom = 16 dp) <~ wire(card)
          <~ Transformer {
          case t: TextView => t <~ gravity(Gravity.CENTER) <~ padding(all = 16 dp) <~
            lp[LinearLayout](LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
      )

      val params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
      if (columns > 1)
        params.setMargins(8 dp, 8 dp, 8 dp, 8 dp)
      else
        params.setMargins(8 dp, 4 dp, 8 dp, 4 dp)
      val cardview = card.get
      cardview.setLayoutParams(params)
//      cardview.setForeground(attrDrawable(R.attr.selectableItemBackground))
//      cardview.setMaxCardElevation(4)
//      card.get.setBackground(new ColorDrawable(0xff282828))
//      cardview.setCardElevation(1)
//      cardview.setShadowPadding(16 dp, 16 dp, 16 dp, 16 dp)
      Holder(text.get, location.get, card.get, view)
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
            location = (view.getY, h.location.getPaddingBottom)
          val height = recycler.get.getHeight - h.card.getHeight - 8.dp + h.location.getPaddingBottom
          val yPosition = - view.getTop + recycler.get.getScrollY + 4.dp
          println(yPosition)
          view.animate().translationY(yPosition).setUpdateListener(new AnimatorUpdateListener {
            override def onAnimationUpdate(anim: ValueAnimator): Unit = {
              val currheight = (anim.getAnimatedFraction * height).toInt
//              println(s"${anim.getAnimatedFraction}, $currheight, $height")
              h.location.setPadding(h.location.getPaddingLeft, h.location.getPaddingTop,
                h.location.getPaddingRight, currheight)
            }
          })
        } else if (p2.getAction == MotionEvent.ACTION_CANCEL || p2.getAction == MotionEvent.ACTION_UP) {
          print("up")
          reset(view)
        } else {
          println("MotionEvent", p2.getAction)
        }
        false
      }

      override def onClick(view: View) = reset(view)
      override def onLongClick(view: View): Boolean = false //{ reset(view, 800); true }

      def reset(view: View, delay: Int = 15) {
        print("click up")
        view.postDelayed(asRunnable {
          if (isLollipop) {
            view.animate().translationZ(0)
          } else {
//            view.asInstanceOf[CardView].setCardElevation(4)
          }
          if (location != null) {
            val base: Int = if (location != null) location._2.toInt else 24.dp
            val height: Float = h.location.getPaddingBottom - base
            view.animate().translationY(0).setUpdateListener(new AnimatorUpdateListener {
              override def onAnimationUpdate(anim: ValueAnimator): Unit = {
                val currheight = ((1 - anim.getAnimatedFraction) * height).toInt
//                println(s"${anim.getAnimatedFraction}, $currheight, $base, $height")
                h.location.setPadding(h.location.getPaddingLeft, h.location.getPaddingTop,
                  h.location.getPaddingRight, currheight + base)
              }
            })
          }
        }, 16 * delay)
      }
    }

    lazy val picasso = {
      val pic = Picasso.`with`(ctx)
      pic.setIndicatorsEnabled(true)
      pic
    }

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
      h.card.setBackgroundColor(0xff282828)

      val url = s"https://maps.googleapis.com/maps/api/streetview?fov=60&size=$dimensions&location=${item._2.latitude},${item._2.longitude}"
      println(item._1, url)
      picasso.load(url).into(new Target {
        override def onBitmapFailed(errorDrawable: Drawable): Unit = {
          println("bitmap failed ", item._1)
          h.card.setBackground(errorDrawable)
          h.card.setBackgroundColor(0xff87ceeb)
        }

        override def onPrepareLoad(placeHolderDrawable: Drawable): Unit = {
//          h.card.setBackground(placeHolderDrawable)
//          h.card.setBackgroundColor(0xff404040)
        }

        override def onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom): Unit = {
          val rs = RenderScript.create(ctx)
          val start = System.currentTimeMillis()
          val input = Allocation.createFromBitmap(rs, bitmap)
          val output = Allocation.createTyped(rs, input.getType)
          val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

          blur.setRadius(4f)
          blur.setInput(input)
          blur.forEach(output)

          output.copyTo(bitmap)
          val drawable = new BitmapDrawable(bitmap)
          drawable.setGravity(Gravity.CENTER | Gravity.FILL_VERTICAL| Gravity.FILL_HORIZONTAL
              | Gravity.CLIP_HORIZONTAL | Gravity.CLIP_VERTICAL)
          drawable.setColorFilter(0xff777777, PorterDuff.Mode.MULTIPLY)
          h.card.setBackground(drawable)
          println(s"${item._1} rendered in ${System.currentTimeMillis() - start}")
        }
      })
    }

    override def getItemCount: Int = {
      DATA.length
    }
  }


}
