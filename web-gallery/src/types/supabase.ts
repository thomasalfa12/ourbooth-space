// Update definisi Device dan tambahkan Ticket

export interface Device {
  id: string;
  created_at: string;
  name: string;
  username: string;
  pin_code: string;
  type: "RENTAL" | "VENDING";
  status: "ACTIVE" | "SUSPENDED";
  expiry_date?: string | null; // [NEW] Untuk fitur Rental Limit
}

export interface Profile {
  id: string;
  role: 'ADMIN' | 'CASHIER';
  assigned_device_id?: string | null;
  created_at: string;
  updated_at: string;
}

export interface Ticket {
  id: string;
  code: string; // 6 Digit Code
  status: "AVAILABLE" | "USED";
  device_id: string;
  created_at: string;
  used_at?: string | null;
}

// Session types remain the same...
export interface Session {
  id: number;
  created_at: string;
  session_uuid: string;
  final_photo_url: string | null;
  video_url: string | null;
  device_id: string | null;
  raw_photos_urls: string | null;
}

export interface SessionInsertPayload {
  id: number;
  created_at: string;
  session_uuid: string;
  final_photo_url: string | null;
  video_url: string | null;
  device_id: string | null;
  raw_photos_urls: string | null;
}

export interface UpdatePhotoRequest {
  final_photo_url: string;
}

export interface UpdateRawPhotosRequest {
  raw_photos_urls: string;
}

export type DeviceCreateRequest = Omit<Device, 'id' | 'created_at'>;
export type DeviceUpdateRequest = Partial<Omit<Device, 'id' | 'created_at'>>;