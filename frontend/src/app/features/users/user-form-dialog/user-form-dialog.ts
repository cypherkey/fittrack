import { Component, inject, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CreateUserRequest, UpdateUserRequest, User } from '../../../core/models/user';

export interface UserFormDialogData {
  user?: User;
}

@Component({
  selector: 'app-user-form-dialog',
  templateUrl: './user-form-dialog.html',
  standalone: false,
  styleUrl: './user-form-dialog.scss',
})
export class UserFormDialog {
  private readonly fb = inject(FormBuilder);

  readonly isEdit: boolean;

  readonly form = this.fb.group({
    username: ['', Validators.required],
    password: [''],
    displayName: ['', Validators.required],
    email: [''],
    admin: [false],
  });

  constructor(
    private readonly dialogRef: MatDialogRef<UserFormDialog, CreateUserRequest | UpdateUserRequest>,
    @Inject(MAT_DIALOG_DATA) data: UserFormDialogData | null,
  ) {
    const user = data?.user;
    this.isEdit = !!user;
    if (user) {
      this.form.patchValue({
        username: user.username ?? '',
        displayName: user.displayName ?? '',
        email: user.email ?? '',
        admin: user.admin ?? false,
      });
      this.form.get('username')?.disable();
      this.form.get('password')?.clearValidators();
    } else {
      this.form.get('password')?.setValidators(Validators.required);
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (this.isEdit) {
      const body: UpdateUserRequest = {
        displayName: v.displayName,
        email: v.email || null,
        admin: v.admin ?? false,
      };
      if (v.password?.trim()) {
        body.password = v.password;
      }
      this.dialogRef.close(body);
    } else {
      this.dialogRef.close({
        username: v.username!,
        password: v.password!,
        displayName: v.displayName!,
        email: v.email || null,
        admin: v.admin ?? false,
      } satisfies CreateUserRequest);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
