<template>
  <div class="wrap">
    <!-- ❌ Nicht eingeloggt: Nur Hinweis, KEINE Inputs/Buttons -->
    <div v-if="!isLoggedIn" class="card locked">
      <h1>Add New Song</h1>
      <p class="locked-msg">
        You must be logged in to create songs.
      </p>
    </div>

    <!-- ✅ Eingeloggt: Formular sichtbar -->
    <div v-else class="card">
      <h1>Add New Song</h1>

      <form @submit.prevent="createSong" class="form" novalidate>
        <!-- Title -->
        <label>Title</label>
        <input
            v-model.trim="song.title"
            :class="{ invalid: $v.song.title.$error }"
            placeholder="Enter song title"
            @blur="$v.song.title.$touch()"
        />
        <p v-if="$v.song.title.$error" class="err">Title is required and max 200 chars.</p>

        <!-- Artist Dropdown -->
        <Select
            label="Artist"
            v-model="artistId"
            :items="artistsWithCustom"
            value-key="id"
            display-key="name"
            placeholder="Select an artist"
            @change="onArtistChange"
        />
        <p v-if="showArtistError" class="err">Please select an artist.</p>

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

        <!-- Genre -->
        <label>Genre</label>
        <input
            v-model.trim="song.genre"
            :class="{ invalid: $v.song.genre.$error }"
            placeholder="Enter song genre"
            @blur="$v.song.genre.$touch()"
        />
        <p v-if="$v.song.genre.$error" class="err">Genre is required and max 80 chars.</p>

        <!-- Length -->
        <label>Length (seconds)</label>
        <input
            v-model.number="song.length"
            type="number"
            min="1"
            :class="{ invalid: $v.song.length.$error }"
            placeholder="Enter length"
            @blur="$v.song.length.$touch()"
        />
        <p v-if="$v.song.length.$error" class="err">Length must be at least 1.</p>

        <!-- File Upload -->
        <label>Upload Music</label>
        <input type="file" accept="audio/*" @change="onFileChange" />

        <!-- Audio Preview -->
        <audio
            v-if="song.musicData"
            controls
            preload="auto"
            :src="song.musicData"
        ></audio>

        <!-- Buttons -->
        <div class="actions">
          <button type="submit" class="btn primary" :disabled="submitting">Save Song</button>
          <button type="button" class="btn" @click="goBack">Cancel</button>
        </div>
      </form>

      <!-- Feedback -->
      <p v-if="successMessage" class="success">{{ successMessage }}</p>
      <p v-if="serverError" class="server-error">{{ serverError }}</p>
    </div>
  </div>
</template>

<script setup>
import api from "../services/api";
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import Select from "./ui/Select.vue";
import useVuelidate from "@vuelidate/core";
import { required, maxLength, minValue } from "@vuelidate/validators";
import { useAuth } from "../composables/useAuth";

const router = useRouter();
const { isLoggedIn } = useAuth();

const artists = ref([]);
const CUSTOM_ID = "__custom__";
const artistsWithCustom = computed(() => [
  ...artists.value,
  { id: CUSTOM_ID, name: "Custom…" }
]);

const artistId = ref("");
const isCustomSelected = computed(() => artistId.value === CUSTOM_ID);
const artistTouched = ref(false);
const submitAttempted = ref(false);

const customArtistName = ref("");
const customArtistError = ref("");
const customArtistOk = ref("");
const creatingArtist = ref(false);
const customArtistValid = computed(() => {
  const n = customArtistName.value.trim();
  return n.length >= 2 && n.length <= 200;
});

// Song: Version braucht es beim Create nicht, Server vergibt sie selbst
const song = ref({
  title: "",
  genre: "",
  length: null,
  musicData: "",
  version: null
});

const successMessage = ref("");
const serverError = ref("");
const submitting = ref(false);
let redirectTimer = null;

// Validation Rules
const rules = computed(() => ({
  song: {
    title: { required, maxLength: maxLength(200) },
    genre: { required, maxLength: maxLength(80) },
    length: { required, minValue: minValue(1) }
  }
}));
const $v = useVuelidate(rules, { song });

// Artist-Validierung: gültig nur, wenn eine echte ID (Zahl) gewählt ist
const validArtistSelected = computed(() => {
  const n = Number(artistId.value);
  return Number.isFinite(n);
});
const showArtistError = computed(
    () => (artistTouched.value || submitAttempted.value) && !validArtistSelected.value
);

// Lade Artists (nur wenn eingeloggt)
const loadArtists = async () => {
  const res = await api.get("/api/artists");
  artists.value = res.data;
};

// Musik-Upload → Base64 speichern
const onFileChange = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => (song.value.musicData = reader.result);
  reader.readAsDataURL(file);
};

