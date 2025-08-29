<script setup>
import { ref, onMounted } from 'vue'
import SongItem from './components/SongItem.vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const songs = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const res = await fetch(`${API_BASE}/api/songs`)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    songs.value = await res.json()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="container">
    <header class="header">
      <h1 class="logo">🎵 YouSong</h1>
      <p class="subtitle">Discover your favorite tracks</p>
    </header>

    <p v-if="loading" class="loading">Loading songs…</p>
    <p v-else-if="error" class="error">Error: {{ error }}</p>

    <section v-else class="songs-grid">
      <SongItem v-for="s in songs" :key="s.id" :song="s" />
    </section>
  </main>
</template>

<style>
/* --- GLOBAL RESET --- */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* --- BODY STYLING --- */
body {
  font-family: 'Inter', system-ui, sans-serif;
  background: #f8f9fb;
  color: #222;
  -webkit-font-smoothing: antialiased;
}

/* --- CONTAINER --- */
.container {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 20px;
}

/* --- HEADER --- */
.header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  font-size: 2rem;
  font-weight: 800;
  color: #111;
  letter-spacing: -0.5px;
}

.subtitle {
  font-size: 1rem;
  color: #666;
  margin-top: 6px;
}

/* --- LOADING & ERROR STATES --- */
.loading,
.error {
  text-align: center;
  margin-top: 30px;
  font-size: 1.1rem;
}

.error {
  color: #e74c3c;
}

/* --- SONGS GRID --- */
.songs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
  margin-top: 20px;
}
</style>
