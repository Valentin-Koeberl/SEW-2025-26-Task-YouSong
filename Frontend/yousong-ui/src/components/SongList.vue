<template>
  <div class="page">
    <h1>All Songs</h1>

    <div v-if="songs.length === 0" class="empty">
      No songs can be found. Please adjust your search.
    </div>

    <div class="vlist">
      <div v-for="song in songs" :key="song.id" class="vitem">
        <div class="row1">
          <div class="title-wrap">
            <h3 class="title" :title="song.title">{{ song.title }}</h3>
            <div class="sub">
              <span class="artist">{{ song.artist?.name ?? "Unknown Artist" }}</span>
              <span class="dot">•</span>
              <span class="len">{{ formatSecs(song.length) }}</span>
            </div>
          </div>
          <div v-if="isLoggedIn" class="actions">
            <button class="icon-btn edit" title="Edit" @click="editSong(song.id)" aria-label="Edit">
              <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1.003 1.003 0 0 0 0-1.42l-2.34-2.34a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 1.84-1.82z" fill="currentColor"/>
              </svg>
            </button>
            <button class="icon-btn delete" title="Delete" @click="deleteSong(song.id)" aria-label="Delete">
              <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                <path d="M7 4h10v2h4v2H3V6h4V4zm1 5h2v10H8V9zm4 0h2v10h-2V9zm4 0h2v10h-2V9z" fill="currentColor"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="row2" v-if="(song.genres || []).length">
          <span v-for="g in (song.genres || [])" :key="g" class="gchip">{{ g }}</span>
        </div>

        <div class="row3 player">
          <button class="icon-btn play" @click="togglePlay(song.id)" :aria-label="ps(song.id).isPlaying ? 'Pause' : 'Play'">
            <svg v-if="!ps(song.id).isPlaying" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
              <path d="M8 5v14l11-7z" fill="currentColor"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
              <path d="M6 5h4v14H6zM14 5h4v14h-4z" fill="currentColor"/>
            </svg>
          </button>

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

          <div class="times">
            <span class="t">{{ formatTime(ps(song.id).currentTime) }}</span>
            <span class="t sep">/</span>
            <span class="t">{{ formatTime(ps(song.id).duration) }}</span>
          </div>

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
    </div>

    <div v-if="totalPages > 1" class="pagination">
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

const activeQuery = computed(() => (searchState.query || "").trim());
const selectedGenres = computed(() => searchState.genres);

const currentPlayingId = ref(null);
const audioRefs = ref({});
const playerState = ref({});

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

const fetchCatalog = async () => {
  serverError.value = "";
  try {
    const params = { page: page.value, size };
    if (activeQuery.value.length >= 2) params.q = activeQuery.value;
    if (selectedGenres.value.length) params.genresCsv = selectedGenres.value.join(",");
    const res = await api.get("/api/songs/catalog", { params });
    songs.value = res.data.content || [];
    totalPages.value = res.data.totalPages ?? 0;
    songs.value.forEach(s => ensureState(s.id));
  } catch (e) {
    serverError.value = "Could not load songs.";
  }
};

const editSong = (id) => router.push({ name: "edit", params: { id } });
const deleteSong = async (id) => {
  if (!confirm("Are you sure you want to delete this song?")) return;
  serverError.value = "";
  try {
    await api.delete(`/api/songs/${id}`);
    await fetchCatalog();
  } catch (e) {
    const status = e?.response?.status;
    if (status === 401) serverError.value = "Please login to delete songs.";
    else if (status === 403) serverError.value = "You can only delete your own songs.";
    else serverError.value = "Failed to delete song.";
  }
};

const prevPage = async () => { if (page.value > 0) { page.value--; await fetchCatalog(); } };
const nextPage = async () => { if (page.value < totalPages.value - 1) { page.value++; await fetchCatalog(); } };
const goToFirst = async () => { page.value = 0; await fetchCatalog(); };
const goToLast = async () => { page.value = totalPages.value - 1; await fetchCatalog(); };

let timer = null;
watch(activeQuery, (q) => {
  clearTimeout(timer);
  page.value = 0;
  timer = setTimeout(fetchCatalog, 200);
});

watch(
    () => [...selectedGenres.value],
    () => {
      clearTimeout(timer);
      page.value = 0;
      timer = setTimeout(fetchCatalog, 150);
    }
);

onMounted(fetchCatalog);
</script>

<style scoped>
.page { width: 100%; max-width: none; margin: 0; padding: 0 0 16px; }
h1 { margin: 0 0 14px; padding: 0 16px; }

.empty { color:#777; margin: 8px 16px 12px; font-style: italic; }

.vlist { display: flex; flex-direction: column; gap: 12px; }

.vitem {
  width: 100%;
  background: #fff; border: 1px solid var(--border); border-radius: 16px; padding: 14px 16px;
  box-shadow: 0 8px 24px rgba(0,0,0,.06);
  display: grid; gap: 8px;
  transition: transform .12s ease, box-shadow .2s ease, border-color .2s;
}
.vitem:hover { transform: translateY(-2px); box-shadow: 0 14px 32px rgba(0,0,0,.10); border-color:#e7edf5; }

.row1 {
  display: grid; grid-template-columns: 1fr auto; gap: 12px; align-items: center;
}
.title-wrap { min-width: 0; }
.title { margin: 0; font-size: 1.15rem; color:#111; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sub { color:#555; display:flex; align-items:center; gap:6px; }
.dot { opacity:.6; }
.len { font-variant-numeric: tabular-nums; }

.actions { display:flex; gap:8px; justify-content:flex-end; }
.icon-btn {
  display: inline-grid; place-items: center; width: 36px; height: 36px;
  border-radius: 10px; border: 1px solid var(--border); background: #fff; cursor: pointer;
  transition: background .15s, border-color .15s, color .15s, box-shadow .15s, transform .05s;
}
.icon-btn:hover { background:#eef3ff; border-color:#c8d9ff; color:#3a68ff; box-shadow: 0 4px 10px rgba(58,104,255,.15); }

.row2 { display:flex; flex-wrap:wrap; gap:6px; }
.gchip {
  display:inline-flex; align-items:center;
  padding: 4px 10px; border-radius: 999px;
  background:#f6f8fb; border:1px solid #e2e8f0; color:#334155; font-weight: 700; font-size: .78rem;
}

.row3.player {
  display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px;
  background: #f8fafc; border: 1px solid var(--border); border-radius: 12px; padding: 8px 10px;
  margin-top: 4px;
}
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

.server-error { color:#e74c3c; margin-top: 10px; font-weight: 700; text-align: center; }
@media (max-width: 720px) {
  h1 { padding: 0 12px; }
  .row1 { grid-template-columns: 1fr; gap: 8px; }
  .actions { justify-content: flex-start; }
}
</style>
