package test.ascroll.scroll

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import android.widget.TextView
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.sp

val VIEW_TAG = "_View"
/**
 * http://www.jb51.net/article/31797.htm
 * Touch 事件分层：Activity, ViewGroup, View
 * 如果该层没有消费/处理ACTION_DOWN事件，A就再也收不到后续的事件，直到过程结束ACTION_UP事件
 * 如果非ACTION_DOWN事件被父View拦截，会收到ACTION_CANCEL事件，转A
 * 如果子View没有处理Touch事件，则父View按照普通方式处理（分发）
 * 如果父View在onInterceptTouchEvent中拦截了事件，则onInterceptTouchEvent不会收到Touch事件，
 * 因为事件直接交由它自己处理（普通View的处理方式）
 */
class OverScrollViewManager(ctx:Context): View(ctx),AnkoLogger{
    override val loggerTag: String
        get() = VIEW_TAG
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
            mScroller.abortAnimation()
            postInvalidate()
        }else{
            mScroller.forceFinished(true)
            invalidate()
        }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        info { "SizeChanged" }
    }
}

//region    ViewCallback ViewAnimationManager
interface ViewCallback{
    val visX:Float
    val visY:Float
    val view:View
    fun moveTo(x:Float, y:Float)
    fun moveOffset(dx:Float, dy:Float)
}

class ViewAnimationManger(private val host:ViewCallback):AnkoLogger {
    override val loggerTag: String
        get() = VIEW_TAG
    private val scroller: OverScroller
    private var animation: ValueAnimator? = null
    private var fling = false
    private var timeSpan:Long = 0

    init {
        scroller = OverScroller(host.view.context)
    }

    //region startXAnimation startYAnimation startFlingAnimation
    fun startXAnimation(xFrom: Float, xTo: Float) {
        info { "startXAnimation(${xFrom.toInt()},${xTo.toInt()})" }
        stopAll()
        timeSpan = System.currentTimeMillis()
        animation = ValueAnimator.ofFloat(xFrom, xTo).apply {
            interpolator = DecelerateInterpolator()
            duration = 400
            val xAnimation = XAnimation()
            addUpdateListener(xAnimation)
            addListener(xAnimation)
            start()
        }
    }

    fun startYAnimation(yFrom: Float, yTo: Float) {
        info { "startYAnimation(${yFrom.toInt()},${yTo.toInt()})" }
        stopAll()
        timeSpan = System.currentTimeMillis()
        animation = ValueAnimator.ofFloat(yFrom, yTo).apply {
            interpolator = DecelerateInterpolator()
            duration = 400
            val yAnimation = YAnimation()
            addUpdateListener(yAnimation)
            addListener(yAnimation)
            start()
        }
    }

    fun startFlingAnimation(startX: Int, startY: Int, velX: Int, velY: Int,
                            minX: Int, maxX: Int, minY: Int, maxY: Int) {
        info { "startFlingAnimation($startX,$velX,$minX - $maxX),($startY,$velY,$minY - $maxY)" }
        stopAll()
        fling = true
        timeSpan = System.currentTimeMillis()
        scroller.fling(startX, startY, velX, velY, minX, maxX, minY, maxY)
    }
    //endregion

    //region    stopAll stopFling
    fun stopAll() {
        animation?.cancel()
        animation = null
        stopFiling()
    }

    fun stopFiling(toFinal: Boolean = false) {
        fling = false
        info { "stopFling" }
        if (toFinal) {
            scroller.abortAnimation()
            host.view.postInvalidate()
        } else {
            scroller.forceFinished(true)
            host.view.invalidate()
        }
    }
    //endregion

    fun computeFling() {
        if (scroller.computeScrollOffset()) {
            host.moveTo(scroller.currX.toFloat(), scroller.currY.toFloat())
        } else if (fling) {
            fling = false
            host.moveTo(scroller.currX.toFloat(), scroller.currY.toFloat())
            val sp = timeSpan.takeIf { it != 0L }.apply {
                "(${System.currentTimeMillis()-this!!})ms"
            } ?: ""
            info { "EndFling$sp(${scroller.currX} - ${scroller.finalX}),(${scroller.currY} - ${scroller.finalY})" }
            timeSpan = 0
        }
    }

    //region    inner class
    inner class XAnimation() : AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.animatedValue as Float
            host.moveTo(offset, host.visY)
        }

        override fun onAnimationCancel(animation: Animator?) {
            if (animation is ValueAnimator){
                val value = animation.animatedValue as Float
                info { "CancelX(${System.currentTimeMillis()-timeSpan})ms(${value.toInt()})" }
                timeSpan = 0
            }
        }

        override fun onAnimationEnd(animation: Animator?) {
            if (animation is ValueAnimator){
                val value = animation.animatedValue as Float
                info { "EndX(${System.currentTimeMillis()-timeSpan})ms(${value.toInt()})" }
                timeSpan = 0
            }
        }
    }

    inner class YAnimation() : AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.animatedValue as Float
            host.moveTo(host.visX, offset)
        }

        override fun onAnimationCancel(animation: Animator?) {
            if (animation is ValueAnimator){
                val value = animation.animatedValue as Float
                info { "CancelY(${System.currentTimeMillis()-timeSpan})ms(${value.toInt()})" }
                timeSpan = 0
            }
        }

        override fun onAnimationEnd(animation: Animator?) {
            if (animation is ValueAnimator){
                val value = animation.animatedValue as Float
                info { "EndY(${System.currentTimeMillis()-timeSpan})ms(${value.toInt()})" }
                timeSpan = 0
            }
        }
    }

    //endregion
}
//endregion

class NormalView(ctx: Context):View(ctx),ViewCallback,AnkoLogger{
    override val loggerTag: String
        get() = VIEW_TAG
    private val animationManager:ViewAnimationManger
    private val textPaint: Paint
    private val fontSize = 26
    private val state = State.DEFAULT
    private val drawTextLines:MutableList<String>

    private var hasSize = false
    init {
        animationManager = ViewAnimationManger(this)
        textPaint = Paint().apply {
            textSize = sp(fontSize).toFloat()
            color = Color.BLACK
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
        }
        drawTextLines = mutableListOf()
        loadConfig()
    }

    private fun loadConfig(){
        drawTextLines.add("$visX")
    }

    //region    override
    override fun onDraw(canvas: Canvas) {
        if(isInEditMode){
            return
        }
        //region    draw background
        var isDrawBackground = false
        background?.let {
            isDrawBackground = true
            it.draw(canvas)
        }
        if (!isDrawBackground){
            canvas.drawColor(Color.WHITE)
        }
        //endregion

    }

    override fun computeScroll() {
        super.computeScroll()
        if (isInEditMode){
            return
        }
        animationManager.computeFling()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        hasSize = true
        super.onSizeChanged(w, h, oldw, oldh)
        if (isInEditMode || state != State.SHOWN){
            return
        }
        animationManager.stopAll()
    }
    //endregion

    private enum class State {DEFAULT, LOADED, SHOWN, ERROR}

    //region    ViewCallback
    override val view: View
        get() = this
    override var visX: Float = 0F
        private set
    override var visY: Float = 0F
        private set

    override fun moveTo(x: Float, y: Float) {
        visX = x
        visY = y
        invalidate()
    }

    override fun moveOffset(dx: Float, dy: Float) {
        visX += dx
        visY += dy
        invalidate()
    }
    //endregion
}
