import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ExamRepository } from '../../../domain/ports/exam.repository';
import { Exam } from '../../../domain/models/exam/exam.model';

@Injectable({ providedIn: 'root' })
export class ListExamsUseCase {
  private readonly examRepository = inject(ExamRepository)

  execute(): Observable<Exam[]> {
    return this.examRepository.list();
  }
}
