import { createContext, useContext, useEffect, useState } from "react";
import { supabase } from "../lib/supabase";
import type { Device } from "../types/supabase";

interface UserContextType {
  role: "ADMIN" | "CASHIER" | null;
  assignedDevice: Device | null;
  loading: boolean;
}

const UserContext = createContext<UserContextType | undefined>(undefined);

export function UserProvider({ children }: { children: React.ReactNode }) {
  const [role, setRole] = useState<"ADMIN" | "CASHIER" | null>(null);
  const [assignedDevice, setAssignedDevice] = useState<Device | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchUserContext() {
      try {
        const {
          data: { user },
        } = await supabase.auth.getUser();
        if (!user) {
          setRole(null);
          setAssignedDevice(null);
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

    // Listen for auth changes
    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((event) => {
      if (event === "SIGNED_IN" || event === "SIGNED_OUT") {
        setLoading(true);
        fetchUserContext();
      }
    });

    // Listen for profile changes (Realtime Role/Device updates)
    const profileSub = supabase
      .channel("user-context-profile")
      .on(
        "postgres_changes",
        { event: "UPDATE", schema: "public", table: "profiles" },
        async (payload) => {
          const {
            data: { user },
          } = await supabase.auth.getUser();
          if (user && payload.new.id === user.id) {
            console.log("Profile updated, refreshing context...");
            fetchUserContext();
          }
        }
      )
      .subscribe();

    return () => {
      subscription.unsubscribe();
      profileSub.unsubscribe();
    };
  }, []);

  return (
    <UserContext.Provider value={{ role, assignedDevice, loading }}>
      {children}
    </UserContext.Provider>
  );
}

export function useUserContext() {
  const context = useContext(UserContext);
  if (context === undefined) {
    throw new Error("useUserContext must be used within a UserProvider");
  }
  return context;
}
