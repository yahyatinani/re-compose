@file:Suppress("UNCHECKED_CAST")

package io.github.yahyatinani.recompose.router

import io.github.yahyatinani.recompose.cofx.registerDbInjectorCofx
import io.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.recompose.fx.registerBuiltinFxHandlers
import io.github.yahyatinani.recompose.router.FsmEvent.ADD_EVENT
import io.github.yahyatinani.recompose.router.FsmEvent.EXCEPTION
import io.github.yahyatinani.recompose.router.FsmEvent.FINISH_RUN
import io.github.yahyatinani.recompose.router.FsmEvent.RUN_QUEUE
import io.github.yahyatinani.recompose.router.State.IDLE
import io.github.yahyatinani.recompose.router.State.RUNNING
import io.github.yahyatinani.recompose.router.State.SCHEDULING
import io.github.yahyatinani.y.concurrency.Atom
import io.github.yahyatinani.y.concurrency.atom
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

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

typealias FsmAction = (arg: Any?) -> Unit

internal class EventQueueFSM(
  internal val eventQueue: EventQueueActions,
  start: State = IDLE,
  val context: CoroutineContext = Dispatchers.Default,
  internal val scope: CoroutineScope = CoroutineScope(
    context + CoroutineName("Recompose Events Scope")
  )
) {
  // -- FSM actions -----------------------------------------------

  /**
   * @return Returns [Deferred] only because of testing purposes.
   */
  @Suppress("UNUSED_PARAMETER")
  internal fun processAllCurrentEvents(arg: Any?): Job = scope.launch {
    try {
      eventQueue.processCurrentEvents()
    } catch (ex: Throwable) {
      fsmTrigger(EXCEPTION, ex)
    }
    // this doesn't execute when an exception occurs because fsmTrigger()
    // throws again in catch block.
    fsmTrigger(FINISH_RUN)
  }

  @Suppress("UNUSED_PARAMETER")
  private fun runQueue(arg: Any?) = fsmTrigger(RUN_QUEUE)

  internal fun enqueueEvent(e: Event) {
    eventQueue.enqueue(e)
  }

  internal fun enqueueEventAndRunQueue(e: Event) {
    eventQueue.enqueue(e)
    runQueue(e)
  }

  @Suppress("UNUSED_PARAMETER")
  internal fun identity(arg: Any?) = Unit

  internal fun exception(ex: Throwable) = eventQueue.exception(ex)

  private val IDLE_identity = Pair(IDLE, ::identity)
  private val SCHEDULING_runQueue = Pair(SCHEDULING, ::runQueue)
  private val SCHEDULING_enqueueEvent = Pair(SCHEDULING, ::enqueueEvent)
  private val RUNNING_enqueuEvent = Pair(RUNNING, ::enqueueEvent)
  private val SCHEDULING_enqueueEventAndRunQueue =
    Pair(SCHEDULING, ::enqueueEventAndRunQueue)
  private val RUNNING_processAllCurrentEvents =
    Pair(RUNNING, { arg: Any? -> processAllCurrentEvents(arg) })
  private val IDLE_exception = Pair(IDLE, ::exception)

  // -- FSM implementation -----------------------------------------------------

  internal val _state: Atom<State> = atom(start)

  val state: State
    get() = _state.deref()

  /**
   * @param currentState Current FSM state.
   * @param trigger An FSM event that occurred.
   *
   * @return Given we're in [currentState], when [trigger] happens, return a vec
   * of next FSM state and action to execute.
   */
  private fun givenWhenThen(currentState: State, trigger: FsmEvent) =
    when (Pair(currentState, trigger)) {
      IDLE__ADD_EVENT -> SCHEDULING_enqueueEventAndRunQueue
      SCHEDULING__ADD_EVENT -> SCHEDULING_enqueueEvent
      SCHEDULING__RUN_QUEUE -> RUNNING_processAllCurrentEvents
      RUNNING__ADD_EVENT -> RUNNING_enqueuEvent
      RUNNING__EXCEPTION -> IDLE_exception
      RUNNING__FINISH_RUN -> when (eventQueue.count) {
        0 -> IDLE_identity
        else -> SCHEDULING_runQueue
      }

      else -> TODO("${currentState.name}, ${trigger.name}")
    }

  /** Emit an FSM trigger/event. */
  fun fsmTrigger(fsmEvent: FsmEvent, arg: Any? = null) {
    while (true) {
      val currentState = state
      val (nextFsmState, actionFn) = givenWhenThen(currentState, fsmEvent)
      if (_state.compareAndSet(currentState, nextFsmState as State)) {
        (actionFn as FsmAction)(arg)
        break
      }
    }
  }

  fun push(event: Event) {
    fsmTrigger(ADD_EVENT, event)
  }

  companion object {
    init {
      registerBuiltinFxHandlers()
      registerDbInjectorCofx()
    }

    /* FSM transitions: */
    internal val IDLE__ADD_EVENT = Pair(IDLE, ADD_EVENT)
    internal val SCHEDULING__RUN_QUEUE = Pair(SCHEDULING, RUN_QUEUE)
    internal val RUNNING__FINISH_RUN = Pair(RUNNING, FINISH_RUN)
    internal val RUNNING__ADD_EVENT = Pair(RUNNING, ADD_EVENT)
    internal val SCHEDULING__ADD_EVENT = Pair(SCHEDULING, ADD_EVENT)
    internal val RUNNING__EXCEPTION = Pair(RUNNING, EXCEPTION)
  }
}
