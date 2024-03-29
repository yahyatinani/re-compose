package io.github.yahyatinani.recompose.httpfx

import io.github.yahyatinani.recompose.dispatch
import io.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.recompose.fx.regFx
import io.github.yahyatinani.y.core.collections.IPersistentMap
import io.github.yahyatinani.y.core.get
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.UnknownHostException

var httpFxClient: HttpClient = HttpClient(Android) {
  install(Logging) {
    logger = Logger.DEFAULT
    level = LogLevel.ALL
  }
  install(HttpTimeout)
  install(ContentNegotiation) {
    json(
      Json {
        isLenient = true
        ignoreUnknownKeys = true
      }
    )
  }
}

// -- Effect -------------------------------------------------------------------

@Suppress("EnumEntryName", "ClassName")
enum class ktor {
  url,
  timeout,
  on_success,
  on_failure,
  method,
  response_type_info,
  http_fx,
  coroutine_scope;

  override fun toString(): String = ":$name"
}

fun httpErrorByException(
  e: Exception,
  url: String,
  method: HttpMethod
) = when (e) {
  is UnknownHostException -> {
    HttpError(
      uri = url,
      method = method.value,
      status = 0,
      error = e.cause?.message,
      debugMessage = e.message
    )
  }

  is HttpRequestTimeoutException -> HttpError(
    uri = url,
    method = method.value,
    error = e.message,
    status = -1,
    debugMessage = "Request timed out"
  )

  is NoTransformationFoundException -> TODO("504 Gateway Time-out: $e")

  else -> throw e
}

fun httpEffect(request: Any?) {
  get<CoroutineScope>(request, ktor.coroutine_scope)!!.launch {
    val onFailure = get<Event>(request, ktor.on_failure)!!
    // TODO: 1. validate(request).
    request as IPersistentMap<Any, Any>

    val url = get<String>(request, ktor.url)!!
    val timeout = request[ktor.timeout]
    val method = request[ktor.method] as HttpMethod // TODO:
    val httpResponse = try {
      httpFxClient.get {
        url(url)
        timeout { requestTimeoutMillis = (timeout as Number?)?.toLong() }
      }
    } catch (e: Exception) {
      dispatch(onFailure.conj(httpErrorByException(e, url, method)))

      return@launch
    }

    val status = httpResponse.status
    if (status == HttpStatusCode.OK) {
      val responseTypeInfo = get<TypeInfo>(request, ktor.response_type_info)!!
      val onSuccess = get<Event>(request, ktor.on_success)!!
      dispatch(onSuccess.conj(httpResponse.call.body(responseTypeInfo)))
    } else {
      val httpRequest = httpResponse.request
      dispatch(
        onFailure.conj(
          HttpError(
            uri = httpRequest.url.toString(),
            method = httpRequest.method.value,
            error = status.description,
            status = status.value,
            debugMessage = "Http response at 400 or 500 level"
          )
        )
      )
    }
  }
}

fun regHttpKtor() {
  regFx(ktor.http_fx, ::httpEffect)
}
