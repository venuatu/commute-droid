import android.Keys._
import android.Dependencies.{LibraryDependency, aar}

android.Plugin.androidBuild

platformTarget in Android := "android-21"

name := "commute"

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
  aar("org.macroid" %% "macroid" % "2.0.+"),
  aar("com.google.android.gms" % "play-services" % "6.+"),
  aar("com.android.support" % "support-v4" % "+"),
  aar("com.android.support" % "appcompat-v7" % "+"),
  "com.android.support" % "recyclerview-v7" % "+",
  "com.android.support" % "cardview-v7" % "+",
  "com.android.support" % "gridlayout-v7" % "+",
  "com.squareup.okhttp" % "okhttp" % "2.+",
  "com.squareup.picasso" % "picasso" % "2.+",
  "com.squareup.okhttp" % "okhttp-urlconnection" % "2.+",
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
  "-keep class scala.Dynamic",
  "-keep class me.venuatu.commute.**",
  "-keep class android.support.v7.**"
)

proguardCache in Android ++= Seq(
  ProguardCache("spray") % "io.spray" %% "spray-json_2.11.0-RC4",
  ProguardCache("macroid") % "org.macroid" %% "macroid",
  ProguardCache("com.google.android.gms") % "com.google.android.gms" % "play-services",
//  ProguardCache("android.support") % "com.android.support" % "gridlayout-v7",
  ProguardCache("android.support") % "com.android.support" % "support-v4",
//  ProguardCache("android.support") % "com.android.support" % "appcompat-v7",
//  ProguardCache("android.support") % "com.android.support" % "cardview-v7",
  ProguardCache("android.support") % "com.android.support" % "recyclerview-v7"
)
