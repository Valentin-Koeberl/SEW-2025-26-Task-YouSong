import { reactive, computed } from "vue";
import api from "../services/api";

const state = reactive({
    username: "",
    hydrated: false, // damit App weiß, dass wir einmal geprüft haben
});

const isLoggedIn = computed(() => !!state.username);

async function refreshSession() {
    try {
        const res = await api.get("/api/auth/me");
        state.username = res?.data?.username || "";
    } catch {
        state.username = "";
    } finally {
        state.hydrated = true;
    }
}

function login(payload) {
    // nach erfolgreichem POST /login
    state.username = payload?.username || "";
    state.hydrated = true;
}

async function logout() {
    try {
        await api.post("/logout");
    } catch {
        // ignore
    } finally {
        state.username = "";
        state.hydrated = true;
    }
}

export function useAuth() {
    return { state, isLoggedIn, login, logout, refreshSession };
}
