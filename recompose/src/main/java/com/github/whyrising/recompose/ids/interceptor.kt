package com.github.whyrising.recompose.ids

@Suppress("EnumEntryName")
enum class interceptor {
  id,
  before,
  after;

  override fun toString(): String = ":${super.toString()}"
}
