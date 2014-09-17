package me.venuatu.background_tracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import macroid.FullDsl._

class MainActivity extends BaseActivity {
  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    val view = l[LinearLayout](
      f[MapFragment].framed(Id.mapFragment, Tag.mapFragment)
    )
    setContentView(getUi(view))
  }
}
