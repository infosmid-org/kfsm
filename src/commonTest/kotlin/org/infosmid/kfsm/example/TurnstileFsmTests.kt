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
import org.infosmid.kfsm.example.TurnstileEvents.COIN
import org.infosmid.kfsm.example.TurnstileEvents.PASS
import org.infosmid.kfsm.example.TurnstileStates.LOCKED
import org.infosmid.kfsm.example.TurnstileStates.UNLOCKED
import org.infosmid.kfsm.stateMachine
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * @suppress
 */
class TurnstileFsmTests {
    private fun verifyTurnstileFSM(
        fsm: AnyStateMachineInstance<TurnstileStates, TurnstileEvents, Turnstile>,
        turnstile: Turnstile
    ) {

        assertTrue { fsm.currentState == LOCKED }
        assertTrue { turnstile.locked }
        // when
        fsm.sendEvent(COIN)
        // then
        assertTrue { fsm.currentState == UNLOCKED }
        assertTrue { !turnstile.locked }
        // when
        fsm.sendEvent(COIN)
        // then
        assertTrue { fsm.currentState == UNLOCKED }
        assertTrue { !turnstile.locked }
        // when
        fsm.sendEvent(PASS)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { turnstile.locked }
        // when
        fsm.sendEvent(PASS)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { turnstile.locked }
    }

    @Test
    fun turnstilePlain() {
        val builder = AnyStateMachineBuilder<TurnstileStates, TurnstileEvents, Turnstile>(
            TurnstileStates.entries.toSet(),
            TurnstileEvents.entries.toSet()
        )
        builder.initialState { if (locked) LOCKED else UNLOCKED }
        builder.transition(LOCKED, COIN, UNLOCKED) {
            unlock()
        }
        builder.transition(LOCKED, PASS) {
            alarm()
        }
        builder.transition(UNLOCKED, COIN) {
            returnCoin()
        }
        builder.transition(UNLOCKED, PASS, LOCKED) {
            lock()
        }
        builder.complete()
        // when
        val turnstile = Turnstile()
        val definition = builder.complete()
        val fsm = definition.create(turnstile)
        // then
        verifyTurnstileFSM(fsm, turnstile)
    }

    @Test
    fun turnstileDSL() {
        // given
        val definition =
            stateMachine(
                TurnstileStates.entries.toSet(),
                TurnstileEvents.entries.toSet(),
                Turnstile::class
            ) {
                initialState { if (locked) LOCKED else UNLOCKED }
                whenState(LOCKED) {
                    onEvent(COIN to UNLOCKED) {
                        unlock()
                    }
                    onEvent(PASS) {
                        alarm()
                    }
                }
                whenState(UNLOCKED) {
                    onEvent(COIN) {
                        returnCoin()
                    }
                    onEvent(PASS to LOCKED) {
                        lock()
                    }
                }
            }.build()
        // when
        val turnstile = Turnstile()

        val fsm = definition.create(turnstile)
        // then
        verifyTurnstileFSM(fsm, turnstile)
    }

    @Test
    fun simpleTurnstileTest() {
        val definition = stateMachine(
            TurnstileStates.entries.toSet(),
            TurnstileEvents.entries.toSet(),
            Turnstile::class
        ) {
            initialState { if (locked) LOCKED else UNLOCKED }
            whenState(LOCKED) {
                onEntry { startState, targetState, _ ->
                    println("entering:$startState -> $targetState for $this")
                }
                onEvent(COIN to UNLOCKED) {
                    unlock()
                }
                onEvent(PASS) {
                    alarm()
                }
                onExit { startState, targetState, _ ->
                    println("exiting:$startState -> $targetState for $this")
                }
            }
            whenState(UNLOCKED) {
                onEntry { startState, targetState, _ ->
                    println("entering:$startState -> $targetState for $this")
                }
                onEvent(COIN) {
                    returnCoin()
                }
                onEvent(PASS to LOCKED) {
                    lock()
                }
                onExit { startState, targetState, _ ->
                    println("exiting:$startState -> $targetState for $this")
                }
            }
        }.build()

        val turnstile = Turnstile()
        val fsm = definition.create(turnstile)

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.sendEvent(COIN)

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }

        fsm.sendEvent(PASS)

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.sendEvent(PASS)

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.sendEvent(COIN)

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }

        fsm.sendEvent(COIN)

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }
    }

    @Test
    fun fsmComponentTest() {
        val turnstile = Turnstile()
        val fsm = TurnstileFSM(turnstile)
        println("--coin1")
        fsm.coin()
        println("--pass1")
        fsm.pass()
        println("--pass2")
        fsm.pass()
        println("--pass3")
        fsm.pass()
        println("--coin2")
        fsm.coin()
        println("--coin3")
        fsm.coin()
    }
}
