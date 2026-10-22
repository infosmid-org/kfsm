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

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * @suppress
 */
class PayingTurnstileFsmTests {
    @Test
    fun fsmComponentTest() {
        // tag::test[]
        val fsm = PayingTurnstileFSM(50)
        assertTrue(fsm.turnstile.locked)
        println("External:${fsm.externalState()}")
        println("--coin1")
        fsm.coin(10)
        assertTrue(fsm.turnstile.locked)
        assertTrue(fsm.turnstile.coins == 10)
        println("--coin2")
        println("External:${fsm.externalState()}")
        val externalState = fsm.externalState()
        PayingTurnstileFSM(50, externalState).apply {
            coin(60)
            assertTrue(turnstile.coins == 0)
            assertTrue(!turnstile.locked)
            println("External:${externalState()}")
            println("--pass1")
            pass()
            assertTrue(turnstile.locked)
            println("--pass2")
            pass()
            println("--pass3")
            pass()
            println("--coin3")
            coin(40)
            assertTrue(turnstile.coins == 40)
            println("--coin4")
            coin(10)
            assertTrue(turnstile.coins == 0)
            assertTrue(!turnstile.locked)
        }
        // end::test[]
    }

    @Test
    fun fsmComponentTestExternalState() {
        val fsm = PayingTurnstileFSM(50)
        assertTrue(fsm.turnstile.locked)
        println("External:${fsm.externalState()}")
        println("--coin1")
        fsm.coin(10)
        assertTrue(fsm.turnstile.locked)
        assertTrue(fsm.turnstile.coins == 10)
        println("--coin2")
        val es1 = fsm.externalState()
        println("External:$es1")
        val es2 = PayingTurnstileFSM(50, es1).apply {
            this.coin(60)
            assertTrue(turnstile.coins == 0)
            assertTrue(!turnstile.locked)
            println("External:${this.externalState()}")
            println("--pass1")
        }.externalState()
        val es3 = PayingTurnstileFSM(50, es2).apply {
            this.pass()
            assertTrue(turnstile.locked)
            println("--pass2")
        }.externalState()
        val es4 = PayingTurnstileFSM(50, es3).apply {
            this.pass()
            println("--pass3")
        }.externalState()
        val es5 = PayingTurnstileFSM(50, es4).apply {
            this.pass()
            println("--coin3")
        }.externalState()
        val es6 = PayingTurnstileFSM(50, es5).apply {
            this.coin(40)
            assertTrue(turnstile.coins == 40)
            println("--coin4")
        }.externalState()
        PayingTurnstileFSM(50, es6).apply {
            this.coin(10)
            assertTrue(turnstile.coins == 0)
            assertTrue(!turnstile.locked)
        }
    }
}
