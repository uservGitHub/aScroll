package test.ascroll.scroll

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import android.widget.RelativeLayout
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 *
 *
 * 注意：
 * 1.computeScroll是通过draw来调用的，里面内容是空
 * 通过它，可实施连续修改偏移并刷新，直到偏移到位
 * scroller的scrollTo和scrollBy都会调用刷新，进而调用draw。
 * 2.scroller 的变化过程：forceFinish立即中断，abortAnimation一部到位
 */
class AnimationManger(private val pdfView: PdfView):AnkoLogger{
    override val loggerTag: String
        get() = "_AM"
    private val scroller:OverScroller
    private var animation:ValueAnimator? = null
    private var fling = false
    private var backing = false
    private var pageFling = false
    private var traceTag = "None"
    init {
        scroller = OverScroller(pdfView.context)
    }

    //region startXAnimation startYAnimation startFlingAnimation startSpringBack
    fun startXAnimation(xFrom:Float, xTo:Float){
        info { "startXAnimation" }
        traceTag = "XAnimation"
        stopAll()
        animation = ValueAnimator.ofFloat(xFrom, xTo).apply {
            interpolator = DecelerateInterpolator()
            duration = 400
            val xAnimation = XAnimation()
            addUpdateListener(xAnimation)
            addListener(xAnimation)
            start()
        }
    }
    fun startYAnimation(yFrom:Float, yTo:Float){
        info { "startYAnimation" }
        traceTag = "YAnimation"
        stopAll()
        animation = ValueAnimator.ofFloat(yFrom, yTo).apply {
            interpolator = DecelerateInterpolator()
            duration = 400
            val yAnimation = YAnimation()
            addUpdateListener(yAnimation)
            addListener(yAnimation)
            start()
        }
    }
    fun startFlingAnimation(startX:Int,startY:Int,velX:Int,velY:Int,
                            minX:Int, maxX:Int, minY:Int, maxY:Int) {
        info { "startFlingAnimation" }
        traceTag = "FlingAnimation"
        stopAll()
        fling = true
        scroller.fling(startX, startY, velX, velY, minX, maxX, minY, maxY)
        //pdfView.invalidate()
    }
    fun startSpringBack(startX:Int,startY:Int,
                        minX:Int, maxX:Int, minY:Int, maxY:Int){
        info { "startSpringBack" }
        traceTag = "SpringBack"
        stopAll()
        backing = true
        scroller.springBack(startX,startY,minX,maxX,minY,maxY)
    }
    //endregion

    //region    stopAll stopMoving
    fun stopAll(){
        animation?.cancel()
        animation = null
        stopMoving()
    }
    fun stopMoving(){
        if (fling){
            stopFiling()
        }
        if (backing){
            stopSpringBack()
        }
    }
    private fun stopFiling(){
        fling = false
        info { "stopFling" }
        scroller.forceFinished(true)
    }
    private fun stopSpringBack(){
        backing = false
        info { "stopSpringBack" }
        scroller.abortAnimation()
    }
    //endregion

    fun computeFling(){
        info { "computeFling: $traceTag" }
        if (scroller.computeScrollOffset()){
            info { "computeFling: scrolling" }
            pdfView.moveTo(scroller.currX.toFloat(), scroller.currY.toFloat())
            pdfView.tryLoadOnePage()
        }else if (fling){
            info { "computeFling: endFling" }
            fling = false
            pdfView.tryLoadPages()
        }else if (backing){
            info { "computeFling: endSpringBack" }
            backing = false
        }
    }

    //region    inner class
    inner class XAnimation(): AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener{
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.animatedValue as Float
            pdfView.moveTo(offset, pdfView.visY)
            pdfView.tryLoadOnePage()
        }

        override fun onAnimationCancel(animation: Animator?) {
            pdfView.tryLoadPages()
            pageFling = false
        }

        override fun onAnimationEnd(animation: Animator?) {
            pdfView.tryLoadPages()
            pageFling = false
        }
    }
    inner class YAnimation(): AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener{
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.animatedValue as Float
            pdfView.moveTo(pdfView.visX, offset)
            pdfView.tryLoadOnePage()
        }

        override fun onAnimationCancel(animation: Animator?) {
            pdfView.tryLoadPages()
            pageFling = false
        }

        override fun onAnimationEnd(animation: Animator?) {
            pdfView.tryLoadPages()
            pageFling = false
        }
    }

    //endregion
}
class PdfView(ctx:Context):RelativeLayout(ctx){
    private var animationManger: AnimationManger? = null
    private lateinit var debugPaint: Paint
    private var state = State.NONE
    private var hasSize = false
    init {
        if(!isInEditMode) {

            animationManger = AnimationManger(this)
            setWillNotDraw(false)
            //debug
            debugPaint = Paint().apply {
                style = Paint.Style.STROKE
            }
        }
    }
    fun moveTo(x:Float, y:Float){
        visX = x
        visY = y
        invalidate()
    }
    fun moveOffset(dx:Float, dy:Float){
        visX += dx
        visY += dy
        invalidate()
    }
    var visX:Float = 0F
        private set
    var visY:Float = 0F
        private set
    fun tryLoadOnePage(){}
    fun tryLoadPages(){}

    override fun computeScroll() {
        super.computeScroll()
        if (isInEditMode){
            return
        }
        animationManger?.computeFling()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        hasSize = true
        if (isInEditMode || state != State.SHOW){
            return
        }

        animationManger?.stopAll()
    }

    override fun onDraw(canvas: Canvas) {
        if (isInEditMode){
            return
        }

        canvas.drawText("${visX.toInt()} , ${visY.toInt()}",(width/4).toFloat(),(height/2).toFloat(),debugPaint)
    }

    class State{
        companion object {
            val NONE = 0
            val SHOW = 1
        }
    }
}