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
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.paging.CombinedLoadStates
import androidx.paging.DifferCallback
import androidx.paging.ItemSnapshotList
import androidx.paging.LOGGER
import androidx.paging.LOG_TAG
import androidx.paging.LoadState
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadStates
import androidx.paging.Logger
import androidx.paging.NullPaddedList
import androidx.paging.PagingData
import androidx.paging.PagingDataDiffer
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
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

private val IncompleteLoadState = NotLoading(false)
private val InitialLoadStates = LoadStates(
  Loading,
  IncompleteLoadState,
  IncompleteLoadState
)

/**
 * @param flow the [Flow] object which contains a stream of [PagingData] elements.
 */
@SuppressLint("RestrictedApi")
class LazyPagingItems<T : Any> internal constructor(
  private val flow: Flow<PagingData<T>>,
  val onAppendEvent: Event,
  val onAppendArgs: ISeq<Any>?,
  private val triggerAppendId: Any
) {
  init {
    regFx(triggerAppendId) { index ->
      // Notify Paging of the item access to trigger any loads necessary to
      // fulfill prefetchDistance.
      if ((index as Int) < pagingDataDiffer.size) {
        pagingDataDiffer[index]
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

  private val mainDispatcher = Dispatchers.Main

  /**
   * Contains the immutable [ItemSnapshotList] of currently presented items,
   * including any placeholders if they are enabled.
   * Note that similarly to [peek] accessing the items in a list will not
   * trigger any loads.
   * Use [get] to achieve such behavior.
   */
  var itemSnapshotList by mutableStateOf(
    ItemSnapshotList<T>(
      placeholdersBefore = 0,
      placeholdersAfter = 0,
      items = emptyList()
    )
  )
    private set

  /**
   * The number of items which can be accessed.
   */
  val itemCount: Int get() = itemSnapshotList.size

  private val differCallback: DifferCallback = object : DifferCallback {
    override fun onChanged(position: Int, count: Int) {
      if (count > 0) {
        updateItemSnapshotList()
      }
    }

    override fun onInserted(position: Int, count: Int) {
      if (count > 0) {
        updateItemSnapshotList()
      }
    }

    override fun onRemoved(position: Int, count: Int) {
      if (count > 0) {
        updateItemSnapshotList()
      }
    }
  }

  private val pagingDataDiffer: PagingDataDiffer<T> =
    object : PagingDataDiffer<T>(
      differCallback = differCallback,
      mainContext = mainDispatcher
    ) {
      override suspend fun presentNewList(
        previousList: NullPaddedList<T>,
        newList: NullPaddedList<T>,
        lastAccessedIndex: Int,
        onListPresentable: () -> Unit
      ): Int? {
        onListPresentable()
        updateItemSnapshotList()
        return null
      }
    }

  private val itemSnapshotListUpdateCount = atomic(0)
  private fun updateItemSnapshotList() {
    val snapshot = pagingDataDiffer.snapshot()
    itemSnapshotList = snapshot
    itemSnapshotListUpdateCount.incrementAndGet()
  }

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
  fun retry() = pagingDataDiffer.retry()

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
  fun refresh() = pagingDataDiffer.refresh()

  /**
   * A [CombinedLoadStates] object which represents the current loading state.
   */
  var loadState: CombinedLoadStates by mutableStateOf(
    pagingDataDiffer.loadStateFlow.value
      ?: CombinedLoadStates(
        refresh = InitialLoadStates.refresh,
        prepend = InitialLoadStates.prepend,
        append = InitialLoadStates.append,
        source = InitialLoadStates
      )
  )
    private set

  internal suspend fun collectLoadState() = pagingDataDiffer
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
            if (itemSnapshotListUpdateCount.value == 0) return@collect

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

  internal suspend fun collectPagingData() = flow.collectLatest {
    pagingDataDiffer.collectFrom(it)
  }

  private companion object {
    init {
      /**
       * Implements the Logger interface from paging-common and injects it into
       * the LOGGER global var stored within Pager.
       *
       * Checks for null LOGGER because other runtime entry points to paging can
       * also inject a Logger
       */
      LOGGER = LOGGER ?: object : Logger {
        override fun isLoggable(level: Int): Boolean {
          return Log.isLoggable(LOG_TAG, level)
        }

        override fun log(level: Int, message: String, tr: Throwable?) {
          when {
            tr != null && level == Log.DEBUG -> Log.d(LOG_TAG, message, tr)
            tr != null && level == Log.VERBOSE -> Log.v(LOG_TAG, message, tr)
            level == Log.DEBUG -> Log.d(LOG_TAG, message)
            level == Log.VERBOSE -> Log.v(LOG_TAG, message)
            else -> {
              throw IllegalArgumentException(
                "debug level $level is requested but Paging only supports " +
                  "default logging for level 2 (DEBUG) or level 3 (VERBOSE)"
              )
            }
          }
        }
      }
    }
  }
}
