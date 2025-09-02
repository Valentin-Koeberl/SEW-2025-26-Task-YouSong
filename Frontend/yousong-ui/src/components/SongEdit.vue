<template>
  <div class="wrap">
    <div class="card">
      <h1>Edit Song</h1>
      <form @submit.prevent="updateSong" class="form">
        <label>Title</label>
        <input v-model="song.title" required />

        <label>Artist</label>
        <input v-model="song.artist" required />

        <label>Genre</label>
        <input v-model="song.genre" required />

        <label>Length (seconds)</label>
        <input v-model.number="song.length" type="number" required />

        <div class="actions">
          <button type="submit" class="btn primary">Save Changes</button>
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
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();
const song = ref({ title: "", artist: "", genre: "", length: "" });
const successMessage = ref("");

const fetchSong = async () => {
  const res = await axios.get(`http://localhost:8080/api/songs/${route.params.id}`);
  song.value = res.data;
};

const updateSong = async () => {
  await axios.put(`http://localhost:8080/api/songs/${route.params.id}`, song.value);
  successMessage.value = "Song updated successfully!";
  setTimeout(() => router.push({ name: "songs" }), 1200);
};

const goBack = () => router.push({ name: "songs" });

onMounted(fetchSong);
</script>

<style scoped>
.wrap {
  display: flex;
  justify-content: center;
  padding: 24px 16px;
}

.card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
  width: 100%;
  max-width: 520px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

input {
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 1rem;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.btn {
  padding: 10px 14px;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}

.btn.primary {
  background: var(--brand);
  color: #fff;
}

.success {
  color: var(--brand);
  margin-top: 10px;
  font-weight: 700;
  text-align: center;
}
</style>
