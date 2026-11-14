import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ConsentDialogComponent } from './consent-dialog.component';
import { LegalApiService } from '../../../core/services/legal-api.service';

describe('ConsentDialogComponent', () => {
  let grantConsent: ReturnType<typeof vi.fn>;
  let revokeConsent: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    grantConsent = vi.fn(() => of({}));
    revokeConsent = vi.fn(() => of({}));

    TestBed.configureTestingModule({
      imports: [ConsentDialogComponent],
      providers: [{ provide: LegalApiService, useValue: { grantConsent, revokeConsent } }],
    });
  });

  it('Aceptar otorga los consentimientos marcados', () => {
    const fixture = TestBed.createComponent(ConsentDialogComponent);
    fixture.componentRef.setInput('studentId', 'student-1');
    fixture.detectChanges();

    fixture.componentInstance.toggle('CAMERA', { target: { checked: true } } as unknown as Event);
    fixture.componentInstance.accept();

    expect(grantConsent).toHaveBeenCalledWith('student-1', 'CAMERA');
    expect(grantConsent).toHaveBeenCalledTimes(1);
  });

  it('Revocar revoca los consentimientos marcados', () => {
    const fixture = TestBed.createComponent(ConsentDialogComponent);
    fixture.componentRef.setInput('studentId', 'student-1');
    fixture.detectChanges();

    fixture.componentInstance.toggle('MICROPHONE', { target: { checked: true } } as unknown as Event);
    fixture.componentInstance.revokeSelected();

    expect(revokeConsent).toHaveBeenCalledWith('student-1', 'MICROPHONE');
  });
});
