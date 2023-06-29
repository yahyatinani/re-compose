package io.github.yahyatinani.recompose.pagingfx

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingState
import androidx.paging.cachedIn
import io.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.recompose.httpfx.httpFxClient
import io.github.yahyatinani.recompose.httpfx.ktor
import io.github.yahyatinani.recompose.regFx
import io.github.yahyatinani.y.core.collections.IPersistentMap
import io.github.yahyatinani.y.core.get
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Suppress("ClassName", "EnumEntryName")
enum class paging {
  fx;

  override fun toString(): String = "${this::class.simpleName}:$name"
}

fun regPagingFx() = regFx(paging.fx, ::pagingEffect)

// val pagingSrcCache: Atom<IPersistentMap<Any, Any>> = atom(m())

data class PagingSourceImp(
  val onFailure: Event,
  val httpCall: suspend (LoadParams<Any>) -> Any
) : PagingSource<Any, Any>() {
  override fun getRefreshKey(state: PagingState<Any, Any>) =
    state.anchorPosition?.let { i -> state.closestPageToPosition(i)?.nextKey }

  override suspend fun load(params: LoadParams<Any>): LoadResult<Any, Any> {
    return when (val result = httpCall(params)) {
      is Page -> LoadResult.Page(result.data, result.prevKey, result.nextKey)
      is Throwable -> LoadResult.Error(result)

      else -> TODO()
    }
  }
}

private const val INITIAL_KEY = "INITIAL_KEY"

internal suspend fun httpCall(request: Any?, loadParams: LoadParams<Any>): Any =
  try {
    // TODO: 1. validate(request).
    request as IPersistentMap<Any, Any>

    val url = when (val nextPageKey = loadParams.key) {
      INITIAL_KEY -> get<String>(request, ktor.url)!!

      else -> "${request["nextUrl"]}&${request["pageName"]}=$nextPageKey"
    }

    val timeout = request[ktor.timeout]
    val method = request[ktor.method] as HttpMethod // TODO:
    val httpResponse = httpFxClient.get {
      url(url)
      timeout {
        requestTimeoutMillis =
          (timeout as Number?)?.toLong()
      }
    }

    if (httpResponse.status == HttpStatusCode.OK) {
      val responseTypeInfo = get<TypeInfo>(request, ktor.response_type_info)!!
      httpResponse.call.body(responseTypeInfo) as Page
//    httpResponse.call.body<Page>()
    } else {
      TODO("${httpResponse.status}, $url")
    }
  } catch (e: Exception) {
    e
  }

fun pagingEffect(request: Any?) {
  val coroutineScope = get<CoroutineScope>(request, ktor.coroutine_scope)!!
  val job = coroutineScope.launch {
    val onFailure = get<Event>(request, ktor.on_failure)!!
    val eventId = get<Any>(request, "eventId")!!
    val pager = Pager(PagingConfig(pageSize = 10), initialKey = INITIAL_KEY) {
      PagingSourceImp(onFailure) { loadParams: LoadParams<Any> ->
        return@PagingSourceImp httpCall(request, loadParams)
      }
    }

    val lazyPagingItems = LazyPagingItems(
      flow = pager.flow.cachedIn(coroutineScope),
      onSuccessEvent = get<Event>(request, ktor.on_success)!!,
      onAppendEvent = get<Event>(request, "on_appending")!!,
      onFailure = onFailure
    )
    launch { lazyPagingItems.collectPagingData() }
      .invokeOnCompletion { lazyPagingItems.clear() }
    launch { lazyPagingItems.collectLoadState() }
      .invokeOnCompletion { lazyPagingItems.clear() }
  }
}
