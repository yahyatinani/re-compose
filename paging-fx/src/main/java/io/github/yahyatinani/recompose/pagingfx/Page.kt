package io.github.yahyatinani.recompose.pagingfx

interface Page {
  val data: List<Any>
  val prevKey: Any?
  val nextKey: Any?
}
