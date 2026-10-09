import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./register/register.component').then(m => m.RegisterComponent),
  },
  {
    path: 'verify-email',
    loadComponent: () => import('./verify-email/verify-email.component').then(m => m.VerifyEmailComponent),
  },
  {
    path: 'guardian-pending',
    loadComponent: () => import('./guardian-pending/guardian-pending.component').then(m => m.GuardianPendingComponent),
  },
  {
    path: 'guardian-consent',
    loadComponent: () => import('./guardian-consent/guardian-consent.component').then(m => m.GuardianConsentComponent),
  },
];
