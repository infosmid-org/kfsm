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

class SecureTurnstileTests {
    @Test
    fun testNormalOperation() {
        // given
        val turnstile = SecureTurnstile()
        val fsm = SecureTurnstileFSM(turnstile)
        // when
        assertTrue { fsm.allowEvent() == setOf("card") }
        assertTrue { turnstile.locked }
        fsm.card(1)
        assertTrue { !turnstile.locked }
        assertTrue { fsm.allowEvent() == setOf("pass", "card") }
        fsm.pass()
        assertTrue { turnstile.locked }
    }

    @Test
    fun testInvalidCard() {
        // given
        val turnstile = SecureTurnstile()
        val fsm = SecureTurnstileFSM(turnstile)
        // when
        assertTrue { fsm.allowEvent() == setOf("card") }
        assertTrue { turnstile.locked }
        fsm.card(2)
        assertTrue { fsm.allowEvent() == setOf("card") }
        assertTrue { turnstile.locked }
        fsm.pass()
        assertTrue { turnstile.locked }
    }

    @Test
    fun testInvalidCardOverride() {
        // given
        val turnstile = SecureTurnstile()
        val fsm = SecureTurnstileFSM(turnstile)
        // when
        assertTrue { turnstile.locked }
        fsm.card(2)
        assertTrue { turnstile.locked }
        fsm.card(42) // override card
        fsm.card(2)
        assertTrue { !turnstile.locked }
        fsm.pass()
        assertTrue { turnstile.locked }
    }

    @Test
    fun testCancelOverrideToLock() {
        // given
        val turnstile = SecureTurnstile()
        val fsm = SecureTurnstileFSM(turnstile)
        // when
        assertTrue { turnstile.locked }
        fsm.card(2)
        assertTrue { turnstile.locked }
        fsm.card(42) // override card
        fsm.card(2)
        assertTrue { !turnstile.locked }
        assertTrue { fsm.allowEvent() == setOf("card", "pass") }
        fsm.card(42) // override card
        assertTrue { turnstile.locked }
    }

    @Test
    fun testCancelOverride() {
        // given
        val turnstile = SecureTurnstile()
        val fsm = SecureTurnstileFSM(turnstile)
        // when
        assertTrue { turnstile.locked }
        fsm.card(2)
        assertTrue { turnstile.locked }
        fsm.card(42) // override card
        assertTrue { turnstile.overrideActive }
        fsm.card(42) // override card
        assertTrue { !turnstile.overrideActive }
        fsm.card(2)
        assertTrue { turnstile.locked }
    }
}
