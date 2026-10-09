import { TestBed } from '@angular/core/testing';
import { PrivacyComponent } from './privacy.component';
import { TermsComponent } from './terms.component';

describe('Legal pages', () => {
  it('mark the Privacy Policy as a draft until counsel signs off', () => {
    const fixture = TestBed.createComponent(PrivacyComponent);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Draft text');
  });

  it('mark the Terms as a draft until counsel signs off', () => {
    const fixture = TestBed.createComponent(TermsComponent);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Draft text');
  });
});
