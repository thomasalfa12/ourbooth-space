import { useEffect, useState } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useSearchParams,
  Outlet,
  useLocation,
} from "react-router-dom";
import { supabase } from "./lib/supabase";
import { Loader2 } from "lucide-react";
import { useUserRole } from "./hooks/useUserRole"; // [NEW]
import { Analytics } from "@vercel/analytics/react";

// Pages
import Login from "./pages/Login";
import ClientDownload from "./pages/ClientDownload";
import AdminLayout from "./components/AdminLayout";
import AdminDashboard from "./pages/AdminDashboard";
import DeviceManager from "./pages/DeviceManager";
import SessionDetail from "./pages/SessionDetail";
import SessionManager from "./pages/SessionManager";
import TicketStation from "./pages/TicketStation"; // [NEW]

// --- RBAC GUARD ---
// Komponen ini memastikan CASHIER tidak bisa akses halaman ADMIN
function RequireAuth() {
  const [session, setSession] = useState<boolean | null>(null);
  const { role, loading: roleLoading } = useUserRole(); // Ambil role
  const location = useLocation();

  useEffect(() => {
    supabase.auth
      .getSession()
      .then(({ data: { session } }) => setSession(!!session));
    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_, session) => setSession(!!session));
    return () => subscription.unsubscribe();
  }, []);

  // 1. Loading State
  if (session === null || roleLoading) {
    return (
      <div className="h-screen flex items-center justify-center flex-col gap-4">
        <Loader2 className="w-10 h-10 text-primary animate-spin" />
        <p className="text-muted-foreground text-sm font-medium">
          Verifying Access...
        </p>
      </div>
    );
  }

  // 2. Not Logged In -> Login Page
  if (!session) return <Navigate to="/login" replace />;

  // 3. Role Based Redirection (Logic Penting!)

  // Jika CASHIER mencoba akses halaman root admin atau halaman terlarang, lempar ke Ticket Station
  if (role === "CASHIER") {
    // Daftar halaman yang BOLEH diakses cashier
    const allowedPaths = ["/admin/tickets"];
    const isAllowed = allowedPaths.some((path) =>
      location.pathname.startsWith(path)
    );

    if (!isAllowed) {
      return <Navigate to="/admin/tickets" replace />;
    }
  }

  // Jika ADMIN, boleh akses semua, lanjut render Outlet
  return <Outlet />;
}

function HomeRouter() {
  const [searchParams] = useSearchParams();
  return searchParams.get("id") ? (
    <ClientDownload />
  ) : (
    <Navigate to="/login" replace />
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomeRouter />} />
        <Route path="/login" element={<Login />} />

        {/* PROTECTED ROUTES */}
        <Route element={<RequireAuth />}>
          <Route path="/admin" element={<AdminLayout />}>
            {/* ADMIN ONLY ROUTES (Implicitly protected by RequireAuth Logic) */}
            <Route index element={<AdminDashboard />} />
            <Route path="sessions" element={<SessionManager />} />
            <Route path="devices" element={<DeviceManager />} />

            {/* SHARED ROUTES (Admin & Cashier) */}
            <Route path="tickets" element={<TicketStation />} />
          </Route>

          <Route path="/session/:sessionUuid" element={<SessionDetail />} />
        </Route>

        <Route
          path="*"
          element={
            <div className="text-center p-10 font-bold text-gray-500">
              404 Not Found
            </div>
          }
        />
      </Routes>
      <Analytics />
    </BrowserRouter>
  );
}

export default App;
