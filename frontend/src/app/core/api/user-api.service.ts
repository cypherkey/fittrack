import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateUserRequest, UpdateUserRequest, User } from '../models/user';

@Injectable({ providedIn: 'root' })
export class UserApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/users`;

  list(): Observable<User[]> {
    return this.http.get<User[]>(this.base);
  }

  create(body: CreateUserRequest): Observable<User> {
    return this.http.post<User>(this.base, body);
  }

  update(id: string, body: UpdateUserRequest): Observable<User> {
    return this.http.put<User>(`${this.base}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
