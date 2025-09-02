<template>
  <div class="wrap">
    <div class="card">
      <h1>Add New Song</h1>
      <form @submit.prevent="createSong" class="form">
        <label>Title</label>
        <input v-model="song.title" required placeholder="Enter song title" />

        <label>Artist</label>
        <input v-model="song.artist" required placeholder="Enter artist name" />

        <label>Genre</label>
        <input v-model="song.genre" required placeholder="Enter song genre" />

        <label>Length (seconds)</label>
        <input v-model.number="song.length" type="number" required placeholder="Enter length" />

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
import { ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const song = ref({ title: "", artist: "", genre: "", length: "" });
const successMessage = ref("");

const createSong = async () => {
  await axios.post("http://localhost:8080/api/songs", song.value);
  successMessage.value = "Song successfully created!";
  song.value = { title: "", artist: "", genre: "", length: "" };
  setTimeout(() => router.push({ name: "songs" }), 1200);
};

const goBack = () => router.push({ name: "songs" });
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
