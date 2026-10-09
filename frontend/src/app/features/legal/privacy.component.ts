import { Component } from '@angular/core';

/** Placeholder text until counsel signs off the final policy (spec 2.7, TODO-1 in section 15). */
@Component({
  selector: 'app-privacy',
  templateUrl: './legal-page.component.html',
  styleUrl: './legal-page.component.scss',
})
export class PrivacyComponent {
  protected readonly title = 'Privacy Policy';
  protected readonly body = [
    'PrepAI stores your email, name, language, plan and age group. Questions you ask are stored so that you can see your lesson history.',
    'Under-18 accounts need guardian consent before lessons start. Parents can confirm consent from the link we email them.',
    'Images you upload are read in memory and are not stored. Only the problem text is sent to our AI providers, never your name or email.',
    'You can download your data or delete your account from the Profile page.',
  ];
}
