package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.router.FsmEvent.ADD_EVENT
import com.github.whyrising.recompose.router.FsmEvent.FINISH_RUN
import com.github.whyrising.recompose.router.FsmEvent.RUN_QUEUE
import com.github.whyrising.recompose.router.State.IDLE
import com.github.whyrising.recompose.router.State.RUNNING
import com.github.whyrising.recompose.router.State.SCHEDULING
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.collections.PersistentVector
import com.github.whyrising.y.core.v
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

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

typealias FsmAction = (e: Event) -> Unit

internal val scope = CoroutineScope(
  context = SupervisorJob() + Dispatchers.Main.immediate
)

internal class EventQueueFSM(
  private val eventQueue: EventQueueActions,
  start: State = IDLE
) {
  private val _state: Atom<State> = atom(start)

  val state: State
    get() = _state()

  private val mutex = Mutex()

  private fun runQueue(e: Event) {
    scope.launch { handle(RUN_QUEUE) }
  }

  private fun processAllCurrentEvents(e: Event) {
    scope.launch {
      eventQueue.processCurrentEvents()
      handle(FINISH_RUN)
    }
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

  private fun givenWhenThen(
    givenFsmState: State,
    whenFsmEvent: FsmEvent
  ): PersistentVector<Any> = when (v(givenFsmState, whenFsmEvent)) {
    IDLE__ADD_EVENT -> SCHEDULING_enqueueEventAndRunQueue
    SCHEDULING__ADD_EVENT -> SCHEDULING_enqueueEvent
    SCHEDULING__RUN_QUEUE -> RUNNING_processAllCurrentEvents
    RUNNING__ADD_EVENT -> RUNNING_enqueuEvent
    RUNNING__FINISH_RUN -> when (eventQueue.count) {
      0 -> IDLE_identity
      else -> SCHEDULING_runQueue
    }
    else -> TODO("${givenFsmState.name}, ${whenFsmEvent.name}")
  }

  fun handle(fsmEvent: FsmEvent, e: Event = v()) {
    while (true) {
      val givenFsmState = state
      val (newFsmState, actionFn) = givenWhenThen(givenFsmState, fsmEvent)
      if (_state.compareAndSet(givenFsmState, newFsmState as State)) {
        (actionFn as FsmAction)(e)
        break
      }
    }
  }

  companion object {
    internal val identity: FsmAction = { _ -> }
  }
}

fun push(event: Event) = scope.launch { eventQueueFSM.handle(ADD_EVENT, event) }
