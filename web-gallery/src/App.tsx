import { useEffect, useState } from "react";
import { createClient } from "@supabase/supabase-js";

// --- TIPE DATA ---
interface SessionData {
  id: number;
  created_at: string;
  session_uuid: string;
  final_photo_url: string;
  gif_url: string | null;
}

// --- CONFIG SUPABASE ---
const supabaseUrl = "https://xrbepnwafkbrvyncxqku.supabase.co";
const supabaseKey =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhyYmVwbndhZmticnZ5bmN4cWt1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM3MjAxMjYsImV4cCI6MjA3OTI5NjEyNn0.K1rbpv_Dduroh-_-mMSHQGdI1oClqNMpjl0j-t3ei1k";
const supabase = createClient(supabaseUrl, supabaseKey);

function App() {
  const [sessionData, setSessionData] = useState<SessionData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const sessionId = params.get("id");

    if (sessionId) {
      fetchSession(sessionId);
    } else {
      setLoading(false);
      setErrorMsg("No Session ID provided.");
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
      setSessionData(data as SessionData);
    } catch (err) {
      console.error(err);
      setErrorMsg("Session not found or link expired.");
    } finally {
      setLoading(false);
    }
  }

  // FIX 1: Gunakan parameter 'title' di dalam object share
  const handleShare = async (url: string, title: string) => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: title, // <-- DIPAKAI DISINI
          text: "Check out my photo from Kubik Booth! ✨",
          url: url,
        });
      } catch (error) {
        console.log("Error sharing", error);
      }
    } else {
      navigator.clipboard.writeText(url);
      alert("Link copied to clipboard!");
    }
  };

  // FIX 2: Gunakan variable error 'e' untuk logging
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
    } catch (e) {
      // <-- DIPAKAI DISINI UNTUK LOGGING
      console.error("Download auto failed, fallback to new tab", e);
      window.open(url, "_blank");
    }
  };

  // --- RENDER ---

  if (loading)
    return (
      <div className="screen-center">
        <div className="loader"></div>
        <p>Fetching Memories...</p>
      </div>
    );

  if (errorMsg || !sessionData)
    return (
      <div className="screen-center">
        <div className="error-icon">💔</div>
        <p>{errorMsg || "Something went wrong."}</p>
      </div>
    );

  return (
    <div className="main-wrapper">
      {/* BACKGROUND DECORATION */}
      <div className="blob blob-1"></div>
      <div className="blob blob-2"></div>

      <div className="container">
        {/* HEADER */}
        <header className="fade-in-up">
          <div className="brand-pill">KUBIK BOOTH</div>
          <h1>
            Your Memories
            <br />
            Are Ready! ✨
          </h1>
          <p className="subtitle">
            {new Date(sessionData.created_at).toLocaleDateString("en-US", {
              weekday: "long",
              year: "numeric",
              month: "long",
              day: "numeric",
            })}
          </p>
        </header>

        {/* CONTENT */}
        <div className="gallery">
          {/* 1. PHOTO CARD */}
          <div className="card fade-in-up delay-1">
            <div className="image-wrapper">
              <img src={sessionData.final_photo_url} alt="My Photo" />
            </div>
            <div className="card-actions">
              <button
                onClick={() =>
                  handleDownload(
                    sessionData.final_photo_url,
                    `kubik_photo_${sessionData.session_uuid}.jpg`
                  )
                }
                className="btn btn-primary"
              >
                Download Photo 📸
              </button>
              <button
                onClick={() =>
                  handleShare(sessionData.final_photo_url, "My Photo")
                }
                className="btn btn-icon"
                aria-label="Share"
              >
                🔗
              </button>
            </div>
          </div>

          {/* 2. GIF CARD (Optional) */}
          {sessionData.gif_url && (
            <div className="card fade-in-up delay-2">
              <div className="badge-live">LIVE BOOMERANG</div>
              <div className="image-wrapper">
                <img src={sessionData.gif_url} alt="My Boomerang" />
              </div>
              <div className="card-actions">
                <button
                  onClick={() =>
                    handleDownload(
                      sessionData.gif_url!,
                      `kubik_gif_${sessionData.session_uuid}.gif`
                    )
                  }
                  className="btn btn-secondary"
                >
                  Save Video / GIF 🎞️
                </button>
              </div>
            </div>
          )}
        </div>

        {/* FOOTER */}
        <footer className="fade-in-up delay-3">
          <p>Tag us on Instagram!</p>
          <a
            href="https://instagram.com"
            target="_blank"
            className="social-link"
          >
            @kubik.photobooth
          </a>
          <div className="copyright">© 2025 Kubik Space</div>
        </footer>
      </div>

      {/* STYLES */}
      <style>{`
        :root {
          --bg-color: #FFF8E7; /* NeoCream */
          --primary: #8E44AD;   /* NeoPurple */
          --accent: #FFD600;    /* NeoYellow */
          --text: #2D3436;      /* NeoBlack */
          --white: #ffffff;
          --shadow: 0 12px 40px rgba(142, 68, 173, 0.15);
        }

        * { box-sizing: border-box; }

        body {
          font-family: 'Plus Jakarta Sans', 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
          background-color: var(--bg-color);
          color: var(--text);
          margin: 0;
          padding: 0;
          min-height: 100vh;
          overflow-x: hidden;
        }

        /* UTILS */
        .screen-center {
          height: 100vh;
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
          color: var(--primary);
          font-weight: bold;
        }
        
        /* ANIMATIONS */
        @keyframes fadeInUp {
          from { opacity: 0; transform: translateY(20px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .fade-in-up { animation: fadeInUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) forwards; opacity: 0; }
        .delay-1 { animation-delay: 0.1s; }
        .delay-2 { animation-delay: 0.2s; }
        .delay-3 { animation-delay: 0.3s; }

        @keyframes blobMove {
          0% { transform: translate(0, 0) scale(1); }
          50% { transform: translate(20px, -20px) scale(1.1); }
          100% { transform: translate(0, 0) scale(1); }
        }

        /* LAYOUT */
        .main-wrapper { position: relative; min-height: 100vh; }
        
        .blob {
          position: absolute;
          border-radius: 50%;
          filter: blur(60px);
          z-index: -1;
          opacity: 0.6;
          animation: blobMove 10s infinite ease-in-out;
        }
        .blob-1 { width: 300px; height: 300px; background: #EADDFF; top: -50px; left: -50px; }
        .blob-2 { width: 250px; height: 250px; background: #FFF5CC; bottom: 0; right: -50px; animation-delay: 5s; }

        .container {
          max-width: 480px; /* Mobile Size Limit */
          margin: 0 auto;
          padding: 40px 24px;
        }

        /* HEADER */
        header { text-align: center; margin-bottom: 40px; }
        
        .brand-pill {
          display: inline-block;
          background: var(--text);
          color: var(--white);
          padding: 6px 16px;
          border-radius: 50px;
          font-size: 0.75rem;
          font-weight: 800;
          letter-spacing: 1px;
          margin-bottom: 16px;
        }

        h1 {
          font-size: 2.2rem;
          line-height: 1.1;
          margin: 0 0 8px 0;
          color: var(--primary);
          font-weight: 800;
        }

        .subtitle { color: #888; font-weight: 500; font-size: 0.9rem; margin: 0; }

        /* CARDS */
        .gallery { display: flex; flex-direction: column; gap: 32px; }

        .card {
          background: var(--white);
          border-radius: 24px;
          padding: 16px;
          box-shadow: var(--shadow);
          position: relative;
          transition: transform 0.3s ease;
        }
        
        .card:active { transform: scale(0.98); }

        .image-wrapper {
          border-radius: 16px;
          overflow: hidden;
          background: #f0f0f0;
          /* Subtle border */
          border: 1px solid rgba(0,0,0,0.05);
        }

        .card img {
          width: 100%;
          height: auto;
          display: block;
        }

        .card-actions {
          display: flex;
          gap: 12px;
          margin-top: 16px;
        }

        .badge-live {
          position: absolute;
          top: 28px;
          left: 28px;
          background: #FF6B81;
          color: white;
          font-size: 0.7rem;
          font-weight: 800;
          padding: 4px 8px;
          border-radius: 6px;
          z-index: 10;
          box-shadow: 0 4px 10px rgba(255, 107, 129, 0.4);
        }

        /* BUTTONS */
        .btn {
          border: none;
          padding: 16px;
          border-radius: 14px;
          font-weight: 700;
          font-size: 1rem;
          cursor: pointer;
          transition: filter 0.2s;
          font-family: inherit;
        }
        
        .btn:active { filter: brightness(0.9); }

        .btn-primary {
          background: var(--primary);
          color: var(--white);
          flex: 1;
          box-shadow: 0 8px 20px rgba(142, 68, 173, 0.3);
        }

        .btn-secondary {
          background: var(--text);
          color: var(--white);
          width: 100%;
        }

        .btn-icon {
          background: #F3E5F5;
          width: 54px;
          font-size: 1.2rem;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        /* FOOTER */
        footer { text-align: center; margin-top: 60px; color: #888; }
        footer p { margin: 0 0 4px 0; font-size: 0.9rem; }
        .social-link { 
          color: var(--primary); 
          font-weight: 800; 
          text-decoration: none; 
          font-size: 1.1rem; 
        }
        .copyright { font-size: 0.7rem; margin-top: 24px; opacity: 0.5; }

        /* LOADER */
        .loader {
          width: 40px;
          height: 40px;
          border: 4px solid #ddd;
          border-top-color: var(--primary);
          border-radius: 50%;
          animation: spin 1s linear infinite;
          margin-bottom: 16px;
        }
        @keyframes spin { 100% { transform: rotate(360deg); } }

      `}</style>
    </div>
  );
}

export default App;
