import { useEffect, useState, useCallback } from "react";
import { supabase } from "../lib/supabase";
import { useUserRole } from "../hooks/useUserRole";
import type { Device, Ticket } from "../types/supabase";
import {
  Ticket as TicketIcon,
  Printer,
  RefreshCcw,
  AlertCircle,
  QrCode,
  MapPin,
  CreditCard,
  Copy,
  Store,
  Loader2,
  History,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Receipt,
  Filter,
} from "lucide-react";
import { Button } from "../components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { Badge } from "../components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../components/ui/table";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "../lib/utils";

// --- TYPES ---
type TicketStatus = "ALL" | "AVAILABLE" | "USED";

// --- SUB-COMPONENTS ---
const TicketCard = ({
  code,
  deviceName,
}: {
  code: string;
  deviceName?: string;
}) => (
  <div className="relative bg-white text-zinc-900 rounded-none w-full max-w-[340px] shadow-2xl mx-auto overflow-hidden">
    {/* Serrated Top */}
    <div
      className="absolute top-0 left-0 right-0 h-4 bg-zinc-900"
      style={{
        maskImage:
          "radial-gradient(circle at 10px 0, transparent 0, transparent 5px, black 6px)",
        maskSize: "20px 10px",
        maskRepeat: "repeat-x",
      }}
    />

    {/* Header */}
    <div className="pt-10 pb-6 px-6 text-center border-b-2 border-dashed border-zinc-200">
      <div className="flex justify-center mb-4">
        <div className="w-12 h-12 bg-zinc-900 rounded-lg flex items-center justify-center text-white">
          <TicketIcon size={24} />
        </div>
      </div>
      <h2 className="text-xl font-black uppercase tracking-tighter">
        KUBIK PHOTO
      </h2>
      <p className="text-[10px] uppercase tracking-[0.2em] text-zinc-400 mt-1">
        Admission Ticket
      </p>
    </div>

    {/* Body */}
    <div className="p-8 text-center bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] bg-fixed">
      <p className="text-[9px] font-bold text-zinc-400 uppercase tracking-widest mb-2">
        Session Code
      </p>
      <div className="text-6xl font-black font-mono tracking-tighter text-zinc-900 mb-6">
        {code}
      </div>

      <div className="flex justify-center mb-6">
        <div className="p-2 border-2 border-zinc-900 rounded-lg">
          <QrCode size={80} />
        </div>
      </div>

      <div className="space-y-1">
        <div className="flex justify-between text-xs font-bold border-b border-zinc-100 pb-2 mb-2">
          <span className="text-zinc-400">LOCATION</span>
          <span className="text-right max-w-[150px] truncate">
            {deviceName}
          </span>
        </div>
        <div className="flex justify-between text-xs font-bold">
          <span className="text-zinc-400">DATE</span>
          <span>{new Date().toLocaleDateString()}</span>
        </div>
        <div className="flex justify-between text-xs font-bold">
          <span className="text-zinc-400">TIME</span>
          <span>
            {new Date().toLocaleTimeString([], {
              hour: "2-digit",
              minute: "2-digit",
            })}
          </span>
        </div>
      </div>
    </div>

    {/* Serrated Bottom */}
    <div
      className="absolute bottom-0 left-0 right-0 h-4 bg-zinc-900"
      style={{
        maskImage:
          "radial-gradient(circle at 10px 10px, transparent 0, transparent 5px, black 6px)",
        maskSize: "20px 10px",
        maskRepeat: "repeat-x",
        transform: "rotate(180deg)",
      }}
    />
  </div>
);

