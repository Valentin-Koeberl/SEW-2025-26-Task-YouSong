<template>
  <div class="page">
    <h1>All Songs</h1>

    <p v-if="hint" class="hint">{{ hint }}</p>
    <p v-if="songs.length === 0 && activeQuery.length >= 2" class="no-results">
      No songs can be found. Please adjust your search.
    </p>

    <div class="list">
      <div v-for="song in songs" :key="song.id" class="item">
        <!-- LEFT (fix) -->
        <div class="left">
          <div class="title">{{ song.title }}</div>
          <div class="info-line">
            <span class="artist">{{ song.artist?.name ?? "Unknown Artist" }}</span>
            <span class="dot">•</span>
            <span class="genre">{{ song.genre }}</span>
            <span class="dot">•</span>
            <span class="len">{{ formatSecs(song.length) }}</span>
          </div>
        </div>

        <!-- MIDDLE (flex, Player zentriert & immer gleich breit) -->
        <div class="middle">
          <div class="player">
            <button class="icon-btn play" @click="togglePlay(song.id)" :aria-label="ps(song.id).isPlaying ? 'Pause' : 'Play'">
              <svg v-if="!ps(song.id).isPlaying" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                <path d="M8 5v14l11-7z" fill="currentColor"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                <path d="M6 5h4v14H6zM14 5h4v14h-4z" fill="currentColor"/>
              </svg>
            </button>

            <div class="bar">
              <input
                  class="seek"
                  type="range"
                  min="0"
                  :max="ps(song.id).duration || 0"
                  :value="ps(song.id).currentTime"
                  @input="onSeek(song.id, $event)"
                  @mousedown="onSeekStart(song.id)"
                  @touchstart.passive="onSeekStart(song.id)"
              />
            </div>

            <div class="times">
              <span class="t">{{ formatTime(ps(song.id).currentTime) }}</span>
              <span class="t sep">/</span>
              <span class="t">{{ formatTime(ps(song.id).duration) }}</span>
            </div>

            <!-- Hidden audio -->
            <audio
                :ref="el => setAudioRef(song.id, el)"
                :src="`/api/songs/${song.id}/music`"
                preload="metadata"
                @timeupdate="onTimeUpdate(song.id)"
                @loadedmetadata="onLoadedMeta(song.id)"
                @ended="onEnded(song.id)"
                @play="onPlay(song.id)"
                @pause="onPause(song.id)"
            ></audio>
          </div>
        </div>

        <!-- RIGHT (fix) -->
        <div class="right" v-if="isLoggedIn">
          <button class="icon-btn edit" title="Edit" @click="editSong(song.id)" aria-label="Edit">
            <!-- Stift -->
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
              <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1.003 1.003 0 0 0 0-1.42l-2.34-2.34a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 1.84-1.82z" fill="currentColor"/>
            </svg>
          </button>
          <button class="icon-btn delete" title="Delete" @click="deleteSong(song.id)" aria-label="Delete">
            <!-- Mistkübel (clean, no glitch) -->
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
              <path d="M7 4h10v2h4v2H3V6h4V4zm1 5h2v10H8V9zm4 0h2v10h-2V9zm4 0h2v10h-2V9z" fill="currentColor"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="!activeQuery && totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="page === 0" @click="goToFirst">⏮ First</button>
      <button class="page-btn" :disabled="page === 0" @click="prevPage">◀ Prev</button>
      <span class="page-indicator">Page {{ page + 1 }} / {{ totalPages }}</span>
      <button class="page-btn" :disabled="page === totalPages - 1" @click="nextPage">Next ▶</button>
      <button class="page-btn" :disabled="page === totalPages - 1" @click="goToLast">Last ⏭</button>
    </div>

    <p v-if="serverError" class="server-error">{{ serverError }}</p>
  </div>
</template>

<script setup>
import api from "../services/api";
import { ref, onMounted, watch, computed } from "vue";
import { useRouter } from "vue-router";
import { useAuth } from "../composables/useAuth";
import { useSearch } from "../composables/useSearch";

const router = useRouter();
const { isLoggedIn } = useAuth();
const { state: searchState } = useSearch();

const songs = ref([]);
const page = ref(0);
const totalPages = ref(0);
const size = 5;
const serverError = ref("");
const hint = ref("");

const activeQuery = computed(() => (searchState.query || "").trim());

