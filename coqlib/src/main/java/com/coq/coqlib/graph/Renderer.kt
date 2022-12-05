// Renderer.kt
// Gestion de l'affichage dans la view OpenGL.
// 9 novembre 2022
// Corentin Faucher

@file:Suppress("unused")

package com.coq.coqlib.graph

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
               private val customFragShadResID: Int?)
    : GLSurfaceView.Renderer
{
    // Fonction à utiliser pour dessiner un noeud (customizable)
    // (en fait, ça ne fait que préparer les "for instance uniforms" pour la frame courante).
    var setForDrawing : (Node.() -> Surface?) = Node::defaultSetNodeForDrawing
    private lateinit var root: AppRootBase

    /*-- Méthodes devant être définies pour Renderer de GLSurfaceView. --*/
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        printhere()
        fun loadShader(type: Int, shaderResource: Int) : Int {
            val inputStream = activity.resources.openRawResource(shaderResource)
            val shaderCode = inputStream.bufferedReader().use { it.readText() }
            return GLES20.glCreateShader(type).also { shader ->
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
                printdebug(GLES20.glGetShaderInfoLog(shader))
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
        root.updateFrameSize(width, height, 0f, 0f, 0f, 0f)
        if (!Texture.loaded) {
            Texture.resume()
        }
    }

    override fun onDrawFrame(gl: GL10?) {
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
        Texture.suspend()
    }

    /*-- Touch et Drag d'objets --*/
    private var downPos = Vector2(0f, 0f)
    private var startMoving = false
    internal fun onDown(posX: Float, posY: Float) {
        // 1. Position normalisée par rapport à la root, e.g. pos dans [-1.7, 1.7] x [-1, 1].
        downPos = root.getPositionFrom(posX, posY)
        startMoving = false
        // 2. Essayer de trouver un noeud à cette position.
        val touched = root.searchBranchForSelectable(downPos, null) ?: return
        when(touched) {
            // Prendre si draggable
            is Draggable -> {
                root.grabbedNode = touched
                val relPos = downPos.inReferentialOf(touched)
                touched.grab(relPos)
            }
            // Sinon activer le noeud sélectionné (non Draggable)
            is Button -> touched.action()
            is Enterable -> touched.enterAction()
        }
    }
    internal fun onMove(newPosX: Float, newPosY: Float) {
        val newPos = root.getPositionFrom(newPosX, newPosY)
        val dist = newPos.distanceTo(downPos)
        // Il faut un minimum de déplacement pour "activer" un déplacement.
        if( !startMoving && dist < 0.03 ) return
        // Ok, on bouge... s'il y a un noeud de grabbé.
        startMoving = true
        val grabbed = root.grabbedNode ?: return
        val relPos = newPos.inReferentialOf(grabbed)
        (grabbed as? Draggable)?.drag(relPos)
    }
    internal fun onUp() {
        val grabbed = root.grabbedNode ?: return
        root.grabbedNode = null
        (grabbed as? Draggable)?.letGo()
    }
    internal fun onHovering(posX: Float, posY: Float) {
        val pos = root.getPositionFrom(posX, posY)
        val hovered = root.searchBranchForSelectable(pos, null)
        if(hovered !== root.selectedNode) {
            (root.selectedNode as? Hoverable)?.stopHovering()
            root.selectedNode = hovered
        }
        // (Oui, on recall startHovering à chaque cursorMove...
        // -> Il faut arrêter de bouger pour qu'il se passe quelque chose.)
        (hovered as? Hoverable)?.startHovering()
    }
    internal fun onScroll(deltaY: Float) {
        printdebug("Scroll $deltaY")
//        val scrollable = root.searchBranchForFirstSelectableTyped<Scrollable>()
        val scrollable: Node = root.searchBranchForFirstSelectableUsing { it is Scrollable } ?: return
        (scrollable as? Scrollable)?.scroll(deltaY)
    }
    /*-- Keyboard events --*/
    internal fun onKeyDown(key: KeyboardKey) {
        when(key.keycode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER ->
                (root.activeScreen as? Enterable)?.let { it.enterAction(); return }
            KeyEvent.KEYCODE_ESCAPE ->
                (root.activeScreen as? Escapable)?.let { it.escapeAction(); return }
        }
        (root.activeScreen as? KeyResponder)?.keyDown(key)
    }
    internal fun onKeyUp(key: KeyboardKey) {
        (root.activeScreen as? KeyResponder)?.keyUp(key)
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
    // 0.1 Copy du parent
    val theParent = parent ?: run {
        printerror("Pas de parent pour noeud non root.")
        return null
    }
    System.arraycopy(theParent.piu.model, 0, piu.model, 0, 16)
    // 1. Cas branche
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
    piu.color[3] = alpha
    // Rien à afficher...
    if (alpha == 0f) { return null }
    piu.model.translate(x.pos, y.pos, z.pos)
    if (containsAFlag(Flag1.popping)) {
        piu.model.scale(width.pos * alpha, height.pos * alpha, 1f)
    } else {
        piu.model.scale(width.pos, height.pos, 1f)
    }
    return this
}
