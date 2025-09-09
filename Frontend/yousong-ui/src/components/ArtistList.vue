<template>
  <div class="page">
    <h1>Manage Artists</h1>

    <!-- CREATE -->
    <div class="card">
      <h2 class="h2">Add Artist</h2>
      <form @submit.prevent="onCreate" class="form" novalidate>
        <label class="lbl">Name</label>
        <input
            v-model.trim="createForm.name"
            :class="{ invalid: $v.createForm.name.$error }"
            placeholder="Artist name"
            @blur="$v.createForm.name.$touch()"
        />
        <p v-if="$v.createForm.name.$error" class="err">Name is required (max 200 chars).</p>

        <label class="lbl">Description (optional)</label>
        <input
            v-model.trim="createForm.description"
            :class="{ invalid: $v.createForm.description.$error }"
            placeholder="Short description"
            @blur="$v.createForm.description.$touch()"
        />
        <p v-if="$v.createForm.description.$error" class="err">Max 500 characters.</p>

        <div class="row">
          <button class="btn primary" type="submit" :disabled="creating">Add</button>
          <button class="btn" type="button" @click="resetCreate" :disabled="creating">Reset</button>
        </div>

        <p v-if="createOk" class="ok">{{ createOk }}</p>
        <p v-if="createErr" class="err">{{ createErr }}</p>
      </form>
    </div>

    <!-- LIST + UPDATE/DELETE -->
    <div class="card">
      <h2 class="h2">Artists</h2>
      <div v-if="loading" class="hint">Loading…</div>
      <div v-else-if="artists.length === 0" class="hint">No artists yet.</div>

      <ul class="list" v-else>
        <li v-for="a in artists" :key="a.id" class="item">
          <template v-if="editingId === a.id">
            <div class="cols">
              <input
                  v-model.trim="editForm.name"
                  class="inp"
                  :maxlength="200"
                  placeholder="Name"
              />
              <input
                  v-model.trim="editForm.description"
                  class="inp"
                  :maxlength="500"
                  placeholder="Description (optional)"
              />
            </div>
            <div class="actions">
              <button class="btn primary" @click="onSave(a.id)" :disabled="saving">Save</button>
              <button class="btn" @click="cancelEdit" :disabled="saving">Cancel</button>
            </div>
            <p v-if="rowMsg[a.id]?.err" class="err">{{ rowMsg[a.id].err }}</p>
            <p v-if="rowMsg[a.id]?.ok" class="ok">{{ rowMsg[a.id].ok }}</p>
          </template>

          <template v-else>
            <div class="cols">
              <div class="name" :title="a.name">{{ a.name }}</div>
              <div class="desc" :title="a.description">{{ a.description || "—" }}</div>
            </div>
            <div class="actions">
              <button class="btn" @click="startEdit(a)">Edit</button>
              <button class="btn danger" @click="onDelete(a.id)">Delete</button>
            </div>
            <p v-if="rowMsg[a.id]?.err" class="err">{{ rowMsg[a.id].err }}</p>
            <p v-if="rowMsg[a.id]?.ok" class="ok">{{ rowMsg[a.id].ok }}</p>
          </template>
        </li>
      </ul>

      <p v-if="serverError" class="err">{{ serverError }}</p>
    </div>
  </div>
</template>

<script setup>
import api from "../services/api";
import { ref, onMounted } from "vue";
import useVuelidate from "@vuelidate/core";
import { required, maxLength } from "@vuelidate/validators";

const artists = ref([]);
const loading = ref(true);
const serverError = ref("");

const createForm = ref({ name: "", description: "" });
const creating = ref(false);
const createErr = ref("");
const createOk = ref("");

const rules = {
  createForm: {
    name: { required, maxLength: maxLength(200) },
    description: { maxLength: maxLength(500) }
  }
};
const $v = useVuelidate(rules, { createForm });

function resetCreate() {
  createForm.value = { name: "", description: "" };
  createErr.value = ""; createOk.value = "";
  $v.value.$reset();
}

async function onCreate() {
  createErr.value = ""; createOk.value = "";
  await $v.value.$validate();
  if ($v.value.$invalid) return;

  creating.value = true;
  try {
    const res = await api.post("/api/artists", {
      name: createForm.value.name.trim(),
      description: createForm.value.description.trim()
    });
    artists.value = [...artists.value, res.data].sort((a,b)=>a.name.localeCompare(b.name));
    resetCreate();
    createOk.value = "Artist created.";
  } catch (e) {
    const s = e?.response?.status;
    if (s === 409) createErr.value = "Artist with this name already exists.";
    else if (s === 400) createErr.value = "Validation failed.";
    else createErr.value = "Failed to create artist.";
  } finally {
    creating.value = false;
  }
}

