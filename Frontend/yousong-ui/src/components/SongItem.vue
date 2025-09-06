<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue";

// Props
const props = defineProps({
  song: { type: Object, required: true },
  isLoggedIn: { type: Boolean, default: false },
  currentPlayingId: { type: [Number, null], default: null }, // für "nur ein Song spielt"
});

// Events: parent steuert Edit/Delete & "nur einer spielt"
const emit = defineEmits(["edit", "delete", "request-play"]);

// Audio handling
const audioRef = ref(null);
const isPlaying = ref(false);
const currentTime = ref(0);
const duration = ref(0);
const seeking = ref(false);

const src = computed(() => `/api/songs/${props.song.id}/music`);
const isThisPlaying = computed(() => props.currentPlayingId === props.song.id);

function formatTime(sec) {
  const s = Math.floor(sec || 0);
  const m = Math.floor(s / 60);
  const r = s % 60;
  return `${m}:${r.toString().padStart(2, "0")}`;
}

function togglePlay() {
  const el = audioRef.value;
  if (!el) return;
  if (isPlaying.value) {
    el.pause();
  } else {
    emit("request-play", props.song.id); // parent setzt currentPlayingId
    el.play().catch(() => {}); // autoplay policies
  }
}

function onTimeUpdate() {
  if (!seeking.value) currentTime.value = audioRef.value.currentTime || 0;
}
function onLoadedMeta() {
  duration.value = audioRef.value.duration || 0;
}
function onEnded() {
  isPlaying.value = false;
  currentTime.value = 0;
}

function onSeek(e) {
  const el = audioRef.value;
  if (!el) return;
  const val = Number(e.target.value);
  el.currentTime = val;
  currentTime.value = val;
  seeking.value = false;
}

function onSeekStart() {
  seeking.value = true;
}
function onPlay() {
  isPlaying.value = true;
}
function onPause() {
  isPlaying.value = false;
}

// Wenn ein anderer Song spielen soll → diesen pausieren
watch(
    () => props.currentPlayingId,
    (id) => {
      const el = audioRef.value;
      if (!el) return;
      if (id !== props.song.id && !el.paused) {
        el.pause();
      }
    }
);

onMounted(() => {
  const el = audioRef.value;
  if (!el) return;
  el.addEventListener("timeupdate", onTimeUpdate);
  el.addEventListener("loadedmetadata", onLoadedMeta);
  el.addEventListener("ended", onEnded);
  el.addEventListener("play", onPlay);
  el.addEventListener("pause", onPause);
});
onBeforeUnmount(() => {
  const el = audioRef.value;
  if (!el) return;
  el.removeEventListener("timeupdate", onTimeUpdate);
  el.removeEventListener("loadedmetadata", onLoadedMeta);
  el.removeEventListener("ended", onEnded);
  el.removeEventListener("play", onPlay);
  el.removeEventListener("pause", onPause);
});
</script>

