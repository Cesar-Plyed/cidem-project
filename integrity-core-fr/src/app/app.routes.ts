import { Routes } from '@angular/router';
import { LoginPage } from './presentation/pages/login/login.page';
import { RegisterPage } from './presentation/pages/register/register.page';
import { roleGuard } from './presentation/guards/role.guard';
import { Role } from './domain/models/role.enum';

export const routes: Routes = [
  {
    path: 'home',
    canActivate: [roleGuard([Role.ADMINISTRATIVO, Role.PROFESOR, Role.ALUMNO])],
    loadComponent: () => import('./home/home.page').then((m) => m.HomePage),
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: 'login',
    loadComponent: () => import('./presentation/pages/login/login.page').then((m) => LoginPage)
  },
  {
    path: 'register',
    loadComponent: () => import('./presentation/pages/register/register.page').then((m) => RegisterPage)
  },
  {
    path: 'exams',
    canActivate: [roleGuard([Role.ADMINISTRATIVO, Role.PROFESOR])],
    loadComponent: () => import('./presentation/pages/exam-list/exam-list.page').then((m) => m.ExamListPage)
  },
  {
    path: 'exams/new',
    canActivate: [roleGuard([Role.ADMINISTRATIVO, Role.PROFESOR])],
    loadComponent: () => import('./presentation/pages/exam-editor/exam-editor.page').then((m) => m.ExamEditorPage)
  },
  {
    path: 'exams/:id/edit',
    canActivate: [roleGuard([Role.ADMINISTRATIVO, Role.PROFESOR])],
    loadComponent: () => import('./presentation/pages/exam-editor/exam-editor.page').then((m) => m.ExamEditorPage)
  },
  {
    path: 'exams/:examId/reviews',
    canActivate: [roleGuard([Role.ADMINISTRATIVO, Role.PROFESOR])],
    loadComponent: () => import('./presentation/pages/exam-reviews/exam-reviews.page').then((m) => m.ExamReviewsPage)
  },
  {
    path: 'exams/:examId/results',
    canActivate: [roleGuard([Role.ADMINISTRATIVO, Role.PROFESOR])],
    loadComponent: () => import('./presentation/pages/exam-results/exam-results.page').then((m) => m.ExamResultsPage)
  },
  {
    path: 'attempts/:attemptId/result',
    canActivate: [roleGuard([Role.ADMINISTRATIVO, Role.PROFESOR, Role.ALUMNO])],
    loadComponent: () => import('./presentation/pages/exam-result/exam-result.page').then((m) => m.ExamResultPage)
  },
  {
    path: 'attempts/:attemptId',
    canActivate: [roleGuard([Role.ALUMNO])],
    loadComponent: () => import('./presentation/pages/exam-attempt/exam-attempt.page').then((m) => m.ExamAttemptPage)
  },
  {
    path: 'exams/:id/take',
    canActivate: [roleGuard([Role.ALUMNO])],
    loadComponent: () => import('./presentation/pages/exam-take/exam-take.page').then((m) => m.ExamTakePage)
  },
  {
    path: 'evaluator-demo',
    canActivate: [roleGuard([Role.ADMINISTRATIVO, Role.PROFESOR])],
    loadComponent: () => import('./presentation/pages/evaluator-demo/evaluator-demo.page').then((m) => m.EvaluatorDemoPage)
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
