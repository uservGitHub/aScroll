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
import android.widget.FrameLayout
import android.widget.OverScroller
import android.widget.RelativeLayout
import android.widget.TextView
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.dip
import org.jetbrains.anko.info
import org.jetbrains.anko.sp
import java.util.*

private val VIEW_TAG = "_View"
private val randColor:Int get() {
    val r = Random()
    return Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256))
}
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
    val visBoundX:Float
    val visBoundY:Float
    val view:View
    val splitView:SplitScreen
    val isSwipeVertical:Boolean
    fun moveTo(x:Float, y:Float)
    fun moveOffset(dx:Float, dy:Float)
    fun hiting(e:MotionEvent)
}

class ViewAnimationManger(private val host:ViewCallback):AnkoLogger {
    override val loggerTag: String
        get() = VIEW_TAG
    private val scroller: OverScroller
    private var animation: ValueAnimator? = null
    private var fling = false
    private var beginFlingTick:Long = 0
    private var beginAnimationTick:Long = 0
    private var skipToEnd:Boolean = false
    private var endValue:Float = 0F

    init {
        scroller = OverScroller(host.view.context)
    }

    //region startXAnimation startYAnimation startFlingAnimation
    fun startXAnimation(xFrom: Float, xTo: Float, toEnd:Boolean = false) {
        info { "startXAnimation(${xFrom.toInt()},${xTo.toInt()})" }
        stopAll()
        skipToEnd = toEnd
        endValue = xTo
        beginAnimationTick = System.currentTimeMillis()
        animation = ValueAnimator.ofFloat(xFrom, xTo).apply {
            interpolator = DecelerateInterpolator()
            duration = 400
            val xAnimation = XAnimation()
            addUpdateListener(xAnimation)
            addListener(xAnimation)

            start()
        }
    }

    fun startYAnimation(yFrom: Float, yTo: Float, toEnd:Boolean = false) {
        info { "startYAnimation(${yFrom.toInt()},${yTo.toInt()})" }
        stopAll()
        skipToEnd = toEnd
        endValue = yTo
        beginAnimationTick = System.currentTimeMillis()
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
        beginFlingTick = System.currentTimeMillis()
        fling = true
        scroller.fling(startX, startY, velX, velY, minX, maxX, minY, maxY)
    }
    //endregion

    //region    stopAll stopFling
    fun stopAll() {
        animation?.cancel()
        animation = null
        stopFiling()
    }

