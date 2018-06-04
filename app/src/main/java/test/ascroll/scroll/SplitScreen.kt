package test.ascroll.scroll

import android.content.Context
import android.transition.Visibility
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*

private val VIEW_TAG = "_View"

interface SplitScreen{
    fun setupLayout(view: NormalView)
    fun destroyLayout()
    fun shown():Boolean
    fun show()
    fun hide()
    fun hideDelayed()
}

class DefaultSplitScreen(ctx:Context):RelativeLayout(ctx),SplitScreen,AnkoLogger{
    override val loggerTag: String
        get() = VIEW_TAG
    private var isSeup = false
    private lateinit var host:NormalView
    //region btn
    private lateinit var btnSplit:Button
    private lateinit var btnSpringBack:Button
    private lateinit var btnOpenLine:Button
    private lateinit var btnAdd:Button
    private lateinit var btnRemove:Button
    private lateinit var btnReverse:Button
    //endregion

    //region    excute
    val close:()->Unit = { hide() }
    val tback:()->Unit = { host.springBack(); hideDelayed() }
    val add:()->Unit = { host.autoAdd() }
    val rFirst:()->Unit = { host.remove(host.visRects.first()) }
    val rLast:()->Unit = { host.remove(host.visRects.last()) }
    val reverse:()->Unit = { host.reverse() }
    //endregion

    init {
        btnSplit = Button(ctx).apply {
            text = "关闭"
            setOnClickListener {
                info { "call SplitView's Close" }
                hide()
            }
        }
        btnSpringBack = Button(ctx).apply {
            text = "回弹"
            setOnClickListener {
                info { "call SplitView's SpringBack" }
                host.springBack()
                hideDelayed()
            }
        }
        btnOpenLine = Button(ctx).apply {
            text = "显示/影藏"
            setOnClickListener {
                info { "call SplitLine's View" }
                host.splitLine?.let {
                    if (it.shown())it.hide()
                    else it.show()
                }
            }
        }
        btnAdd = Button(ctx).apply {
            text = "添加"
            setOnClickListener {
                info { "call Add VisRect" }
                host.autoAdd()
            }
        }
        btnRemove = Button(ctx).apply {
            text = "随机删除"
            setOnClickListener {
                info { "call random remove VisRect" }
                val random = Random()
                val index = Math.abs(random.nextInt())
                val visRect = host.visRects[index.rem(host.visRects.size)]
                host.remove(visRect)
            }
        }
        btnReverse = Button(ctx).apply {
            text = "反转"
            setOnClickListener {
                info { "call random reverse VisRects" }
                host.reverse()
            }
        }

        visibility = View.INVISIBLE
    }

    //region    SplitScreen

    override fun setupLayout(view: NormalView) {
        if (isSeup){
            return
        }
        isSeup = true
        val tvlp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        tvlp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        val panel = LinearLayout(view.context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(btnSpringBack)
            addView(btnSplit)
            addView(btnOpenLine)
            addView(btnAdd)
            addView(btnRemove)
            addView(btnReverse)
        }
        addView(panel, tvlp)

        val lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        view.addView(this, lp)

        host = view
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