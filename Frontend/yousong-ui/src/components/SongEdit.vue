<template>
  <div class="wrap">
    <div class="card">
      <h1>Edit Song</h1>

      <form @submit.prevent="updateSong" class="form" novalidate>
        <label for="title">Title</label>
        <input id="title" v-model.trim="song.title" :class="{ invalid: $v.song.title.$error }" placeholder="Enter song title" />
        <p v-if="$v.song.title.$error" class="err">Title is required and max 200 chars.</p>

        <Select
            label="Artist"
            v-model="artistId"
            :items="artists"
            value-key="id"
            display-key="name"
            placeholder="Select an artist"
            search-placeholder="Search artists..."
        />
        <p v-if="artistError" class="err">Please select an artist.</p>

        <label for="genre">Genre</label>
        <input id="genre" v-model.trim="song.genre" :class="{ invalid: $v.song.genre.$error }" placeholder="Enter song genre" />
        <p v-if="$v.song.genre.$error" class="err">Genre max 80 chars.</p>

        <label for="length">Length (seconds)</label>
        <input
            id="length"
            v-model.number="song.length"
            type="number"
            min="1"
            :class="{ invalid: $v.song.length.$error }"
            placeholder="Enter length"
        />
        <p v-if="$v.song.length.$error" class="err">Length must be at least 1.</p>

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
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import Select from "./ui/Select.vue";
import useVuelidate from "@vuelidate/core";
import { required, maxLength, minValue } from "@vuelidate/validators";

const route = useRoute();
const router = useRouter();

const artists = ref([]);
const artistId = ref("");
const song = ref({ title: "", genre: "", length: null });
const successMessage = ref("");
const serverError = ref("");
const submitting = ref(false);

const rules = computed(() => ({
  song: {
    title: { required, maxLength: maxLength(200) },
    genre: { required, maxLength: maxLength(80) },
    length: { required, minValue: minValue(1) }
  }
}));
const $v = useVuelidate(rules, { song });
const artistError = computed(() => !artistId.value);

const loadArtists = async () => {
  const res = await axios.get("http://localhost:8080/api/artists");
  artists.value = res.data;
};

const loadSong = async () => {
  const res = await axios.get(`http://localhost:8080/api/songs/${route.params.id}`);
  const data = res.data;
  song.value = {
    title: data.title ?? "",
    genre: data.genre ?? "",
    length: data.length ?? null
  };
  // Artist im Dropdown vorauswählen (als Zahl!)
  artistId.value = data.artist?.id ? Number(data.artist.id) : "";
};

const updateSong = async () => {
  serverError.value = "";
  await $v.value.$validate();
  if ($v.value.$invalid || artistError.value) return;

  submitting.value = true;
  try {
    await axios.put(`http://localhost:8080/api/songs/${route.params.id}`, {
      title: song.value.title,
      genre: song.value.genre,
      length: song.value.length,
      artist: { id: Number(artistId.value) }
    });
    successMessage.value = "Song updated successfully!";
    setTimeout(() => router.push({ name: "songs" }), 1000);
  } catch (e) {
    serverError.value =
        e?.response?.data?.message ||
        Object.values(e?.response?.data?.fieldErrors || {})[0] ||
        "Failed to update song.";
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
