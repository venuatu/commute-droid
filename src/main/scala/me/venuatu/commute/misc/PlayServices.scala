package me.venuatu.commute.misc

import android.support.v4.app.FragmentActivity
import com.google.android.gms.common.{ConnectionResult, ErrorDialogFragment, GooglePlayServicesUtil}
import macroid.ActivityContext

object PlayServices {
  val CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000

  def isAvailable(implicit ctx: ActivityContext) = {
    GooglePlayServicesUtil.isGooglePlayServicesAvailable(ctx.get) match {
      case ConnectionResult.SUCCESS =>
        true
      case e =>
        showErrorDialog(e)
        false
    }
  }

  def showErrorDialog(errno: Int)(implicit ctx: ActivityContext) {
    val errorDialog = GooglePlayServicesUtil.getErrorDialog(errno, ctx.get, CONNECTION_FAILURE_RESOLUTION_REQUEST)
    val errorFragment = ErrorDialogFragment.newInstance(errorDialog)
    errorFragment.show(ctx.get.asInstanceOf[FragmentActivity].getFragmentManager, "Activity Recognition")
  }
}
