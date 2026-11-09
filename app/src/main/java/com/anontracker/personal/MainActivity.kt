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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private data class Palette(val bg: Color, val bg2: Color, val card: Color, val raised: Color, val text: Color, val muted: Color, val accent: Color, val accentSoft: Color, val violet: Color, val border: Color)
private fun palette(dark: Boolean) = if (dark) Palette(Color(0xFF070B12),Color(0xFF0D1922),Color(0xFF111E2A),Color(0xFF192B3A),Color(0xFFEAF5F5),Color(0xFF91A6B4),Color(0xFF63F5C5),Color(0xFF153D38),Color(0xFFB8A4FF),Color(0xFF2A414D)) else Palette(Color(0xFFF3F7FA),Color(0xFFEAF1F5),Color.White,Color(0xFFE9F0F4),Color(0xFF142632),Color(0xFF607581),Color(0xFF087B62),Color(0xFFDDF4EB),Color(0xFF6554B5),Color(0xFFD1DEE5))

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  WindowCompat.setDecorFitsSystemWindows(window,false)
  val dao=TrackerDb.get(this).incomeDao()
  val prefs=getSharedPreferences("tracker",0)
  setContent {
   var tab by remember { mutableIntStateOf(0) }
   var dark by remember { mutableStateOf(prefs.getBoolean("dark_theme",true)) }
   var upi by remember { mutableStateOf(prefs.getString("upi","") ?: "") }
   var payee by remember { mutableStateOf(prefs.getString("payee","Anon") ?: "Anon") }
   var session by remember { mutableStateOf(prefs.getBoolean("qr_session_active",false)) }
   var add by remember { mutableStateOf(false) }
   val entries by dao.observeAll().collectAsState(initial=emptyList())
   val p=remember(dark) { palette(dark) }
   SideEffect {
    window.statusBarColor=android.graphics.Color.TRANSPARENT
    window.navigationBarColor=android.graphics.Color.TRANSPARENT
    WindowInsetsControllerCompat(window,window.decorView).apply { isAppearanceLightStatusBars=!dark; isAppearanceLightNavigationBars=!dark }
   }
   val colors=if(dark) darkColorScheme(primary=p.accent,onPrimary=Color(0xFF05231C),secondary=p.violet,background=p.bg,surface=p.card,surfaceVariant=p.raised,onBackground=p.text,onSurface=p.text,onSurfaceVariant=p.muted,outline=p.border) else lightColorScheme(primary=p.accent,onPrimary=Color.White,secondary=p.violet,background=p.bg,surface=p.card,surfaceVariant=p.raised,onBackground=p.text,onSurface=p.text,onSurfaceVariant=p.muted,outline=p.border)
   MaterialTheme(colorScheme=colors,typography=Typography().copy(bodyLarge=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace),bodyMedium=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace),bodySmall=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace),titleLarge=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace),titleMedium=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace),titleSmall=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace),labelLarge=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace),labelMedium=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace),labelSmall=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace))) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.bg,p.bg2)))) {
     Column(Modifier.fillMaxSize().padding(WindowInsets.systemBars.asPaddingValues())) {
      Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically) {
       Box(Modifier.size(39.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(p.accent,p.violet))),contentAlignment=Alignment.Center) { Icon(Icons.Rounded.Terminal,null,tint=Color(0xFF07151A),modifier=Modifier.size(23.dp)) }
       Column(Modifier.weight(1f).padding(start=11.dp)) { Text("ANON TRACKER",color=p.text,fontSize=12.sp,fontWeight=FontWeight.Black,letterSpacing=1.8.sp); Text("PERSONAL FINANCE CONSOLE",color=p.muted,fontSize=8.sp,letterSpacing=1.1.sp) }
       Surface(shape=CircleShape,color=p.accentSoft,border=BorderStroke(1.dp,p.accent.copy(alpha=.35f))) { Text("● SECURE MODE",color=p.accent,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp)) }
      }
      Text(listOf("Command Center","Transaction Log","Income Analytics","System Settings")[tab],color=p.text,fontSize=23.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.8).sp,modifier=Modifier.padding(horizontal=20.dp,vertical=5.dp))
      BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
       val icons=listOf(Icons.Rounded.SpaceDashboard,Icons.Rounded.ReceiptLong,Icons.Rounded.QueryStats,Icons.Rounded.Tune)
       val labels=listOf("Home","Transactions","Insights","Settings")
       if(maxWidth>=600.dp) {
        Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically) {
         NavigationRail(containerColor=p.card.copy(alpha=.98f),modifier=Modifier.padding(end=12.dp).clip(RoundedCornerShape(22.dp)).border(1.dp,p.border,RoundedCornerShape(22.dp))) {
          labels.forEachIndexed { i,label ->
           NavigationRailItem(selected=tab==i,onClick={tab=i},icon={Icon(icons[i],null,modifier=Modifier.size(21.dp))},label={Text(label,fontSize=9.sp)},alwaysShowLabel=true,colors=NavigationRailItemDefaults.colors(selectedIconColor=p.accent,selectedTextColor=p.accent,indicatorColor=p.accentSoft,unselectedIconColor=p.muted,unselectedTextColor=p.muted))
          }
         }
         Box(Modifier.weight(1f).fillMaxHeight()) {
          when(tab) {
           0 -> Home(upi,payee,entries,session,p,{tab=3}) { session=!session; prefs.edit().putBoolean("qr_session_active",session).apply() }
           1 -> Ledger(entries,p,{add=true}) { item -> lifecycleScope.launch { dao.update(item.copy(status="CONFIRMED")) } }
           2 -> Insights(entries,p)
           3 -> Prefs(upi,payee,dark,p,{upi=it;prefs.edit().putString("upi",it).apply()},{payee=it;prefs.edit().putString("payee",it).apply()},{dark=it;prefs.edit().putBoolean("dark_theme",it).apply()},{startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))})
          }
         }
        }
       } else {
        Column(Modifier.fillMaxSize()) {
         Box(Modifier.weight(1f).fillMaxWidth()) {
          when(tab) {
           0 -> Home(upi,payee,entries,session,p,{tab=3}) { session=!session; prefs.edit().putBoolean("qr_session_active",session).apply() }
           1 -> Ledger(entries,p,{add=true}) { item -> lifecycleScope.launch { dao.update(item.copy(status="CONFIRMED")) } }
           2 -> Insights(entries,p)
           3 -> Prefs(upi,payee,dark,p,{upi=it;prefs.edit().putString("upi",it).apply()},{payee=it;prefs.edit().putString("payee",it).apply()},{dark=it;prefs.edit().putBoolean("dark_theme",it).apply()},{startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))})
          }
         }
         NavigationBar(containerColor=p.card.copy(alpha=.98f),tonalElevation=0.dp,modifier=Modifier.padding(horizontal=14.dp).clip(RoundedCornerShape(23.dp)).border(1.dp,p.border,RoundedCornerShape(23.dp))) {
          labels.forEachIndexed { i,label ->
           NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icons[i],null,modifier=Modifier.size(21.dp))},label={Text(label,fontSize=9.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=p.accent,selectedTextColor=p.accent,indicatorColor=p.accentSoft,unselectedIconColor=p.muted,unselectedTextColor=p.muted))
          }
         }
        }
       }
      }
      Text("DEVELOPED WITH ♥ ANONVOX",color=p.muted,fontSize=8.sp,letterSpacing=1.6.sp,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=8.dp,bottom=5.dp))
     }
     if(add) AddDialog(p,{add=false}) { amount,payer,note -> lifecycleScope.launch { dao.insert(IncomeTransaction(amount=amount,payer=payer,reference="MANUAL-"+UUID.randomUUID(),note=note,receivedAt=System.currentTimeMillis())) }; add=false }
    }
   }
  }
 }
}

