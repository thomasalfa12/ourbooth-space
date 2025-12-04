import type { ReactNode } from "react";

// Base component props
export interface BaseButtonProps {
  children: ReactNode;
  disabled?: boolean;
  className?: string;
  onClick?: () => void;
  type?: "button" | "submit" | "reset";
}

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  children: ReactNode;
  title?: string;
  size?: "sm" | "md" | "lg" | "xl";
}

export interface InputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  type?: "text" | "email" | "password" | "number";
  disabled?: boolean;
  required?: boolean;
  error?: string;
  label?: string;
  className?: string;
}

export interface SelectProps<T extends string> {
  value: T;
  onChange: (value: T) => void;
  options: Array<{ value: T; label: string }>;
  disabled?: boolean;
  required?: boolean;
  error?: string;
  label?: string;
  className?: string;
}

export interface TableProps<T> {
  data: T[];
  columns: Array<{
    key: keyof T;
    label: string;
    render?: (value: T[keyof T], row: T) => ReactNode;
  }>;
  loading?: boolean;
  error?: string;
  onRowClick?: (row: T) => void;
  className?: string;
}

export interface CardProps {
  children: ReactNode;
  title?: string;
  subtitle?: string;
  actions?: ReactNode;
  className?: string;
}

export interface LoadingProps {
  size?: "sm" | "md" | "lg";
  text?: string;
  className?: string;
}

export interface ErrorProps {
  message: string;
  onRetry?: () => void;
  className?: string;
}

// Form types
export interface FormFieldProps {
  name: string;
  label: string;
  required?: boolean;
  error?: string;
  children: ReactNode;
}

export interface FormProps {
  onSubmit: (data: Record<string, unknown>) => void;
  children: ReactNode;
  className?: string;
}

// Navigation types
export interface NavItem {
  id: string;
  label: string;
  path: string;
  icon?: ReactNode;
  badge?: string | number;
  disabled?: boolean;
  role?: "ADMIN" | "CASHIER" | "ALL"; // Added role property for RBAC
}

export interface BreadcrumbItem {
  label: string;
  path?: string;
  active?: boolean;
}

// Layout types
export interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  items: NavItem[];
  activeItem?: string;
  className?: string;
}

export interface HeaderProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  breadcrumbs?: BreadcrumbItem[];
  className?: string;
}

// Utility types for common patterns
export type AsyncState<T> = 
  | { status: 'idle'; data: null; error: null }
  | { status: 'loading'; data: null; error: null }
  | { status: 'success'; data: T; error: null }
  | { status: 'error'; data: null; error: string };

export type SortDirection = 'asc' | 'desc';

export interface SortConfig<T> {
  key: keyof T;
  direction: SortDirection;
}

export interface PaginationConfig {
  page: number;
  limit: number;
  total: number;
}

export interface FilterConfig {
  search?: string;
  dateRange?: {
    start: Date;
    end: Date;
  };
  status?: string[];
  [key: string]: unknown;
}

// Event handler types
export type ChangeHandler<T = string> = (value: T) => void;
export type SubmitHandler<T = Record<string, unknown>> = (data: T) => void | Promise<void>;
export type ClickHandler = () => void;