const onArtistChange = () => {
  artistTouched.value = true;
  customArtistError.value = "";
  customArtistOk.value = "";
};

const createArtist = async () => {
  customArtistError.value = "";
  customArtistOk.value = "";
  const name = customArtistName.value.trim();
  if (!customArtistValid.value) {
    customArtistError.value = "Please enter a valid artist name (2–200 chars).";
    return;
  }
  creatingArtist.value = true;
  try {
    const res = await api.post("/api/artists", { name, description: "" });
    const newArtist = res.data;
    // Liste aktualisieren und Auswahl setzen
    artists.value = [...artists.value, newArtist];
    artistId.value = newArtist.id;
    customArtistName.value = "";
    customArtistOk.value = "Artist created and selected.";
  } catch (e) {
    const status = e?.response?.status;
    if (status === 409) {
      // existiert bereits → Liste neu laden & auswählen
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

const createSong = async () => {
  serverError.value = "";
  successMessage.value = "";
  submitAttempted.value = true;

  if (!isLoggedIn.value) {
    serverError.value = "Please login to create songs.";
    return;
  }

  await $v.value.$validate();

  // Artist muss echte ID sein
  if (!validArtistSelected.value) return;
  if ($v.value.$invalid) return;

  submitting.value = true;
  try {
    await api.post("/api/songs", {
      ...song.value,
      artist: { id: Number(artistId.value) }
    });

    successMessage.value = "Song successfully created! Redirecting…";

    // Formular & Validierung zurücksetzen
    song.value = { title: "", genre: "", length: null, musicData: "", version: null };
    artistId.value = "";
    customArtistName.value = "";
    customArtistError.value = "";
    customArtistOk.value = "";
    artistTouched.value = false;
    submitAttempted.value = false;
    $v.value.$reset();

    clearTimeout(redirectTimer);
    redirectTimer = setTimeout(() => {
      router.push({ name: "songs" });
    }, 900);
  } catch (e) {
    const status = e?.response?.status;
    if (status === 401) {
      serverError.value = "Please login to create songs.";
    } else if (status === 400) {
      serverError.value =
          e?.response?.data?.message ||
          Object.values(e?.response?.data || {})[0] ||
          "Failed to create song due to validation.";
    } else {
      serverError.value = "Failed to create song.";
    }
  } finally {
    submitting.value = false;
  }
};

const goBack = () => router.push({ name: "songs" });

onMounted(() => {
  if (isLoggedIn.value) loadArtists();
});
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
  border-radius: 16px;
  padding: 24px;
  width: 100%;
  max-width: 560px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
}
.locked { text-align: center; }
.locked-msg {
  color: #e67e22; background: #fff7ec; border: 1px solid #f6d3b4;
  border-radius: 10px; padding: 10px 12px; font-weight: 600;
}

h1 { margin: 0 0 12px; font-size: 1.4rem; }
.form { display: flex; flex-direction: column; gap: 14px; }
input, .inp {
  padding: 12px 14px; border: 1px solid var(--border); border-radius: 12px;
  font-size: 1rem; background: #f9fafb; transition: 0.2s;
}
input:focus, .inp:focus {
  outline: none; border-color: #42b983; background: #fff;
  box-shadow: 0 0 0 4px rgba(66, 185, 131, 0.15);
}
input.invalid { border-color: #e74c3c; box-shadow: 0 0 0 4px rgba(231, 76, 60, 0.15); }
.err { color: #e74c3c; margin: -6px 0 0; font-size: 0.9rem; }
.ok { color: #1f8f5f; margin: -6px 0 0; font-size: 0.95rem; font-weight: 700; }
.hint { color:#777; margin-top: 4px; font-size: .85rem; }

.custom-artist {
  border: 1px dashed var(--border);
  border-radius: 12px;
  padding: 12px;
  background: #fafafa;
}
.custom-label { font-weight: 600; margin-bottom: 6px; display: inline-block; }
.custom-row {
  display: grid; grid-template-columns: 1fr auto; gap: 8px; align-items: center;
}

.server-error {
  color: #e74c3c; margin-top: 10px; font-weight: 600; text-align: center;
}
.actions { display: flex; gap: 10px; margin-top: 6px; flex-wrap: wrap; }
.btn {
  padding: 10px 16px; border: 1px solid var(--border);
  border-radius: 10px; font-weight: 700; cursor: pointer; transition: all .2s;
  background: #fff;
}
.btn.primary { background: var(--brand); color: #fff; border-color: transparent; }
.btn.primary:disabled { opacity: .6; cursor: not-allowed; }
.success { color: var(--brand); margin-top: 10px; font-weight: 700; text-align: center; }
</style>
