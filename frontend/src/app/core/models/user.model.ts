export type Plan = 'FREE' | 'PRO' | 'PRO_PLUS';

export interface User {
  id: string;
  email: string;
  name: string;
  language: 'en' | 'hi';
  plan: Plan;
  isMinor: boolean;
  emailVerified: boolean;
  guardianConsentAt: string | null;
}

export interface Usage {
  plan: Plan;
  sessionsToday: number;
  sessionLimit: number | null;
  maxSessionMinutes: number;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  dateOfBirth: string;
  guardianEmail: string | null;
  termsAccepted: true;
  language: 'en' | 'hi';
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}
