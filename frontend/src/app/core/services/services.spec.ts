import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LessonService } from './lesson.service';
import { SubscriptionService } from './subscription.service';
import { UserService } from './user.service';

describe('API services', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('lists history with page, size and subject query parameters', () => {
    TestBed.inject(LessonService).history(1, 10, 'PHYSICS').subscribe();
    const request = backend.expectOne(req => req.url === '/api/v1/lessons/history');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('subject')).toBe('PHYSICS');
    request.flush({ content: [], page: 1, totalPages: 1 });
  });

  it('uploads the extracted photo as the image field of a multipart request', () => {
    TestBed.inject(LessonService).extract(new Blob(['x'], { type: 'image/jpeg' })).subscribe();
    const request = backend.expectOne('/api/v1/lessons/extract');
    expect(request.request.body instanceof FormData).toBe(true);
    expect((request.request.body as FormData).has('image')).toBe(true);
    request.flush({ problemText: 'p', confidence: 'HIGH', hasDiagram: false });
  });

  it('asks for the data export as a file, not JSON', () => {
    TestBed.inject(UserService).exportData().subscribe();
    const request = backend.expectOne('/api/v1/users/me/export');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['{}']));
  });

  it('starts a Razorpay checkout for a plan', () => {
    TestBed.inject(SubscriptionService).checkout('pro').subscribe();
    const request = backend.expectOne('/api/v1/subscriptions/checkout');
    expect(request.request.body).toEqual({ planId: 'pro', paymentMethod: 'razorpay' });
    request.flush({ orderId: 'o', keyId: 'k', amountPaise: 1, currency: 'INR' });
  });
});
