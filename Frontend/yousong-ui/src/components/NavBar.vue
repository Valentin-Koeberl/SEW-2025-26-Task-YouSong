<template>
  <header class="nav">
    <div class="nav-left" @click="goHome" title="YouSong">YouSong</div>
    <div class="nav-right">
      <input
          class="search"
          type="text"
          v-model="search"
          placeholder="Search by title or artist…"
          @input="onType"
      />
      <button class="btn primary" @click="goCreate">Add Song</button>
    </div>
  </header>
</template>

<script setup>
import { ref } from "vue";
import { useRoute,useRouter } from "vue-router";
const router=useRouter(), route=useRoute();
const search=ref(route.query.q?.toString() ?? "");
let t; const onType=()=>{ clearTimeout(t); t=setTimeout(()=>{
  const q=search.value.trim(); router.replace({ name: route.name ?? "songs", query: q?{q}:{}}); },300); };
const goCreate=()=>router.push({name:"create"});
const goHome=()=>router.push({name:"songs"});
</script>

<style scoped>
/* Full-bleed: über gesamte Viewport-Breite, unabhängig vom Parent */
.nav{
  position: sticky;
  top: 0;
  z-index: 50;

  /* der Trick: nimm 100vw und zentriere relativ zum Viewport */
  width: 100vw;
  left: 50%;
  transform: translateX(-50%);

  /* Layout */
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px clamp(16px, 3vw, 32px);
  box-sizing: border-box;

  background: var(--card);
  border-bottom: 1px solid var(--border);
  box-shadow: 0 2px 6px rgba(0,0,0,.05);
}

.nav-left{
  font-size: 1.25rem;
  font-weight: 800;
  cursor: pointer;
  user-select: none;
}

.nav-right{
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

/* abgerundete Suche */
.search{
  width: min(36vw, 420px);
  min-width: 160px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 999px;
  font-size: .95rem;
  background: #fff;
  transition: border-color .15s, box-shadow .15s;
}
.search:focus{
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 4px rgba(66,185,131,.2);
}

.btn{
  padding: 10px 16px;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
}
.btn.primary{ background: var(--brand); color:#fff; }
.btn.primary:hover{ background: var(--brand-600); }

/* Responsiv: auf kleinen Screens wrappt die rechte Seite */
@media (max-width: 700px){
  .nav{ flex-wrap: wrap; row-gap: 10px; }
  .nav-left{ flex: 1 0 100%; }
  .nav-right{ flex: 1 0 100%; display: grid; grid-template-columns: 1fr auto; gap: 10px; }
  .search{ width: 100%; min-width: 0; }
}
@media (max-width: 420px){
  .nav-right{ grid-template-columns: 1fr; }
  .btn.primary{ width: 100%; }
}
</style>
