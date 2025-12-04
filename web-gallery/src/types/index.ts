// Export all types from sub-modules
export type {
  Device,
  Session,
  SessionInsertPayload,
  UpdatePhotoRequest,
  UpdateRawPhotosRequest,
  DeviceCreateRequest,
  DeviceUpdateRequest,
  Ticket
} from './supabase';

export type {
  ApiResponse,
  ApiError,
  SessionResponse,
  DeviceResponse,
  DevicesResponse,
  SessionsResponse,
  LoginRequest,
  LoginResponse,
  DeviceCreateRequest as ApiDeviceCreateRequest,
  DeviceUpdateRequest as ApiDeviceUpdateRequest,
  ErrorCode,
  AppError
} from './api';

export type {
  BaseButtonProps,
  ModalProps,
  InputProps,
  SelectProps,
  TableProps,
  CardProps,
  LoadingProps,
  ErrorProps,
  FormFieldProps,
  FormProps,
  NavItem,
  BreadcrumbItem,
  SidebarProps,
  HeaderProps,
  AsyncState,
  SortDirection,
  SortConfig,
  PaginationConfig,
  FilterConfig,
  ChangeHandler,
  SubmitHandler,
  ClickHandler
} from './ui';

export type UserRole = 'ADMIN' | 'CASHIER';

export interface UserProfile {
  id: string;
  role: UserRole;
  assigned_device_id?: string | null;
  created_at?: string;
  updated_at?: string;
}

// Re-export commonly used utility types
export type { ReactNode } from 'react';