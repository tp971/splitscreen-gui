# splitscreen-gui

SplitScreen is a program made to compare speedruns:
it allows you to split video recordings into segments and compare them side-by-side.

## Installation

### Windows

Download the latest release for Windows under [releases](https://github.com/tp971/splitscreen-gui/releases),
extract the folder "SplitScreen" from the zip-file to any location and start SplitScreen.exe.

### Linux

TODO

## Development

### Dependencies

* [splitscreen-cli](https://github.com/tp971/splitscreen-cli) and its dependencies
* Java 17
* gstreamer 1.8 or higher

TODO how to install dependencies

### Building

1. Build or install [splitscreen-cli](https://github.com/tp971/splitscreen-cli).
2. Run the project using `./gradlew run` or build a jar using `./gradlew shadowJar`, which creates a jar-file in `build/libs/`.

splitscreen-gui searches for splitscreen-cli in the directory of the jar-file, the current working directory and PATH.
For development, you can create a symlink to splitscreen-cli in the project's directory.
