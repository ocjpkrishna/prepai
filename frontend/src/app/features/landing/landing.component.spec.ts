import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { SubscriptionService } from '../../core/services/subscription.service';
import { LandingComponent } from './landing.component';

describe('LandingComponent', () => {
  it('shows the hero, the feature cards, the demo placeholder and the plans', () => {
    const plans = [{ id: 'pro', name: 'Pro', plan: 'PRO', priceInr: 199, sessionsPerDay: 30, sessionMinutes: 20 }];
    TestBed.configureTestingModule({
      imports: [LandingComponent],
      providers: [provideRouter([]), { provide: SubscriptionService, useValue: { plans: () => of(plans) } }],
    });
    const fixture = TestBed.createComponent(LandingComponent);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Understand it on the whiteboard');
    expect(element.querySelectorAll('.features article').length).toBe(3);
    expect(element.textContent).toContain('Demo video coming soon');
    expect(element.textContent).toContain('₹199 / month');
  });
});
