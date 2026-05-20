# eNROLL Neo Capacitor Plugin

Capacitor plugin for the **eNROLL Neo SDK** — eKYC identity verification for Ionic and Capacitor mobile apps on Android and iOS.

eNROLL is a lightweight compliance solution that prevents identity fraud and phishing. Powered by AI, it reduces errors and speeds up identification, ensuring secure verification.

> **⚠️ Native mobile only.** This plugin does **not** support browser/web usage. It requires Capacitor running on a physical or emulated Android/iOS device.

Current native SDK versions:
- **Android:** eNROLL-Lite-Android v1.3.2 (via JitPack)
- **iOS:** EnrollFramework xcframework + EnrollNeoCore 1.0.17 (via CocoaPods)

> This is the **Neo / Lumin Light** variant of the eNROLL SDK. For the standard eNROLL SDK, see the [eNROLL documentation](https://lumin-soft.gitbook.io/ekyc/integration-guide/mobile-plugin/enroll-android-sdk).

## Requirements

| Platform | Minimum |
|----------|---------|
| Capacitor | 8.0+ |
| Android minSdk | 24 |
| Android compileSdk | 36 |
| Android targetSdk | 36 |
| iOS deployment target | 15.5 |
| Kotlin | 2.1.0 |
| Swift | 5.0 |
| Node.js | 18+ |

## Client Integration Checklist

Before starting integration, make sure you have:

- `tenantId` and `tenantSecret` for the target environment.
- The required flow: `onboarding`, `auth`, `update`, `signContract`, or `forgetProfileData`.
- `applicantId` and `levelOfTrust` if you will use `auth`.
- `applicantId` if you will use `update`.
- `templateId` if you will use `signContract`.
- A physical iOS device for iOS testing.
- Android Studio / Xcode configured for the host Capacitor app.

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

For projects that manage repositories from `android/settings.gradle`, add JitPack inside `dependencyResolutionManagement.repositories`.

#### 2. Verify minSdkVersion

Ensure `minSdkVersion` is at least **24** in `android/variables.gradle`:

```gradle
ext {
    minSdkVersion = 24
    compileSdkVersion = 36
    targetSdkVersion = 36
}
```

### iOS Setup

#### 1. Add Pod Sources

Add these sources to the **top** of your `ios/App/Podfile` (before `platform :ios`):

```ruby
source 'https://github.com/LuminSoft/eNROLL-Neo-Core-specs.git'
source 'https://github.com/CocoaPods/Specs.git'

platform :ios, '15.5'
use_frameworks! :linkage => :static
```

#### 2. Add Info.plist Permissions

Add to `ios/App/App/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>We need camera access to capture your ID and face for verification</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>We need your location for security compliance</string>
```

#### 3. Install Pods

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

### Basic Example

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
} catch (error: any) {
  console.error('Enrollment failed:', error?.data ?? error);
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

### Update Mode

```typescript
const result = await Enroll.startEnroll({
  tenantId: 'YOUR_TENANT_ID',
  tenantSecret: 'YOUR_TENANT_SECRET',
  enrollMode: 'update',
  applicantId: 'APPLICANT_ID',
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

### Forget Profile Data Mode

```typescript
const result = await Enroll.startEnroll({
  tenantId: 'YOUR_TENANT_ID',
  tenantSecret: 'YOUR_TENANT_SECRET',
  enrollMode: 'forgetProfileData',
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

## Request ID and Resume

The SDK can emit a `requestId` while the flow is still running. Store it on your backend if you need to resume an interrupted enrollment later.

```typescript
const listener = await Enroll.addListener('onRequestId', ({ requestId }) => {
  // Send requestId to your backend and link it to your user/session.
});

await Enroll.startEnroll({
  tenantId: 'YOUR_TENANT_ID',
  tenantSecret: 'YOUR_TENANT_SECRET',
  enrollMode: 'onboarding',
  requestId: 'PREVIOUS_REQUEST_ID',
});

await listener.remove();
```

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
| `enrollTheme` | `EnrollTheme` | | — | Unified theme (colors + icons). Takes priority over `enrollColors` |
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

## Theme & Icon Customization

Use `enrollTheme` for unified color + icon customization. When both `enrollTheme` and `enrollColors` are set, `enrollTheme` takes priority.

> **Note:** Both color and icon customization are supported on **Android and iOS**. On Android, `assetName` refers to drawable resources; on iOS, it refers to image assets in `Assets.xcassets`.

```typescript
await Enroll.startEnroll({
  // ...required params...
  enrollTheme: {
    colors: {
      primary: { r: 29, g: 86, b: 184 },
    },
    icons: {
      logo: {
        mode: 'custom',
        assetName: 'my_logo',
        renderingMode: 'original',
        showSponsoredBy: false,
      },
      nationalId: {
        tutorial: { assetName: 'custom_nid_tutorial', renderingMode: 'template' },
      },
    },
  },
});
```

### Icon Asset Names

Icon `assetName` values reference platform-specific image assets:

- **Android:** drawable resource names in `android/app/src/main/res/drawable/` without the `R.drawable.` prefix.
- **iOS:** image asset names in the app target's `Assets.xcassets`.

### Logo Configuration

| Property | Type | Description |
|----------|------|-------------|
| `mode` | `'defaultLogo' \| 'hidden' \| 'custom'` | Logo display mode |
| `assetName` | `string` | Drawable name for custom logo |
| `renderingMode` | `'original' \| 'template'` | Color rendering mode |
| `showSponsoredBy` | `boolean` | Show "Sponsored by" label (default `true`) |

## Enrollment Step Types

Used with `enrollExitStep` to terminate the flow after a specific step:

`phoneOtp` · `personalConfirmation` · `smileLiveness` · `emailOtp` · `saveMobileDevice` · `deviceLocation` · `password` · `securityQuestions` · `amlCheck` · `termsAndConditions` · `electronicSignature` · `ntraCheck` · `csoCheck`

---

## Platform Limitations

| Feature | Android | iOS |
|---------|---------|-----|
| Color theming | ✅ | ✅ |
| Icon customization | ✅ | ✅ |
| Neo SDK | ✅ | ✅ |
| Simulator support | ✅ (emulator) | ❌ (device only) |

## Troubleshooting

### Web Preview Error

This plugin is native-only. Running in a browser, Ionic serve, or Vite preview will throw an unavailable error. Use:

```bash
npx cap run android
npx cap run ios
```

### FLOW_IN_PROGRESS

`FLOW_IN_PROGRESS` means `startEnroll` was called while another enrollment flow is already open. Disable the launch button until the returned promise resolves or rejects.

### INVALID_ARGUMENT

Check the required fields for the selected mode:

- `onboarding`: `tenantId`, `tenantSecret`
- `auth`: `tenantId`, `tenantSecret`, `applicantId`, `levelOfTrust`
- `update`: `tenantId`, `tenantSecret`, `applicantId`
- `signContract`: `tenantId`, `tenantSecret`, `templateId`
- `forgetProfileData`: `tenantId`, `tenantSecret`

### iOS Pod Install or Build Issues

Confirm that the Podfile includes the required pod sources, `platform :ios, '15.5'`, and `use_frameworks! :linkage => :static`. Then run:

```bash
cd ios/App
pod install
```

Build and test on a physical iOS device.

## Security Notes

- **Never hardcode** `tenantSecret`, `levelOfTrust`, or API keys in client-side code.
- Use secure storage (Keychain on iOS, Keystore on Android).
- Rooted/jailbroken devices are blocked by default.
- All SDK network calls use HTTPS.
- Regularly update the plugin to the latest stable version.


## License

MIT
