package com.github.yahyatinani.recompose.ids

@Suppress("EnumEntryName")
enum class InterceptSpec {
  id,
  before,
  after,
  after_async;

  override fun toString(): String = ":${super.toString()}"
}
