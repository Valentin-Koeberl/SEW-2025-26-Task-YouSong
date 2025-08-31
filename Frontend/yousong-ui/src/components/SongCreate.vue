<template>
  <div class="wrap">
    <div class="card">
      <h1>Add New Song</h1>
      <form @submit.prevent="createSong" class="form">
        <label>Title</label>
        <input v-model="song.title" required placeholder="Enter song title" />

        <Select
            label="Artist"
            v-model="song.artistId"
            :items="artists"
            value-key="id"
            display-key="name"
            placeholder="Select an artist"
            search-placeholder="Search artists..."
        />

        <label>Genre</label>
        <input v-model="song.genre" placeholder="Enter song genre" />

        <label>Length (seconds)</label>
        <input v-model.number="song.length" type="number" min="0" required placeholder="Enter length" />

        <div class="actions">
          <button type="submit" class="btn primary">Save Song</button>
          <button type="button" class="btn" @click="goBack">Cancel</button>
        </div>
      </form>

      <p v-if="successMessage" class="success">{{ successMessage }}</p>
    </div>
  </div>
</template>

<script setup>
import axios from "axios";
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import Select from "./ui/Select.vue";

const router = useRouter();
const artists = ref([]);
const song = ref({ title: "", artistId: "", genre: "", length: "" });
const successMessage = ref("");

const loadArtists = async () => {
  const res = await axios.get("http://localhost:8080/api/artists");
  artists.value = res.data;
};

const createSong = async () => {
  if (!song.value.artistId) {
    alert("Please select an artist.");
    return;
  }
  await axios.post("http://localhost:8080/api/songs", song.value);
  successMessage.value = "Song successfully created!";
  song.value = { title: "", artistId: "", genre: "", length: "" };
  setTimeout(() => router.push({ name: "songs" }), 1200);
};
const goBack = () => router.push({ name: "songs" });

onMounted(loadArtists);
</script>

<style scoped>
.wrap{ display:flex; justify-content:center; padding:24px 16px; }
.card{
  background:#fff; border:1px solid var(--border); border-radius:16px;
  padding:24px; width:100%; max-width:520px; box-shadow:0 6px 16px rgba(0,0,0,.05);
}
h1{ margin:0 0 12px; font-size:1.4rem; }
.form{ display:flex; flex-direction:column; gap:14px; }
input{
  padding:12px 14px; border:1px solid var(--border); border-radius:12px; font-size:1rem;
  transition:all .25s ease; background:#f9fafb;
}
input:focus{
  outline:none; border-color:#42b983; background:#fff; box-shadow:0 0 0 4px rgba(66,185,131,.15);
}
.actions{ display:flex; gap:10px; margin-top:6px; flex-wrap:wrap; }
.btn{ padding:10px 16px; border:none; border-radius:10px; font-weight:700; cursor:pointer; transition:all .2s; }
.btn.primary{ background:var(--brand); color:#fff; }
.btn.primary:hover{ background:#2ea673; }
.success{ color:var(--brand); margin-top:10px; font-weight:700; text-align:center; }
</style>
