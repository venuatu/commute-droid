package me.venuatu.background_tracker

import android.app.Activity
import android.os.Bundle
import android.view.Window
import macroid.{Contexts, IdGeneration}

class BaseActivity extends Activity with Contexts[Activity] with IdGeneration {
  def ctx = this

  private var _visible = false
  def visible = _visible

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
    setProgressBarIndeterminate(true)
  }

  override def onStart() {
    super.onStart()
    _visible = true
  }

  override def onStop() {
    super.onStop()
    _visible = false
  }

  def onUiThread(block: => Unit) = {
    runOnUiThread(asRunnable({
      try {
        if (visible) {
          block
        }
      } catch {
        case e: NullPointerException => {
          e.printStackTrace()
          recreate()
        }
      }
    }))
  }

  def asRunnable(block: => Unit): Runnable = {
    new Runnable {
      override def run(): Unit = {
        block
      }
    }
  }

  def find[A](id: Int) = {
    findViewById(id).asInstanceOf[A]
  }
}