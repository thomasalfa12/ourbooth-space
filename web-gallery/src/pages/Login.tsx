import { useState } from "react";
import { supabase } from "../lib/supabase";
import { useNavigate } from "react-router-dom";
import { Lock, Mail, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function Login() {
  const navigate = useNavigate();
  const [identifier, setIdentifier] = useState(""); // Changed from email to identifier to support both email and username
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg(null);

    try {
      // 1. JIKA LOGIN PAKAI EMAIL (Bisa Admin, Bisa Cashier)
      if (identifier.includes("@")) {
        const { data: authData, error } =
          await supabase.auth.signInWithPassword({
            email: identifier,
            password: password,
          });

        if (error) throw error;

        if (authData.session) {
          // --- CEK ROLE & ASSIGNMENT ---
          const { data: profile } = await supabase
            .from("profiles")
            .select("role, assigned_device_id")
            .eq("id", authData.session.user.id)
            .single();

          // A. JIKA ADMIN -> Ke Dashboard
          if (profile?.role === "ADMIN") {
            navigate("/admin");
          }

          // B. JIKA CASHIER -> Cek Device Link
          else if (profile?.role === "CASHIER" && profile.assigned_device_id) {
            // Ambil detail device yang terhubung
            const { data: deviceData } = await supabase
              .from("devices")
              .select("*")
              .eq("id", profile.assigned_device_id)
              .single();

            if (deviceData) {
              // SIMPAN SESSION DEVICE (SAMA SEPERTI LOGIN MANUAL)
              // Ini triknya! Sistem Ticket Station akan membaca ini.
              localStorage.setItem(
                "kubik_device_session",
                JSON.stringify(deviceData)
              );

              navigate("/admin/tickets");
            } else {
              setErrorMsg("Account is valid, but linked Device not found.");
            }
          } else {
            // Cashier tapi belum dilink ke device manapun
            setErrorMsg("Your account is not assigned to any photobooth.");
          }
        }
      }
      // 2. JIKA LOGIN PAKAI USERNAME/PIN (Device Login)
      else {
        // Cari device berdasarkan username
        const { data: deviceData, error: deviceError } = await supabase
          .from("devices")
          .select("*")
          .eq("username", identifier)
          .eq("pin_code", password)
          .eq("status", "ACTIVE")
          .single();

        if (deviceError || !deviceData) {
          setErrorMsg("Invalid device credentials or device is inactive");
          return;
        }

        // Simpan device session
        localStorage.setItem(
          "kubik_device_session",
          JSON.stringify(deviceData)
        );

        // Redirect ke ticket station
        navigate("/admin/tickets");
      }
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Login failed";
      setErrorMsg(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 px-4">
      <div className="w-full max-w-md bg-white p-8 rounded-xl shadow-lg">
        <div className="text-center mb-6">
          <div className="mx-auto w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mb-2">
            <Lock className="w-6 h-6 text-blue-600" />
          </div>
          <h1 className="text-2xl font-bold">Kubik Portal</h1>
          <p className="text-sm text-gray-600 mt-1">Admin & Cashier Login</p>
        </div>

        {errorMsg && (
          <div className="mb-4 p-3 bg-red-50 text-red-600 text-sm rounded border border-red-100">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-1">
              Email or Username
            </label>
            <div className="relative">
              <Mail className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
              <input
                type="text"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                className="w-full pl-9 p-2 border rounded focus:outline-blue-500"
                required
                placeholder="email@example.com or booth_1234"
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">
              Password or PIN
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-9 p-2 border rounded focus:outline-blue-500"
                required
                placeholder="••••••••"
              />
            </div>
          </div>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? (
              <Loader2 className="animate-spin mr-2 h-4 w-4" />
            ) : (
              "Sign In"
            )}
          </Button>
        </form>

        <div className="mt-6 text-center">
          <p className="text-xs text-gray-500">
            Admin: Use email & password • Cashier: Use email & password or
            device credentials
          </p>
        </div>
      </div>
    </div>
  );
}
