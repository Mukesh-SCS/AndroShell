package com.androshell.androshell

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
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
    private lateinit var shellEnv: ShellEnvironment
    private lateinit var cmdExecutor: CommandExecutor
    private var useBuiltinCommands = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        
        val home = File(filesDir, "home").apply { mkdirs() }
        shellEnv = ShellEnvironment(home)
        cmdExecutor = CommandExecutor(shellEnv)
        
        startShell()

        b.input.setOnEditorActionListener { v, _, _ ->
            val line = v.text.toString().trim()
            if (line.isNotEmpty()) {
                handleCommand(line)
            }
            v.text = null
            true
        }
    }

    private fun startShell() {
        val home = File(filesDir, "home").apply { mkdirs() }
        val envp = arrayOf(
            "HOME=${home.absolutePath}",
            "PATH=/system/bin:/system/xbin", 
            "TERM=xterm-256color",
            "USER=android"
        )

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
        
        // Show welcome message
        appendToConsole("╔════════════════════════════════════╗\n")
        appendToConsole("║     Welcome to AndroShell v1.0     ║\n")
        appendToConsole("╚════════════════════════════════════╝\n")
        appendToConsole("Type 'help' for available commands\n\n")
        appendToConsole(shellEnv.getPrompt())
    }

    private fun handleCommand(cmdLine: String) {
        appendToConsole("$cmdLine\n")
        
        when {
            cmdLine == "exit" -> finish()
            cmdLine == "clear" -> b.console.text = ""
            useBuiltinCommands -> {
                val output = cmdExecutor.execute(cmdLine)
                if (output.isNotEmpty()) {
                    appendToConsole(output)
                }
                shellEnv.updatePwd()
                appendToConsole(shellEnv.getPrompt())
            }
            else -> send("$cmdLine\n")
        }
    }

    private fun appendToConsole(text: String) {
        scope.launch(Dispatchers.Main) {
            b.console.append(text)
            b.console.post {
                (b.console.parent as android.widget.ScrollView)
                    .fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
    }

    private fun send(s: String) { scope.launch { outS?.write(s.toByteArray()) } }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                b.console.text = ""
                appendToConsole(shellEnv.getPrompt())
                true
            }
            R.id.action_help -> {
                handleCommand("help")
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About AndroShell")
            .setMessage("""
                AndroShell v1.0
                
                A lightweight Android terminal with 30+ built-in commands.
                
                Features:
                • Pure Kotlin implementation
                • No external binaries required
                • Native PTY support
                • Material Design UI
                
                Developed by Mukesh-SCS
                
                License: MIT
                GitHub: github.com/Mukesh-SCS/AndroShell
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        try { send("exit\n") } catch (_: Exception) {}
        inS?.close(); outS?.close(); pfd?.close()
        scope.cancel()
        super.onDestroy()
    }
}