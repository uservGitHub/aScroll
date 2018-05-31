package test.ascroll.scroll

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.InputType
import android.view.GestureDetector
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


//region    ViewCallback ViewAnimationManager
interface ViewCallback{
    val visX:Float
    val visY:Float
    val view:View
    val isSwipeVertical:Boolean
    fun moveTo(x:Float, y:Float)
    fun moveOffset(dx:Float, dy:Float)
    //动画不还原，加速可以还原
    var hasMove:Boolean

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
            host.hasMove = true
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
            host.hasMove = true
            start()
        }
    }

    fun startFlingAnimation(startX: Int, startY: Int, velX: Int, velY: Int,
                            minX: Int, maxX: Int, minY: Int, maxY: Int) {
        info { "startFlingAnimation($startX,$velX,$minX - $maxX),($startY,$velY,$minY - $maxY)" }
        stopAll()
        fling = true
        timeSpan = System.currentTimeMillis()
        host.hasMove = true
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
        if (fling) {
            info { "stopFling" }
            if (toFinal) {
                scroller.abortAnimation()
                host.view.postInvalidate()
            } else {
                scroller.forceFinished(true)
                host.view.invalidate()
            }
        }
    }
    //endregion

    fun computeFling() {
        if (scroller.computeScrollOffset()) {
            host.moveTo(scroller.currX.toFloat(), scroller.currY.toFloat())
        } else if (fling) {
            fling = false
            host.moveTo(scroller.currX.toFloat(), scroller.currY.toFloat())
            val sp = "(${System.currentTimeMillis()-timeSpan})ms"
            info { "EndFling$sp(${scroller.currX} - ${scroller.finalX}),(${scroller.currY} - ${scroller.finalY})" }
            timeSpan = 0
            host.hasMove = false
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

//region    ViewDragPinchManager

class ViewDragPinchManager(private val host: ViewCallback, private val animationManger: ViewAnimationManger):
        GestureDetector.OnGestureListener, View.OnTouchListener,GestureDetector.OnDoubleTapListener,AnkoLogger{
    override val loggerTag: String
        get() = VIEW_TAG
    private val gestureDetector:GestureDetector
    private var scrolling = false
    private var enabled = false
    init {
        gestureDetector = GestureDetector(host.view.context, this)
        host.view.setOnTouchListener(this)
    }
    fun enable(){
        enabled = true
    }
    fun disable(){
        enabled = false
    }
    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        //内部控件的单击处理，比如ScrollHandled的处理
        //...
        info { "hostPreClick" }
        host.view.performClick()
        return true
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        //可以进行放大等双击操作
        //...
        info { "hostDbClick" }
        return true
    }

    override fun onLongPress(e: MotionEvent?) = Unit
    override fun onSingleTapUp(e: MotionEvent?) = false
    override fun onDoubleTapEvent(e: MotionEvent?) = false
    override fun onShowPress(e: MotionEvent?) = Unit
    override fun onDown(e: MotionEvent?): Boolean {
        info { "Down" }
        animationManger.stopAll()
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        if (!scrolling){
            info { "BeginScroll" }
            scrolling = true
        }
        host.moveOffset(-distanceX, -distanceY)
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        if (host.isSwipeVertical){
            animationManger.startFlingAnimation(host.visX.toInt(),host.visY.toInt(),
                    0,velocityY.toInt(),
                    0,0,
                    -host.view.height,host.view.height)
        }
        return true
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!enabled){
            return false
        }
        val retVal = gestureDetector.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_UP){
            if (scrolling){
                scrolling = false
                onScrollEnd(event)
            }
        }
        return retVal
    }
    private fun onScrollEnd(event: MotionEvent){
        info { "EndScroll" }
        //pdfView.tryLoadPages()
        //是否回弹
        //host.hasMove = false
    }
}

//endregion

class NormalView(ctx: Context):View(ctx),ViewCallback,AnkoLogger{
    override val loggerTag: String
        get() = VIEW_TAG
    private val animationManager:ViewAnimationManger
    private val dragPinchManager:ViewDragPinchManager
    private val textPaint: Paint
    private val fontSize = 26
    private val state = State.DEFAULT
    private val drawTextLines:MutableList<String>

    private var hasSize = false

    init {
        animationManager = ViewAnimationManger(this)
        dragPinchManager = ViewDragPinchManager(this, animationManager).apply {
            enable()
        }
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
        drawTextLines.add("""pt (${visX.toInt()},${visY.toInt()})""")
    }

    //region    override
    override fun onDraw(canvas: Canvas) {
        if (isInEditMode) {
            return
        }
        //region    draw background
        var isDrawBackground = false
        background?.let {
            isDrawBackground = true
            it.draw(canvas)
        }
        if (!isDrawBackground) {
            canvas.drawColor(Color.WHITE)
        }
        //endregion

        //region    textlines
        val dh = sp(fontSize + 4).toFloat()
        val cx = width.toFloat() / 2
        val list = listOf<String>("pt (${visX.toInt()},${visY.toInt()})")
        var vcy = (height.toFloat() - dh * list.size) / 2

        list.forEach {
            /*if (hasMove) {
                canvas.drawText(it, cx, vcy, textPaint)
            } else {
                canvas.drawText(it, cx + visX, vcy + visY, textPaint)
            }*/
            canvas.drawText(it, cx, vcy, textPaint)
            canvas.drawText(it, cx + visX, vcy + visY, textPaint)
            vcy += dh
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

    override var isSwipeVertical: Boolean = true
        private set
    override var hasMove: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    //1.5秒后还原位置
                    postDelayed({
                        invalidate()
                        visX = 0F
                        visY = 0F
                    }, 1500)
                }
            }
        }
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