    /**
     * 停止加速（是否要到结束点）
     */
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
            val sp = "(${System.currentTimeMillis()-beginFlingTick})ms"
            info { "EndFling$sp(${scroller.currX} - ${scroller.finalX}),(${scroller.currY} - ${scroller.finalY})" }
        }
    }

    //region    inner class
    inner class XAnimation() : AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.animatedValue as Float
            host.moveTo(offset, host.visY)
        }

        override fun onAnimationCancel(animation: Animator?) {
            if (skipToEnd){
                host.moveTo(endValue, host.visY)
            }
            info { "CancelX(${System.currentTimeMillis()-beginAnimationTick})ms" }
        }

        override fun onAnimationEnd(animation: Animator?) {
            info { "EndX(${System.currentTimeMillis()-beginAnimationTick})ms(${host.visX}=$endValue)" }
        }
    }

    inner class YAnimation() : AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.animatedValue as Float
            host.moveTo(host.visX, offset)
        }

        override fun onAnimationCancel(animation: Animator?) {
            if (skipToEnd){
                host.moveTo(host.visX, endValue)
            }
            info { "CancelY(${System.currentTimeMillis()-beginAnimationTick})ms($endValue)" }
        }

        override fun onAnimationEnd(animation: Animator?) {
            info { "EndY(${System.currentTimeMillis()-beginAnimationTick})ms(${host.visY}=${endValue})" }
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
    private var dbClickTick: Long = 0

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

    //DoubleClick后面为什么要触发一个Down操作呢？
    override fun onDoubleTap(e: MotionEvent): Boolean {
        //可以进行放大等双击操作
        dbClickTick = System.currentTimeMillis()
        info { "hostDbClick" }
        if (!host.splitView.shown()){
            host.splitView.show()
        }
        /*if (host.isSwipeVertical){
            if (host.visBoundY != host.visY){
                animationManger.startYAnimation(host.visY, host.visBoundY, true)
            }
        }else{
            if (host.visBoundX != host.visX){
                animationManger.startXAnimation(host.visX, host.visBoundX, true)
            }
        }*/
        return true
    }

    override fun onLongPress(e: MotionEvent?) = Unit
    override fun onSingleTapUp(e: MotionEvent?) = false
    override fun onDoubleTapEvent(e: MotionEvent?) = false
    override fun onShowPress(e: MotionEvent?) = Unit
    override fun onDown(e: MotionEvent): Boolean {
        val tick = System.currentTimeMillis()
        //防止DbClick后面紧跟着执行stopAll操作
        if (tick - dbClickTick > 50) {
            info { "Down-StopAll" }
            animationManger.stopAll()
        }
        host.hiting(e)
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        if (!scrolling){
            info { "BeginScroll" }
            scrolling = true
        }
        if (host.isSwipeVertical){
            if (Math.abs(distanceX)>Math.abs(distanceY)*1.5F){
                host.moveOffset(-distanceX, 0F)
            }else{
                host.moveOffset(0F, -distanceY)
            }
        }
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        if (host.isSwipeVertical) {
            val maxVelY = host.view.height * 1
            val velY = if (velocityY > maxVelY) maxVelY else velocityY.toInt()
            val y = host.visY.toInt()
            val minY = y - 8000
            val maxY = y + 8000
            animationManger.startFlingAnimation(host.visX.toInt(), y,
                    0, velY,
                    0, 0,
                    minY, maxY)
        } else {
            val maxVelX = host.view.width * 1
            val velX = if (velocityX > maxVelX) maxVelX else velocityX.toInt()
            val x = host.visX.toInt()
            //模拟无限
            val minX = x - 8000
            val maxX = x + 8000
            animationManger.startFlingAnimation(x, host.visY.toInt(),
                    velX, 0,
                    minX, maxX,
                    0, 0)
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
        //回弹不能移动的边
        if (host.isSwipeVertical){
            if (host.visBoundX != host.visX){
                animationManger.startXAnimation(host.visX, host.visBoundX, true)
            }
        }else{
            if (host.visBoundY != host.visY){
                animationManger.startYAnimation(host.visY, host.visBoundY, true)
            }
        }
    }
}

//endregion

class NormalView(ctx: Context):RelativeLayout(ctx),ViewCallback,AnkoLogger {
    companion object {
        private const val SelectHandleColor = Color.RED
        private const val DefaultHandleColor = Color.BLACK
        private fun strokePaintFrom(isSelected: Boolean, thickness: Float): Paint =
                Paint().apply {
                    color = if (isSelected) SelectHandleColor else DefaultHandleColor
                    style = Paint.Style.STROKE
                    strokeWidth = thickness
                    strokeCap = Paint.Cap.BUTT
                    strokeJoin = Paint.Join.MITER
                }

        private fun textPaintFrom(colorInt: Int, fontSize: Float) =
                Paint().apply {
                    textSize = fontSize
                    color = colorInt
                    style = Paint.Style.FILL
                    textAlign = Paint.Align.LEFT
                }
    }

    override val loggerTag: String
        get() = VIEW_TAG
    private val animationManager: ViewAnimationManger
    private val dragPinchManager: ViewDragPinchManager
    //destroy时要设置为null
    private var splitScreen: SplitScreen? = null
    internal var splitLine: SplitScreen? = null
    private val textPaint: Paint
    private val fontSize = 26
    private var state = State.DEFAULT
    private val drawTextLines: MutableList<String>
    private var visType:Int = 0
    internal val visRects = mutableListOf<VisRect>()


    private var hasSize = false

    init {
        setWillNotDraw(false)
        animationManager = ViewAnimationManger(this)
        dragPinchManager = ViewDragPinchManager(this, animationManager).apply {
            enable()
        }
        splitLine = DefaultSplitLine(ctx).apply {
            setupLayout(this@NormalView)
        }
        splitScreen = DefaultSplitScreen(ctx).apply {
            setupLayout(this@NormalView)
        }

        textPaint = Paint().apply {
            textSize = sp(fontSize).toFloat()
            color = Color.BLACK
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
        }
        drawTextLines = mutableListOf()
        loadConfig()
        state = State.SHOWN
        visRects.add(VisRect().apply {

        })
    }

    private fun loadConfig() {
        drawTextLines.add("""pt (${visX.toInt()},${visY.toInt()})""")
    }

    //region internal
    /**
     * 添加、调整布局、刷新
     */
    internal fun autoAdd() {
        if (visRects.size != 1) {
            return
        }
        visRects.add(VisRect())
        updateVisRectLayout()
        invalidate()
    }

    /**
     * 删除、调整布局、刷新
     */
    internal fun remove(visRect: VisRect){
        if (visRects.size != 2){
            return
        }
        visRects.remove(visRect)
        updateVisRectLayout()
        invalidate()
    }

    internal fun reverse(){
        if (visRects.size != 2){
            return
        }
        updateVisRectLayout(true)
        invalidate()
    }

    /**
     * 调整布局
     */
    private fun updateVisRectLayout(reverse:Boolean = false, isAuto:Boolean = true){
        when{
            reverse -> {
                visType = if (visType == VisRect.HOR_ALL)VisRect.VER_ALL else VisRect.HOR_ALL
            }
            isAuto -> {
                if (height == width){
                    info { "visType=0" }
                    visType = VisRect.FILL_ALL
                }else {
                    visType = if (height > width) VisRect.VER_ALL else VisRect.HOR_ALL
                }
            }
        }
        val rectF = RectF(0F, 0F, width.toFloat(), height.toFloat())
        info { "updateVisLayout(count=${visRects.size},visType=$visType)" }
        VisRect.nativeArrangeVisRectLayout(visRects, rectF, visType)
    }

    /**
     * 回弹
     */
    internal fun springBack() {
        if (isSwipeVertical) {
            if (visBoundY != visY) {
                animationManager.startYAnimation(visY, visBoundY, true)
            }
        } else {
            if (visBoundX != visX) {
                animationManager.startXAnimation(visX, visBoundX, true)
            }
        }
    }
    //endregion

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

        //region    visRects
        val dt = dip(30)
        visRects.forEachIndexed { index, visRect ->
            //val rect = Rect(dt, dt, width - dt, height - dt)
            info { visRect }
            visRect.look(canvas){
                //canvas.drawRect(rect, Paint().also { it.color = visRect.testColor })
                val paint = textPaintFrom(visRect.testColor, sp(fontSize + 4*(index+1)).toFloat())
                canvas.drawText(
                        "$index:(${visX.toInt()},${visY.toInt()}),visRect$index:($visRect)",
                        visRect.worldX+visRect.clipX,visRect.worldY+visRect.clipY,paint)
            }
        }
        //endregion


    }

    override fun computeScroll() {
        super.computeScroll()
        if (isInEditMode) {
            return
        }
        animationManager.computeFling()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        hasSize = true
        super.onSizeChanged(w, h, oldw, oldh)
        if (isInEditMode || state != State.SHOWN) {
            return
        }
        info { "hasSize($w,$h)" }
        animationManager.stopAll()
        updateVisRectLayout(false, false)
    }
    //endregion

    private enum class State { DEFAULT, LOADED, SHOWN, ERROR }

    //region ViewCallback

    override val splitView: SplitScreen
        get() = splitScreen!!
    override var isSwipeVertical: Boolean = true
        private set
    override val view: View
        get() = this
    override var visX: Float = 0F
        private set
    override var visY: Float = 0F
        private set
    override var visBoundX: Float = 0F
        private set
    override var visBoundY: Float = 0F
        private set


    override fun moveTo(x: Float, y: Float) {
        visX = x
        visY = y
        visRects.forEach {
            if (it.flagHiting){
                it.worldX = x
                it.worldY = y
            }
        }
        invalidate()
    }

    override fun moveOffset(dx: Float, dy: Float) {
        visX += dx
        visY += dy
        visRects.forEach {
            if (it.flagHiting){
                it.worldX += dx
                it.worldY += dy
            }
        }
        invalidate()
    }

    override fun hiting(e: MotionEvent) {
        visRects.forEach {
            it.flagHiting = it.clipRect.contains(e.x, e.y)
        }
    }

    //endregion

    //region    VisRect
    class VisRect(val testColor: Int = randColor){
        var width: Float = 0F
        var height: Float = 0F
        var clipX: Float = 0F
        var clipY: Float = 0F

        companion object {
            /**
             * 水平排列，从左向右，等分
             */
            val FILL_ALL = 0    //全屏布置
            val HOR_ALL = 1 //水平排列，从左向右，等分
            val VER_ALL = 2 //垂直排列，从上向下，等分

            val MOVE_CLIP = 1   //移动窗口
            val MOVE_WORLD = 2  //移动世界坐标系

            val MOVE_CLIP_L = 1   //移动左

            val MOVE_CLIP_T = 2   //移动左

            //region    native模型函数
            /**
             * 重新排列所有可视区
             */
            internal fun nativeArrangeVisRectLayout(list: List<VisRect>, rect: RectF, type: Int) {

                val count = list.size
                if (count == 0) {
                    return
                }

                when (type) {
                    VisRect.HOR_ALL -> {
                        val dWidth = rect.width() / count
                        var x = rect.left
                        list.forEachIndexed { index, visRect ->
                            visRect.also {
                                it.clipX = x
                                it.clipY = rect.top
                                it.width = dWidth
                                it.height = rect.height()
                                //这个值添加的时候设置
                                //it.zIndex = index + 1 //从1开始
                            }
                            x += dWidth
                        }
                    }
                    VisRect.VER_ALL -> {
                        val dHeight = rect.height() / count
                        var y = rect.top
                        list.forEachIndexed { index, visRect ->
                            visRect.also {
                                it.clipX = 0F
                                it.clipY = y
                                it.width = rect.width()
                                it.height = dHeight
                                //这个值添加的时候设置
                                //it.zIndex = index + 1 //从1开始
                            }
                            y += dHeight
                        }
                    }
                    VisRect.FILL_ALL ->{
                        list.forEachIndexed { index, visRect ->
                            visRect.also {
                                it.clipX = 0F
                                it.clipY = 0F
                                it.width = rect.width()
                                it.height = rect.height()
                                //这个值添加的时候设置
                                //it.zIndex = index + 1 //从1开始
                            }
                        }
                    }
                }
            }
            //endregion
        }

        var worldX: Float = 0F
        var worldY: Float = 0F
        var zIndex = 0
        var locked = true   //不随动
        var flagHiting = false  //点击命中，选中，只能有一个选中
        var flagWorldMoving = false //世界坐标系可移动
        var flagClipMoving = false //视区起点可移动
        val clipRect: RectF get() = RectF(clipX, clipY, clipX + width, clipY + height)

        fun look(canvas: Canvas, f: (Canvas) -> Unit) {
            canvas.save()
            canvas.clipRect(clipRect)   //指定区域
            if (flagHiting) {
                //canvas.drawColor(Color.LTGRAY)
            }
            //look的位置
            //canvas.translate(-worldX, -worldY)
            //draw world
            f.invoke(canvas)
            canvas.restore()
        }

        fun moveWorldOffset(dx: Float, dy: Float) {
            worldX += dx
            worldY += dy
        }

        fun moveClipOffset(dx: Float, dy: Float) {
            //要求内容world也要随之移动
            clipX += dx
            clipY += dy
            moveWorldOffset(-dx, -dy)
        }

        override fun toString(): String {
            return "clip:$clipRect,world:${PointF(worldX,worldY)}"
        }
    }


    //endregion

    inner class VisLine(
            val horizon: Boolean,
            var showing: Boolean = false
    ) {

    }

}
