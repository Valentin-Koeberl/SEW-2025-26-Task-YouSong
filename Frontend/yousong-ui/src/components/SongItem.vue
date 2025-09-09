<template>
  <div class="vitem">
    <div class="row1">
      <div class="title-wrap">
        <h3 class="title" :title="song.title">{{ song.title }}</h3>
        <div class="sub">
          <span class="artist">{{ song.artist?.name ?? "Unknown Artist" }}</span>
          <span class="dot">•</span>
          <span class="len">{{ formatTime(song.length) }}</span>
        </div>
      </div>

      <div v-if="isLoggedIn" class="actions">
        <button class="icon-btn edit" title="Edit" @click="$emit('edit', song.id)" aria-label="Edit">
          <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
            <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1.003 1.003 0 0 0 0-1.42l-2.34-2.34a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 1.84-1.82z" fill="currentColor"/>
          </svg>
        </button>
        <button class="icon-btn delete" title="Delete" @click="$emit('delete', song.id)" aria-label="Delete">
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
      <button
          class="icon-btn play"
          @click="togglePlay"
          :aria-label="isPlaying ? 'Pause' : 'Play'"
      >
        <svg v-if="!isPlaying" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
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
          :max="duration || 0"
          :value="currentTime"
          @input="onSeek"
          @mousedown="seeking = true"
          @touchstart.passive="seeking = true"
      />

      <div class="times">
        <span class="t">{{ formatTime(currentTime) }}</span>
        <span class="t sep">/</span>
        <span class="t">{{ formatTime(duration) }}</span>
      </div>

      <audio
          ref="audioEl"
          :src="`/api/songs/${song.id}/music`"
          preload="metadata"
          @timeupdate="onTimeUpdate"
          @loadedmetadata="onLoadedMeta"
          @ended="onEnded"
          @play="onPlay"
          @pause="onPause"
      ></audio>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from "vue";

const props = defineProps({
  song: { type: Object, required: true },
  isLoggedIn: { type: Boolean, default: false },
  currentPlayingId: { type: [String, Number, null], default: null }
});

const emit = defineEmits(["edit", "delete", "playing"]);

const audioEl = ref(null);
const isPlaying = ref(false);
const seeking = ref(false);
const currentTime = ref(0);
const duration = ref(0);

// Wenn ein anderes Item spielt, pausieren
watch(() => props.currentPlayingId, (now) => {
  if (now !== props.song.id && isPlaying.value && audioEl.value) {
    audioEl.value.pause();
  }
});

const togglePlay = () => {
  const el = audioEl.value;
  if (!el) return;
  if (isPlaying.value) {
    el.pause();
  } else {
    emit("playing", props.song.id); // Parent markiert dieses Item als aktiv
    el.play().catch(() => {});
  }
};

const onTimeUpdate = () => {
  if (!seeking.value) currentTime.value = Math.floor(audioEl.value?.currentTime || 0);
};
const onLoadedMeta = () => { duration.value = Math.floor(audioEl.value?.duration || 0); };
const onEnded = () => { isPlaying.value = false; currentTime.value = 0; };
const onPlay = () => { isPlaying.value = true; };
const onPause = () => { isPlaying.value = false; };
const onSeek = (e) => {
  const el = audioEl.value; if (!el) return;
  const val = Number(e.target.value);
  el.currentTime = val; currentTime.value = val; seeking.value = false;
};

const formatTime = (sec) => {
  const s = Math.floor(sec || 0);
  const m = Math.floor(s / 60);
  const r = s % 60;
  return `${m}:${r.toString().padStart(2, "0")}`;
};
</script>

<style scoped>
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

@media (max-width: 720px) {
  .row1 { grid-template-columns: 1fr; gap: 8px; }
  .actions { justify-content: flex-start; }
}
</style>
