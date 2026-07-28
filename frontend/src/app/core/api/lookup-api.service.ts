import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Equipment, Muscle } from '../models/lookup';

@Injectable({ providedIn: 'root' })
export class LookupApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  equipment(): Observable<Equipment[]> {
    return this.http.get<Equipment[]>(`${this.base}/api/v1/equipment`);
  }

  muscles(): Observable<Muscle[]> {
    return this.http.get<Muscle[]>(`${this.base}/api/v1/muscles`);
  }
}
