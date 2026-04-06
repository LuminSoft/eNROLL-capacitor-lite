# iOS Integration Guide

Detailed setup instructions for using the eNROLL Capacitor Plugin on iOS.

## Prerequisites

- Xcode 15+
- **Physical iOS device** (the EnrollFramework does NOT include a simulator slice)
- CocoaPods (`gem install cocoapods`)
- Apple Developer account (for device provisioning)
- iOS 15.5+ deployment target

## Step 1: Install the Plugin

```bash
npm install enroll-capacitor-plugin
npx cap sync ios
```

## Step 2: Add Pod Sources

The `EnrollNeoCore` pod is hosted in a private spec repo. Add these sources to the **top** of your `ios/App/Podfile` (before `platform :ios`):

```ruby
source 'https://github.com/LuminSoft/eNROLL-Neo-Core-specs.git'
source 'https://github.com/CocoaPods/Specs.git'
```

## Step 3: Set Deployment Target

In `ios/App/Podfile`, ensure:

```ruby
platform :ios, '15.5'
```

## Step 4: Add Info.plist Permissions

Add to `ios/App/App/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>We need camera access to capture your ID and face for verification</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>We need your location for security compliance</string>
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsArbitraryLoads</key>
    <true/>
</dict>
```

## Step 5: Install Pods and Build

```bash
cd ios/App && pod install && cd ../..
npx cap open ios   # Opens in Xcode
```

Select your physical device in Xcode and build.

## ePassport / NFC (Optional)

If your eKYC flow includes electronic passport reading via NFC:

### 1. Add to Info.plist

```xml
<key>com.apple.developer.nfc.readersession.felica.systemcodes</key>
<array><string>A0000002471001</string></array>
<key>com.apple.developer.nfc.readersession.iso7816.select-identifiers</key>
<array><string>A0000002471001</string></array>
<key>NFCReaderUsageDescription</key>
<string>We need NFC access to read your electronic passport</string>
```

### 2. Enable NFC Capability

1. Open `ios/App/App.xcworkspace` in Xcode
2. Select your Target → **Signing & Capabilities**
3. Click **+ Capability**
4. Add **Near Field Communication Tag Reading**

> NFC requires a physical device. It will not work on simulators.

## Firebase Considerations

The plugin depends on `FirebaseRemoteConfig` via CocoaPods. If your app already uses Firebase:
- Ensure your Firebase pod versions are compatible
- The plugin uses `FirebaseRemoteConfig` — version conflicts may require aligning versions in your Podfile

## Troubleshooting

### "No simulator slice" / Build fails on simulator
- The `EnrollFramework.xcframework` is arm64 only. You **must** test on a physical device.
- For development without a device, you can stub the plugin calls in your TypeScript code.

### Pod install fails with "Unable to find a specification for EnrollNeoCore"
- Ensure the pod source is added at the top of your Podfile:
  `source 'https://github.com/LuminSoft/eNROLL-Neo-Core-specs.git'`

### Firebase version conflict
- If you see Firebase version conflicts, try specifying compatible versions in your Podfile.

### Build fails with deployment target error
- Set `platform :ios, '15.5'` in your Podfile
- In Xcode, set the deployment target to 15.5 for your app target
