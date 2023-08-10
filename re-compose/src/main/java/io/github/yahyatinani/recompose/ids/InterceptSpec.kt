package io.github.yahyatinani.recompose.ids

@Suppress("EnumEntryName")
enum class InterceptSpec {
  id,
  before,
  after;

  override fun toString(): String = ":${super.toString()}"
}