@Composable private fun Eyebrow(value:String,p:Palette) { Text(value.uppercase(),color=p.accent,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.6.sp) }
@Composable private fun Panel(p:Palette,content:@Composable ColumnScope.()->Unit) {
 Column(Modifier.fillMaxWidth().padding(vertical=6.dp).clip(RoundedCornerShape(22.dp)).background(p.card.copy(alpha=.97f)).border(1.dp,p.border,RoundedCornerShape(22.dp)).padding(17.dp),content=content)
}

@Composable private fun Home(upi:String,payee:String,items:List<IncomeTransaction>,session:Boolean,p:Palette,setup:()->Unit,toggle:()->Unit) {
 val total=items.filter { it.status=="CONFIRMED" }.sumOf { it.amount }
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=17.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
  item { Panel(p) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) { Eyebrow("Total confirmed income",p); Text("₹"+String.format("%,.2f",total),color=p.text,fontSize=31.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp); Text(items.size.toString()+" recorded entries",color=p.muted,fontSize=10.sp) }
    Box(Modifier.size(48.dp).clip(CircleShape).background(p.accentSoft),contentAlignment=Alignment.Center) { Icon(Icons.Rounded.AccountBalanceWallet,null,tint=p.accent,modifier=Modifier.size(24.dp)) }
   }
   HorizontalDivider(color=p.border,modifier=Modifier.padding(vertical=12.dp))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("PENDING VERIFICATION",color=p.muted,fontSize=8.sp,letterSpacing=1.sp); Text(items.count { it.status=="PENDING" }.toString().padStart(2,'0'),color=p.violet,fontSize=10.sp,fontWeight=FontWeight.Bold) }
  } }
  item { Panel(p) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) { Eyebrow("Payment interface 01",p); Text("Your UPI QR",color=p.text,fontSize=17.sp,fontWeight=FontWeight.Bold) }
    Surface(color=if(session)p.accentSoft else p.raised,shape=CircleShape) { Text(if(session)"● LIVE" else "○ STANDBY",color=if(session)p.accent else p.muted,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp)) }
   }
   Spacer(Modifier.height(13.dp))
   if(upi.isNotBlank()) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(p.raised,p.accentSoft.copy(alpha=.7f),p.raised))).border(1.dp,p.accent.copy(alpha=.35f),RoundedCornerShape(22.dp)).padding(vertical=17.dp),contentAlignment=Alignment.Center) {
     Box(Modifier.size(248.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).border(1.dp,Color(0xFFE1E8E8),RoundedCornerShape(18.dp)).padding(9.dp),contentAlignment=Alignment.Center) {
      val bitmap=remember(upi,payee) { qr("upi://pay?pa="+Uri.encode(upi)+"&pn="+Uri.encode(payee)+"&cu=INR") }
      Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
     }
    }
    Text(upi,color=p.text,fontSize=12.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=12.dp))
    Text(payee,color=p.muted,fontSize=10.sp,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=3.dp))
    Button(onClick=toggle,modifier=Modifier.fillMaxWidth().padding(top=13.dp).height(49.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=if(session)p.raised else p.accent,contentColor=if(session)p.text else Color(0xFF04231C))) {
     Icon(if(session)Icons.Rounded.StopCircle else Icons.Rounded.Radar,null,modifier=Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if(session)"END PAYMENT SESSION" else "ACTIVATE PAYMENT SESSION",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=.6.sp)
    }
    Text(if(session)"Listening for supported payment alerts · verify every match" else "Activate only while displaying this QR to a customer.",color=p.muted,fontSize=9.sp,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=9.dp))
   } else {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.raised).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
     Icon(Icons.Rounded.QrCode2,null,tint=p.accent,modifier=Modifier.size(42.dp)); Text("QR NOT CONFIGURED",color=p.text,fontWeight=FontWeight.Bold,fontSize=12.sp,modifier=Modifier.padding(top=9.dp)); Text("Add your UPI ID to generate a scan-ready code.",color=p.muted,fontSize=10.sp,modifier=Modifier.padding(top=5.dp)); Button(onClick=setup,modifier=Modifier.padding(top=12.dp),shape=RoundedCornerShape(12.dp)) { Text("CONFIGURE UPI",fontSize=10.sp) }
    }
   }
  } }
  item { Row(Modifier.fillMaxWidth().padding(top=5.dp),verticalAlignment=Alignment.CenterVertically) { Eyebrow("Recent activity",p); Spacer(Modifier.weight(1f)); Text(items.size.toString()+" TOTAL",color=p.muted,fontSize=8.sp) } }
  items.take(3).forEach { entry -> item {
   Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(p.card).border(1.dp,p.border,RoundedCornerShape(15.dp)).padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
    Icon(Icons.Rounded.ArrowDownward,null,tint=p.accent,modifier=Modifier.size(18.dp))
    Column(Modifier.weight(1f).padding(start=10.dp)) { Text(entry.payer,color=p.text,fontSize=11.sp,fontWeight=FontWeight.Bold); Text(entry.status,color=p.muted,fontSize=8.sp) }
    Text("₹"+String.format("%.2f",entry.amount),color=p.accent,fontSize=11.sp,fontWeight=FontWeight.Bold)
   }
  } }
 }
}

