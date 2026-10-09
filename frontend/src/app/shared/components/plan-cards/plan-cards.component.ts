import { Component, input, output } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { SubscriptionPlan } from '../../../core/models/subscription.model';

/** Plan cards used on the landing page and the pricing page. Choosing a plan is the page's decision. */
@Component({
  selector: 'app-plan-cards',
  imports: [MatButton],
  templateUrl: './plan-cards.component.html',
  styleUrl: './plan-cards.component.scss',
})
export class PlanCardsComponent {
  readonly plans = input<SubscriptionPlan[]>([]);
  readonly choose = output<SubscriptionPlan>();

  protected sessionsLabel(plan: SubscriptionPlan): string {
    return plan.sessionsPerDay === null ? 'Unlimited sessions' : `${plan.sessionsPerDay} sessions a day`;
  }
}
