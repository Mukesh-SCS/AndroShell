# AndroShell

AndroShell is an Android-based terminal environment that provides a native shell with built-in commands.  
It allows you to run common Linux commands directly inside an Android application without requiring external binaries.

---

## Features
- Interactive shell with custom prompt
- **30+ built-in commands** implemented in pure Kotlin (no external dependencies)
- PTY (pseudo-terminal) process handling
- Clean Material Design UI
- Simple integration with Android apps

## Built-in Commands

### File Operations
- `ls [-la]` - List directory contents
- `cd [dir]` - Change directory
- `pwd` - Print working directory
- `cat <file>` - Display file contents
- `touch <file>` - Create empty file or update timestamp
- `mkdir [-p] <dir>` - Create directories
- `rm [-rf] <file>` - Remove files/directories
- `cp <src> <dst>` - Copy files
- `mv <src> <dst>` - Move/rename files
- `find [path]` - Find files recursively

### Text Processing
- `echo <text>` - Display text
- `grep <pattern> <file>` - Search in files
- `head [-n lines] <file>` - Show first lines of file
- `tail [-n lines] <file>` - Show last lines of file
- `wc <file>` - Count lines, words, and characters

### System Information
- `uname [-a]` - Show system information
- `whoami` - Show current user
- `date` - Show current date and time
- `env` - Display environment variables
- `export KEY=VALUE` - Set environment variables

### Utilities
- `clear` - Clear the screen
- `help` - Show all available commands
- `exit` - Exit the application

## Installation

1. Clone this repository
2. Open in Android Studio
3. Build and run on your Android device (API 24+)

## Requirements
- Android SDK 24+ (Android 7.0 Nougat or higher)
- Supports ARM64 and x86_64 architectures

## Project Structure
```
app/
├── src/main/
│   ├── java/com/androshell/androshell/
│   │   ├── MainActivity.kt         # Main activity with UI logic
│   │   ├── CommandExecutor.kt      # Command execution engine
│   │   ├── ShellEnvironment.kt     # Environment state management
│   │   └── NativePty.kt            # PTY native bridge
│   └── cpp/
│       └── ptybridge.cpp           # Native PTY implementation
```

## How It Works

AndroShell uses a hybrid approach:
1. **Native PTY layer** (C++) provides pseudo-terminal support
2. **Command Executor** (Kotlin) implements common Unix commands
3. **Shell Environment** (Kotlin) manages working directory and environment variables

Commands are processed in Kotlin using standard Android/Java APIs, providing a lightweight alternative to bundling external binaries like BusyBox.

## License

MIT License - See LICENSE file for details

## Contributing

Contributions are welcome! Feel free to submit issues or pull requests.

## Roadmap
- [ ] Add pipe (`|`) support
- [ ] Add output redirection (`>`, `>>`)
- [ ] Add command history (up/down arrows)
- [ ] Add tab completion
- [ ] Add more text processing commands (sed, awk-like functionality)
- [ ] Add package management commands