private fun qr(value:String):Bitmap {
 val matrix=QRCodeWriter().encode(value,BarcodeFormat.QR_CODE,600,600)
 return Bitmap.createBitmap(600,600,Bitmap.Config.ARGB_8888).also { bitmap -> for(x in 0 until 600) for(y in 0 until 600) bitmap.setPixel(x,y,if(matrix[x,y])android.graphics.Color.BLACK else android.graphics.Color.WHITE) }
}

@Composable private fun Ledger(items:List<IncomeTransaction>,p:Palette,add:()->Unit,verify:(IncomeTransaction)->Unit) {
 Column(Modifier.fillMaxSize().padding(horizontal=17.dp)) {
  Row(Modifier.fillMaxWidth().padding(bottom=8.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Eyebrow("All incoming records",p); Text(items.size.toString()+" transactions",color=p.text,fontSize=12.sp,fontWeight=FontWeight.Bold) }; Button(onClick=add,shape=RoundedCornerShape(13.dp)) { Icon(Icons.Rounded.Add,null,modifier=Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("RECORD",fontSize=9.sp) } }
  LazyColumn(verticalArrangement=Arrangement.spacedBy(3.dp),contentPadding=PaddingValues(bottom=12.dp)) {
   items(items,key={it.id}) { t -> Panel(p) {
    Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(t.payer,color=p.text,fontWeight=FontWeight.Bold,fontSize=12.sp); Text(SimpleDateFormat("dd MMM yyyy · hh:mm a",Locale.getDefault()).format(Date(t.receivedAt)),color=p.muted,fontSize=9.sp) }; Text("+₹"+String.format("%.2f",t.amount),color=p.accent,fontWeight=FontWeight.Bold,fontSize=13.sp) }
    Row(Modifier.padding(top=9.dp),verticalAlignment=Alignment.CenterVertically) { Surface(color=if(t.status=="PENDING")p.raised else p.accentSoft,shape=CircleShape) { Text(t.status,color=if(t.status=="PENDING")p.violet else p.accent,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp)) }; Spacer(Modifier.width(7.dp)); Text(t.source,color=p.muted,fontSize=8.sp) }
    if(t.status=="PENDING") OutlinedButton(onClick={verify(t)},modifier=Modifier.fillMaxWidth().padding(top=8.dp),shape=RoundedCornerShape(11.dp)) { Icon(Icons.Rounded.Verified,null,modifier=Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("VERIFY & CONFIRM",fontSize=9.sp) }
    if(t.note.isNotBlank()) Text(t.note,color=p.muted,fontSize=9.sp,modifier=Modifier.padding(top=7.dp))
    Text("REF  "+t.reference,color=p.muted,fontSize=8.sp,modifier=Modifier.padding(top=5.dp))
   } }
  }
 }
}

