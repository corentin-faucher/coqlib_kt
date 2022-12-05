package com.coq.coqlib

interface Flagable {
    var flags: Long

    fun removeFlags(toRemove: Long) {
        flags = flags and toRemove.inv()
    }

    fun addFlags(toAdd: Long) {
        flags = flags or toAdd
    }

    fun addRemoveFlags(toAdd: Long, toRemove: Long) {
        flags = (flags or toAdd) and toRemove.inv()
    }

    fun containsAFlag(flagsRef: Long) = (flags and flagsRef != 0L)

    fun setFlag(toSet: Long, isOn: Boolean) {
        if (isOn) {
            flags = flags or toSet
        } else {
            flags = flags and toSet.inv()
        }
    }
}
