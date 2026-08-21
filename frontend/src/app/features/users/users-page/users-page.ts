import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { UserApi } from '../../../core/api/user-api.service';
import { CreateUserRequest, UpdateUserRequest, User } from '../../../core/models/user';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import { UserFormDialog, UserFormDialogData } from '../user-form-dialog/user-form-dialog';

@Component({
  selector: 'app-users-page',
  templateUrl: './users-page.html',
  standalone: false,
  styleUrl: './users-page.scss',
})
export class UsersPage implements OnInit {
  private readonly userApi = inject(UserApi);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  readonly users = signal<User[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.userApi.list().subscribe({
      next: (items) => {
        this.users.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(errorMessage(err, 'Failed to load users'));
      },
    });
  }

  createUser(): void {
    const ref = this.dialog.open<UserFormDialog, UserFormDialogData>(UserFormDialog, {
      width: '420px',
      data: {},
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