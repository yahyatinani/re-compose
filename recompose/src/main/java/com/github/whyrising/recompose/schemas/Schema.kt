package com.github.whyrising.recompose.schemas

@Suppress("EnumEntryName")
enum class Schema {
    event,
    originalEvent,
    dofx,
    db,
    notFound;

    override fun toString(): String = ":${super.toString()}"
}
