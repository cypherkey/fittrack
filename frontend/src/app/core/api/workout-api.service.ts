import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ReorderSetsRequest } from '../models/template';
import { Workout, WorkoutListParams, WorkoutRequest } from '../models/workout';

@Injectable({ providedIn: 'root' })
export class WorkoutApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/workouts`;

  list(params: WorkoutListParams = {}): Observable<Workout[]> {
    let httpParams = new HttpParams();
    if (params.from) {
      httpParams = httpParams.set('from', params.from);
    }
    if (params.to) {
      httpParams = httpParams.set('to', params.to);
    }
    return this.http.get<Workout[]>(this.base, { params: httpParams });
  }

  get(id: string): Observable<Workout> {
    return this.http.get<Workout>(`${this.base}/${id}`);
  }

  create(body: WorkoutRequest): Observable<Workout> {
    return this.http.post<Workout>(this.base, body);
  }

  update(id: string, body: WorkoutRequest): Observable<Workout> {
    return this.http.put<Workout>(`${this.base}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  reorderSets(id: string, body: ReorderSetsRequest): Observable<Workout> {
    return this.http.patch<Workout>(`${this.base}/${id}/sets/reorder`, body);
  }
}
