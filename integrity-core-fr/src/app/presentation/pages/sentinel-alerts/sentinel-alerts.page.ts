import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { IonContent, IonIcon } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { alertCircleOutline, filterOutline, refreshOutline, shieldCheckmarkOutline } from 'ionicons/icons';
import { finalize } from 'rxjs';
import { ListExamsUseCase } from '../../../application/use-cases/exam/list-exams.use-case';
import { GetSentinelAlertsUseCase } from '../../../application/use-cases/sentinel/get-sentinel-alerts.use-case';
import { ResolveSentinelAlertUseCase } from '../../../application/use-cases/sentinel/resolve-sentinel-alert.use-case';
import { Exam } from '../../../domain/models/exam/exam.model';
import {
  SentinelAlert,
  SentinelAlertFilters,
  SentinelSeverity,
  SentinelSeverityConfig,
} from '../../../domain/models/sentinel/sentinel-alert.model';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-sentinel-alerts',
  templateUrl: './sentinel-alerts.page.html',
  styleUrls: ['./sentinel-alerts.page.scss'],
  standalone: true,
  imports: [CommonModule, IonContent, IonIcon, AppHeaderComponent],
})
export class SentinelAlertsPage implements OnInit {
  private readonly listExamsUseCase = inject(ListExamsUseCase);
  private readonly getAlertsUseCase = inject(GetSentinelAlertsUseCase);
  private readonly resolveAlertUseCase = inject(ResolveSentinelAlertUseCase);

  readonly exams = signal<Exam[]>([]);
  readonly selectedExamId = signal<string>('');
  readonly severity = signal<'all' | SentinelSeverity>('all');
  readonly fromDate = signal<string>('');
  readonly toDate = signal<string>('');
  readonly alerts = signal<SentinelAlert[]>([]);
  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly page = signal<number>(1);
  readonly resolvingId = signal<string | null>(null);

  readonly pageSize = PAGE_SIZE;
  readonly severityOptions: Array<'all' | SentinelSeverity> = ['all', 'LOW', 'MEDIUM', 'HIGH'];
  readonly severityConfig = SentinelSeverityConfig;

  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.alerts().length / PAGE_SIZE))
  );

  readonly pagedAlerts = computed(() => {
    const start = (this.page() - 1) * PAGE_SIZE;
    return this.alerts().slice(start, start + PAGE_SIZE);
  });

  constructor() {
    addIcons({ alertCircleOutline, filterOutline, refreshOutline, shieldCheckmarkOutline });
  }

  ngOnInit(): void {
    this.loadExams();
  }

  loadExams(): void {
    this.loading.set(true);
    this.listExamsUseCase.execute().subscribe({
      next: (exams) => {
        this.exams.set(exams);
        if (exams.length > 0) {
          this.selectedExamId.set(this.selectedExamId() || exams[0].id);
          this.loadAlerts();
        } else {
          this.alerts.set([]);
          this.loading.set(false);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(
          (err.error as { message?: string } | undefined)?.message ??
            'No se pudieron cargar los exámenes.'
        );
      },
    });
  }

  onExamChange(event: Event): void {
    this.selectedExamId.set((event.target as HTMLSelectElement).value);
    this.page.set(1);
    this.loadAlerts();
  }

  onSeverityChange(event: Event): void {
    this.severity.set((event.target as HTMLSelectElement).value as 'all' | SentinelSeverity);
  }

  onFromDateChange(event: Event): void {
    this.fromDate.set((event.target as HTMLInputElement).value);
  }

  onToDateChange(event: Event): void {
    this.toDate.set((event.target as HTMLInputElement).value);
  }

  applyFilters(): void {
    this.page.set(1);
    this.loadAlerts();
  }

  clearFilters(): void {
    this.severity.set('all');
    this.fromDate.set('');
    this.toDate.set('');
    this.page.set(1);
    this.loadAlerts();
  }

  loadAlerts(): void {
    const examId = this.selectedExamId();
    if (!examId) {
      this.alerts.set([]);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.getAlertsUseCase
      .execute(examId, this.currentFilters())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (alerts) => this.alerts.set(alerts),
        error: (err: HttpErrorResponse) => {
          this.alerts.set([]);
          this.errorMessage.set(this.errorMessageFrom(err));
        },
      });
  }

  resolve(alert: SentinelAlert): void {
    if (alert.resolved || this.resolvingId()) {
      return;
    }
    this.resolvingId.set(alert.id);
    this.resolveAlertUseCase
      .execute(alert.id)
      .pipe(finalize(() => this.resolvingId.set(null)))
      .subscribe({
        next: () => {
          this.alerts.update((items) =>
            items.map((item) =>
              item.id === alert.id ? { ...item, resolved: true } : item
            )
          );
        },
        error: (err: HttpErrorResponse) => this.errorMessage.set(this.errorMessageFrom(err)),
      });
  }

  previousPage(): void {
    if (this.page() > 1) {
      this.page.update((page) => page - 1);
    }
  }

  nextPage(): void {
    if (this.page() < this.totalPages()) {
      this.page.update((page) => page + 1);
    }
  }

  severityLabel(severity: SentinelSeverity): string {
    return SentinelSeverityConfig[severity]?.label ?? severity;
  }

  severityBadgeClass(severity: SentinelSeverity): string {
    return `text-bg-${SentinelSeverityConfig[severity]?.color ?? 'secondary'}`;
  }

  studentLabel(alert: SentinelAlert): string {
    return alert.studentName || `Alumno ${alert.studentId.slice(0, 8)}`;
  }

  examLabel(alert: SentinelAlert): string {
    return alert.examName || this.exams().find((exam) => exam.id === alert.examId)?.title || '—';
  }

  private currentFilters(): SentinelAlertFilters {
    const severity = this.severity();
    return {
      severity: severity === 'all' ? undefined : severity,
      from: this.toIsoStart(this.fromDate()),
      to: this.toIsoEnd(this.toDate()),
    };
  }

  private toIsoStart(value: string): string | undefined {
    return value ? new Date(`${value}T00:00:00`).toISOString() : undefined;
  }

  private toIsoEnd(value: string): string | undefined {
    return value ? new Date(`${value}T23:59:59.999`).toISOString() : undefined;
  }

  private errorMessageFrom(err: HttpErrorResponse): string {
    return (
      (err.error as { message?: string } | undefined)?.message ??
      'No se pudieron cargar las alertas.'
    );
  }
}