// Single-player
const currentPlayingId = ref(null);
const audioRefs = ref({});      // { [id]: HTMLAudioElement }
const playerState = ref({});    // { [id]: { isPlaying, currentTime, duration, seeking } }

function ensureState(id) {
  if (!playerState.value[id]) {
    playerState.value[id] = { isPlaying: false, currentTime: 0, duration: 0, seeking: false };
  }
}
function setAudioRef(id, el) { if (el) audioRefs.value[id] = el; }
function ps(id) { ensureState(id); return playerState.value[id]; }

function formatTime(sec) {
  const s = Math.floor(sec || 0);
  const m = Math.floor(s / 60);
  const r = s % 60;
  return `${m}:${r.toString().padStart(2, "0")}`;
}
function formatSecs(sec) { return formatTime(sec); }

function togglePlay(id) {
  const el = audioRefs.value[id];
  if (!el) return;
  if (ps(id).isPlaying) {
    el.pause();
  } else {
    if (currentPlayingId.value && currentPlayingId.value !== id) {
      const other = audioRefs.value[currentPlayingId.value];
      if (other && !other.paused) other.pause();
    }
    currentPlayingId.value = id;
    el.play().catch(() => {});
  }
}
function onTimeUpdate(id) { if (!ps(id).seeking) ps(id).currentTime = audioRefs.value[id]?.currentTime || 0; }
function onLoadedMeta(id) { ps(id).duration = audioRefs.value[id]?.duration || 0; }
function onEnded(id) { ps(id).isPlaying = false; ps(id).currentTime = 0; if (currentPlayingId.value === id) currentPlayingId.value = null; }
function onPlay(id) { ps(id).isPlaying = true; currentPlayingId.value = id; }
function onPause(id) { ps(id).isPlaying = false; }
function onSeekStart(id) { ps(id).seeking = true; }
function onSeek(id, e) {
  const el = audioRefs.value[id]; if (!el) return;
  const val = Number(e.target.value);
  el.currentTime = val; ps(id).currentTime = val; ps(id).seeking = false;
}

// Daten laden (paging)
const fetchAll = async () => {
  serverError.value = "";
  try {
    const res = await api.get("/api/songs", { params: { page: page.value, size } });
    songs.value = res.data.content;
    totalPages.value = res.data.totalPages;
    songs.value.forEach(s => ensureState(s.id));
  } catch { serverError.value = "Could not load songs."; }
};

// Suche
const searchSongs = async (q) => {
  serverError.value = "";
  try {
    const res = await api.get("/api/songs/search", { params: { query: q } });
    songs.value = res.data;
    totalPages.value = 1; page.value = 0;
    songs.value.forEach(s => ensureState(s.id));
  } catch (e) {
    if (e?.response?.status === 400) { hint.value = "Type at least 2 characters to search."; songs.value = []; }
    else { serverError.value = "Search failed."; }
  }
};

// Debounce auf Query
let timer = null;
watch(activeQuery, (q) => {
  clearTimeout(timer);
  const val = q;
  if (!val) { hint.value = ""; timer = setTimeout(fetchAll, 150); return; }
  if (val.length < 2) { hint.value = "Type at least 2 characters to search."; songs.value = []; totalPages.value = 0; page.value = 0; return; }
  hint.value = ""; timer = setTimeout(() => searchSongs(val), 200);
});

const editSong = (id) => router.push({ name: "edit", params: { id } });

const deleteSong = async (id) => {
  if (!confirm("Are you sure you want to delete this song?")) return;
  serverError.value = "";
  try {
    await api.delete(`/api/songs/${id}`);
    songs.value = songs.value.filter((s) => s.id !== id);
    delete playerState.value[id]; delete audioRefs.value[id];
    if (currentPlayingId.value === id) currentPlayingId.value = null;
  } catch (e) {
    const status = e?.response?.status;
    if (status === 401) serverError.value = "Please login to delete songs.";
    else if (status === 403) serverError.value = "You can only delete your own songs.";
    else serverError.value = "Failed to delete song.";
  }
};

// Paging
const prevPage = async () => { if (page.value > 0) { page.value--; await fetchAll(); } };
const nextPage = async () => { if (page.value < totalPages.value - 1) { page.value++; await fetchAll(); } };
const goToFirst = async () => { page.value = 0; await fetchAll(); };
const goToLast = async () => { page.value = totalPages.value - 1; await fetchAll(); };

onMounted(fetchAll);
</script>