@Composable private fun Insights(items:List<IncomeTransaction>,p:Palette) {
 val valid=items.filter { it.status=="CONFIRMED" }; val now=System.currentTimeMillis()
 val day=valid.filter { now-it.receivedAt<86400000L }.sumOf { it.amount }
 val week=valid.filter { now-it.receivedAt<604800000L }.sumOf { it.amount }
 val month=valid.filter { val d=Calendar.getInstance().apply { timeInMillis=it.receivedAt }; val n=Calendar.getInstance(); d.get(Calendar.MONTH)==n.get(Calendar.MONTH)&&d.get(Calendar.YEAR)==n.get(Calendar.YEAR) }.sumOf { it.amount }
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=17.dp),verticalArrangement=Arrangement.spacedBy(3.dp),contentPadding=PaddingValues(bottom=12.dp)) {
  item { Metric("Today",day,p,Icons.Rounded.Today) }; item { Metric("Last 7 days",week,p,Icons.Rounded.DateRange) }; item { Metric("This month",month,p,Icons.Rounded.CalendarMonth) }
  item { Panel(p) {
   Eyebrow("Hourly distribution 24H",p); Text("Income activity",color=p.text,fontSize=15.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=5.dp))
   val bars=(0..23).map { hour -> valid.filter { Calendar.getInstance().apply { timeInMillis=it.receivedAt }.get(Calendar.HOUR_OF_DAY)==hour }.sumOf { it.amount } }; val max=(bars.maxOrNull()?:0.0).coerceAtLeast(1.0)
   Row(Modifier.fillMaxWidth().height(126.dp).padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=Alignment.Bottom) { bars.forEach { value -> Box(Modifier.weight(1f).fillMaxHeight((value/max).toFloat().coerceAtLeast(if(value>0).06f else .025f)).clip(RoundedCornerShape(topStart=4.dp,topEnd=4.dp)).background(if(value>0) Brush.verticalGradient(listOf(p.accent,p.violet)) else Brush.verticalGradient(listOf(p.raised,p.raised)))) } }
   Row(Modifier.fillMaxWidth().padding(top=7.dp),horizontalArrangement=Arrangement.SpaceBetween) { Text("00:00",color=p.muted,fontSize=8.sp); Text("12:00",color=p.muted,fontSize=8.sp); Text("23:00",color=p.muted,fontSize=8.sp) }
  } }
  item { Panel(p) { Eyebrow("Data integrity",p); Row(Modifier.padding(top=8.dp),verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Rounded.VerifiedUser,null,tint=p.accent); Text("Only confirmed records are included in totals.",color=p.text,fontSize=10.sp,modifier=Modifier.padding(start=9.dp)) } } }
 }
}

