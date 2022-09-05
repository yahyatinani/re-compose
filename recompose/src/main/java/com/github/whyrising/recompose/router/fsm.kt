package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.fx.registerBuiltinEffectHandlers
import com.github.whyrising.recompose.ids.recompose
import com.github.whyrising.recompose.router.FsmEvent.ADD_EVENT
import com.github.whyrising.recompose.router.FsmEvent.EXCEPTION
import com.github.whyrising.recompose.router.FsmEvent.FINISH_RUN
import com.github.whyrising.recompose.router.FsmEvent.RUN_QUEUE
import com.github.whyrising.recompose.router.State.IDLE
import com.github.whyrising.recompose.router.State.RUNNING
import com.github.whyrising.recompose.router.State.SCHEDULING
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.v
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal enum class State {
  IDLE,
  SCHEDULING,
  RUNNING,
  PAUSED
}

internal enum class FsmEvent {
  ADD_EVENT,
  RUN_QUEUE,
  FINISH_RUN,
  PAUSE,
  RESUME,
  EXCEPTION
}

/* FSM transitions:*/
internal val IDLE__ADD_EVENT = v(IDLE, ADD_EVENT)
internal val SCHEDULING__RUN_QUEUE = v(SCHEDULING, RUN_QUEUE)
internal val RUNNING__FINISH_RUN = v(RUNNING, FINISH_RUN)
internal val RUNNING__ADD_EVENT = v(RUNNING, ADD_EVENT)
internal val SCHEDULING__ADD_EVENT = v(SCHEDULING, ADD_EVENT)
internal val RUNNING__EXCEPTION = v(RUNNING, EXCEPTION)

typealias FsmAction = (arg: Any?) -> Unit

internal val scope = CoroutineScope(
  context = SupervisorJob() + Dispatchers.Default
)

internal class EventQueueFSM(
  internal val eventQueue: EventQueueActions,
  start: State = IDLE,
  handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, _ -> }
) {
  private var consumeSignal = CompletableDeferred<Unit>()

  init {
    registerBuiltInStuff()
    // events consumer coroutine.
    scope.launch(handler) {
      while (true) {
        consumeSignal.await()
        try {
          eventQueue.processCurrentEvents()
        } catch (ex: Exception) {
          handle(EXCEPTION, ex)
        }
        // this doesn't execute when an exception occurs because handle() throws
        // again in catch block.
        handle(FINISH_RUN)

        consumeSignal = CompletableDeferred()
      }
    }
  }

  private val _state: Atom<State> = atom(start)

  val state: State
    get() = _state()

  private fun runQueue(arg: Any?) {
    scope.launch { handle(RUN_QUEUE) }
  }

  internal fun processAllCurrentEvents(arg: Any?) {
    consumeSignal.complete(Unit)
  }

  private val IDLE_identity = v(IDLE, identity)
  private val SCHEDULING_runQueue = v(SCHEDULING, ::runQueue)
  private val SCHEDULING_enqueueEvent = v(SCHEDULING, { e: Event ->
    eventQueue.enqueue(e)
  })
  private val SCHEDULING_enqueueEventAndRunQueue = v(SCHEDULING, { e: Event ->
    eventQueue.enqueue(e)
    runQueue(e)
  })
  private val RUNNING_enqueuEvent = v(RUNNING, { e: Event ->
    eventQueue.enqueue(e)
  })
  private val RUNNING_processAllCurrentEvents =
    v(RUNNING, ::processAllCurrentEvents)
  private val IDLE_exception = v(IDLE, { ex: Exception ->
    eventQueue.exception(ex)
  })

  private fun givenWhenThen(givenFsmState: State, whenFsmEvent: FsmEvent) =
    when (v(givenFsmState, whenFsmEvent)) {
      IDLE__ADD_EVENT -> SCHEDULING_enqueueEventAndRunQueue
      SCHEDULING__ADD_EVENT -> SCHEDULING_enqueueEvent
      SCHEDULING__RUN_QUEUE -> RUNNING_processAllCurrentEvents
      RUNNING__ADD_EVENT -> RUNNING_enqueuEvent
      RUNNING__EXCEPTION -> IDLE_exception
      RUNNING__FINISH_RUN -> when (eventQueue.count) {
        0 -> IDLE_identity
        else -> SCHEDULING_runQueue
      }
      else -> TODO("${givenFsmState.name}, ${whenFsmEvent.name}")
    }

  fun handle(fsmEvent: FsmEvent, arg: Any? = null) {
    while (true) {
      val givenFsmState = state
      val (newFsmState, actionFn) = givenWhenThen(givenFsmState, fsmEvent)
      if (_state.compareAndSet(givenFsmState, newFsmState as State)) {
        (actionFn as FsmAction)(arg)
        break
      }
    }
  }

  companion object {
    internal val identity: FsmAction = { _ -> }
    internal fun registerBuiltInStuff() {
      regCofx(id = recompose.db) { coeffects ->
        coeffects.assoc(recompose.db, appDb.deref())
      }

      registerBuiltinEffectHandlers()
    }
  }
}
