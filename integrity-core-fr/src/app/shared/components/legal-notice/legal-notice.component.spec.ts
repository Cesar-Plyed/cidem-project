import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { LegalNoticeComponent } from './legal-notice.component';
import { LegalContentService } from '../../../core/services/legal-content.service';

function buttonByText(root: HTMLElement, text: string): HTMLButtonElement {
  const button = Array.from(root.querySelectorAll('button'))
    .find((candidate) => candidate.textContent?.includes(text));
  if (!button) {
    throw new Error(`Boton no encontrado: ${text}`);
  }
  return button as HTMLButtonElement;
}

describe('LegalNoticeComponent', () => {
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    navigate = vi.fn(() => Promise.resolve(true));
    TestBed.configureTestingModule({
      imports: [LegalNoticeComponent],
      providers: [{ provide: Router, useValue: { navigate } }],
    });
  });

  it('muestra el contenido de los articulos 13 y 14', () => {
    const fixture = TestBed.createComponent(LegalNoticeComponent);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Art. 13');
    expect(text).toContain('Art. 14');
    expect(text).toContain('dpo@cidemags.edu');
  });

  it('Acepto habilita el inicio del examen', () => {
    const fixture = TestBed.createComponent(LegalNoticeComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const accepted = vi.spyOn(component.accepted, 'emit');

    buttonByText(fixture.nativeElement, 'Acepto').click();

    expect(accepted).toHaveBeenCalled();
    expect(component.acceptedState()).toBe(true);
  });

  it('Rechazo redirige a /login', () => {
    const fixture = TestBed.createComponent(LegalNoticeComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    buttonByText(fixture.nativeElement, 'Rechazo').click();

    expect(component.acceptedState()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('el servicio de contenido devuelve Art. 13 y Art. 14', () => {
    const service = TestBed.inject(LegalContentService);
    const notice = service.getLegalNotice();
    expect(notice.articles.join(' ')).toContain('Art. 13');
    expect(notice.articles.join(' ')).toContain('Art. 14');
    expect(service.getPrivacyPolicy()).toContain('POLÍTICA DE PRIVACIDAD');
    expect(service.getConsentForm()).toHaveLength(4);
  });
});
