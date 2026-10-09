import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/errors/api-error';
import { SubscriptionPlan } from '../../core/models/subscription.model';
import { SubscriptionService } from '../../core/services/subscription.service';
import { ApiErrorComponent } from '../../shared/components/api-error/api-error.component';
import { PlanCardsComponent } from '../../shared/components/plan-cards/plan-cards.component';

interface Feature {
  title: string;
  text: string;
}

const FEATURES: Feature[] = [
  { title: 'A whiteboard that draws', text: 'Each step is drawn on a board, with a voice that explains it.' },
  { title: 'Built for JEE, NEET and boards', text: 'Physics, Chemistry and Maths, matched to your exam and level.' },
  { title: 'Snap a photo', text: 'Photograph a problem from your book. Check the reading, then get the lesson.' },
];

@Component({
  selector: 'app-landing',
  imports: [RouterLink, MatButton, PlanCardsComponent, ApiErrorComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
})
export class LandingComponent implements OnInit {
  private readonly subscriptions = inject(SubscriptionService);
  private readonly router = inject(Router);

  protected readonly features = FEATURES;
  protected readonly plans = signal<SubscriptionPlan[]>([]);
  protected readonly error = signal<ApiError | null>(null);

  ngOnInit(): void {
    this.subscriptions.plans().subscribe({
      next: plans => this.plans.set(plans),
      error: (error: ApiError) => this.error.set(error),
    });
  }

  protected choosePlan(): void {
    void this.router.navigate(['/register']);
  }
}
