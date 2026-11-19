"use client";

import { api } from "./api";
import type { Checkout, PlusStatus } from "./types";

export const rupees = (paise: number) => `₹${(paise / 100).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;

type RazorpayResponse = { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature: string };
type RazorpayCtor = new (opts: Record<string, unknown>) => { open: () => void; on: (e: string, cb: () => void) => void };

function loadRazorpay(): Promise<RazorpayCtor> {
  const w = window as Window & { Razorpay?: RazorpayCtor };
  if (w.Razorpay) return Promise.resolve(w.Razorpay);
  return new Promise((ok, fail) => {
    const s = document.createElement("script");
    s.src = "https://checkout.razorpay.com/v1/checkout.js"; // Razorpay's official checkout (allowed in our CSP)
    s.onload = () => (w.Razorpay ? ok(w.Razorpay) : fail(new Error("Checkout didn't load")));
    s.onerror = () => fail(new Error("Couldn't reach the payment page. Check your connection."));
    document.head.appendChild(s);
  });
}

/**
 * Start a purchase. Razorpay: opens Razorpay Checkout; the server verifies the signature before Plus turns on.
 * Mock (local/CI): returns the order so the page can show its clearly-labelled test checkout.
 */
export async function startCheckout(planId: string, accent: string): Promise<{ mock: Checkout } | { status: PlusStatus } | { cancelled: true }> {
  const order = await api<Checkout>("/api/users/me/billing/checkout", { method: "POST", body: { planId } });
  if (order.provider === "mock") return { mock: order };
  const Razorpay = await loadRazorpay();
  return new Promise((resolve, reject) => {
    const rz = new Razorpay({
      key: order.checkout.keyId,
      order_id: order.orderId,
      amount: order.amountPaise,
      currency: order.currency,
      name: "SoftLaunch",
      description: order.planName,
      theme: { color: accent },
      handler: (r: RazorpayResponse) => {
        api<PlusStatus>("/api/users/me/billing/confirm", {
          method: "POST",
          body: { orderId: r.razorpay_order_id, paymentId: r.razorpay_payment_id, signature: r.razorpay_signature },
        }).then((status) => resolve({ status }), reject);
      },
      modal: { ondismiss: () => resolve({ cancelled: true }) },
    });
    rz.open();
  });
}

export function mockPay(orderId: string) {
  return api<PlusStatus>("/api/users/me/billing/mock-pay", { method: "POST", body: { orderId } });
}
