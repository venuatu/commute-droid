package me.venuatu.commute.views

import android.content.Context
import android.graphics.{Color, Paint, Canvas}
import android.util.DisplayMetrics
import android.view.{WindowManager, View}
import android.view.View.MeasureSpec
import macroid._
import macroid.FullDsl._
import me.venuatu.commute.web.Commute.{Trip, TripLeg}

class TripView(ctx: Context) extends View(ctx) with Contexts[View] {
  var trip: Trip = null
  var color = Color.BLACK
  lazy val density = {
    val display = ctx.getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager].getDefaultDisplay
    val metrics = new DisplayMetrics
    display.getMetrics(metrics)
    metrics.density
  }

  def dp(i: Int) = i * density

  override def onDraw(canvas: Canvas) {
    val padding = dp(48) /2
    val height = getHeight - padding

    val brush = new Paint()
    val middle = getWidth / 2

    brush.setARGB(0, 0, 0, 0)
    canvas.drawRect(0, 0, getWidth, getHeight, brush)
    brush.setAntiAlias(true)

    val length = trip.duration
    var cumulativeLength = 0f

    var last = (0f, 0f, 0)

    trip.legs.foreach(leg => {
      val x = getWidth / 2
      val y = cumulativeLength / length * getHeight + padding

      brush.clearShadowLayer()
      brush.setColor(last._3)
      brush.setStrokeWidth(dp(4))
      canvas.drawLine(last._1, last._2, x, y, brush)

      brush.setShadowLayer(getWidth / 2 + dp(15), 0, 0, 0xFF555555)
      brush.setColor(getColour(leg.transport))
      canvas.drawCircle(x, y, getWidth / 2, brush)
      cumulativeLength += leg.duration

      last = (x, y, color)
    })
  }

  def setTrip(theTrip: Trip) {
    trip = theTrip

    invalidate()
  }

  def getColour(theType: String): Int = theType match {
    case "BUS" =>   0xFFFFFF00
    case "RAIL" =>  0xFF0000FF
    case "WALK" =>  0xFF00FFFF
    case "FERRY" => 0xFFADD8E6

    case _ =>       0xFFD3D3D3
//    WALK CAR TRANSIT BUS RAIL FERRY WAIT
  }

  override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

    val widthMode = MeasureSpec.getMode(widthMeasureSpec)
    val widthSize = MeasureSpec.getSize(widthMeasureSpec)
    val heightMode = MeasureSpec.getMode(heightMeasureSpec)
    val heightSize = MeasureSpec.getSize(heightMeasureSpec)

    setMeasuredDimension(widthSize, heightSize)
  }
}
