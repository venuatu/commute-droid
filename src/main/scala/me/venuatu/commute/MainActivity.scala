package me.venuatu.commute

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.ViewGroup.LayoutParams
import android.view.Window
import android.widget.{FrameLayout, LinearLayout}
import macroid.FullDsl._
import macroid._

class MainActivity extends views.BaseActivity {
  var toolbar: Toolbar = null

  override def onCreate(savedInstanceState: Bundle) = {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
    super.onCreate(savedInstanceState)
    toolbar = new Toolbar(ctx.ctx)
    toolbar.setBackground(new ColorDrawable(getResources.getColor(R.color.primary)))
//    toolbar.setMinimumHeight(getResources.getDimensionPixelSize(R.attr.actionBarSize))
    setSupportActionBar(toolbar)
    setProgressBarIndeterminate(true)
    val view = getUi(
      l[LinearLayout](
        Ui(toolbar) <~ lp[FrameLayout](LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        l[FrameLayout]() <~ id(Id.content_view) <~ lp[FrameLayout](LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
      ) <~ vertical
    )
    setContentView(view)

    getSupportFragmentManager.beginTransaction()
      .replace(Id.content_view, new LocationListFragment)
      .commit()
  }
}
