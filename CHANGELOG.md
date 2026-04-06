# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - Unreleased

### Added
- Initial Capacitor plugin scaffolding
- TypeScript type definitions for full eNROLL API surface
- Android native bridge (Kotlin) calling eNROLL-Lite-Android v1.2.4
- iOS native bridge (Swift) calling EnrollFramework xcframework
- Support for all 5 enrollment modes: onboarding, auth, update, signContract, forgetProfileData
- Full success model exposure (applicantId, enrollMessage, documentId, requestId, exitStepCompleted, completedStepName)
- Custom color theming support
- Forced document type support
- Exit step support
- Localization support (English and Arabic with RTL)
- Double-launch prevention guard
- Input validation with clear error codes
- Web stub that throws clear "not supported" error
- Comprehensive documentation (README, ARCHITECTURE, PROJECT_RULES, integration guides)