@Composable private fun Metric(label:String,value:Double,p:Palette,icon:androidx.compose.ui.graphics.vector.ImageVector) {
 Panel(p) { Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Eyebrow(label,p); Text("₹"+String.format("%,.2f",value),fontSize=25.sp,fontWeight=FontWeight.Bold,color=p.text,letterSpacing=(-.8).sp) }; Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(p.accentSoft),contentAlignment=Alignment.Center) { Icon(icon,null,tint=p.accent) } } }
}

@Composable private fun Prefs(upi:String,payee:String,dark:Boolean,p:Palette,onUpi:(String)->Unit,onPayee:(String)->Unit,onTheme:(Boolean)->Unit,access:()->Unit) {
 var u by remember(upi) { mutableStateOf(upi) }; var name by remember(payee) { mutableStateOf(payee) }
 val fields=OutlinedTextFieldDefaults.colors(focusedTextColor=p.text,unfocusedTextColor=p.text,focusedBorderColor=p.accent,unfocusedBorderColor=p.border,focusedLabelColor=p.accent,unfocusedLabelColor=p.muted,cursorColor=p.accent,focusedContainerColor=p.raised,unfocusedContainerColor=p.raised)
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=17.dp),verticalArrangement=Arrangement.spacedBy(3.dp),contentPadding=PaddingValues(bottom=12.dp)) {
  item { Panel(p) {
   Eyebrow("Payment identity 01",p); Text("UPI configuration",color=p.text,fontSize=16.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=5.dp,bottom=8.dp))
   OutlinedTextField(name,{name=it},label={Text("Payee name")},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=fields,shape=RoundedCornerShape(13.dp))
   Spacer(Modifier.height(9.dp)); OutlinedTextField(u,{u=it},label={Text("UPI ID")},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=fields,shape=RoundedCornerShape(13.dp))
   Button(onClick={onPayee(name.trim());onUpi(u.trim())},modifier=Modifier.fillMaxWidth().padding(top=12.dp).height(47.dp),shape=RoundedCornerShape(13.dp)) { Icon(Icons.Rounded.Sync,null,modifier=Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("SAVE & REGENERATE QR",fontSize=9.sp,fontWeight=FontWeight.Bold) }
  } }
  item { Panel(p) {
   Eyebrow("Appearance 02",p); Row(Modifier.fillMaxWidth().padding(top=9.dp),verticalAlignment=Alignment.CenterVertically) {
    Box(Modifier.size(39.dp).clip(RoundedCornerShape(12.dp)).background(p.accentSoft),contentAlignment=Alignment.Center) { Icon(if(dark)Icons.Rounded.DarkMode else Icons.Rounded.LightMode,null,tint=p.accent) }
    Column(Modifier.weight(1f).padding(start=11.dp)) { Text(if(dark)"Cyber night" else "Clean light",color=p.text,fontWeight=FontWeight.Bold,fontSize=12.sp); Text(if(dark)"Deep graphite · neon mint" else "Porcelain · emerald",color=p.muted,fontSize=9.sp) }
    Switch(checked=dark,onCheckedChange=onTheme,colors=SwitchDefaults.colors(checkedThumbColor=p.accent,checkedTrackColor=p.accentSoft))
   }
  } }
  item { Panel(p) {
   Eyebrow("Detection engine 03",p); Text("Payment notifications",color=p.text,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=5.dp))
   Text("Supported UPI and bank alerts are matched only while a QR session is active. Every match stays pending until you verify it in your payment app.",color=p.muted,fontSize=10.sp,lineHeight=16.sp,modifier=Modifier.padding(top=6.dp))
   OutlinedButton(onClick=access,modifier=Modifier.fillMaxWidth().padding(top=11.dp),shape=RoundedCornerShape(12.dp)) { Icon(Icons.Rounded.NotificationsActive,null,modifier=Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("CONFIGURE NOTIFICATION ACCESS",fontSize=9.sp) }
  } }
  item { Text("NO PAYMENT IS INITIATED BY THIS APP · SMS AND EMAIL ARE NOT READ",color=p.muted,fontSize=8.sp,letterSpacing=.7.sp,lineHeight=13.sp,modifier=Modifier.padding(horizontal=4.dp,vertical=8.dp)) }
 }
}

