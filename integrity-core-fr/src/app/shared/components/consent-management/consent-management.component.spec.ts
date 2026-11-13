import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ConsentManagementComponent } from './consent-management.component';
import { LegalApiService } from '../../../core/services/legal-api.service';
import { ConsentOverview } from '../../../domain/models/legal/legal.model';

function overview(): ConsentOverview {
  return {
    studentId: 'student-1',
    consents: [
      { id: '1', type: 'CAMERA', status: 'granted', grantedAt: '2026-11-13T10:00:00Z', revokedAt: null },
      { id: '2', type: 'MICROPHONE', status: 'revoked', grantedAt: null, revokedAt: '2026-11-13T11:00:00Z' },
      { id: '3', type: 'SCREENSHOT', status: 'granted', grantedAt: '2026-11-13T10:00:00Z', revokedAt: null },
      { id: '4', type: 'TAB_MONITORING', status: 'granted', grantedAt: '2026-11-13T10:00:00Z', revokedAt: null },
    ],
  };
}

describe('ConsentManagementComponent', () => {
  let getConsentStatus: ReturnType<typeof vi.fn>;
  let revokeAllConsents: ReturnType<typeof vi.fn>;
  let revokeConsent: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    getConsentStatus = vi.fn(() => of(overview()));
    revokeAllConsents = vi.fn(() => of([]));
    revokeConsent = vi.fn(() => of({}));

    TestBed.configureTestingModule({
      imports: [ConsentManagementComponent],
      providers: [{
        provide: LegalApiService,
        useValue: { getConsentStatus, revokeAllConsents, revokeConsent },
      }],
    });
  });

  it('carga los consentimientos al iniciar', () => {
    const fixture = TestBed.createComponent(ConsentManagementComponent);
    fixture.componentRef.setInput('studentId', 'student-1');
    fixture.detectChanges();

    expect(getConsentStatus).toHaveBeenCalledWith('student-1');
    expect(fixture.componentInstance.consents()).toHaveLength(4);
  });

  it('Revocar todo llama a POST revoke-all y recarga la tabla', () => {
    const fixture = TestBed.createComponent(ConsentManagementComponent);
    fixture.componentRef.setInput('studentId', 'student-1');
    fixture.detectChanges();

    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const revokeAll = buttons.find((button) => button.textContent?.includes('Revocar todo'));
    revokeAll!.click();

    expect(revokeAllConsents).toHaveBeenCalledWith('student-1');
    expect(getConsentStatus).toHaveBeenCalledTimes(2);
  });

  it('Revocar un tipo concreto llama al endpoint de revocacion', () => {
    const fixture = TestBed.createComponent(ConsentManagementComponent);
    fixture.componentRef.setInput('studentId', 'student-1');
    fixture.detectChanges();

    fixture.componentInstance.revoke('CAMERA');

    expect(revokeConsent).toHaveBeenCalledWith('student-1', 'CAMERA');
  });
});
