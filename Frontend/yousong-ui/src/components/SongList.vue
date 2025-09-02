<template>
  <div class="page">
    <h1>All Songs</h1>

    <!-- Toolbar mit Suche + Button -->
    <div class="toolbar">
      <input
          type="text"
          v-model="searchQuery"
          @input="onSearch"
          placeholder="Search by title or artist..."
      />
      <button class="btn primary" @click="goToCreate">Add Song</button>
    </div>

    <!-- Keine Ergebnisse -->
    <p v-if="songs.length === 0 && searchQuery" class="no-results">
      No songs can be found. Please adjust your search.
    </p>

    <!-- Song-Liste -->
    <div class="list">
      <div v-for="song in songs" :key="song.id" class="item">
        <div class="info">
          <div class="title-row">
            <strong class="title">{{ song.title }}</strong>
            <span class="genre">{{ song.genre }}</span>
          </div>
          <div class="sub">{{ song.artist?.name ?? "Unknown Artist" }}</div>
          <div class="meta">{{ song.length }} sec</div>
        </div>
        <div class="actions">
          <button class="btn edit" @click="editSong(song.id)">Edit</button>
          <button class="btn delete" @click="deleteSong(song.id)">Delete</button>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="!searchQuery && totalPages > 1" class="pagination">
      <button class="btn" :disabled="page === 0" @click="goToFirst">⏮ First</button>
      <button class="btn" :disabled="page === 0" @click="prevPage">◀ Prev</button>
      <span>Page {{ page + 1 }} / {{ totalPages }}</span>
      <button class="btn" :disabled="page === totalPages - 1" @click="nextPage">Next ▶</button>
      <button class="btn" :disabled="page === totalPages - 1" @click="goToLast">Last ⏭</button>
    </div>
  </div>
</template>

<script setup>
import axios from "axios";
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

const songs = ref([]);
const searchQuery = ref("");
const router = useRouter();

const page = ref(0);
const totalPages = ref(0);
const size = 5;

// 🔹 Fetch Songs mit Paging
const fetchAll = async () => {
  const res = await axios.get("http://localhost:8080/api/songs", {
    params: { page: page.value, size }
  });
  songs.value = res.data.content;
  totalPages.value = res.data.totalPages;
};

// 🔹 Suche ohne Paging
const searchSongs = async (q) => {
  const res = await axios.get("http://localhost:8080/api/songs/search", {
    params: { query: q },
  });
  songs.value = res.data;
  totalPages.value = 1;
  page.value = 0;
};

// 🔹 Trigger Suche oder Paging
const onSearch = async () => {
  const q = searchQuery.value.trim();
  if (!q) return fetchAll();
  await searchSongs(q);
};

const goToCreate = () => router.push({ name: "create" });
const editSong = (id) => router.push({ name: "edit", params: { id } });

const deleteSong = async (id) => {
  if (!confirm("Are you sure you want to delete this song?")) return;
  await axios.delete(`http://localhost:8080/api/songs/${id}`);
  songs.value = songs.value.filter((s) => s.id !== id);
};

// 🔹 Pagination-Methoden
const prevPage = async () => {
  if (page.value > 0) {
    page.value--;
    await fetchAll();
  }
};
const nextPage = async () => {
  if (page.value < totalPages.value - 1) {
    page.value++;
    await fetchAll();
  }
};
const goToFirst = async () => {
  page.value = 0;
  await fetchAll();
};
const goToLast = async () => {
  page.value = totalPages.value - 1;
  await fetchAll();
};

onMounted(fetchAll);
</script>

<style scoped>
.page { max-width: 900px; margin: 24px auto; padding: 0 16px; }
h1 { margin: 0 0 14px; }

.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.toolbar input { flex: 1 1 auto; padding: 10px 12px; border: 1px solid var(--border); border-radius: 6px; font-size: 0.95rem; }

.btn { padding: 8px 12px; border: none; border-radius: 6px; font-weight: 600; cursor: pointer; }
.btn.primary { background: var(--brand); color: #fff; }
.btn.edit { background: #4e9eff; color: #fff; }
.btn.delete { background: #e74c3c; color: #fff; }

.no-results { color: #777; margin: 8px 0 12px; font-style: italic; }

.list { display: flex; flex-direction: column; gap: 12px; }
.item { background: var(--card); border: 1px solid var(--border); border-radius: 10px; padding: 12px; display: flex; justify-content: space-between; gap: 12px; }
.title-row { display: flex; align-items: center; gap: 8px; }
.title { font-size: 1.05rem; }
.genre { font-size: .75rem; padding: 2px 8px; border-radius: 999px; border: 1px solid #d8efe5; color: #166b4c; background: #eef9f3; }
.sub { color: #333; }
.meta { color: #777; font-size: .9rem; }
.actions { display: flex; gap: 8px; align-items: center; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 10px; margin-top: 18px; }
.pagination span { font-weight: 600; color: #333; }
.pagination .btn:disabled { background: #ccc; cursor: not-allowed; }
</style>
