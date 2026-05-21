# Changelog — eNROLL Neo Capacitor Plugin

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.2] - 2026-05-21

### Changed

- Expanded README documentation for `enrollExitStep`, partial-flow behavior, resume handling, and native Android step mapping.

## [1.1.1] - 2026-05-21

### Changed

- Updated README integration requirements and client-facing setup guidance.
- Improved publish review scan exclusions for internal linkage documentation.

### Fixed

- Replaced example app tenant credentials with placeholders.

## [1.1.0] - 2026-05-19

### Added

- Custom logo theme support (mode, assetName, renderingMode, showSponsoredBy)
- Full `enrollTheme` support on iOS — colors and icons now work on both platforms

### Changed

- Updated Android SDK from v1.2.6 to v1.3.2
- Updated iOS EnrollNeoCore pod dependency from 1.0.6 to 1.0.17
- Added `OpenSSL-Universal` pod dependency for iOS
- Improved iOS build script with simulator support

### Fixed

- Remove incorrect `forgetProfileData` mode overclaim from documentation

## [1.0.0] - 2026-04-09

### Added
- Initial public release of `enroll-capacitor-neo`
- TypeScript type definitions for the full eNROLL Neo API surface
- Android native bridge (Kotlin) using eNROLL-Lite-Android v1.2.4 via JitPack
- iOS native bridge (Swift) using EnrollFramework xcframework + EnrollNeoCore 1.0.6 via CocoaPods
- Support for all 5 enrollment modes: onboarding, auth, update, signContract, forgetProfileData
- Full success model exposure (applicantId, enrollMessage, documentId, requestId, exitStepCompleted, completedStepName)
- Custom color theming support
- Forced document type support
- Exit step support
- Localization support (English and Arabic with RTL)
- Double-launch prevention guard
- Input validation with clear error codes
- Web stub that throws clear "not supported" error
- Comprehensive documentation and integration guides
