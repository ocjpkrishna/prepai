import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { SubscriptionPlan } from '../../core/models/subscription.model';
import { RazorpayCheckoutService } from '../../core/services/razorpay-checkout.service';
import { SubscriptionService } from '../../core/services/subscription.service';
import { UserService } from '../../core/services/user.service';
import { PricingComponent } from './pricing.component';

describe('PricingComponent', () => {
  const plans: SubscriptionPlan[] = [
    { id: 'free', name: 'Free', plan: 'FREE', priceInr: 0, sessionsPerDay: 3, sessionMinutes: 5 },
    { id: 'pro', name: 'Pro', plan: 'PRO', priceInr: 199, sessionsPerDay: 30, sessionMinutes: 20 },
  ];
  let loggedIn: boolean;
  let router: Router;
  let checkout: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    loggedIn = false;
    checkout = vi.fn().mockReturnValue(of({ orderId: 'o', keyId: 'k', amountPaise: 19900, currency: 'INR' }));
    TestBed.configureTestingModule({
      imports: [PricingComponent],
      providers: [
        provideRouter([]),
        { provide: SubscriptionService, useValue: { plans: () => of(plans), checkout } },
        { provide: UserService, useValue: { me: () => of({ email: 'a@b.c' }) } },
        { provide: RazorpayCheckoutService, useValue: { open: vi.fn().mockResolvedValue(undefined) } },
        { provide: AuthService, useValue: { isLoggedIn: () => loggedIn } },
      ],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('sends a logged-out visitor who picks a paid plan to sign up, without a checkout', () => {
    const fixture = TestBed.createComponent(PricingComponent);
    fixture.detectChanges();
    fixture.componentInstance['choose'](plans[1]);
    expect(checkout).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/register']);
  });

  it('starts a checkout for a logged-in student who picks a paid plan', () => {
    loggedIn = true;
    const fixture = TestBed.createComponent(PricingComponent);
    fixture.detectChanges();
    fixture.componentInstance['choose'](plans[1]);
    expect(checkout).toHaveBeenCalledWith('pro');
  });
});
