import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TemplateVisibility } from '../models/enums';
import {
  CloneTemplateRequest,
  ReorderSetsRequest,
  Template,
  TemplateRequest,
} from '../models/template';
import { Workout } from '../models/workout';

@Injectable({ providedIn: 'root' })
export class TemplateApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/templates`;

  list(visibility?: TemplateVisibility): Observable<Template[]> {
    let params = new HttpParams();
    if (visibility) {
      params = params.set('visibility', visibility);
    }
    return this.http.get<Template[]>(this.base, { params });
  }

  get(id: string): Observable<Template> {
    return this.http.get<Template>(`${this.base}/${id}`);
  }

  create(body: TemplateRequest): Observable<Template> {
    return this.http.post<Template>(this.base, body);
  }

  update(id: string, body: TemplateRequest): Observable<Template> {
    return this.http.put<Template>(`${this.base}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  reorderSets(id: string, body: ReorderSetsRequest): Observable<Template> {
    return this.http.patch<Template>(`${this.base}/${id}/sets/reorder`, body);
  }

  clone(id: string, body: CloneTemplateRequest): Observable<Workout> {
    return this.http.post<Workout>(`${this.base}/${id}/clone`, body);
  }
}
