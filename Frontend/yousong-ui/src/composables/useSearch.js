import { reactive } from "vue";

export const DEFAULT_GENRES = [
    "Pop","Rock","Hip Hop","R&B","Electronic","Classical","Jazz","Country",
    "Metal","Reggae","Folk","Blues","Indie","K-Pop","Soundtrack","Acoustic"
];

const state = reactive({
    query: "",
    genres: []
});

function toggleGenre(g) {
    const i = state.genres.indexOf(g);
    if (i >= 0) state.genres.splice(i, 1);
    else state.genres.push(g);
}

function clearGenres() {
    state.genres = [];
}

export function useSearch() {
    return { state, DEFAULT_GENRES, toggleGenre, clearGenres };
}
