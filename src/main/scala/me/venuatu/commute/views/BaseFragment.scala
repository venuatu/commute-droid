package me.venuatu.commute.views

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view._
import macroid.{Contexts, IdGeneration}

class BaseFragment(layout: Int = -1, menuLayout: Int = -1) extends Fragment with Contexts[Fragment]
    with IdGeneration with ViewMixins {
  def ctx = getActivity.asInstanceOf[BaseActivity]

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