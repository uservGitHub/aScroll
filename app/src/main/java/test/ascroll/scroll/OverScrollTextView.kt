package test.ascroll.scroll

import android.content.Context
import android.graphics.*
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.widget.TextView
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
class OverScrollTextView(ctx:Context):TextView(ctx){
    private var touchGroupIndex = 0
    private var touchSate = "None"
        set(value) {
            if (value != field) {
                field = value
                refreshText(field)
            }
        }
    private inline fun refreshText(touchState:String){
        //像素单位，在父坐标中的位置
        val posRect = Rect(left,top,right,bottom)
        //像素单位，可视坐标范围
        val visRectF = RectF(x,y,x+width,y+height)
        text = "pos:$posRect\nvis:$visRectF\n$touchGroupIndex$touchSate"
    }
    //按下时的值（可以是可视坐标x,y 也可以是屏幕坐标rawX,rawY）
    private var downX:Float = 0F
    private var downY:Float = 0F
    //上次移动后的值（同down同类型）
    private var lastMoveX:Float = 0F
    private var lastMoveY:Float = 0F
    init {
        //多行显示
        inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        setSingleLine(false)
        gravity = Gravity.CENTER
        textSize = sp(16).toFloat()
        refreshText("Init")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //用屏幕坐标
                lastMoveX = event.rawX
                lastMoveY = event.rawY

                touchGroupIndex++
                touchSate = "_DOWN"
                //TextView默认不消费该事件，需要设置
                //这里消费了，下面的自动消费，它们是一组
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastMoveX
                var dy = event.rawY - lastMoveY
                offsetLeftAndRight(dx.toInt())
                offsetTopAndBottom(dy.toInt())
                lastMoveX = event.rawX
                lastMoveY = event.rawY

                touchSate = "_MOVE"
            }
            MotionEvent.ACTION_UP -> {
                touchSate = "_UP"
            }
        }
        return super.onTouchEvent(event)
    }

}