import { useEffect, useState } from "react";
import { supabase } from "../lib/supabase";
import {
  Download,
  Share2,
  Image as ImageIcon,
  Check,
  Camera,
  Heart,
  Loader2, // Tambahan icon loading
  ArrowDownToLine,
} from "lucide-react";
import type { Session } from "../types";
import { motion } from "framer-motion";
import { cn } from "@/lib/utils";

// --- SUB-COMPONENT: Image with Loading State ---
const SmartImage = ({
  src,
  alt,
  className,
  aspectRatio = "aspect-2/3", // FIX WARNING: Pakai class standar, bukan arbitrary value
}: {
  src: string;
  alt: string;
  className?: string;
  aspectRatio?: string;
}) => {
  const [loaded, setLoaded] = useState(false);

  return (
    <div
      className={cn(
        "relative overflow-hidden bg-gray-100",
        aspectRatio,
        className
      )}
    >
      {!loaded && (
        <div className="absolute inset-0 flex items-center justify-center bg-gray-100 text-gray-300">
          <Loader2 className="w-8 h-8 animate-spin" />
        </div>
      )}
      <img
        src={src}
        alt={alt}
        className={cn(
          "w-full h-full object-cover transition-all duration-500",
          loaded ? "opacity-100 scale-100" : "opacity-0 scale-105"
        )}
        onLoad={() => setLoaded(true)}
      />
    </div>
  );
};

