// Meshes.kt
// Classe pour le maillage d'un objet OpenGL.
// (Vertices, etc.)
//
// 9 novembre 2022
// Corentin Faucher

@file:Suppress("unused", "MemberVisibilityCanBePrivate", "ConvertSecondaryConstructorToPrimary",
    "LocalVariableName"
)
package com.coq.coqlib.graph

import android.opengl.GLES20
import com.coq.coqlib.printerror
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.*

open class Mesh(val vertices: FloatArray,  // Seul vertices reste visible.
                protected val indices: IntArray?,
                private val primitiveType: Int,
                private val posCount: Int = 3, private val normCount: Int = 3, private val uvCount: Int = 2,
) : Cloneable {
    // Vertices Buffer OpenGL ID.
    private val vboId: Int
    private var indicesVboId: Int = -1
    // Positions / dimensions dans le float array...
    private val normalOffset = posCount * 4  // (En bytes)
    private val uvOffset = (posCount + normCount) * 4
    private val floatsPerVertex = posCount + normCount + uvCount
    private val vertexSize = floatsPerVertex * 4  // Taille d'un vertex en bytes.
    private val vertexCount: Int = vertices.size / floatsPerVertex

    init {
        // 1. Créer un vbo OpenGL des vertices.
        val vboIDarray = IntArray(1)
        GLES20.glGenBuffers(1, vboIDarray, 0)
        vboId = vboIDarray[0]
        // Se lier au nouveau buffer.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        // Et y mettre les données des vertex (converties en buffer).
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertices.size * 4,
            vertices.toBuffer(),
            GLES20.GL_STATIC_DRAW)
        // 2. Création d'un buffer pour les indices si besoin (semblable...)
        indices?.let { indices ->
            val vboIndIDarray = IntArray(1)
            GLES20.glGenBuffers(1, vboIndIDarray, 0)
            indicesVboId = vboIndIDarray[0]
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesVboId)
            GLES20.glBufferData(
                GLES20.GL_ELEMENT_ARRAY_BUFFER,
                indices.size * 4, indices.toBuffer(), GLES20.GL_STATIC_DRAW)
        }
        // Fini l'init de la mesh. Désactiver le lien... "unbind".
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        currentMesh = null
    }
    // Constructeur de copie
    private constructor(other: Mesh) : this(other.vertices.clone(), other.indices?.clone(),
        other.primitiveType, other.posCount, other.normCount, other.uvCount)
    public override fun clone() = Mesh(this)

    // Update des buffers (après une modification de vertices)
    fun updateVerticesBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertices.size * 4, vertices.toBuffer(), GLES20.GL_STATIC_DRAW)
    }

    // Changer la mesh présentement utilisée.
    private fun bind() {
        // Se lier à son vertex buffer.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        // Setter les vertex attributes... (position/décalages en bytes de pos, norm, uv)
        GLES20.glVertexAttribPointer(
            attrPositionID, posCount, GLES20.GL_FLOAT,
            false, vertexSize, 0
        )
        if(attrNormalID >= 0) {
            GLES20.glVertexAttribPointer(
                attrNormalID, normCount, GLES20.GL_FLOAT,
                false, vertexSize, normalOffset
            )
        }
        if(attrUVID >= 0) {
            GLES20.glVertexAttribPointer(attrUVID, uvCount, GLES20.GL_FLOAT,
                false, vertexSize, uvOffset
            )
        }
        if (indices == null) return
        // Lier la liste d'indices.
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesVboId)
    }
    // Dessiner la mesh
    private fun draw() {
        indices?.let {
            GLES20.glDrawElements(
                primitiveType,
                it.size, GLES20.GL_UNSIGNED_INT, 0)
        } ?: run {
            GLES20.glDrawArrays(primitiveType, 0, vertexCount)
        }
    }

    // Setters
    fun setXofVertex(x: Float, vertexID: Int) {
        vertices[vertexID * floatsPerVertex] = x
    }
    fun setYofVertex(y: Float, vertexID: Int) {
        vertices[vertexID * floatsPerVertex+1] = y
    }
    fun setZofVertex(z: Float, vertexID: Int) {
        vertices[vertexID * floatsPerVertex+2] = z
    }

    /*-- Statics: meshes de bases --*/
    companion object {
        lateinit var sprite: Mesh
            private set
        lateinit var triangle: Mesh
            private set

        // Init des meshes de bases, à caller au début.
        internal fun init(programID: Int) {
            // Vertex Attributes
            attrPositionID = GLES20.glGetAttribLocation(programID, "position").also {
                GLES20.glEnableVertexAttribArray(it)
            }
            attrNormalID = GLES20.glGetAttribLocation(programID, "normal").also {
                if (it >= 0) {
                    GLES20.glEnableVertexAttribArray(it)
                }
            }
            attrUVID = GLES20.glGetAttribLocation(programID, "uv").also {
                if (it >= 0) {
                    GLES20.glEnableVertexAttribArray(it)
                }
            }
            // Les meshes par défaut
            sprite = Mesh(floatArrayOf(
                -0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
                -0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
                0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f),
                null, GLES20.GL_TRIANGLE_STRIP)
            triangle = Mesh(
                floatArrayOf(
                    0.0f, 0.5f, 0.0f,      // top
                    -0.433f, -0.25f, 0.0f,    // bottom left
                    0.433f, -0.25f, 0.0f      // bottom right
                ),
                null, GLES20.GL_TRIANGLES,
                3, 0, 0
            )
        }

        internal fun bindTo(newMesh: Mesh) {
            if (newMesh === currentMesh) {
                return
            }
            newMesh.bind()
            currentMesh = newMesh
        }
        internal fun drawCurrent() {
            currentMesh?.draw()
        }
        internal fun unbind() {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            currentMesh = null
        }
        private var currentMesh: Mesh? = null

        private var attrPositionID: Int = -1
        private var attrNormalID: Int = -1
        private var attrUVID: Int = -1
    }
}