<style scoped>
.page { max-width: 1120px; margin: 24px auto; padding: 0 16px; }
h1 { margin: 0 0 14px; }

.hint { color:#666; font-size:.9rem; margin:0 0 12px; }
.no-results { color: #777; margin: 8px 0 12px; font-style: italic; }

.list { display: flex; flex-direction: column; gap: 12px; }

/* Feste Spalten: links fix, rechts fix, Mitte flexibel → Player immer gleich ausgerichtet */
.item {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 12px;
  display: grid;
  grid-template-columns: 360px 1fr 120px;
  align-items: center;
  gap: 16px;
  transition: border-color .18s, box-shadow .2s, transform .1s;
}
.item:hover {
  border-color: #e7edf5;
  box-shadow: 0 8px 20px rgba(0,0,0,.06);
  transform: translateY(-1px);
}

/* LEFT */
.left { min-width: 0; }
.title { font-size: 1.05rem; font-weight: 800; color: #111; margin: 0 0 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.info-line { display:flex; align-items:center; gap:6px; color:#555; font-size:.9rem; }
.genre { background:#eef9f3; color:#166b4c; border:1px solid #d8efe5; border-radius:999px; padding: 2px 8px; font-size:.72rem; }
.dot { opacity:.6; }
.len { font-variant-numeric: tabular-nums; }

/* MIDDLE – Player füllt den mittleren Bereich und ist zentriert */
.middle { display:flex; justify-content:center; align-items:center; }
.player {
  width: 100%;
  max-width: 640px;               /* immer gleiche max-Breite */
  min-width: 480px;               /* und min-Breite → stabiler Start/Ende */
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 8px 10px;
}

/* RIGHT */
.right { display:flex; gap:8px; justify-content:flex-end; }

/* Buttons & Slider */
.icon-btn {
  display: inline-grid; place-items: center;
  width: 36px; height: 36px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  transition: background .15s, border-color .15s, color .15s, box-shadow .15s, transform .05s;
}
.icon-btn:hover { background: #f6f8fb; border-color: #dfe7ee; box-shadow: 0 4px 10px rgba(0,0,0,.06); }
.icon-btn.play:hover { background: #eef3ff; border-color: #c8d9ff; color: #3a68ff; }
.icon-btn.edit:hover { background: #eef3ff; border-color: #c8d9ff; color: #3a68ff; }
.icon-btn.delete:hover { background: #fff6f6; border-color: #f3d3d3; color: #e74c3c; }

.bar { display:flex; align-items:center; }
.seek {
  width: 100%;
  -webkit-appearance: none; appearance: none;
  height: 8px; border-radius: 999px;
  background: linear-gradient(90deg, #42b983 0%, #a0e3c8 100%);
  outline: none;
}
.seek::-webkit-slider-thumb {
  -webkit-appearance: none; appearance: none;
  width: 14px; height: 14px; border-radius: 50%;
  background: #fff; border: 2px solid #42b983; box-shadow: 0 2px 6px rgba(0,0,0,.2);
}
.seek::-moz-range-thumb {
  width: 14px; height: 14px; border-radius: 50%;
  background: #fff; border: 2px solid #42b983; box-shadow: 0 2px 6px rgba(0,0,0,.2);
}

.times { display:flex; gap:6px; color:#333; font-variant-numeric: tabular-nums; }
.t.sep { opacity:.5; }

/* Pagination modern */
.pagination {
  display: flex; justify-content: center; align-items: center;
  gap: 10px; margin-top: 20px;
}
.page-indicator { font-weight: 700; color:#334; }
.page-btn {
  padding: 10px 14px;
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  transition: background .15s, border-color .15s, box-shadow .15s, transform .05s;
}
.page-btn:hover { background:#f6f8fb; border-color:#dfe7ee; box-shadow: 0 6px 14px rgba(0,0,0,.06); }
.page-btn:active { transform: translateY(1px); }
.page-btn:disabled { opacity:.55; cursor:not-allowed; box-shadow:none; }

/* Responsive */
@media (max-width: 960px) {
  .item { grid-template-columns: 300px 1fr 100px; }
  .player { min-width: 380px; max-width: 560px; }
}
@media (max-width: 760px) {
  .item { grid-template-columns: 1fr; align-items: stretch; }
  .left { order: 1; }
  .middle { order: 2; justify-content: stretch; }
  .player { width: 100%; min-width: 0; max-width: none; }
  .right { order: 3; justify-content: flex-end; }
}
</style>
