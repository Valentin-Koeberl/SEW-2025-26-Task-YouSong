import { createRouter, createWebHistory } from "vue-router";
import SongList from "../components/SongList.vue";
import SongCreate from "../components/SongCreate.vue";
import SongEdit from "../components/SongEdit.vue";
import Login from "../components/Login.vue";
import ArtistList from "../components/ArtistList.vue";

const routes = [
    { path: "/", name: "songs", component: SongList },
    { path: "/create", name: "create", component: SongCreate },
    { path: "/edit/:id", name: "edit", component: SongEdit, props: true },
    { path: "/login", name: "login", component: Login },
    { path: "/artists", name: "artists", component: ArtistList }, // NEW
];

export default createRouter({
    history: createWebHistory(),
    routes,
});
