# RELEASING.md — How updates ship

## The rule
Same `applicationId` (`dev.vratik.anc`) + same signing key + higher version = Android updates the app in place and keeps its settings. Break either of the first two and the phone must uninstall first.

## Ship an update
```
git tag v0.1.1 && git push origin v0.1.1
```
GitHub Actions (`.github/workflows/release.yml`) runs the tests, builds the APK, signs it, and publishes a GitHub Release with `earbuds-anc-v0.1.1.apk` attached.
On the phone: app → Updates → Check for updates → Update.

Version code = major·10000 + minor·100 + patch (from the tag), so tags must only go up.

## One-time setup: signing key
Generated on the G14, never committed:
- Keystore: `~/.config/earbuds-anc/release.jks`
- Passwords + alias: `~/.config/earbuds-anc/keystore.env` (chmod 600)
- **Back both up somewhere off the laptop** (password manager / USB). Lose them = no more updates without a reinstall.

GitHub repo → Settings → Secrets and variables → Actions:
| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 ~/.config/earbuds-anc/release.jks` |
| `KEYSTORE_PASSWORD` | from keystore.env |
| `KEY_ALIAS` | from keystore.env |
| `KEY_PASSWORD` | from keystore.env |

## Updater needs a public release source
The app checks `api.github.com/repos/vrapatel2002/earbuds-anc/releases/latest` with no token. A private repo returns 404. Either make the repo public or point `UPDATE_REPO` (app/build.gradle.kts) at a separate public releases repo.

## Debug builds
Every push to `main` builds a debug APK (CI artifact). It installs as `dev.vratik.anc.debug`, next to the real app, never replacing it.

## Monitoring
- Builds/releases: GitHub Actions + Releases pages (Claude can read them from chat).
- Crashes: app saves the stack trace; next launch shows "Report on GitHub" → opens a pre-filled issue labelled `crash`.
- Logs: app → Log → Export log.
