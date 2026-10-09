import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { authInterceptor } from '../../core/auth/auth.interceptor';
import { TokenService } from '../../core/auth/token.service';
import { errorInterceptor } from '../../core/interceptors/error.interceptor';
import { RazorpayCheckoutService } from '../../core/services/razorpay-checkout.service';
import { PricingComponent } from './pricing.component';

const PLANS_URL = '/api/v1/subscriptions/plans';
const CHECKOUT_URL = '/api/v1/subscriptions/checkout';
const ME_URL = '/api/v1/users/me';

const PLANS = [
  { id: 'free', name: 'Free', plan: 'FREE', priceInr: 0, sessionsPerDay: 3, sessionMinutes: 5 },
  { id: 'pro', name: 'Pro', plan: 'PRO', priceInr: 199, sessionsPerDay: 30, sessionMinutes: 20 },
];
const ORDER = { orderId: 'order_1', keyId: 'rzp_test_1', amountPaise: 19900, currency: 'INR' };
const FREE_BUTTON = 0;
const PRO_BUTTON = 1;

describe('pricing flow (mocked HTTP)', () => {
  let fixture: ComponentFixture<PricingComponent>;
  let backend: HttpTestingController;
  let router: Router;
  let razorpay: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    razorpay = { open: vi.fn().mockResolvedValue(undefined) };
    TestBed.configureTestingModule({
      imports: [PricingComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor, authInterceptor])),
        provideHttpClientTesting(),
        { provide: RazorpayCheckoutService, useValue: razorpay },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  function openPage(): void {
    fixture = TestBed.createComponent(PricingComponent);
    fixture.detectChanges();
    backend.expectOne(PLANS_URL).flush(PLANS);
    fixture.detectChanges();
  }

  function chooseButton(index: number): void {
    const buttons = fixture.nativeElement.querySelectorAll(
      'button',
    ) as NodeListOf<HTMLButtonElement>;
    buttons[index].click();
    fixture.detectChanges();
  }

  function logIn(): void {
    TestBed.inject(TokenService).save({ accessToken: 'student-token', refreshToken: 'refresh' });
  }

  it('shows every plan the API returns', () => {
    openPage();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Free');
    expect(text).toContain('₹199 / month');
  });

  it('shows the error when the plans cannot be loaded', () => {
    fixture = TestBed.createComponent(PricingComponent);
    fixture.detectChanges();
    backend
      .expectOne(PLANS_URL)
      .flush(
        { code: 'INTERNAL_ERROR', message: 'boom' },
        { status: 500, statusText: 'Server Error' },
      );
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-api-error')).not.toBeNull();
  });

  it('sends a logged-out visitor who picks a paid plan to register, without a checkout call', () => {
    openPage();
    chooseButton(PRO_BUTTON);

    expect(router.navigate).toHaveBeenCalledWith(['/register']);
  });

  it('sends a student who picks the free plan to the lesson form', () => {
    logIn();
    openPage();
    chooseButton(FREE_BUTTON);

    expect(router.navigate).toHaveBeenCalledWith(['/lessons/new']);
  });

  it('starts a Razorpay checkout for a logged-in student who picks a paid plan', () => {
    logIn();
    openPage();
    chooseButton(PRO_BUTTON);

    const checkout = backend.expectOne(CHECKOUT_URL);
    expect(checkout.request.body).toEqual({ planId: 'pro', paymentMethod: 'razorpay' });
    checkout.flush(ORDER);
    backend.expectOne(ME_URL).flush({ email: 'asha@example.com' });

    expect(razorpay.open).toHaveBeenCalledWith(ORDER, 'asha@example.com', expect.any(Function));
  });

  it('confirms the payment and returns the student to the dashboard', () => {
    logIn();
    openPage();
    chooseButton(PRO_BUTTON);
    backend.expectOne(CHECKOUT_URL).flush(ORDER);
    backend.expectOne(ME_URL).flush({ email: 'asha@example.com' });

    const onPaid = razorpay.open.mock.calls[0][2] as () => void;
    onPaid();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Payment received for Pro');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('shows the error and opens no payment window when checkout fails', () => {
    logIn();
    openPage();
    chooseButton(PRO_BUTTON);
    backend
      .expectOne(CHECKOUT_URL)
      .flush(
        { code: 'INTERNAL_ERROR', message: 'boom' },
        { status: 500, statusText: 'Server Error' },
      );
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-api-error')).not.toBeNull();
    expect(razorpay.open).not.toHaveBeenCalled();
  });
});
