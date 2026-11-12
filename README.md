# Peakfall

A Java Swing arcade game about a sure-footed mountain goat climbing snowy peaks
and dodging falling rocks.

## Requirements

- Java JDK 14 or newer. Check with `java -version` and `javac -version`.
  If you don't have it, install a JDK such as [Eclipse Temurin](https://adoptium.net).

## Run

### VS Code (macOS or Windows)

Install the **Extension Pack for Java**, open the project folder, and run
`src/App.java` with the **Run** button.

### macOS (Terminal)

```sh
cd path/to/Peakfall
javac -d out src/*.java
java -cp "out:src" App
```

### Windows (Command Prompt or PowerShell)

```bat
cd path\to\Peakfall
javac -d out src\*.java
java -cp "out;src" App
```

The classpath separator is `:` on macOS and `;` on Windows. `src` is on the
classpath so the game can find its images, fonts and audio in `src/assets`.

## Controls

- Arrow keys: move the goat
- Escape: pause
