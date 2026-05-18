# enroll-capacitor-neo — Linkage & Sync Guide

## What This Repo Is

**Capacitor plugin** for the **enroll-neo** product line. Wraps the eNROLL Lite Android SDK (without Innovatrics) for Ionic/Capacitor apps.

## Product Line

**enroll-neo** — lightweight eKYC without Innovatrics dependency.

## Native SDK Dependency

| Field | Value |
|---|---|
| Branch | `development-lumin-sdk` |
| Artifact | `com.github.LuminSoft:eNROLL-Lite-Android` |
| Current Version | `v1.2.6` |
| Declared in | `android/build.gradle` |
| iOS Distribution | XCFramework (`ios/Frameworks/EnrollFramework.xcframework`) |
| iOS Core Pod | `EnrollNeoCore 1.0.13` (should be 1.0.17) |

> **WARNING**: SDK version is OUTDATED. Flutter Neo plugin is at v1.3.2.

## Sibling Projects (same product line)

| Plugin | Path | Type |
|---|---|---|
| enroll_neo_plugin | `/Users/luminsoft/StudioProjects/enroll_neo_plugin` | Flutter |
| enroll-neo-react-native | `/Users/luminsoft/StudioProjects/enroll-neo-react-native` | React Native |

## What This Plugin Exposes

- `startEnroll(options)` — launches the enrollment flow
- `addListener('onRequestId', callback)` — mid-flow event
- `removeAllListeners()` — cleanup
- Modes: onboarding, auth, update, signContract, forgetProfileData
- Colors: `enrollColors` (basic color customization)
- Localization: en, ar
- Options: forcedDocumentType, exitStep, skipTutorial, correlationId, googleApiKey, requestId, contractSigning

## How to Update When Native SDK Changes

1. Update `android/build.gradle` → change `eNROLL-Lite-Android:vX.Y.Z`
2. Update `package.json` → bump plugin version
3. Mirror new parameters/types to TypeScript API (`src/definitions.ts`)
4. Update `.enroll-linkage.json` with new version
5. Run sync check: `bash /Users/luminsoft/StudioProjects/ekyc-android/scripts/check-enroll-sync.sh`

## Where to Update Docs

- `README.md` — installation and usage
- `CHANGELOG.md` — version history
- `docs/api.md` — API reference
- `docs/integration-android.md` — Android setup
- `docs/integration-ios.md` — iOS setup
- `docs/integration-ionic-angular.md` — Ionic/Angular setup
- `.enroll-linkage.json` — machine-readable metadata

## TODO — Pending Issues

- [ ] **Android SDK version outdated** — Currently v1.2.6, should be v1.3.2
  - Update: `android/build.gradle` → `eNROLL-Lite-Android:v1.3.2`
- [ ] **iOS EnrollNeoCore pod outdated** — Currently 1.0.13, Flutter Neo is at 1.0.17
  - Update: `EnrollCapacitorNeo.podspec` → `EnrollNeoCore 1.0.17`
- [ ] **XCFramework outdated** — MD5 mismatch with Flutter Neo
  - Copy from: `enroll_neo_plugin/ios/Frameworks/EnrollFramework.xcframework`
  - To: `enroll-capacitor-neo/ios/Frameworks/EnrollFramework.xcframework`
- [ ] **enrollTheme** — Flutter Neo has unified theme with colors + icons, this plugin only has `enrollColors`
  - Implementation: `src/definitions.ts` (add `EnrollTheme`, `EnrollIcons` interfaces)
- [ ] **iconCustomization** — Flutter Neo has icon customization types
  - Implementation: `src/definitions.ts`
- [ ] **showSponsoredBy** — Flutter Neo has logo option
  - Implementation: `src/definitions.ts`
- **Wait for Flutter implementation of missing features before implementing here**
