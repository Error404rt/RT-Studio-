# RT•Studio

RT•Studio is an Android icon creation and icon-pack editing studio.

## Beta 0.1

The first beta foundation includes:
- Kotlin and Jetpack Compose
- Material 3-based adaptive light/dark color schemes
- Three icon-workflow entry points
- Real image import through Android's document picker
- Preview of the selected bitmap
- GitHub Actions workflow to build and upload a debug APK

This is an early beta foundation, not the complete product described in the project specification. Icon-pack APK parsing, background removal, vector path editing, project persistence, AI providers and icon-pack APK building are not implemented yet.

## Build

The GitHub Actions workflow **Android Beta APK** runs on pushes to `main` and can also be started manually. It uploads `RT-Studio-beta-debug` when the build succeeds.
