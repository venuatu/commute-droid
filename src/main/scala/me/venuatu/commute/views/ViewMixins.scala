package me.venuatu.commute.views

import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.TextView
import macroid.FullDsl._
import macroid._

trait ViewMixins {

  def asRunnable(block: => Unit): Runnable = {
    new Runnable {
      override def run(): Unit = {
        block
      }
    }
  }

  def isLollipop =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

  def textStyle(res: Int)(implicit ctx: ActivityContext) =
    Tweak[TextView]{_.setTextAppearance(ctx.get, res)}

  def gravity(res: Int) =
    Tweak[TextView]{_.setGravity(res)}

  def attrDrawable(res: Int)(implicit ctx: ActivityContext): Drawable = {
    val attrs = Array[Int](res)
    val ta = ctx.get.obtainStyledAttributes(attrs)

    val drawableFromTheme = ta.getDrawable(0)
    ta.recycle()

    drawableFromTheme
  }
}
