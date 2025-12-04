import { createClient } from "@supabase/supabase-js";

// Vite menggunakan import.meta.env untuk mengakses variabel
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseKey = import.meta.env.VITE_SUPABASE_KEY;

if (!supabaseUrl || !supabaseKey) {
  throw new Error("Missing Supabase URL or Key in .env file");
}

export const supabase = createClient(supabaseUrl, supabaseKey);