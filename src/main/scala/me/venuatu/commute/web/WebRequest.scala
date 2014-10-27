package me.venuatu.commute.web

import java.io.IOException
import scala.concurrent.{Future, Promise}
import com.squareup.okhttp.{Callback, OkHttpClient, Request, Response}

object WebRequest {
  lazy val client = new OkHttpClient()

  def doRequest(req: Request): Future[Response] = {
    val promise = Promise[Response]()
    client.newCall(req).enqueue(new Callback {
      override def onResponse(response: Response): Unit = promise.success(response)
      override def onFailure(request: Request, e: IOException): Unit = promise.failure(e)
    })
    promise.future
  }
}
