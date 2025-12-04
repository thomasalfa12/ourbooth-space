import { useEffect, useState } from "react";
import { supabase } from "../lib/supabase";
import type { Device } from "../types/supabase";

export function useUserRole() {
  const [role, setRole] = useState<"ADMIN" | "CASHIER" | null>(null);
  const [assignedDevice, setAssignedDevice] = useState<Device | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchUserContext() {
      try {
        const { data: { user } } = await supabase.auth.getUser();
        if (!user) {
          setRole(null);
          return;
        }

        // 1. Ambil Role & Assignment dari Profile
        const { data: profile } = await supabase
          .from("profiles")
          .select("role, assigned_device_id")
          .eq("id", user.id)
          .single();

        if (profile) {
          setRole(profile.role);

          // 2. Jika ada device assigned, ambil detailnya
          if (profile.assigned_device_id) {
            const { data: device } = await supabase
              .from("devices")
              .select("*")
              .eq("id", profile.assigned_device_id)
              .single();
            
            if (device) setAssignedDevice(device as Device);
          }
        }
      } catch (error) {
        console.error("Auth Context Error:", error);
      } finally {
        setLoading(false);
      }
    }

    fetchUserContext();
  }, []);

  return { role, assignedDevice, loading };
}