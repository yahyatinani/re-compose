/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.yahyatinani.recompose.pagingfx

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.paging.CombinedLoadStates
import androidx.paging.ItemSnapshotList
import androidx.paging.LoadState
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import io.github.yahyatinani.recompose.clearEvent
import io.github.yahyatinani.recompose.clearFx
import io.github.yahyatinani.recompose.dispatch
import io.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.recompose.fx.BuiltInFx.fx
import io.github.yahyatinani.recompose.regEventFx
import io.github.yahyatinani.recompose.regFx
import io.github.yahyatinani.y.core.collections.ISeq
import io.github.yahyatinani.y.core.fold
import io.github.yahyatinani.y.core.m
import io.github.yahyatinani.y.core.v
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

/**
 * @param flow the [Flow] object which contains a stream of [PagingData] elements.
 */
@SuppressLint("RestrictedApi")
class LazyPagingItems<T : Any> internal constructor(
  private val flow: Flow<PagingData<T>>,
  val onAppendEvent: Event,
  val onAppendArgs: ISeq<Any>?,
  private val triggerAppendId: Any,
) {
  init {
    regFx(triggerAppendId) { index ->
      // Notify Paging of the item access to trigger any loads necessary to
      // fulfill prefetchDistance.
      if ((index as Int) < pagingDataPresenter.size) {
        pagingDataPresenter[index]
      }
    }

    regEventFx(triggerAppendId) { _, event ->
      m(fx to v(event))
    }
  }

  fun clear() {
    clearEvent(triggerAppendId)
    clearFx(triggerAppendId)
  }

  private val mainDispatcher = AndroidUiDispatcher.Main

  /**
   * If the [flow] is a SharedFlow, it is expected to be the flow returned by from
   * pager.flow.cachedIn(scope) which could contain a cached PagingData. We pass the cached
   * PagingData to the presenter so that if the PagingData contains cached data, the presenter can
   * be initialized with the data prior to collection on pager.
   */
  private val pagingDataPresenter =
    object :
      PagingDataPresenter<T>(
        mainContext = mainDispatcher,
        cachedPagingData =
        if (flow is SharedFlow<PagingData<T>>) flow.replayCache.firstOrNull() else null,
      ) {
      override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
        updateItemSnapshotList()
      }
    }

  /**
   * Contains the immutable [ItemSnapshotList] of currently presented items, including any
   * placeholders if they are enabled. Note that similarly to [peek] accessing the items in a list
   * will not trigger any loads. Use [get] to achieve such behavior.
   */
  var itemSnapshotList by mutableStateOf(pagingDataPresenter.snapshot())
    private set

  /**
   * The number of items which can be accessed.
   */
  val itemCount: Int
    get() = itemSnapshotList.size

  private fun updateItemSnapshotList() {
    itemSnapshotList = pagingDataPresenter.snapshot()
  }

  operator fun get(index: Int): T? {
    pagingDataPresenter[index] // this registers the value load
    return itemSnapshotList[index]
  }

  fun peek(index: Int): T? = itemSnapshotList[index]

  /**
   * Retry any failed load requests that would result in a [LoadState.Error]
   * update to this [LazyPagingItems].
   *
   * Unlike [refresh], this does not invalidate [PagingSource], it only retries
   * failed loads within the same generation of [PagingData].
   *
   * [LoadState.Error] can be generated from two types of load requests:
   *  * [PagingSource.load] returning [PagingSource.LoadResult.Error]
   *  * [RemoteMediator.load] returning [RemoteMediator.MediatorResult.Error]
   */
  fun retry() {
    pagingDataPresenter.retry()
  }

  /**
   * Refresh the data presented by this [LazyPagingItems].
   *
   * [refresh] triggers the creation of a new [PagingData] with a new instance
   * of [PagingSource] to represent an updated snapshot of the backing dataset.
   * If a [RemoteMediator] is set, calling [refresh] will also trigger a call to
   * [RemoteMediator.load] with [LoadType] [REFRESH] to allow [RemoteMediator]
   * to check for updates to the dataset backing [PagingSource].
   *
   * Note: This API is intended for UI-driven refresh signals, such as
   * swipe-to-refresh.
   * Invalidation due repository-layer signals, such as DB-updates, should
   * instead use [PagingSource.invalidate].
   *
   * @see PagingSource.invalidate
   */
  fun refresh() {
    pagingDataPresenter.refresh()
  }

  /**
   * A [CombinedLoadStates] object which represents the current loading state.
   */
  var loadState: CombinedLoadStates by
    mutableStateOf(
      pagingDataPresenter.loadStateFlow.value
        ?: CombinedLoadStates(
          refresh = InitialLoadStates.refresh,
          prepend = InitialLoadStates.prepend,
          append = InitialLoadStates.append,
          source = InitialLoadStates,
        ),
    )
    private set

  internal suspend fun collectLoadState() = pagingDataPresenter
    .loadStateFlow
    .filterNotNull()
    .collect { loadStates: CombinedLoadStates ->
      loadState = loadStates

      val event = when (loadStates.refresh) {
        is Error ->
          onAppendEvent
            .conj(v(triggerAppendId, "error"))
            .conj((loadStates.refresh as Error).error)
            .let {
              onAppendArgs?.fold(it) { acc, arg -> acc.conj(arg) } ?: it
            }

        else -> when (val append = loadStates.source.append) {
          is Error ->
            onAppendEvent
              .conj(v(triggerAppendId, "error"))
              .conj(append.error)
              .let {
                onAppendArgs?.fold(it) { acc, arg -> acc.conj(arg) } ?: it
              }

          Loading ->
            onAppendEvent
              .conj(v(triggerAppendId, "loading"))
              .let {
                onAppendArgs?.fold(it) { acc, arg -> acc.conj(arg) } ?: it
              }

          is NotLoading -> {
            if (itemSnapshotList.items.isEmpty()) return@collect

            onAppendEvent
              .conj(v(triggerAppendId, "done_loading"))
              .conj(itemSnapshotList)
              .let {
                onAppendArgs?.fold(it) { acc, arg -> acc.conj(arg) } ?: it
              }
              .conj(append.endOfPaginationReached)
          }
        }
      }

      dispatch(event)
    }

  internal suspend fun collectPagingData() {
    flow.collectLatest { pagingDataPresenter.collectFrom(it) }
  }
}

private val IncompleteLoadState = NotLoading(endOfPaginationReached = false)
private val InitialLoadStates = LoadStates(
  refresh = Loading,
  prepend = IncompleteLoadState,
  append = IncompleteLoadState,
)
