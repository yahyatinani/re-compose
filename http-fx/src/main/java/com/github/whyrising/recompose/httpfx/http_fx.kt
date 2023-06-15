package com.github.whyrising.recompose.httpfx

import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.fx.regFx
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.m
import io.ktor.client.HttpClient
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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.net.UnknownHostException

var client: HttpClient = HttpClient(Android) {
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
//        serializersModule = searchModule
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

  override fun toString(): String = name
}

fun httpEffect(request: Any?) {
  get<CoroutineScope>(request, ktor.coroutine_scope)!!.launch {
    val onFailure = get<Event>(request, ktor.on_failure)!!
    try {
      // TODO: 1. validate(request).
      request as IPersistentMap<Any, Any>

      val url = get<String>(request, ktor.url)!!
      val timeout = request[ktor.timeout]
      val method = request[ktor.method] as HttpMethod // TODO:
      val httpResponse = client.get {
        url(url)
        timeout {
          requestTimeoutMillis = (timeout as Number?)?.toLong()
        }
      }

      if (httpResponse.status == HttpStatusCode.OK) {
        val responseTypeInfo = get<TypeInfo>(request, ktor.response_type_info)!!
        val onSuccess = get<Event>(request, ktor.on_success)!!
        dispatch(onSuccess.conj(httpResponse.call.body(responseTypeInfo)))
      } else {
        // TODO: build error details and throw exception to remove duplication
        dispatch(onFailure.conj(httpResponse.status.value))
      }
    } catch (e: Exception) {
      val status = when (e) {
        is UnknownHostException -> 0
        is HttpRequestTimeoutException -> -1
        /*
        catch (e: NoTransformationFoundException) {
            TODO("504 Gateway Time-out: $e")
        } */
        else -> throw e
      }

      dispatch(onFailure.conj(status))
    }
  }
}

val regHttpKtor = run { regFx(ktor.http_fx, ::httpEffect) }

/*
 * -- :dispatch_debounce -------------------------------------------------------
 */

private val debounceRecord: Atom<IPersistentMap<Any, Any>> = atom(m())

@Suppress("EnumEntryName", "ClassName")
enum class bounce_fx {
  id,
  event,
  delay,
  time_received;

  override fun toString(): String = name
}

fun regBounceFx() {
  fun dispatchLater(debounce: IPersistentMap<Any?, Any?>) {
    // TODO: pass a CoroutineScope?
    GlobalScope.launch {
      val delayPeriod = get<Any>(debounce, bounce_fx.delay)!!
      delay((delayPeriod as Number).toLong())

      val timeReceived = debounce[bounce_fx.time_received]
      if (timeReceived == get<Any>(debounceRecord(), debounce[bounce_fx.id])) {
        dispatch(debounce[bounce_fx.event] as Event)
      }
    }
  }

  regFx(id = "dispatch_debounce") { debounce ->
    debounce as IPersistentMap<*, *>
    val now = Clock.System.now()
    val id = get<Any>(debounce, bounce_fx.id)!!
    debounceRecord.swap { it.assoc(id, now) }
    dispatchLater(debounce.assoc(bounce_fx.time_received, now))
  }
}
