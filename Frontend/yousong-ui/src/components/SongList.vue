<template>
  <div>
    <h1>All Songs</h1>
    <button @click="goToCreate" class="add-btn">➕ Add Song</button>

    <div v-for="song in songs" :key="song.id" class="song-card">
      <p><b>{{ song.title }}</b> by {{ song.artist }} ({{ song.genre }})</p>
      <p>{{ song.length }} sec</p>

      <!-- Edit & Delete Buttons -->
      <button @click="editSong(song.id)" class="edit-btn">✏️ Edit</button>
      <button @click="deleteSong(song.id)" class="delete-btn">🗑 Delete</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import axios from "axios";

const songs = ref([]);
const router = useRouter();

const fetchSongs = async () => {
  try {
    const response = await axios.get("http://localhost:8080/api/songs");
    songs.value = response.data;
  } catch (err) {
    console.error("Fehler beim Laden der Songs:", err);
  }
};

const goToCreate = () => {
  router.push("/create");
};

const editSong = (id) => {
  router.push(`/edit/${id}`);
};

const deleteSong = async (id) => {
  if (confirm("⚠️ Are you sure you want to delete this song?")) {
    try {
      await axios.delete(`http://localhost:8080/api/songs/${id}`);
      // Lokale Liste aktualisieren
      songs.value = songs.value.filter(song => song.id !== id);
    } catch (err) {
      console.error("Fehler beim Löschen des Songs:", err);
    }
  }
};

onMounted(fetchSongs);
</script>

<style scoped>
.edit-btn {
  background-color: #4e9eff;
  color: white;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  margin-right: 8px;
  cursor: pointer;
  transition: 0.2s;
}

.edit-btn:hover {
  background-color: #347fd6;
}

.delete-btn {
  background-color: #e74c3c;
  color: white;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
}

.delete-btn:hover {
  background-color: #c0392b;
}
</style>
