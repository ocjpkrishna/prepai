import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CheckoutOrder, SubscriptionPlan } from '../models/subscription.model';

const SUBSCRIPTION_PATH = `${environment.apiBaseUrl}/subscriptions`;

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly http = inject(HttpClient);

  plans(): Observable<SubscriptionPlan[]> {
    return this.http.get<SubscriptionPlan[]>(`${SUBSCRIPTION_PATH}/plans`);
  }

  checkout(planId: string): Observable<CheckoutOrder> {
    return this.http.post<CheckoutOrder>(`${SUBSCRIPTION_PATH}/checkout`, { planId, paymentMethod: 'razorpay' });
  }
}
