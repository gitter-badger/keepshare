import android.Keys._

name := "keepshare"

libraryDependencies ++= Seq(
  "com.google.apis" % "google-api-services-drive" % "v2-rev130-1.18.0-rc" intransitive(),
  "com.google.api-client" % "google-api-client-android" % "1.18.0-rc" intransitive(),
  "com.google.api-client" % "google-api-client" % "1.18.0-rc" intransitive(),
  "com.google.http-client" % "google-http-client" % "1.18.0-rc" intransitive(),
  "com.google.http-client" % "google-http-client-gson" % "1.18.0-rc" intransitive(),
  "com.google.http-client" % "google-http-client-android" % "1.18.0-rc" intransitive(),
  "com.google.oauth-client" % "google-oauth-client" % "1.18.0-rc" intransitive(),
  "com.hanhuy" %% "android-common" % "0.3-SNAPSHOT",
  "com.google.code.findbugs" % "jsr305" % "2.0.1",
  "com.google.code.gson" % "gson" % "2.2.4",
  "com.android.support" % "support-v4" % "19.1.0",
  "com.google.android.gms" % "play-services" % "4.4.52"
)

proguardOptions in Android += "-keepclassmembers class scala.runtime.RichInt { ** until(); }"

run <<= run in android.Keys.Android
