// Renderer.kt
// Gestion de l'affichage dans la view OpenGL.
// 9 novembre 2022
// Corentin Faucher

@file:Suppress("unused")

package com.coq.coqlib.graph

import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.KeyEvent
import com.coq.coqlib.*
import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.maths.distanceTo
import com.coq.coqlib.maths.scale
import com.coq.coqlib.maths.translate
import com.coq.coqlib.nodes.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Renderer(private val activity: CoqActivity,
               private val customVertShadResID: Int?,
               private val customFragShadResID: Int?,
               setForDrawing: (Node.() -> Surface?)?
)
    : GLSurfaceView.Renderer
{
    // Fonction à utiliser pour dessiner un noeud (customizable)
    // (en fait, ça ne fait que préparer les "for instance uniforms" pour la frame courante).
    private val setForDrawing : (Node.() -> Surface?) = setForDrawing ?: Node::defaultSetNodeForDrawing
    private lateinit var root: AppRootBase

    private var isPaused: Boolean = false
        set(value) {
            RenderingChrono.isPaused = value
            AppChrono.isPaused = value
            if(value != field) {
                if(value)
                    root.willSleep()
                else
                    root.didResume(AppChrono.lastSleepTimeSec)
            }
            field = value
        }

    /*-- Méthodes devant être définies pour Renderer de GLSurfaceView. --*/
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        fun loadShader(type: Int, shaderResource: Int) : Int {
            val inputStream = activity.resources.openRawResource(shaderResource)
            val shaderCode = inputStream.bufferedReader().use { it.readText() }
            return GLES20.glCreateShader(type).also { shader ->
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
//                printdebug(GLES20.glGetShaderInfoLog(shader))
            }
        }
        // 1. Création du program.
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER,
            customVertShadResID ?: R.raw.shadervert
        )
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER,
            customFragShadResID ?: R.raw.shaderfrag
        )
        programID = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
            GLES20.glUseProgram(it)
        }
        // 2. Init divers OpenGL (blending, clear color,...)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        // ID OpenGL de la matrice de projection et le temps pour les shaders.
        pfuProjectionID = GLES20.glGetUniformLocation(programID, "projection")
        pfuTimeID =  GLES20.glGetUniformLocation(programID, "time")
        // 3. Init de Texture
        Texture.init(activity, programID, activity.getExtraTextureTilings())
        // 4. Init des meshes de bases et des vertex attributes.
        Mesh.init(programID)
        // 5. Init des Per Instance Uniforms (ID opengl), e.g. matrice model, couleur d'un objet...
        PerInstanceUniforms.init(programID)
        // 6. Init de la structure (à faire après les autres init)
        root = activity.getAppRoot()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val rect = Rect()
        activity.window.decorView.getWindowVisibleDisplayFrame(rect)
        root.updateFrameSize(width, height, rect.top,0f, 0f, 0f, 0f)
        if (!Texture.loaded) {
            isPaused = false
            printdebug("Resuming renderer")
            Texture.resume()
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        if(RenderingChrono.shouldSleep) {
            isPaused = true
            Thread.sleep(500)
        }
        // 1. Mise à jour de la couleur de fond.
        GLES20.glClearColor(root.smR.pos, root.smG.pos, root.smB.pos, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // 2. Mise à jour des "Per frame uniforms" et du chrono.
        RenderingChrono.update()
        GLES20.glUniform1f(pfuTimeID, RenderingChrono.elapsedAngle)
        GLES20.glUniformMatrix4fv(
            pfuProjectionID, 1, false,
            root.getProjectionMatrix(), 0)
        // 3. Action sur la structure avant l'affichage
        root.willDrawFrame()
        // 4. Boucle d'affichage de la structure.
        val sq = Squirrel(root)
        do {
            sq.pos.setForDrawing()?.draw()
        } while (sq.goToNextToDisplay())
        // 5. Fini remettre les pointeur opengl à zéro...
        Mesh.unbind()
        Texture.unbind()
    }

    internal fun onPause() {
        isPaused = true
        Texture.suspend()
    }

    /*-- Touch et Drag d'objets --*/
    private var downAbsPos = Vector2(0f, 0f)
    private var startMoving = false
    private var wasSlidingMenu = false
    internal fun onDown(posX: Float, posY: Float) {
        isPaused = false
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
    internal fun onMove(newPosX: Float, newPosY: Float) {
        isPaused = false
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
    internal fun onUp() {
        val grabbed = root.grabbedNode ?: return
        root.grabbedNode = null
        (grabbed as? Draggable)?.letGo()
    }
    internal fun onHovering(posX: Float, posY: Float) {
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
    internal fun onScroll(deltaY: Float) {
        val scrollable: Node = root.searchBranchForFirstSelectableUsing { it is Scrollable } ?: return
        (scrollable as? Scrollable)?.scroll(deltaY)
    }
    /*-- Keyboard events --*/
    internal fun onKeyDown(key: KeyboardInput) {
        isPaused = false
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
    internal fun onKeyUp(key: KeyboardInput) {
        val kr = (root.activeScreen as? KeyResponder) ?: return
        if(Scancode.isMod(key.scancode))
            kr.modifiersChangedTo(key.keymod)
        else
            kr.keyUp(key)
    }

    /*-- Private stuff --*/
    /** Dessiner une surface */
    private fun Surface.draw() {
        // 1. Update la mesh (si besoin)
        Mesh.bindTo(mesh)
        // 2. Mise a jour de la texture (si besoin)
        Texture.bindTo(tex)
        // 3. Mise à jour des "PerInstanceUniforms"
        piu.setGLUniforms()
        // 4. Dessiner
        Mesh.drawCurrent()
    }

    /*-- Stuff OpenGL... --*/
    // ID du programme des shaders...
    private var programID: Int = -1
    // ID des "per frame uniforms"
    private var pfuProjectionID: Int = -1
    private var pfuTimeID: Int = -1
}

/** La fonction utilisé par défaut pour CoqRenderer.setNodeForDrawing.
 * Retourne la surface à afficher (le noeud présent si c'est une surface). */
private fun Node.defaultSetNodeForDrawing() : Surface? {
    // 0. Cas racine
    if(containsAFlag(Flag1.isRoot)) {
        (this as RootNode).setModelMatrix()
        return null
    }
    // 1. Copy du parent
    val theParent = parent ?: run {
        printerror("Pas de parent pour noeud non root.")
        return null
    }
    System.arraycopy(theParent.piu.model, 0, piu.model, 0, 16)
    // 2. Cas branche
    if (firstChild != null) {
        piu.model.translate(x.pos, y.pos, z.pos)
        piu.model.scale(scaleX.pos, scaleY.pos, 1f)
        return null
    }
    // 3. Cas feuille
    // Laisser faire si n'est pas affichable...
    if (this !is Surface) {return null}
    // Facteur d'"affichage"
    val alpha = trShow.setAndGet(containsAFlag(Flag1.show))
    // Sortir si rien à afficher...
    if (alpha == 0f) { return null }
    piu.show = alpha
    // Ajuster la matrice model.
    piu.model.translate(x.pos, y.pos, z.pos)
    if (containsAFlag(Flag1.popping)) {
        piu.model.scale(width.pos * alpha, height.pos * alpha, 1f)
    } else {
        piu.model.scale(width.pos, height.pos, 1f)
    }
    // Retourner la surface pour la dessiner.
    return this
}
