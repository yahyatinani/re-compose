package com.github.whyrising.recompose.ids

@Suppress("EnumEntryName")
enum class recompose {
  dofx,
  db,
  notFound;

  override fun toString(): String = ":${super.toString()}"
}
