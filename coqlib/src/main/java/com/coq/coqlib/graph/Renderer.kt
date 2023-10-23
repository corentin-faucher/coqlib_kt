// Renderer.kt
// Gestion de l'affichage dans la view OpenGL.
// 9 novembre 2022
// Corentin Faucher

@file:Suppress("unused")

package com.coq.coqlib.graph

import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import com.coq.coqlib.*
import com.coq.coqlib.maths.scale
import com.coq.coqlib.maths.translate
import com.coq.coqlib.nodes.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class Renderer(private val activity: CoqActivity,
               private val customVertShadResID: Int?,
               private val customFragShadResID: Int?,
               setForDrawing: (Node.() -> Surface?)?
) : GLSurfaceView.Renderer
{
    // ID du programme des shaders...
    private var programID: Int = -1
    // Préparation d'un noeud avant l'affichage.
    // Si c'est une surface à dessiner, retourne le noeud en tant que surface.
    private val setForDrawing : (Node.() -> Surface?) = setForDrawing ?: Node::defaultSetNodeForDrawing

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

        // Init de Texture
        Texture.init(activity, programID, activity.getExtraTextureTilings())
        // Init des meshes de bases et des vertex attributes.
        Mesh.init(programID)
        // Init des Per Instance Uniforms (ID opengl), e.g. matrice model, couleur d'un objet...
        PerInstanceUniforms.init(programID)
        // 8. Init de la structure (à faire en dernier)
        activity.setAppRoot()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val rect = Rect()
        activity.window.decorView.getWindowVisibleDisplayFrame(rect)
        if (!Texture.loaded) {
            activity.isPaused = false
            printdebug("Resuming renderer")
            Texture.resume()
        }
        val root = activity.root ?: run {
            printerror("Changing GL surface without root?")
            return
        }
        root.updateFrameSize(width, height,
            if(activity.isWindows) 0 else rect.top, // Taille de la status bar en haut... un peu bogue sur tablettes android? TODO next : verifier dans chromebook...)
            0f, 0f, 0f, 0f)

    }

    override fun onDrawFrame(gl: GL10?) {
        if(RenderingChrono.shouldSleep) {
            activity.isPaused = true
            Thread.sleep(500)
        }
        // 1. Mise à jour de la couleur de fond.
        val root = activity.root ?: run {
            GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            return
        }
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
