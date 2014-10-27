package me.venuatu.commute.web

import android.net.Uri
import scala.concurrent.Future
import com.google.android.gms.maps.model.LatLng
import com.squareup.okhttp.Request
import spray.json._, DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global

object Flickr {
  val KEY = "267a74d2bf3a20f8fbd061e9116b5256"
  val SECRET = "b2ae1f1553ba7690"

  val URI = Uri.parse("https://api.flickr.com/services/rest/")

  private def createUri(params: Seq[(String, String)] = Seq()) = {
    var uri = URI.buildUpon()
      .appendQueryParameter("api_key", KEY)
      .appendQueryParameter("format", "json")
      .appendQueryParameter("nojsoncallback", "1")
    for (param <- params) {
      uri = uri.appendQueryParameter(param._1, param._2)
    }
    uri.build().toString
  }

  //      { "id": "4925145599", "owner": "8454450@N08", "secret": "4ab34a1a52", "server": "4081", "farm": 5, "title": "St. George Street Fountain", "ispublic": 1, "isfriend": 0, "isfamily": 0 },
  case class PhotoResult(id: String, owner: String, secret: String, server: String, farm: Int, title: String, public: Int) {
    def url =
      s"https://farm$farm.staticflickr.com/$server/${id}_${secret}_z.jpg"
  }
  case class PhotoPage(page: Int, pages: Int, perPage: Int, total: String, photo: Seq[PhotoResult])
  case class SearchResult(photos: PhotoPage, status: String)

  implicit val photoResultFmt = jsonFormat(PhotoResult, "id", "owner", "secret", "server", "farm", "title", "ispublic")
  implicit val photoPageFmt = jsonFormat(PhotoPage, "page", "pages", "perpage", "total", "photo")
  implicit val searchResultFmt = jsonFormat(SearchResult, "photos", "stat")

  def searchAround(location: LatLng): Future[Seq[PhotoResult]] = {
    // https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=f1f22be1aefedbeedca3a97a77e8bfb5&lat=-33.862896&lon=151.207323&radius=2&radius_units=km&format=json&nojsoncallback=1
    // &auth_token=72157648988696015-fe323c93bace255b&api_sig=fc13759b9f3dad9a6c38f92b81765589

    val req = new Request.Builder()
      .url(createUri(Seq(
        "method" -> "flickr.photos.search",
        "radius" -> "2",
        "radius_units" -> "km",
        "sort" -> "interestingness-desc",
        "lat" -> location.latitude.toString,
        "lon" -> location.longitude.toString
      )))
      .build()

    WebRequest.doRequest(req).map{data =>
      val result = data.body().string().parseJson
      result.convertTo[SearchResult].photos.photo
    }
  }

}
