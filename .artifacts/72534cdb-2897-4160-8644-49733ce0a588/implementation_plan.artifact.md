# Project Setup and Run Plan

The project currently fails to sync because **Gradle 8.11.1** does not support the **Java 25/26** versions currently active on your system and in Android Studio. This plan details how to resolve the compatibility issues and get the app running.

## User Review Required

> [!IMPORTANT]
> You need to install **JDK 17** manually because my attempts to install it via Homebrew failed due to permission issues on your system.

## Proposed Changes

### 1. Environment Setup
The project is configured for Java 17 compatibility (`jvmTarget = "17"`). We need to ensure a compatible JDK is available and selected.

#### [Step] Install JDK 17
Please run the following in your terminal:
```bash
brew install openjdk@17
```
If you encounter permission errors, you may need to fix your Homebrew locks or install it from [Adoptium](https://adoptium.net/temurin/releases/?version=17).

#### [Step] Configure Android Studio JDK
Once installed, set the JDK in Android Studio:
1. Go to **Settings** (or **Settings/Preferences** on macOS).
2. Navigate to **Build, Execution, Deployment > Build Tools > Gradle**.
3. Change **Gradle JDK** to the newly installed JDK 17.

### 2. Emulator Setup
No connected devices or emulators were found.

#### [Step] Create a Virtual Device
1. Open **Device Manager** in Android Studio.
2. Click **Create Device**.
3. Select a device (e.g., **Pixel 8**).
4. Download and select a system image (e.g., **VanillaIceCream** or **UpsideDownCake**).
5. Click **Finish**.

### 3. Verification
Once the environment is ready:
1. **Sync Project**: Click the "Elephant" icon in the top right.
2. **Run App**: Click the "Play" icon.

## Verification Plan

### Automated Tests
- Run `./gradlew test` to verify the `core` module.
- Run `./gradlew assembleDebug` to verify the build.

### Manual Verification
- Verify the app launches on the emulator and shows the `MainActivity`.
