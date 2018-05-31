package test.ascroll.scroll

import android.content.Context
import android.graphics.*
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import android.widget.TextView
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.sp

/**
 * http://www.jb51.net/article/31797.htm
 * Touch 事件分层：Activity, ViewGroup, View
 * 如果该层没有消费/处理ACTION_DOWN事件，A就再也收不到后续的事件，直到过程结束ACTION_UP事件
 * 如果非ACTION_DOWN事件被父View拦截，会收到ACTION_CANCEL事件，转A
 * 如果子View没有处理Touch事件，则父View按照普通方式处理（分发）
 * 如果父View在onInterceptTouchEvent中拦截了事件，则onInterceptTouchEvent不会收到Touch事件，
 * 因为事件直接交由它自己处理（普通View的处理方式）
 */
class OverScrollView(ctx:Context): View(ctx),AnkoLogger{
    override val loggerTag: String
        get() = "_View"
    private var touchGroupIndex = 0
    private var touchSate = "None"
        set(value) {
            if (value != field) {
                field = value
                postInvalidate()
            }
        }
    var visX:Float = 0F
        private set
    var visY:Float = 0F
        private set
    fun moveOffset(dx:Float, dy:Float){
        visX += dx
        visY += dy
        invalidate()
    }
    fun moveTo(x:Float, y:Float){
        visX = x
        visY = y
        invalidate()
    }
    //按下时的值（可以是可视坐标x,y 也可以是屏幕坐标rawX,rawY）
    private var downX:Float = 0F
    private var downY:Float = 0F
    //上次移动后的值（同down同类型）
    private var lastMoveX:Float = 0F
    private var lastMoveY:Float = 0F
    private val mScroller:OverScroller
    private var tickFling: Long = 0
    private var countFling: Int = 0
    private var numFling: Int = 0
    private var flagFling:Boolean = false
    private val FONTSIZE = 26
    private val paint:Paint
    init {
        mScroller = OverScroller(ctx)
        paint = Paint().apply {
            textSize = sp(FONTSIZE).toFloat()
            color = Color.BLACK
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
        }
        /*setOnLongClickListener {
            info { "LongClick" }
            return@setOnLongClickListener true
        }
        setOnClickListener {
            info { "Click" }
        }*/
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (flagFling){
                    stopFling()
                }
                //用屏幕坐标
                lastMoveX = event.rawX
                lastMoveY = event.rawY

                touchGroupIndex++
                info { "PreDown" }
                touchSate = "_DOWN"
                //TextView默认不消费该事件，需要设置
                //这里消费了，下面的自动消费，它们是一组
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastMoveX
                var dy = event.rawY - lastMoveY
                moveOffset(dx, dy)
                lastMoveX = event.rawX
                lastMoveY = event.rawY

                touchSate = "_MOVE"
            }
            MotionEvent.ACTION_UP -> {
                //touchSate = "_UP"
                info { "PostUp" }
                info { "fling(500,2500,0,5000)" }
                //如果不过界，由速率决定最终置，因为时间是一定的
                //速率越大，持续时间越长
                //overX 到最大值后，会回弹到minX值
                flagFling = true
                mScroller.fling(500,500,2500,0,
                        0,1000, 0, 1000,250,0)
                countFling = 0
                numFling = 500
                tickFling = System.currentTimeMillis()
                //启动fling
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        //info { "Draw" }
        val dh = sp(FONTSIZE+4).toFloat()
        val cx = width.toFloat() / 2

        val visPt = "(x,y) = ${visX.toInt()},${visY.toInt()}"
        val visRect = "(curX,curY,isFinish) = ${mScroller.currX},${mScroller.currY},${mScroller.isFinished}"
        val touchEvent = "$touchGroupIndex$touchSate"
        val strs = listOf<String>(touchEvent,visRect,visPt)
        var vcy = (height.toFloat() - dh * strs.size) / 2

        strs.forEach {
            canvas.drawText(it, cx, vcy, paint)
            vcy += dh
        }
        //_MOVE事件移动至View的外面时，仍然有效；移动至Activity之外_UP事件才触发
    }

    override fun computeScroll() {
        //info { "computeScroll:(${mScroller.isFinished},${mScroller.currX},${mScroller.currY})" }
        super.computeScroll()
        if (mScroller.computeScrollOffset()){
            //中间值
            countFling++
            invalidate()
        }else if (flagFling){
            //最后一个值
            flagFling = false
            var curTick = System.currentTimeMillis()
            info { "$numFling - ${mScroller.finalX} (${mScroller.currX}), timespan: ${curTick- tickFling} ms, count: $countFling" }
        }
    }
    fun stopFling(completed: Boolean = false){
        //flagFling = false
        if (completed){
            mScroller.forceFinished(true)
            invalidate()
        }else{
            mScroller.abortAnimation()
            postInvalidate()
        }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        info { "SizeChanged" }
    }

}