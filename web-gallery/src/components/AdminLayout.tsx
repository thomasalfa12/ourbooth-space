import { Outlet, Link, useLocation, useNavigate } from "react-router-dom";
import { supabase } from "../lib/supabase";
import {
  LogOut,
  LayoutDashboard,
  Smartphone,
  Ticket,
  Hexagon,
  Layers,
  Search,
  Bell,
  ChevronRight,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useUserRole } from "../hooks/useUserRole";
import { motion, AnimatePresence } from "framer-motion";

// --- TYPES (Clean Code: Defined Locally or Imported) ---
interface NavItem {
  id: string;
  label: string;
  path: string;
  icon: React.ReactNode;
  role: "ADMIN" | "CASHIER" | "ALL";
  disabled?: boolean;
}

export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { role, loading } = useUserRole();

  async function handleLogout() {
    await supabase.auth.signOut();
    navigate("/login");
  }

  // --- CONFIGURATION ---
  const allNavItems: NavItem[] = [
    {
      id: "dashboard",
      label: "Overview",
      path: "/admin",
      icon: <LayoutDashboard size={20} />,
      role: "ADMIN",
    },
    {
      id: "sessions",
      label: "Gallery",
      path: "/admin/sessions",
      icon: <Layers size={20} />,
      role: "ADMIN",
    },
    {
      id: "devices",
      label: "Devices",
      path: "/admin/devices",
      icon: <Smartphone size={20} />,
      role: "ADMIN",
    },
    {
      id: "tickets",
      label: "Station",
      path: "/admin/tickets",
      icon: <Ticket size={20} />,
      role: "ALL",
    },
  ];

  const navItems = allNavItems.filter((item) => {
    if (loading) return false;
    if (role === "ADMIN") return true;
    if (role === "CASHIER") return item.role === "ALL";
    return false;
  });

  if (loading) return null;

  return (
    <div className="min-h-screen bg-[#FAFAFA] flex font-sans selection:bg-blue-600 selection:text-white overflow-hidden">
      {/* ========================================
          DESKTOP SIDEBAR (Static + Motion)
          ========================================
      */}
      <aside className="hidden md:flex w-[260px] flex-col h-screen sticky top-0 border-r border-gray-200/60 bg-white/80 backdrop-blur-xl z-40">
        {/* BRAND HEADER */}
        <div className="h-20 flex items-center gap-3 px-6">
          <div className="w-9 h-9 bg-blue-600 rounded-xl flex items-center justify-center text-white shadow-lg shadow-blue-600/20">
            <Hexagon size={18} strokeWidth={3} />
          </div>
          <div>
            <h1 className="text-lg font-extrabold text-slate-900 tracking-tight leading-none">
              KUBIK
            </h1>
            <p className="text-[10px] font-bold text-slate-400 tracking-widest uppercase mt-0.5">
              Admin Space
            </p>
          </div>
        </div>

        {/* NAVIGATION STACK */}
        <nav className="flex-1 px-3 py-6 space-y-1">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.id}
                to={item.path}
                className="block relative group"
              >
                <div
                  className={cn(
                    "relative flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold transition-all duration-200 z-10",
                    isActive
                      ? "text-blue-600"
                      : "text-slate-500 hover:text-slate-900 hover:bg-slate-50"
                  )}
                >
                  {/* MOTION: Sliding Active Background */}
                  {isActive && (
                    <motion.div
                      layoutId="sidebarActiveBg"
                      className="absolute inset-0 bg-blue-50/80 rounded-xl border border-blue-100"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      transition={{
                        type: "spring",
                        stiffness: 300,
                        damping: 30,
                      }}
                    />
                  )}

                  <span className="relative z-10">{item.icon}</span>
                  <span className="relative z-10">{item.label}</span>

                  {isActive && (
                    <motion.div
                      initial={{ opacity: 0, x: -5 }}
                      animate={{ opacity: 1, x: 0 }}
                      className="ml-auto relative z-10"
                    >
                      <ChevronRight size={14} />
                    </motion.div>
                  )}
                </div>
              </Link>
            );
          })}
        </nav>

        {/* FOOTER PROFILE */}
        <div className="p-4 border-t border-gray-100/50">
          <div className="flex items-center gap-3 p-3 rounded-2xl bg-gray-50/50 border border-gray-100 hover:border-gray-200 transition-colors group">
            <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center text-white font-bold text-xs shadow-sm">
              A
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-bold text-slate-900 truncate">
                Admin User
              </p>
              <p className="text-[10px] text-slate-400 truncate">
                Super Access
              </p>
            </div>
            <button
              onClick={handleLogout}
              className="w-8 h-8 flex items-center justify-center rounded-full text-gray-400 hover:text-red-500 hover:bg-red-50 transition-all opacity-0 group-hover:opacity-100"
              title="Sign Out"
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </aside>

      {/* ========================================
          MAIN CONTENT (The Stage)
          ========================================
      */}
      <div className="flex-1 flex flex-col h-screen overflow-hidden relative">
        {/* GLOBAL HEADER */}
        <header className="h-16 md:h-20 px-4 md:px-8 flex items-center justify-between bg-[#FAFAFA]/80 backdrop-blur-md sticky top-0 z-30">
          {/* Mobile Brand (Only Visible on Mobile) */}
          <div className="md:hidden flex items-center gap-2">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center text-white">
              <Hexagon size={18} strokeWidth={3} />
            </div>
            <span className="font-bold text-slate-900">KUBIK</span>
          </div>

          {/* Global Search Bar (Desktop Only) */}
          <div className="hidden md:flex items-center gap-3 w-full max-w-md bg-white px-4 py-2.5 rounded-full border border-gray-200/60 shadow-sm focus-within:ring-2 focus-within:ring-blue-500/10 focus-within:border-blue-500/20 transition-all">
            <Search size={16} className="text-gray-400" />
            <input
              type="text"
              placeholder="Command Center..."
              className="bg-transparent border-none outline-none text-sm w-full placeholder:text-gray-400 text-slate-700"
            />
            <kbd className="hidden lg:inline-flex h-5 items-center gap-1 rounded border border-gray-200 bg-gray-50 px-1.5 font-mono text-[10px] font-medium text-gray-500 opacity-100">
              <span className="text-xs">⌘</span>K
            </kbd>
          </div>

          {/* Header Actions */}
          <div className="flex items-center gap-2 md:gap-4">
            <button className="relative w-10 h-10 flex items-center justify-center rounded-full bg-white border border-gray-100 text-gray-400 hover:text-blue-600 hover:border-blue-100 hover:shadow-sm transition-all">
              <Bell size={18} />
              <span className="absolute top-2 right-2.5 w-2 h-2 bg-red-500 rounded-full border-2 border-white" />
            </button>

            {/* Mobile Logout (Simple) */}
            <button
              onClick={handleLogout}
              className="md:hidden w-10 h-10 flex items-center justify-center rounded-full bg-white border border-gray-100 text-gray-400"
            >
              <LogOut size={18} />
            </button>
          </div>
        </header>

        {/* CONTENT AREA WITH TRANSITION */}
        <main className="flex-1 overflow-y-auto p-4 md:p-8 pb-32 md:pb-8 scroll-smooth">
          <div className="max-w-7xl mx-auto">
            {/* MOTION: Page Transition Fade In/Up */}
            <AnimatePresence mode="wait">
              <motion.div
                key={location.pathname}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.25, ease: "easeOut" }}
              >
                <Outlet />
              </motion.div>
            </AnimatePresence>
          </div>
        </main>

        {/* ========================================
            MOBILE DOCK (Floating Island)
            ========================================
        */}
        <div className="md:hidden fixed bottom-6 inset-x-0 flex justify-center z-50 pointer-events-none">
          <nav className="bg-white/90 backdrop-blur-xl border border-white/20 shadow-[0_8px_30px_rgba(0,0,0,0.12)] rounded-2xl p-1.5 flex items-center gap-1 ring-1 ring-black/5 pointer-events-auto max-w-[90%]">
            {navItems.map((item) => {
              const isActive = location.pathname === item.path;
              return (
                <Link key={item.id} to={item.path} className="relative group">
                  <div className="px-5 py-3 flex flex-col items-center justify-center gap-1 min-w-[64px]">
                    {/* MOTION: Sliding Pill for Mobile */}
                    {isActive && (
                      <motion.div
                        layoutId="mobileNavPill"
                        className="absolute inset-0 bg-gray-100 rounded-xl"
                        initial={false}
                        transition={{
                          type: "spring",
                          stiffness: 500,
                          damping: 30,
                        }}
                      />
                    )}

                    <div className="relative z-10 flex flex-col items-center gap-0.5">
                      <motion.span
                        animate={{
                          y: isActive ? -2 : 0,
                          scale: isActive ? 1.1 : 1,
                          color: isActive ? "#2563EB" : "#94A3B8",
                        }}
                        className="transition-colors duration-200"
                      >
                        {item.icon}
                      </motion.span>
                      {/* Optional: Label on active, or just dot */}
                      {isActive && (
                        <motion.div
                          initial={{ opacity: 0, scale: 0 }}
                          animate={{ opacity: 1, scale: 1 }}
                          className="w-1 h-1 bg-blue-600 rounded-full"
                        />
                      )}
                    </div>
                  </div>
                </Link>
              );
            })}
          </nav>
        </div>
      </div>
    </div>
  );
}
