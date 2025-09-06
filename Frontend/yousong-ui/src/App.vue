<template>
  <div>
    <header class="topbar">
      <!-- LINKS (fix) -->
      <div class="left">
        <span class="brand">YouSong</span>
        <router-link :to="{ name: 'songs' }" class="link">Songs</router-link>
        <router-link :to="{ name: 'create' }" class="link">Add</router-link>
      </div>

      <!-- MITTE (flex) – nur der Platzhalter, damit links/rechts fix sind -->
      <div class="middle"></div>

      <!-- RECHTS (fix) -->
      <div class="right">
        <template v-if="isLoggedIn">
          <span class="user">Hi, {{ username }}</span>
          <button class="btn" @click="onLogout">Logout</button>
        </template>
        <template v-else>
          <router-link :to="{ name: 'login' }" class="btn primary">Login</router-link>
        </template>
      </div>

      <!-- SUCHE: absolut in der Header-Mitte, unabhängig von Links/Rechts -->
      <div class="search-wrap">
        <input
            class="search"
            v-model.trim="searchState.query"
            placeholder="Search by title or artist…"
        />
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
const { state: searchState } = useSearch();

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

/* 3-Spalten-Header: auto | 1fr | auto  */
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

/* LINKS: fix */
.left { display: flex; align-items: center; gap: 10px; }
.brand { font-weight: 800; }

/* MITTE: flex – leerer Platzhalter, damit die rechte Spalte fix bleibt */
.middle { min-height: 40px; }

/* RECHTS: fix */
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

/* ABSOLUT ZENTRIERTE SUCHE – relativ zur gesamten Topbar */
.search-wrap {
  position: absolute;
  left: 50%;
  top: 50%;
  translate: -50% -50%;
  pointer-events: none; /* blockiert nix darunter */
}

.search {
  width: clamp(240px, 40vw, 520px);
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  font-size: 0.95rem;
  background: #f9fafb;
  pointer-events: auto; /* Klicks erlauben */
}
.search:focus {
  outline: none;
  border-color: var(--brand);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(66,185,131,.15);
}

.main { padding: 16px; }

/* Responsive: unter 720px stacken wir und die Suche geht in den Fluss */
@media (max-width: 720px) {
  .topbar {
    grid-template-columns: 1fr;
    row-gap: 8px;
  }
  .search-wrap {
    position: static;
    translate: 0 0;
    pointer-events: auto;
    display: flex; justify-content: center;
    order: 2;
  }
  .search { width: 100%; max-width: 560px; }
  .left  { order: 1; }
  .right { order: 3; justify-content: flex-end; }
}
</style>
