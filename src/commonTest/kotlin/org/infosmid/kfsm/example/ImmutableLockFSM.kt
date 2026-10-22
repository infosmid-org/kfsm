/*
 * Copyright 2019-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infosmid.kfsm.example

import org.infosmid.kfsm.functionalStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @suppress
 */
// tag::context[]
data class ImmutableLock(val locked: Int = 1) {

    fun lock(): ImmutableLock {
        require(locked == 0)
        println("Lock")
        return copy(locked = locked + 1)
    }

    fun doubleLock(): ImmutableLock {
        require(locked == 1)
        println("DoubleLock")
        return copy(locked = locked + 1)
    }

    fun unlock(): ImmutableLock {
        require(locked == 1)
        println("Unlock")
        return copy(locked = locked - 1)
    }

    fun doubleUnlock(): ImmutableLock {
        require(locked == 2)
        println("DoubleUnlock")
        return copy(locked = locked - 1)
    }

    override fun toString(): String {
        return "Lock(locked=$locked)"
    }
}
// end::context[]

// tag::definition[]
class ImmutableLockFSM {

    companion object {
        fun handleEvent(context: ImmutableLock, event: LockEvents): ImmutableLock {
            val fsm = definition.create(context)
            return fsm.sendEvent(event, context) ?: error("Expected context not null")
        }

        private val definition = functionalStateMachine(
            LockStates.entries.toSet(),
            LockEvents.entries.toSet(),
            ImmutableLock::class
        ) {
            defaultInitialState = LockStates.LOCKED
            initialState {
                when (locked) {
                    0 -> LockStates.UNLOCKED
                    1 -> LockStates.LOCKED
                    2 -> LockStates.DOUBLE_LOCKED
                    else -> error("Invalid state locked=$locked")
                }
            }
            invariant("invalid locked value") { locked in 0..2 }
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            default {
                action { state, event, _ ->
                    println("Default action for state($state) -> on($event) for $this")
                    this
                }
                onEntry { startState, targetState, _ ->
                    println("entering:$startState -> $targetState for $this")
                }
                onExit { startState, targetState, _ ->
                    println("exiting:$startState -> $targetState for $this")
                }
            }
            whenState(LockStates.LOCKED) {
                onEvent(LockEvents.LOCK to LockStates.DOUBLE_LOCKED) {
                    doubleLock()
                }
                onEvent(LockEvents.UNLOCK to LockStates.UNLOCKED) {
                    unlock()
                }
            }
            whenState(LockStates.DOUBLE_LOCKED) {
                onEvent(LockEvents.UNLOCK to LockStates.LOCKED) {
                    doubleUnlock()
                }
            }
            whenState(LockStates.UNLOCKED) {
                onEvent(LockEvents.LOCK to LockStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }
}

operator fun ImmutableLock.plus(event: LockEvents): ImmutableLock {
    return ImmutableLockFSM.handleEvent(this, event)
}
// end::definition[]

class TestFunctionalLock {
    @Test
    fun testState() {
        // tag::test[]
        val lock = ImmutableLock(0)
        val locked = lock + LockEvents.LOCK
        assertEquals(locked.locked, 1)
        val doubleLocked = locked + LockEvents.LOCK
        assertEquals(doubleLocked.locked, 2)
        // end::test[]
    }
}
