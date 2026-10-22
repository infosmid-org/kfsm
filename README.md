# Kotlin Finite-state machine

This work is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)

This is a small implementation of an FSM in Kotlin.

## Resources
* [Documentation](https://infosmid.org/projects/kfsm/index.html)
* [API Docs](https://infosmid.org/projects/kfsm/javadoc/index.html)
* [Visualization Support org.infosmid.kfsm-viz](https://github.com/infosmid-org/kfsm-viz)
* [Sample Project](https://github.com/infosmid-org/kfsm-samples)
* [Turnstile Sample for Browser](https://github.com/infosmid-org/kfsm-web)
* [Turnstile Sample with KVision for Browser](https://github.com/infosmid-org/kfsm-kvision-web)
* [Turnstile Sample for Android](https://github.com/infosmid-org/kfsm-android)
* [Turnstile Sample for Spring MVC](https://github.com/infosmid-org/kfsm-spring)
* [Turnstile Sample for Spring HATEOAS](https://github.com/infosmid-org/kfsm-spring-rest)
* [Traffic Intersection for Android Compose](https://github.com/infosmid-org/kfsm-android-compose-traffic)

## Getting Started

The state machine implementation supports events triggering transitions from one state to another while performing an optional action as well as entry and exit actions.

## Features
* Event driven state machine.
* External and internal transitions
* State entry and exit actions.
* Default state actions.
* Default entry and exit actions.
* Determine allowed events.
* Multiple state maps with push / pop transitions
* Automatic transitions
* Externalisation of state.
* Typed event parameters and return values.

## Todo
* [x] Multiple state maps
* [x] Push / pop transitions
* [x] Automatic transitions
* [x] Externalisation of state
* [x] Typed event parameters
* [x] Typed event return values
* [x] Simple Visualization
* [x] Detail Visualization
* [x] Gradle Plugin for Visualization
* [x] Timeouts
* [x] Corountines
* [ ] Different type of contexts for Nested statemaps

## Authors
* [corneil](http://github.com/corneil)

## Star History
[![Star History](https://api.star-history.com/svg?repos=infosmid-org/kfsm&type=Date)](https://star-history.com/#infosmid-org/kfsm&Date)

## Quick Tutorial
This is the classic turnstile FSM model from [SMC](http://smc.sourceforge.net/)

### Simple turnstile example
Assume we and to manage the state on a simple lock.
We want to ensure that the `lock()` function is only called when the lock is not locked and we want `unlock()` to be called when locked.

Then we use the DSL to declare a definition of a statemachine matching the diagram:

#### State Diagram

![Turnstile state diagram](src/docs/asciidoc/turnstile-fsm.png)

#### State Table

| Start State | Event | End State | Action |
| --- | --- | --- | --- |
| LOCKED | PASS | LOCKED | alarm |
| LOCKED | COIN | UNLOCKED | unlock |
| UNLOCKED | PASS | LOCKED | lock |
| UNLOCKED | COIN | UNLOCKED | returnCoin |

#### Context class
```kotlin
class Turnstile(var locked: Boolean = true) {
    fun unlock() {
        assert(locked) { "Cannot unlock when not locked" }
        println("Unlock")
        locked = false
    }

    fun lock() {
        assert(!locked) { "Cannot lock when locked" }
        println("Lock")
        locked = true
    }

    fun alarm() {
        println("Alarm")
    }

    fun returnCoin() {
        println("Return coin")
    }
    override fun toString(): String {
        return "Turnstile(locked=$locked)"
    }
}
```

#### Enums for States and Events
We declare 2 enums, one for the possible states and one for the possible events.

```kotlin
enum class TurnstileStates {
    LOCKED,
    UNLOCKED
}

enum class TurnstileEvents {
    COIN,
    PASS
}
```

#### Packaged definition and execution
```kotlin
class TurnstileFSM(turnstile: Turnstile) {
    private val fsm = definition.create(turnstile)

    fun coin() = fsm.sendEvent(TurnstileEvents.COIN)
    fun pass() = fsm.sendEvent(TurnstileEvents.PASS)
    companion object {
        private val definition = stateMachine(
            TurnstileStates.values().toSet(),
            TurnstileEvents::class,
            Turnstile::class
        ) {
            initialState {
            if (locked)
                TurnstileStates.LOCKED
            else
                TurnstileStates.UNLOCKED
            }
            default {
                onEntry { startState, targetState, _ ->
                    println("entering:$startState -> $targetState for $this")
                }
                // default transition will invoke alarm
                action { state, event, _ ->
                    println("Default action for state($state) -> event($event) for $this")
                    alarm()
                }
                onExit { startState, targetState, _ ->
                    println("exiting:$startState -> $targetState for $this")
                }
            }
            // when current state is LOCKED
            whenState(TurnstileStates.LOCKED) {
                // external transition on COIN to UNLOCKED state
                onEvent(TurnstileEvents.COIN to TurnstileStates.UNLOCKED) {
                    unlock()
                }
            }
            // when current state is UNLOCKED
            whenState(TurnstileStates.UNLOCKED) {
                // internal transition on COIN
                onEvent(TurnstileEvents.COIN) {
                    returnCoin()
                }
                // external transition on PASS to LOCKED state
                onEvent(TurnstileEvents.PASS to TurnstileStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }
}
```

With this definition we are saying:
When the state is `LOCKED` and on a `COIN` event then transition to `UNLOCKED` and execute the lambda which is treated
as a member of the context `{ unlock() }`

When the state is `LOCKED` and on event `PASS` we perform the action `alarm()` without changing the end state.

#### Usage
Then we instantiate the FSM and provide a context to operate on:

```kotlin
val turnstile = Turnstile()
val fsm = TurnstileFSM(turnstile)
```

Now we have a context that is independent of the FSM.

Sending events may invoke actions:
```kotlin
// State state is LOCKED
fsm.coin()
// Expect unlock action end state is UNLOCKED
fsm.pass()
// Expect lock() action and end state is LOCKED
fsm.pass()
// Expect alarm() action and end state is LOCKED
fsm.coin()
// Expect unlock() and end state is UNLOCKED
fsm.coin()
// Expect returnCoin() and end state is UNLOCKED
```

This model means the FSM can be instantiated as needed if the context has values that represent the state. The idea is that the context will properly maintain it's internal state.

The FSM can derive the formal state from the value(s) of properties of the context.

The [Documentation](https://infosmid.org/projects/kfsm/index.html) contains more detail on creating finite state machine implementations.

The documentation contains examples for:

* [Turnstile providing for coin values.](https://infosmid.org/projects/kfsm/index.html#advanced-features)
* [Secure turnstile with card and override.](https://infosmid.org/projects/kfsm/index.html#secure-turnstile-example)
* [Packet Reader example.](https://infosmid.org/projects/kfsm/index.html#packet-reader-example)
* [ImmutableLock and FSM.](https://infosmid.org/projects/kfsm/index.html#immutable-context-example)

### Repository

Use this repository for SNAPSHOT builds. Releases are on Maven Central
```groovy
repositories {
    maven {
        url 'https://oss.sonatype.org/content/groups/public'
    }
}
```
### Dependencies

#### KMP Projects

The dependency used in common modules.

```groovy
dependencies {
    implementation 'org.infosmid:kfsm:2.0.0'
}
```

#### JVM Projects

```groovy
dependencies {
    implementation 'org.infosmid:kfsm-jvm:2.0.0'
}
```

#### KotlinJS Projects

```groovy
dependencies {
    implementation 'org.infosmid:kfsm-js:2.0.0'
}
```

#### Kotlin/Native Projects using LinuxX64

```groovy
dependencies {
    implementation 'org.infosmid:kfsm-linuxX64:2.0.0'
}
```

#### Kotlin/Native Projects using MinGW64

```groovy
dependencies {
    implementation 'org.infosmid:kfsm-mingwX64:2.0.0'
}
```

#### Kotlin/Native Projects using macOS

```groovy
dependencies {
    implementation 'org.infosmid:kfsm-macosX64:2.0.0'
}
```

## Visualisation

For more information about visualization options use [org.infosmid:kfsm-viz](https://github.com/infosmid-org/kfsm-viz)


### Plantuml Examples

#### Turnstile FSM

[TurnstileTypes.kt](./src/commonTest/kotlin/org/infosmidkfsm/example/TurnstileTypes.kt)

![](turnstile.png)

#### Paying Turnstile FSM

[PayingTurnstileTypes.kt](./src/commonTest/kotlin/org/infosmidkfsm/example/PayingTurnstileTypes.kt)

![](paying_turnstile.png)

#### Secure Turnstile FSM

[SecureTurnstile.kt](./src/commonTest/kotlin/org/infosmidkfsm/example/SecureTurnstile.kt)

![](secure_turnstile.png)

#### Packer Reader FSM

[PacketReaderTests.kt](./src/jvmTest/kotlin/org/infosmidkfsm/example/PacketReaderTests.kt)

![](packet_reader.png)


## Questions:
* Should entry / exit actions receive state or event as arguments?
* Should default actions receive state or event as arguments?
* Is there a more elegant way to define States and Events using sealed classes?
* Are any features missing from the implementation?
