package me.venuatu.commute.views

import android.content.Context
import android.graphics.{Color, Paint, Canvas}
import android.util.DisplayMetrics
import android.view.{WindowManager, View}
import android.view.View.MeasureSpec
import macroid._
import macroid.FullDsl._
import me.venuatu.commute.web.Commute.TripLeg

class TripStopView(ctx: Context) extends View(ctx) with Contexts[View] {
  var leg: TripLeg = null
  var color = Color.BLACK
  lazy val density = {
    val display = ctx.getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager].getDefaultDisplay
    val metrics = new DisplayMetrics
    display.getMetrics(metrics)
    metrics.density
  }

  val offset = dp(5)

  setLayerType(View.LAYER_TYPE_SOFTWARE, null)

  def dp(i: Int) = i * density

  override def onDraw(canvas: Canvas) {
    val brush = new Paint()
    val middle = getWidth / 2

    brush.setARGB(0, 0, 0, 0)
    canvas.drawRect(0, 0, getWidth, getHeight - offset, brush)
    brush.setAntiAlias(true)

    brush.setColor(color)

    if (leg != null) {
      brush.clearShadowLayer()
      canvas.drawRect(middle - dp(3), 0, middle + dp(3), getHeight - offset, brush)
    }

//    brush.setColor(0xFFcccccc)
    brush.setShadowLayer(dp(5), 0, 0, 0xFF333333)
    canvas.drawCircle(middle, getHeight - getWidth / 3 - offset, getWidth / 3, brush)

  }

  def setLeg(Leg: TripLeg) {
    leg = Leg
    if (leg == null) {
      color = 0xffdddddd
    } else {
      color = leg.transport match {
        case "BUS" => Color.YELLOW
        case "RAIL" => Color.BLUE
        case "TRAM" => 0xFF800000
        case "FERRY" => Color.CYAN
        case "CAR" => Color.RED
        case "WALK" => 0xffffffff
        case _ => Color.WHITE
      }
    }

    invalidate()
  }

  override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

    val widthMode = MeasureSpec.getMode(widthMeasureSpec)
    val widthSize = MeasureSpec.getSize(widthMeasureSpec)
    val heightMode = MeasureSpec.getMode(heightMeasureSpec)
    val heightSize = MeasureSpec.getSize(heightMeasureSpec)

    setMeasuredDimension(widthSize, heightSize)
  }
}
