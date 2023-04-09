@file:Suppress("unused")

package com.coq.coqlib.graph

import com.coq.coqlib.R
import com.coq.coqlib.nodes.Node
import com.coq.coqlib.nodes.TiledSurface

/** Les couleurs des disques disks.png. */
enum class DiskColor {
    Yellow,
    Green,
    Red,
    Blue,

    Orange,
    Purple,
    BlueSky,
    Pink,

    Black,
    White,
    Gray,
    BlackWhite,

    Beige;
}

fun Node.addColorDisk(x: Float, y: Float, height: Float, diskColor: DiskColor,
                      lambda: Float = 0f, flags: Long = 0L, emph: Float = 0.25f
) = TiledSurface(this, R.drawable.disks, x, y, height, lambda,
        diskColor.ordinal, flags).also { piu.emph = emph }

fun TiledSurface.setToDiskColor(diskColor: DiskColor) {
    this.updateTile(diskColor.ordinal, 0)

}
