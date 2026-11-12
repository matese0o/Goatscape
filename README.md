# Peakfall

<img src="src/assets/icon.png" alt="Peakfall icon" width="128" align="right">

A pixel-art Java Swing arcade game. Guide a sure-footed mountain goat up a
snowy cliff and dodge the boulders tumbling down from the peak.

- Opens as a full window, and the cliff stretches to fit any window size.
- More rocks fall on wider cliffs, so the game is just as hard at any size.
- Comes with its own Peakfall icon for the macOS Dock and the Windows taskbar.

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
java -Xdock:icon=src/assets/icon.png -Xdock:name=Peakfall -cp "out:src" App
```

The `-Xdock` options show the Peakfall icon and name in the Dock as soon as
the game starts. Without them, the game still runs, but the Java icon shows
for a moment before the Peakfall icon replaces it.

### Windows (Command Prompt or PowerShell)

```bat
cd path\to\Peakfall
javac -d out src\*.java
java -cp "out;src" App
```

The classpath separator is `:` on macOS and `;` on Windows. `src` is on the
classpath so the game can find its images, fonts and audio in `src/assets`.

You only need to run `javac` again after changing the code.

## Controls

| Key | Action |
| --- | --- |
| Arrow keys | Move the goat |
| Esc | Pause |
| Green window button or Ctrl+Cmd+F (macOS) | Toggle full screen |
| Maximize button (Windows) | Fill the screen |

## Project layout

| Path | Contents |
| --- | --- |
| `src/App.java` | Entry point |
| `src/GameFrame.java` | Window setup, full-window mode and app icon |
| `src/MenuPanel.java` | Title screen |
| `src/GamePanel.java` | Gameplay loop, pause and game-over screens |
| `src/MountainArt.java` | Pixel-art renderer for the sky, mountains, cliff and goat |
| `src/Player.java`, `src/Rock.java` | The goat and the falling boulders |
| `src/MusicManager.java` | Music and sound effects |
| `src/assets/` | Icon, buttons, fonts and audio |
