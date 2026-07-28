import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Exercise, ExerciseListParams, ExerciseRequest } from '../models/exercise';
import { PageResponse } from '../models/page-response';

@Injectable({ providedIn: 'root' })
export class ExerciseApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/exercises`;

  list(params: ExerciseListParams = {}): Observable<PageResponse<Exercise>> {
    let httpParams = new HttpParams();
    if (params.q) {
      httpParams = httpParams.set('q', params.q);
    }
    if (params.muscle) {
      httpParams = httpParams.set('muscle', params.muscle);
    }
    if (params.equipment) {
      httpParams = httpParams.set('equipment', params.equipment);
    }
    if (params.category) {
      httpParams = httpParams.set('category', params.category);
    }
    if (params.customOnly) {
      httpParams = httpParams.set('customOnly', 'true');
    }
    if (params.page != null) {
      httpParams = httpParams.set('page', String(params.page));
    }
    if (params.size != null) {
      httpParams = httpParams.set('size', String(params.size));
    }
    return this.http.get<PageResponse<Exercise>>(this.base, { params: httpParams });
  }

  get(id: string): Observable<Exercise> {
    return this.http.get<Exercise>(`${this.base}/${id}`);
  }

  create(body: ExerciseRequest): Observable<Exercise> {
    return this.http.post<Exercise>(this.base, body);
  }

  update(id: string, body: ExerciseRequest): Observable<Exercise> {
    return this.http.put<Exercise>(`${this.base}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
