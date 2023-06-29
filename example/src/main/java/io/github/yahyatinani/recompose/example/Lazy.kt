package io.github.yahyatinani.recompose.example

class Lazy<out A>(f: () -> A) : () -> A {
  private val value: A by lazy { f() }

  override operator fun invoke(): A = value
}
