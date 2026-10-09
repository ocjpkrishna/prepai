import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';
import { CheckoutOrder } from '../models/subscription.model';

const CHECKOUT_SCRIPT_URL = 'https://checkout.razorpay.com/v1/checkout.js';

interface RazorpayOptions {
  key: string;
  order_id: string;
  amount: number;
  currency: string;
  name: string;
  prefill: { email: string };
  handler: () => void;
}

interface RazorpayInstance {
  open(): void;
}

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => RazorpayInstance;
  }
}

/** Loads Razorpay's checkout script on demand and opens it for one order. */
@Injectable({ providedIn: 'root' })
export class RazorpayCheckoutService {
  private readonly document = inject(DOCUMENT);

  async open(order: CheckoutOrder, email: string, onPaid: () => void): Promise<void> {
    await this.loadScript();
    const RazorpayClass = this.document.defaultView?.Razorpay;
    if (!RazorpayClass) {
      throw new Error('Razorpay checkout did not load');
    }
    new RazorpayClass({
      key: order.keyId,
      order_id: order.orderId,
      amount: order.amountPaise,
      currency: order.currency,
      name: 'PrepAI',
      prefill: { email },
      handler: onPaid,
    }).open();
  }

  private loadScript(): Promise<void> {
    if (this.document.defaultView?.Razorpay) {
      return Promise.resolve();
    }
    return new Promise((resolve, reject) => {
      const script = this.document.createElement('script');
      script.src = CHECKOUT_SCRIPT_URL;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Razorpay checkout script failed to load'));
      this.document.body.appendChild(script);
    });
  }
}
