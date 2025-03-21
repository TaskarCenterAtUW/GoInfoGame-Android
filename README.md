
## Project Overview
This document provides an overview of the GoInfoGame Android project, including its structure, key components, and instructions for setting up the development environment.

## Project Structure
The project is organized as follows:

- `app/`: Contains the main application code.
  - `src/main/java/`: Contains the Java/Kotlin source files.
  - `src/main/res/`: Contains the resource files (layouts, strings, images, etc.).
  - `src/main/AndroidManifest.xml`: The Android manifest file.
- `build.gradle`: The Gradle build file for the project.
- `README.md`: Project documentation.

## Key Components
### Activities and Fragments
- **WorkSpaceActivity**: The activity that handles environment selection, user login and workspace list
- **MainActivity**: The main activity that displays maps.
- **MainMapFragment**: A fragment that handles the map and component like quests and geometry.

### Utility Classes
- **QuestPinsManager** : Manages the quest pins on the map.
- **QuestTypeRegistry** : Manages the quest types that should be displayed on the map.

### Network
- **GIGOsmConnection**: Handles network requests and responses to the workspace osm server.
- **WorkspaceApiService**: Ktor service that handles the workspace api requests.

## Setting Up the Development Environment
1. **Install Android Studio**: Download and install the latest version of Android Studio from the [official website](https://developer.android.com/studio).
2. **Clone the Repository**: Clone the project repository from GitHub.
   ```sh
   git clone git@github.com:TaskarCenterAtUW/GoInfoGame-Android.git
   ```
3. **Open the Project**: Open the cloned project in Android Studio.
4. **Sync Gradle**: Allow Android Studio to sync the Gradle files and download the necessary dependencies.
5. **Run the Project**: Use the `Run` button in Android Studio to build and run the project on an emulator or a physical device.

## Deployment
1. **Generate APK**: Build the APK using the following command:
   ```sh
   ./gradlew assembleDebug
   ```
2. **Upload to Play Store**: Follow the instructions on the [Google Play Console](https://play.google.com/console) to upload the APK.

