import type { Device, Session } from './supabase';

// API Response wrapper types
export interface ApiResponse<T> {
  data: T | null;
  error: string | null;
}

export interface ApiError {
  data: null;
  error: {
    message: string;
    code?: string;
    details?: unknown;
  };
}

// Specific API response types
export type SessionResponse = ApiResponse<Session>;
export type DeviceResponse = ApiResponse<Device>;
export type DevicesResponse = ApiResponse<Device[]>;
export type SessionsResponse = ApiResponse<Session[]>;

// Login request/response types
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  session: {
    access_token: string;
    refresh_token: string;
    expires_in: number;
    user: {
      id: string;
      email: string;
    };
  } | null;
  error: string | null;
}

// Device management types
export interface DeviceCreateRequest {
  name: string;
  type: "RENTAL" | "VENDING";
  username: string;
  pin_code: string;
  status: "ACTIVE" | "SUSPENDED";
}

export interface DeviceUpdateRequest {
  name?: string;
  type?: "RENTAL" | "VENDING";
  status?: "ACTIVE" | "SUSPENDED";
}

// Error codes for consistent error handling
export const ErrorCode = {
  DEVICE_NOT_FOUND: "DEVICE_NOT_FOUND",
  INVALID_CREDENTIALS: "INVALID_CREDENTIALS",
  SESSION_NOT_FOUND: "SESSION_NOT_FOUND",
  UPLOAD_FAILED: "UPLOAD_FAILED",
  NETWORK_ERROR: "NETWORK_ERROR",
  VALIDATION_ERROR: "VALIDATION_ERROR",
  UNAUTHORIZED: "UNAUTHORIZED",
  FORBIDDEN: "FORBIDDEN"
} as const;

export type ErrorCode = typeof ErrorCode[keyof typeof ErrorCode];

// Standardized error type
export interface AppError {
  code: ErrorCode;
  message: string;
  details?: unknown;
}