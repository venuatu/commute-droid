package me.venuatu.commute.web

import java.io._

import android.graphics.{PorterDuff, Bitmap}
import android.graphics.drawable.{BitmapDrawable, Drawable}
import android.net.Uri
import android.renderscript.{Element, ScriptIntrinsicBlur, Allocation, RenderScript}
import android.view.Gravity
import com.google.android.gms.maps.model.LatLng
import com.squareup.okhttp.Request
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.{Picasso, Target}
import macroid._

import scala.annotation.tailrec
import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class StreetView {

  val URL = Uri.parse("https://maps.googleapis.com/maps/api/streetview")

  private val FILE_LOCATION = "/streetview"
  private val FILTERED_PATTERN = ""

  def getBlurredImage(loc: LatLng)(implicit ctx: AppContext, picasso: Picasso): Future[File] = {
    downloadImage(loc) flatMap filterImage(loc)
  }

  def downloadImage(loc: LatLng)(implicit ctx: AppContext): Future[File] = {
    val prom = Promise[File]()
    val file = locToFile(loc)
    if (file.exists()) {
      println(s"$file exists")
      prom.success(file)
    } else {
      val locstring = locToString(loc)
      val url = WebRequest.createUri(URL, Seq(
        "fov" -> "60",
        "size" -> "1024x1024",
        "location" -> locstring
      ))
      val req = new Request.Builder()
        .url(url)
        .build()

      WebRequest.doRequest(req) map { data =>
        val stream = data.body().byteStream()
        file.createNewFile()
        val output = new FileOutputStream(file)
        val buffer = new Array[Byte](4096)
        try {
          copyStream(stream, output, buffer)
        } finally {
          stream.close()
          output.close()
        }
        println(s"$file downloaded")
        prom.success(file)
      } onFailure {
        case e =>
          println(s"$file failed")
          e.printStackTrace()
          prom.failure(e)
      }
    }
    prom.future
  }

  @tailrec
  private def copyStream(in: InputStream, out: OutputStream, buf: Array[Byte]) {
    in.read(buf) match {
      case -1 => ()
      case count =>
        out.write(buf, 0, count)
        copyStream(in, out, buf)
    }
  }

  private def FOLDER(implicit ctx: AppContext) =
    ctx.get.getFilesDir + FILE_LOCATION

  private def ensureFolderExists()(implicit ctx: AppContext) = {
    val folder = new File(FOLDER)
    if (!folder.exists())
      folder.mkdirs()
  }

  private def locToFile(loc: LatLng)(implicit ctx: AppContext) = {
    ensureFolderExists()
    new File(FOLDER, locToString(loc) + ".jpg")
  }

  private def locToBlurFile(loc: LatLng)(implicit ctx: AppContext) = {
    ensureFolderExists()
    new File(FOLDER, locToString(loc) + "-blur.webp")
  }

  private def locToString(loc: LatLng) =
    round(loc.latitude) + "," + round(loc.longitude)

  private def round(n: Double) = {
    val intval = (n * 1000000).toLong
    intval.toDouble / 1000000
  }

  private def filterImage(loc: LatLng)(inputFile: File)(implicit picasso: Picasso, ctx: AppContext): Future[File] = {
    val prom = Promise[File]()
    val file = locToBlurFile(loc)
    if (file.exists()) {
      println(s"$file exists")
      prom.success(file)
    } else {
      var bitmap: Bitmap = null
      try {
        bitmap = picasso.load(inputFile).get()
      } catch {
        case e: Throwable =>
          e.printStackTrace()
          prom.failure(e)
      }
      if (bitmap != null) {
        val rs = RenderScript.create(ctx.app)
        val start = System.currentTimeMillis()
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.getType)
        val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

        blur.setRadius(4f)
        blur.setInput(input)
        blur.forEach(output)

        output.copyTo(bitmap)

        println(s"$file rendered in ${System.currentTimeMillis() - start}")

        file.createNewFile()
        val outputstream = new FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.WEBP, 80, outputstream)

        prom.success(file)
      }
    }
    prom.future
  }
}
