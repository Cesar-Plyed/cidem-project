import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { SafeguardEvent } from '../../domain/models/safeguard/safeguard.model';
import { SafeguardRepository } from '../../domain/ports/safeguard.repository';
import { SafeguardEventDispatcher } from './safeguard-event-dispatcher';

function event(id: string): SafeguardEvent {
  return {
    eventId: id,
    examId: 'exam-1',
    attemptId: 'attempt-1',
    sessionId: 'session-1',
    type: 'COPY_ATTEMPT',
    result: 'BLOCKED',
    occurredAt: new Date().toISOString(),
  };
}

describe('SafeguardEventDispatcher', () => {
  let registerEvent: ReturnType<typeof vi.fn<(e: SafeguardEvent) => Observable<void>>>;
  let dispatcher: SafeguardEventDispatcher;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    registerEvent = vi.fn<(e: SafeguardEvent) => Observable<void>>(() => of(undefined));

    TestBed.configureTestingModule({
      providers: [{ provide: SafeguardRepository, useValue: { registerEvent } }],
    });
    dispatcher = TestBed.inject(SafeguardEventDispatcher);
  });

  afterEach(() => {
    dispatcher.reset();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('envía los eventos en orden y espaciados por minIntervalMs', () => {
    dispatcher.enqueue(event('a'));
    dispatcher.enqueue(event('b'));
    expect(dispatcher.pending()).toBe(2);

    vi.advanceTimersByTime(0);
    expect(registerEvent).toHaveBeenCalledTimes(1);
    expect(registerEvent.mock.calls[0][0].eventId).toBe('a');

    vi.advanceTimersByTime(499);
    expect(registerEvent).toHaveBeenCalledTimes(1); // todavía dentro del espaciado

    vi.advanceTimersByTime(1);
    expect(registerEvent).toHaveBeenCalledTimes(2);
    expect(registerEvent.mock.calls[1][0].eventId).toBe('b');
    expect(dispatcher.pending()).toBe(0);
  });

  it('si falla la red conserva el evento y reintenta con espera exponencial', () => {
    registerEvent.mockReturnValue(throwError(() => ({ status: 0 })));
    dispatcher.enqueue(event('a'));

    vi.advanceTimersByTime(0); // 1.er intento
    expect(registerEvent).toHaveBeenCalledTimes(1);
    expect(dispatcher.pending()).toBe(1);
    expect(dispatcher.unreachable()).toBe(true);

    vi.advanceTimersByTime(1000); // 2.º intento tras 1 s
    expect(registerEvent).toHaveBeenCalledTimes(2);

    vi.advanceTimersByTime(1999);
    expect(registerEvent).toHaveBeenCalledTimes(2);
    vi.advanceTimersByTime(1); // 3.er intento tras 2 s
    expect(registerEvent).toHaveBeenCalledTimes(3);
  });

  it('entrega el evento pendiente cuando el servidor se recupera', () => {
    registerEvent.mockReturnValueOnce(throwError(() => ({ status: 503 }))).mockReturnValue(of(undefined));
    dispatcher.enqueue(event('a'));

    vi.advanceTimersByTime(0);
    expect(dispatcher.pending()).toBe(1);

    vi.advanceTimersByTime(1000);
    expect(dispatcher.pending()).toBe(0);
    expect(dispatcher.unreachable()).toBe(false);
  });

  it('descarta el evento que el servidor rechaza (4xx) y sigue con el siguiente', () => {
    registerEvent.mockReturnValueOnce(throwError(() => ({ status: 400 }))).mockReturnValue(of(undefined));
    dispatcher.enqueue(event('mal'));
    dispatcher.enqueue(event('bien'));

    vi.advanceTimersByTime(0);
    expect(dispatcher.pending()).toBe(1);
    expect(dispatcher.unreachable()).toBe(false);

    vi.advanceTimersByTime(500);
    expect(registerEvent.mock.calls.map((call) => call[0].eventId)).toEqual(['mal', 'bien']);
    expect(dispatcher.pending()).toBe(0);
  });

  it('flushNow reintenta de inmediato sin esperar el backoff', () => {
    registerEvent.mockReturnValueOnce(throwError(() => ({ status: 0 }))).mockReturnValue(of(undefined));
    dispatcher.enqueue(event('a'));
    vi.advanceTimersByTime(0);
    expect(dispatcher.pending()).toBe(1);

    dispatcher.flushNow(); // por ejemplo, el navegador avisa que volvió la red
    vi.advanceTimersByTime(0);
    expect(dispatcher.pending()).toBe(0);
  });

  it('descarta el evento más antiguo cuando la cola supera maxQueueSize', () => {
    registerEvent.mockReturnValue(throwError(() => ({ status: 0 })));
    dispatcher.configure({ maxQueueSize: 2 });

    dispatcher.enqueue(event('a'));
    dispatcher.enqueue(event('b'));
    dispatcher.enqueue(event('c'));

    expect(dispatcher.pending()).toBe(2);
    vi.advanceTimersByTime(0);
    expect(registerEvent.mock.calls[0][0].eventId).toBe('b'); // 'a' fue descartado
  });

  it('tras finalize deja de insistir cuando se acaba el plazo', () => {
    registerEvent.mockReturnValue(throwError(() => ({ status: 0 })));
    dispatcher.configure({ finalizeGraceMs: 5000 });
    dispatcher.enqueue(event('a'));
    dispatcher.finalize();

    vi.advanceTimersByTime(10_000); // el fallo de t=7 s ya cae fuera del plazo de 5 s
    expect(dispatcher.pending()).toBe(0);

    const calls = registerEvent.mock.calls.length;
    vi.advanceTimersByTime(60_000);
    expect(registerEvent).toHaveBeenCalledTimes(calls); // ya no reintenta
  });
});
