# Changelog — eNROLL Neo Capacitor Plugin

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-04-08

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
