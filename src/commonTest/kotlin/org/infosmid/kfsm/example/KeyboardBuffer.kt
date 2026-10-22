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

import org.infosmid.kfsm.stateMachine
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Copyright (c) 2020-2021. Open JumpCO
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

enum class KeyboardBufferStates {
    DEFAULT,
    CAPS_LOCKED
}

enum class KeyboardEvent {
    CAPS_LOCK,
    ANY_KEY
}

class KeyboardBuffer {
    private val buffer: MutableList<Char> = mutableListOf()
    fun add(ch: Char) {
        buffer.add(ch)
    }

    fun addUpperCase(ch: Char) {
        buffer.add(ch.uppercaseChar())
    }

    fun read(): Char = buffer.removeAt(0)
}

class KeyboardBufferFSM(context: KeyboardBuffer) {
    companion object {
        val definition = stateMachine(
            KeyboardBufferStates.entries.toSet(),
            KeyboardEvent.entries.toSet(),
            KeyboardBuffer::class,
            Char::class
        ) {
            initialState { KeyboardBufferStates.DEFAULT }
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            whenState(KeyboardBufferStates.DEFAULT) {
                onEvent(KeyboardEvent.ANY_KEY) { input ->
                    requireNotNull(input) { "input is required" }
                    add(input)
                }
                onEvent(KeyboardEvent.CAPS_LOCK to KeyboardBufferStates.CAPS_LOCKED) {}
            }
            whenState(KeyboardBufferStates.CAPS_LOCKED) {
                onEvent(KeyboardEvent.ANY_KEY) { input ->
                    requireNotNull(input) { "input is required" }
                    addUpperCase(input)
                }
                onEvent(KeyboardEvent.CAPS_LOCK to KeyboardBufferStates.DEFAULT) {}
            }
        }.build()
    }

    private val fsm = definition.create(context)

    fun capsLock() = fsm.sendEvent(KeyboardEvent.CAPS_LOCK)
    fun anyKey(ch: Char) = fsm.sendEvent(KeyboardEvent.ANY_KEY, ch)
}

class KeyboardBufferTest {
    @Test
    fun testFSM() {
        val buffer = KeyboardBuffer()
        val fsm = KeyboardBufferFSM(buffer)
        fsm.anyKey('A')
        assertEquals('A', buffer.read())
        fsm.anyKey('a')
        assertEquals('a', buffer.read())
        fsm.capsLock()
        fsm.anyKey('B')
        assertEquals('B', buffer.read())
        fsm.anyKey('b')
        assertEquals('B', buffer.read())
        fsm.capsLock()
        fsm.anyKey('a')
        assertEquals('a', buffer.read())
    }
}
