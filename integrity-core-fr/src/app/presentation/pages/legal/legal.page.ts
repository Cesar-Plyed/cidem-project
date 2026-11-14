import { Component, OnInit, inject, signal } from '@angular/core';
import { IonContent } from '@ionic/angular';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';
import { SessionService } from '../../../application/session/session.service';
import { Role } from '../../../domain/models/role.enum';
import { LegalNoticeComponent } from '../../../shared/components/legal-notice/legal-notice.component';
import { ConsentManagerComponent } from '../../../shared/components/consent-manager/consent-manager.component';
import { ConsentManagementComponent } from '../../../shared/components/consent-management/consent-management.component';
import { AccessRequestComponent } from '../../../shared/components/access-request/access-request.component';
import { PortabilityExportComponent } from '../../../shared/components/portability-export/portability-export.component';
import { DataRectificationComponent } from '../../../shared/components/data-rectification/data-rectification.component';
import { RestrictionRequestComponent } from '../../../shared/components/restriction-request/restriction-request.component';
import { BreachNoticeComponent } from '../../../shared/components/breach-notice/breach-notice.component';
import { DpiaReviewComponent } from '../../../shared/components/dpia-review/dpia-review.component';

/** Centro de privacidad: derechos GDPR del alumno y revision legal (specs 37-50). */
@Component({
  selector: 'app-legal-page',
  templateUrl: 'legal.page.html',
  styleUrls: ['legal.page.scss'],
  imports: [
    IonContent,
    AppHeaderComponent,
    LegalNoticeComponent,
    ConsentManagerComponent,
    ConsentManagementComponent,
    AccessRequestComponent,
    PortabilityExportComponent,
    DataRectificationComponent,
    RestrictionRequestComponent,
    BreachNoticeComponent,
    DpiaReviewComponent,
  ],
})
export class LegalPage implements OnInit {
  private readonly session = inject(SessionService);

  readonly studentId = signal<string | null>(null);
  readonly staff = signal(false);

  ngOnInit(): void {
    void this.load();
  }

  private async load(): Promise<void> {
    const { userId, role } = await this.session.getSession();
    this.studentId.set(userId);
    this.staff.set(role === Role.ADMINISTRATIVO || role === Role.PROFESOR);
  }
}