/** Fan, pour un disque de progression. */
class FanMesh : Mesh(
    FloatArray(10 * 8) { 0f }.also {
        // Centre de la fan.
        it[5] = 1f; it[6] = 0.5f; it[7] = 0.5f
        // Tous des triangles qui passe par le centre...
        for (i in 1..9) {
            it[i * 8] = -0.5f * sin(2f * PI.toFloat() * (i - 1).toFloat() / 8f)
            it[i * 8 + 1] = 0.5f * cos(2f * PI.toFloat() * (i - 1).toFloat() / 8f)
            it[i * 8 + 5] = 1f  // normal = (0, 0, 1)
            it[i * 8 + 6] = 0.5f - 0.5f * sin(2f * PI.toFloat() * (i - 1).toFloat() / 8f)
            it[i * 8 + 7] = 0.5f - 0.5f * cos(2f * PI.toFloat() * (i - 1).toFloat() / 8f)
        }
    }, intArrayOf( // Tous des triangles qui passe par le centre...
        0, 1, 2,
        0, 2, 3,
        0, 3, 4,
        0, 4, 5,
        0, 5, 6,
        0, 6, 7,
        0, 7, 8,
        0, 8, 9
    ), GLES20.GL_TRIANGLES
) {
    fun updateWithRatio(ratio: Float) {
        if (vertices.size < 80) {
            printerror("Bad size."); return}
        for(i in 1..9) {
            vertices[i*8]   = -0.5f * sin(ratio*2f* PI.toFloat() * (i-1).toFloat()/8f)
            vertices[i*8+1] = 0.5f * cos(ratio*2f* PI.toFloat() * (i-1).toFloat()/8f)
            vertices[i*8+6] = 0.5f - 0.5f * sin(ratio*2f* PI.toFloat() * (i-1).toFloat()/8f)
            vertices[i*8+7] = 0.5f - 0.5f * cos(ratio*2f* PI.toFloat() * (i-1).toFloat()/8f)
        }
        updateVerticesBuffer()
    }
}

/** Sprite particulière pour les "bar", i.e. sprite qui s'étire en largeur,
 * mais où juste le milieu s'étire (voir bar_gray.png et Surfaces.kt -> Bar). */