<template>
  <article class="card">
    <div class="head">
      <div class="title-wrap">
        <h3 class="title" :title="song.title">{{ song.title }}</h3>
        <span class="genre">{{ song.genre }}</span>
      </div>
      <div class="sub">
        <span class="artist">{{ song.artist?.name ?? "Unknown Artist" }}</span>
        <span class="dot">•</span>
        <span class="len">{{ Math.floor((song.length ?? 0) / 60) }}:{{
            ((song.length ?? 0) % 60).toString().padStart(2, "0")
          }}</span>
      </div>
    </div>

    <!-- Custom Player -->
    <div class="player">
      <button class="icon-btn play" @click="togglePlay" :aria-label="isPlaying ? 'Pause' : 'Play'">
        <!-- Play/Pause Icon -->
        <svg v-if="!isPlaying" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
          <path d="M8 5v14l11-7z" fill="currentColor"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
          <path d="M6 5h4v14H6zM14 5h4v14h-4z" fill="currentColor"/>
        </svg>
      </button>

      <div class="bar">
        <input
            class="seek"
            type="range"
            min="0"
            :max="duration || 0"
            :value="seeking ? undefined : currentTime"
            @input="onSeek"
            @mousedown="onSeekStart"
            @touchstart.passive="onSeekStart"
        />
      </div>

      <div class="times">
        <span class="t">{{ formatTime(currentTime) }}</span>
        <span class="t sep">/</span>
        <span class="t">{{ formatTime(duration) }}</span>
      </div>

      <!-- Hidden audio element -->
      <audio ref="audioRef" :src="src" preload="metadata"></audio>
    </div>

    <!-- Actions: nur wenn eingeloggt -->
    <div v-if="isLoggedIn" class="actions">
      <button class="icon-btn edit" title="Edit" @click="$emit('edit', song.id)" aria-label="Edit">
        <!-- Stift -->
        <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
          <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1.003 1.003 0 0 0 0-1.42l-2.34-2.34a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 1.84-1.82z" fill="currentColor"/>
        </svg>
      </button>
      <button class="icon-btn delete" title="Delete" @click="$emit('delete', song.id)" aria-label="Delete">
        <!-- Mistkübel -->
        <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
          <path d="M21,4H17.9A5.009,5.009,0,0,0,13,0H11A5.009,5.009,0,0,0,6.1,4H3A1,1,0,0,0,3,6H4V19a5.006,5.006,0,0,0,5,5h6a5.006,5.006,0,0,0,5-5V6h1a1,1,0,0,0,0-2ZM11,2h2a3.006,3.006,0,0,1,2.829,2H8.171A3.006,3.006,0,0,1,11,2Zm7,17a3,3,0,0,1-3,3H9a3,3,0,0,1-3-3V6H18Z"/><path d="M10,18a1,1,0,0,0,1-1V11a1,1,0,0,0-2,0v6A1,1,0,0,0,10,18Z"/><path d="M14,18a1,1,0,0,0,1-1V11a1,1,0,0,0-2,0v6A1,1,0,0,0,14,18Z"  fill="currentColor"/>
        </svg>
      </button>
    </div>
  </article>
</template>

<style scoped>
.card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px 14px 12px;
  box-shadow: 0 6px 16px rgba(0,0,0,.06);
  display: grid;
  gap: 10px;
  transition: transform .15s ease, box-shadow .2s ease, border-color .2s;
}
.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 28px rgba(0,0,0,.12);
  border-color: #dfe7ee;
}

.head { display: grid; gap: 4px; }
.title-wrap { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.title { margin: 0; font-size: 1.05rem; color: #111; font-weight: 800; }
.genre { font-size: .72rem; padding: 2px 8px; border-radius: 999px; border: 1px solid #d8efe5; color: #166b4c; background: #eef9f3; }
.sub { color:#555; display:flex; align-items:center; gap:6px; }
.dot { opacity:.6; }
.len { font-variant-numeric: tabular-nums; }

.player {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 8px 10px;
}

.icon-btn {
  display: inline-grid; place-items: center;
  width: 36px; height: 36px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  transition: background .15s, box-shadow .15s, transform .05s, color .15s, border-color .15s;
}
.icon-btn:hover { background: #f0fdf4; border-color: #bfead2; color: #1f8f5f; box-shadow: 0 4px 12px rgba(31,143,95,.15); }
.icon-btn:active { transform: translateY(1px); }

.icon-btn.play:hover { background: #eef3ff; border-color: #c8d9ff; color: #3a68ff; box-shadow: 0 4px 12px rgba(58,104,255,.15); }
.icon-btn.edit:hover { background: #eef3ff; border-color: #c8d9ff; color: #3a68ff; }
.icon-btn.delete:hover { background: #fff5f5; border-color: #ffd1d1; color: #e74c3c; }

.bar { display:flex; align-items:center; }
.seek {
  width: 100%;
  -webkit-appearance: none;
  appearance: none;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #42b983 0%, #a0e3c8 100%);
  outline: none;
}
.seek::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 14px; height: 14px; border-radius: 50%;
  background: #fff; border: 2px solid #42b983; box-shadow: 0 2px 6px rgba(0,0,0,.2);
}
.seek::-moz-range-thumb {
  width: 14px; height: 14px; border-radius: 50%;
  background: #fff; border: 2px solid #42b983; box-shadow: 0 2px 6px rgba(0,0,0,.2);
}

.times { display:flex; gap:6px; color:#333; font-variant-numeric: tabular-nums; }
.t.sep { opacity:.5; }
.actions { display:flex; gap: 8px; justify-content: flex-end; }
</style>
