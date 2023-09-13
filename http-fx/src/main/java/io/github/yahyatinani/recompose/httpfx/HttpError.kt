package io.github.yahyatinani.recompose.httpfx

data class HttpError(
  val uri: String,
  val method: String,
  val status: Int,
  val error: String?,
  val debugMessage: String?
) : RuntimeException()
