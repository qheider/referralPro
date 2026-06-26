export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  username: string;
  companyId: number;
  role: string;
}

export interface CurrentUserResponse {
  userId: number;
  username: string;
  companyId: number;
  companyName: string;
  role: string;
}

export interface User {
  userId: number;
  username: string;
  companyId: number;
  companyName?: string;
  role: string;
}