class BarMesh : Mesh(
    floatArrayOf(
            -0.5000f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 0.0f,
            -0.5000f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 1.0f,
            -0.1667f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 0.0f,
            -0.1667f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 1.0f,
            0.1667f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 0.0f,
            0.1667f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 1.0f,
            0.5000f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 0.0f,
            0.5000f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 1.0f),
    null, GLES20.GL_TRIANGLE_STRIP
) {
    fun updateWithCenterRatio(ratio: Float) {
        if(ratio < 0 || ratio > 1)
            printerror("Bad ratio : $ratio.")
        val x = 0.5f * max(min(ratio, 1f), 0f)
        vertices[16] = -x
        vertices[24] = -x
        vertices[32] = x
        vertices[40] = x
        updateVerticesBuffer()
    }
}

/** Sprite particulière pour les "frame", i.e. sprite qui s'étire en largeur et hauteur,
 * mais où juste le milieu s'étire (voir frame_mocha.png et Surfaces.kt -> Frame). */
class FrameMesh : Mesh(
    floatArrayOf(
            -0.5000f,  0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 0.000f, // 0
            -0.5000f,  0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 0.333f, // 1
            -0.5000f, -0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 0.667f, // 2
            -0.5000f, -0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 1.000f, // 3
            -0.1667f,  0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 0.000f, // 4
            -0.1667f,  0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 0.333f, // 5
            -0.1667f, -0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 0.667f, // 6
            -0.1667f, -0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 1.000f, // 7
            0.1667f,  0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 0.000f,  // 8
            0.1667f,  0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 0.333f,  // 9
            0.1667f, -0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 0.667f,  // 10
            0.1667f, -0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 1.000f,  // 11
            0.5000f,  0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 0.000f,  // 12
            0.5000f,  0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 0.333f,  // 13
            0.5000f, -0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 0.667f,  // 14
            0.5000f, -0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 1.000f), // 15
    intArrayOf(
                0, 1, 4,  1, 5, 4,
                1, 2, 5,  2, 6, 5,
                2, 3, 6,  3, 7, 6,
                4, 5, 8,  5, 9, 8,
                5, 6, 9,  6, 10, 9,
                6, 7, 10, 7, 11, 10,
                8, 9, 12, 9, 13, 12,
                9, 10, 13, 10, 14, 13,
                10, 11, 14, 11, 15, 14
    ), GLES20.GL_TRIANGLES
) {
    fun updateWithCenterRatios(x_ratio: Float, y_ratio: Float) {
        val x = 0.5f * max(min(x_ratio, 1f), 0f)
        val y = 0.5f * max(min(y_ratio, 1f), 0f)
        vertices[32] = -x; vertices[40] = -x
        vertices[48] = -x; vertices[56] = -x
        vertices[64] = x;  vertices[72] = x
        vertices[80] = x;  vertices[88] = x
        vertices[9]  = y;  vertices[41] = y
        vertices[73] = y;  vertices[105] = y
        vertices[17] = -y; vertices[49] = -y
        vertices[81] = -y; vertices[113] = -y
        updateVerticesBuffer()
    }
}

/** Mesh d'un graphe 3D : z = f(x, y). */
class GraphMesh(val m: Int, val n: Int) : Mesh(
    FloatArray((m+1)*(n+1) * 8) { 0f }.also { v ->
        for(i in 0..m) {
            for(j in 0..n) {
                val index = (i*(n+1) + j) * 8
                v[index] = i.toFloat() / m.toFloat() - 0.5f
                v[index + 1] = j.toFloat() / n.toFloat() - 0.5f
                v[index + 5] = 1f
                v[index + 6] = i.toFloat() / m.toFloat()
                v[index + 7] = j.toFloat() / n.toFloat()
            }
        }
    }, IntArray(m*n*6).also { ind ->
        for(i in 0 until m) {
            for(j in 0 until n) {
                // 2 triangles par quad.
                val vind1 = i*(n+1) + j
                val vind2 = (i+1)*(n+1) + j
                val dec = i*n*6 + j*6
                ind[dec + 0]     = vind1
                ind[dec + 1] = vind1 + 1
                ind[dec + 2] = vind2
                ind[dec + 3] = vind1 + 1
                ind[dec + 4] = vind2 + 1
                ind[dec + 5] = vind2
            }
        }
    }, GLES20.GL_TRIANGLES
) {
    fun updateZwith(f: (Float, Float) -> Float) {
        for(i in 0..m) {
            for (j in 0..n) {
                vertices[(i*(n+1) + j) * 8 + 2] =
                    f(i.toFloat() / m.toFloat(), j.toFloat() / n.toFloat())
            }
        }
    }
}

