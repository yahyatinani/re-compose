package com.github.whyrising.recompose.schemas

@Suppress("EnumEntryName")
enum class Schema {
    event,
    originalEvent,
    db,
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
