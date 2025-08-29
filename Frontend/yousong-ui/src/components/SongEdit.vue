<template>
  <div class="form-container">
    <div class="form-card">
      <h1 class="form-title">Edit Song</h1>

      <form @submit.prevent="updateSong" class="form">
        <div class="form-group">
          <label for="title">Title</label>
          <input
              id="title"
              v-model="song.title"
              required
              placeholder="Enter song title"
          />
        </div>

        <div class="form-group">
          <label for="artist">Artist</label>
          <input
              id="artist"
              v-model="song.artist"
              required
              placeholder="Enter artist name"
          />
        </div>

        <div class="form-group">
          <label for="genre">Genre</label>
          <input
              id="genre"
              v-model="song.genre"
              required
              placeholder="Enter song genre"
          />
        </div>

        <div class="form-group">
          <label for="length">Length (seconds)</label>
          <input
              id="length"
              v-model.number="song.length"
              type="number"
              required
              placeholder="Enter length"
          />
        </div>

        <div class="button-group">
          <button type="submit" class="btn primary">Save Changes</button>
          <button type="button" class="btn secondary" @click="cancelEdit">
            Cancel
          </button>
        </div>
      </form>

      <p v-if="successMessage" class="success-msg">{{ successMessage }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import axios from "axios";

const route = useRoute();
const router = useRouter();
const song = ref({
  title: "",
  artist: "",
  genre: "",
  length: ""
});
const successMessage = ref("");

// Song-Daten laden, um Formular vorzufüllen
const fetchSong = async () => {
  try {
    const response = await axios.get(
        `http://localhost:8080/api/songs/${route.params.id}`
    );
    song.value = response.data;
  } catch (err) {
    console.error(err);
  }
};

// Song speichern
const updateSong = async () => {
  try {
    await axios.put(
        `http://localhost:8080/api/songs/${route.params.id}`,
        song.value
    );
    successMessage.value = "Song updated successfully!";
    setTimeout(() => {
      router.push("/");
    }, 1500);
  } catch (err) {
    console.error(err);
  }
};

const cancelEdit = () => {
  router.push("/");
};

onMounted(fetchSong);
</script>

<style scoped>
/* Container */
.form-container {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

/* Card */
.form-card {
  background-color: #ffffff;
  padding: 2rem;
  width: 100%;
  max-width: 450px;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

/* Titel */
.form-title {
  font-size: 1.6rem;
  margin-bottom: 1.5rem;
  text-align: center;
  color: #222;
}

/* Formular */
.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* Form-Gruppen */
.form-group {
  display: flex;
  flex-direction: column;
}

/* Labels */
label {
  font-weight: 600;
  margin-bottom: 0.4rem;
  color: #333;
}

/* Inputs */
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

/* Buttons */
.button-group {
  display: flex;
  justify-content: space-between;
  margin-top: 1rem;
}

.btn {
  padding: 0.8rem 1.2rem;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.2s ease-in-out;
}

/* Primärer Button */
.btn.primary {
  background-color: #42b983;
  color: white;
}

.btn.primary:hover {
  background-color: #369d6f;
}

/* Sekundärer Button */
.btn.secondary {
  background-color: #f3f4f6;
  color: #333;
}

.btn.secondary:hover {
  background-color: #e5e7eb;
}

/* Erfolgsmeldung */
.success-msg {
  margin-top: 1rem;
  color: #42b983;
  text-align: center;
  font-weight: bold;
}
</style>
