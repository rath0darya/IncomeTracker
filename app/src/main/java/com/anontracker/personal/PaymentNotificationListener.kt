package com.anontracker.personal
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
class PaymentNotificationListener:NotificationListenerService(){
 override fun onNotificationPosted(sbn:StatusBarNotification){
  val prefs=getSharedPreferences("tracker",MODE_PRIVATE)
  if(!prefs.getBoolean("qr_session_active",false)) return
  val e=sbn.notification.extras
  val title=e.getCharSequence("android.title")?.toString().orEmpty()
  val body=e.getCharSequence("android.text")?.toString().orEmpty()
  val text=title+" "+body
  val pkg=sbn.packageName.lowercase()
  val supported=listOf("phonepe","paytm","gpay","google","bhim","bank").any{pkg.contains(it)}
  val received=Regex("received|credited|deposited",RegexOption.IGNORE_CASE).containsMatchIn(text)
  val amount=Regex("(?:₹|rs\\.?\\s*)([0-9,]+(?:\\.[0-9]{1,2})?)",RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)?.replace(",","")?.toDoubleOrNull()
  if(!supported||!received||amount==null||amount<=0)return
  val reference="NOTICE-"+UUID.nameUUIDFromBytes((sbn.key+text).toByteArray()).toString()
  CoroutineScope(Dispatchers.IO).launch{
   TrackerDb.get(applicationContext).incomeDao().insert(IncomeTransaction(amount=amount,payer=title.ifBlank{"Verify payer"},reference=reference,note="Notification match; verify in your UPI/bank app.",receivedAt=sbn.postTime,status="PENDING",source="NOTIFICATION"))
  }
 }
}
