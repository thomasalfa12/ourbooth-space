import { useEffect, useState } from "react";
import { createClient } from "@supabase/supabase-js";

// 1. DEFINISI TIPE DATA (Interface)
// Ini wajib di TypeScript agar codingan tidak error saat akses properti
interface SessionData {
  id: number;
  created_at: string;
  session_uuid: string;
  final_photo_url: string;
  gif_url: string | null; // Bisa null kalau tidak ada boomerang
  raw_photos_urls?: string[] | null;
}

// --- KONFIGURASI SUPABASE ---
// Ganti string di bawah ini dengan URL & Anon Key asli Anda
const supabaseUrl = "https://xrbepnwafkbrvyncxqku.supabase.co";
const supabaseKey =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhyYmVwbndhZmticnZ5bmN4cWt1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM3MjAxMjYsImV4cCI6MjA3OTI5NjEyNn0.K1rbpv_Dduroh-_-mMSHQGdI1oClqNMpjl0j-t3ei1k";
const supabase = createClient(supabaseUrl, supabaseKey);

function App() {
  // 2. STATE DENGAN TIPE DATA
  const [sessionData, setSessionData] = useState<SessionData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    // Ambil ID dari URL (contoh: website.com/?id=123-abc)
    const params = new URLSearchParams(window.location.search);
    const sessionId = params.get("id");

    if (sessionId) {
      fetchSession(sessionId);
    } else {
      setLoading(false);
    }
  }, []);

  // Fungsi Fetch dengan tipe parameter string
  async function fetchSession(uuid: string) {
    try {
      const { data, error } = await supabase
        .from("sessions")
        .select("*")
        .eq("session_uuid", uuid)
        .single();

      if (error) throw error;

      // TypeScript sekarang tahu bahwa 'data' adalah SessionData
      setSessionData(data as SessionData);
    } catch (err) {
      console.error("Gagal ambil data:", err);
    } finally {
      setLoading(false);
    }
  }

  if (loading)
    return <div className="center-screen">Loading Memories... ⏳</div>;
  if (!sessionData)
    return (
      <div className="center-screen">
        Sesi tidak ditemukan atau Link salah 😢
      </div>
    );

  return (
    <div className="container">
      <header>
        <h1>✨ YOUR MEMORIES ✨</h1>
        <p>Kubik Photobooth</p>
      </header>

      <div className="gallery">
        {/* FOTO UTAMA */}
        <div className="card">
          <img src={sessionData.final_photo_url} alt="Photo Grid" />
          <a
            href={sessionData.final_photo_url}
            download
            className="btn-download"
          >
            Download Photo 📸
          </a>
        </div>

        {/* GIF BOOMERANG (Hanya muncul jika url-nya ada/tidak null) */}
        {sessionData.gif_url && (
          <div className="card">
            <img src={sessionData.gif_url} alt="Boomerang" />
            <a
              href={sessionData.gif_url}
              download
              className="btn-download secondary"
            >
              Download Boomerang 🎞️
            </a>
          </div>
        )}
      </div>

      <footer>
        <p>Terima kasih sudah seru-seruan bareng Kubik! 💙</p>
      </footer>

      {/* CSS INLINE (Agar praktis satu file) */}
      <style>{`
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #fdfbf7; color: #333; margin: 0; }
        .center-screen { display: flex; height: 100vh; justify-content: center; align-items: center; font-weight: bold; color: #666; }
        .container { max-width: 600px; margin: 0 auto; padding: 40px 20px; text-align: center; }
        
        header { margin-bottom: 40px; }
        header h1 { color: #6200EE; margin: 0; font-size: 2rem; letter-spacing: -1px; }
        header p { color: #888; margin-top: 5px; font-weight: 500; }

        .gallery { display: flex; flex-direction: column; gap: 20px; }
        
        .card { background: white; padding: 15px; border-radius: 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.08); transition: transform 0.2s; }
        .card:hover { transform: translateY(-5px); }
        .card img { width: 100%; border-radius: 16px; margin-bottom: 15px; display: block; }
        
        .btn-download { 
          display: block; 
          background: #6200EE; 
          color: white; 
          text-decoration: none; 
          padding: 16px; 
          border-radius: 16px; 
          font-weight: bold; 
          transition: background 0.2s;
        }
        .btn-download:hover { background: #4e00c2; }
        
        .btn-download.secondary { background: #03DAC6; color: #004d46; }
        .btn-download.secondary:hover { background: #01bdaf; }

        footer { margin-top: 50px; color: #aaa; font-size: 0.9rem; }
      `}</style>
    </div>
  );
}

export default App;
