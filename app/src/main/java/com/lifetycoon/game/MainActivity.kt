package com.lifetycoon.game
import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.*
import android.view.*
import kotlin.math.pow

class MainActivity:Activity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(Game(this))}
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
 fun box(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int,r:Float=20f){p.color=color;c.drawRoundRect(x,y,x+w,y+h,r,r,p)}
 fun text(c:Canvas,s:String,x:Float,y:Float,size:Float=22f,color:Int=dark){p.color=color;p.textSize=size;p.typeface=Typeface.create("sans-serif",Typeface.BOLD);p.textAlign=Paint.Align.CENTER;c.drawText(s,x,y,p)}
 fun format(v:Double):String=when{v>=1e12->"%.1f조".format(v/1e12);v>=1e8->"%.1f억".format(v/1e8);v>=1e4->"%.1f만".format(v/1e4);else->"%.0f".format(v)}
 fun cost(base:Double,level:Int)=base*1.2.pow(level.toDouble())
 fun save(){store.edit().putLong("cash",cash.coerceAtMost(Long.MAX_VALUE.toDouble()).toLong()).putInt("tap",tap).putInt("staff",staff).putInt("shop",shop).apply()}
 override fun onDraw(c:Canvas){
  val now=System.currentTimeMillis();cash+=(staff*2+shop*25)*(now-last).coerceIn(0,60000)/1000.0;last=now
  c.save();c.scale(width/400f,height/800f);c.drawColor(Color.rgb(255,247,223))
  box(c,0f,0f,400f,180f,Color.rgb(255,216,130),0f)
  text(c,"인생역전",200f,53f,34f)
  box(c,20f,78f,360f,85f,Color.WHITE)
  text(c,format(cash)+" 원",200f,117f,30f)
  text(c,"초당 +"+format((staff*2+shop*25).toDouble())+"원",200f,146f,17f)
  if(tab==0){
   box(c,18f,194f,364f,420f,Color.rgb(241,226,190))
   // Original cartoon protagonist, rendered directly in Android Canvas.
   p.color=Color.rgb(91,127,79);c.drawOval(126f,404f,274f,545f,p)
   p.color=Color.rgb(246,190,135);c.drawCircle(200f,350f,82f,p)
   p.color=Color.rgb(54,43,40);c.drawArc(118f,261f,282f,351f,180f,180f,false,p)
   p.color=Color.WHITE;c.drawCircle(169f,351f,13f,p);c.drawCircle(231f,351f,13f,p)
   p.color=dark;c.drawCircle(169f,352f,5f,p);c.drawCircle(231f,352f,5f,p)
   p.color=Color.rgb(156,68,64);c.drawOval(174f,380f,226f,407f,p)
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
 }
 override fun onTouchEvent(e:MotionEvent):Boolean{
  if(e.action!=MotionEvent.ACTION_UP)return true
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