/** Affichage d'une fonction y = f(x)
 * A priori, les xs/ys devraient êtr préformaté
 * pour être contenu dans [-0.5, 0.5] x [-0.5, 0.5]...
 * delta: épaisseur des lignes. */
class PlotMesh : Mesh {
    // Workaround kotlin pour préparer les données avant le call à super...
    private constructor(vertices: FloatArray,
                        indices: IntArray
    ) : super(vertices, indices, GLES20.GL_TRIANGLES)

    companion object {
        operator fun invoke(
            xs: FloatArray, ys: FloatArray, delta: Float = 0.02f, ratio: Float = 1f
        ): PlotMesh {
            val n_lines = xs.size - 1
            val n_points = xs.size
            val vertices = FloatArray((n_lines + n_points) * 4 * 8) { 0f }
            val indices = IntArray((n_lines + n_points) * 6)
            // Init des lignes entre les points
            for (i in 0 until n_lines) {
                // Angle de la ligne, tel que tan(theta) = delta y / delta x :
                val theta = atan((ys[i + 1] - ys[i]) / (xs[i + 1] - xs[i]))
                val deltax = delta * sin(theta) / ratio
                val deltay = delta * cos(theta)
                val vert_dec = i * 4
                // 4 coins du rectangle (la ligne entre deux points)
                var index = vert_dec * 8
                vertices[index + 0] = xs[i] - deltax
                vertices[index + 1] = ys[i] + deltay
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 0f  // (uv)
                vertices[index + 7] = 0f
                index += 8
                vertices[index + 0] = xs[i] + deltax
                vertices[index + 1] = ys[i] - deltay
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 0f  // (uv)
                vertices[index + 7] = 1f
                index += 8
                vertices[index + 0] = xs[i + 1] - deltax
                vertices[index + 1] = ys[i + 1] + deltay
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 0.75f  // (uv)
                vertices[index + 7] = 0f
                index += 8
                vertices[index + 0] = xs[i + 1] + deltax
                vertices[index + 1] = ys[i + 1] - deltay
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 0.75f  // (uv)
                vertices[index + 7] = 1f
                // 2 triangles...
                index = i * 6
                indices[index + 0] = vert_dec
                indices[index + 1] = vert_dec + 1
                indices[index + 2] = vert_dec + 2
                indices[index + 3] = vert_dec + 1
                indices[index + 4] = vert_dec + 2
                indices[index + 5] = vert_dec + 3

            }
            // Init des points du graph
            val pts_deltax = delta * 1.15f / ratio
            val pts_deltay = delta * 1.15f
            for (i in 0 until n_points) {
                val vert_dec = n_lines * 4 + i * 4
                // 4 coins du point (4 vertex)
                var index = vert_dec * 8  // (8 floats par vertex)
                vertices[index + 0] = xs[i] - pts_deltax
                vertices[index + 1] = ys[i] + pts_deltay
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 1f  // (uv)
                vertices[index + 7] = 0f
                index += 8
                vertices[index + 0] = xs[i] - pts_deltax
                vertices[index + 1] = ys[i] - pts_deltay
                vertices[index + 5] = 1f
                vertices[index + 6] = 1f
                vertices[index + 7] = 1f
                index += 8
                vertices[index + 0] = xs[i] + pts_deltax
                vertices[index + 1] = ys[i] + pts_deltay
                vertices[index + 5] = 1f
                vertices[index + 6] = 2f
                vertices[index + 7] = 0f
                index += 8
                vertices[index + 0] = xs[i] + pts_deltax
                vertices[index + 1] = ys[i] - pts_deltay
                vertices[index + 5] = 1f
                vertices[index + 6] = 2f
                vertices[index + 7] = 1f
                // 2 triangles...
                index = n_lines * 6 + i * 6
                indices[index + 0] = vert_dec + 0
                indices[index + 1] = vert_dec + 1
                indices[index + 2] = vert_dec + 2
                indices[index + 3] = vert_dec + 1
                indices[index + 4] = vert_dec + 2
                indices[index + 5] = vert_dec + 3
            }
            return PlotMesh(vertices, indices)
        }
    }
}

