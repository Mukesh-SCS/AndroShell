package com.androshell.androshell
import android.os.ParcelFileDescriptor

object NativePty {
    init { System.loadLibrary("ptybridge") }

    @JvmStatic external fun startProcess(
        cmd: String, cwd: String, envp: Array<String>
    ): ParcelFileDescriptor?

    @JvmStatic external fun setWindowSize(cols: Int, rows: Int): Int
    @JvmStatic external fun pid(): Int
}
