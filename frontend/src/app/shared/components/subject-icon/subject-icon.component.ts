import { Component, computed, input } from '@angular/core';
import { Subject } from '../../../core/models/lesson.model';

const SUBJECT_LABELS: Record<Subject, { icon: string; label: string }> = {
  PHYSICS: { icon: '🧲', label: 'Physics' },
  CHEMISTRY: { icon: '🧪', label: 'Chemistry' },
  MATHEMATICS: { icon: '➗', label: 'Maths' },
};

@Component({
  selector: 'app-subject-icon',
  templateUrl: './subject-icon.component.html',
})
export class SubjectIconComponent {
  readonly subject = input.required<Subject>();

  protected readonly info = computed(() => SUBJECT_LABELS[this.subject()]);
}