export default function TicketStation() {
  const { role, assignedDevice, loading: roleLoading } = useUserRole();

  // --- STATE ---
  const [vendingDevices, setVendingDevices] = useState<Device[]>([]);
  const [selectedDeviceId, setSelectedDeviceId] = useState<string>("");
  const [generatedCode, setGeneratedCode] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [isCopied, setIsCopied] = useState(false);

  // History State
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [ticketHistoryLoading, setTicketHistoryLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<TicketStatus>("ALL");
  const [showHistory, setShowHistory] = useState(false);

  // --- DATA FETCHING ---
  useEffect(() => {
    if (roleLoading) return;

    if (role === "ADMIN") {
      const fetchAllDevices = async () => {
        const { data } = await supabase
          .from("devices")
          .select("*")
          .eq("type", "VENDING")
          .eq("status", "ACTIVE")
          .order("name", { ascending: true });

        if (data) {
          setVendingDevices(data as Device[]);
          if (data.length > 0) setSelectedDeviceId(data[0].id);
        }
      };
      fetchAllDevices();
    } else if (role === "CASHIER" && assignedDevice) {
      setVendingDevices([assignedDevice]);
      setSelectedDeviceId(assignedDevice.id);
    }
  }, [role, assignedDevice, roleLoading]);

  // --- HISTORY LOGIC (Wrapped in useCallback) ---
  const fetchTicketHistory = useCallback(async () => {
    if (!selectedDeviceId) return;
    setTicketHistoryLoading(true);
    try {
      let query = supabase
        .from("tickets")
        .select("*")
        .eq("device_id", selectedDeviceId)
        .order("created_at", { ascending: false })
        .limit(20);

      if (statusFilter !== "ALL") {
        query = query.eq("status", statusFilter);
      }

      const { data, error } = await query;
      if (error) throw error;
      if (data) setTickets(data as Ticket[]);
    } catch (err) {
      console.error("Fetch Error:", err);
    } finally {
      setTicketHistoryLoading(false);
    }
  }, [selectedDeviceId, statusFilter]);

  // Auto-fetch history when dependencies change AND history is open
  useEffect(() => {
    if (selectedDeviceId && showHistory) fetchTicketHistory();
  }, [selectedDeviceId, statusFilter, showHistory, fetchTicketHistory]);

  // --- ACTIONS ---
  const handleGenerateTicket = async () => {
    if (!selectedDeviceId) return;
    setLoading(true);
    setGeneratedCode(null);

    try {
      const newCode = Math.floor(100000 + Math.random() * 900000).toString();
      const { error } = await supabase.from("tickets").insert({
        code: newCode,
        device_id: selectedDeviceId,
        status: "AVAILABLE",
      });
      if (error) throw error;

      setTimeout(() => {
        setGeneratedCode(newCode);
        setLoading(false);
        if (showHistory) fetchTicketHistory();
      }, 800);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Unknown Error";
      alert("Failed: " + message);
      setLoading(false);
    }
  };

  const copyToClipboard = () => {
    if (generatedCode) {
      navigator.clipboard.writeText(generatedCode);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  // --- LOADING / ACCESS STATES ---
  if (roleLoading) {
    return (
      <div className="h-screen flex flex-col items-center justify-center bg-zinc-50">
        <Loader2 className="w-8 h-8 text-indigo-600 animate-spin mb-4" />
        <p className="text-zinc-400 text-xs font-bold uppercase tracking-widest">
          System Booting...
        </p>
      </div>
    );
  }

  if (role === "CASHIER" && !assignedDevice) {
    return (
      <div className="h-screen flex items-center justify-center p-8 bg-zinc-50">
        <div className="bg-white p-8 rounded-2xl shadow-xl text-center max-w-md border border-red-100">
          <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4">
            <AlertCircle className="w-8 h-8 text-red-500" />
          </div>
          <h3 className="font-bold text-xl text-zinc-900 mb-2">
            Access Denied
          </h3>
          <p className="text-zinc-500">
            No vending machine assigned to your ID.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F8F9FA] p-4 md:p-8 font-sans selection:bg-indigo-500 selection:text-white">
      <div className="max-w-7xl mx-auto space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-700">
        {/* HEADER */}
        <header className="flex flex-col md:flex-row md:items-end justify-between gap-4 pb-4 border-b border-zinc-200">
          <div>
            <h1 className="text-4xl font-black text-zinc-900 tracking-tighter flex items-center gap-3">
              TICKET STATION
              <span className="text-xs bg-black text-white px-2 py-0.5 rounded font-bold tracking-widest -translate-y-2">
                PRO
              </span>
            </h1>
            <p className="text-zinc-500 mt-1 font-medium text-sm flex items-center gap-2">
              <Receipt size={14} /> Digital Kiosk Management
            </p>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-full border border-zinc-200 shadow-sm">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
              </span>
              <span className="text-[10px] font-bold text-zinc-600 uppercase tracking-wider">
                Operational
              </span>
            </div>
          </div>
        </header>

        {/* MAIN LAYOUT */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* LEFT: CONTROLS (5 Columns) */}
          <div className="lg:col-span-5 space-y-6">
            {/* DEVICE CONFIG CARD */}
            <div className="bg-white p-6 md:p-8 rounded-3xl shadow-[0_2px_20px_rgba(0,0,0,0.04)] border border-zinc-100">
              <h3 className="text-sm font-bold text-zinc-400 uppercase tracking-widest mb-6 flex items-center gap-2">
                <MapPin size={16} /> Configuration
              </h3>

              <div className="space-y-6">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-zinc-900 ml-1">
                    TARGET DEVICE
                  </label>
                  {role === "ADMIN" ? (
                    <Select
                      value={selectedDeviceId}
                      onValueChange={(v) => {
                        setSelectedDeviceId(v);
                        setGeneratedCode(null);
                      }}
                    >
                      <SelectTrigger className="h-14 rounded-xl bg-zinc-50 border-zinc-200 focus:ring-indigo-500 text-base font-medium">
                        <SelectValue placeholder="Select Device" />
                      </SelectTrigger>
                      <SelectContent>
                        {vendingDevices.map((d) => (
                          <SelectItem
                            key={d.id}
                            value={d.id}
                            className="font-medium"
                          >
                            {d.name}{" "}
                            <span className="opacity-50 text-xs ml-2">
                              ({d.username})
                            </span>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  ) : (
                    <div className="h-14 flex items-center gap-4 px-4 bg-indigo-50 border border-indigo-100 rounded-xl text-indigo-900">
                      <Store size={20} className="text-indigo-600" />
                      <div>
                        <p className="font-bold text-sm leading-tight">
                          {assignedDevice?.name}
                        </p>
                        <p className="text-[10px] opacity-70 font-mono leading-tight">
                          {assignedDevice?.username}
                        </p>
                      </div>
                      <Badge className="ml-auto bg-indigo-200 text-indigo-800 hover:bg-indigo-200">
                        LOCKED
                      </Badge>
                    </div>
                  )}
                </div>

                <div className="p-4 bg-amber-50 rounded-xl border border-amber-100 flex gap-3">
                  <CreditCard
                    className="text-amber-500 shrink-0 mt-0.5"
                    size={18}
                  />
                  <div>
                    <p className="text-xs font-bold text-amber-800 mb-1">
                      PAYMENT CHECK
                    </p>
                    <p className="text-[11px] text-amber-700/80 leading-relaxed">
                      Verify payment before generation. Ticket valid for{" "}
                      <b>one session</b> only.
                    </p>
                  </div>
                </div>

                <Button
                  onClick={handleGenerateTicket}
                  disabled={loading || !selectedDeviceId}
                  className="w-full h-16 text-lg font-bold rounded-2xl shadow-xl shadow-indigo-500/20 bg-indigo-600 hover:bg-indigo-700 text-white transition-all active:scale-[0.98] mt-4"
                >
                  {loading ? (
                    <div className="flex items-center gap-3">
                      <RefreshCcw className="h-6 w-6 animate-spin text-white/50" />
                      <span>PROCESSING...</span>
                    </div>
                  ) : (
                    <div className="flex items-center gap-3">
                      <Printer className="h-6 w-6" />
                      <span>PRINT TICKET</span>
                    </div>
                  )}
                </Button>
              </div>
            </div>

            {/* HISTORY CARD */}
            <div className="bg-white rounded-3xl shadow-sm border border-zinc-100 overflow-hidden">
              <button
                onClick={() => setShowHistory(!showHistory)}
                className="w-full p-6 flex items-center justify-between hover:bg-zinc-50 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-500">
                    <History size={20} />
                  </div>
                  <div className="text-left">
                    <p className="text-sm font-bold text-zinc-900">
                      Recent Transactions
                    </p>
                    <p className="text-xs text-zinc-400">
                      View issued tickets log
                    </p>
                  </div>
                </div>
                {showHistory ? (
                  <ChevronUp size={20} className="text-zinc-400" />
                ) : (
                  <ChevronDown size={20} className="text-zinc-400" />
                )}
              </button>

              <AnimatePresence>
                {showHistory && (
                  <motion.div
                    initial={{ height: 0 }}
                    animate={{ height: "auto" }}
                    exit={{ height: 0 }}
                    className="overflow-hidden"
                  >
                    <div className="px-6 pb-6 pt-0 border-t border-zinc-100">
                      <div className="flex items-center justify-between py-4">
                        <div className="flex items-center gap-2">
                          <Filter size={12} className="text-zinc-400 mr-1" />
                          {(["ALL", "AVAILABLE", "USED"] as const).map(
                            (status) => (
                              <button
                                key={status}
                                onClick={() => setStatusFilter(status)}
                                className={cn(
                                  "px-3 py-1 rounded-full text-[10px] font-bold transition-all border",
                                  statusFilter === status
                                    ? "bg-zinc-900 text-white border-zinc-900"
                                    : "bg-white text-zinc-500 border-zinc-200 hover:border-zinc-300"
                                )}
                              >
                                {status}
                              </button>
                            )
                          )}
                        </div>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={fetchTicketHistory}
                          className="h-8 w-8 rounded-full"
                        >
                          <RefreshCcw
                            size={14}
                            className={
                              ticketHistoryLoading ? "animate-spin" : ""
                            }
                          />
                        </Button>
                      </div>

                      <div className="max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
                        <Table>
                          <TableHeader>
                            <TableRow className="hover:bg-transparent border-none">
                              <TableHead className="h-8 text-[10px] font-bold text-zinc-400 uppercase">
                                Code
                              </TableHead>
                              <TableHead className="h-8 text-[10px] font-bold text-zinc-400 uppercase">
                                Status
                              </TableHead>
                              <TableHead className="h-8 text-[10px] font-bold text-zinc-400 uppercase text-right">
                                Time
                              </TableHead>
                            </TableRow>
                          </TableHeader>
                          <TableBody>
                            {tickets.length === 0 ? (
                              <TableRow>
                                <TableCell
                                  colSpan={3}
                                  className="py-4 text-center text-xs text-zinc-400"
                                >
                                  No data available
                                </TableCell>
                              </TableRow>
                            ) : (
                              tickets.map((t) => (
                                <TableRow
                                  key={t.id}
                                  className="group border-none hover:bg-zinc-50"
                                >
                                  <TableCell className="py-3 font-mono font-bold text-xs text-zinc-900">
                                    {t.code}
                                  </TableCell>
                                  <TableCell className="py-3">
                                    <Badge
                                      variant="secondary"
                                      className={cn(
                                        "text-[10px] h-5 px-1.5",
                                        t.status === "AVAILABLE"
                                          ? "bg-emerald-50 text-emerald-600"
                                          : "bg-zinc-100 text-zinc-500"
                                      )}
                                    >
                                      {t.status === "AVAILABLE"
                                        ? "ACTIVE"
                                        : "USED"}
                                    </Badge>
                                  </TableCell>
                                  <TableCell className="py-3 text-right">
                                    <p className="text-[10px] text-zinc-400 font-medium">
                                      {formatDate(t.created_at)}
                                    </p>
                                  </TableCell>
                                </TableRow>
                              ))
                            )}
                          </TableBody>
                        </Table>
                      </div>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>

          {/* RIGHT: OUTPUT AREA (7 Columns) */}
          <div className="lg:col-span-7 h-full min-h-[500px] bg-zinc-900 rounded-[2.5rem] p-8 md:p-12 relative overflow-hidden flex flex-col items-center justify-center shadow-2xl">
            <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/stardust.png')] opacity-20" />
            <div className="absolute top-0 right-0 w-[400px] h-[400px] bg-indigo-500/20 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-0 left-0 w-[300px] h-[300px] bg-emerald-500/10 rounded-full blur-[100px] pointer-events-none" />

            <AnimatePresence mode="wait">
              {generatedCode ? (
                <motion.div
                  key="ticket"
                  initial={{ y: -100, opacity: 0, scale: 0.9 }}
                  animate={{ y: 0, opacity: 1, scale: 1 }}
                  exit={{ y: 100, opacity: 0, scale: 0.8 }}
                  transition={{ type: "spring", stiffness: 120, damping: 20 }}
                  className="relative z-10 w-full flex flex-col items-center"
                >
                  <TicketCard
                    code={generatedCode}
                    deviceName={
                      vendingDevices.find((d) => d.id === selectedDeviceId)
                        ?.name || "Unknown Device"
                    }
                  />

                  <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.3 }}
                    className="mt-8 flex gap-4 w-full max-w-[340px]"
                  >
                    <Button
                      variant="secondary"
                      onClick={copyToClipboard}
                      className="flex-1 bg-white/10 text-white hover:bg-white/20 border-0 backdrop-blur-md h-12 rounded-xl font-bold text-xs tracking-wide"
                    >
                      {isCopied ? (
                        <CheckCircle2
                          size={16}
                          className="mr-2 text-emerald-400"
                        />
                      ) : (
                        <Copy size={16} className="mr-2 opacity-70" />
                      )}
                      {isCopied ? "COPIED" : "COPY CODE"}
                    </Button>
                    <Button
                      onClick={handleGenerateTicket}
                      className="flex-1 bg-indigo-500 hover:bg-indigo-600 text-white border-0 h-12 rounded-xl font-bold text-xs tracking-wide"
                    >
                      PRINT NEW
                    </Button>
                  </motion.div>
                </motion.div>
              ) : (
                <motion.div
                  key="empty"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="text-center relative z-10"
                >
                  <div className="w-24 h-24 rounded-3xl bg-white/5 border border-white/10 flex items-center justify-center mx-auto mb-6 backdrop-blur-sm shadow-xl">
                    <Printer
                      className="text-white/30"
                      size={40}
                      strokeWidth={1.5}
                    />
                  </div>
                  <h2 className="text-2xl font-bold text-white mb-2 tracking-tight">
                    Ready to Print
                  </h2>
                  <p className="text-zinc-400 max-w-xs mx-auto text-sm leading-relaxed">
                    Configure the booth settings on the left panel and click
                    'Print Ticket' to generate a unique session code.
                  </p>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      </div>
    </div>
  );
}
