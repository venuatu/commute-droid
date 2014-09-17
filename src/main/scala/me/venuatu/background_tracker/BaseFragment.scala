package me.venuatu.background_tracker

import android.app.Fragment
import android.os.Bundle
import android.view._
import macroid.{Contexts, IdGeneration}

class BaseFragment(layout: Int = -1, menuLayout: Int = -1) extends Fragment with Contexts[Fragment] with IdGeneration {
  def ctx = getActivity.asInstanceOf[BaseActivity]

  def onUiThread(block: => Unit) {
    try {
      ctx.onUiThread(block)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  override def onCreate(instanceState: Bundle) {
    super.onCreate(instanceState)
    setHasOptionsMenu(menuLayout != -1)
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(menuLayout, menu)
  }

  protected var view: View = null
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup,
                            instanceState: Bundle): View = {
    if (view == null && layout != -1) {
      view = inflater.inflate(layout, container, false)
    }
    view
  }
}