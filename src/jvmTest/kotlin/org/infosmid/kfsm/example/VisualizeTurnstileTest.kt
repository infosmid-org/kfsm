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

import org.infosmid.kfsm.viz.Parser
import org.infosmid.kfsm.viz.Visualization
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

class VisualizeTurnstileTest {
    @BeforeTest
    fun setup() {
        val generated = File("generated")
        if (generated.exists() && !generated.isDirectory) {
            error("Expected generated to be a directory")
        } else if (!generated.exists()) {
            generated.mkdirs()
        }
    }

    @Test
    fun produceVisualizationTurnstileFSM() {
        println("== TurnStile")
        val visualisation = Parser.parseStateMachine(
            "TurnstileFSM",
            File("src/commonTest/kotlin/org/infosmid/kfsm/example/TurnstileTypes.kt")
        )
        println(visualisation)
        File("generated", "turnstile.plantuml").writeText(Visualization.plantUml(visualisation))
    }

    @Test
    fun produceVisualizationPayingTurnstile() {
        println("== PayingTurnstile")
        val visualization = Parser.parseStateMachine(
            "PayingTurnstileFSM",
            File("src/commonTest/kotlin/org/infosmid/kfsm/example/PayingTurnstileTypes.kt")
        )
        println(visualization)
        File("generated", "paying-turnstile.plantuml").writeText(Visualization.plantUml(visualization))
    }

    @Test
    fun produceVisualizationSecureTurnstile() {
        println("== SecureTurnstile")
        val visualization = Parser.parseStateMachine(
            "SecureTurnstileFSM",
            File("src/commonTest/kotlin/org/infosmid/kfsm/example/SecureTurnstile.kt")
        )
        println(visualization)
        File("generated", "secure-turnstile.plantuml").writeText(Visualization.plantUml(visualization))
    }

    @Test
    fun produceVisualizationPacketReader() {
        println("== PacketReader")
        val visualization = Parser.parseStateMachine(
            "PacketReaderFSM",
            File("src/commonTest/kotlin/org/infosmid/kfsm/example/PacketReaderTests.kt")
        )
        println(visualization)
        File("generated", "packet-reader.plantuml").writeText(Visualization.plantUml(visualization))
    }
}
