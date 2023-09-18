package com.coq.coqlib.nodes

import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.coq.coqlib.R
import com.coq.coqlib.printdebug

class TextViewWrapper(
    ref: Node?, private val root: AppRootBase,
    private val htmlText: String,
    x: Float, y: Float, width: Float, height: Float
) : Node(ref, x, y, width, height) {
    private var textView: TextView? = null
    init {
        addRootFlag(Flag1.reshapeableRoot)
    }
    override fun open() {
        super.open()
        val (pos, delta) = positionAndDeltaAbsolute()
        printdebug("pos and delta $pos, $delta.")
        val (xy, wh) = root.getFrameFrom(pos, delta)

        Handler(Looper.getMainLooper()).post {
            val textView = LayoutInflater.from(root.ctx)
                .inflate(R.layout.scroll_text_view, null, false) as TextView
            textView.x = xy.x
            textView.y = xy.y
            textView.text = Html.fromHtml(
                htmlText, Html.FROM_HTML_MODE_LEGACY
            )
            textView.setBackgroundResource(R.drawable.rounded_corner)
            textView.setTextColor(root.ctx.resources.getColor(R.color.black, null))
            textView.textSize = 20f
            textView.movementMethod = ScrollingMovementMethod()
            textView.isVerticalScrollBarEnabled = true
            textView.scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            textView.isScrollbarFadingEnabled = false
            textView.scrollBarSize = 5

            val params = ViewGroup.LayoutParams(wh.x.toInt(), wh.y.toInt())
            textView.layoutParams = params
            textView.requestLayout()
            root.ctx.addContentView(textView, textView.layoutParams)
            this.textView = textView
        }
    }

    override fun reshape() {
        super.reshape()
        val (pos, delta) = positionAndDeltaAbsolute()
        printdebug("reshape pos and delta $pos, $delta.")
        val (xy, wh) = root.getFrameFrom(pos, delta)
        val textView = textView ?: return
        Handler(Looper.getMainLooper()).post {
            textView.x = xy.x
            textView.y = xy.y
            val params = textView.layoutParams
            params.height = wh.y.toInt()
            params.width = wh.x.toInt()
//            textView.layoutParams = params
            textView.requestLayout()
        }
    }

    override fun close() {
        super.close()
        val textView = textView ?: return
        Handler(Looper.getMainLooper()).post {
            (textView.parent as ViewGroup).removeView(textView)
        }
    }
}