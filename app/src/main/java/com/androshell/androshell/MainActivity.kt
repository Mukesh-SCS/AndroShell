package com.androshell.androshell

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.androshell.androshell.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.*
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var inS: InputStream? = null
    private var outS: OutputStream? = null
    private var pfd: android.os.ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        startShell()

        b.input.setOnEditorActionListener { v, _, _ ->
            val line = v.text.toString()
            send("$line\n")
            v.text = null
            true
        }
    }

    private fun startShell() {
        val home = File(filesDir, "home").apply { mkdirs() }
        val envp = arrayOf("HOME=${home.absolutePath}",
            "PATH=/system/bin:/system/xbin", "TERM=xterm-256color")

        pfd = NativePty.startProcess("/system/bin/sh", home.absolutePath, envp)
        val fd = pfd?.fileDescriptor ?: error("PTY open failed")
        inS = FileInputStream(fd)
        outS = FileOutputStream(fd)

        scope.launch {
            val buf = ByteArray(4096)
            val dec = Charset.forName("UTF-8")
            while (isActive) {
                val n = inS!!.read(buf); if (n <= 0) break
                val text = String(buf, 0, n, dec)
                withContext(Dispatchers.Main) {
                    b.console.append(text)
                    b.console.post {
                        (b.console.parent as android.widget.ScrollView)
                            .fullScroll(android.view.View.FOCUS_DOWN)
                    }
                }
            }
        }
        send("export PS1='\\u@androshell:\\w\\$ '\n")
    }

    private fun send(s: String) { scope.launch { outS?.write(s.toByteArray()) } }

    override fun onDestroy() {
        try { send("exit\n") } catch (_: Exception) {}
        inS?.close(); outS?.close(); pfd?.close()
        scope.cancel()
        super.onDestroy()
    }


    private fun installBusyBox(): File {
        val rt = File(filesDir, "runtime/bin").apply { mkdirs() }
        val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "x86_64"
        val assetName = if (abi.contains("86")) "bin/busybox-x86_64" else "bin/busybox-aarch64"
        val dst = File(rt, "busybox")
        if (!dst.exists()) {
            assets.open(assetName).use { inp ->
                FileOutputStream(dst).use { out -> inp.copyTo(out) }
            }
            try { android.system.Os.chmod(dst.absolutePath, 0b111_101_101) } // 0755
            catch (_: Throwable) { dst.setExecutable(true, false) }
        }
        return dst
    }

}