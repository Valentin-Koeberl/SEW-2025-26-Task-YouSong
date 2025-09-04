<template>
  <div class="wrap">
    <div class="card">
      <h1>Edit Song</h1>

      <form @submit.prevent="updateSong" class="form" novalidate>
        <label>Title</label>
        <input v-model.trim="song.title" placeholder="Enter song title" />

        <Select
            label="Artist"
            v-model="artistId"
            :items="artists"
            value-key="id"
            display-key="name"
            placeholder="Select an artist"
        />

        <label>Genre</label>
        <input v-model.trim="song.genre" placeholder="Enter song genre" />

        <label>Length (seconds)</label>
        <input v-model.number="song.length" type="number" min="1" placeholder="Enter length" />

        <label>Upload Music</label>
        <input type="file" accept="audio/*" @change="onFileChange" />

        <audio v-if="song.musicData" controls :src="song.musicData"></audio>
        <audio v-else controls :src="`http://localhost:8080/api/songs/${route.params.id}/music`"></audio>

        <div class="actions">
          <button type="submit" class="btn primary" :disabled="submitting">Save Changes</button>
          <button type="button" class="btn" @click="goBack">Cancel</button>
        </div>
      </form>

      <p v-if="successMessage" class="success">{{ successMessage }}</p>
      <p v-if="serverError" class="server-error">{{ serverError }}</p>
    </div>
  </div>
</template>

<script setup>
import axios from "axios";
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import Select from "./ui/Select.vue";

const route = useRoute();
const router = useRouter();

const artists = ref([]);
const artistId = ref("");
const song = ref({ title: "", genre: "", length: null, musicData: "", version: null });
const successMessage = ref("");
const serverError = ref("");
const submitting = ref(false);

const loadArtists = async () => {
  const res = await axios.get("http://localhost:8080/api/artists");
  artists.value = res.data;
};

const loadSong = async () => {
  const res = await axios.get(`http://localhost:8080/api/songs/${route.params.id}`);
  song.value = res.data;
  artistId.value = res.data.artist?.id ? Number(res.data.artist.id) : "";
};

const onFileChange = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => song.value.musicData = reader.result;
  reader.readAsDataURL(file);
};

const updateSong = async () => {
  serverError.value = "";
  submitting.value = true;
  try {
    await axios.put(`http://localhost:8080/api/songs/${route.params.id}`, {
      ...song.value,
      artist: { id: Number(artistId.value) }
    });
    successMessage.value = "Song updated successfully!";
    setTimeout(() => router.push({ name: "songs" }), 1000);
  } catch (e) {
    if (e.response?.status === 409) {
      serverError.value = "⚠️ Song wurde bereits von jemand anderem geändert. Bitte Seite neu laden!";
    } else {
      serverError.value = "Failed to update song.";
    }
  } finally {
    submitting.value = false;
  }
};

const goBack = () => router.push({ name: "songs" });

onMounted(async () => {
  await Promise.all([loadArtists(), loadSong()]);
});
</script>


<style scoped>
.wrap{ display:flex; justify-content:center; padding:24px 16px; }
.card{
  background:#fff; border:1px solid var(--border); border-radius:16px;
  padding:24px; width:100%; max-width:520px; box-shadow:0 6px 16px rgba(0,0,0,.05);
}
h1{ margin:0 0 12px; font-size:1.4rem; }
.form{ display:flex; flex-direction:column; gap:14px; }
input{ padding:12px 14px; border:1px solid var(--border); border-radius:12px; font-size:1rem; background:#f9fafb; transition:.2s; }
input:focus{ outline:none; border-color:#42b983; background:#fff; box-shadow:0 0 0 4px rgba(66,185,131,.15); }
input.invalid{ border-color:#e74c3c; box-shadow:0 0 0 4px rgba(231,76,60,.15); }
.err{ color:#e74c3c; margin:-6px 0 0; font-size:.9rem; }
.server-error{ color:#e74c3c; margin-top:10px; font-weight:600; text-align:center; }
.actions{ display:flex; gap:10px; margin-top:6px; flex-wrap:wrap; }
.btn{ padding:10px 16px; border:none; border-radius:10px; font-weight:700; cursor:pointer; transition:all .2s; }
.btn.primary{ background:var(--brand); color:#fff; }
.btn.primary:disabled{ opacity:.6; cursor:not-allowed; }
.success{ color:var(--brand); margin-top:10px; font-weight:700; text-align:center; }
</style>
