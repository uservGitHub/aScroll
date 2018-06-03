package test.ascroll.scroll

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.sp

private val VIEW_TAG = "_View"

class DefaultSplitLine(ctx: Context): FrameLayout(ctx),SplitScreen, AnkoLogger {
    override val loggerTag: String
        get() = VIEW_TAG
    private var isSeup = false
    private lateinit var host:NormalView
    private lateinit var tbMessage:TextView
    init {
        tbMessage = TextView(ctx).apply {
            gravity = Gravity.CENTER
            textSize = sp(20).toFloat()
            text = "显示当前坐标"
            //setBackgroundColor(Color.TRANSPARENT)
        }
        visibility = View.INVISIBLE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

    }

    //region    SplitScreen

    override fun setupLayout(view: NormalView) {
        if (isSeup){
            return
        }
        isSeup = true
        val tvlp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        val panel = LinearLayout(view.context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(tbMessage)
        }
        addView(panel, tvlp)

        val lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        //lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        //view.addView(this, lp)
        view.addView(this, lp)

        host = view
        panel.x = host.visX
        panel.y = host.visY
    }

    override fun destroyLayout() {
        if (isSeup) {
            host.removeView(this)
        }
    }

    override fun hide() {
        visibility = View.INVISIBLE
    }

    override fun hideDelayed() {
        postDelayed({hide()},1000)
    }

    override fun show() {
        visibility = View.VISIBLE
    }

    override fun shown(): Boolean {
        return visibility == View.VISIBLE
    }
    //endregion
}