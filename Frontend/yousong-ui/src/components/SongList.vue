<template>
  <div class="page">
    <h1>All Songs</h1>

    <div class="toolbar">
      <input
          type="text"
          v-model="searchQuery"
          @input="onSearch"
          placeholder="Search by title or artist..."
      />
      <button class="btn primary" @click="goToCreate">Add Song</button>
    </div>

    <p v-if="songs.length === 0 && searchQuery" class="no-results">
      No songs can be found. Please adjust your search.
    </p>

  <div class="list">
      <div v-for="song in songs" :key="song.id" class="item">
        <div class="info">
          <div class="title-row">
            <strong class="title">{{ song.title }}</strong>
            <span class="genre">{{ song.genre }}</span>
          </div>
          <div class="sub">{{ song.artistName ?? "Unknown Artist" }}</div>
          <div class="meta">{{ song.length }} sec</div>
        </div>
        <div class="actions">
          <button class="btn edit" @click="editSong(song.id)">Edit</button>
          <button class="btn delete" @click="deleteSong(song.id)">Delete</button>
        </div>
      </div>
    </div>
    <div class="pagination" v-if="!searchQuery">
      <button class="btn" @click="goToFirst" :disabled="page === 0">First</button>
      <button class="btn" @click="goToPrev" :disabled="page === 0">Prev</button>
      <span class="page-info">Page {{ page + 1 }} / {{ totalPages }}</span>
      <button class="btn" @click="goToNext" :disabled="page >= totalPages - 1">Next</button>
      <button class="btn" @click="goToLast" :disabled="page >= totalPages - 1">Last</button>
    </div>
  </div>
</template>

<script setup>
import axios from "axios";
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

const songs = ref([]);
const searchQuery = ref("");
const page = ref(0);
const totalPages = ref(0);
const pageSize = 5;
const router = useRouter();

const fetchPage = async (p = 0) => {
  const res = await axios.get("http://localhost:8080/api/songs", {
    params: { page: p, size: pageSize },
  });
  songs.value = res.data.songs;
  page.value = res.data.page;
  totalPages.value = res.data.totalPages;
};

const searchSongs = async (q) => {
  const res = await axios.get("http://localhost:8080/api/songs/search", {
    params: { query: q },
  });
  songs.value = res.data;
};

const onSearch = async () => {
  const q = searchQuery.value.trim();
  if (!q) return fetchPage(0);
  await searchSongs(q);
};

const goToCreate = () => router.push({ name: "create" });
const editSong = (id) => router.push({ name: "edit", params: { id } });

const deleteSong = async (id) => {
  if (!confirm("Are you sure you want to delete this song?")) return;
  await axios.delete(`http://localhost:8080/api/songs/${id}`);
  await fetchPage(page.value);
};

const goToFirst = () => fetchPage(0);
const goToPrev = () => fetchPage(page.value - 1);
const goToNext = () => fetchPage(page.value + 1);
const goToLast = () => fetchPage(totalPages.value - 1);

onMounted(() => fetchPage(0));
</script>

<style scoped>
.page{ max-width: 900px; margin: 24px auto; padding: 0 16px; }
h1{ margin: 0 0 14px; }

.toolbar{ display:flex; gap:10px; align-items:center; margin-bottom:14px; }
.toolbar input{ flex:1 1 auto; padding:10px 12px; border:1px solid var(--border); border-radius:6px; font-size:0.95rem; }

.btn{ padding:8px 12px; border:none; border-radius:6px; font-weight:600; cursor:pointer; }
.btn.primary{ background:var(--brand); color:#fff; }
.btn.edit{ background:#4e9eff; color:#fff; }
.btn.delete{ background:#e74c3c; color:#fff; }

.no-results{ color:#777; margin:8px 0 12px; font-style:italic; }

.list{ display:flex; flex-direction:column; gap:12px; }
.item{ background:var(--card); border:1px solid var(--border); border-radius:10px; padding:12px; display:flex; justify-content:space-between; gap:12px; }
.title-row{ display:flex; align-items:center; gap:8px; }
.title{ font-size:1.05rem; }
.genre{ font-size:.75rem; padding:2px 8px; border-radius:999px; border:1px solid #d8efe5; color:#166b4c; background:#eef9f3; }
.sub{ color:#333; }
.meta{ color:#777; font-size:.9rem; }
.actions{ display:flex; gap:8px; align-items:center; }
.pagination{ display:flex; gap:8px; align-items:center; margin-top:16px; }
.pagination .page-info{ margin:0 8px; }
</style>
