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

import org.infosmid.kfsm.ExternalState
import org.infosmid.kfsm.stateMachine

/**
 * @suppress
 */
// tag::context[]
class PayingTurnstile(
    val requiredCoins: Int,
    locked: Boolean = true,
    coins: Int = 0
) {
    var coins: Int = coins
        private set
    var locked: Boolean = locked
        private set

    fun unlock() {
        require(locked) { "Cannot unlock when not locked" }
        require(coins >= requiredCoins) { "Not enough coins. ${requiredCoins - coins} required" }
        println("Unlock")
        locked = false
    }

    fun lock() {
        require(!locked) { "Cannot lock when locked" }
        require(coins == 0) { "Coins $coins must be returned" }
        println("Lock")
        locked = true
    }

    fun alarm() {
        println("Alarm")
    }

    fun coin(value: Int): Int {
        coins += value
        println("Coin received=$value, Total=$coins")
        return coins
    }

    fun returnCoin(returnCoins: Int) {
        println("Return Coin:$returnCoins")
        coins -= returnCoins
    }

    fun reset() {
        coins = 0
        println("Reset coins=$coins")
    }

    override fun toString(): String {
        return "Turnstile(locked=$locked,coins=$coins)"
    }
}
// end::context[]
/**
 * @suppress
 */
// tag::states-events[]
enum class PayingTurnstileStates {
    LOCKED,
    COINS,
    UNLOCKED
}

/**
 * @suppress
 */
enum class PayingTurnstileEvents {
    COIN,
    PASS
}

// end::states-events[]

/**
 * @suppress
 */
// tag::packaged[]
data class PayingTurnstileFSMExternalState(
    val coins: Int,
    val locked: Boolean,
    val initialState: ExternalState<PayingTurnstileStates>
)

class PayingTurnstileFSM(
    requiredCoins: Int,
    savedState: PayingTurnstileFSMExternalState? = null
) {
    // not private for visibility for tests
    val turnstile: PayingTurnstile = PayingTurnstile(requiredCoins, savedState?.locked ?: true, savedState?.coins ?: 0)
    val fsm = if (savedState != null) {
        definition.create(turnstile, savedState.initialState)
    } else {
        definition.create(turnstile)
    }

    fun coin(value: Int) {
        println("sendEvent:COIN:$value")
        fsm.sendEvent(PayingTurnstileEvents.COIN, value)
    }

    fun pass() {
        println("sendEvent:PASS")
        fsm.sendEvent(PayingTurnstileEvents.PASS)
    }

    fun allowedEvents() = fsm.allowed().map { it.name.lowercase() }.toSet()
    fun externalState(): PayingTurnstileFSMExternalState {
        return PayingTurnstileFSMExternalState(turnstile.coins, turnstile.locked, fsm.externalState())
    }

    companion object {
        val definition = stateMachine(
            setOf(PayingTurnstileStates.LOCKED, PayingTurnstileStates.UNLOCKED),
            PayingTurnstileEvents.entries.toSet(),
            PayingTurnstile::class,
            Int::class
        ) {
            defaultInitialState = PayingTurnstileStates.LOCKED
            invariant("negative coins") { coins >= 0 }
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            invariant("negative coins") { coins >= 0 }
            default {
                onEntry { _, targetState, arg ->
                    if (arg != null) {
                        println("entering:$targetState ($arg) for $this")
                    } else {
                        println("entering:$targetState for $this")
                    }
                }
                action { state, event, arg ->
                    if (arg != null) {
                        println("Default action for state($state) -> on($event, $arg) for $this")
                    } else {
                        println("Default action for state($state) -> on($event) for $this")
                    }
                    alarm()
                }
                onExit { startState, _, arg ->
                    if (arg != null) {
                        println("exiting:$startState ($arg) for $this")
                    } else {
                        println("exiting:$startState for $this")
                    }
                }
            }
            stateMap("coins", setOf(PayingTurnstileStates.COINS)) {
                whenState(PayingTurnstileStates.COINS) {
                    automaticPop(PayingTurnstileStates.UNLOCKED, guard = { coins > requiredCoins }) {
                        println("automaticPop:returnCoin")
                        returnCoin(coins - requiredCoins)
                        unlock()
                        reset()
                    }
                    automaticPop(PayingTurnstileStates.UNLOCKED, guard = { coins == requiredCoins }) {
                        println("automaticPop")
                        unlock()
                        reset()
                    }
                    onEvent(PayingTurnstileEvents.COIN) { value ->
                        requireNotNull(value) { "argument required for COIN" }
                        coin(value)
                        println("Coins=$coins")
                        if (coins < requiredCoins) {
                            println("Please add ${requiredCoins - coins}")
                        }
                    }
                }
            }
            whenState(PayingTurnstileStates.LOCKED) {
                // The coin brings amount to exact amount
                onEventPush(PayingTurnstileEvents.COIN, "coins", PayingTurnstileStates.COINS) { value ->
                    requireNotNull(value) { "argument required for COIN" }
                    coin(value)
                    unlock()
                    reset()
                }
                // The coins add up to more than required
                onEventPush(
                    PayingTurnstileEvents.COIN,
                    "coins",
                    PayingTurnstileStates.COINS,
                    guard = { value ->
                        requireNotNull(value) { "argument required for COIN" }
                        value + coins < requiredCoins
                    }
                ) { value ->
                    requireNotNull(value) { "argument required for COIN" }
                    println("PUSH TRANSITION")
                    coin(value)
                    println("Coins=$coins, Please add ${requiredCoins - coins}")
                }
            }
            whenState(PayingTurnstileStates.UNLOCKED) {
                onEvent(PayingTurnstileEvents.COIN) { value ->
                    requireNotNull(value) { "argument required for COIN" }
                    returnCoin(coin(value))
                }
                onEvent(PayingTurnstileEvents.PASS to PayingTurnstileStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }
}
// end::packaged[]
