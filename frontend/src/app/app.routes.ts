import { Routes } from '@angular/router';

export const routes: Routes = [
	{ path: '', loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent) },
	{
		path: 'lesson/:id',
		loadComponent: () => import('./features/lesson/lesson-player/lesson-player.component').then((m) => m.LessonPlayerComponent),
	},
	{ path: '**', redirectTo: '' },
];
