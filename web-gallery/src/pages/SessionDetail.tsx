import { useEffect, useState, useCallback } from "react";
import { useParams } from "react-router-dom";
import { supabase } from "../lib/supabase";
import {
  X, // Ganti ArrowLeft jadi X karena konteksnya Close Tab
  Download,
  Image as ImageIcon,
  Calendar,
  Smartphone,
  Maximize2,
  CheckCircle2,
  Clock,
  Share2,
  ChevronRight,
  Keyboard,
} from "lucide-react";
import type { Session } from "../types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

// --- TYPES ---
interface PhotoAsset {
  id: string;
  url: string;
  type: "final" | "raw";
  index: number;
}

export default function SessionDetail() {
  const { sessionUuid } = useParams<{ sessionUuid: string }>();

  // --- STATE ---
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [activePhoto, setActivePhoto] = useState<PhotoAsset | null>(null);
  const [assets, setAssets] = useState<PhotoAsset[]>([]);
  const [isDownloading, setIsDownloading] = useState(false);

  // --- DATA FETCHING ---
  const fetchSession = useCallback(async () => {
    if (!sessionUuid) return;
    setLoading(true);

    try {
      const { data, error } = await supabase
        .from("sessions")
        .select("*")
        .eq("session_uuid", sessionUuid)
        .single();

      if (error) throw error;

      if (data) {
        setSession(data);

        // Transform Raw URLs to Asset Objects
        const rawUrls: string[] = data.raw_photos_urls
          ? JSON.parse(data.raw_photos_urls)
          : [];
        const processedAssets: PhotoAsset[] = [];

        if (data.final_photo_url) {
          processedAssets.push({
            id: "final",
            url: data.final_photo_url,
            type: "final",
            index: 0,
          });
        }

        rawUrls.forEach((url, idx) => {
          processedAssets.push({
            id: `raw-${idx}`,
            url,
            type: "raw",
            index: idx + 1,
          });
        });

        setAssets(processedAssets);
        if (processedAssets.length > 0) setActivePhoto(processedAssets[0]);
      }
    } catch (err) {
      console.error("Failed to fetch session:", err);
    } finally {
      setLoading(false);
    }
  }, [sessionUuid]);

  useEffect(() => {
    fetchSession();
  }, [fetchSession]);

  // --- KEYBOARD NAVIGATION (World Class UX) ---
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!activePhoto || assets.length <= 1) return;

      const currentIndex = assets.findIndex((a) => a.id === activePhoto.id);
      if (e.key === "ArrowRight") {
        const nextIndex = (currentIndex + 1) % assets.length;
        setActivePhoto(assets[nextIndex]);
      } else if (e.key === "ArrowLeft") {
        const prevIndex = (currentIndex - 1 + assets.length) % assets.length;
        setActivePhoto(assets[prevIndex]);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [activePhoto, assets]);

  // --- ACTIONS ---
  const handleDownloadSingle = async (url: string, filename: string) => {
    try {
      const response = await fetch(url);
      const blob = await response.blob();
      const link = document.createElement("a");
      link.href = window.URL.createObjectURL(blob);
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (e) {
      console.error("Download failed", e);
    }
  };

  const handleDownloadAll = async () => {
    if (!session || assets.length === 0) return;
    setIsDownloading(true);

    for (let i = 0; i < assets.length; i++) {
      const asset = assets[i];
      const filename =
        asset.type === "final"
          ? `kubik-${session.session_uuid.substring(0, 8)}-final.jpg`
          : `kubik-${session.session_uuid.substring(0, 8)}-raw-${
              asset.index
            }.jpg`;

      await handleDownloadSingle(asset.url, filename);
      // Stagger to prevent browser throttling
      await new Promise((resolve) => setTimeout(resolve, 300));
    }
    setIsDownloading(false);
  };

  // --- RENDER: LOADING STATE ---
  if (loading) {
    return (
      <div className="h-screen w-full flex flex-col items-center justify-center bg-gray-50 gap-4">
        <div className="w-12 h-12 border-4 border-blue-600/20 border-t-blue-600 rounded-full animate-spin" />
        <p className="text-gray-500 font-medium animate-pulse text-sm">
          Loading Inspection Room...
        </p>
      </div>
    );
  }

  // --- RENDER: NOT FOUND ---
  if (!session) {
    return (
      <div className="h-screen flex flex-col items-center justify-center bg-gray-50">
        <div className="text-center space-y-4">
          <div className="w-16 h-16 bg-gray-200 rounded-full flex items-center justify-center mx-auto text-gray-400">
            <ImageIcon size={24} />
          </div>
          <h2 className="text-lg font-bold text-gray-900">
            Session Data Not Found
          </h2>
          <Button onClick={() => window.close()} variant="outline">
            Close Tab
          </Button>
        </div>
      </div>
    );
  }

  // --- RENDER: MAIN UI ---
  return (
    <TooltipProvider delayDuration={300}>
      <div className="h-screen bg-white flex flex-col overflow-hidden font-sans">
        {/* 1. HEADER */}
        <header className="h-16 border-b border-gray-100 flex items-center justify-between px-6 bg-white/90 backdrop-blur-md z-50 sticky top-0">
          <div className="flex items-center gap-4">
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => window.close()}
                  className="rounded-full hover:bg-gray-100 text-gray-500"
                >
                  <X size={20} />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Close Tab</TooltipContent>
            </Tooltip>

            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-bold text-gray-900 text-lg tracking-tight">
                  Inspection Room
                </h1>
                <Badge
                  variant="outline"
                  className="font-mono text-[10px] text-gray-500 bg-gray-50 border-gray-200"
                >
                  {session.session_uuid.split("-")[0]}
                </Badge>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="outline"
                  size="sm"
                  className="hidden md:flex gap-2 text-gray-600"
                  onClick={() => {
                    navigator.clipboard.writeText(session.session_uuid);
                    // Toast logic here ideally
                  }}
                >
                  <Share2 size={14} />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Copy Session ID</TooltipContent>
            </Tooltip>

            <Button
              onClick={handleDownloadAll}
              disabled={isDownloading}
              size="sm"
              className={cn(
                "gap-2 transition-all min-w-[140px]",
                isDownloading
                  ? "bg-gray-100 text-gray-400"
                  : "bg-blue-600 hover:bg-blue-700 text-white"
              )}
            >
              {isDownloading ? (
                <>Processing...</>
              ) : (
                <>
                  <Download size={14} /> Download All
                </>
              )}
            </Button>
          </div>
        </header>

        {/* 2. WORKSPACE */}
        <div className="flex-1 flex overflow-hidden">
          {/* CANVAS AREA (Center) */}
          <div className="flex-1 bg-gray-50/50 relative flex items-center justify-center p-8 overflow-hidden group select-none">
            {/* Background Pattern */}
            <div
              className="absolute inset-0 opacity-[0.03] pointer-events-none"
              style={{
                backgroundImage:
                  "radial-gradient(#64748B 1px, transparent 1px)",
                backgroundSize: "24px 24px",
              }}
            />

            <AnimatePresence mode="wait">
              {activePhoto && (
                <motion.div
                  key={activePhoto.id}
                  initial={{ opacity: 0, scale: 0.98 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.2 }}
                  className="relative max-w-full max-h-full shadow-2xl shadow-gray-200/50 rounded-lg overflow-hidden ring-1 ring-black/5"
                >
                  <img
                    src={activePhoto.url}
                    alt="Inspection View"
                    className="max-w-full max-h-[calc(100vh-140px)] object-contain block bg-white"
                  />

                  {/* Floating Actions Overlay */}
                  <div className="absolute bottom-4 right-4 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 scale-95 group-hover:scale-100">
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button
                          size="icon"
                          variant="secondary"
                          className="rounded-full bg-white/90 backdrop-blur shadow-sm h-9 w-9"
                          onClick={() => window.open(activePhoto.url, "_blank")}
                        >
                          <Maximize2 size={14} />
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent>Open Full Size</TooltipContent>
                    </Tooltip>

                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button
                          size="icon"
                          variant="secondary"
                          className="rounded-full bg-white/90 backdrop-blur shadow-sm h-9 w-9"
                          onClick={() =>
                            handleDownloadSingle(activePhoto.url, `photo.jpg`)
                          }
                        >
                          <Download size={14} />
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent>Download This Photo</TooltipContent>
                    </Tooltip>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Keyboard Helper Hint */}
            <div className="absolute bottom-6 left-1/2 -translate-x-1/2 text-[10px] text-gray-300 flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <Keyboard size={12} />
              <span>Use Arrow Keys to Navigate</span>
            </div>
          </div>

          {/* INSPECTOR PANEL (Right Sidebar) */}
          <div className="w-[300px] bg-white border-l border-gray-100 flex flex-col z-10">
            {/* Metadata Section */}
            <div className="p-5">
              <h3 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4">
                Metadata
              </h3>
              <div className="space-y-4">
                <div className="flex items-center justify-between group">
                  <div className="flex items-center gap-2 text-sm text-gray-500">
                    <Smartphone size={14} />
                    <span>Device</span>
                  </div>
                  <span className="text-xs font-mono font-medium text-gray-900 bg-gray-50 px-1.5 py-0.5 rounded">
                    {session.device_id
                      ? session.device_id.substring(0, 8)
                      : "Unknown"}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm text-gray-500">
                    <Clock size={14} />
                    <span>Time</span>
                  </div>
                  <span className="text-xs font-medium text-gray-900">
                    {new Date(session.created_at).toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm text-gray-500">
                    <CheckCircle2 size={14} />
                    <span>Type</span>
                  </div>
                  <Badge
                    variant="secondary"
                    className={cn(
                      "text-[10px] h-5",
                      session.final_photo_url
                        ? "bg-emerald-50 text-emerald-600"
                        : "bg-orange-50 text-orange-600"
                    )}
                  >
                    {session.final_photo_url ? "Processed" : "Raw"}
                  </Badge>
                </div>
              </div>
            </div>

            <Separator className="bg-gray-100" />

            {/* Assets List Section */}
            <div className="flex-1 flex flex-col min-h-0">
              <div className="p-5 pb-2">
                <h3 className="text-xs font-bold text-gray-400 uppercase tracking-widest">
                  Assets ({assets.length})
                </h3>
              </div>

              <div className="flex-1 overflow-y-auto p-3 space-y-2 custom-scrollbar">
                {assets.map((asset) => (
                  <div
                    key={asset.id}
                    onClick={() => setActivePhoto(asset)}
                    className={cn(
                      "group flex gap-3 p-2 rounded-lg cursor-pointer transition-all duration-200 border",
                      activePhoto?.id === asset.id
                        ? "bg-blue-50/50 border-blue-200 ring-1 ring-blue-100"
                        : "bg-white border-transparent hover:border-gray-200 hover:bg-gray-50"
                    )}
                  >
                    {/* Thumbnail */}
                    <div className="w-12 h-12 rounded bg-gray-100 overflow-hidden shrink-0 relative border border-gray-100">
                      <img
                        src={asset.url}
                        alt="thumb"
                        className="w-full h-full object-cover"
                        loading="lazy"
                      />
                      {activePhoto?.id === asset.id && (
                        <div className="absolute inset-0 bg-blue-600/10" />
                      )}
                    </div>

                    {/* Info */}
                    <div className="flex-1 flex flex-col justify-center min-w-0">
                      <div className="flex justify-between items-center">
                        <p
                          className={cn(
                            "text-xs font-medium truncate",
                            activePhoto?.id === asset.id
                              ? "text-blue-700"
                              : "text-gray-700"
                          )}
                        >
                          {asset.type === "final"
                            ? "Final Result"
                            : `Raw Capture ${asset.index}`}
                        </p>
                      </div>
                      <p className="text-[10px] text-gray-400 mt-0.5">
                        JPG • Original
                      </p>
                    </div>

                    {/* Active Arrow */}
                    {activePhoto?.id === asset.id && (
                      <div className="flex items-center text-blue-500">
                        <ChevronRight size={14} />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Footer Info */}
            <div className="p-3 border-t border-gray-100 text-center bg-gray-50/50">
              <p className="text-[9px] text-gray-400 font-medium flex items-center justify-center gap-1">
                <Calendar size={10} />
                {new Date(session.created_at).toLocaleDateString()}
              </p>
            </div>
          </div>
        </div>
      </div>
    </TooltipProvider>
  );
}
