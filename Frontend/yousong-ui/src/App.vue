<template>
  <div>
    <header class="topbar">
      <div class="left">
        <span class="brand">YouSong</span>
        <router-link :to="{ name: 'songs' }" class="link">Songs</router-link>
        <router-link :to="{ name: 'create' }" class="link">Add</router-link>
      </div>

      <div class="middle"></div>

      <div class="right">
        <template v-if="isLoggedIn">
          <span class="user">Hi, {{ username }}</span>
          <button class="btn" @click="onLogout">Logout</button>
        </template>
        <template v-else>
          <router-link :to="{ name: 'login' }" class="btn primary">Login</router-link>
        </template>
      </div>

      <div class="search-wrap">
        <div class="search-tools">
          <input
              class="search"
              v-model.trim="searchState.query"
              placeholder="Search by title, artist or genre…"
          />
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useAuth } from "./composables/useAuth";
import { useSearch } from "./composables/useSearch";

const router = useRouter();
const { state, isLoggedIn, logout } = useAuth();
const { state: searchState, DEFAULT_GENRES, toggleGenre, clearGenres } = useSearch();

const username = computed(() => state.username || "Guest");
function onLogout() {
  logout();
  router.push({ name: "login" });
}
</script>

<style>
:root {
  --brand: #42b983;
  --ink: #222;
  --border: #e5e7eb;
  --bg: #f7f7f8;
  --card: #fff;
}

html, body, #app {
  margin: 0; padding: 0; height: 100%;
  background: var(--bg); color: var(--ink);
  font-family: system-ui, -apple-system, Segoe UI, Roboto, Inter, Arial, sans-serif;
}

.topbar {
  position: sticky; top: 0; z-index: 10;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid var(--border);
}

.left { display: flex; align-items: center; gap: 10px; }
.brand { font-weight: 800; }

.middle { min-height: 40px; }

.right { display: flex; align-items: center; gap: 10px; }

.link {
  color: #333; text-decoration: none; padding: 6px 8px; border-radius: 8px;
}
.link.router-link-active { background: #eef9f3; color: #166b4c; }

.btn {
  padding: 8px 12px; border: none; border-radius: 8px;
  font-weight: 700; cursor: pointer; background: #eee;
}
.btn.primary { background: var(--brand); color: #fff; }

.user { color:#333; font-weight: 600; }

.search-wrap {
  position: absolute;
  left: 50%;
  top: 50%;
  translate: -50% -50%;
  pointer-events: none;
  width: min(92vw, 860px);
}

.search-tools{
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 10px;
  pointer-events: auto;
}

.search {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  font-size: 0.95rem;
  background: #f9fafb;
}
.search:focus {
  outline: none;
  border-color: var(--brand);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(66,185,131,.15);
}

.genre-chips{
  display:flex; align-items:center; gap: 6px;
  overflow: auto;
  max-width: 360px;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
}
.genre-chips::-webkit-scrollbar{ height:8px; }
.genre-chips::-webkit-scrollbar-thumb{ background:#e5e7eb; border-radius:999px; }

.chip{
  padding: 6px 10px;
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 999px;
  cursor: pointer;
  font-weight: 700;
  font-size: .85rem;
  white-space: nowrap;
  transition: background .15s, border-color .15s, color .15s, box-shadow .15s;
}
.chip:hover{ background:#f6f8fb; border-color:#dfe7ee; }
.chip.active{ background:#eef9f3; border-color:#ccefe1; color:#166b4c; box-shadow: 0 4px 10px rgba(31,143,95,.12); }
.chip.clear{ border-style: dashed; }

.main { padding: 16px; }

@media (max-width: 960px) {
  .search-tools{
    grid-template-columns: 1fr;
  }
  .genre-chips{ max-width: 100%; }
}

@media (max-width: 720px) {
  .topbar {
    grid-template-columns: 1fr;
    row-gap: 8px;
  }
  .search-wrap {
    position: static;
    translate: 0 0;
    pointer-events: auto;
    display: grid; gap: 8px;
    order: 2;
    width: 100%;
  }
  .left  { order: 1; }
  .right { order: 3; justify-content: flex-end; }
}
</style>
