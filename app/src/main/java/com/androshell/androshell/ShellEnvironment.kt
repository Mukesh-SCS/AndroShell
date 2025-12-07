package com.androshell.androshell

import java.io.File

/**
 * Manages shell environment state including current directory and environment variables
 */
class ShellEnvironment(initialHome: File) {
    var cwd: String = initialHome.absolutePath
        set(value) {
            field = File(value).canonicalPath
        }

    private val envVars = mutableMapOf<String, String>().apply {
        put("HOME", initialHome.absolutePath)
        put("USER", "android")
        put("SHELL", "/system/bin/sh")
        put("TERM", "xterm-256color")
        put("PATH", "/system/bin:/system/xbin")
        put("PWD", initialHome.absolutePath)
    }

    fun getEnv(key: String): String? = envVars[key]

    fun setEnv(key: String, value: String) {
        envVars[key] = value
        if (key == "PWD") cwd = value
    }

    fun getAllEnv(): Map<String, String> = envVars.toMap()

    fun updatePwd() {
        envVars["PWD"] = cwd
    }

    fun getPrompt(): String {
        val user = getEnv("USER") ?: "android"
        val shortPath = cwd.replace(getEnv("HOME") ?: "", "~")
        return "$user@androshell:$shortPath$ "
    }
}
