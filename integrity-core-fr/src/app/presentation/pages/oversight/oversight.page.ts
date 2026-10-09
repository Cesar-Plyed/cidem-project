import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  IonContent,
  IonIcon,
  // IonBadge se usa en el HTML que inyecta showAttemptDetail (alert de Ionic),
  // así que debe seguir importado aunque el template ya no lo use.
  IonBadge,
  IonRefresher,
  IonRefresherContent,
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
import { GetExamSummaryUseCase } from '../../../application/use-cases/oversight/get-exam-summary.use-case';
import { GetExamTimelineUseCase } from '../../../application/use-cases/oversight/get-exam-timeline.use-case';
import { GetAttemptIntegrityUseCase } from '../../../application/use-cases/oversight/get-attempt-integrity.use-case';
import { ExamSummary, LogEntry, IntegritySnapshot, RiskLevel, RiskLevelConfig, SeverityConfig, LogSourceConfig } from '../../../domain/models/oversight/oversight.model';
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
    // IonBadge se usa en el HTML que inyecta showAttemptDetail (alert de Ionic),
    // así que debe seguir importado aunque el template ya no lo use.
    IonBadge,
    IonRefresher,
    IonRefresherContent,
    AppHeaderComponent,
  ],
})
export class OversightPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly getExamSummary = inject(GetExamSummaryUseCase);
  private readonly getExamTimeline = inject(GetExamTimelineUseCase);
  private readonly getAttemptIntegrity = inject(GetAttemptIntegrityUseCase);
  private readonly alertCtrl = inject(AlertController);
  private readonly toastCtrl = inject(ToastController);

  // State
  examId = signal<string>('');
  examTitle = signal<string>('');
  summary = signal<ExamSummary[]>([]);
  timeline = signal<LogEntry[]>([]);
  selectedAttempt = signal<IntegritySnapshot | null>(null);
  activeTab = signal<'summary' | 'timeline' | 'students'>('summary');
  searchTerm = signal<string>('');
  filterSeverity = signal<string>('all');
  filterSource = signal<string>('all');
  isLoading = signal<boolean>(false);
  lastRefresh = signal<Date | null>(null);
  autoRefreshSub: Subscription | null = null;

  // Computed
  filteredTimeline = computed(() => {
    let result = this.timeline();
    const term = this.searchTerm().toLowerCase();
    const severity = this.filterSeverity();
    const source = this.filterSource();

    if (term) {
      result = result.filter(
        (entry) =>
          entry.studentName.toLowerCase().includes(term) ||
          entry.eventType.toLowerCase().includes(term) ||
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
    const timeline = this.timeline();
    // Group timeline entries by student
    const studentMap = new Map<string, {
      attemptId: string;
      studentId: string;
      studentName: string;
      totalAlerts: number;
      totalBlocks: number;
      sources: Set<string>;
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
          sources: new Set(),
          severities: [],
          lastEventAt: entry.occurredAt,
        });
      }
      const student = studentMap.get(key)!;
      student.totalAlerts++;
      if (entry.source === 'SAFEGUARD') student.totalBlocks++;
      student.sources.add(entry.source);
      student.severities.push(entry.severity);
      if (!student.lastEventAt || entry.occurredAt > student.lastEventAt) {
        student.lastEventAt = entry.occurredAt;
      }
    }

    // Convert to array and calculate risk level
    return Array.from(studentMap.values()).map((student) => {
      const hasCritical = student.severities.includes('CRITICAL');
      const hasHigh = student.severities.includes('HIGH');
      const hasMedium = student.severities.includes('MEDIUM');

      let riskLevel: RiskLevel = 'GREEN';
      if (hasCritical || hasHigh) riskLevel = 'RED';
      else if (hasMedium) riskLevel = 'YELLOW';

      // Calculate a simple integrity index based on alerts/blocks
      const integrityIndex = Math.max(0, 100 - student.totalAlerts * 5 - student.totalBlocks * 10);

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
        this.loadData();
        this.startAutoRefresh();
      }
    });
  }

  ngOnDestroy() {
    this.stopAutoRefresh();
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

    // Load summary and timeline in parallel
    this.getExamSummary.execute(this.examId()).subscribe({
      next: (data: ExamSummary[]) => {
        this.summary.set(data);
        if (data.length > 0) {
          this.examTitle.set(data[0].examTitle);
        }
        this.isLoading.set(false);
        this.lastRefresh.set(new Date());
      },
      error: (err: unknown) => {
        console.error('Error loading summary:', err);
        this.isLoading.set(false);
        this.showToast('Error al cargar el resumen', 'danger');
      },
    });

    this.getExamTimeline.execute(this.examId()).subscribe({
      next: (data: LogEntry[]) => this.timeline.set(data),
      error: (err: unknown) => console.error('Error loading timeline:', err),
    });

    return of(void 0);
  }

  onRefresh(event: any) {
    this.loadData();
    setTimeout(() => event.target.complete(), 1000);
  }

  onTabChange(event: any) {
    this.activeTab.set(event.detail.value);
  }

  onSearch(event: any) {
    this.searchTerm.set(event.detail.value ?? '');
  }

  onSeverityChange(event: any) {
    this.filterSeverity.set(event.detail.value);
  }

  onSourceChange(event: any) {
    this.filterSource.set(event.detail.value);
  }

  async viewAttemptDetail(attemptId: string) {
    this.isLoading.set(true);
    this.getAttemptIntegrity.execute(attemptId).subscribe({
      next: (snapshot: IntegritySnapshot | null) => {
        this.selectedAttempt.set(snapshot);
        this.isLoading.set(false);
        this.showAttemptDetail(snapshot);
      },
      error: (err: unknown) => {
        console.error('Error loading attempt integrity:', err);
        this.isLoading.set(false);
        this.showToast('Error al cargar detalles del intento', 'danger');
      },
    });
  }

  private async showAttemptDetail(snapshot: IntegritySnapshot | null) {
    if (!snapshot) return;

    const alert = await this.alertCtrl.create({
      header: `Detalles de integridad - ${snapshot.studentName}`,
      message: `
        <div class="detail-content">
          <div class="detail-row">
            <strong>Índice de integridad:</strong>
            <ion-badge color="${this.getRiskColor(snapshot.riskLevel)}">${snapshot.integrityIndex}/100</ion-badge>
          </div>
          <div class="detail-row">
            <strong>Nivel de riesgo:</strong>
            <ion-badge color="${this.getRiskColor(snapshot.riskLevel)}">${RiskLevelConfig[snapshot.riskLevel].label}</ion-badge>
          </div>
          <div class="detail-row">
            <strong>Total alertas:</strong> ${snapshot.totalAlerts}
          </div>
          <div class="detail-row">
            <strong>Total bloqueos:</strong> ${snapshot.totalBlocks}
          </div>
          <div class="detail-row">
            <strong>Último evento:</strong> ${snapshot.lastEventAt ? new Date(snapshot.lastEventAt).toLocaleString() : 'N/A'}
          </div>
          <div class="detail-row">
            <strong>Actualizado:</strong> ${new Date(snapshot.updatedAt).toLocaleString()}
          </div>
        </div>
      `,
      buttons: ['Cerrar'],
      cssClass: 'oversight-detail-alert',
    });
    await alert.present();
  }

  getRiskColor(riskLevel: RiskLevel): 'success' | 'warning' | 'danger' | 'medium' {
    switch (riskLevel) {
      case 'GREEN':
        return 'success';
      case 'YELLOW':
        return 'warning';
      case 'RED':
        return 'danger';
      default:
        return 'medium';
    }
  }

  getSeverityColor(severity: string): string {
    const config = SeverityConfig[severity as keyof typeof SeverityConfig];
    return config?.color || 'medium';
  }

  getSourceColor(source: string): string {
    const config = LogSourceConfig[source as keyof typeof LogSourceConfig];
    // Return a default color based on source
    switch (source) {
      case 'SAFEGUARD': return 'primary';
      case 'SENTINEL': return 'warning';
      case 'EVALUATOR': return 'success';
      case 'SYSTEM': return 'medium';
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