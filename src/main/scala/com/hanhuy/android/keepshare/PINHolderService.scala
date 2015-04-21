package com.hanhuy.android.keepshare

import com.hanhuy.android.common.{ServiceBus, LogcatTag, AndroidConversions, RichLogger}
import RichLogger._
import AndroidConversions._

import android.app.{Notification, PendingIntent, Service}
import android.content.{IntentFilter, Context, BroadcastReceiver, Intent}
import javax.crypto.spec.{PBEKeySpec, SecretKeySpec}
import javax.crypto.{SecretKey, SecretKeyFactory}
import android.os.Handler
import android.support.v4.app.NotificationCompat

object Notifications {
  val NOTIF_FOUND = 0
  val NOTIF_DATABASE_UNLOCKED = 1
  val NOTIF_CREDENTIALS_READY = 2
}
object PINHolderService {
  var instance = Option.empty[PINHolderService]

  val EXTRA_PIN = "com.hanhuy.android.keepshare.extra.PIN"
  val ACTION_CANCEL = "com.hanhuy.android.keepshare.action.PIN_CANCEL"

  val PIN_VERIFIER = EXTRA_PIN

  def keyFor(pin: String): SecretKey = {
    val spec = new PBEKeySpec(pin.toCharArray,
      "com.hanhuy.android.keepshare".getBytes("utf-8"), 1000, 256)
    val kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    // must convert the KEY or else IV failure on 4.2 and below
    new SecretKeySpec(kf.generateSecret(spec).getEncoded, KeyManager.ALG)
  }
}

/** Makes a best effort at holding the user's PIN-key in memory for
  * the requested amount of time.
  */
class PINHolderService extends Service {
  import PINHolderService._
  implicit private val TAG = LogcatTag("PINHolderService")

  private val handler = new Handler

  lazy val settings = Settings(this)

  def ping(): Unit = {
    handler.removeCallbacks(finishRunner)
    handler.postDelayed(finishRunner,
      settings.get(Settings.PIN_TIMEOUT) * 60 * 1000)
  }
  def pinKey: SecretKey = {
    ping()
    _key
  }
  private var _key: SecretKey = _

  def onBind(p1: Intent) = null

  override def onCreate() {
    instance = Some(this)
    ServiceBus.send(PINServiceStart)
  }

  override def onDestroy() {
    Database.close()
    instance = None
    ServiceBus.send(PINServiceExit)
  }

  val finishRunner: Runnable = () => {
    unregisterReceiver(receiver)
    stopForeground(true)
    stopSelf()
  }

  override def onStartCommand(intent: Intent, flags: Int, startId: Int) = {
    val pin = intent.getStringExtra(EXTRA_PIN)
    _key = keyFor(pin)
    val builder = new NotificationCompat.Builder(this)
      .setPriority(Notification.PRIORITY_MIN)
      .setContentText(getString(R.string.pin_holder_notif_text))
      .setContentTitle(getString(R.string.pin_holder_notif_title, getString(R.string.appname)))
      .setSmallIcon(R.drawable.ic_lock)
      .setContentIntent(PendingIntent.getBroadcast(
      this, 0, new Intent(ACTION_CANCEL), PendingIntent.FLAG_UPDATE_CURRENT))
    ping()
    startForeground(Notifications.NOTIF_DATABASE_UNLOCKED, builder.build)
    registerReceiver(receiver, ACTION_CANCEL)
    Service.START_NOT_STICKY
  }

  val receiver = new BroadcastReceiver {
    override def onReceive(c: Context, i: Intent) {
      handler.removeCallbacks(finishRunner)
      finishRunner.run()
    }
  }
}
