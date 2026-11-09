package com.anontracker.personal
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
private val mint=Color(0xFF9BF2C5)
private val night=Color(0xFF07110F)
private val panel=Color(0xFF14251F)
class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);WindowCompat.setDecorFitsSystemWindows(window,false)
  val dao=TrackerDb.get(this).incomeDao()
  setContent{
   var tab by remember{mutableIntStateOf(0)}
   var dark by remember{mutableStateOf(true)}
   var upi by remember{mutableStateOf(getSharedPreferences("tracker",0).getString("upi","")?:"")}
   var payee by remember{mutableStateOf(getSharedPreferences("tracker",0).getString("payee","Anon")?:"Anon")}
   var session by remember{mutableStateOf(getSharedPreferences("tracker",0).getBoolean("qr_session_active",false))}
   var add by remember{mutableStateOf(false)}
   val entries by dao.observeAll().collectAsState(initial=emptyList())
   val fg=if(dark) Color.White else Color(0xFF10231B)
   val bg=if(dark)night else Color(0xFFF1F6F3)
   MaterialTheme(colorScheme=if(dark) darkColorScheme(primary=mint,background=bg,surface=panel) else lightColorScheme(primary=Color(0xFF176B48),background=bg,surface=Color.White)){
    Column(Modifier.fillMaxSize().background(bg).padding(WindowInsets.systemBars.asPaddingValues())){
     Column(Modifier.weight(1f)){
      Text("ANON TRACKER",color=mint,fontSize=11.sp,letterSpacing=3.sp,modifier=Modifier.padding(start=20.dp,top=14.dp))
      Text(listOf("Your income, in focus","Transactions","Income intelligence","Settings")[tab],fontSize=25.sp,fontWeight=FontWeight.Bold,color=fg,modifier=Modifier.padding(horizontal=20.dp,vertical=8.dp))
      when(tab){
       0->Home(upi,payee,entries,session,fg,{tab=3},{session=!session;getSharedPreferences("tracker",0).edit().putBoolean("qr_session_active",session).apply()})
       1->Ledger(entries,fg,{add=true},{item->lifecycleScope.launch{dao.update(item.copy(status="CONFIRMED"))}})
       2->Insights(entries,fg)
       3->Prefs(upi,payee,dark,fg,{upi=it;getSharedPreferences("tracker",0).edit().putString("upi",it).apply()},{payee=it;getSharedPreferences("tracker",0).edit().putString("payee",it).apply()},{dark=it},{startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))})
      }
     }
     NavigationBar(containerColor=panel.copy(alpha=.96f),modifier=Modifier.padding(horizontal=12.dp).clip(RoundedCornerShape(25.dp))){
      val icons=listOf(Icons.Rounded.Home,Icons.Rounded.ReceiptLong,Icons.Rounded.Insights,Icons.Rounded.Settings)
      listOf("Home","Transactions","Insights","Settings").forEachIndexed{i,s->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icons[i],null)},label={Text(s,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=mint,selectedTextColor=mint,indicatorColor=mint.copy(alpha=.12f)))}
     }
     Text("Developed With ♥️ AnonVox",color=fg.copy(alpha=.5f),fontSize=10.sp,modifier=Modifier.align(Alignment.CenterHorizontally).padding(7.dp))
    }
    if(add) AddDialog({add=false}){amount,payer,note->lifecycleScope.launch{dao.insert(IncomeTransaction(amount=amount,payer=payer,reference="MANUAL-"+UUID.randomUUID(),note=note,receivedAt=System.currentTimeMillis()))};add=false}
   }
  }
 }
}
@Composable private fun CardBox(content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxWidth().padding(vertical=6.dp).clip(RoundedCornerShape(23.dp)).background(panel).border(1.dp,Color.White.copy(alpha=.08f),RoundedCornerShape(23.dp)).padding(17.dp),content=content)}
@Composable private fun Home(upi:String,payee:String,items:List<IncomeTransaction>,session:Boolean,fg:Color,setup:()->Unit,toggle:()->Unit){
 val total=items.filter{it.status=="CONFIRMED"}.sumOf{it.amount}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=17.dp,vertical=5.dp)){
  item{CardBox{Text("CONFIRMED INCOME",color=mint,fontSize=10.sp,letterSpacing=2.sp);Text("₹"+String.format("%,.2f",total),fontSize=34.sp,fontWeight=FontWeight.Bold,color=fg);Text(items.size.toString()+" recorded entries",color=fg.copy(alpha=.6f))}}
  item{CardBox{
   Text("YOUR UPI QR",color=mint,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
   if(upi.isNotBlank()){val bitmap=remember(upi,payee){qr("upi://pay?pa="+Uri.encode(upi)+"&pn="+Uri.encode(payee)+"&cu=INR")};Image(bitmap.asImageBitmap(),null,Modifier.size(210.dp).align(Alignment.CenterHorizontally).background(Color.White).padding(8.dp));Text(upi,color=fg,modifier=Modifier.align(Alignment.CenterHorizontally))}else{Text("Configure your UPI ID to generate your QR.",color=fg);Button(onClick=setup){Text("Set up UPI")}}
   Button(onClick=toggle,modifier=Modifier.fillMaxWidth()){Text(if(session)"End QR session" else "Start QR payment session")}
   Text(if(session)"Session active · matched alerts require verification" else "Start a session when showing your QR.",color=fg.copy(alpha=.55f),fontSize=11.sp)
  }}
 }
}
private fun qr(value:String):Bitmap{val m=QRCodeWriter().encode(value,BarcodeFormat.QR_CODE,500,500);return Bitmap.createBitmap(500,500,Bitmap.Config.ARGB_8888).also{b->for(x in 0 until 500)for(y in 0 until 500)b.setPixel(x,y,if(m[x,y])android.graphics.Color.BLACK else android.graphics.Color.WHITE)}}
@Composable private fun Ledger(items:List<IncomeTransaction>,fg:Color,add:()->Unit,verify:(IncomeTransaction)->Unit){
 Column(Modifier.fillMaxSize().padding(horizontal=17.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Payment ledger",color=fg,modifier=Modifier.weight(1f));Button(onClick=add){Icon(Icons.Rounded.Add,null);Text("Record")}}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(5.dp)){items(items,key={it.id}){t->CardBox{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(t.payer,color=fg,fontWeight=FontWeight.Bold);Text(SimpleDateFormat("dd MMM yyyy · hh:mm a",Locale.getDefault()).format(Date(t.receivedAt)),color=fg.copy(alpha=.55f),fontSize=11.sp)};Text("+₹"+String.format("%.2f",t.amount),color=mint,fontWeight=FontWeight.Bold)};Text(t.status+" · "+t.source,color=Color(0xFFD8B978),fontSize=10.sp);if(t.status=="PENDING")TextButton(onClick={verify(t)}){Text("Verify & confirm")};if(t.note.isNotBlank())Text(t.note,color=fg.copy(alpha=.65f),fontSize=11.sp);Text("Ref: "+t.reference,color=fg.copy(alpha=.4f),fontSize=9.sp)}}}
 }
}
@Composable private fun Insights(items:List<IncomeTransaction>,fg:Color){
 val valid=items.filter{it.status=="CONFIRMED"};val now=System.currentTimeMillis()
 val day=valid.filter{now-it.receivedAt<86400000L}.sumOf{it.amount}
 val week=valid.filter{now-it.receivedAt<604800000L}.sumOf{it.amount}
 val month=valid.filter{Date(it.receivedAt).month==Date(now).month&&Date(it.receivedAt).year==Date(now).year}.sumOf{it.amount}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=17.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Metric("Today",day,fg)};item{Metric("Last 7 days",week,fg)};item{Metric("This month",month,fg)}
  item{CardBox{Text("HOURLY EARNINGS",color=mint,fontWeight=FontWeight.Bold);val bars=(0..23).map{h->valid.filter{Calendar.getInstance().apply{timeInMillis=it.receivedAt}.get(Calendar.HOUR_OF_DAY)==h}.sumOf{it.amount}};val max=(bars.maxOrNull()?:0.0).coerceAtLeast(1.0);Row(Modifier.fillMaxWidth().height(110.dp),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=Alignment.Bottom){bars.forEach{v->Box(Modifier.weight(1f).fillMaxHeight((v/max).toFloat().coerceAtLeast(.02f)).background(mint, RoundedCornerShape(topStart=4.dp,topEnd=4.dp)))}};Text("12 AM                                                  12 PM                                               11 PM",fontSize=9.sp,color=fg.copy(alpha=.5f))}}
 }
}
@Composable private fun Metric(label:String,value:Double,fg:Color){CardBox{Text(label.uppercase(),color=mint,fontSize=10.sp,letterSpacing=2.sp);Text("₹"+String.format("%,.2f",value),fontSize=27.sp,fontWeight=FontWeight.Bold,color=fg)}}
@Composable private fun Prefs(upi:String,payee:String,dark:Boolean,fg:Color,onUpi:(String)->Unit,onPayee:(String)->Unit,onTheme:(Boolean)->Unit,access:()->Unit){
 var u by remember(upi){mutableStateOf(upi)};var p by remember(payee){mutableStateOf(payee)}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=17.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{CardBox{Text("PAYMENT PROFILE",color=mint,fontWeight=FontWeight.Bold);OutlinedTextField(p,{p=it},label={Text("Payee name")},modifier=Modifier.fillMaxWidth());OutlinedTextField(u,{u=it},label={Text("UPI ID")},modifier=Modifier.fillMaxWidth());Button(onClick={onPayee(p.trim());onUpi(u.trim())},modifier=Modifier.fillMaxWidth()){Text("Save & refresh QR")}}}
  item{CardBox{Row(verticalAlignment=Alignment.CenterVertically){Text("Dark theme",color=fg,modifier=Modifier.weight(1f));Switch(dark,onTheme)}}}
  item{CardBox{Text("PAYMENT DETECTION",color=mint,fontWeight=FontWeight.Bold);Text("Enable notification access to detect supported UPI payment alerts only while a QR session is active. Alerts are unverified until you confirm them in your bank or UPI app.",color=fg,fontSize=12.sp);Button(onClick=access,modifier=Modifier.fillMaxWidth()){Text("Configure notification access")}}}
  item{Text("No payments are initiated or handled by Anon Tracker. SMS and email inboxes are not read.",color=fg.copy(alpha=.55f),fontSize=11.sp)}
 }
}
@Composable private fun AddDialog(close:()->Unit,save:(Double,String,String)->Unit){
 var amount by remember{mutableStateOf("")};var payer by remember{mutableStateOf("")};var note by remember{mutableStateOf("")}
 AlertDialog(onDismissRequest=close,title={Text("Record income")},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){OutlinedTextField(amount,{amount=it},label={Text("Amount ₹")});OutlinedTextField(payer,{payer=it},label={Text("Received from")});OutlinedTextField(note,{note=it},label={Text("Work / note")})}},confirmButton={Button(onClick={val a=amount.toDoubleOrNull();if(a!=null&&a>0&&payer.isNotBlank())save(a,payer.trim(),note.trim())},enabled=(amount.toDoubleOrNull()?:0.0)>0&&payer.isNotBlank()){Text("Save")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})
}