class GridMesh : Mesh {
    // Ici aussi, tour de passe-passe pour préparer les données avant de caller le super constructor...
    // Mettre le constructor private et créer un "operator fun invoke" dans le companion object.
    private constructor(vertices: FloatArray,
                        indices: IntArray
    ) : super(vertices, indices, GLES20.GL_TRIANGLES)

    companion object {
        operator fun invoke(xmin: Float, xmax: Float, xR: Float, deltax: Float,
                            ymin: Float, ymax: Float, yR: Float, deltay: Float,
                            lineWidthRatio: Float = 0.1f
        ) : GridMesh
        {
            val x0 = xR - floor((xR - xmin) / deltax) * deltax
            val m = ((xmax - x0) / deltax).toInt() + 1
            val xlinedelta = deltax * lineWidthRatio * 0.5f
            val y0 = yR - floor((yR - ymin) / deltay) * deltay
            val n = ((ymax - y0) / deltay).toInt() + 1
            val ylinedelta = deltay * lineWidthRatio * 0.5f
            val vertices = FloatArray(4*(m+n)*8) { 0f }
            val indices = IntArray(6*(m+n))
            // Les lignes verticales (espacées en x)
            for(i in 0 until m) {
                val vert_dec = i*4
                val x = x0 + i.toFloat() * deltax
                var index = vert_dec*8
                vertices[index + 0] = x - xlinedelta
                vertices[index + 1] = ymax
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 0f  // (uv)
                vertices[index + 7] = 0f
                index += 8
                vertices[index + 0] = x - xlinedelta
                vertices[index + 1] = ymin
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 0f  // (uv)
                vertices[index + 7] = 1f
                index += 8
                vertices[index + 0] = x + xlinedelta
                vertices[index + 1] = ymax
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 1f  // (uv)
                vertices[index + 7] = 0f
                index += 8
                vertices[index + 0] = x + xlinedelta
                vertices[index + 1] = ymin
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 1f  // (uv)
                vertices[index + 7] = 1f
                // 2 triangles...
                index = i*6
                indices[index + 0] = vert_dec
                indices[index + 1] = vert_dec + 1
                indices[index + 2] = vert_dec + 2
                indices[index + 3] = vert_dec + 1
                indices[index + 4] = vert_dec + 2
                indices[index + 5] = vert_dec + 3
            }
            // Les lignes horizontales (espacées en y)
            for(i in 0 until n) {
                val vert_dec = 4 * m + 4 * i
                val y = y0 + i.toFloat() * deltay
                var index = vert_dec*8
                vertices[index + 0] = xmin
                vertices[index + 1] = y + ylinedelta
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 0f  // (uv)
                vertices[index + 7] = 0f
                index += 8
                vertices[index + 0] = xmin
                vertices[index + 1] = y - ylinedelta
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 0f  // (uv)
                vertices[index + 7] = 1f
                index += 8
                vertices[index + 0] = xmax
                vertices[index + 1] = y + ylinedelta
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 1f  // (uv)
                vertices[index + 7] = 0f
                index += 8
                vertices[index + 0] = xmax
                vertices[index + 1] = y - ylinedelta
                vertices[index + 5] = 1f  // (normal)
                vertices[index + 6] = 1f  // (uv)
                vertices[index + 7] = 1f
                // 2 triangles...
                index = m*6 + i*6
                indices[index + 0] = vert_dec
                indices[index + 1] = vert_dec + 1
                indices[index + 2] = vert_dec + 2
                indices[index + 3] = vert_dec + 1
                indices[index + 4] = vert_dec + 2
                indices[index + 5] = vert_dec + 3
            }
            return GridMesh(vertices, indices)
        }
    }
}

private fun IntArray.toBuffer() : IntBuffer {
    return ByteBuffer.allocateDirect(this.size * 4).run {
        order(ByteOrder.nativeOrder())
        asIntBuffer().apply{
            put(this@toBuffer)
            position(0)
        }
    }
}

private fun FloatArray.toBuffer() : FloatBuffer {
    return ByteBuffer.allocateDirect(this.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(this@toBuffer)
            position(0)
        }
    }
}