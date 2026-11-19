# iPhone and Android apps (Capacitor)

The apps wrap the same Next.js app as the website (`web/`), exported as static files and shipped **inside** the app
(no website loaded at runtime). Native features come from Capacitor plugins.

| | iOS | Android |
|---|---|---|
| Project | `web/ios/` (Swift Package Manager, iOS 15+) | `web/android/` (Gradle, Android 7+ / SDK 24, target 36) |
| Page origin | `capacitor://localhost` | `https://localhost` |
| Login session | refresh token in the **Keychain** | refresh token in the **Keystore** |
| Screenshots of chats / match moment | hidden in the app switcher and recordings (iOS can't block screenshots; chat-service warns the other person) | **blocked** (FLAG_SECURE) |
| Discreet icon | ✅ alternate icon "discreet" (Settings → Discreet app icon) | ⏳ not yet: needs testing on a device first |
| Haptics | swipe + match | swipe + match |
| Back button | – | closes the match moment, then goes back, then sends the app to the background |
| Buying Plus | hidden (Apple IAP still to do) | hidden (Google Play Billing still to do) |

## 1. Install the tools (once, by you)

- **iPhone:** Xcode from the Mac App Store (big download, needs your Apple ID). Then run: `sudo xcode-select -s /Applications/Xcode.app` and open Xcode once to accept the license and install an iOS Simulator.
- **Android:** Android Studio from developer.android.com. In its first-run wizard install the Android SDK and create an emulator (Device Manager).

## 2. Build and run (local backend)

```bash
cd ~/Documents/softlaunch/web
CAP_DEV=1 NEXT_PUBLIC_API_URL=http://localhost:8080 npm run build:mobile   # static export + copy into both apps
```

- **iOS Simulator:** `npm run ios` opens Xcode → pick an iPhone simulator → ▶ Run. The simulator reaches your Mac at `localhost`.
- **Android emulator:** start the emulator, then forward its `localhost` to your Mac, then run from Android Studio:

```bash
adb reverse tcp:8080 tcp:8080
```
```bash
adb reverse tcp:9000 tcp:9000
```
```bash
npm run android
```

`CAP_DEV=1` lets the apps call the plain-HTTP local backend. **Never ship a build made with it.**

## 3. Before the stores

- [ ] `appId` in `web/capacitor.config.ts`: a reverse domain you own (`com.yourdomain.softlaunch`). It can never change after the first release.
- [ ] Release builds: `NEXT_PUBLIC_API_URL=https://api.yourdomain.com npm run build:mobile` (no `CAP_DEV`).
- [ ] Backend `WEB_ORIGINS` includes `capacitor://localhost,https://localhost` (the apps' origins; already in `.env.example`).
- [ ] Signing: Apple Developer account (team + provisioning) and an Android upload key (keep it in your password manager).
- [ ] **Plus inside the apps:** Apple In-App Purchase + Google Play Billing (plus server-side receipt checks). Until then the apps don't sell Plus.
- [ ] Store listing stays **calm** (no After dark screenshots): Apple 2.3.8 / Play listing rules. Age rating 18+.
- [ ] Data safety / App Privacy answers: `docs/DATA_SAFETY.md` (add "Device or other IDs" if push notifications are added).
- [ ] Android discreet icon: test the activity-alias approach on a real device before enabling it.
- [ ] Push notifications (FCM / APNs): not built yet.

## How login works in the apps
The website keeps the refresh token in an HttpOnly cookie. The apps run on a different site, so that cookie can't
work. They send `X-Refresh-Transport: body` and get the refresh token in the response body, keep it in the
Keychain/Keystore, and send it back as `X-Refresh-Token`. The server only does this for the apps' own origins, so a
web page can't use it to read its token. Rotation and theft detection work exactly as on the web.
