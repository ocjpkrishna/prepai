import { Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink, MatButton],
  template: `
    <section class="page">
      <h1>We could not find that page</h1>
      <p class="muted">The link may be out of date. Head back to the start.</p>
      <a matButton="filled" routerLink="/">Back to PrepAI</a>
    </section>
  `,
})
export class NotFoundComponent {}
