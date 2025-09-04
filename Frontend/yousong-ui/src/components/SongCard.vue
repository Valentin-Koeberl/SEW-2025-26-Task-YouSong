<script setup>
import { computed } from 'vue';

const props = defineProps({
  song: { type: Object, required: true }
});

const lengthMMSS = computed(() => {
  const secs = Number(props.song.length ?? 0);
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
});
</script>

<template>
  <article class="song-card">
    <div class="song-info">
      <h3 class="song-title">{{ song.title }}</h3>
      <p class="song-artist">by {{ song.artist?.name || 'Unknown Artist' }}</p>
    </div>
    <div class="song-meta">
      <span class="song-genre">{{ song.genre }}</span>
      <span class="song-length">{{ lengthMMSS }}</span>
    </div>
  </article>
</template>

<style scoped>
.song-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: all 0.2s ease;
  cursor: pointer;
  border: 1px solid #eaeaea;
}
.song-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
}
.song-info { margin-bottom: 14px; }
.song-title { font-size: 1.15rem; font-weight: 700; color: #111; margin-bottom: 6px; }
.song-artist { font-size: 0.95rem; color: #666; }
.song-meta { display: flex; justify-content: space-between; font-size: 0.9rem; }
.song-genre { background: #eef3ff; color: #3a68ff; padding: 3px 8px; border-radius: 6px; font-weight: 500; }
.song-length { color: #555; font-weight: 600; }
</style>
