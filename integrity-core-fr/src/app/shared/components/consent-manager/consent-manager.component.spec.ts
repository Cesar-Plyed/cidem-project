import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ConsentManagerComponent } from './consent-manager.component';
import { LegalApiService } from '../../../core/services/legal-api.service';

describe('ConsentManagerComponent', () => {
  let getConsentStatus: ReturnType<typeof vi.fn>;
  let getConsentHistory: ReturnType<typeof vi.fn>;
  let grantConsent: ReturnType<typeof vi.fn>;
  let revokeConsent: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    getConsentStatus = vi.fn(() => of({
      studentId: 'student-1',
      consents: [
        { id: '1', type: 'CAMERA', status: 'revoked', grantedAt: null, revokedAt: null },
        { id: '2', type: 'MICROPHONE', status: 'revoked', grantedAt: null, revokedAt: null },
        { id: '3', type: 'SCREENSHOT', status: 'revoked', grantedAt: null, revokedAt: null },
        { id: '4', type: 'TAB_MONITORING', status: 'revoked', grantedAt: null, revokedAt: null },
      ],
    }));
    getConsentHistory = vi.fn(() => of({ studentId: 'student-1', history: [] }));
    grantConsent = vi.fn(() => of({}));
    revokeConsent = vi.fn(() => of({}));

    TestBed.configureTestingModule({
      imports: [ConsentManagerComponent],
      providers: [{
        provide: LegalApiService,
        useValue: { getConsentStatus, getConsentHistory, grantConsent, revokeConsent },
      }],
    });
  });

  it('muestra la tabla y el historial', () => {
    const fixture = TestBed.createComponent(ConsentManagerComponent);
    fixture.componentRef.setInput('studentId', 'student-1');
    fixture.detectChanges();

    expect(getConsentStatus).toHaveBeenCalledWith('student-1');
    expect(getConsentHistory).toHaveBeenCalledWith('student-1');
    expect(fixture.nativeElement.textContent).toContain('CAMERA');
    expect(fixture.nativeElement.textContent).toContain('Historial de cambios');
  });

  it('Otorgar llama al POST y refresca la tabla', () => {
    const fixture = TestBed.createComponent(ConsentManagerComponent);
    fixture.componentRef.setInput('studentId', 'student-1');
    fixture.detectChanges();

    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    buttons.find((button) => button.textContent?.includes('Otorgar'))!.click();

    expect(grantConsent).toHaveBeenCalledWith('student-1', 'CAMERA');
    expect(getConsentStatus).toHaveBeenCalledTimes(2);
  });
});