async function loadArtists() {
  serverError.value = "";
  try {
    const res = await api.get("/api/artists");
    artists.value = Array.isArray(res.data)
        ? [...res.data].sort((a,b)=>a.name.localeCompare(b.name))
        : [];
  } catch {
    serverError.value = "Could not load artists.";
  } finally {
    loading.value = false;
  }
}

const editingId = ref(null);
const editForm = ref({ id: null, name: "", description: "" });
const saving = ref(false);
const rowMsg = ref({}); // { [id]: {ok?, err?} }

function startEdit(a) {
  rowMsg.value[a.id] = {};
  editingId.value = a.id;
  editForm.value = { id: a.id, name: a.name ?? "", description: a.description ?? "" };
}
function cancelEdit() {
  editingId.value = null;
  editForm.value = { id: null, name: "", description: "" };
}

async function onSave(id) {
  rowMsg.value[id] = { ok: "", err: "" };
  const name = (editForm.value.name || "").trim();
  const description = (editForm.value.description || "").trim();
  if (!name) { rowMsg.value[id].err = "Name is required."; return; }
  if (name.length > 200) { rowMsg.value[id].err = "Max 200 characters for name."; return; }
  if (description.length > 500) { rowMsg.value[id].err = "Max 500 characters for description."; return; }

  saving.value = true;
  try {
    const res = await api.put(`/api/artists/${id}`, { name, description });
    const idx = artists.value.findIndex(a => a.id === id);
    if (idx !== -1) artists.value[idx] = res.data;
    artists.value = [...artists.value].sort((a,b)=>a.name.localeCompare(b.name));
    rowMsg.value[id].ok = "Changes saved.";
    cancelEdit();
  } catch (e) {
    const s = e?.response?.status;
    if (s === 404) rowMsg.value[id].err = "Artist not found.";
    else if (s === 400) rowMsg.value[id].err = "Validation failed.";
    else if (s === 409 || s === 500) rowMsg.value[id].err = "Name already exists.";
    else rowMsg.value[id].err = "Failed to save changes.";
  } finally {
    saving.value = false;
  }
}

async function onDelete(id) {
  rowMsg.value[id] = { ok: "", err: "" };
  if (!confirm("Delete this artist?")) return;

  try {
    await api.delete(`/api/artists/${id}`);
    artists.value = artists.value.filter(a => a.id !== id);
    rowMsg.value[id].ok = "Artist deleted.";
  } catch (e) {
    const s = e?.response?.status;
    if (s === 404) rowMsg.value[id].err = "Artist not found.";
    else if (s === 409 || s === 500) rowMsg.value[id].err = "Artist cannot be deleted because it is referenced by songs.";
    else rowMsg.value[id].err = "Failed to delete artist.";
  }
}

onMounted(loadArtists);
</script>

<style scoped>
.page { max-width: 960px; margin: 24px auto; padding: 0 16px; }
h1 { margin: 0 0 16px; }
.h2 { margin: 0 0 10px; font-size: 1.1rem; }

.card {
  background: #fff; border: 1px solid var(--border); border-radius: 16px;
  padding: 16px; margin-bottom: 14px; box-shadow: 0 6px 16px rgba(0,0,0,.05);
}

.form { display: grid; gap: 10px; }
.lbl { font-weight: 600; color: #222; }
input, .inp {
  padding: 12px 14px; border: 1px solid var(--border); border-radius: 12px;
  background: #f9fafb; font-size: 1rem; transition: .2s;
}
input:focus, .inp:focus {
  outline: none; border-color: var(--brand); background: #fff;
  box-shadow: 0 0 0 4px rgba(66,185,131,.15);
}
input.invalid { border-color: #e74c3c; box-shadow: 0 0 0 4px rgba(231,76,60,.15); }

.row { display: flex; gap: 8px; flex-wrap: wrap; }
.btn {
  padding: 10px 14px; border: 1px solid var(--border); border-radius: 10px;
  font-weight: 700; cursor: pointer; background: #fff; transition: .15s;
}
.btn.primary { background: var(--brand); color: #fff; border-color: transparent; }
.btn.danger { color: #e74c3c; }
.ok { color: #1f8f5f; font-weight: 700; }
.err { color: #e74c3c; font-weight: 600; }
.hint { color:#666; }

.list { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.item {
  border: 1px solid var(--border); border-radius: 12px; padding: 12px;
  display: grid; grid-template-columns: 1fr auto; gap: 10px; align-items: center;
  background: #f8fafc;
}
.cols { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; min-width: 0; }
.name { font-weight: 800; color: #111; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.desc { color:#555; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.actions { display: flex; gap: 8px; justify-content: flex-end; }
</style>
