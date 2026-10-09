import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ApiErrorComponent } from './shared/components/api-error/api-error.component';
import { ErrorBoundaryComponent } from './shared/components/error-boundary/error-boundary.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { NavbarComponent } from './shared/components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent, FooterComponent, ErrorBoundaryComponent],
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {}
