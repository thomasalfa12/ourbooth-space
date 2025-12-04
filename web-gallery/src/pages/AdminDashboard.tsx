import { useEffect, useState, useCallback, useMemo } from "react";
import { supabase } from "../lib/supabase";
import { useNavigate } from "react-router-dom";
import {
  Trash2,
  RefreshCw,
  Smartphone,
  ExternalLink,
  Image as ImageIcon,
  TrendingUp,
  Activity,
  ArrowUpRight,
  Calendar,
} from "lucide-react";
import type { RealtimePostgresChangesPayload } from "@supabase/supabase-js";
import type { Session, SessionInsertPayload, Device } from "../types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { motion } from "framer-motion";

// 1. DEFINISI TYPE YANG JELAS (CLEAN CODE: NO ANY)
interface StatCardProps {
  title: string;
  value: string | number;
  sub: string;
  icon: React.ReactNode;
  trend: string;
  color: string; // Tailwind class string
  pulse?: boolean;
  alert?: boolean;
  isCurrency?: boolean;
}

interface ChartDataPoint {
  name: string;
  sessions: number;
  fullDate: string;
}

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [sessions, setSessions] = useState<Session[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);

  // --- DATA FETCHING ---
  const fetchSessions = useCallback(async () => {
    setLoading(true);
    try {
      const [sessionsData, devicesData] = await Promise.all([
        supabase
          .from("sessions")
          .select("*")
          .order("created_at", { ascending: false })
          .limit(50),
        supabase
          .from("devices")
          .select("*")
          .order("created_at", { ascending: false }),
      ]);

      if (!sessionsData.error && sessionsData.data) {
        setSessions(sessionsData.data as Session[]);
      }

      if (!devicesData.error && devicesData.data) {
        setDevices(devicesData.data as Device[]);
      }
    } catch (err) {
      console.error("Failed to fetch data:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSessions();
    const subscription = supabase
      .channel("sessions")
      .on(
        "postgres_changes",
        { event: "INSERT", schema: "public", table: "sessions" },
        (payload: RealtimePostgresChangesPayload<SessionInsertPayload>) => {
          const newSession = payload.new as Session;
          setSessions((prev) => [newSession, ...prev]);
        }
      )
      .subscribe();

    return () => {
      subscription.unsubscribe();
    };
  }, [fetchSessions]);

  // --- DELETE LOGIC ---
  async function deleteSession(id: number) {
    if (!window.confirm("Delete this session record permanently?")) return;
    const { error } = await supabase.from("sessions").delete().eq("id", id);
    if (!error) {
      setSessions((prev) => prev.filter((s) => s.id !== id));
    }
  }

  // --- STATISTICS CALCULATION ---
  const stats = useMemo(() => {
    const total = sessions.length;

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayCount = sessions.filter(
      (s) => new Date(s.created_at) >= today
    ).length;

    const activeDevs = devices.filter((d) => d.status === "ACTIVE").length;
    const revenue = total * 35000;

    return { total, todayCount, activeDevs, revenue };
  }, [sessions, devices]);

  // --- CHART DATA PREPARATION ---
  const chartData: ChartDataPoint[] = useMemo(() => {
    const last7Days = Array.from({ length: 7 }, (_, i) => {
      const d = new Date();
      d.setDate(d.getDate() - (6 - i));
      return d.toISOString().split("T")[0];
    });

    return last7Days.map((date) => {
      const count = sessions.filter((s) =>
        s.created_at.startsWith(date)
      ).length;
      return {
        name: new Date(date).toLocaleDateString("en-US", { weekday: "short" }),
        sessions: count,
        fullDate: date,
      };
    });
  }, [sessions]);

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
      {/* --- HEADER --- */}
      <div className="flex flex-col sm:flex-row justify-between items-end gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-kubik-black tracking-tight">
            Dashboard Overview
          </h1>
          <p className="text-kubik-grey mt-1 font-medium">
            Here's what's happening in your ecosystem today.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-xs font-bold text-kubik-grey/60 bg-white px-3 py-1.5 rounded-lg border border-gray-100 shadow-sm">
            Last updated: {new Date().toLocaleTimeString()}
          </span>
          <Button
            onClick={fetchSessions}
            variant="outline"
            size="sm"
            className="bg-white border-gray-200 hover:bg-gray-50 text-kubik-black"
          >
            <RefreshCw
              className={`mr-2 h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`}
            />
            Refresh Data
          </Button>
        </div>
      </div>

      {/* --- STATS GRID --- */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Sessions"
          value={stats.total}
          sub="Lifetime photos taken"
          icon={<ImageIcon className="text-white" size={18} />}
          trend="+12% from last month"
          color="bg-kubik-blue"
        />
        <StatCard
          title="Today's Activity"
          value={stats.todayCount}
          sub="Sessions in last 24h"
          icon={<Activity className="text-white" size={18} />}
          trend="Live updates"
          color="bg-purple-500"
          pulse
        />
        <StatCard
          title="Active Devices"
          value={`${stats.activeDevs}/${devices.length}`}
          sub="Booths currently online"
          icon={<Smartphone className="text-white" size={18} />}
          trend={
            stats.activeDevs === devices.length
              ? "All systems operational"
              : "Check inactive units"
          }
          color="bg-emerald-500"
          alert={stats.activeDevs < devices.length}
        />
        <StatCard
          title="Est. Revenue"
          value={`Rp ${stats.revenue.toLocaleString()}`}
          sub="Gross volume"
          icon={<TrendingUp className="text-white" size={18} />}
          trend="Based on Rp 35k/session"
          color="bg-kubik-gold"
          isCurrency
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* --- MAIN CHART SECTION --- */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-gray-100 shadow-sm p-6 relative overflow-hidden group">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-lg font-bold text-kubik-black">
                Weekly Trends
              </h3>
              <p className="text-sm text-kubik-grey">
                Session volume over the last 7 days
              </p>
            </div>
            <div className="flex gap-2">
              <div className="flex items-center gap-2 text-xs font-medium text-kubik-grey">
                <span className="w-2 h-2 rounded-full bg-kubik-blue"></span>
                Sessions
              </div>
            </div>
          </div>

          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient
                    id="colorSessions"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >
                    <stop offset="5%" stopColor="#2962FF" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#2962FF" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid
                  strokeDasharray="3 3"
                  vertical={false}
                  stroke="#F1F5F9"
                />
                <XAxis
                  dataKey="name"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#94A3B8", fontSize: 12 }}
                  dy={10}
                />
                <YAxis
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#94A3B8", fontSize: 12 }}
                />
                <Tooltip
                  contentStyle={{
                    borderRadius: "12px",
                    border: "none",
                    boxShadow: "0 4px 20px rgba(0,0,0,0.05)",
                  }}
                  itemStyle={{ color: "#2962FF", fontWeight: "bold" }}
                />
                <Area
                  type="monotone"
                  dataKey="sessions"
                  stroke="#2962FF"
                  strokeWidth={3}
                  fillOpacity={1}
                  fill="url(#colorSessions)"
                  activeDot={{ r: 6, strokeWidth: 0, fill: "#2962FF" }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* --- QUICK ACTIONS PANEL (CLEAN CODE: Standard Values) --- */}
        {/* Note: bg-gradient-to-br is standard v3. Use bg-linear-to-br only if on v4/custom config */}
        <div className="bg-gradient-to-br from-kubik-blue to-blue-700 rounded-2xl shadow-xl shadow-blue-500/20 p-6 text-white relative overflow-hidden">
          {/* FIX: Mengganti arbitrary values [-20px] dengan standard spacing scale 
                -20px = -5 (karena 1 unit = 4px, jadi 5 * 4 = 20)
             */}
          <div className="absolute -top-5 -right-5 w-32 h-32 bg-white/10 rounded-full blur-2xl pointer-events-none" />
          <div className="absolute -bottom-5 -left-5 w-40 h-40 bg-black/10 rounded-full blur-2xl pointer-events-none" />

          <h3 className="text-xl font-bold mb-1 relative z-10">
            Quick Actions
          </h3>
          <p className="text-blue-100 text-sm mb-6 relative z-10">
            Manage your booth ecosystem efficiently.
          </p>

          <div className="space-y-3 relative z-10">
            <Button
              variant="secondary"
              className="w-full justify-between bg-white/10 hover:bg-white/20 text-white border-0 h-12"
              onClick={() => navigate("/admin/sessions")}
            >
              <span>View All Sessions</span>
              <ArrowUpRight size={16} />
            </Button>
            <Button
              variant="secondary"
              className="w-full justify-between bg-white/10 hover:bg-white/20 text-white border-0 h-12"
              onClick={() => navigate("/admin/tickets")}
            >
              <span>Issue New Ticket</span>
              <Calendar size={16} />
            </Button>
            <Button
              variant="secondary"
              className="w-full justify-between bg-white/10 hover:bg-white/20 text-white border-0 h-12"
              onClick={() => navigate("/admin/devices")}
            >
              <span>Add Device</span>
              <Smartphone size={16} />
            </Button>
          </div>

          <div className="mt-8 pt-6 border-t border-white/10">
            <p className="text-xs font-medium text-blue-200 uppercase tracking-widest mb-2">
              System Status
            </p>
            <div className="flex items-center gap-2 text-sm font-semibold">
              <span className="w-2.5 h-2.5 bg-emerald-400 rounded-full animate-pulse shadow-[0_0_8px_rgba(52,211,153,0.6)]"></span>
              Operational
            </div>
          </div>
        </div>
      </div>

      {/* --- RECENT SESSIONS TABLE --- */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-gray-50 flex justify-between items-center">
          <h3 className="text-lg font-bold text-kubik-black">
            Recent Live Sessions
          </h3>
          <Button
            variant="ghost"
            size="sm"
            className="text-kubik-blue"
            onClick={() => navigate("/admin/sessions")}
          >
            View Full Log
          </Button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-gray-50/50 text-kubik-grey font-semibold uppercase text-[11px] tracking-wider">
              <tr>
                <th className="px-6 py-4">Preview</th>
                <th className="px-6 py-4">Session UUID</th>
                <th className="px-6 py-4">Timestamp</th>
                <th className="px-6 py-4">Device</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loading ? (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-gray-400">
                    Loading live data...
                  </td>
                </tr>
              ) : sessions.length === 0 ? (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-gray-400">
                    No sessions recorded yet.
                  </td>
                </tr>
              ) : (
                sessions.slice(0, 5).map((session, idx) => (
                  <motion.tr
                    key={session.id}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: idx * 0.05 }}
                    className="group hover:bg-blue-50/30 transition-colors"
                  >
                    <td className="px-6 py-3">
                      <div className="w-12 h-12 rounded-lg bg-gray-100 border border-gray-200 overflow-hidden relative shadow-sm group-hover:shadow-md transition-all">
                        {session.final_photo_url ? (
                          <img
                            src={session.final_photo_url}
                            className="w-full h-full object-cover"
                            alt="Thumb"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-gray-300">
                            <ImageIcon size={16} />
                          </div>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-3">
                      <p className="font-mono font-medium text-kubik-black">
                        {session.session_uuid.substring(0, 8)}
                      </p>
                      <p className="text-[10px] text-gray-400">UUID</p>
                    </td>
                    <td className="px-6 py-3">
                      <div className="flex flex-col">
                        <span className="font-bold text-gray-700">
                          {new Date(session.created_at).toLocaleTimeString([], {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </span>
                        <span className="text-[10px] text-gray-400">
                          {new Date(session.created_at).toLocaleDateString()}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-3">
                      <Badge
                        variant="outline"
                        className="bg-white hover:bg-white text-gray-500 border-gray-200 font-mono text-[10px]"
                      >
                        {session.device_id
                          ? session.device_id.substring(0, 6)
                          : "UNK"}
                      </Badge>
                    </td>
                    <td className="px-6 py-3 text-right">
                      <div className="flex justify-end gap-1 opacity-60 group-hover:opacity-100 transition-opacity">
                        <Button
                          size="icon"
                          variant="ghost"
                          className="h-8 w-8 text-kubik-blue hover:bg-blue-100"
                          onClick={() =>
                            navigate(`/session/${session.session_uuid}`)
                          }
                        >
                          <ExternalLink size={14} />
                        </Button>
                        <Button
                          size="icon"
                          variant="ghost"
                          className="h-8 w-8 text-red-400 hover:bg-red-50 hover:text-red-600"
                          onClick={() => deleteSession(session.id)}
                        >
                          <Trash2 size={14} />
                        </Button>
                      </div>
                    </td>
                  </motion.tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// 2. SUB-COMPONENT DENGAN TYPE YANG JELAS
function StatCard({
  title,
  value,
  sub,
  icon,
  trend,
  color,
  pulse,
  alert,
  isCurrency,
}: StatCardProps) {
  return (
    <div className="bg-white rounded-2xl p-5 border border-gray-100 shadow-[0_2px_10px_rgba(0,0,0,0.02)] hover:shadow-[0_8px_30px_rgba(0,0,0,0.04)] transition-all duration-300 group">
      <div className="flex justify-between items-start mb-4">
        <div
          className={`w-10 h-10 rounded-xl flex items-center justify-center shadow-lg ${color} ${
            pulse ? "animate-pulse" : ""
          } ${alert ? "bg-red-500" : ""}`}
        >
          {icon}
        </div>
        {pulse && (
          <span className="flex h-3 w-3 relative">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500"></span>
          </span>
        )}
      </div>
      <div>
        <p className="text-sm font-medium text-kubik-grey mb-1">{title}</p>
        <h3
          className={`text-2xl font-extrabold text-kubik-black tracking-tight ${
            isCurrency ? "text-kubik-blue" : ""
          }`}
        >
          {value}
        </h3>
        <div className="flex items-center gap-2 mt-2">
          <TrendingUp
            size={12}
            className={alert ? "text-red-500" : "text-emerald-500"}
          />
          <p
            className={`text-xs font-bold ${
              alert ? "text-red-500" : "text-emerald-600"
            }`}
          >
            {trend}
          </p>
        </div>
        <p className="text-[10px] text-gray-400 mt-1">{sub}</p>
      </div>
    </div>
  );
}
