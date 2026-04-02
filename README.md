# GuiWuLite

`GuiWuLite` is an Android prototype for personal asset tracking. It focuses on the core MVP flow first: record assets, manage categories, review stats, and keep a wishlist for future purchases.

## Version

- Version: `0.1.0`
- Status: `Prototype / MVP`
- Platform: `Android`
- Min SDK: `21`
- Target SDK: `31`

## Features

- Add assets with name, category, price, purchase date, extra cost, note, and image
- Edit existing assets from the main list
- Delete assets from the edit page or by long press on the home list
- Manage custom categories
- Filter assets by status, category, sort mode, and keyword
- View stats by week, month, year, or all time
- Track wishlist items and target amounts
- Persist data locally with `SharedPreferences + Gson`

## Project Structure

- `app/src/main/java/com/example/guiwulite/MainActivity.kt`: home page, filters, stats, wishlist
- `app/src/main/java/com/example/guiwulite/AddAssetActivity.kt`: add and edit asset flow
- `app/src/main/java/com/example/guiwulite/CategoryManagementActivity.kt`: category management
- `app/src/main/java/com/example/guiwulite/data/AssetRepository.kt`: local persistence
- `app/src/main/java/com/example/guiwulite/data/AssetModels.kt`: models
- `app/src/main/java/com/example/guiwulite/ui/`: adapters and chart view

## Run Locally

1. Open `GuiWuLite` in Android Studio
2. Wait for Gradle sync to finish
3. Connect an emulator or Android device
4. Run the `app` module

If Gradle wrapper files are available, Windows command line build is:

```powershell
.\gradlew.bat assembleDebug
```

## Build Output

Debug APK:

```powershell
.\gradlew.bat assembleDebug
```

Release APK:

```powershell
.\gradlew.bat assembleRelease
```

Default output folders:

- `app/build/outputs/apk/debug/`
- `app/build/outputs/apk/release/`

## Versioning

This project uses semantic versioning:

- `MAJOR`: breaking changes
- `MINOR`: new backward-compatible features
- `PATCH`: backward-compatible fixes

Update these files for every release:

- `app/build.gradle`
- `CHANGELOG.md`

## Roadmap

- Cloud sync and account support
- Better image handling and OCR-assisted input
- Maintenance, warranty, and lifecycle tracking
- Export and backup support
- Richer dashboard and analytics
