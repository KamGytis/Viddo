import { useState, useEffect, useRef } from "react";

const API = "http://localhost:8080/api";
const FORMATS = ["MP4", "MP3", "WEBM", "AVI", "MOV"];

export default function App() {
  const [url, setUrl] = useState("");
  const [format, setFormat] = useState("MP4");
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const pollingRef = useRef({});

  const submit = async () => {
    if (!url.trim()) return setError("Please enter a URL");
    setError("");
    setLoading(true);
    try {
      const res = await fetch(`${API}/convert`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url, outputFormat: format }),
      });
      if (!res.ok) throw new Error("Failed to submit job");
      const job = await res.json();
      setJobs((prev) => [job, ...prev]);
      setUrl("");
      startPolling(job.id);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const startPolling = (jobId) => {
    if (pollingRef.current[jobId]) return;
    pollingRef.current[jobId] = setInterval(async () => {
      try {
        const res = await fetch(`${API}/jobs/${jobId}`);
        const updated = await res.json();
        setJobs((prev) => prev.map((j) => (j.id === jobId ? updated : j)));
        if (updated.status === "DONE" || updated.status === "ERROR") {
          clearInterval(pollingRef.current[jobId]);
          delete pollingRef.current[jobId];
        }
      } catch {}
    }, 1500);
  };

  const download = (job) => {
    window.open(`${API}/jobs/${job.id}/download`, "_blank");
  };

  const statusColor = (status) => ({
    PENDING: "#888",
    DOWNLOADING: "#3b9eff",
    CONVERTING: "#f59e0b",
    DONE: "#00ffa3",
    ERROR: "#ff4466",
  }[status] || "#888");

  return (
    <div style={styles.page}>
      <div style={styles.wrapper}>

        {/* Header */}
        <div style={styles.header}>
          <span style={styles.badge}>VIDEO CONVERTER</span>
          <h1 style={styles.title}>
            Convert any<br /><span style={styles.accent}>link.</span>
          </h1>
          <p style={styles.subtitle}>YouTube · Twitter · Instagram · TikTok · and 1000+ more</p>
        </div>

        {/* Form */}
        <div style={styles.card}>
          <div style={styles.field}>
            <label style={styles.label}>VIDEO URL</label>
            <input
              style={styles.input}
              type="text"
              placeholder="https://youtube.com/watch?v=..."
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && submit()}
            />
          </div>

          <div style={styles.field}>
            <label style={styles.label}>OUTPUT FORMAT</label>
            <div style={styles.formatGrid}>
              {FORMATS.map((f) => (
                <button
                  key={f}
                  style={{ ...styles.formatBtn, ...(format === f ? styles.formatBtnActive : {}) }}
                  onClick={() => setFormat(f)}
                >
                  {f}
                </button>
              ))}
            </div>
          </div>

          {error && <p style={styles.error}>{error}</p>}

          <button style={styles.submitBtn} onClick={submit} disabled={loading}>
            {loading ? "SUBMITTING..." : "CONVERT →"}
          </button>
        </div>

        {/* Jobs */}
        {jobs.length > 0 && (
          <div>
            <div style={styles.jobsHeader}>
              <span style={styles.jobsTitle}>JOBS</span>
              <span style={styles.jobsCount}>{jobs.length}</span>
            </div>
            <div style={styles.jobsList}>
              {jobs.map((job) => (
                <div key={job.id} style={styles.jobCard}>
                  <div style={styles.jobTop}>
                    <span style={styles.jobUrl}>{job.url}</span>
                    <span style={{ ...styles.jobStatus, color: statusColor(job.status) }}>
                      ● {job.status}
                    </span>
                  </div>
                  <div style={styles.jobMeta}>
                    <span style={styles.jobFormat}>{job.outputFormat}</span>
                    <span style={styles.jobMessage}>{job.message}</span>
                  </div>
                  {job.status !== "DONE" && job.status !== "ERROR" && (
                    <div style={styles.progressBar}>
                      <div style={{ ...styles.progressFill, width: `${job.progressPercent}%` }} />
                    </div>
                  )}
                  {job.status === "DONE" && (
                    <button style={styles.downloadBtn} onClick={() => download(job)}>
                      ↓ DOWNLOAD {job.outputFormat}
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

const styles = {
  page: { background: "#0a0a0f", minHeight: "100vh", color: "#e8e8f0", fontFamily: "'Segoe UI', sans-serif" },
  wrapper: { maxWidth: 680, margin: "0 auto", padding: "72px 24px" },
  header: { marginBottom: 48 },
  badge: { display: "inline-block", fontSize: 11, color: "#00ffa3", border: "1px solid #00ffa3", padding: "3px 10px", letterSpacing: "0.15em", marginBottom: 16 },
  title: { fontSize: 56, fontWeight: 800, lineHeight: 1.05, letterSpacing: "-0.03em", margin: "0 0 12px" },
  accent: { color: "#00ffa3" },
  subtitle: { color: "#555570", fontSize: 14, fontFamily: "monospace" },
  card: { background: "#111118", border: "1px solid #1e1e2e", padding: 32, marginBottom: 40, borderTop: "2px solid #00ffa3" },
  field: { marginBottom: 20 },
  label: { display: "block", fontSize: 11, letterSpacing: "0.12em", textTransform: "uppercase", color: "#555570", marginBottom: 8, fontFamily: "monospace" },
  input: { width: "100%", background: "#0a0a0f", border: "1px solid #1e1e2e", color: "#e8e8f0", fontFamily: "monospace", fontSize: 14, padding: "12px 14px", outline: "none", boxSizing: "border-box" },
  formatGrid: { display: "grid", gridTemplateColumns: "repeat(5, 1fr)", gap: 8 },
  formatBtn: { padding: "10px 8px", background: "#0a0a0f", border: "1px solid #1e1e2e", color: "#555570", fontFamily: "monospace", fontSize: 12, fontWeight: 700, cursor: "pointer", textAlign: "center" },
  formatBtnActive: { background: "#00ffa3", borderColor: "#00ffa3", color: "#000" },
  error: { color: "#ff4466", fontFamily: "monospace", fontSize: 13, marginBottom: 12 },
  submitBtn: { width: "100%", padding: 16, background: "#00ffa3", border: "none", color: "#000", fontSize: 14, fontWeight: 800, letterSpacing: "0.05em", cursor: "pointer", marginTop: 8 },
  jobsHeader: { display: "flex", alignItems: "center", gap: 12, marginBottom: 16 },
  jobsTitle: { fontFamily: "monospace", fontSize: 11, letterSpacing: "0.15em", textTransform: "uppercase", color: "#555570" },
  jobsCount: { background: "#00ffa3", color: "#000", fontFamily: "monospace", fontSize: 10, fontWeight: 700, padding: "2px 8px" },
  jobsList: { display: "flex", flexDirection: "column", gap: 10 },
  jobCard: { background: "#111118", border: "1px solid #1e1e2e", padding: "18px 20px" },
  jobTop: { display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8, gap: 12 },
  jobUrl: { fontFamily: "monospace", fontSize: 12, color: "#888", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", maxWidth: "70%" },
  jobStatus: { fontFamily: "monospace", fontSize: 11, fontWeight: 700, whiteSpace: "nowrap" },
  jobMeta: { display: "flex", gap: 12, alignItems: "center" },
  jobFormat: { fontFamily: "monospace", fontSize: 10, background: "#1e1e2e", padding: "2px 8px", color: "#00ffa3" },
  jobMessage: { fontFamily: "monospace", fontSize: 12, color: "#555570" },
  progressBar: { marginTop: 12, height: 2, background: "#1e1e2e" },
  progressFill: { height: "100%", background: "#00ffa3", transition: "width 0.4s ease" },
  downloadBtn: { marginTop: 12, padding: "8px 16px", background: "transparent", border: "1px solid #00ffa3", color: "#00ffa3", fontFamily: "monospace", fontSize: 12, fontWeight: 700, cursor: "pointer" },
};