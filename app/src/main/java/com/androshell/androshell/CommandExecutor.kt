package com.androshell.androshell

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Executes built-in shell commands without requiring BusyBox
 */
class CommandExecutor(private val environment: ShellEnvironment) {

    fun execute(cmdLine: String): String {
        val trimmed = cmdLine.trim()
        if (trimmed.isEmpty()) return ""

        val parts = parseCommand(trimmed)
        if (parts.isEmpty()) return ""

        val cmd = parts[0]
        val args = parts.drop(1)

        return try {
            when (cmd) {
                "cd" -> cd(args)
                "pwd" -> pwd()
                "ls" -> ls(args)
                "cat" -> cat(args)
                "echo" -> echo(args)
                "mkdir" -> mkdir(args)
                "rm" -> rm(args)
                "touch" -> touch(args)
                "clear" -> "\u001b[2J\u001b[H"
                "help" -> help()
                "env" -> env()
                "export" -> export(args)
                "date" -> date()
                "whoami" -> whoami()
                "uname" -> uname(args)
                "cp" -> cp(args)
                "mv" -> mv(args)
                "find" -> find(args)
                "grep" -> grep(args)
                "head" -> head(args)
                "tail" -> tail(args)
                "wc" -> wc(args)
                else -> "$cmd: command not found\n"
            }
        } catch (e: Exception) {
            "$cmd: ${e.message}\n"
        }
    }

    private fun parseCommand(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuote = false
        var quoteChar = ' '

        for (c in line) {
            when {
                (c == '"' || c == '\'') && !inQuote -> {
                    inQuote = true
                    quoteChar = c
                }
                c == quoteChar && inQuote -> {
                    inQuote = false
                }
                c.isWhitespace() && !inQuote -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current = StringBuilder()
                    }
                }
                else -> current.append(c)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }

    private fun cd(args: List<String>): String {
        val target = when {
            args.isEmpty() -> environment.getEnv("HOME") ?: "/"
            args[0] == "~" -> environment.getEnv("HOME") ?: "/"
            args[0].startsWith("~/") -> {
                val home = environment.getEnv("HOME") ?: "/"
                args[0].replaceFirst("~", home)
            }
            else -> args[0]
        }

        val newDir = File(if (target.startsWith("/")) target else File(environment.cwd, target).absolutePath)
        return if (newDir.exists() && newDir.isDirectory) {
            environment.cwd = newDir.canonicalPath
            ""
        } else {
            "cd: $target: No such directory\n"
        }
    }

    private fun pwd(): String = "${environment.cwd}\n"

