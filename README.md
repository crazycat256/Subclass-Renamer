# Recaf plugin: Subclass Renamer

This plugin allows finding and renaming of classes that extend a specific class. It has options for defining the precise pattern of renaming, and if the renaming should be recursive.
This plugin is made for Recaf 4.x.
Version for Recaf 2.x can be found [here](https://github.com/crazycat256/Subclass-Renamer-recaf-2x)

Note: this plugin is not yet stable and may have problems or errors. Please report any issues you find.

![demo](demo.gif)

## Download: [Here](https://github.com/crazycat256/Subclass-Renamer/releases)

## Building & modification

Once you've downloaded or cloned the repository, you can compile with `mvn clean package`.
This will generate the file `build/libs/subclass-renamer-{VERSION}.jar`. To add your plugin to Recaf:

1. Navigate to the `plugins` folder.
    - Windows: `%APPDATA%/Recaf/plugins`
    - Linux: `$HOME/Recaf/plugins`
2. Copy your plugin jar into this folder
3. Run Recaf to verify your plugin loads.

## Credits
[Col-E](https://github.com/Col-E) for [Recaf](https://github.com/Col-E/Recaf) and [Auto Renamer](https://github.com/Recaf-Plugins/Auto-Renamer).