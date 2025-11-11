<template>
  <div class="wrap">
    <div v-if="!isLoggedIn" class="card locked">
      <h1>Add New Song</h1>
      <p class="locked-msg">You must be logged in to create songs.</p>
    </div>

    <div v-else class="card">
      <h1>Add New Song</h1>

      <form @submit.prevent="createSong" class="form" novalidate>
        <label>Title</label>
        <input
            v-model.trim="song.title"
            :class="{ invalid: $v.song.title.$error }"
            placeholder="Enter song title"
            @blur="$v.song.title.$touch()"
        />
        <p v-if="$v.song.title.$error" class="err">Title is required and max 200 chars.</p>

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

        <label>Genres</label>
        <div class="chips-input" :class="{ invalid: $v.song.genres.$error }">
          <div class="chips">
            <span v-for="g in song.genres" :key="g" class="chip">
              {{ g }}
              <button type="button" class="x" @click="removeGenre(g)" aria-label="Remove">×</button>
            </span>
            <input
                ref="genreRef"
                v-model.trim="genreDraft"
                list="genre-suggestions"
                class="chip-input"
                placeholder="Type & press Enter…"
                @keydown.enter.prevent="commitGenre()"
                @keydown.tab.prevent="commitGenre(true)"
                @keydown="onGenreKeydown"
                @blur="commitGenre()"
            />
          </div>
          <datalist id="genre-suggestions">
            <option v-for="g in DEFAULT_GENRES" :key="g" :value="g" />
          </datalist>
        </div>
        <p v-if="$v.song.genres.$error" class="err">Please add at least one genre.</p>

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

        <label>Upload Music</label>
        <input type="file" accept="audio/*" @change="onFileChange" />

        <audio v-if="song.musicData" controls preload="auto" :src="song.musicData"></audio>

        <div class="actions">
          <button type="submit" class="btn primary" :disabled="submitting">Save Song</button>
          <button type="button" class="btn" @click="goBack">Cancel</button>
        </div>
      </form>

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

const DEFAULT_GENRES = [
  "Pop","Rock","Hip Hop","R&B","Electronic","Classical","Jazz","Country",
  "Metal","Reggae","Folk","Blues","Indie","K-Pop","Soundtrack","Acoustic"
];

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

// Wichtig: version/id NICHT im Create-Body mitschicken
const song = ref({
  title: "",
  genres: [],
  length: null,
  musicData: ""
});

const genreDraft = ref("");
const genreRef = ref(null);

function commitGenre(allowEmpty = false) {
  const raw = genreDraft.value.trim();
  if (!raw && !allowEmpty) return;
  if (raw) {
    const val = raw.slice(0, 80);
    if (!song.value.genres.includes(val)) song.value.genres.push(val);
  }
  genreDraft.value = "";
}
function onGenreKeydown(e) {
  if (e.isComposing) return; // IME-Schutz
  if (e.key === "," || e.code === "Comma") {
    e.preventDefault();
    commitGenre();
  }
}
function removeGenre(g) {
  song.value.genres = song.value.genres.filter(x => x !== g);
}

const successMessage = ref("");
const serverError = ref("");
const submitting = ref(false);
let redirectTimer = null;

const rules = computed(() => ({
  song: {
    title: { required, maxLength: maxLength(200) },
    genres: { required },
    length: { required, minValue: minValue(1) }
  }
}));
const $v = useVuelidate(rules, { song });

const validArtistSelected = computed(() => Number.isFinite(Number(artistId.value)));
const showArtistError = computed(
    () => (artistTouched.value || submitAttempted.value) && !validArtistSelected.value
);

const loadArtists = async () => {
  const res = await api.get("/api/artists");
  artists.value = res.data;
};

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

const createSong = async () => {
  serverError.value = "";
  successMessage.value = "";
  submitAttempted.value = true;

  if (!isLoggedIn.value) {
    serverError.value = "Please login to create songs.";
    return;
  }

  await $v.value.$validate();

  if (!validArtistSelected.value) return;
  if ($v.value.$invalid) return;

  submitting.value = true;
  try {
    // Expliziter Payload OHNE id/version
    const payload = {
      title: song.value.title,
      genres: [...song.value.genres],
      length: song.value.length,
      ...(song.value.musicData ? { musicData: song.value.musicData } : {}),
      artist: { id: Number(artistId.value) }
    };

    await api.post("/api/songs", payload);

    successMessage.value = "Song successfully created! Redirecting…";

    song.value = { title: "", genres: [], length: null, musicData: "" };
    artistId.value = "";
    customArtistName.value = "";
    customArtistError.value = "";
    customArtistOk.value = "";
    artistTouched.value = false;
    submitAttempted.value = false;
    $v.value.$reset();
    genreDraft.value = "";

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

.chips-input { border: 1px solid var(--border); border-radius: 12px; padding: 8px; background:#f9fafb; }
.chips-input.invalid { border-color: #e74c3c; box-shadow: 0 0 0 4px rgba(231,76,60,.15); }
.chips { display:flex; flex-wrap:wrap; gap:6px; }
.chip {
  display:inline-flex; align-items:center; gap:6px;
  padding: 6px 10px; background:#eef9f3; color:#166b4c; border:1px solid #d8efe5; border-radius:999px; font-weight:700; font-size:.9rem;
}
.chip .x { background:transparent; border:none; cursor:pointer; font-weight:900; color:#0a5b3c; }
.chip-input {
  min-width: 140px; flex:1 1 160px; border:none; background:transparent; outline:none; padding:6px 8px; font-size:1rem;
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
