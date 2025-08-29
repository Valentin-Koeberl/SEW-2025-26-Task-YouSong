<template>
  <div class="form-container">
    <div class="form-card">
      <h1 class="form-title">Add New Song</h1>
      <form @submit.prevent="createSong" class="form">
        <div class="form-group">
          <label for="title">Title</label>
          <input id="title" v-model="song.title" required placeholder="Enter song title" />
        </div>

        <div class="form-group">
          <label for="artist">Artist</label>
          <input id="artist" v-model="song.artist" required placeholder="Enter artist name" />
        </div>

        <div class="form-group">
          <label for="genre">Genre</label>
          <input id="genre" v-model="song.genre" required placeholder="Enter song genre" />
        </div>

        <div class="form-group">
          <label for="length">Length (seconds)</label>
          <input id="length" v-model.number="song.length" type="number" required placeholder="Enter length" />
        </div>

        <button type="submit" class="btn primary">Save Song</button>
      </form>

      <p v-if="successMessage" class="success-msg">{{ successMessage }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import axios from "axios";
import { useRouter } from "vue-router";

const router = useRouter();
const song = ref({
  title: "",
  artist: "",
  genre: "",
  length: ""
});
const successMessage = ref("");

const createSong = async () => {
  try {
    await axios.post("http://localhost:8080/api/songs", song.value);
    successMessage.value = "Song successfully created!";
    song.value = { title: "", artist: "", genre: "", length: "" };
    setTimeout(() => {
      router.push("/");
    }, 1500);
  } catch (err) {
    console.error(err);
  }
};
</script>

<style scoped>
.form-container {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

.form-card {
  background-color: #ffffff;
  padding: 2rem;
  width: 100%;
  max-width: 450px;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.form-title {
  font-size: 1.6rem;
  margin-bottom: 1.5rem;
  text-align: center;
  color: #222;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
}

label {
  font-weight: 600;
  margin-bottom: 0.4rem;
  color: #333;
}

input {
  padding: 0.7rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 1rem;
  transition: all 0.2s ease-in-out;
}

input:focus {
  outline: none;
  border-color: #42b983;
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.2);
}

.btn {
  padding: 0.8rem 1.2rem;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.2s ease-in-out;
}

.btn.primary {
  background-color: #42b983;
  color: white;
}

.btn.primary:hover {
  background-color: #369d6f;
}

.success-msg {
  margin-top: 1rem;
  color: #42b983;
  text-align: center;
  font-weight: bold;
}
</style>
