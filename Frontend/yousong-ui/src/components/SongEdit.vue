<template>
  <div class="wrap">
    <div class="card">
      <h1>Edit Song</h1>

      <div v-if="loading" class="loading">Loading…</div>

      <template v-else>
        <form v-if="!serverError" @submit.prevent="updateSong" class="form" novalidate>
          <label>Title</label>
          <input v-model.trim="song.title" placeholder="Enter song title" />

          <Select
              label="Artist"
              v-model="artistId"
              :items="artistsWithCustom"
              value-key="id"
              display-key="name"
              placeholder="Select an artist"
              @change="onArtistChange"
          />

          <!-- Custom Artist Box -->
          <div v-if="isCustomSelected" class="custom-artist">
            <label class="custom-label">New artist name</label>
            <div class="custom-row">
              <input
                  v-model.trim="customArtistName"
                  :maxlength="200"
                  class="inp"
                  placeholder="Type artist name…"
                  @keyup.enter.prevent="createArtist"
              />
              <button
                  class="btn"
                  type="button"
                  @click="createArtist"
                  :disabled="creatingArtist || !customArtistValid"
                  title="Add artist"
              >
                <span v-if="creatingArtist">Adding…</span>
                <span v-else>Add</span>
              </button>
            </div>
            <p v-if="customArtistError" class="err">{{ customArtistError }}</p>
            <p v-if="customArtistOk" class="ok">{{ customArtistOk }}</p>
            <p class="hint">Max 200 characters. Duplicates will be detected.</p>
          </div>

          <label>Genre</label>
          <input v-model.trim="song.genre" placeholder="Enter song genre" />

          <label>Length (seconds)</label>
          <input v-model.number="song.length" type="number" min="1" placeholder="Enter length" />

          <label>Upload Music</label>
          <input type="file" accept="audio/*" @change="onFileChange" />

          <audio v-if="song.musicData" controls :src="song.musicData"></audio>
          <audio v-else-if="song.id" controls :src="`/api/songs/${song.id}/music`"></audio>

          <div class="actions">
            <button type="submit" class="btn primary" :disabled="submitting">Save Changes</button>
            <button type="button" class="btn" @click="goBack">Cancel</button>
          </div>

          <p v-if="successMessage" class="success">{{ successMessage }}</p>
        </form>

        <div v-else class="error-box">
          <p class="server-error">{{ serverError }}</p>
          <details v-if="debugJson"><summary>Details</summary><pre>{{ debugJson }}</pre></details>
          <button class="btn" @click="goBack">Back to list</button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import api from "../services/api";
import Select from "./ui/Select.vue";
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const artists = ref([]);
const CUSTOM_ID = "__custom__";
const artistsWithCustom = computed(() => [
  ...artists.value,
  { id: CUSTOM_ID, name: "Custom…" }
]);

const artistId = ref("");
const isCustomSelected = computed(() => artistId.value === CUSTOM_ID);

const customArtistName = ref("");
const customArtistError = ref("");
const customArtistOk = ref("");
const creatingArtist = ref(false);
const customArtistValid = computed(() => {
  const n = customArtistName.value.trim();
  return n.length >= 2 && n.length <= 200;
});

const song = ref({
  id: null,
  title: "",
  genre: "",
  length: null,
  musicData: "",
  version: 0,
  artist: null
});
const successMessage = ref("");
const serverError = ref("");
const debugJson = ref("");
const submitting = ref(false);

function normalizeSong(data) {
  const lenRaw = data?.length;
  const len = typeof lenRaw === "number" ? lenRaw : Number.parseInt(String(lenRaw ?? ""), 10);
  return {
    id: data?.id ?? null,
    title: data?.title ?? "",
    genre: data?.genre ?? "",
    length: Number.isFinite(len) ? len : null,
    musicData: data?.musicData ?? "",
    version: data?.version ?? 0,
    artist: data?.artist ? { id: data.artist.id, name: data.artist.name } : null
  };
}

const getNumericId = () => {
  const raw = route.params.id;
  const n = Number(raw);
  return Number.isFinite(n) ? n : NaN;
};

const loadArtists = async () => {
  const res = await api.get("/api/artists");
  artists.value = Array.isArray(res.data) ? res.data.map(a => ({ ...a })) : [];
};

const loadSong = async () => {
  serverError.value = ""; debugJson.value = "";
  const id = getNumericId();
  if (Number.isNaN(id)) {
    serverError.value = "Invalid song id in route.";
    debugJson.value = JSON.stringify({ routeParam: route.params.id }, null, 2);
    return;
  }
  try {
    const res = await api.get(`/api/songs/${id}`);
    song.value = normalizeSong(res.data);
    artistId.value = song.value.artist?.id ? Number(song.value.artist.id) : "";
  } catch (e) {
    const status = e?.response?.status;
    const data = e?.response?.data;
    debugJson.value = JSON.stringify({ status, data, message: e?.message, urlTried: `/api/songs/${id}` }, null, 2);

    if (status === 404) serverError.value = "Song not found (404). It may have been deleted.";
    else if (status === 401) serverError.value = "Please login to load this song (401).";
    else if (status === 403) serverError.value = "Access to this song is forbidden (403).";
    else if (!e.response) serverError.value = "Backend is not reachable. Is the server running?";
    else serverError.value = "Could not load song.";
  }
};

