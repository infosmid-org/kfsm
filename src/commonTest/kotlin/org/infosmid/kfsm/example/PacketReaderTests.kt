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

import org.infosmid.dbc.preconditionNotNull
import org.infosmid.kfsm.stateMachine


import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// tag::context[]
class Block {
    private val byteArray = mutableListOf<Byte>()
    fun addByte(byte: Int) {
        byteArray.add(byte.toByte())
    }
    fun toByteArray(): ByteArray = byteArray.toByteArray()
    fun size() = byteArray.size
}

interface ProtocolHandler {
    fun sendNACK()
    fun sendACK()
}

interface PacketHandler : ProtocolHandler {
    val checksumValid: Boolean
    fun print()
    fun addField()
    fun endField()
    fun addByte(byte: Int)
    fun addChecksum(byte: Int)
    fun checksum()
}

class ProtocolSender : ProtocolHandler {
    override fun sendNACK() {
        println("NACK")
    }

    override fun sendACK() {
        println("ACK")
    }
}

class Packet(private val protocolHandler: ProtocolHandler) :
    PacketHandler,
    ProtocolHandler by protocolHandler {
    val fields = mutableListOf<ByteArray>()
    private var currentField: Block? = null
    private var _checksumValid: Boolean = false

    override val checksumValid: Boolean
        get() = _checksumValid
    private val checkSum = Block()

    override fun print() {
        println("Checksum:$checksumValid:Fields:${fields.size}")
        fields.forEachIndexed { index, bytes ->
            print("FLD:$index:")
            bytes.forEach { byte ->
                val hex = byte.toString(16).padStart(2, '0')
                print(" $hex")
            }
            println()
        }
        println()
    }

    override fun addField() {
        currentField = Block()
    }

    override fun endField() {
        val field = currentField
        requireNotNull(field) { "expected currentField to have a value" }
        fields.add(field.toByteArray())
        currentField = null
    }

    override fun addByte(byte: Int) {
        val field = currentField
        preconditionNotNull(field) { "expected currentField to have a value" }
        field.addByte(byte)
    }

    override fun addChecksum(byte: Int) {
        checkSum.addByte(byte)
    }

    override fun checksum() {
        require(checkSum.size() > 0)
        val checksumBytes = checkSum.toByteArray()
        _checksumValid = if (checksumBytes.size == fields.size) {
            checksumBytes.mapIndexed { index, cs ->
                cs == fields[index][0]
            }.reduce { a, b -> a && b }
        } else {
            false
        }
    }
}

// end::context[]
// tag::states-events[]
class CharacterConstants {
    companion object {
        const val SOH = 0x01
        const val STX = 0x02
        const val ETX = 0x03
        const val EOT = 0x04
        const val ACK = 0x06
        const val NAK = 0x15
        const val ESC = 0x1b
    }
}

/**
 * CTRL :
 * BYTE everything else
 */
enum class ReaderEvents {
    BYTE, // everything else
    CTRL, // SOH, EOT, STX, ETX, ACK, NAK
    ESC // ESC = 0x1B
}

enum class ReaderStates {
    START,
    RCVPCKT,
    RCVDATA,
    RCVESC,
    RCVCHK,
    RCVCHKESC,
    CHKSUM,
    END
}

// end::states-events[]
// tag::packaged[]
class PacketReaderFSM(packetHandler: PacketHandler) {
    companion object {

        val definition = stateMachine(
            ReaderStates.entries.toSet(),
            ReaderEvents.entries.toSet(),
            PacketHandler::class,
            Int::class
        ) {
            defaultInitialState = ReaderStates.START
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            default {
                onEvent(ReaderEvents.BYTE to ReaderStates.END) {
                    sendNACK()
                }
                onEvent(ReaderEvents.CTRL to ReaderStates.END) {
                    sendNACK()
                }
                onEvent(ReaderEvents.ESC to ReaderStates.END) {
                    sendNACK()
                }
            }
            whenState(ReaderStates.START) {
                onEvent(
                    ReaderEvents.CTRL to ReaderStates.RCVPCKT,
                    guard = { byte -> byte == CharacterConstants.SOH }
                ) {}
            }
            whenState(ReaderStates.RCVPCKT) {
                onEvent(ReaderEvents.CTRL to ReaderStates.RCVDATA, guard = { byte -> byte == CharacterConstants.STX }) {
                    addField()
                }
                onEvent(ReaderEvents.BYTE to ReaderStates.RCVCHK) { byte ->
                    requireNotNull(byte)
                    addChecksum(byte)
                }
            }
            whenState(ReaderStates.RCVDATA) {
                onEvent(ReaderEvents.BYTE) { byte ->
                    requireNotNull(byte)
                    addByte(byte)
                }
                onEvent(ReaderEvents.CTRL to ReaderStates.RCVPCKT, guard = { byte -> byte == CharacterConstants.ETX }) {
                    endField()
                }
                onEvent(ReaderEvents.ESC to ReaderStates.RCVESC) {}
            }
            whenState(ReaderStates.RCVESC) {
                onEvent(ReaderEvents.ESC to ReaderStates.RCVDATA) {
                    addByte(CharacterConstants.ESC)
                }
                onEvent(ReaderEvents.CTRL to ReaderStates.RCVDATA) { byte ->
                    requireNotNull(byte)
                    addByte(byte)
                }
            }
            whenState(ReaderStates.RCVCHK) {
                onEvent(ReaderEvents.BYTE) { byte ->
                    requireNotNull(byte)
                    addChecksum(byte)
                }
                onEvent(ReaderEvents.ESC to ReaderStates.RCVCHKESC) {}
                onEvent(ReaderEvents.CTRL to ReaderStates.CHKSUM, guard = { byte -> byte == CharacterConstants.EOT }) {
                    checksum()
                }
            }
            whenState(ReaderStates.CHKSUM) {
                automatic(ReaderStates.END, guard = { !checksumValid }) {
                    sendNACK()
                }
                automatic(ReaderStates.END, guard = { checksumValid }) {
                    sendACK()
                }
            }
            whenState(ReaderStates.RCVCHKESC) {
                onEvent(ReaderEvents.ESC to ReaderStates.RCVCHK) {
                    addChecksum(CharacterConstants.ESC)
                }
                onEvent(ReaderEvents.CTRL to ReaderStates.RCVCHK) { byte ->
                    requireNotNull(byte)
                    addChecksum(byte)
                }
            }
        }.build()
    }

