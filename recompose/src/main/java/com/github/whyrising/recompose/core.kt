package com.github.whyrising.recompose

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.cofx.injectDb
import com.github.whyrising.recompose.events.handle
import com.github.whyrising.recompose.events.register
import com.github.whyrising.recompose.fx.doFx
import com.github.whyrising.recompose.stdinterceptors.dbHandlerToInterceptor
import com.github.whyrising.recompose.stdinterceptors.fxHandlerToInterceptor
import com.github.whyrising.y.collections.map.IPersistentMap
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.launch

// -- Dispatch -----------------------------------------------------------------

fun dispatch(event: List<Any>) {
    publisher.onNext(event)
}

fun dispatchSync(event: List<Any>) {
    handle(event)
}

// -- Events ---------------------------------------------------

/**
 * Register the given event `handler` (function) for the given `id`.
 */
fun regEventDb(
    id: Any,
    interceptors: List<Any> = arrayListOf(),
    handler: (db: Any, vec: List<Any>) -> Any
) {
    register(
        id = id,
        interceptors = arrayListOf(
            injectDb,
            doFx,
            interceptors,
            dbHandlerToInterceptor(handler)
        )
    )
}

fun regEventFx(
    id: Any,
    interceptors: List<Any> = arrayListOf(),
    handler: (
        cofx: IPersistentMap<Any, Any>,
        event: List<Any>
    ) -> IPersistentMap<Any, Any>
) {
    register(
        id = id,
        interceptors = arrayListOf(
            injectDb,
            doFx,
            interceptors,
            fxHandlerToInterceptor(handler)
        )
    )
}

// -- Subscriptions ------------------------------------------------------------

fun <T> subscribe(qvec: List<Any>): T {
    return com.github.whyrising.recompose.subs.subscribe(qvec)
}

fun <T> regSub(
    queryId: Any,
    computationFn: (db: T, queryVec: List<Any>) -> Any,
) {
    com.github.whyrising.recompose.subs.regSub(queryId, computationFn)
}

fun regSub(
    queryId: Any,
    inputFn: (queryVec: List<Any>) -> Any,
    computationFn: (input: Any, queryVec: List<Any>) -> Any,
) {
    com.github.whyrising.recompose.subs.regSub(
        queryId,
        inputFn,
        computationFn
    )
}

// -- Effects ------------------------------------------------------------------

/**
 * Register the given effect `handler` for the given `id`
 * @param id
 * @param handler is a side-effecting function which takes a single argument
 * and whose return value is ignored.
 */
fun regFx(id: Any, handler: (value: Any) -> Unit) {
    com.github.whyrising.recompose.fx.regFx(id, handler)
}

// -- Framework ----------------------------------------------------------------

val publisher: PublishSubject<Any> = PublishSubject.create()

private inline fun <reified T> subscribe(): Observable<T> = publisher.filter {
    it is T
}.map {
    it as T
}

class Framework : ViewModel() {
    private val receiver = subscribe<List<Any>>().subscribe { eventVec ->
        if (eventVec.isEmpty()) return@subscribe

        viewModelScope.launch {
            handle(eventVec)
        }
    }

    fun halt() {
        Log.i("halt", "receiver disposed.")
        receiver.dispose()
    }

    override fun onCleared() {
        super.onCleared()
        halt()
    }
}
