export interface User {
  id: string;
  username: string | null;
  email: string | null;
  displayName: string | null;
  avatarUrl: string | null;
  admin: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  displayName: string;
  email?: string | null;
  admin?: boolean | null;
}

export interface UpdateUserRequest {
  displayName?: string | null;
  email?: string | null;
  password?: string | null;
  admin?: boolean | null;
}
