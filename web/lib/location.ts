"use client";

/**
 * One position for "Use my location". Tries the quick, low-power fix first (Wi-Fi / mobile network); if that
 * times out (no network location, e.g. emulators or some phones indoors) it asks for a GPS fix instead.
 * With "Approximate" permission Android rounds the result either way. We only ever store a rounded location.
 */
export function currentPosition(): Promise<GeolocationPosition> {
  const once = (high: boolean, timeout: number) =>
    new Promise<GeolocationPosition>((resolve, reject) => {
      if (!navigator.geolocation) return reject(Object.assign(new Error("unsupported"), { code: 0 }));
      navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: high, timeout, maximumAge: 600_000 });
    });
  return once(false, 8_000).catch((e: GeolocationPositionError) => {
    if (e.code === 1) throw e; // permission denied: asking again won't help
    return once(true, 15_000);
  });
}
