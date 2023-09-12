package io.github.yahyatinani.recompose.pagingfx

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingState
import androidx.paging.cachedIn
import io.github.yahyatinani.recompose.dispatch
import io.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.recompose.httpfx.httpFxClient
import io.github.yahyatinani.recompose.httpfx.ktor
import io.github.yahyatinani.recompose.regFx
import io.github.yahyatinani.y.core.collections.IPersistentMap
import io.github.yahyatinani.y.core.collections.ISeq
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.seq
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.UnknownHostException

@Suppress("ClassName", "EnumEntryName")
enum class paging {
  fx,
  page_size,
  trigger_append_id,
  on_append,
  pageName,
  nextUrl,
  on_append_args;

  override fun toString(): String = "${this::class.simpleName}:$name"
}

// val pagingSrcCache: Atom<IPersistentMap<Any, Any>> = atom(m())

internal suspend fun httpCall(
  request: Any?,
  loadParams: LoadParams<Any>,
  onSuccess: Event?
): Any {
  // TODO: 1. validate(request).
  request as IPersistentMap<Any, Any>

  val url = when (val nextPageKey = loadParams.key) {
    INITIAL_KEY -> get<String>(request, ktor.url)!!

    else -> {
      val nextUrl = get<String>(request, paging.nextUrl)!!.let {
        if (it.contains("?")) "$it&" else "$it?"
      }
      "$nextUrl${request[paging.pageName]}=$nextPageKey"
    }
  }

  val timeout = request[ktor.timeout]
  val method = request[ktor.method] as HttpMethod // TODO:

  val httpResponse = try {
    httpFxClient.get {
      url(url)
      timeout {
        requestTimeoutMillis =
          (timeout as Number?)?.toLong()
      }
    }
  } catch (e: Exception) {
    return httpErrorByException(e, url, method)
  }

  val status = httpResponse.status
  return if (status == HttpStatusCode.OK) {
    val ret =
      httpResponse.call.body(get<TypeInfo>(request, ktor.response_type_info)!!)
    if (onSuccess != null) {
      dispatch(onSuccess.conj(ret))
    }
    ret
  } else {
    val httpRequest = httpResponse.request
    HttpError(
      uri = httpRequest.url.toString(),
      method = httpRequest.method.value,
      error = status.description,
      status = status.value,
      debugMessage = "Http response at 400 or 500 level"
    )
  }
}

data class PagingSourceImp(
  val onSuccess: Event?,
  val request: Any?
) : PagingSource<Any, Any>() {
  override fun getRefreshKey(state: PagingState<Any, Any>) =
    state.anchorPosition?.let { i -> state.closestPageToPosition(i)?.nextKey }

  override suspend fun load(params: LoadParams<Any>): LoadResult<Any, Any> =
    when (val result = httpCall(request, params, onSuccess)) {
      is Page -> LoadResult.Page(result.data, result.prevKey, result.nextKey)
      is Throwable -> LoadResult.Error(result)

      else -> TODO()
    }
}

private const val INITIAL_KEY = "INITIAL_KEY"

private fun httpErrorByException(
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

fun pagingEffect(request: Any?) {
  val coroutineScope = get<CoroutineScope>(request, ktor.coroutine_scope)!!
  val job = coroutineScope.launch {
    val onSuccess = get<Event>(request, ktor.on_success)
    val appendId = get<Any>(request, paging.trigger_append_id)!!
    val pager = Pager(
      config = PagingConfig(pageSize = get(request, paging.page_size, 10)!!),
      initialKey = INITIAL_KEY
    ) {
      PagingSourceImp(
        onSuccess = onSuccess,
        request = request
      )
    }

    val onAppendEvent = get<Event>(request, paging.on_append)!!
    val onAppendArgs = seq(get<ISeq<Any>>(request, paging.on_append_args))
    val lazyPagingItems = LazyPagingItems(
      flow = pager.flow.cachedIn(coroutineScope),
      onAppendEvent = onAppendEvent,
      onAppendArgs = onAppendArgs as ISeq<Any>?,
      triggerAppendId = appendId
    )
    launch { lazyPagingItems.collectPagingData() }
      .invokeOnCompletion { lazyPagingItems.clear() }

    launch { lazyPagingItems.collectLoadState() }
  }
}

fun regPagingFx() = regFx(paging.fx, ::pagingEffect)
