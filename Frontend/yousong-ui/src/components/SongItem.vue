<script setup>
import { computed } from 'vue';

const props = defineProps({
  song: { type: Object, required: true },
  isLoggedIn: { type: Boolean, default: false },
  currentPlayingId: { type: [Number, null], default: null },
});

const emit = defineEmits(["edit", "delete", "request-play"]);

const src = computed(() => `/api/songs/${props.song.id}/music`);
const isThisPlaying = computed(() => props.currentPlayingId === props.song.id);

const lengthMMSS = computed(() => {
  const secs = Number(props.song.length ?? 0);
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
});
</script>

<template>
  <article class="card">
    <div class="head">
      <div class="title-wrap">
        <h3 class="title" :title="song.title">{{ song.title }}</h3>
        <div class="genres">
          <span v-for="g in (song.genres || [])" :key="g" class="genre">{{ g }}</span>
        </div>
      </div>
      <div class="sub">
        <span class="artist">{{ song.artist?.name ?? "Unknown Artist" }}</span>
        <span class="dot">•</span>
        <span class="len">{{ lengthMMSS }}</span>
      </div>
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
}
.head { display: grid; gap: 4px; }
.title-wrap { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.title { margin: 0; font-size: 1.05rem; color: #111; font-weight: 800; }
.genres { display:flex; gap:6px; flex-wrap:wrap; }
.genre { font-size: .72rem; padding: 2px 8px; border-radius: 999px; border: 1px solid #d8efe5; color: #166b4c; background: #eef9f3; }
.sub { color:#555; display:flex; align-items:center; gap:6px; }
.dot { opacity:.6; }
.len { font-variant-numeric: tabular-nums; }
</style>
