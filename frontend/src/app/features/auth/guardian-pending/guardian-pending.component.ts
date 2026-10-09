import { Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';

/** Shown to an under-18 student until their guardian confirms consent (spec 2.7, CONSENT_REQUIRED). */
@Component({
  selector: 'app-guardian-pending',
  imports: [RouterLink, MatButton],
  templateUrl: './guardian-pending.component.html',
  styleUrl: '../auth-form.scss',
})
export class GuardianPendingComponent {}