export default function ClientDownload() {
  const [sessionData, setSessionData] = useState<Session | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isCopied, setIsCopied] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const sessionId = params.get("id");

    if (sessionId) {
      fetchSession(sessionId);
    } else {
      setLoading(false);
      setErrorMsg("Invalid Link.");
    }
  }, []);

  async function fetchSession(uuid: string) {
    try {
      const { data, error } = await supabase
        .from("sessions")
        .select("*")
        .eq("session_uuid", uuid)
        .single();

      if (error) throw error;
      setSessionData(data as Session);
    } catch (err) {
      console.error(err);
      setErrorMsg("Session expired or not found.");
    } finally {
      setLoading(false);
    }
  }

  const rawPhotos: string[] = sessionData?.raw_photos_urls
    ? JSON.parse(sessionData.raw_photos_urls)
    : [];

  // --- HANDLERS ---
  const handleShare = async () => {
    const url = window.location.href;
    if (navigator.share) {
      try {
        await navigator.share({
          title: "My Kubik Photo",
          text: "Look at my photos from Kubik Booth! 📸",
          url: url,
        });
      } catch (error) {
        console.log(error);
      }
    } else {
      navigator.clipboard.writeText(url);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    }
  };

  const handleDownload = async (url: string, filename: string) => {
    try {
      const response = await fetch(url);
      const blob = await response.blob();
      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = blobUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(blobUrl);
    } catch {
      window.open(url, "_blank");
    }
  };

  // --- RENDER STATES ---

  if (loading)
    return (
      // FIX WARNING: bg-[#FAFAFA] -> bg-kubik-bg
      <div className="min-h-screen flex flex-col items-center justify-center bg-kubik-bg">
        <div className="relative">
          <div className="w-16 h-16 border-4 border-blue-100 border-t-kubik-blue rounded-full animate-spin" />
          <div className="absolute inset-0 flex items-center justify-center">
            <Camera size={20} className="text-kubik-blue" />
          </div>
        </div>
        <p className="mt-6 font-bold text-kubik-black text-sm tracking-widest uppercase animate-pulse">
          Developing Photos...
        </p>
      </div>
    );

  if (errorMsg || !sessionData)
    return (
      // FIX WARNING: bg-[#FAFAFA] -> bg-kubik-bg
      <div className="min-h-screen flex flex-col items-center justify-center bg-kubik-bg px-6 text-center">
        <div className="w-20 h-20 bg-red-50 rounded-full flex items-center justify-center mb-6 shadow-sm">
          <Heart className="w-10 h-10 text-red-400 fill-red-400" />
        </div>
        <h2 className="text-2xl font-black text-kubik-black mb-2">
          Link Expired
        </h2>
        <p className="text-gray-500 max-w-xs mx-auto leading-relaxed">
          {errorMsg || "We couldn't find the photos you're looking for."}
        </p>
      </div>
    );

  return (
    // FIX WARNING: bg-[#FAFAFA] -> bg-kubik-bg
    <div className="min-h-screen w-full bg-kubik-bg font-sans selection:bg-kubik-blue selection:text-white pb-32">
      {/* BACKGROUND DECOR */}
      <div
        className="fixed inset-0 pointer-events-none opacity-[0.03]"
        style={{
          backgroundImage: "radial-gradient(#000 1px, transparent 1px)",
          backgroundSize: "20px 20px",
        }}
      />

      {/* --- HERO SECTION --- */}
      <div className="p-6 pb-0 pt-12 text-center space-y-4 relative z-10">
        <motion.div
          initial={{ y: -20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.6 }}
        >
          <div className="inline-flex items-center gap-2 bg-kubik-black text-white px-4 py-1.5 rounded-full text-[10px] font-bold tracking-[0.2em] uppercase mb-4 shadow-lg shadow-kubik-black/20">
            <Camera size={12} /> Kubik Moment
          </div>
          <h1 className="text-4xl font-black text-kubik-black tracking-tight leading-tight">
            Your <span className="text-kubik-blue">Photos</span>
            <br />
            Are Ready!
          </h1>
          <p className="text-gray-400 text-xs font-bold uppercase tracking-widest mt-3">
            {new Date(sessionData.created_at).toLocaleDateString("en-US", {
              weekday: "long",
              day: "numeric",
              month: "long",
            })}
          </p>
        </motion.div>
      </div>

      <div className="max-w-md mx-auto relative flex flex-col">
        {/* --- MAIN CONTENT --- */}
        <main className="flex-1 p-6 space-y-8">
          {/* FINAL PHOTO CARD (POLAROID STYLE) */}
          <motion.div
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ delay: 0.2, duration: 0.5 }}
            className="relative group"
          >
            {/* KOTAK FOTO */}
            <div className="bg-white p-3 pb-16 rounded-sm shadow-[0_20px_50px_-12px_rgba(0,0,0,0.15)] rotate-1 transition-transform duration-500 group-hover:rotate-0 border border-gray-100">
              {sessionData.final_photo_url ? (
                // FIX WARNING: aspect-[2/3] -> aspect-2/3
                <SmartImage
                  src={sessionData.final_photo_url}
                  alt="Final Print"
                  aspectRatio="aspect-2/3"
                  className="rounded-sm"
                />
              ) : (
                <div className="aspect-2/3 bg-gray-100 flex items-center justify-center text-gray-300">
                  No Image
                </div>
              )}

              {/* Watermark */}
              <div className="absolute bottom-6 left-0 w-full text-center">
                <p className="font-handwriting text-gray-400 text-sm transform -rotate-2">
                  #kubikmoments
                </p>
              </div>
            </div>

            {/* --- FIX UI: TOMBOL DIHAPUS DARI SINI --- */}
            {/* Kode lama (absolute -bottom-6) dihapus agar tidak error layout */}
          </motion.div>

          {/* RAW PHOTOS SECTION */}
          {rawPhotos.length > 0 && (
            <motion.div
              initial={{ y: 20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.4 }}
              className="pt-8"
            >
              <div className="flex items-center justify-between mb-6 px-1">
                <h3 className="font-bold text-lg text-kubik-black flex items-center gap-2">
                  <ImageIcon className="text-kubik-blue" size={20} />
                  Original Snaps
                </h3>
                <span className="text-xs font-bold text-gray-400 bg-gray-100 px-2 py-1 rounded-md">
                  {rawPhotos.length} ITEMS
                </span>
              </div>

              <div className="grid grid-cols-2 gap-4">
                {rawPhotos.map((url, idx) => (
                  <div key={idx} className="space-y-3">
                    <div className="rounded-2xl overflow-hidden shadow-sm border border-gray-100 bg-gray-50 relative group">
                      <SmartImage
                        src={url}
                        alt={`Raw ${idx}`}
                        aspectRatio="aspect-4/5"
                      />
                      <button
                        onClick={() =>
                          handleDownload(
                            url,
                            `kubik-${sessionData.session_uuid}-raw-${idx}.jpg`
                          )
                        }
                        className="absolute bottom-2 right-2 w-8 h-8 bg-white/90 backdrop-blur rounded-full flex items-center justify-center text-kubik-black shadow-sm opacity-0 group-hover:opacity-100 transition-opacity active:scale-90"
                      >
                        <ArrowDownToLine size={14} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          )}
        </main>
      </div>

      {/* --- FIX UI: TOMBOL PINDAH KE SINI (Sticky Bottom) --- */}
      {/* Ini akan menjamin tombol berwarna BIRU dan selalu terlihat */}
      <div className="fixed bottom-0 inset-x-0 bg-white/90 backdrop-blur-xl border-t border-gray-100 p-4 pb-8 z-50">
        <div className="max-w-md mx-auto flex gap-3">
          {/* Share Button */}
          <button
            onClick={handleShare}
            className="h-14 w-14 flex items-center justify-center rounded-2xl bg-gray-50 border border-gray-200 text-kubik-black active:scale-95 transition-transform hover:bg-gray-100"
          >
            {isCopied ? (
              <Check size={20} className="text-green-600" />
            ) : (
              <Share2 size={20} />
            )}
          </button>

          {/* Download Button (Primary) - PASTI BIRU */}
          {sessionData && sessionData.final_photo_url && (
            <button
              onClick={() =>
                handleDownload(sessionData.final_photo_url!, "kubik-final.jpg")
              }
              className="flex-1 h-14 rounded-2xl bg-kubik-blue text-white font-bold text-base tracking-wide shadow-xl shadow-kubik-blue/30 flex items-center justify-center gap-2 active:scale-95 transition-all hover:bg-kubik-blueLight"
            >
              <Download size={20} />
              <span>SAVE TO GALLERY</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
