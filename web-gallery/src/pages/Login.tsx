import { useState } from "react";
import { supabase } from "../lib/supabase";
import { useNavigate } from "react-router-dom";
import {
  Mail,
  Loader2,
  Camera,
  User,
  KeyRound,
  AlertCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { motion, AnimatePresence } from "framer-motion";

export default function Login() {
  const navigate = useNavigate();
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg(null);

    // Simulasi delay sedikit agar animasi loading terasa (UX: Feedback)
    // await new Promise(r => setTimeout(r, 800));

    try {
      // 1. JIKA LOGIN PAKAI EMAIL
      if (identifier.includes("@")) {
        const { data: authData, error } =
          await supabase.auth.signInWithPassword({
            email: identifier,
            password: password,
          });

        if (error) throw error;

        if (authData.session) {
          const { data: profile } = await supabase
            .from("profiles")
            .select("role, assigned_device_id")
            .eq("id", authData.session.user.id)
            .single();

          if (profile?.role === "ADMIN") {
            navigate("/admin");
          } else if (
            profile?.role === "CASHIER" &&
            profile.assigned_device_id
          ) {
            const { data: deviceData } = await supabase
              .from("devices")
              .select("*")
              .eq("id", profile.assigned_device_id)
              .single();

            if (deviceData) {
              localStorage.setItem(
                "kubik_device_session",
                JSON.stringify(deviceData)
              );
              navigate("/admin/tickets");
            } else {
              setErrorMsg("Account valid, but no Device linked.");
            }
          } else {
            setErrorMsg("Your account is not assigned to any photobooth.");
          }
        }
      }
      // 2. JIKA LOGIN PAKAI USERNAME (DEVICE)
      else {
        const { data: deviceData, error: deviceError } = await supabase
          .from("devices")
          .select("*")
          .eq("username", identifier)
          .eq("pin_code", password)
          .eq("status", "ACTIVE")
          .single();

        if (deviceError || !deviceData) {
          setErrorMsg("Invalid credentials or device inactive.");
          return;
        }

        localStorage.setItem(
          "kubik_device_session",
          JSON.stringify(deviceData)
        );
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
    <div className="min-h-screen w-full flex items-center justify-center bg-kubik-bg relative overflow-hidden font-sans text-kubik-black">
      {/* --- BACKGROUND DECORATION --- */}
      <div className="absolute inset-0 pointer-events-none">
        <div
          className="absolute inset-0 opacity-[0.03]"
          style={{
            backgroundImage: "radial-gradient(#000 1px, transparent 1px)",
            backgroundSize: "24px 24px",
          }}
        />
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[800px] h-[500px] bg-kubik-blue/5 rounded-full blur-3xl" />
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="w-full max-w-[400px] px-6 relative z-10"
      >
        {/* --- HEADER --- */}
        <div className="text-center mb-8 space-y-3">
          <motion.div
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ delay: 0.1 }}
            className="w-16 h-16 bg-linear-to-tr from-kubik-blue to-kubik-blueLight rounded-2xl mx-auto flex items-center justify-center shadow-lg shadow-kubik-blue/30 mb-6"
          >
            <Camera className="w-8 h-8 text-white" strokeWidth={2} />
          </motion.div>
          <h1 className="text-3xl font-bold tracking-tight text-kubik-black">
            Welcome Back
          </h1>
          <p className="text-kubik-grey text-sm">
            Enter your credentials to access the portal.
          </p>
        </div>

        {/* --- FORM CARD --- */}
        <div className="bg-white/80 backdrop-blur-xl border border-white/50 p-1 rounded-3xl shadow-2xl shadow-gray-200/50">
          <div className="bg-white rounded-[20px] p-6 space-y-6 border border-gray-100">
            {/* Error Message Animation */}
            <AnimatePresence>
              {errorMsg && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  className="overflow-hidden"
                >
                  <div className="flex items-center gap-2 p-3 bg-red-50 border border-red-100 rounded-xl text-xs font-medium text-red-600">
                    <AlertCircle size={16} />
                    {errorMsg}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            <form onSubmit={handleLogin} className="space-y-4">
              {/* INPUT 1: IDENTITY */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-kubik-black uppercase tracking-wider ml-1">
                  Identity
                </label>
                <div className="group relative transition-all duration-300">
                  <div className="absolute left-3 top-3 text-gray-400 group-focus-within:text-kubik-blue transition-colors">
                    {identifier.includes("@") ? (
                      <Mail size={18} />
                    ) : (
                      <User size={18} />
                    )}
                  </div>
                  <input
                    type="text"
                    value={identifier}
                    onChange={(e) => setIdentifier(e.target.value)}
                    className="w-full bg-gray-50 border border-gray-200 text-kubik-black text-sm rounded-xl py-3 pl-10 pr-4 outline-none focus:bg-white focus:border-kubik-blue focus:ring-4 focus:ring-kubik-blue/10 transition-all placeholder:text-gray-400"
                    required
                    placeholder="Email or Device ID"
                  />
                </div>
              </div>

              {/* INPUT 2: PASSWORD */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-kubik-black uppercase tracking-wider ml-1">
                  Passcode
                </label>
                <div className="group relative transition-all duration-300">
                  <div className="absolute left-3 top-3 text-gray-400 group-focus-within:text-kubik-blue transition-colors">
                    <KeyRound size={18} />
                  </div>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full bg-gray-50 border border-gray-200 text-kubik-black text-sm rounded-xl py-3 pl-10 pr-4 outline-none focus:bg-white focus:border-kubik-blue focus:ring-4 focus:ring-kubik-blue/10 transition-all placeholder:text-gray-400"
                    required
                    placeholder="Password or PIN"
                  />
                </div>
              </div>

              <div className="pt-2">
                <Button
                  type="submit"
                  disabled={loading}
                  className="w-full h-12 bg-kubik-blue hover:bg-kubik-blueLight text-white font-bold rounded-xl shadow-lg shadow-kubik-blue/20 transition-all active:scale-[0.98]"
                >
                  {loading ? (
                    <div className="flex items-center gap-2">
                      <Loader2 className="animate-spin h-5 w-5" />
                      <span>Verifying...</span>
                    </div>
                  ) : (
                    "Sign In"
                  )}
                </Button>
              </div>
            </form>
          </div>
        </div>

        {/* --- FOOTER --- */}
        <div className="mt-8 text-center space-y-2">
          <p className="text-xs text-gray-400 font-medium">
            Protected by Kubik Secure Access
          </p>
        </div>
      </motion.div>
    </div>
  );
}
