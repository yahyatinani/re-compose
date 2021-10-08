package com.github.whyrising.recompose

@Suppress("EnumEntryName")
enum class RKeys {
    effects,
    coeffects,
    db,
    event,
    originalEvent,
    queue,
    stack,
    fx,
    dispatch,
    dispatchN,
    dofx,
    id,
    before,
    after;

    override fun toString(): String {
        return ":${super.toString()}"
    }
}