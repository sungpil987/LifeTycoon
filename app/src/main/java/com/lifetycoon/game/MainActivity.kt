package com.lifetycoon.game
import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.*
import android.view.*
import kotlin.math.pow

class MainActivity:Activity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val preferences=getSharedPreferences("save",0)
  val previous=preferences.getString("last_crash",null)
  Thread.setDefaultUncaughtExceptionHandler { _, error ->
   try {
    preferences.edit().putString("last_crash",android.util.Log.getStackTraceString(error).take(5000)).commit()
   } catch (_:Throwable) {}
   android.os.Process.killProcess(android.os.Process.myPid())
   kotlin.system.exitProcess(10)
  }
  if(previous!=null){
   val message=android.widget.TextView(this)
   message.text="인생역전 오류 진단 화면\n\n아래 내용을 캡처해서 보내줘:\n\n"+previous
   message.textSize=14f
   message.setTextColor(android.graphics.Color.BLACK)
   message.setPadding(24,40,24,24)
   val scroll=android.widget.ScrollView(this)
   scroll.addView(message)
   setContentView(scroll)
   preferences.edit().remove("last_crash").apply()
  }else{
   try {setContentView(Game(this))}
   catch(error:Throwable){
    val message=android.widget.TextView(this)
    message.text="시작 오류:\n"+android.util.Log.getStackTraceString(error)
    message.textSize=14f
    setContentView(message)
   }
  }
 }
}
class Game(ctx:Context):View(ctx){
 val store=ctx.getSharedPreferences("save",0)
 var cash=store.getLong("cash",0).toDouble()
 var tap=store.getInt("tap",1)
 var staff=store.getInt("staff",0)
 var shop=store.getInt("shop",0)
 var tab=0
 var last=System.currentTimeMillis()
 val p=Paint(3)
 val dark=Color.rgb(56,43,40)
 val hero:Bitmap?=try{
   val sheet=ctx.assets.open("a_clean_colorful_cartoon_game_assets_sheet_flat_c.png").use { BitmapFactory.decodeStream(it) }
   val sx=sheet.width/1536f
   val sy=sheet.height/1024f
   val left=0
   val top=0
   val right=(360*sx).toInt().coerceAtMost(sheet.width)
   val bottom=(535*sy).toInt().coerceAtMost(sheet.height)
   val crop=Bitmap.createBitmap(sheet,left,top,right-left,bottom-top)
   val pixels=IntArray(crop.width*crop.height)
   crop.getPixels(pixels,0,crop.width,0,0,crop.width,crop.height)
   for(i in pixels.indices){
     val color=pixels[i]
     val r=Color.red(color);val g=Color.green(color);val b=Color.blue(color)
     val minimum=minOf(r,g,b)
     if(minimum>243 && maxOf(r,g,b)-minimum<15) pixels[i]=Color.TRANSPARENT
     else if(minimum>225 && maxOf(r,g,b)-minimum<12){
       val alpha=((243-minimum)*255/18).coerceIn(0,255)
       pixels[i]=Color.argb(alpha,r,g,b)
     }
   }
   val result=Bitmap.createBitmap(crop.width,crop.height,Bitmap.Config.ARGB_8888)
   result.setPixels(pixels,0,crop.width,0,0,crop.width,crop.height)
   crop.recycle();sheet.recycle();result
 }catch(e:Exception){android.util.Log.e("LifeTycoon","Asset load failed",e);null}
 fun box(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int,r:Float=20f){p.color=color;c.drawRoundRect(x,y,x+w,y+h,r,r,p)}
 fun text(c:Canvas,s:String,x:Float,y:Float,size:Float=22f,color:Int=dark){p.color=color;p.textSize=size;p.typeface=Typeface.create("sans-serif",Typeface.BOLD);p.textAlign=Paint.Align.CENTER;c.drawText(s,x,y,p)}
 fun format(v:Double):String=when{v>=1e12->"%.1f조".format(v/1e12);v>=1e8->"%.1f억".format(v/1e8);v>=1e4->"%.1f만".format(v/1e4);else->"%.0f".format(v)}
 fun cost(base:Double,level:Int)=base*1.2.pow(level.toDouble())
 fun save(){store.edit().putLong("cash",cash.coerceAtMost(Long.MAX_VALUE.toDouble()).toLong()).putInt("tap",tap).putInt("staff",staff).putInt("shop",shop).apply()}
 override fun onDraw(c:Canvas){
  try {
  val now=System.currentTimeMillis();cash+=(staff*2+shop*25)*(now-last).coerceIn(0,60000)/1000.0;last=now
  c.save();c.scale(width/400f,height/800f);c.drawColor(Color.rgb(255,247,223))
  box(c,0f,0f,400f,180f,Color.rgb(255,216,130),0f)
  text(c,"인생역전",200f,53f,34f)
  box(c,20f,78f,360f,85f,Color.WHITE)
  text(c,format(cash)+" 원",200f,117f,30f)
  text(c,"초당 +"+format((staff*2+shop*25).toDouble())+"원",200f,146f,17f)
  if(tab==0){
   box(c,18f,194f,364f,420f,Color.rgb(255,250,237))
   if(hero!=null){
    p.color=Color.WHITE;p.alpha=255
    c.drawBitmap(hero,null,RectF(76f,242f,324f,552f),p)
   }else{
    p.color=Color.rgb(91,127,79);c.drawOval(126f,404f,274f,545f,p)
    p.color=Color.rgb(246,190,135);c.drawCircle(200f,350f,82f,p)
    text(c,"캐릭터 로딩 실패",200f,440f,18f)
   }
   box(c,47f,555f,306f,66f,Color.rgb(255,188,55))
   text(c,"터치해서 돈 벌기!",200f,597f,25f)
   text(c,"캐릭터를 눌러 수익 획득",200f,665f,17f)
  }else{
   text(c,if(tab==1)"능력 강화" else "직원과 사업",200f,225f,30f)
   for(i in 0..1){
    val y=251f+i*154f
    val isTap=tab==1&&i==0
    val isStaff=(tab==1&&i==1)||(tab==2&&i==0)
    val level=if(isTap)tap-1 else if(isStaff)staff else shop
    val price=cost(if(isTap)10.0 else if(isStaff)100.0 else 1500.0,level)
    box(c,20f,y,360f,137f,Color.WHITE)
    text(c,if(isTap)"손놀림 강화" else if(isStaff)"알바생 고용" else "노점 운영",200f,y+35f,23f)
    text(c,if(isTap)"터치 +1" else if(isStaff)"초당 +2원" else "초당 +25원",200f,y+64f,17f)
    box(c,45f,y+78f,310f,45f,if(cash>=price)Color.rgb(255,188,55) else Color.LTGRAY)
    text(c,format(price)+"원 구매",200f,y+107f,19f)
   }
  }
  box(c,0f,704f,400f,96f,Color.WHITE,0f)
  val names=listOf("터치","강화","사업")
  for(i in 0..2){box(c,8f+i*132f,719f,124f,63f,if(tab==i)Color.rgb(255,188,55) else Color.rgb(239,231,210));text(c,names[i],70f+i*132f,759f,21f)}
  c.restore();postInvalidateDelayed(40)
  }catch(error:Throwable){
   android.util.Log.e("LifeTycoon","Draw error",error)
   c.drawColor(Color.WHITE)
   p.color=Color.RED;p.textSize=20f;p.textAlign=Paint.Align.LEFT
   c.drawText("Drawing error: "+error.javaClass.simpleName,15f,60f,p)
   throw error
  }
 }
 override fun onTouchEvent(e:MotionEvent):Boolean{
  if(e.action!=MotionEvent.ACTION_UP)return true
  if(width<=0||height<=0)return true
  val x=e.x*400f/width;val y=e.y*800f/height
  if(y>=704f){tab=(x/133f).toInt().coerceIn(0,2)}
  else if(tab==0&&y in 194f..625f){cash+=tap}
  else if(tab!=0){
   val i=if(y in 251f..388f)0 else if(y in 405f..542f)1 else -1
   if(i>=0){
    val a=tab==1&&i==0;val b=(tab==1&&i==1)||(tab==2&&i==0)
    val price=cost(if(a)10.0 else if(b)100.0 else 1500.0,if(a)tap-1 else if(b)staff else shop)
    if(cash>=price){cash-=price;if(a)tap++ else if(b)staff++ else shop++}
   }
  }
  save();invalidate();return true
 }
 override fun onDetachedFromWindow(){save();super.onDetachedFromWindow()}
}
