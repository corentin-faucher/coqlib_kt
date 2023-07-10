/** CoqActivity.kt
 * Class de base pour une app OpenGL.
 * -> Il faut créé la root (AppRootBase) dans onCreate avant le super.onCreate.
 * 11 novembre 2022
 * Corentin Faucher
 * */

package com.coq.coqlib

import android.content.Context
import android.content.res.Configuration
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import com.coq.coqlib.graph.Renderer
import com.coq.coqlib.graph.Texture
import com.coq.coqlib.nodes.AppRootBase
import com.coq.coqlib.nodes.Hoverable
import com.coq.coqlib.nodes.Node
import com.coq.coqlib.nodes.Surface
import java.lang.ref.WeakReference
import java.util.*

abstract class CoqActivity(private val appThemeID: Int,
                           private val vertShaderID: Int?,
                           private val fragShaderID: Int?,
                           private val setForDrawing: (Node.() -> Surface?)?
                           ) : AppCompatActivity()
{
    private lateinit var renderer: Renderer
    internal lateinit var view: GLSurfaceView
    // L'activité garde la référence du context localisé (e.g. français)
    // (Ne peut pas être gardé par Language... Mais c'est Language qui l'utilise.)
    internal var localizedCtx: Context? = null

    abstract fun getExtraTextureTilings() : Map<Int, Texture.Tiling>?
    abstract fun getExtraSoundIdsWithVolumeIds() : Array<Pair<Int, Int>>?
    abstract fun getAppRoot() : AppRootBase

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appThemeID)
        super.onCreate(savedInstanceState)
        // 1. Init des sons
        SoundManager.initWith(this, getExtraSoundIdsWithVolumeIds())
        // 2. Init view OpenGL
        view = GLSurfaceView(this)
        view.setEGLContextClientVersion(2)
        view.preserveEGLContextOnPause = true
        // 3. Init du renderer : init aussi Texture, Mesh, Root.
        renderer = Renderer(this, vertShaderID, fragShaderID, setForDrawing)
        view.setRenderer(renderer)
        setContentView(view)
        currentView = WeakReference(view)

        Language.initWith(this)

        printdebug("Activity on create.")
    }

//    override fun attachBaseContext(newBase: Context) {
//        val language = if(nextBoolean()) "en" else "fr"
//        Language.getContextForLanguage(newBase, language)
//        super.attachBaseContext(newBase)
//    }

    override fun onResume() {
        super.onResume()
        view.onResume()
        printdebug("Activity on resume.")
    }
    override fun onPause() {
        super.onPause()
        with(view) {
            queueEvent {
                renderer.onPause()
            }
        }
        view.onPause()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event == null) { return super.onTouchEvent(null) }
        Hoverable.inTouchScreen = event.source == InputDevice.SOURCE_TOUCHSCREEN
        when(event.action) {
            MotionEvent.ACTION_DOWN -> view.queueEvent {
                renderer.onDown(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> view.queueEvent { renderer.onMove(event.x, event.y) }
            MotionEvent.ACTION_CANCEL -> { printwarning("Cancel ? utile ?"); view.queueEvent { renderer.onUp() }}
            MotionEvent.ACTION_UP -> view.queueEvent { renderer.onUp() }
            else -> return false
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if(event == null) { return super.onTouchEvent(null) }
        Hoverable.inTouchScreen = event.source == InputDevice.SOURCE_TOUCHSCREEN
        when(event.action) {
            MotionEvent.ACTION_HOVER_MOVE -> view.queueEvent { renderer.onHovering(event.x, event.y) }
            MotionEvent.ACTION_SCROLL -> view.queueEvent { renderer.onScroll(
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL)) }
            else -> {
                // Ok, semble marché si on retourne false (sera récupéré par onTouchEvent on dirait...)
                // printdebug("Other unhandled generic motion ${MotionEvent.actionToString(event.action)}.")
                return false
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(event == null || event.repeatCount > 0)
            return super.onKeyDown(keyCode, event)
        with(view) { queueEvent {
            renderer.onKeyDown(KeyboardInputStruct(event.scanCode, keyCode, event.metaState, false))
        }}
        return if(keyCode == KeyEvent.KEYCODE_ESCAPE) true else super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if(event == null) return super.onKeyUp(keyCode, null)
        with(view) { queueEvent {
            renderer.onKeyUp(KeyboardInputStruct(event.scanCode, keyCode, event.metaState, false))
        }}
        return super.onKeyUp(keyCode, event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            WindowCompat.setDecorFitsSystemWindows(window, false)
//        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
//        }
        // TODO check pour les status en haut de l'écran...
//        WindowInsetsControllerCompat(window, mainContainer)
    }

    internal fun setLocalizedContextTo(iso: String) {
            val config = Configuration(resources.configuration)
            val locale = Locale(iso)
            config.setLocale(locale)
            localizedCtx = createConfigurationContext(config)
        }

    companion object {
        internal var currentView: WeakReference<GLSurfaceView> = WeakReference(null)
    }
}



