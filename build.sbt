import android.Keys._
import android.Dependencies.{LibraryDependency, aar}

android.Plugin.androidBuild

platformTarget in Android := "android-19"

name := "hidden-tracker"

scalaVersion := "2.11.1"

run <<= run in Android

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "jcenter" at "http://jcenter.bintray.com",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

scalacOptions in (Compile, compile) ++=
  (dependencyClasspath in Compile).value.files.map("-P:wartremover:cp:" + _.toURI.toURL)

scalacOptions in (Compile, compile) ++= Seq(
  "-P:wartremover:traverser:macroid.warts.CheckUi"
)

libraryDependencies ++= Seq(
  aar("org.macroid" %% "macroid" % "2.0.0-M3"),
  aar("com.google.android.gms" % "play-services" % "5.0.89"),
//  aar("com.android.support" % "support-v4" % "20.0.0"),
//  "joda-time" % "joda-time" % "2.4",
  "io.spray" % "spray-json_2.11.0-RC4" % "1.2.6",
  compilerPlugin("org.brianmckenna" %% "wartremover" % "0.10")
)

proguardScala in Android := true

proguardOptions in Android ++= Seq(
  "-ignorewarnings",
  "-dontobfuscate",
  "-dontoptimize",
  "-dontwarn org.joda.convert.**",
  "-keep class scala.Dynamic"
)

proguardCache in Android ++= Seq(
  ProguardCache("spray") % "io.spray" %% "spray-json_2.11.0-RC4",
  ProguardCache("com.google.android.gms") % "com.google.android.gms" % "play-services"
)
