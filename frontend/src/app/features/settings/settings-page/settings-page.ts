import { Component, OnInit, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { UserApi } from '../../../core/api/user-api.service';
import { AuthService } from '../../../core/auth.service';
import { CreateUserRequest, UpdateUserRequest, User } from '../../../core/models/user';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import { UserFormDialog, UserFormDialogData } from '../user-form-dialog/user-form-dialog';

@Component({
  selector: 'app-settings-page',
  templateUrl: './settings-page.html',
  standalone: false,
  styleUrl: './settings-page.scss',
})
export class SettingsPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly userApi = inject(UserApi);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  readonly apiBaseUrl =
    environment.apiBaseUrl || '(dev proxy → http://localhost:8080)';
  readonly user$: Observable<User | null> = this.auth.user$;

  users: User[] = [];
  usersLoading = false;

  ngOnInit(): void {
    this.auth.user$.subscribe((user) => {
      if (user?.admin) {
        this.loadUsers();
      }
    });
  }

  loadUsers(): void {
    this.usersLoading = true;
    this.userApi.list().subscribe({
      next: (items) => {
        this.users = items;
        this.usersLoading = false;
      },
      error: (err) => {
        this.usersLoading = false;
        this.notify.error(errorMessage(err, 'Failed to load users'));
      },
    });
  }

  createUser(): void {
    const ref = this.dialog.open<UserFormDialog, UserFormDialogData>(UserFormDialog, {
      width: '420px',
    });
    ref.afterClosed().subscribe((body) => {
      if (!body) {
        return;
      }
      this.userApi.create(body as CreateUserRequest).subscribe({
        next: () => {
          this.notify.success('User created');
          this.loadUsers();
        },
        error: (err) => this.notify.error(errorMessage(err, 'Failed to create user')),
      });
    });
  }

  editUser(user: User): void {
    const ref = this.dialog.open<UserFormDialog, UserFormDialogData>(UserFormDialog, {
      width: '420px',
      data: { user },
    });
    ref.afterClosed().subscribe((body) => {
      if (!body) {
        return;
      }
      this.userApi.update(user.id, body as UpdateUserRequest).subscribe({
        next: () => {
          this.notify.success('User updated');
          this.loadUsers();
        },
        error: (err) => this.notify.error(errorMessage(err, 'Failed to update user')),
      });
    });
  }

  deleteUser(user: User): void {
    const label = user.displayName || user.username || user.id;
    if (!confirm(`Delete user "${label}"?`)) {
      return;
    }
    this.userApi.delete(user.id).subscribe({
      next: () => {
        this.notify.success('User deleted');
        this.loadUsers();
      },
      error: (err) => this.notify.error(errorMessage(err, 'Failed to delete user')),
    });
  }
}
