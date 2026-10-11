import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  IonContent,
  IonIcon,
  IonRefresher,
  IonRefresherContent,
  RefresherCustomEvent,
  AlertController,
  ToastController,
} from '@ionic/angular';
import { addIcons } from 'ionicons';
import {
  shieldCheckmark,
  shieldHalf,
  shield,
  warning,
  documentText,
  lockClosed,
  cog,
  refresh,
  search,
  filter,
  time,
  person,
  school,
  statsChart,
  alertCircle,
  chevronForward,
  ellipsisVertical,
  people,
} from 'ionicons/icons';
import { Subscription, interval, switchMap, startWith, of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { GetExamSnapshotsUseCase } from '../../../application/use-cases/oversight/get-exam-snapshots.use-case';
import { GetExamTimelineUseCase } from '../../../application/use-cases/oversight/get-exam-timeline.use-case';
import { GetAttemptIntegrityUseCase } from '../../../application/use-cases/oversight/get-attempt-integrity.use-case';
import { GetExamAverageUseCase } from '../../../application/use-cases/evaluator/get-exam-average.use-case';
import { GetExamResultsUseCase } from '../../../application/use-cases/evaluator/get-exam-results.use-case';
import { RevokeExamAttemptUseCase } from '../../../application/use-cases/evaluator/revoke-exam-attempt.use-case';
import { OversightStompService } from '../../../infrastructure/http/oversight-stomp.service';
import {
  ExamIntegritySummary,
  LogEntry,
  IntegritySnapshot,
  RiskLevel,
  RiskLevelConfig,
  SeverityConfig,
  LogSourceConfig,
} from '../../../domain/models/oversight/oversight.model';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';

@Component({
  selector: 'app-oversight',
  templateUrl: './oversight.page.html',
  styleUrls: ['./oversight.page.scss'],
  standalone: true,
  imports: [
    CommonModule,
    IonContent,
    IonIcon,
    IonRefresher,
    IonRefresherContent,
    AppHeaderComponent,
  ],
})
export class OversightPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly getExamSnapshots = inject(GetExamSnapshotsUseCase);
  private readonly getExamTimeline = inject(GetExamTimelineUseCase);
  private readonly getAttemptIntegrity = inject(GetAttemptIntegrityUseCase);
  private readonly getExamAverage = inject(GetExamAverageUseCase);
  private readonly getExamResults = inject(GetExamResultsUseCase);
  private readonly revokeExamAttempt = inject(RevokeExamAttemptUseCase);
  private readonly oversightStomp = inject(OversightStompService);
  private readonly alertController = inject(AlertController);
  private readonly toastCtrl = inject(ToastController);

  // State
  examId = signal<string>('');
  examAverage = signal<ExamIntegritySummary | null>(null);
  snapshots = signal<IntegritySnapshot[]>([]);
  timeline = signal<LogEntry[]>([]);
  /** intento → nombre del alumno; oversight solo trae el studentId. */
  studentNames = signal<Map<string, string>>(new Map());
  selectedAttempt = signal<IntegritySnapshot | null>(null);
  activeTab = signal<'summary' | 'timeline' | 'students'>('summary');
  searchTerm = signal<string>('');
  filterSeverity = signal<string>('all');
  filterSource = signal<string>('all');
  isLoading = signal<boolean>(false);
  lastRefresh = signal<Date | null>(null);
  autoRefreshSub: Subscription | null = null;
  private stopRealtime: (() => void) | null = null;
  readonly revokingAttemptId = signal<string | null>(null);

  // Computed
  examTitle = computed(() => this.examAverage()?.examTitle ?? '');

  /**
   * Resumen del examen: título y conteos de intentos vienen del promedio de
   * evaluator; los conteos de riesgo y el índice promedio salen de los
   * snapshots de oversight (el backend no expone este agregado).
   */
  stats = computed<ExamIntegritySummary | null>(() => {
    const average = this.examAverage();
    const snapshots = this.snapshots();
    if (!average) return null;
    const high = snapshots.filter((s) => s.riskLevel === 'RED').length;
    const medium = snapshots.filter((s) => s.riskLevel === 'YELLOW').length;
    const low = snapshots.filter((s) => s.riskLevel === 'GREEN').length;
    const averageIntegrityIndex = snapshots.length
      ? Math.round(snapshots.reduce((sum, s) => sum + s.integrityIndex, 0) / snapshots.length)
      : 0;
    return {
      examId: average.examId,
      examTitle: average.examTitle,
      totalAttempts: average.totalAttempts,
      gradedAttempts: average.gradedAttempts,
      pendingReviewCount: average.pendingReviewCount,
      averageIntegrityIndex,
      highRiskCount: high,
      mediumRiskCount: medium,
      lowRiskCount: low,
    };
  });

  /** Línea de tiempo con el nombre del alumno unido por intento. */
  timelineWithNames = computed(() => {
    const names = this.studentNames();
    return this.timeline().map((entry) => ({
      ...entry,
      studentName: names.get(entry.attemptId) ?? `Alumno ${entry.studentId.slice(0, 8)}`,
    }));
  });

  filteredTimeline = computed(() => {
    let result = this.timelineWithNames();
    const term = this.searchTerm().toLowerCase();
    const severity = this.filterSeverity();
    const source = this.filterSource();

    if (term) {
      result = result.filter(
        (entry) =>
          entry.studentName.toLowerCase().includes(term) ||
          entry.message.toLowerCase().includes(term)
      );
    }
    if (severity !== 'all') {
      result = result.filter((entry) => entry.severity === severity);
    }
    if (source !== 'all') {
      result = result.filter((entry) => entry.source === source);
    }
    return result;
  });

  studentsWithRisk = computed(() => {
    const timeline = this.timelineWithNames();
    const snapshotsByAttempt = new Map(this.snapshots().map((s) => [s.attemptId, s]));

    // Agrupa los eventos por alumno y usa el snapshot como verdad de riesgo e
    // índice; el conteo de alertas/bloqueos se deriva de la línea de tiempo.
    const studentMap = new Map<string, {
      attemptId: string;
      studentId: string;
      studentName: string;
      totalAlerts: number;
      totalBlocks: number;
      severities: string[];
      lastEventAt: string | null;
    }>();

    for (const entry of timeline) {
      const key = entry.studentId;
      if (!studentMap.has(key)) {
        studentMap.set(key, {
          attemptId: entry.attemptId,
          studentId: entry.studentId,
          studentName: entry.studentName,
          totalAlerts: 0,
          totalBlocks: 0,
          severities: [],
          lastEventAt: entry.occurredAt,
        });
      }
      const student = studentMap.get(key)!;
      student.totalAlerts++;
      if (entry.source === 'SAFEGUARD') student.totalBlocks++;
      student.severities.push(entry.severity);
      if (!student.lastEventAt || entry.occurredAt > student.lastEventAt) {
        student.lastEventAt = entry.occurredAt;
      }
    }

    return Array.from(studentMap.values()).map((student) => {
      const snapshot = snapshotsByAttempt.get(student.attemptId);
      const hasHigh = student.severities.includes('HIGH');
      const hasMedium = student.severities.includes('MEDIUM');

      // El snapshot manda; si el alumno no tiene eventos, se estima con las
      // severidades y los contadores.
      let riskLevel: RiskLevel = 'GREEN';
      if (hasHigh) riskLevel = 'RED';
      else if (hasMedium) riskLevel = 'YELLOW';
      let integrityIndex = Math.max(0, 100 - student.totalAlerts * 5 - student.totalBlocks * 10);
      if (snapshot) {
        riskLevel = snapshot.riskLevel;
        integrityIndex = snapshot.integrityIndex;
      }

      return {
        attemptId: student.attemptId,
        studentId: student.studentId,
        studentName: student.studentName,
        totalAlerts: student.totalAlerts,
        totalBlocks: student.totalBlocks,
        riskLevel,
        integrityIndex,
        lastEventAt: student.lastEventAt,
        riskConfig: RiskLevelConfig[riskLevel],
      };
    });
  });

  constructor() {
    addIcons({
      shieldCheckmark,
      shieldHalf,
      shield,
      warning,
      documentText,
      lockClosed,
      cog,
      refresh,
      search,
      filter,
      time,
      person,
      school,
      statsChart,
      alertCircle,
      chevronForward,
      ellipsisVertical,
      people,
    });
  }

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('examId');
      if (id) {
        this.examId.set(id);
        this.stopRealtime?.();
        this.stopRealtime = this.oversightStomp.subscribeToExam(id, () => this.loadData());
        this.loadData();
        this.startAutoRefresh();
      }
    });
  }

  ngOnDestroy() {
    this.stopAutoRefresh();
    this.stopRealtime?.();
    this.stopRealtime = null;
  }

  private startAutoRefresh() {
    // Auto-refresh every 30 seconds
    this.autoRefreshSub = interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => this.loadData())
      )
      .subscribe();
  }

  private stopAutoRefresh() {
    this.autoRefreshSub?.unsubscribe();
    this.autoRefreshSub = null;
  }

  loadData() {
    if (!this.examId()) return of(void 0);
    this.isLoading.set(true);

    this.getExamSnapshots.execute(this.examId()).subscribe({
      next: (data: IntegritySnapshot[]) => this.snapshots.set(data),
      error: (err: unknown) => console.error('Error loading snapshots:', err),
    });

    this.getExamTimeline.execute(this.examId()).subscribe({
      next: (data: LogEntry[]) => this.timeline.set(data),
      error: (err: unknown) => console.error('Error loading timeline:', err),
    });

    // Título y conteos de intentos: evaluator (staff-only) expone lo que
    // oversight no calcula.
    this.getExamAverage.execute(this.examId()).subscribe({
      next: (average) => {
        this.examAverage.set({
          examId: average.examId,
          examTitle: average.examTitle,
          totalAttempts: average.totalAttempts,
          gradedAttempts: average.gradedAttempts,
          pendingReviewCount: average.pendingReviewCount,
          averageIntegrityIndex: 0,
          highRiskCount: 0,
          mediumRiskCount: 0,
          lowRiskCount: 0,
        });
        this.isLoading.set(false);
        this.lastRefresh.set(new Date());
      },
      error: (err: unknown) => {
        console.error('Error loading exam average:', err);
        this.isLoading.set(false);
        this.showToast('Error al cargar el resumen del examen', 'danger');
      },
    });

    // Nombres de alumno por intento (oversight solo trae el id).
    this.getExamResults.execute(this.examId()).subscribe({
      next: (results) => {
        const names = new Map<string, string>();
        for (const result of results) {
          const name =
            `${result.studentFirstName ?? ''} ${result.studentLastName ?? ''}`.trim() ||
            result.studentEmail ||
            `Alumno ${result.studentId.slice(0, 8)}`;
          names.set(result.attemptId, name);
        }
        this.studentNames.set(names);
      },
      error: (err: unknown) => console.error('Error loading results:', err),
    });

    return of(void 0);
  }

  onRefresh(event: RefresherCustomEvent): void {
    this.loadData();
    setTimeout(() => event.target.complete(), 1000);
  }

  // Los manejadores reciben el valor directo: la plantilla usa elementos HTML
  // nativos, no componentes Ionic (antes se construían eventos sintéticos
  // `{ detail: { value } }` para imitar la API de Ionic).
  setTab(tab: 'summary' | 'timeline' | 'students'): void {
    this.activeTab.set(tab);
  }

  onSearch(value: string): void {
    this.searchTerm.set(value ?? '');
  }

  onSeverityChange(value: string): void {
    this.filterSeverity.set(value);
  }

  onSourceChange(value: string): void {
    this.filterSource.set(value);
  }

  /**
   * Detalle de integridad de un intento. El snapshot del backend no incluye el
   * nombre del alumno, así que se toma de la fila que se clicó.
   */
  viewAttemptDetail(attemptId: string, studentName: string): void {
    this.getAttemptIntegrity.execute(attemptId).subscribe({
      next: (snapshot: IntegritySnapshot | null) => {
        this.selectedAttempt.set(snapshot ? { ...snapshot, studentName } : null);
      },
      error: (err: unknown) => {
        console.error('Error loading attempt integrity:', err);
        this.showToast('Error al cargar detalles del intento', 'danger');
      },
    });
  }

  closeAttemptDetail(): void {
    this.selectedAttempt.set(null);
  }

  async requestRevokeAttempt(attemptId: string): Promise<void> {
    if (this.revokingAttemptId()) return;
    const alert = await this.alertController.create({
      header: 'Revocar intento',
      message: 'El intento se cerrará inmediatamente y quedará reprobado. Esta acción no se puede deshacer.',
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        { text: 'Revocar y reprobar', role: 'destructive', handler: () => this.revokeAttemptNow(attemptId) },
      ],
    });
    await alert.present();
  }

  private revokeAttemptNow(attemptId: string): void {
    if (this.revokingAttemptId()) return;
    this.revokingAttemptId.set(attemptId);
    this.revokeExamAttempt.execute(attemptId).subscribe({
      next: () => {
        this.revokingAttemptId.set(null);
        this.showToast('El intento fue revocado y quedó reprobado.', 'success');
        this.loadData();
      },
      error: (err: unknown) => {
        console.error('Error al revocar el intento:', err);
        this.revokingAttemptId.set(null);
        this.showToast('No se pudo revocar el intento.', 'danger');
      },
    });
  }

  getRiskColor(riskLevel: RiskLevel): 'success' | 'warning' | 'danger' | 'secondary' {
    switch (riskLevel) {
      case 'GREEN':
        return 'success';
      case 'YELLOW':
        return 'warning';
      case 'RED':
        return 'danger';
      default:
        return 'secondary';
    }
  }

  getSeverityColor(severity: string): string {
    const config = SeverityConfig[severity as keyof typeof SeverityConfig];
    // 'secondary' porque es la única clase text-bg-* garantizada por Bootstrap;
    // un valor inventado deja la insignia sin estilo.
    return config?.color || 'secondary';
  }

  getSourceColor(source: string): string {
    switch (source) {
      case 'SAFEGUARD': return 'primary';
      case 'SENTINEL': return 'warning';
      case 'EVALUATOR': return 'success';
      default: return 'medium';
    }
  }

  getSourceIcon(source: string): string {
    const config = LogSourceConfig[source as keyof typeof LogSourceConfig];
    return config?.icon || 'help';
  }

  // Expose LogSourceConfig for template access
  get logSourceConfig() {
    return LogSourceConfig;
  }

  // Expose RiskLevelConfig for template access
  get riskLevelConfig() {
    return RiskLevelConfig;
  }

  formatTime(dateString: string): string {
    return new Date(dateString).toLocaleTimeString();
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }

  formatDateTime(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  private async showToast(message: string, color: 'success' | 'danger' | 'warning' = 'success') {
    const toast = await this.toastCtrl.create({
      message,
      duration: 3000,
      color,
      position: 'bottom',
    });
    await toast.present();
  }

  goBack() {
    this.router.navigate(['/exams']);
  }
}
