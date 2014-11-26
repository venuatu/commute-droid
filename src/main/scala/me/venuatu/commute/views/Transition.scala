package me.venuatu.commute.views

import android.animation.TimeInterpolator
import android.os.SystemClock
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.animation.AnimationUtils
import macroid.{AppContext, Ui}

import scala.concurrent.{Promise, Future}
import scala.concurrent.duration.Duration

object Transition {
  def apply(interpolator: TimeInterpolator) =
    new Transition(interpolator)

  def apply(res: Int)(implicit ctx: AppContext) =
    new Transition(AnimationUtils.loadInterpolator(ctx.get, res))

  implicit class MultipliableRange(range: Range) {
    def *(value: Float): Float =
      (range.end - range.start) * value + range.start

    def *|(value: Float) =
      (range * value).toInt
  }

  object CancelAnimation extends Exception {}
}

class Transition(interpolator: TimeInterpolator) {
  def apply(duration: Duration, actions: (Float => Ui[Any])*): Promise[Unit] = {
    val choreo = Choreographer.getInstance()
    val start = SystemClock.uptimeMillis()
    val total = duration.toMillis
    val promise = Promise[Unit]()

    val runner: FrameCallback = new FrameCallback {
      override def doFrame(nanos: Long) {
        val millis = nanos / 1000000
        val time = millis - start
        val progress = interpolator.getInterpolation(Math.min(1.toFloat, time.toFloat / total))
//        println(s"animation: ${time.toFloat / total} $progress")

        for (action <- actions) {
          action(progress).run
        }
        if (!promise.isCompleted) {
          if (time < total)
            choreo.postFrameCallback(this)
          else
            promise.success()
        }
      }
    }
    choreo.postFrameCallback(runner)
    promise
  }
}