    private val fsm = definition.create(packetHandler)
    fun receiveByte(byte: Int) {
        when (byte) {
            CharacterConstants.ESC -> fsm.sendEvent(ReaderEvents.ESC, CharacterConstants.ESC)
            CharacterConstants.SOH,
            CharacterConstants.EOT,
            CharacterConstants.ETX,
            CharacterConstants.STX,
            CharacterConstants.ACK,
            CharacterConstants.NAK -> fsm.sendEvent(ReaderEvents.CTRL, byte)

            else -> fsm.sendEvent(ReaderEvents.BYTE, byte)
        }
    }
}
// end::packaged[]

// tag::tests[]
class PacketReaderTests {
    @Test
    fun testReaderExpectACK() {
        val protocolHandler = ProtocolSender()
        val packetReader = Packet(protocolHandler)
        val fsm = PacketReaderFSM(packetReader)
        val stream =
            listOf(
                CharacterConstants.SOH,
                CharacterConstants.STX,
                'A'.code,
                'B'.code,
                'C'.code,
                CharacterConstants.ETX,
                'A'.code,
                CharacterConstants.EOT
            )
        stream.forEach { byte ->
            fsm.receiveByte(byte)
        }
        packetReader.print()
        protocolHandler.sendACK()
        assertTrue { packetReader.checksumValid }
    }

    @Test
    fun testReaderESCExpectACK() {
        val protocolHandler = ProtocolSender()
        val packetReader = Packet(protocolHandler)
        val fsm = PacketReaderFSM(packetReader)
        val stream =
            listOf(
                CharacterConstants.SOH,
                CharacterConstants.STX,
                'A'.code,
                CharacterConstants.ESC,
                CharacterConstants.EOT,
                'C'.code,
                CharacterConstants.ETX,
                'A'.code,
                CharacterConstants.EOT
            )
        stream.forEach { byte ->
            fsm.receiveByte(byte)
        }
        packetReader.print()

        assertTrue { packetReader.checksumValid }
        assertTrue { packetReader.fields.size == 1 }
        assertTrue { packetReader.fields[0].size == 3 }
        assertTrue { packetReader.fields[0][1].toInt() == CharacterConstants.EOT }
    }

    @Test
    fun testReaderESCExpectNACK() {
        val protocolHandler = ProtocolSender()

        val packetReader = Packet(protocolHandler)
        val fsm = PacketReaderFSM(packetReader)
        val stream =
            listOf(
                CharacterConstants.SOH,
                CharacterConstants.STX,
                'A'.code,
                CharacterConstants.ESC,
                'C'.code,
                CharacterConstants.ETX,
                'A'.code,
                CharacterConstants.EOT
            )
        stream.forEach { byte ->
            fsm.receiveByte(byte)
        }
        packetReader.print()

        assertFalse { packetReader.checksumValid }
        assertTrue { packetReader.fields.isEmpty() }
    }

    @Test
    fun testReaderExpectNACK() {
        val protocolHandler = ProtocolSender()

        val packetReader = Packet(protocolHandler)
        val fsm = PacketReaderFSM(packetReader)
        val stream =
            listOf(
                CharacterConstants.SOH,
                CharacterConstants.STX,
                'A'.code,
                'B'.code,
                'C'.code,
                CharacterConstants.ETX,
                'B'.code,
                CharacterConstants.EOT
            )
        stream.forEach { byte ->
            fsm.receiveByte(byte)
        }
        packetReader.print()

        assertFalse { packetReader.checksumValid }
    }

    @Test
    fun testReaderMultipleFieldsExpectACK() {
        val protocolHandler = ProtocolSender()
        val packetReader = Packet(protocolHandler)
        val fsm = PacketReaderFSM(packetReader)
        val stream =
            listOf(
                CharacterConstants.SOH,
                CharacterConstants.STX,
                'A'.code,
                'B'.code,
                'C'.code,
                CharacterConstants.ETX,
                CharacterConstants.STX,
                'D'.code,
                'E'.code,
                'F'.code,
                CharacterConstants.ETX,
                'A'.code,
                'D'.code,
                CharacterConstants.EOT
            )
        stream.forEach { byte ->
            fsm.receiveByte(byte)
        }
        packetReader.print()

        assertTrue { packetReader.checksumValid }
        assertTrue { packetReader.fields.size == 2 }
    }
}
// end::tests[]
