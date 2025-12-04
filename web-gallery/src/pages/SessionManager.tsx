import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { supabase } from "../lib/supabase";
import {
  Trash2,
  RefreshCw,
  Smartphone,
  ExternalLink,
  Image as ImageIcon,
  Calendar,
  Grid3X3,
  List as ListIcon,
  Search,
  Filter,
  Check,
  MoreHorizontal,
  Clock,
  Layers,
} from "lucide-react";
import type { RealtimePostgresChangesPayload } from "@supabase/supabase-js";
import type { Session, SessionInsertPayload } from "../types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

// --- TYPES & INTERFACES (Clean Code) ---
type ViewMode = "table" | "grid";
type SortOption = "newest" | "oldest";

// Menghilangkan 'any' dengan mendefinisikan props yang jelas
interface MetricCardProps {
  label: string;
  value: number | string;
  icon: React.ReactNode;
  color?: string;
}

export default function SessionManager() {
  const navigate = useNavigate();

  // --- STATE ---
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [sortOption, setSortOption] = useState<SortOption>("newest");
  const [viewMode, setViewMode] = useState<ViewMode>("table");
  const [selectedSessions, setSelectedSessions] = useState<number[]>([]);

  // --- DATA FETCHING ---
  const fetchSessions = useCallback(async () => {
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from("sessions")
        .select("*")
        .order("created_at", { ascending: false });

      if (!error && data) {
        setSessions(data as Session[]);
      }
    } catch (err) {
      console.error("Failed to fetch sessions:", err);
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

  // --- LOGIC HELPERS ---
  const getRawPhotos = (session: Session): string[] => {
    if (!session.raw_photos_urls) return [];
    try {
      return JSON.parse(session.raw_photos_urls);
    } catch {
      return [];
    }
  };

  const getSessionStatus = (session: Session) => {
    const hasFinal = !!session.final_photo_url;
    const rawCount = getRawPhotos(session).length;

    if (hasFinal && rawCount > 0)
      return {
        label: "Complete",
        color: "text-emerald-600 bg-emerald-50 border-emerald-100",
        dot: "bg-emerald-500",
      };
    if (hasFinal)
      return {
        label: "Final Only",
        color: "text-blue-600 bg-blue-50 border-blue-100",
        dot: "bg-blue-500",
      };
    if (rawCount > 0)
      return {
        label: "Raw Only",
        color: "text-orange-600 bg-orange-50 border-orange-100",
        dot: "bg-orange-500",
      };
    return {
      label: "Empty",
      color: "text-gray-500 bg-gray-50 border-gray-100",
      dot: "bg-gray-400",
    };
  };

  // --- ACTIONS ---
  const handleSelect = (id: number) => {
    setSelectedSessions((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleSelectAll = () => {
    if (selectedSessions.length === filteredSessions.length) {
      setSelectedSessions([]);
    } else {
      setSelectedSessions(filteredSessions.map((s) => s.id));
    }
  };

  const handleDelete = async (ids: number[]) => {
    if (!confirm(`Permanently delete ${ids.length} session(s)?`)) return;

    const { error } = await supabase.from("sessions").delete().in("id", ids);
    if (!error) {
      setSessions((prev) => prev.filter((s) => !ids.includes(s.id)));
      setSelectedSessions([]);
    }
  };

  // --- FILTERING & SORTING ---
  const filteredSessions = useMemo(() => {
    let result = sessions.filter(
      (s) =>
        s.session_uuid.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (s.device_id &&
          s.device_id.toLowerCase().includes(searchTerm.toLowerCase()))
    );

    if (sortOption === "oldest") {
      result = result.sort(
        (a, b) =>
          new Date(a.created_at).getTime() - new Date(b.created_at).getTime()
      );
    } else {
      result = result.sort(
        (a, b) =>
          new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
      );
    }

    return result;
  }, [sessions, searchTerm, sortOption]);

  // --- RENDER HELPERS ---
  if (loading && sessions.length === 0) {
    return (
      <div className="h-[80vh] flex flex-col items-center justify-center gap-4">
        <div className="w-12 h-12 border-4 border-kubik-blue/20 border-t-kubik-blue rounded-full animate-spin" />
        <p className="text-kubik-grey animate-pulse text-sm font-medium">
          Syncing Gallery...
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-8 min-h-screen relative">
      {/* 1. HEADER SECTION */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-kubik-black tracking-tight">
            Session Gallery
          </h1>
          <p className="text-kubik-grey mt-1 font-medium">
            Manage photo assets and transaction history.
          </p>
        </div>

        {/* Toolbar */}
        <div className="flex items-center gap-2 bg-white p-1 rounded-xl border border-gray-200 shadow-sm">
          {/* Search */}
          <div className="relative group">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4 group-focus-within:text-kubik-blue transition-colors" />
            <Input
              placeholder="Search UUID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9 h-9 w-[180px] lg:w-[250px] border-0 bg-transparent focus-visible:ring-0 placeholder:text-gray-400"
            />
          </div>

          <div className="w-px h-5 bg-gray-200 mx-1" />

          {/* View Toggle */}
          <div className="flex bg-gray-100 rounded-lg p-0.5 relative">
            <motion.div
              className="absolute inset-y-0.5 bg-white rounded-md shadow-sm"
              layoutId="sessionViewModePill"
              initial={false}
              animate={{
                x: viewMode === "table" ? 0 : "100%",
                width: "50%",
              }}
              transition={{ type: "spring", stiffness: 500, damping: 30 }}
            />
            <button
              onClick={() => setViewMode("table")}
              className={cn(
                "relative z-10 p-1.5 rounded-md transition-colors",
                viewMode === "table" ? "text-kubik-black" : "text-gray-400"
              )}
            >
              <ListIcon size={16} />
            </button>
            <button
              onClick={() => setViewMode("grid")}
              className={cn(
                "relative z-10 p-1.5 rounded-md transition-colors",
                viewMode === "grid" ? "text-kubik-black" : "text-gray-400"
              )}
            >
              <Grid3X3 size={16} />
            </button>
          </div>

          {/* Sort Dropdown */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="h-9 px-2 text-kubik-grey hover:text-kubik-black"
              >
                <Filter size={16} className="mr-2" />
                Sort
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-40">
              <DropdownMenuLabel>Sort Order</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => setSortOption("newest")}>
                Newest First{" "}
                {sortOption === "newest" && (
                  <Check size={14} className="ml-auto" />
                )}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => setSortOption("oldest")}>
                Oldest First{" "}
                {sortOption === "oldest" && (
                  <Check size={14} className="ml-auto" />
                )}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          <Button
            variant="ghost"
            size="icon"
            className="h-9 w-9 text-kubik-grey hover:text-kubik-blue"
            onClick={fetchSessions}
          >
            <RefreshCw size={16} />
          </Button>
        </div>
      </div>

      {/* 2. STATS OVERVIEW */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <MetricCard
          label="Total Sessions"
          value={sessions.length}
          icon={<Layers size={16} />}
        />
        <MetricCard
          label="Photos Stored"
          value={sessions.reduce(
            (acc, s) =>
              acc + (s.final_photo_url ? 1 : 0) + getRawPhotos(s).length,
            0
          )}
          icon={<ImageIcon size={16} />}
        />
        <MetricCard
          label="Completed"
          value={sessions.filter((s) => !!s.final_photo_url).length}
          icon={<Check size={16} />}
          color="text-emerald-600"
        />
        <MetricCard
          label="Pending/Raw"
          value={sessions.filter((s) => !s.final_photo_url).length}
          icon={<Clock size={16} />}
          color="text-orange-600"
        />
      </div>

      {/* 3. CONTENT AREA */}
      {filteredSessions.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col items-center justify-center py-20 bg-white rounded-2xl border border-dashed border-gray-200"
        >
          <div className="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mb-4">
            <Search className="text-gray-300" size={32} />
          </div>
          <h3 className="text-lg font-bold text-kubik-black">
            No sessions found
          </h3>
          <p className="text-kubik-grey text-sm">
            Try adjusting your filters or search terms.
          </p>
        </motion.div>
      ) : (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.2 }}
        >
          {viewMode === "table" ? (
            // --- TABLE VIEW ---
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
              <table className="w-full text-sm text-left">
                <thead className="bg-gray-50/50 text-kubik-grey font-semibold uppercase text-[11px] tracking-wider border-b border-gray-100">
                  <tr>
                    <th className="w-12 px-6 py-4">
                      <input
                        type="checkbox"
                        className="rounded border-gray-300 text-kubik-blue focus:ring-kubik-blue"
                        checked={
                          selectedSessions.length === filteredSessions.length &&
                          filteredSessions.length > 0
                        }
                        onChange={handleSelectAll}
                      />
                    </th>
                    <th className="px-6 py-4">Preview</th>
                    <th className="px-6 py-4">Session Info</th>
                    <th className="px-6 py-4">Status</th>
                    <th className="px-6 py-4">Device</th>
                    <th className="px-6 py-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {filteredSessions.map((session) => {
                    const status = getSessionStatus(session);
                    const isSelected = selectedSessions.includes(session.id);
                    return (
                      <tr
                        key={session.id}
                        className={cn(
                          "group transition-colors",
                          isSelected ? "bg-blue-50/50" : "hover:bg-gray-50/50"
                        )}
                      >
                        <td className="px-6 py-4">
                          <input
                            type="checkbox"
                            className="rounded border-gray-300 text-kubik-blue focus:ring-kubik-blue cursor-pointer"
                            checked={isSelected}
                            onChange={() => handleSelect(session.id)}
                          />
                        </td>
                        <td className="px-6 py-4">
                          <div
                            className="w-16 h-12 bg-gray-100 rounded-lg border border-gray-200 overflow-hidden relative cursor-pointer"
                            onClick={() =>
                              window.open(
                                `/session/${session.session_uuid}`,
                                "_blank"
                              )
                            }
                          >
                            {session.final_photo_url ? (
                              <img
                                src={session.final_photo_url}
                                className="w-full h-full object-cover"
                                alt="Thumb"
                                loading="lazy"
                              />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center text-gray-300">
                                <ImageIcon size={16} />
                              </div>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <p className="font-mono text-xs font-bold text-kubik-black">
                            {session.session_uuid.substring(0, 8)}...
                          </p>
                          <div className="flex items-center gap-1.5 text-[11px] text-gray-400 mt-1">
                            <Calendar size={10} />
                            {new Date(session.created_at).toLocaleDateString()}
                            <span className="w-0.5 h-0.5 bg-gray-300 rounded-full" />
                            {new Date(session.created_at).toLocaleTimeString(
                              [],
                              { hour: "2-digit", minute: "2-digit" }
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <Badge
                            variant="outline"
                            className={cn(
                              "text-[10px] font-bold border rounded-full px-2.5 py-0.5",
                              status.color
                            )}
                          >
                            <span
                              className={cn(
                                "w-1.5 h-1.5 rounded-full mr-1.5",
                                status.dot
                              )}
                            />
                            {status.label}
                          </Badge>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-1.5 text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded-md w-fit">
                            <Smartphone size={12} />
                            {session.device_id
                              ? session.device_id.substring(0, 6)
                              : "UNK"}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-8 w-8 text-gray-400 hover:text-kubik-black"
                              >
                                <MoreHorizontal size={16} />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem
                                onClick={() =>
                                  navigate(`/session/${session.session_uuid}`)
                                }
                              >
                                <ExternalLink className="mr-2 h-4 w-4" /> View
                                Details
                              </DropdownMenuItem>
                              <DropdownMenuItem
                                onClick={() => handleDelete([session.id])}
                                className="text-red-600 focus:text-red-600 focus:bg-red-50"
                              >
                                <Trash2 className="mr-2 h-4 w-4" /> Delete
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            // --- GRID VIEW ---
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
              {filteredSessions.map((session) => {
                const isSelected = selectedSessions.includes(session.id);
                const status = getSessionStatus(session);

                return (
                  <motion.div
                    layout
                    key={session.id}
                    className={cn(
                      "group relative bg-white rounded-2xl border transition-all duration-200 overflow-hidden cursor-pointer",
                      isSelected
                        ? "ring-2 ring-kubik-blue border-transparent shadow-md"
                        : "border-gray-100 shadow-sm hover:shadow-lg hover:-translate-y-1"
                    )}
                    onClick={() => navigate(`/session/${session.session_uuid}`)}
                  >
                    {/* Selection Overlay Checkbox */}
                    <div
                      className="absolute top-3 left-3 z-20"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <input
                        type="checkbox"
                        className="w-5 h-5 rounded border-white/50 bg-black/20 text-kubik-blue focus:ring-kubik-blue checked:bg-kubik-blue checked:border-transparent backdrop-blur-sm cursor-pointer transition-all"
                        checked={isSelected}
                        onChange={() => handleSelect(session.id)}
                      />
                    </div>

                    {/* Status Badge */}
                    <div className="absolute top-3 right-3 z-20">
                      <span
                        className={cn(
                          "w-2.5 h-2.5 rounded-full block border border-white/20 shadow-sm",
                          status.dot
                        )}
                        title={status.label}
                      />
                    </div>

                    {/* Image Area - FIXED: canonical aspect-4/3 and bg-linear-to-t */}
                    <div className="aspect-4/3 bg-gray-100 relative overflow-hidden">
                      {session.final_photo_url ? (
                        <img
                          src={session.final_photo_url}
                          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                          loading="lazy"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-gray-300">
                          <ImageIcon size={32} />
                        </div>
                      )}
                      <div className="absolute inset-0 bg-linear-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-end justify-center pb-4">
                        <p className="text-white text-xs font-medium flex items-center gap-1">
                          <ExternalLink size={12} /> View Session
                        </p>
                      </div>
                    </div>

                    {/* Footer Info */}
                    <div className="p-4">
                      <p className="font-mono text-xs font-bold text-kubik-black truncate">
                        {session.session_uuid}
                      </p>
                      <p className="text-[10px] text-gray-400 mt-1 flex justify-between">
                        <span>
                          {new Date(session.created_at).toLocaleDateString()}
                        </span>
                        <span>
                          {session.device_id
                            ? session.device_id.substring(0, 4)
                            : "UNK"}
                        </span>
                      </p>
                    </div>
                  </motion.div>
                );
              })}
            </div>
          )}
        </motion.div>
      )}

      {/* 4. CONTEXTUAL FLOATING ACTION BAR */}
      <AnimatePresence>
        {selectedSessions.length > 0 && (
          <motion.div
            initial={{ y: 100, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: 100, opacity: 0 }}
            className="fixed bottom-8 left-1/2 -translate-x-1/2 z-50"
          >
            <div className="bg-kubik-black text-white px-6 py-3 rounded-full shadow-2xl shadow-kubik-black/30 flex items-center gap-6 ring-1 ring-white/10 backdrop-blur-md">
              <div className="flex items-center gap-3">
                <span className="bg-white/20 text-xs font-bold px-2 py-0.5 rounded-md">
                  {selectedSessions.length}
                </span>
                <span className="text-sm font-medium">Selected</span>
              </div>

              <div className="h-4 w-px bg-white/20" />

              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="ghost"
                  className="h-8 hover:bg-white/10 hover:text-white text-gray-300"
                  onClick={() => setSelectedSessions([])}
                >
                  Cancel
                </Button>
                <Button
                  size="sm"
                  className="h-8 bg-red-600 hover:bg-red-500 text-white border-0 shadow-none"
                  onClick={() => handleDelete(selectedSessions)}
                >
                  <Trash2 size={14} className="mr-2" />
                  Delete
                </Button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// --- SUB COMPONENTS FOR CLEAN CODE ---

function MetricCard({
  label,
  value,
  icon,
  color = "text-kubik-blue",
}: MetricCardProps) {
  return (
    <div className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm flex flex-col justify-between h-24">
      <div className="flex justify-between items-start">
        <span className="text-[10px] uppercase font-bold text-gray-400 tracking-wider">
          {label}
        </span>
        <span className={cn("opacity-80", color)}>{icon}</span>
      </div>
      <p className="text-2xl font-extrabold text-kubik-black">{value}</p>
    </div>
  );
}
