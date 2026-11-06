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
  @Suppress("DEPRECATION")
  window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
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
 var totalTaps=store.getLong("totalTaps",0)
 var notice=""
 var overlay=0
 var questClaimed=store.getBoolean("questClaimed",false)
 var rouletteUsed=store.getLong("rouletteUsed",0L)
 var last=System.currentTimeMillis()
 val p=Paint(3)
 val dark=Color.rgb(56,43,40)
 val amber=Color.rgb(255,188,55)
 val deep=Color.rgb(65,51,53)
 val lime=Color.rgb(141,212,57)
 val background:Bitmap?=try {
   val file=ctx.assets.list("")?.firstOrNull { it.endsWith(".png",true) && it!="a_clean_colorful_cartoon_game_assets_sheet_flat_c.png" && it!="lifetycoon_business_icons_0_6.png" && it!="file_00000000ac148209960818905b980f98.png" && it!="file_00000000ce1c82098ec8c79ec7a14403.png" }
   if(file!=null)ctx.assets.open(file).use { BitmapFactory.decodeStream(it) } else null
 }catch(e:Exception){android.util.Log.e("LifeTycoon","Background load failed",e);null}
 val businessIcons:Bitmap?=try {
   ctx.assets.open("lifetycoon_business_icons_0_6.png").use { BitmapFactory.decodeStream(it) }
 }catch(e:Exception){android.util.Log.e("LifeTycoon","Business icons load failed",e);null}
 val hudSheet:Bitmap?=try {
   ctx.assets.open("file_00000000ac148209960818905b980f98.png").use { BitmapFactory.decodeStream(it) }
 }catch(e:Exception){android.util.Log.e("LifeTycoon","HUD asset load failed",e);null}
 val hudBounds:Rect?=hudSheet?.let { bitmap ->
   // The asset contains generous transparent padding; locate actual painted pixels.
   var left=bitmap.width;var right=0;var top=bitmap.height;var bottom=0
   val pixels=IntArray(bitmap.width)
   for(y in 0 until bitmap.height step 2){
    bitmap.getPixels(pixels,0,bitmap.width,0,y,bitmap.width,1)
    for(x in 0 until bitmap.width step 2){
     if(Color.alpha(pixels[x])>35){
      left=minOf(left,x);right=maxOf(right,x)
      top=minOf(top,y);bottom=maxOf(bottom,y)
     }
    }
   }
   if(right>left&&bottom>top)Rect(left,top,right+1,bottom+1) else null
 }
 fun hudIcon(c:Canvas,start:Float,end:Float,x:Float,y:Float,w:Float,h:Float){
   val bitmap=hudSheet?:return
   val bounds=hudBounds?:return
   val left=(bounds.left+bounds.width()*start).toInt().coerceIn(0,bitmap.width-1)
   val right=(bounds.left+bounds.width()*end).toInt().coerceIn(left+1,bitmap.width)
   p.color=Color.WHITE;p.alpha=255;p.isFilterBitmap=true
   c.drawBitmap(bitmap,Rect(left,bounds.top,right,bounds.bottom),RectF(x,y,x+w,y+h),p)
 }
 val sideSheet:Bitmap?=try{
   ctx.assets.open("file_00000000ce1c82098ec8c79ec7a14403.png").use { BitmapFactory.decodeStream(it) }
 }catch(e:Exception){android.util.Log.e("LifeTycoon","Side icons load failed",e);null}
 // Artwork is arranged as two columns with four icons on the left and three on the right.
 // Crop bounds are fractional so they remain valid if the PNG resolution changes.
 fun sideIcon(c:Canvas,index:Int,x:Float,y:Float,w:Float,h:Float){
   val bitmap=sideSheet?:return
   val col=if(index<4)0 else 1
   val row=if(index<4)index else index-4
   val bounds=if(col==0)arrayOf(
    floatArrayOf(.035f,.02f,.36f,.285f),
    floatArrayOf(.035f,.285f,.36f,.52f),
    floatArrayOf(.035f,.515f,.36f,.75f),
    floatArrayOf(.035f,.735f,.36f,.995f)
   ) else arrayOf(
    floatArrayOf(.61f,.02f,.94f,.29f),
    floatArrayOf(.61f,.29f,.94f,.56f),
    floatArrayOf(.61f,.565f,.95f,.82f)
   )
   val a=bounds[row]
   p.color=Color.WHITE;p.alpha=255;p.isFilterBitmap=true
   c.drawBitmap(bitmap,Rect((a[0]*bitmap.width).toInt(),(a[1]*bitmap.height).toInt(),(a[2]*bitmap.width).toInt(),(a[3]*bitmap.height).toInt()),RectF(x,y,x+w,y+h),p)
 }
 var tapPulse=0f
 val bursts=mutableListOf<Triple<Float,Float,Long>>()
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
 fun save(){store.edit().putLong("cash",cash.coerceAtMost(Long.MAX_VALUE.toDouble()).toLong()).putInt("tap",tap).putInt("staff",staff).putInt("shop",shop).putLong("totalTaps",totalTaps).putBoolean("questClaimed",questClaimed).putLong("rouletteUsed",rouletteUsed).apply()}
 override fun onDraw(c:Canvas){
  try {
  val now=System.currentTimeMillis();cash+=(staff*2+shop*25)*(now-last).coerceIn(0,60000)/1000.0;last=now
  c.save();c.scale(width/400f,height/800f);c.drawColor(Color.rgb(255,247,223))
  box(c,0f,0f,400f,92f,Color.rgb(255,220,146),0f)
  box(c,8f,12f,148f,67f,deep,16f)
  hudIcon(c,0f,0.145f,10f,9f,61f,69f)
  text(c,"Lv."+((totalTaps/100)+1)+" 거지",111f,36f,16f,Color.WHITE)
  box(c,76f,47f,69f,14f,Color.rgb(35,30,31),7f)
  box(c,76f,47f,69f*(totalTaps%100)/100f,14f,amber,7f)
  text(c,""+(totalTaps%100)+"%",110f,73f,12f,Color.WHITE)
  box(c,162f,15f,139f,55f,deep,16f)
  hudIcon(c,0.375f,0.49f,165f,18f,48f,46f)
  text(c,format(cash),253f,48f,22f,Color.rgb(255,215,79))
  box(c,305f,15f,88f,55f,deep,16f)
  hudIcon(c,0.67f,0.77f,307f,23f,37f,37f)
  text(c,"0",366f,49f,22f,Color.WHITE)
  if(notice.isNotEmpty())text(c,notice,200f,89f,13f,deep)
  if(tab==0){
   if(background!=null){
    p.color=Color.WHITE;p.alpha=255
    c.save();c.clipRect(18f,94f,382f,442f)
    val cropWidth=background.width*0.85f
    val source=Rect(((background.width-cropWidth)/2).toInt(),0,((background.width+cropWidth)/2).toInt(),background.height)
    c.drawBitmap(background,source,RectF(18f,94f,382f,442f),p)
    c.restore()
   }else box(c,18f,94f,364f,348f,Color.rgb(255,250,237))
   if(hero!=null){
    p.color=Color.WHITE;p.alpha=255
    val pulse=1f+tapPulse*0.035f
    c.save();c.scale(pulse,pulse,200f,370f)
    c.drawBitmap(hero,null,RectF(103f,158f,297f,416f),p)
    c.restore()
   }else{
    p.color=Color.rgb(91,127,79);c.drawOval(126f,404f,274f,545f,p)
    p.color=Color.rgb(246,190,135);c.drawCircle(200f,350f,82f,p)
    text(c,"캐릭터 로딩 실패",200f,440f,18f)
   }
   // Seven illustrated shortcuts, positioned around the central character.
   for(i in 0..6){
    val left=i<4
    val x=if(left)16f else 328f
    val row=if(left)i else i-4
    val y=106f+row*74f
    if(sideSheet!=null){
     sideIcon(c,i,x,y,58f,65f)
    }else{
     box(c,x,y,58f,65f,Color.rgb(255,225,160),12f)
     text(c,listOf("퀘스트","업적","도감","이벤트","랭킹","룰렛","보상")[i],x+29f,y+40f,12f)
    }
   }
   tapPulse=(tapPulse-0.08f).coerceAtLeast(0f)
   val iter=bursts.iterator()
   while(iter.hasNext()){
    val item=iter.next()
    val age=(now-item.third)/650f
    if(age>=1f){iter.remove();continue}
    p.color=Color.argb(((1f-age)*255).toInt(),255,215,60)
    c.drawCircle(item.first,item.second-age*90f,15f*(1f-age*0.4f),p)
    text(c,"+"+format(tap.toDouble()),item.first,item.second-age*90f-22f,23f,Color.rgb(255,230,55))
   }
   box(c,61f,421f,278f,54f,Color.rgb(130,76,33),20f)
   box(c,61f,415f,278f,54f,amber,20f)
   text(c,"터치해서 돈 벌기!",200f,451f,23f)
   text(c,"터치 +"+format(tap.toDouble())+"원       자동 +"+format((staff*2+shop*25).toDouble())+"원/초",200f,494f,16f)
   val quick=listOf("직원 고용","가게 운영","부동산","기업 인수","도시 확장","환생","연구소","업적")
   for(i in quick.indices){
    val x=8f+(i%4)*98f;val y=510f+(i/4)*76f
    if(businessIcons!=null){
     val col=i%4
     val row=i/4
     val tileW=businessIcons.width/4
     val tileH=businessIcons.height/2
     val source=Rect(col*tileW,row*tileH,(col+1)*tileW,(row+1)*tileH)
     p.color=Color.WHITE;p.alpha=255;p.isFilterBitmap=true
     c.drawBitmap(businessIcons,source,RectF(x,y,x+91f,y+68f),p)
    }else{
     box(c,x,y+3f,91f,68f,Color.rgb(147,112,82),12f)
     box(c,x,y,91f,68f,if(i<2)Color.rgb(255,216,153) else Color.rgb(214,198,178),12f)
     text(c,quick[i],x+45f,y+42f,13f)
    }
   }
  }else if(tab==1||tab==2){
   text(c,if(tab==1)"능력 강화" else "직원과 사업",200f,154f,30f)
   for(i in 0..1){
    val y=183f+i*154f
    val isTap=tab==1&&i==0
    val isStaff=(tab==1&&i==1)||(tab==2&&i==0)
    val level=if(isTap)tap-1 else if(isStaff)staff else shop
    val price=cost(if(isTap)10.0 else if(isStaff)100.0 else 1500.0,level)
    box(c,20f,y+4f,360f,137f,Color.rgb(204,180,146))
    box(c,20f,y,360f,137f,Color.WHITE)
    text(c,if(isTap)"손놀림 강화" else if(isStaff)"알바생 고용" else "노점 운영",200f,y+35f,23f)
    text(c,if(isTap)"터치 +1" else if(isStaff)"초당 +2원" else "초당 +25원",200f,y+64f,17f)
    box(c,45f,y+81f,310f,45f,Color.rgb(128,101,67))
    box(c,45f,y+77f,310f,45f,if(cash>=price)lime else Color.LTGRAY)
    text(c,format(price)+"원 구매",200f,y+107f,19f)
   }
  }
  if(tab>=3){
   text(c,when(tab){3->"업적";4->"도감";else->"상점"},200f,153f,30f)
   val lines=when(tab){
    3->listOf("첫 터치   "+if(totalTaps>=1)"완료" else "미완료","100번 터치   "+if(totalTaps>=100)"완료" else "미완료","직원 1명 고용   "+if(staff>=1)"완료" else "미완료","가게 1개 운영   "+if(shop>=1)"완료" else "미완료")
    4->listOf("거리의 시작   발견","알바생   "+if(staff>0)"발견" else "미발견","노점   "+if(shop>0)"발견" else "미발견","더 많은 수집품은 다음 업데이트")
    else->listOf("현재 버전은 무료 플레이","광고와 유료 결제 없음","상점 콘텐츠 준비 중")
   }
   for(i in lines.indices){
    val y=190f+i*92f
    box(c,22f,y,356f,75f,Color.WHITE,15f)
    text(c,lines[i],200f,y+44f,19f)
   }
  }
  if(overlay!=0){
   box(c,20f,193f,360f,294f,deep,23f)
   box(c,26f,199f,348f,282f,Color.rgb(255,244,216),19f)
   val heading=when(overlay){1->"오늘의 퀘스트";2->"이벤트";3->"랭킹";else->"행운 룰렛"}
   text(c,heading,200f,239f,27f)
   text(c,"닫기 X",335f,225f,14f)
   when(overlay){
    1->{
     text(c,"터치 50회 달성",200f,292f,22f)
     text(c,"${totalTaps.coerceAtMost(50L)}/50회",200f,328f,19f)
     box(c,66f,355f,268f,55f,if(totalTaps>=50&&!questClaimed)lime else Color.LTGRAY,13f)
     text(c,if(questClaimed)"보상 수령 완료" else "보상 받기  +500원",200f,390f,19f)
    }
    4->{
     text(c,"하루 1회 무료 보너스",200f,291f,21f)
     val available=System.currentTimeMillis()-rouletteUsed>=86400000L
     text(c,if(available)"오늘의 행운을 확인해 봐!" else "오늘은 이미 참여했어",200f,329f,18f)
     box(c,66f,355f,268f,55f,if(available)lime else Color.LTGRAY,13f)
     text(c,if(available)"돌리기 (100~500원)" else "내일 다시 도전!",200f,390f,18f)
    }
    else->{
     text(c,if(overlay==2)"새 이벤트 준비 중" else "온라인 랭킹 준비 중",200f,305f,21f)
     text(c,"추후 업데이트에서 공개",200f,348f,17f)
    }
   }
  }
  box(c,0f,681f,400f,119f,deep,0f)
  val names=listOf("홈","강화","사업","업적","도감","상점")
  val symbols=listOf("⌂","↑","▣","★","▤","▥")
  for(i in 0..5){
   val x=3f+i*66f
   box(c,x,692f,64f,70f,if(tab==i)amber else Color.rgb(105,90,88),12f)
   text(c,symbols[i],x+32f,724f,23f,if(tab==i)deep else Color.WHITE)
   text(c,names[i],x+32f,748f,13f,if(tab==i)deep else Color.WHITE)
  }
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
  if(overlay!=0 && y<681f){
   if(y in 193f..251f && x>288f){overlay=0}
   else if(y in 355f..420f){
    if(overlay==1 && totalTaps>=50 && !questClaimed){cash+=500.0;questClaimed=true;notice="퀘스트 보상 +500원!"}
    if(overlay==4 && System.currentTimeMillis()-rouletteUsed>=86400000L){
     val reward=(1..5).random()*100
     cash+=reward;rouletteUsed=System.currentTimeMillis();notice="룰렛 보상 +${reward}원!"
    }
    overlay=0
   }
   save();invalidate();return true
  }
  if(y>=681f){overlay=0;tab=(x/66.6667f).toInt().coerceIn(0,5);notice=""}
  else if(tab==0&&y in 106f..403f && (x<79f||x>323f)){
   val row=((y-106f)/74f).toInt()
   if(x<79f){
    when(row){0->overlay=1;1->tab=3;2->tab=4;3->overlay=2}
   }else{
    when(row){0->overlay=3;1->overlay=4;2->notice="보상받기는 준비 중"}
   }
  }
  else if(tab==0&&y in 510f..655f){
   val col=(x/100f).toInt().coerceIn(0,3)
   val row=if(y<586f)0 else 1
   val choice=row*4+col
   when(choice){0,1->tab=2;7->tab=3;else->notice="추후 업데이트에서 해금!"}
  }
  else if(tab==0&&y in 94f..480f){cash+=tap;totalTaps++;tapPulse=1f;bursts.add(Triple(x.coerceIn(70f,330f),y.coerceIn(265f,515f),System.currentTimeMillis()));if(bursts.size>12)bursts.removeAt(0)}
  else if(tab==1||tab==2){
   val i=if(y in 183f..320f)0 else if(y in 337f..474f)1 else -1
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
