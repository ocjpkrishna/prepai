import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding, withHashLocation } from '@angular/router';
import { routes } from './app.routes';
import { LessonSource } from './core/services/lesson-source';
import { MockLessonSource } from './core/services/mock-lesson-source';

export const appConfig: ApplicationConfig = {
	providers: [
		provideBrowserGlobalErrorListeners(),
		provideRouter(routes, withHashLocation(), withComponentInputBinding()),
		{ provide: LessonSource, useClass: MockLessonSource },
	],
};
