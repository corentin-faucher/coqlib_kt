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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.rotationMatrix
import com.coq.coqlib.graph.Renderer
import com.coq.coqlib.graph.Texture
import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.maths.distanceTo
import com.coq.coqlib.nodes.*
import com.coq.coqlib.nodes.Surface
import java.lang.ref.WeakReference
import java.util.*

abstract class CoqActivity(private val appThemeID: Int,
                           private val vertShaderID: Int?,
                           private val fragShaderID: Int?,
                           private val setForDrawing: (Node.() -> Surface?)?
                           ) : AppCompatActivity()
{
    // Téléphone ? i.e. small screen
    val isSmallScreen: Boolean
        get() {
            val conf = resources.configuration
            return when(conf.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                Configuration.SCREENLAYOUT_SIZE_SMALL, Configuration.SCREENLAYOUT_SIZE_NORMAL -> true
            else -> false
            }
        }
    // Windows
    val isWindows: Boolean
        get() = Build.BOARD == "windows"
    // laptop / chromeOS ?
    val isChromeBook: Boolean
        get() = packageManager.hasSystemFeature("org.chromium.arc.device_management")
    // Keyboard ?
    val isKeyboardConnected: Boolean
        get() = resources.configuration.keyboard == Configuration.KEYBOARD_QWERTY
    // Mode sombre
    val isDarkTheme: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            resources.configuration.isNightModeActive else false

    abstract fun getExtraTextureTilings() : Map<Int, Texture.Tiling>?
    abstract fun getExtraSoundIdsWithVolumeIds() : Array<Pair<Int, Int>>?
    abstract fun getAppRoot() : AppRootBase

    var root: AppRootBase? = null
        internal set
    internal var isPaused: Boolean = false
        set(value) {
            RenderingChrono.isPaused = value
            AppChrono.isPaused = value
            if(value != field) {
                if(value)
                    root?.willSleep()
                else
                    root?.didResume(AppChrono.lastSleepTimeSec)
            }
            field = value
        }
    private lateinit var renderer: Renderer
    internal lateinit var view: GLSurfaceView
    // L'activité garde la référence du context localisé (e.g. français)
    // (Ne peut pas être gardé par Language... Mais c'est Language qui l'utilise.)
    internal var localizedCtx: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 0. Reset des chronos
        RenderingChrono.reset()
        AppChrono.reset()
        // 1. Init des sons
        SoundManager.initWith(this, getExtraSoundIdsWithVolumeIds())
        // 1.2 Set language
        Language.initWith(this)
        // 2. Init view OpenGL
        view = GLSurfaceView(this)
        view.setEGLContextClientVersion(2)
        view.preserveEGLContextOnPause = true
        // 3. Init du renderer et de ceux qui depende d'OpenGL : Texture, Mesh, PerInstanceUniforms.
        renderer = Renderer(this, vertShaderID, fragShaderID, setForDrawing)
        // (onSurfaceCreated est call plus tard)
        view.setRenderer(renderer)
        setContentView(view)
        currentView = WeakReference(view)
        setTheme(appThemeID)
    }
    override fun onDestroy() {
        super.onDestroy()
        SoundManager.deinit()
        Language.deinit()
        Texture.deinit()
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Pass pour l'instant... pourrait verifier le clavier ou la langue... (superflu ?)
    }
    // Init de la structure (fait dans onSurfaceCreated du renderer)
    internal fun setAppRoot() {
        val root = getAppRoot()
        if(root.openingScreen == null)
            return
        root.changeActiveScreenToNewOfClass(root.openingScreen!!)
        this.root = root
    }


    /*-- Gestion des events ------------------------*/
    override fun onResume() {
        super.onResume()
        view.onResume()
    }
    override fun onPause() {
        super.onPause()
        isPaused = true
        with(view) {
            queueEvent {
                Texture.suspend()
            }
        }
        view.onPause()
    }

    /*-- Touch et Drag d'objets --*/
    private var downAbsPos = Vector2(0f, 0f)
    private var startMoving = false
    private var wasSlidingMenu = false
    private fun onDown(posX: Float, posY: Float) {
        isPaused = false
        val root = this.root ?: return
        // 1. Position normalisée (et absolue) par rapport à la root, e.g. pos dans [-1.7, 1.7] x [-1, 1].
        downAbsPos = root.getPositionFrom(posX, posY)
        startMoving = false
        wasSlidingMenu = false
        // 2. Essayer de trouver un noeud à cette position.
        val selected = root.searchBranchForSelectableAt(downAbsPos, null) ?: return
        // Vérifier le cas sliding menu (prendre le sliding menu au lieu du selected child si ce dernier n'est pas dragable).
        val touched = if(selected !is Draggable) (selected.parent?.parent as? SlidingMenu) ?: selected else selected
        when(touched) {
            // Prendre si draggable
            is Draggable -> {
                root.grabbedNode = touched
                touched.grab(downAbsPos)
            }
            // Sinon activer le noeud sélectionné (non Draggable)
            is Button -> touched.action()
            is Enterable -> touched.enterAction()
        }
    }
    private fun onMove(newPosX: Float, newPosY: Float) {
        isPaused = false
        val root = this.root ?: return
        val newAbsPos = root.getPositionFrom(newPosX, newPosY)
        val dist = newAbsPos.distanceTo(downAbsPos)
        // Il faut un minimum de déplacement pour "activer" le mode déplacement déplacement.
        // (Si bouge pas vraiment -> simple "touch")
        if( !startMoving && dist < 0.03 ) return
        // Ok, on bouge... (le noeud grabbé si présent)
        startMoving = true
        val grabbed = root.grabbedNode ?: return
        (grabbed as? Draggable)?.drag(newAbsPos)
    }
    private fun onUp() {
        val root = this.root ?: return
        val grabbed = root.grabbedNode ?: return
        root.grabbedNode = null
        (grabbed as? Draggable)?.letGo()
    }
    private fun onHovering(posX: Float, posY: Float) {
        val root = this.root ?: return
        val absPos = root.getPositionFrom(posX, posY)
        val hovered = root.searchBranchForSelectableAt(absPos, null)
        if(hovered !== root.selectedNode) {
            (root.selectedNode as? Hoverable)?.stopHovering()
            root.selectedNode = hovered
        }
        // (Oui, on recall startHovering à chaque cursorMove...
        // -> Il faut arrêter de bouger pour qu'il se passe quelque chose.)
        (hovered as? Hoverable)?.startHovering()
    }
    private fun onScroll(deltaY: Float) {
        isPaused = false
        val root = this.root ?: return
        val scrollable: Node = root.searchBranchForFirstSelectableUsing { it is Scrollable } ?: return
        (scrollable as? Scrollable)?.scroll(deltaY)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event == null) { return super.onTouchEvent(null) }
        Hoverable.inTouchScreen = event.source == InputDevice.SOURCE_TOUCHSCREEN
        when(event.action) {
            MotionEvent.ACTION_DOWN -> view.queueEvent { onDown(event.x, event.y) }
            MotionEvent.ACTION_MOVE -> view.queueEvent { onMove(event.x, event.y) }
            MotionEvent.ACTION_CANCEL -> { printwarning("Cancel ? utile ?"); view.queueEvent { onUp() }}
            MotionEvent.ACTION_UP -> view.queueEvent { onUp() }
            else -> return false
        }
        return true
    }
    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if(event == null) { return super.onTouchEvent(null) }
        Hoverable.inTouchScreen = event.source == InputDevice.SOURCE_TOUCHSCREEN
        when(event.action) {
           // MotionEvent.ACTION_BUTTON_PRESS -> view.queueEvent { onDown(event.x, event.y) } // Juste ACTION_DOWN est suffisant.
            MotionEvent.ACTION_HOVER_MOVE -> view.queueEvent { onHovering(event.x, event.y) }
            MotionEvent.ACTION_SCROLL -> view.queueEvent { onScroll(
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL)) }
            else -> {
                // Ok, semble marché si on retourne false (sera récupéré par onTouchEvent on dirait...)
                // printdebug("Other unhandled generic motion ${MotionEvent.actionToString(event.action)}.")
                return false
            }
        }
        return true
    }

    /*-- Keyboard events --*/
    private fun onKeyDown(key: KeyboardInput) {
        isPaused = false
        val root = this.root ?: return
        when(key._keycode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER ->
                (root.activeScreen as? Enterable)?.let { it.enterAction(); return }
            KeyEvent.KEYCODE_ESCAPE ->
                (root.activeScreen as? Escapable)?.let { it.escapeAction(); return }
        }
        val kr = (root.activeScreen as? KeyResponder) ?: return
        if(Scancode.isMod(key.scancode))
            kr.modifiersChangedTo(key.keymod)
        else
            kr.keyDown(key)
    }
    private fun onKeyUp(key: KeyboardInput) {
        val root = this.root ?: return
        val kr = (root.activeScreen as? KeyResponder) ?: return
        if(Scancode.isMod(key.scancode))
            kr.modifiersChangedTo(key.keymod)
        else
            kr.keyUp(key)
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(event == null || event.repeatCount > 0)
            return super.onKeyDown(keyCode, event)
        with(view) { queueEvent {
            onKeyDown(KeyboardInputStruct(event.scanCode, keyCode, event.metaState, false))
        }}
        return if(keyCode == KeyEvent.KEYCODE_ESCAPE) true else super.onKeyDown(keyCode, event)
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if(event == null) return super.onKeyUp(keyCode, null)
        with(view) { queueEvent {
            onKeyUp(KeyboardInputStruct(event.scanCode, keyCode, event.metaState, false))
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
//        WindowInsetsControllerCompat(window, mainContainer)
    }

    // Pour le changement de langue...
    internal fun setLocalizedContextTo(iso: String) {
            val config = Configuration(resources.configuration)
            val locale = Locale(iso)
            config.setLocale(locale)
            localizedCtx = createConfigurationContext(config)
        }

    companion object {
        internal var currentView: WeakReference<GLSurfaceView> = WeakReference(null)
    }

    //    override fun attachBaseContext(newBase: Context) {
//        val language = if(nextBoolean()) "en" else "fr"
//        Language.getContextForLanguage(newBase, language)
//        super.attachBaseContext(newBase)
//    }
}



