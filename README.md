# eNROLL Neo Capacitor Plugin

Capacitor plugin for the **eNROLL Neo SDK** — eKYC identity verification for Ionic and Capacitor mobile apps on Android and iOS.

eNROLL is a lightweight compliance solution that prevents identity fraud and phishing. Powered by AI, it reduces errors and speeds up identification, ensuring secure verification.

> **⚠️ Native mobile only.** This plugin does **not** support browser/web usage. It requires Capacitor running on a physical or emulated Android/iOS device.

Current native SDK versions:
- **Android:** eNROLL-Lite-Android v1.2.4 (via JitPack)
- **iOS:** EnrollFramework xcframework + EnrollNeoCore 1.0.6 (via CocoaPods)

> This is the **Neo / Lumin Light** variant of the eNROLL SDK. For the standard eNROLL SDK, see the [eNROLL documentation](https://lumin-soft.gitbook.io/ekyc/integration-guide/mobile-plugin/enroll-android-sdk).

## Requirements

| Platform | Minimum |
|----------|---------|
| Capacitor | 6.0+ |
| Android minSdk | 24 |
| Android compileSdk | 34 |
| iOS deployment target | 15.5 |
| Kotlin | 2.1.0 |
| Swift | 5.0 |
| Node.js | 18+ |

## Installation

```bash
npm install enroll-capacitor-neo
npx cap sync
```

### Android Setup

#### 1. Add JitPack Repository

Add the JitPack repository to your **project-level** `android/build.gradle` (or `android/settings.gradle` for newer Gradle structures):

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### 2. Verify minSdkVersion

Ensure `minSdkVersion` is at least **24** in `android/variables.gradle`:

```gradle
ext {
    minSdkVersion = 24
}
```

### iOS Setup

#### 1. Add Pod Sources

Add these sources to the **top** of your `ios/App/Podfile` (before `platform :ios`):

```ruby
source 'https://github.com/LuminSoft/eNROLL-Neo-Core-specs.git'
source 'https://github.com/CocoaPods/Specs.git'
```

#### 2. Set Deployment Target

Ensure iOS deployment target is at least **15.5** in your `ios/App/Podfile`:

```ruby
platform :ios, '15.5'
```

#### 3. Add Info.plist Permissions

Add to `ios/App/App/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>We need camera access to capture your ID and face for verification</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>We need your location for security compliance</string>
```

#### 4. Install Pods

```bash
cd ios/App && pod install && cd ../..
```

> **Note:** iOS builds require a **physical device**. The EnrollFramework does not include a simulator architecture.

### ePassport / NFC (Optional — iOS only)

If you need electronic passport NFC reading, add to `Info.plist`:

```xml
<key>com.apple.developer.nfc.readersession.felica.systemcodes</key>
<array><string>A0000002471001</string></array>
<key>com.apple.developer.nfc.readersession.iso7816.select-identifiers</key>
<array><string>A0000002471001</string></array>
<key>NFCReaderUsageDescription</key>
<string>We need NFC access to read your electronic passport</string>
```

Then enable **Near Field Communication Tag Reading** in Xcode → Target → Signing & Capabilities.

---

## Usage

### Basic Example (Ionic/Angular)

```typescript
import { Enroll } from 'enroll-capacitor-neo';
import type { EnrollSuccessResult } from 'enroll-capacitor-neo';

// Listen for request ID events (fires mid-flow)
const listener = await Enroll.addListener('onRequestId', (data) => {
  console.log('Request ID:', data.requestId);
});

try {
  const result: EnrollSuccessResult = await Enroll.startEnroll({
    tenantId: 'YOUR_TENANT_ID',
    tenantSecret: 'YOUR_TENANT_SECRET',
    enrollMode: 'onboarding',
    enrollEnvironment: 'staging',
    localizationCode: 'en',
    skipTutorial: false,
  });

  console.log('Success! Applicant ID:', result.applicantId);
  console.log('Exit step completed:', result.exitStepCompleted);
} catch (error) {
  console.error('Enrollment failed:', error);
} finally {
  await listener.remove();
}
```

### Authentication Mode

```typescript
const result = await Enroll.startEnroll({
  tenantId: 'YOUR_TENANT_ID',
  tenantSecret: 'YOUR_TENANT_SECRET',
  enrollMode: 'auth',
  applicantId: 'APPLICANT_ID',
  levelOfTrust: 'LEVEL_OF_TRUST_TOKEN',
});
```

### Sign Contract Mode

```typescript
const result = await Enroll.startEnroll({
  tenantId: 'YOUR_TENANT_ID',
  tenantSecret: 'YOUR_TENANT_SECRET',
  enrollMode: 'signContract',
  templateId: '12345',
  contractParameters: '{"key": "value"}',
});
```

---

## Enroll Modes

| Mode | Description | Required Params |
|------|-------------|-----------------|
| `onboarding` | Register a new user | `tenantId`, `tenantSecret` |
| `auth` | Authenticate existing user | + `applicantId`, `levelOfTrust` |
| `update` | Re-verify / update user | + `applicantId` |
| `signContract` | Sign contract templates | + `templateId` |
| `forgetProfileData` | Request profile data deletion | `tenantId`, `tenantSecret` |

## Configuration Options

| Key | Type | Required | Default | Description |
|-----|------|----------|---------|-------------|
| `tenantId` | `string` | ✅ | — | Organization tenant ID |
| `tenantSecret` | `string` | ✅ | — | Organization tenant secret |
| `enrollMode` | `EnrollMode` | ✅ | — | SDK flow mode |
| `applicantId` | `string` | mode-dep | — | Applicant ID (required for `auth`, `update`) |
| `levelOfTrust` | `string` | mode-dep | — | Level-of-trust token (required for `auth`) |
| `templateId` | `string` | mode-dep | — | Contract template ID (required for `signContract`) |
| `enrollEnvironment` | `EnrollEnvironment` | | `'staging'` | Target environment |
| `localizationCode` | `EnrollLocalization` | | `'en'` | UI language |
| `googleApiKey` | `string` | | — | Google Maps API key for location step |
| `skipTutorial` | `boolean` | | `false` | Skip the tutorial screen |
| `correlationId` | `string` | | — | Link your user ID with eNROLL request ID |
| `requestId` | `string` | | — | Resume a previous enrollment request |
| `contractParameters` | `string` | | — | JSON string of contract parameters |
| `enrollColors` | `EnrollColors` | | — | Custom UI color overrides |
| `enrollForcedDocumentType` | `EnrollForcedDocumentType` | | — | Force specific document type |
| `enrollExitStep` | `EnrollStepType` | | — | Auto-close SDK after this step |

## Success Result

| Field | Type | Description |
|-------|------|-------------|
| `applicantId` | `string` | Assigned applicant ID |
| `enrollMessage` | `string?` | Human-readable success message |
| `documentId` | `string?` | Document ID (if applicable) |
| `requestId` | `string?` | Request ID for resuming later |
| `exitStepCompleted` | `boolean` | `true` if flow ended early via `enrollExitStep` |
| `completedStepName` | `string?` | Name of the completed exit step |

## Custom Colors

```typescript
await Enroll.startEnroll({
  // ...required params...
  enrollColors: {
    primary: { r: 29, g: 86, b: 184, opacity: 1.0 },
    secondary: { r: 87, g: 145, b: 219 },
    appBackgroundColor: { r: 255, g: 255, b: 255 },
    textColor: { r: 0, g: 65, b: 148 },
    errorColor: { r: 219, g: 48, b: 91 },
    successColor: { r: 97, g: 204, b: 61 },
    warningColor: { r: 249, g: 213, b: 72 },
  },
});
```

## Enrollment Step Types

Used with `enrollExitStep` to terminate the flow after a specific step:

`phoneOtp` · `personalConfirmation` · `smileLiveness` · `emailOtp` · `saveMobileDevice` · `deviceLocation` · `password` · `securityQuestions` · `amlCheck` · `termsAndConditions` · `electronicSignature` · `ntraCheck` · `csoCheck`

---

## Security Notes

- **Never hardcode** `tenantSecret`, `levelOfTrust`, or API keys in client-side code.
- Use secure storage (Keychain on iOS, Keystore on Android).
- Rooted/jailbroken devices are blocked by default.
- All SDK network calls use HTTPS.
- Regularly update the plugin to the latest stable version.

## Documentation

- [API Reference](docs/api.md)
- [Android Integration](docs/integration-android.md)
- [iOS Integration](docs/integration-ios.md)
- [Ionic/Angular Integration](docs/integration-ionic-angular.md)
- [Testing Guide](docs/testing.md)
- [Release Process](docs/release.md)
- [Publish Checklist](docs/publish-checklist.md)
- [Contributing](CONTRIBUTING.md)

## License

MIT
