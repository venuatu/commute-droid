package me.venuatu.commute.views

import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import macroid.{Contexts, IdGeneration}

class BaseActivity extends ActionBarActivity with Contexts[ActionBarActivity] with IdGeneration with ViewMixins {
  def ctx = this

  private var _visible = false
  def visible = _visible

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
  }

  override def onStart() {
    super.onStart()
    _visible = true
  }

  override def onStop() {
    super.onStop()
    _visible = false
  }
}