import { HttpRequest } from '@angular/common/http';
import { CheckoutOrder, SubscriptionPlan } from '../models/subscription.model';
import { ExtractResult, LessonHistoryPage, LessonResponse } from '../models/lesson-api.model';
import { AuthTokens, Usage, User } from '../models/user.model';

export interface MockRoute {
  method: string;
  path: string;
  status?: number;
  body: (req: HttpRequest<unknown>) => unknown;
}

const TOKENS: AuthTokens = { accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token' };

const USER: User = {
  id: '00000000-0000-0000-0000-000000000001',
  email: 'student@example.com',
  name: 'Asha',
  language: 'en',
  plan: 'FREE',
  isMinor: false,
  emailVerified: true,
  guardianConsentAt: null,
};

const USAGE: Usage = { plan: 'FREE', sessionsToday: 1, sessionLimit: 3, maxSessionMinutes: 5 };

const LESSON: LessonResponse = {
  lessonId: '11111111-1111-1111-1111-111111111111',
  title: 'Projectile Motion: Angled Launch',
  subject: 'PHYSICS',
  topic: 'Kinematics',
  difficulty: 'MEDIUM',
  totalSteps: 4,
  estimatedDurationSeconds: 180,
};

const HISTORY: LessonHistoryPage = {
  content: [
    { lessonId: LESSON.lessonId, title: LESSON.title, subject: 'PHYSICS', createdAt: '2026-10-08T10:00:00Z' },
    { lessonId: '22222222-2222-2222-2222-222222222222', title: 'Balancing Redox Equations', subject: 'CHEMISTRY', createdAt: '2026-10-07T18:30:00Z' },
  ],
  page: 0,
  totalPages: 1,
};

const EXTRACT: ExtractResult = {
  problemText: 'A particle is projected at 60 degrees with speed 20 m/s. Find the time of flight.',
  confidence: 'HIGH',
  hasDiagram: false,
};

const PLANS: SubscriptionPlan[] = [
  { id: 'free', name: 'Free', plan: 'FREE', priceInr: 0, sessionsPerDay: 3, sessionMinutes: 5 },
  { id: 'pro', name: 'Pro', plan: 'PRO', priceInr: 199, sessionsPerDay: 30, sessionMinutes: 20 },
  { id: 'pro-plus', name: 'Pro+', plan: 'PRO_PLUS', priceInr: 399, sessionsPerDay: null, sessionMinutes: 30 },
];

const CHECKOUT: CheckoutOrder = { orderId: 'order_mock_001', keyId: 'rzp_test_mock', amountPaise: 19900, currency: 'INR' };

const NO_CONTENT = 204;

export const MOCK_ROUTES: MockRoute[] = [
  { method: 'POST', path: '/auth/login', body: () => TOKENS },
  { method: 'POST', path: '/auth/google', body: () => TOKENS },
  { method: 'POST', path: '/auth/refresh', body: () => TOKENS },
  { method: 'POST', path: '/auth/register', status: 201, body: () => null },
  { method: 'GET', path: '/auth/verify-email', body: () => null },
  { method: 'POST', path: '/auth/guardian-consent/confirm', body: () => null },
  { method: 'GET', path: '/users/me', body: () => USER },
  { method: 'GET', path: '/users/me/usage', body: () => USAGE },
  { method: 'PUT', path: '/users/me/preferences', status: NO_CONTENT, body: () => null },
  { method: 'GET', path: '/users/me/export', body: () => new Blob(['{"user":"mock"}'], { type: 'application/json' }) },
  { method: 'DELETE', path: '/users/me', status: NO_CONTENT, body: () => null },
  { method: 'GET', path: '/lessons/history', body: () => HISTORY },
  { method: 'POST', path: '/lessons/generate', status: 201, body: () => LESSON },
  { method: 'POST', path: '/lessons/extract', body: () => EXTRACT },
  { method: 'GET', path: '/subscriptions/plans', body: () => PLANS },
  { method: 'POST', path: '/subscriptions/checkout', body: () => CHECKOUT },
];
