import { useEffect, useState, useCallback } from "react";
import { supabase } from "../lib/supabase";
import {
  Plus,
  Smartphone,
  Trash2,
  Power,
  PowerOff,
  Loader2,
  CalendarClock,
  Monitor,
  AlertTriangle,
  User,
  Mail,
  Key,
} from "lucide-react";
import type { Device } from "../types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function DeviceManager() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);

  // Modal States
  const [showModal, setShowModal] = useState(false);
  const [showCreateUserModal, setShowCreateUserModal] = useState(false);
  const [newName, setNewName] = useState("");
  const [newType, setNewType] = useState<"RENTAL" | "VENDING">("RENTAL");
  const [expiryDate, setExpiryDate] = useState("");
  const [isCreating, setIsCreating] = useState(false);

  // User Creation States
  const [cashierEmail, setCashierEmail] = useState("");
  const [cashierPassword, setCashierPassword] = useState("");
  const [isCreatingUser, setIsCreatingUser] = useState(false);
  const [createdCredentials, setCreatedCredentials] = useState<{
    username: string;
    pin: string;
    email: string;
  } | null>(null);

  // 1. Fetch Devices
  const fetchDevices = useCallback(async () => {
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from("devices")
        .select("*")
        .order("created_at", { ascending: false });

      if (!error && data) setDevices(data as Device[]);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDevices();
  }, [fetchDevices]);

  // 2. Create Device (Original Method)
  async function handleCreateDevice(e: React.FormEvent) {
    e.preventDefault();
    setIsCreating(true);

    const randomSuffix = Math.floor(1000 + Math.random() * 9000);
    const generatedUsername = `booth_${randomSuffix}`;
    const generatedPin = Math.floor(100000 + Math.random() * 900000).toString();

    let finalExpiry: string | null = null;
    if (newType === "RENTAL" && expiryDate) {
      finalExpiry = new Date(expiryDate).toISOString();
    }

    const { error } = await supabase.from("devices").insert({
      name: newName,
      type: newType,
      username: generatedUsername,
      pin_code: generatedPin,
      status: "ACTIVE",
      expiry_date: finalExpiry,
    });

    if (!error) {
      // Menggunakan UI feedback sederhana (bisa diganti Toast nanti)
      alert(
        `✅ Device Berhasil Dibuat!\n\nUsername: ${generatedUsername}\nPIN: ${generatedPin}`
      );
      setShowModal(false);
      setNewName("");
      setExpiryDate("");
      fetchDevices();
    } else {
      alert("❌ Gagal membuat device: " + error.message);
    }
    setIsCreating(false);
  }

  // 3. Create Device + Cashier User (Zero Friction Method)
  async function handleCreateDeviceWithUser(e: React.FormEvent) {
    e.preventDefault();
    setIsCreatingUser(true);

    try {
      // Get current session token
      const {
        data: { session },
      } = await supabase.auth.getSession();
      if (!session) {
        throw new Error("You must be logged in to perform this action");
      }

      // Call Edge Function
      const { data, error } = await supabase.functions.invoke(
        "create-cashier-device",
        {
          body: {
            deviceName: newName,
            deviceType: newType,
            email: cashierEmail,
            password: cashierPassword,
          },
          headers: {
            Authorization: `Bearer ${session.access_token}`,
          },
        }
      );

      if (error) {
        throw error;
      }

      if (data.success) {
        setCreatedCredentials({
          username: data.credentials.username,
          pin: data.credentials.pin,
          email: cashierEmail,
        });

        // Reset form
        setNewName("");
        setCashierEmail("");
        setCashierPassword("");
        setExpiryDate("");

        // Refresh devices list
        fetchDevices();
      } else {
        throw new Error(data.error || "Failed to create device and user");
      }
    } catch (error: unknown) {
      const errorMessage =
        error instanceof Error ? error.message : "An unexpected error occurred";
      alert("❌ Error: " + errorMessage);
    } finally {
      setIsCreatingUser(false);
    }
  }

  // 3. Toggle Status (Optimistic UI)
  async function toggleStatus(id: string, currentStatus: string) {
    const newStatus: "ACTIVE" | "SUSPENDED" =
      currentStatus === "ACTIVE" ? "SUSPENDED" : "ACTIVE";

    const { error } = await supabase
      .from("devices")
      .update({ status: newStatus })
      .eq("id", id);

    if (!error) {
      setDevices((prev) =>
        prev.map((d) => (d.id === id ? { ...d, status: newStatus } : d))
      );
    } else {
      alert("Gagal mengubah status: " + error.message);
    }
  }

  // 4. Delete Device (Updated Logic)
  async function deleteDevice(id: string) {
    const isConfirmed = window.confirm(
      "⚠️ PERINGATAN KERAS\n\nMenghapus device ini akan menghapus seluruh HISTORY SESI FOTO dan TIKET yang terkait secara permanen.\n\nTindakan ini tidak dapat dibatalkan. Apakah Anda yakin?"
    );

    if (!isConfirmed) return;

    const { error } = await supabase.from("devices").delete().eq("id", id);

    if (!error) {
      setDevices((prev) => prev.filter((d) => d.id !== id));
    } else {
      alert(
        "Gagal menghapus device. Pastikan tidak ada data yang terkunci atau hubungi developer. Error: " +
          error.message
      );
    }
  }

  // Helper Format Date
  const formatDate = (isoString?: string | null) => {
    if (!isoString) return "Unlimited Access";
    return new Date(isoString).toLocaleString("id-ID", {
      day: "numeric",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div className="p-6 md:p-10 max-w-7xl mx-auto animate-in fade-in duration-500 font-sans text-slate-800">
      {/* HEADER SECTION */}
      <header className="flex flex-col md:flex-row md:justify-between md:items-center mb-10 gap-4">
        <div>
          <h2 className="text-3xl font-black tracking-tight text-slate-900">
            Device Manager
          </h2>
          <p className="text-slate-500 mt-2 text-sm md:text-base">
            Kelola akses tablet photobooth, pantau status, dan atur perizinan.
          </p>
        </div>
        <div className="flex gap-3">
          <Button
            onClick={() => setShowCreateUserModal(true)}
            className="bg-slate-900 hover:bg-slate-800 text-white shadow-lg shadow-slate-200 transition-all hover:scale-105 active:scale-95 h-12 px-6 rounded-xl"
          >
            <Plus size={18} className="mr-2" />
            Create Device + Cashier
          </Button>
          <Button
            onClick={() => setShowModal(true)}
            variant="outline"
            className="hover:bg-slate-50 transition-all hover:scale-105 active:scale-95 h-12 px-6 rounded-xl"
          >
            <Plus size={18} className="mr-2" />
            Device Only
          </Button>
        </div>
      </header>

      {/* CONTENT SECTION */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 opacity-50">
          <Loader2 className="animate-spin w-10 h-10 text-slate-400 mb-4" />
          <p className="text-sm font-medium text-slate-400">
            Syncing devices...
          </p>
        </div>
      ) : devices.length === 0 ? (
        // EMPTY STATE
        <div className="flex flex-col items-center justify-center py-20 border-2 border-dashed border-slate-200 rounded-3xl bg-slate-50/50">
          <div className="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mb-4">
            <Monitor className="text-slate-400" size={32} />
          </div>
          <h3 className="text-lg font-bold text-slate-700">No Devices Found</h3>
          <p className="text-slate-500 text-sm max-w-md text-center mt-2 mb-6">
            Anda belum memiliki device yang terdaftar. Tambahkan device baru
            untuk memulai operasional photobooth.
          </p>
          <Button onClick={() => setShowModal(true)} variant="outline">
            Create First Device
          </Button>
        </div>
      ) : (
        // GRID LAYOUT
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {devices.map((device) => (
            <div
              key={device.id}
              className="group bg-white rounded-2xl border border-slate-200 p-6 relative hover:shadow-xl hover:shadow-slate-200/50 hover:border-slate-300 transition-all duration-300"
            >
              {/* HEADER CARD */}
              <div className="flex items-start justify-between mb-6">
                <div className="flex items-center gap-4">
                  <div
                    className={`w-14 h-14 rounded-2xl flex items-center justify-center shadow-sm ${
                      device.type === "RENTAL"
                        ? "bg-blue-50 text-blue-600"
                        : "bg-purple-50 text-purple-600"
                    }`}
                  >
                    <Smartphone size={28} strokeWidth={1.5} />
                  </div>
                  <div>
                    <h3 className="font-bold text-lg text-slate-900 leading-tight">
                      {device.name}
                    </h3>
                    <div className="flex items-center gap-2 mt-1">
                      <span
                        className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-md ${
                          device.type === "RENTAL"
                            ? "bg-blue-100 text-blue-700"
                            : "bg-purple-100 text-purple-700"
                        }`}
                      >
                        {device.type}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Status Indicator */}
                <div
                  className={`w-3 h-3 rounded-full shadow-sm ring-4 ring-white ${
                    device.status === "ACTIVE" ? "bg-emerald-500" : "bg-red-500"
                  }`}
                  title={device.status}
                />
              </div>

              {/* CREDENTIALS BOX */}
              <div className="bg-slate-50 rounded-xl p-4 space-y-3 mb-6 border border-slate-100 group-hover:bg-slate-100/80 transition-colors">
                <div className="flex justify-between items-center text-sm">
                  <span className="text-slate-500 font-medium text-xs uppercase tracking-wide">
                    Username
                  </span>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-slate-800 select-all cursor-text hover:text-blue-600 transition-colors">
                      {device.username}
                    </span>
                  </div>
                </div>
                <div className="h-px bg-slate-200 w-full" />
                <div className="flex justify-between items-center text-sm">
                  <span className="text-slate-500 font-medium text-xs uppercase tracking-wide">
                    PIN Code
                  </span>
                  <span className="font-mono font-bold text-slate-800 tracking-widest select-all cursor-text hover:text-blue-600 transition-colors">
                    {device.pin_code}
                  </span>
                </div>
              </div>

              {/* FOOTER INFO */}
              <div className="space-y-4">
                {device.type === "RENTAL" ? (
                  <div className="flex items-center gap-2 text-xs text-slate-500 bg-white border border-slate-100 px-3 py-2 rounded-lg shadow-sm">
                    <CalendarClock size={14} className="text-blue-500" />
                    <span className="truncate">
                      Expires:{" "}
                      <strong className="text-slate-700">
                        {formatDate(device.expiry_date)}
                      </strong>
                    </span>
                  </div>
                ) : (
                  <div className="flex items-center gap-2 text-xs text-slate-500 bg-white border border-slate-100 px-3 py-2 rounded-lg shadow-sm">
                    <AlertTriangle size={14} className="text-purple-500" />
                    <span>Mode Vending (Per Ticket)</span>
                  </div>
                )}

                {/* ACTION BUTTONS */}
                <div className="flex gap-3 pt-2">
                  <Button
                    variant="outline"
                    onClick={() => toggleStatus(device.id, device.status)}
                    className={`flex-1 text-xs font-bold h-9 border-0 ring-1 ring-inset ${
                      device.status === "ACTIVE"
                        ? "ring-slate-200 text-slate-600 hover:bg-red-50 hover:text-red-600 hover:ring-red-200"
                        : "ring-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
                    }`}
                  >
                    {device.status === "ACTIVE" ? (
                      <>
                        <PowerOff size={14} className="mr-2" /> Suspend
                      </>
                    ) : (
                      <>
                        <Power size={14} className="mr-2" /> Activate
                      </>
                    )}
                  </Button>

                  <Button
                    variant="ghost"
                    onClick={() => deleteDevice(device.id)}
                    className="w-10 h-9 px-0 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                  >
                    <Trash2 size={16} />
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* MODAL CREATE (Backdrop Blur & Smooth) */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl p-8 w-full max-w-md shadow-2xl shadow-slate-900/20 animate-in zoom-in-95 duration-200 border border-white/20 ring-1 ring-slate-900/5">
            <div className="text-center mb-8">
              <div className="w-12 h-12 bg-slate-100 rounded-2xl mx-auto flex items-center justify-center mb-4">
                <Plus size={24} className="text-slate-900" />
              </div>
              <h3 className="text-xl font-black text-slate-900">
                Setup New Device
              </h3>
              <p className="text-slate-500 text-sm mt-1">
                Generate credentials for a new booth.
              </p>
            </div>

            <form onSubmit={handleCreateDevice} className="space-y-5">
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-700 uppercase tracking-wide">
                  Device Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Lobby Hotel Mulia"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none transition-all placeholder:text-slate-400 font-medium"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-700 uppercase tracking-wide">
                  Business Model
                </label>
                <div className="relative">
                  <select
                    value={newType}
                    onChange={(e) =>
                      setNewType(e.target.value as "RENTAL" | "VENDING")
                    }
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-slate-900 outline-none appearance-none font-medium text-slate-700"
                  >
                    <option value="RENTAL">
                      Rental (Unlimited / Flat Fee)
                    </option>
                    <option value="VENDING">
                      Vending (Pay Per Use / Ticket)
                    </option>
                  </select>
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
                    <Monitor size={16} />
                  </div>
                </div>
              </div>

              {/* Conditional Expiry Input with Animation */}
              {newType === "RENTAL" && (
                <div className="bg-blue-50/50 p-4 rounded-xl border border-blue-100/50 animate-in slide-in-from-top-2">
                  <label className="flex items-center gap-2 text-xs font-bold text-blue-800 uppercase mb-2">
                    <CalendarClock size={14} /> Rental Expiry (Auto Lock)
                  </label>
                  <input
                    type="datetime-local"
                    value={expiryDate}
                    onChange={(e) => setExpiryDate(e.target.value)}
                    className="w-full p-2.5 border border-blue-200 rounded-lg bg-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 text-slate-700"
                  />
                  <p className="text-[10px] text-blue-600/80 mt-2 font-medium">
                    *Leave empty for Unlimited Time (Forever)
                  </p>
                </div>
              )}

              <div className="pt-4 flex gap-3">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => setShowModal(false)}
                  className="flex-1 h-12 rounded-xl text-slate-500 hover:text-slate-900 hover:bg-slate-100"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={isCreating}
                  className="flex-1 h-12 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold"
                >
                  {isCreating ? (
                    <Loader2 className="animate-spin" />
                  ) : (
                    "Create Credentials"
                  )}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL CREATE DEVICE + USER (Zero Friction) */}
      {showCreateUserModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl p-8 w-full max-w-md shadow-2xl shadow-slate-900/20 animate-in zoom-in-95 duration-200 border border-white/20 ring-1 ring-slate-900/5">
            <div className="text-center mb-8">
              <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-purple-500 rounded-2xl mx-auto flex items-center justify-center mb-4">
                <User size={24} className="text-white" />
              </div>
              <h3 className="text-xl font-black text-slate-900">
                Create Device & Cashier
              </h3>
              <p className="text-slate-500 text-sm mt-1">
                Zero-friction setup: Device + User in one click
              </p>
            </div>

            {createdCredentials ? (
              // Success State with Credentials
              <div className="space-y-4">
                <div className="bg-green-50 border border-green-200 rounded-xl p-4">
                  <h4 className="font-bold text-green-800 mb-3">
                    ✅ Successfully Created!
                  </h4>

                  <div className="space-y-3">
                    <div>
                      <Label className="text-xs font-bold text-green-700 uppercase">
                        Device Credentials
                      </Label>
                      <div className="mt-1 p-2 bg-white rounded border border-green-100">
                        <p className="text-sm font-mono">
                          <strong>Username:</strong>{" "}
                          {createdCredentials.username}
                        </p>
                        <p className="text-sm font-mono">
                          <strong>PIN:</strong> {createdCredentials.pin}
                        </p>
                      </div>
                    </div>

                    <div>
                      <Label className="text-xs font-bold text-green-700 uppercase">
                        Cashier Login
                      </Label>
                      <div className="mt-1 p-2 bg-white rounded border border-green-100">
                        <p className="text-sm font-mono">
                          <strong>Email:</strong> {createdCredentials.email}
                        </p>
                        <p className="text-sm font-mono">
                          <strong>Password:</strong> [Set during creation]
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                <Button
                  onClick={() => {
                    setShowCreateUserModal(false);
                    setCreatedCredentials(null);
                  }}
                  className="w-full h-12 rounded-xl bg-green-600 hover:bg-green-700 text-white font-bold"
                >
                  Done
                </Button>
              </div>
            ) : (
              // Form State
              <form onSubmit={handleCreateDeviceWithUser} className="space-y-5">
                <div className="space-y-1.5">
                  <Label className="text-xs font-bold text-slate-700 uppercase tracking-wide">
                    Device Name
                  </Label>
                  <Input
                    type="text"
                    required
                    placeholder="e.g. Lobby Hotel Mulia"
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none transition-all placeholder:text-slate-400 font-medium"
                  />
                </div>

                <div className="space-y-1.5">
                  <Label className="text-xs font-bold text-slate-700 uppercase tracking-wide">
                    Business Model
                  </Label>
                  <div className="relative">
                    <select
                      value={newType}
                      onChange={(e) =>
                        setNewType(e.target.value as "RENTAL" | "VENDING")
                      }
                      className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-slate-900 outline-none appearance-none font-medium text-slate-700"
                    >
                      <option value="RENTAL">
                        Rental (Unlimited / Flat Fee)
                      </option>
                      <option value="VENDING">
                        Vending (Pay Per Use / Ticket)
                      </option>
                    </select>
                    <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
                      <Monitor size={16} />
                    </div>
                  </div>
                </div>

                <div className="h-px bg-slate-200" />

                <div className="space-y-1.5">
                  <Label className="text-xs font-bold text-slate-700 uppercase tracking-wide flex items-center gap-2">
                    <Mail size={14} />
                    Cashier Email
                  </Label>
                  <Input
                    type="email"
                    required
                    placeholder="cashier@kopi.com"
                    value={cashierEmail}
                    onChange={(e) => setCashierEmail(e.target.value)}
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none transition-all placeholder:text-slate-400 font-medium"
                  />
                </div>

                <div className="space-y-1.5">
                  <Label className="text-xs font-bold text-slate-700 uppercase tracking-wide flex items-center gap-2">
                    <Key size={14} />
                    Cashier Password
                  </Label>
                  <Input
                    type="password"
                    required
                    placeholder="••••••••"
                    value={cashierPassword}
                    onChange={(e) => setCashierPassword(e.target.value)}
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none transition-all placeholder:text-slate-400 font-medium"
                  />
                </div>

                <div className="pt-4 flex gap-3">
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => setShowCreateUserModal(false)}
                    className="flex-1 h-12 rounded-xl text-slate-500 hover:text-slate-900 hover:bg-slate-100"
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    disabled={isCreatingUser}
                    className="flex-1 h-12 rounded-xl bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-bold"
                  >
                    {isCreatingUser ? (
                      <Loader2 className="animate-spin" />
                    ) : (
                      "Create & Link"
                    )}
                  </Button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
