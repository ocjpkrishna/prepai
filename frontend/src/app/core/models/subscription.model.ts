import { Plan } from './user.model';

export interface SubscriptionPlan {
  id: string;
  name: string;
  plan: Plan;
  priceInr: number;
  sessionsPerDay: number | null;
  sessionMinutes: number;
}

export interface CheckoutOrder {
  orderId: string;
  keyId: string;
  amountPaise: number;
  currency: 'INR';
}
