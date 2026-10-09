import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiError } from '../../core/errors/api-error';
import { CheckoutOrder, SubscriptionPlan } from '../../core/models/subscription.model';
import { RazorpayCheckoutService } from '../../core/services/razorpay-checkout.service';
import { SubscriptionService } from '../../core/services/subscription.service';
import { UserService } from '../../core/services/user.service';
import { ApiErrorComponent } from '../../shared/components/api-error/api-error.component';
import { PlanCardsComponent } from '../../shared/components/plan-cards/plan-cards.component';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-pricing',
  imports: [PlanCardsComponent, ApiErrorComponent],
  templateUrl: './pricing.component.html',
  styleUrl: './pricing.component.scss',
})
export class PricingComponent implements OnInit {
  private readonly subscriptions = inject(SubscriptionService);
  private readonly users = inject(UserService);
  private readonly razorpay = inject(RazorpayCheckoutService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly plans = signal<SubscriptionPlan[]>([]);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly paymentMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.subscriptions.plans().subscribe({
      next: plans => this.plans.set(plans),
      error: (error: ApiError) => this.error.set(error),
    });
  }

  protected choose(plan: SubscriptionPlan): void {
    if (plan.priceInr === 0) {
      void this.router.navigate([this.auth.isLoggedIn() ? '/lessons/new' : '/register']);
      return;
    }
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/register']);
      return;
    }
    this.startCheckout(plan);
  }

  private startCheckout(plan: SubscriptionPlan): void {
    this.error.set(null);
    this.paymentMessage.set(null);
    this.subscriptions.checkout(plan.id).subscribe({
      next: order => this.payWithEmail(order, plan),
      error: (error: ApiError) => this.error.set(error),
    });
  }

  private payWithEmail(order: CheckoutOrder, plan: SubscriptionPlan): void {
    this.users.me().subscribe({
      next: user => void this.launchCheckout(order, user.email, plan),
      error: (error: ApiError) => this.error.set(error),
    });
  }

  private async launchCheckout(order: CheckoutOrder, email: string, plan: SubscriptionPlan): Promise<void> {
    try {
      await this.razorpay.open(order, email, () => this.onPaid(plan));
    } catch {
      this.paymentMessage.set('The payment window could not open. Please try again.');
    }
  }

  private onPaid(plan: SubscriptionPlan): void {
    this.paymentMessage.set(`Payment received for ${plan.name}. Your plan updates in a moment.`);
    void this.router.navigate(['/dashboard']);
  }
}
