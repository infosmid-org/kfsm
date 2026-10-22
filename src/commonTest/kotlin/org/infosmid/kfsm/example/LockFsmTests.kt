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

import org.infosmid.kfsm.AnyStateMachineBuilder
import org.infosmid.kfsm.AnyStateMachineInstance
import org.infosmid.kfsm.example.LockEvents.LOCK
import org.infosmid.kfsm.example.LockEvents.UNLOCK
import org.infosmid.kfsm.example.LockStates.DOUBLE_LOCKED
import org.infosmid.kfsm.example.LockStates.LOCKED
import org.infosmid.kfsm.example.LockStates.UNLOCKED
import org.infosmid.kfsm.stateMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * @suppress
 */
class LockFsmTests {

    private fun verifyLockFSM(fsm: AnyStateMachineInstance<LockStates, LockEvents, Lock>, lock: Lock) {
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked == 1 }
        // when
        fsm.sendEvent(UNLOCK)
        // then
        assertTrue { fsm.currentState == UNLOCKED }
        assertTrue { lock.locked == 0 }
        try {
            // when
            fsm.sendEvent(UNLOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Already unlocked", x.message)
        }
        // when
        fsm.sendEvent(LOCK)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked == 1 }
        // when
        fsm.sendEvent(LOCK)
        // then
        assertTrue { fsm.currentState == DOUBLE_LOCKED }
        assertTrue { lock.locked == 2 }
        try {
            // when
            fsm.sendEvent(LOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Already double locked", x.message)
        }
        assertTrue { lock.locked == 2 }
    }

    @Test
    fun testPlainCreationOfFsm() {
        // given
        val builder = AnyStateMachineBuilder<LockStates, LockEvents, Lock>(
            LockStates.entries.toSet(),
            LockEvents.entries.toSet()
        )
        builder.initialState {
            when (locked) {
                0 -> UNLOCKED
                1 -> LOCKED
                2 -> DOUBLE_LOCKED
                else -> error("Invalid state locked=$locked")
            }
        }
        builder.transition(LOCKED, UNLOCK, UNLOCKED) {
            unlock()
        }
        builder.transition(LOCKED, LOCK, DOUBLE_LOCKED) {
            doubleLock()
        }
        builder.transition(DOUBLE_LOCKED, UNLOCK, LOCKED) {
            doubleUnlock()
        }
        builder.transition(DOUBLE_LOCKED, LOCK) {
            error("Already double locked")
        }
        builder.transition(UNLOCKED, LOCK, LOCKED) {
            lock()
        }
        builder.transition(UNLOCKED, UNLOCK) {
            error("Already unlocked")
        }
        val definition = builder.complete()
        // when
        val lock = Lock()
        val fsm = definition.create(lock)
        // then
        verifyLockFSM(fsm, lock)
    }

    @Test
    fun testDslCreationOfFsm() {
        // given
        val definition = stateMachine(
            LockStates.entries.toSet(),
            LockEvents.entries.toSet(),
            Lock::class
        ) {
            initialState {
                when (locked) {
                    0 -> UNLOCKED
                    1 -> LOCKED
                    2 -> DOUBLE_LOCKED
                    else -> error("Invalid state locked=$locked")
                }
            }

            whenState(LOCKED) {
                onEvent(LOCK to DOUBLE_LOCKED) {
                    doubleLock()
                }
                onEvent(UNLOCK to UNLOCKED) {
                    unlock()
                }
            }
            whenState(DOUBLE_LOCKED) {
                onEvent(UNLOCK to LOCKED) {
                    doubleUnlock()
                }
                onEvent(LOCK) {
                    error("Already double locked")
                }
            }
            whenState(UNLOCKED) {
                onEvent(LOCK to LOCKED) {
                    lock()
                }
                onEvent(UNLOCK) {
                    error("Already unlocked")
                }
            }
        }.build()
        // when
        val lock = Lock()
        val fsm = definition.create(lock)
        // then
        verifyLockFSM(fsm, lock)
    }

    @Test
    fun simpleLockTest() {
        val lock = Lock(0)
        val fsm = LockFSM(lock)
        println("--lock1")
        fsm.lock()
        println("--lock2")
        fsm.lock()
        println("--lock3")
        fsm.lock()
        println("--unlock1")
        fsm.unlock()
        println("--unlock2")
        fsm.unlock()
        println("--unlock3")
        fsm.unlock()
    }
}
