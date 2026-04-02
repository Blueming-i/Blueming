# Release Guide

## Pre-release Checklist

1. Update `versionCode` and `versionName` in `app/build.gradle`
2. Add release notes to `CHANGELOG.md`
3. Run at least a debug build locally
4. Run a release build before publishing a formal release

## Recommended Git Flow

```powershell
git status
git add .
git commit -m "release: v0.1.0"
git tag v0.1.0
git push origin main
git push origin v0.1.0
```

## Version Rules

- Fix only: `0.1.0 -> 0.1.1`
- New feature: `0.1.0 -> 0.2.0`
- Breaking change: `0.1.0 -> 1.0.0`

## Repository Notes

- Suggested repository name: `guiwu-lite` or `guiwu-asset-android`
- Create a matching Git release for every version tag
- Keep tags for every published version to simplify rollback and comparison
