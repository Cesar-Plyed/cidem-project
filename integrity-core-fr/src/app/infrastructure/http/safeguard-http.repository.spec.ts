import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { SafeguardHttpRepository, mapEventType, normalizeConfig } from './safeguard-http.repository';
import { SafeguardEvent } from '../../domain/models/safeguard/safeguard.model';

describe('SafeguardHttpRepository', () => {
  let repository: SafeguardHttpRepository;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SafeguardHttpRepository,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    repository = TestBed.inject(SafeguardHttpRepository);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('usa el endpoint real de configuración /api/safeguard', async () => {
    const resultPromise = firstValueFrom(repository.getConfig('exam-1'));
    const req = http.expectOne('http://localhost:8080/api/safeguard/exams/exam-1/config');
    expect(req.request.method).toBe('GET');
    req.flush({
      examId: 'exam-1',
      enabled: true,
      requireCamera: false,
      requireMicrophone: false,
      requireFullscreen: true,
      blockCopyPaste: false,
      maxTabSwitches: 2,
      riskThreshold: 80,
      autoTerminateOnThreshold: true,
    });

    const config = await resultPromise;
    expect(config.enabled).toBe(true);
    expect(config.blockCopy).toBe(false);
    expect(config.blockCut).toBe(false);
    expect(config.blockPaste).toBe(false);
    expect(config.requireFullscreen).toBe(true);
    expect(config.maxTabSwitches).toBe(2);
  });

  it('inicia sesión con /sessions/start y envía userAgent', async () => {
    const resultPromise = firstValueFrom(repository.startSession('exam-1', 'attempt-1'));
    const req = http.expectOne('http://localhost:8080/api/safeguard/sessions/start');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.examId).toBe('exam-1');
    expect(req.request.body.attemptId).toBe('attempt-1');
    expect(typeof req.request.body.userAgent).toBe('string');
    req.flush({
      sessionId: 'session-1',
      examId: 'exam-1',
      attemptId: 'attempt-1',
      status: 'ACTIVE',
      riskScore: 0,
      startedAt: '2026-10-05T18:00:00Z',
      endedAt: null,
      lastHeartbeatAt: '2026-10-05T18:00:00Z',
      requireCamera: false,
      requireMicrophone: false,
      requireFullscreen: false,
      blockCopyPaste: true,
    });

    const session = await resultPromise;
    expect(session.sessionId).toBe('session-1');
    expect(session.status).toBe('ACTIVE');
  });

  it('traduce eventos web al enum que entiende el backend', async () => {
    const event: SafeguardEvent = {
      eventId: 'event-1',
      examId: 'exam-1',
      attemptId: 'attempt-1',
      sessionId: 'session-1',
      type: 'COPY_ATTEMPT',
      result: 'BLOCKED',
      occurredAt: '2026-10-05T18:00:00Z',
      detail: 'Ctrl+C',
    };

    const resultPromise = firstValueFrom(repository.registerEvent(event));
    const req = http.expectOne('http://localhost:8080/api/safeguard/events');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      sessionId: 'session-1',
      eventId: 'event-1',
      eventType: 'COPY_PASTE',
      occurredAt: '2026-10-05T18:00:00Z',
      metadata: JSON.stringify({
        clientEventType: 'COPY_ATTEMPT',
        result: 'BLOCKED',
        detail: 'Ctrl+C',
        examId: 'exam-1',
        attemptId: 'attempt-1',
      }),
    });
    req.flush({});
    await resultPromise;
  });

  it('envía clientTime en el heartbeat', async () => {
    const resultPromise = firstValueFrom(repository.heartbeat('session-1'));
    const req = http.expectOne('http://localhost:8080/api/safeguard/sessions/session-1/heartbeat');
    expect(req.request.method).toBe('POST');
    expect(typeof req.request.body.clientTime).toBe('string');
    req.flush({ status: 'ACTIVE', riskScore: 0, serverTime: '2026-10-05T18:00:01Z', closed: false });
    await resultPromise;
  });

  it('mantiene el cierre en /sessions/{id}/close', async () => {
    const resultPromise = firstValueFrom(repository.closeSession('session-1'));
    const req = http.expectOne('http://localhost:8080/api/safeguard/sessions/session-1/close');
    expect(req.request.method).toBe('POST');
    req.flush({});
    await resultPromise;
  });

  it('mapea los tipos locales al enum del backend', () => {
    expect(mapEventType('TAB_HIDDEN')).toBe('TAB_SWITCH');
    expect(mapEventType('WINDOW_BLUR')).toBe('WINDOW_BLUR');
    expect(mapEventType('FULLSCREEN_EXIT')).toBe('FULLSCREEN_EXIT');
    expect(mapEventType('COPY_ATTEMPT')).toBe('COPY_PASTE');
    expect(mapEventType('CUT_ATTEMPT')).toBe('COPY_PASTE');
    expect(mapEventType('PASTE_ATTEMPT')).toBe('COPY_PASTE');
    expect(mapEventType('CONTEXT_MENU')).toBe('RIGHT_CLICK');
    expect(mapEventType('RESTRICTED_SHORTCUT')).toBe('SUSPICIOUS_ACTIVITY');
  });

  it('normaliza campos que el backend sí conoce sin debilitar la protección web', () => {
    const config = normalizeConfig({
      enabled: false,
      blockCopyPaste: false,
      maxTabSwitches: 0,
      riskThreshold: 50,
      requireCamera: true,
      requireMicrophone: true,
      requireFullscreen: true,
      autoTerminateOnThreshold: true,
    });

    expect(config.enabled).toBe(false);
    expect(config.blockCopy).toBe(false);
    expect(config.blockCut).toBe(false);
    expect(config.blockPaste).toBe(false);
    expect(config.maxTabSwitches).toBe(0);
    expect(config.riskThreshold).toBe(50);
    expect(config.requireCamera).toBe(true);
  });
});