@Composable private fun AddDialog(p:Palette,close:()->Unit,save:(Double,String,String)->Unit) {
 var amount by remember { mutableStateOf("") }; var payer by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
 val fields=OutlinedTextFieldDefaults.colors(focusedTextColor=p.text,unfocusedTextColor=p.text,focusedBorderColor=p.accent,unfocusedBorderColor=p.border,focusedLabelColor=p.accent,unfocusedLabelColor=p.muted,cursorColor=p.accent,focusedContainerColor=p.raised,unfocusedContainerColor=p.raised)
 AlertDialog(onDismissRequest=close,containerColor=p.card,titleContentColor=p.text,textContentColor=p.muted,title={Text("Record income",fontWeight=FontWeight.Bold)},text={
  Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   OutlinedTextField(amount,{amount=it},label={Text("Amount ₹")},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=fields)
   OutlinedTextField(payer,{payer=it},label={Text("Received from")},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=fields)
   OutlinedTextField(note,{note=it},label={Text("Work note")},modifier=Modifier.fillMaxWidth(),colors=fields)
  }
 },confirmButton={Button(onClick={val a=amount.toDoubleOrNull();if(a!=null&&a>0&&payer.isNotBlank())save(a,payer.trim(),note.trim())},enabled=(amount.toDoubleOrNull()?:0.0)>0&&payer.isNotBlank()){Text("SAVE")}},dismissButton={TextButton(onClick=close){Text("CANCEL")}})
}