const onFileChange = (e) => {
  const file = e.target.files[0]; if (!file) return;
  const reader = new FileReader();
  reader.onload = () => (song.value.musicData = reader.result);
  reader.readAsDataURL(file);
};

const onArtistChange = () => {
  customArtistError.value = "";
  customArtistOk.value = "";
};

const createArtist = async () => {
  customArtistError.value = "";
  customArtistOk.value = "";
  const name = customArtistName.value.trim();
  if (!(name.length >= 2 && name.length <= 200)) {
    customArtistError.value = "Please enter a valid artist name (2–200 chars).";
    return;
  }
  creatingArtist.value = true;
  try {
    const res = await api.post("/api/artists", { name, description: "" });
    const newArtist = res.data;
    artists.value = [...artists.value, newArtist];
    artistId.value = newArtist.id;
    customArtistName.value = "";
    customArtistOk.value = "Artist created and selected.";
  } catch (e) {
    const status = e?.response?.status;
    if (status === 409) {
      await loadArtists();
      const found = artists.value.find(a => a.name?.toLowerCase() === name.toLowerCase());
      if (found) {
        artistId.value = found.id;
        customArtistOk.value = "Artist already exists — selected it.";
      } else {
        customArtistError.value = "Artist already exists.";
      }
    } else if (status === 400) {
      customArtistError.value = e?.response?.data?.message || "Validation failed.";
    } else if (status === 401) {
      customArtistError.value = "Please login to create artists.";
    } else {
      customArtistError.value = "Failed to create artist.";
    }
  } finally {
    creatingArtist.value = false;
  }
};

const updateSong = async () => {
  serverError.value = ""; debugJson.value = ""; submitting.value = true;
  try {
    // Nur echte ID akzeptieren (nicht "__custom__")
    const numericArtistId = Number(artistId.value);
    if (!Number.isFinite(numericArtistId)) {
      serverError.value = "Please select or create a valid artist.";
      submitting.value = false;
      return;
    }

    await api.put(`/api/songs/${song.value.id}`, {
      ...song.value,
      artist: { id: numericArtistId }
    });
    successMessage.value = "Song updated successfully!";
    setTimeout(() => router.push({ name: "songs" }), 900);
  } catch (e) {
    const status = e?.response?.status; const data = e?.response?.data;
    debugJson.value = JSON.stringify({ status, data, message: e?.message }, null, 2);

    if (status === 401) serverError.value = "Please login to update songs.";
    else if (status === 403) serverError.value = "You can only edit your own songs.";
    else if (status === 409) serverError.value = "⚠️ Song wurde bereits von jemand anderem geändert. Bitte Seite neu laden!";
    else if (status === 400) serverError.value = data?.message || "Update failed due to validation. Please check your inputs.";
    else if (!e.response) serverError.value = "Backend is not reachable. Is the server running?";
    else serverError.value = "Failed to update song.";
  } finally {
    submitting.value = false;
  }
};

const goBack = () => router.push({ name: "songs" });

onMounted(async () => {
  try {
    // WICHTIG: zuerst Artists, dann Song (damit der Select korrekt vorbelegt)
    await loadArtists();
    await loadSong();
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.wrap{ display:flex; justify-content:center; padding:24px 16px; }
.card{
  background:#fff; border:1px solid var(--border); border-radius:16px;
  padding:24px; width:100%; max-width:560px; box-shadow:0 6px 16px rgba(0,0,0,.05);
}
h1{ margin:0 0 12px; font-size:1.4rem; }
.loading{ color:#666; }
.form{ display:flex; flex-direction:column; gap:14px; }
input, .inp{
  padding:12px 14px; border:1px solid var(--border); border-radius:12px; font-size:1rem; background:#f9fafb; transition:.2s;
}
input:focus, .inp:focus{ outline:none; border-color:#42b983; background:#fff; box-shadow:0 0 0 4px rgba(66,185,131,.15); }

.custom-artist {
  border: 1px dashed var(--border);
  border-radius: 12px;
  padding: 12px;
  background: #fafafa;
}
.custom-label { font-weight: 600; margin-bottom: 6px; display: inline-block; }
.custom-row { display:grid; grid-template-columns: 1fr auto; gap:8px; align-items:center; }
.err{ color:#e74c3c; margin:6px 0 0; font-size:.9rem; }
.ok{ color:#1f8f5f; margin:6px 0 0; font-size:.95rem; font-weight:700; }
.hint{ color:#777; margin-top:4px; font-size:.85rem; }

.server-error{ color:#e74c3c; margin:10px 0; font-weight:600; }
.error-box{ display:flex; flex-direction:column; gap:10px; align-items:flex-start; }
details summary{ cursor:pointer; margin-bottom:6px; }
pre{ background:#f6f8fa; padding:10px; border-radius:8px; max-width:100%; overflow:auto; }
.actions{ display:flex; gap:10px; margin-top:6px; flex-wrap:wrap; }
.btn{
  padding:10px 16px; border:1px solid var(--border); border-radius:10px; font-weight:700; cursor:pointer; transition:all .2s; background:#fff;
}
.btn.primary{ background:var(--brand); color:#fff; border-color: transparent; }
.success{ color:#42b983; margin-top:10px; font-weight:700; text-align:left; }
</style>
