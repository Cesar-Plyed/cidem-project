import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ExamRepository } from '../../../domain/ports/exam.repository';
import { Exam } from '../../../domain/models/exam/exam.model';

@Injectable({ providedIn: 'root' })
export class GetExamUseCase {
  constructor(private readonly examRepository: ExamRepository) {}

  execute(id: string): Observable<Exam | undefined> {
    return this.examRepository.getById(id);
  }
}
