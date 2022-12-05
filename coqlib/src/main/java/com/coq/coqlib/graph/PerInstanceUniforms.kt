// PerInstanceUniforms.kt
// Les propriétés d'affichage OpenGL pour un objet quelconque.
// 9 novembre 2022
// Corentin Faucher

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.coqlib.graph

import android.opengl.GLES20

class PerInstanceUniforms {
    var model: FloatArray  // Matrice "model" de l'objet.
    var color: FloatArray  // Couleur/teinte de l'objet.
    var i: Float    // Tile
    var j: Float
    var emph: Float // Facteur d'emphase. [0, 1]
    var flags: Int  // Autres flags d'affichage, e.g. "one sided".

    constructor() {
        model = FloatArray(16)
        i = 0.0f
        j = 0.0f
        color = floatArrayOf(1f, 1f, 1f, 1f) // alpha pour l'apparition de l'objet (0,caché) -> (1,montré)
        emph = 0.0f
        flags = 0
    }

    constructor(piu: PerInstanceUniforms) {
        model = piu.model.copyOf()
        i = piu.i
        j = piu.j
        color = piu.color.copyOf()
        emph = piu.emph
        flags = piu.flags
    }

    internal fun setGLUniforms() {
        GLES20.glUniformMatrix4fv(
            piuModelID, 1, false,
            model, 0)
        GLES20.glUniform2f(piuTexIJId, i, j)
        GLES20.glUniform4fv(piuColorID, 1, color, 0)
        GLES20.glUniform1f(piuEmphID, emph)
        GLES20.glUniform1i(piuFlagsID, flags)
    }

    companion object {
        /** Exemple de flag pour les shaders (ajouter à piu.flags). */
        const val isOneSided: Int = 1

        internal fun init(programID: Int) {
            piuModelID = GLES20.glGetUniformLocation(programID, "model")
            piuTexIJId = GLES20.glGetUniformLocation(programID, "texIJ")
            piuColorID = GLES20.glGetUniformLocation(programID, "color")
            piuEmphID  = GLES20.glGetUniformLocation(programID, "emph")
            piuFlagsID = GLES20.glGetUniformLocation(programID, "flags")
        }
        // ID OpenGL des "per instance uniforms"
        private var piuModelID: Int = -1
        private var piuTexIJId: Int = -1
        private var piuColorID: Int = -1
        private var piuEmphID: Int = -1
        private var piuFlagsID: Int = -1
    }
}
