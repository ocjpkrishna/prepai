import { Component, input } from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

/** Spinner with a line of text. Used for "PrepAI is thinking..." while a lesson is generated. */
@Component({
  selector: 'app-loading-spinner',
  imports: [MatProgressSpinner],
  templateUrl: './loading-spinner.component.html',
  styleUrl: './loading-spinner.component.scss',
})
export class LoadingSpinnerComponent {
  readonly label = input('Loading...');
}
