import { createRouter, createWebHistory } from "vue-router";
import SongList from "../components/SongList.vue";
import SongCreate from "../components/SongCreate.vue";
import SongEdit from "../components/SongEdit.vue";

const routes = [
    { path: "/", name: "songs", component: SongList },
    { path: "/create", name: "create", component: SongCreate },
    { path: "/edit/:id", name: "edit", component: SongEdit, props: true }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;
