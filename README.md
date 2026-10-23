# RT Studio

RT Studio is an Android-first procedural icon design workspace built with Kotlin and Jetpack Compose. It creates icon artwork from geometric primitives and deterministic rules; it does not rely on generative AI.

## Current capabilities

- Live preview with 12 procedural glyphs and 5 backing shapes.
- Per-icon background, foreground and accent palettes, scaling, rotation, corner radius and outline.
- Toggleable artwork layers, undo/redo and automatic local project persistence.
- Additional vector objects: circles, rectangles, triangles, stars, lines and diamonds. Each object has editable position, scale, rotation, opacity, color, visibility, layer order and a transparency-cutout mode.
- Procedural randomization and reusable style recipes.
- PNG and editable SVG export for the current icon.
- Import of an image as a visual reference with safe downsampling for large sources.
- Approximate auto-tracing of a dominant silhouette into an editable SVG path layer for foreground-background separated images. It uses classical thresholding and connected-component detection, not AI; complex shapes and inner holes may need correction.
- APK/ZIP archive inspection for existing icon packs: counts raster assets, detects appfilter.xml, lists resource names and samples colors as a starting point for a new palette.
- Build a pack collection by naming icons and entering optional ComponentInfo mappings; export PNG resources and appfilter.xml in a ZIP.

## Build

The GitHub Actions workflow builds, verifies and uploads a debug APK for each push to main. See the Actions tab for the result and artifact.

## Export boundaries

The pack ZIP is a resource bundle, not an installable icon-pack APK. Launcher compatibility depends on the launcher-specific theme activity, metadata and mapping conventions. Those APK packaging adapters must be implemented and tested against each target launcher before an export can be described as a finished APK. Imported packs are treated as visual references; this project does not copy their application code or automatically trace every imported raster asset to editable vector paths.

## Technical notes

- Minimum Android version: Android 8.0 (API 26).
- Compose-based UI.
- Project data is stored locally in app preferences.
