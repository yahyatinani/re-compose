package com.github.whyrising.recompose.ids

@Suppress("EnumEntryName")
enum class InterceptSpec {
  id,
  before,
  after,
  after_async;

  override fun toString(): String = ":${super.toString()}"
}