    private fun ls(args: List<String>): String {
        val showHidden = "-a" in args
        val longFormat = "-l" in args
        val paths = args.filter { !it.startsWith("-") }.ifEmpty { listOf(environment.cwd) }

        val output = StringBuilder()
        for (path in paths) {
            val dir = File(if (path.startsWith("/")) path else File(environment.cwd, path).absolutePath)
            if (!dir.exists()) {
                output.append("ls: cannot access '$path': No such file or directory\n")
                continue
            }

            if (dir.isFile) {
                output.append("${dir.name}\n")
                continue
            }

            val files = dir.listFiles()?.filter { showHidden || !it.name.startsWith(".") }
                ?.sortedBy { it.name } ?: emptyList()

            if (longFormat) {
                val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.US)
                for (file in files) {
                    val perms = if (file.isDirectory) "d" else "-"
                    val rwx = if (file.canRead()) "r" else "-"
                    val w = if (file.canWrite()) "w" else "-"
                    val x = if (file.canExecute()) "x" else "-"
                    val size = if (file.isDirectory) "4096" else file.length().toString()
                    val date = dateFormat.format(Date(file.lastModified()))
                    output.append("$perms$rwx$w$x$rwx$w$x$rwx$w$x 1 root root ${size.padStart(8)} $date ${file.name}\n")
                }
            } else {
                for (file in files) {
                    output.append("${file.name}${if (file.isDirectory) "/" else ""}  ")
                }
                if (files.isNotEmpty()) output.append("\n")
            }
        }
        return output.toString()
    }

    private fun cat(args: List<String>): String {
        if (args.isEmpty()) return "cat: missing file operand\n"
        val output = StringBuilder()
        for (path in args) {
            val file = File(if (path.startsWith("/")) path else File(environment.cwd, path).absolutePath)
            if (!file.exists() || !file.isFile) {
                output.append("cat: $path: No such file or directory\n")
            } else {
                output.append(file.readText())
            }
        }
        return output.toString()
    }

    private fun echo(args: List<String>): String = "${args.joinToString(" ")}\n"

    private fun mkdir(args: List<String>): String {
        if (args.isEmpty()) return "mkdir: missing operand\n"
        val recursive = "-p" in args
        val paths = args.filter { !it.startsWith("-") }

        for (path in paths) {
            val dir = File(if (path.startsWith("/")) path else File(environment.cwd, path).absolutePath)
            val success = if (recursive) dir.mkdirs() else dir.mkdir()
            if (!success) return "mkdir: cannot create directory '$path'\n"
        }
        return ""
    }

    private fun rm(args: List<String>): String {
        if (args.isEmpty()) return "rm: missing operand\n"
        val recursive = "-r" in args || "-rf" in args
        val force = "-f" in args || "-rf" in args
        val paths = args.filter { !it.startsWith("-") }

        for (path in paths) {
            val file = File(if (path.startsWith("/")) path else File(environment.cwd, path).absolutePath)
            if (!file.exists() && !force) {
                return "rm: cannot remove '$path': No such file or directory\n"
            }
            if (file.isDirectory && !recursive) {
                return "rm: cannot remove '$path': Is a directory\n"
            }
            if (recursive) file.deleteRecursively() else file.delete()
        }
        return ""
    }

    private fun touch(args: List<String>): String {
        if (args.isEmpty()) return "touch: missing file operand\n"
        for (path in args) {
            val file = File(if (path.startsWith("/")) path else File(environment.cwd, path).absolutePath)
            if (!file.exists()) file.createNewFile()
            else file.setLastModified(System.currentTimeMillis())
        }
        return ""
    }

    private fun help(): String = """
        Built-in commands:
          cd [dir]      - Change directory
          pwd           - Print working directory
          ls [-la]      - List directory contents
          cat <file>    - Display file contents
          echo <text>   - Display text
          mkdir [-p]    - Create directory
          rm [-rf]      - Remove files/directories
          touch <file>  - Create empty file or update timestamp
          cp <src> <dst>- Copy files
          mv <src> <dst>- Move/rename files
          find [path]   - Find files
          grep <pat> <f>- Search in files
          head [-n] <f> - Show first lines of file
          tail [-n] <f> - Show last lines of file
          wc <file>     - Count lines, words, characters
          env           - Show environment variables
          export KEY=VAL- Set environment variable
          date          - Show current date/time
          whoami        - Show current user
          uname [-a]    - Show system information
          clear         - Clear screen
          help          - Show this help
          exit          - Exit shell
        
    """.trimIndent() + "\n"

    private fun env(): String {
        return environment.getAllEnv().entries.joinToString("\n") { "${it.key}=${it.value}" } + "\n"
    }

    private fun export(args: List<String>): String {
        if (args.isEmpty()) return env()
        for (arg in args) {
            val parts = arg.split("=", limit = 2)
            if (parts.size == 2) {
                environment.setEnv(parts[0], parts[1])
            }
        }
        return ""
    }

    private fun date(): String = Date().toString() + "\n"

    private fun whoami(): String = environment.getEnv("USER") ?: "android\n"

    private fun uname(args: List<String>): String {
        return if ("-a" in args) {
            "Android ${android.os.Build.VERSION.RELEASE} ${android.os.Build.DEVICE} ${android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}\n"
        } else {
            "Android\n"
        }
    }

    private fun cp(args: List<String>): String {
        if (args.size < 2) return "cp: missing file operand\n"
        val src = File(if (args[0].startsWith("/")) args[0] else File(environment.cwd, args[0]).absolutePath)
        val dst = File(if (args[1].startsWith("/")) args[1] else File(environment.cwd, args[1]).absolutePath)
        
        if (!src.exists()) return "cp: cannot stat '${args[0]}': No such file or directory\n"
        src.copyTo(dst, overwrite = true)
        return ""
    }

    private fun mv(args: List<String>): String {
        if (args.size < 2) return "mv: missing file operand\n"
        val src = File(if (args[0].startsWith("/")) args[0] else File(environment.cwd, args[0]).absolutePath)
        val dst = File(if (args[1].startsWith("/")) args[1] else File(environment.cwd, args[1]).absolutePath)
        
        if (!src.exists()) return "mv: cannot stat '${args[0]}': No such file or directory\n"
        src.renameTo(dst)
        return ""
    }

    private fun find(args: List<String>): String {
        val startPath = args.firstOrNull() ?: environment.cwd
        val dir = File(if (startPath.startsWith("/")) startPath else File(environment.cwd, startPath).absolutePath)
        if (!dir.exists()) return "find: '$startPath': No such file or directory\n"
        
        val output = StringBuilder()
        dir.walkTopDown().forEach { output.append("${it.absolutePath}\n") }
        return output.toString()
    }

    private fun grep(args: List<String>): String {
        if (args.size < 2) return "grep: missing pattern or file\n"
        val pattern = args[0]
        val output = StringBuilder()
        
        for (i in 1 until args.size) {
            val file = File(if (args[i].startsWith("/")) args[i] else File(environment.cwd, args[i]).absolutePath)
            if (!file.exists() || !file.isFile) continue
            file.readLines().forEach { line ->
                if (line.contains(pattern)) output.append("$line\n")
            }
        }
        return output.toString()
    }

    private fun head(args: List<String>): String {
        var lines = 10
        var fileArgs = args
        if (args.isNotEmpty() && args[0] == "-n" && args.size > 1) {
            lines = args[1].toIntOrNull() ?: 10
            fileArgs = args.drop(2)
        }
        
        if (fileArgs.isEmpty()) return "head: missing file operand\n"
        val file = File(if (fileArgs[0].startsWith("/")) fileArgs[0] else File(environment.cwd, fileArgs[0]).absolutePath)
        if (!file.exists()) return "head: cannot open '${fileArgs[0]}'\n"
        
        return file.readLines().take(lines).joinToString("\n") + "\n"
    }

    private fun tail(args: List<String>): String {
        var lines = 10
        var fileArgs = args
        if (args.isNotEmpty() && args[0] == "-n" && args.size > 1) {
            lines = args[1].toIntOrNull() ?: 10
            fileArgs = args.drop(2)
        }
        
        if (fileArgs.isEmpty()) return "tail: missing file operand\n"
        val file = File(if (fileArgs[0].startsWith("/")) fileArgs[0] else File(environment.cwd, fileArgs[0]).absolutePath)
        if (!file.exists()) return "tail: cannot open '${fileArgs[0]}'\n"
        
        return file.readLines().takeLast(lines).joinToString("\n") + "\n"
    }

    private fun wc(args: List<String>): String {
        if (args.isEmpty()) return "wc: missing file operand\n"
        val file = File(if (args[0].startsWith("/")) args[0] else File(environment.cwd, args[0]).absolutePath)
        if (!file.exists()) return "wc: ${args[0]}: No such file or directory\n"
        
        val lines = file.readLines()
        val lineCount = lines.size
        val wordCount = lines.sumOf { it.split(Regex("\\s+")).size }
        val charCount = file.length()
        
        return "$lineCount $wordCount $charCount ${args[0]}\n"
    }
}
