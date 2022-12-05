package com.coq.coqlib

import com.coq.coqlib.R.drawable.switch_back
import com.coq.coqlib.graph.Texture
import com.coq.coqlib.nodes.Surface
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 3)
    }

    @Test
    fun node_cloning() {
        val surf = Surface(null, Texture.getPng(switch_back), 0f, 0f, 1f)
        val surf2 = surf.clone()
        assertNotSame(surf, surf2)
        assertSame(surf.tex, surf2.tex)
        assertNotSame(surf.x, surf2.x)
    }
}