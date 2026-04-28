package com.animan.wholesalemanager.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.util.*

class BluetoothPrinter {

    private var socket      : BluetoothSocket? = null
    private var outputStream: OutputStream?    = null

    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            socket   = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            Thread.sleep(500)
            outputStream = socket?.outputStream
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Print a text string.
     * NOTE: Does NOT send a RESET (0x1B 0x40) — caller controls init.
     * This prevents mid-job resets when called multiple times in a loop.
     */
    fun print(text: String) {
        try {
            val bytes = text.toByteArray(Charsets.UTF_8)

            val chunkSize = 256  // safe size
            var i = 0

            while (i < bytes.size) {
                val end = (i + chunkSize).coerceAtMost(bytes.size)
                outputStream?.write(bytes.copyOfRange(i, end))
                outputStream?.flush()
                Thread.sleep(50) // VERY IMPORTANT
                i = end
            }

            outputStream?.flush()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Send raw bytes directly — used for ESC/POS image data (GS v 0),
     * or any binary command that must not be re-encoded as a String.
     */
    fun printRaw(bytes: ByteArray) {
        try {
            outputStream?.write(bytes)
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Initialize the printer — call ONCE at the start of a print job.
     * Sends ESC @ (reset to default settings).
     */
    fun initialize() {
        try {
            outputStream?.write(byteArrayOf(0x1B, 0x40))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
    }
}