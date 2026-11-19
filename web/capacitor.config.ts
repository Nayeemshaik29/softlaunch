import type { CapacitorConfig } from "@capacitor/cli";

// The iOS and Android apps wrap the static export of this Next.js app (npm run build:mobile → out/).
// appId: change to a reverse domain YOU own before publishing (it can never change after the first release).
// CAP_DEV=1 npm run build:mobile → the apps may call your Mac's plain-http backend (localhost:8080).
// Never ship a build made with CAP_DEV=1.
const dev = process.env.CAP_DEV === "1";

const config: CapacitorConfig = {
  appId: "com.softlaunch.app", // [FILL IN] e.g. com.yourdomain.softlaunch
  appName: "SoftLaunch",
  webDir: "out",
  backgroundColor: "#0e0c10",
  ios: { contentInset: "never", scheme: "capacitor" },          // pages run at capacitor://localhost
  android: { allowMixedContent: dev },                          // pages run at https://localhost
  ...(dev ? { server: { cleartext: true } } : {}),
  plugins: {
    SplashScreen: { launchAutoHide: false, backgroundColor: "#0e0c10", showSpinner: false }, // hidden by setupNative()
    PrivacyScreen: { enable: false },   // switched on per screen (chat, match moment)
  },
};

export default config;
