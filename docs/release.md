# Release Process

How to publish a new version of the eNROLL Capacitor Plugin.

## Pre-Release Checklist

- [ ] All manual tests passing on both platforms
- [ ] No breaking changes (or migration plan documented)
- [ ] Version numbers updated (see below)
- [ ] CHANGELOG.md updated with new version entry
- [ ] TypeScript builds cleanly (`npm run build`)
- [ ] README.md up to date
- [ ] Security review completed (no hardcoded secrets, no PII logging)

## Version Bump

Update the version in `package.json`:

```json
{
  "version": "X.Y.Z"
}
```

Follow semantic versioning:

| Change Type | Bump | Example |
|-------------|------|---------|
| Bug fix (no API change) | PATCH | 0.1.0 → 0.1.1 |
| New feature (backward compatible) | MINOR | 0.1.0 → 0.2.0 |
| Breaking change | MAJOR | 0.1.0 → 1.0.0 |

The podspec reads version from `package.json` automatically.

## Build

```bash
npm run build
```

This runs: clean → docgen → tsc → rollup

## Publish to npm

```bash
# Login to npm (first time only)
npm login

# Publish (prepublishOnly hook runs build automatically)
npm publish
```

## Post-Publish

1. **Git tag:** Create a version tag and push:
   ```bash
   git tag v0.1.0
   git push origin v0.1.0
   ```

2. **GitHub Release:** Create a release on GitHub with:
   - Tag: `vX.Y.Z`
   - Title: `vX.Y.Z`
   - Body: Copy from CHANGELOG.md

3. **Verify:** Install the published version in a test project:
   ```bash
   npm install enroll-capacitor-plugin@X.Y.Z
   npx cap sync
   ```

## Consumer Update Instructions

When clients need to update:

```bash
npm update enroll-capacitor-plugin
npx cap sync
# iOS only:
cd ios/App && pod install && cd ../..
```

## Rollback

If a bad version is published:

```bash
npm unpublish enroll-capacitor-plugin@X.Y.Z
```

> Note: npm unpublish only works within 72 hours of publishing. After that, publish a patch fix instead.
