<template>
  <div class="page">
    <h1>All Songs</h1>

    <div v-if="songs.length === 0" class="empty">
      No songs can be found. Please adjust your search.
    </div>

    <div class="vlist">
      <SongItem
          v-for="song in songs"
          :key="song.id"
          :song="song"
          :is-logged-in="isLoggedIn"
          :current-playing-id="currentPlayingId"
          @edit="editSong"
          @delete="deleteSong"
          @playing="onItemPlaying"
      />
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

// Neue Child-Komponente
import SongItem from "../components/SongItem.vue";

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

// Single-Play-Steuerung (wird an Items durchgereicht)
const currentPlayingId = ref(null);
const onItemPlaying = (id) => { currentPlayingId.value = id; };

const fetchCatalog = async () => {
  serverError.value = "";
  try {
    const params = { page: page.value, size };
    if (activeQuery.value.length >= 2) params.q = activeQuery.value;
    if (selectedGenres.value.length) params.genresCsv = selectedGenres.value.join(",");
    const res = await api.get("/api/songs/catalog", { params });
    songs.value = res.data.content || [];
    totalPages.value = res.data.totalPages ?? 0;
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
watch(activeQuery, () => {
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
/* Neu: Seiten-Container mit Randabstand links/rechts */
.page {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;           /* zentriert und Abstand zu Browserrändern */
  padding: 12px 20px 24px;  /* zusätzlicher Innenabstand */
}

h1 { margin: 0 0 14px; }

.empty { color:#777; margin: 8px 0 12px; font-style: italic; }

.vlist { display: flex; flex-direction: column; gap: 12px; }

/* Pagination & Fehlermeldung bleiben wie gehabt */
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
  .page { padding: 10px 14px 20px; }
}
</style>
