package com.animan.wholesalemanager.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.util.*

class BluetoothPrinter {

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()

            Thread.sleep(500)
            outputStream = socket?.outputStream
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun print(text: String) {
        try {
            // Initialize printer
            outputStream?.write(byteArrayOf(0x1B, 0x40))

            // Print text
            outputStream?.write(text.toByteArray())

            // Feed lines
            outputStream?.write("\n\n\n".toByteArray())

            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        outputStream?.close()
        socket?.close()
    }
}