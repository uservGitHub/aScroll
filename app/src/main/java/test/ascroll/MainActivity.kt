package test.ascroll

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.ctx
import test.ascroll.scroll.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*setContentView(TextView(ctx).apply {
            text = "这是文本"
            gravity = Gravity.CENTER
        })*/
        setContentView(
                NormalView(ctx),
                ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT))
    }
}
