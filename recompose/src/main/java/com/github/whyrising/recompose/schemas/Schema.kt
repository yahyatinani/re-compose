package com.github.whyrising.recompose.schemas

@Suppress("EnumEntryName")
enum class Schema {
    dofx,
    db,
    notFound;

    override fun toString(): String = ":${super.toString()}"
}
