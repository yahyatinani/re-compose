package com.github.whyrising.recompose.schemas

@Suppress("EnumEntryName")
enum class InterceptorSchema {
  id,
  before,
  after;

  override fun toString(): String = ":${super.toString()}"
}
