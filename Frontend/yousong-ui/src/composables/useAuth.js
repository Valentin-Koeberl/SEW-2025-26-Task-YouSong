import { reactive, computed } from "vue";

const state = reactive({
    username: localStorage.getItem("username") || null,
    token: localStorage.getItem("token") || null,
});

function login({ username, token }) {
    state.username = username || null;
    state.token = token || null;
    if (username) localStorage.setItem("username", username); else localStorage.removeItem("username");
    if (token) localStorage.setItem("token", token); else localStorage.removeItem("token");
}

function logout() {
    state.username = null;
    state.token = null;
    localStorage.removeItem("username");
    localStorage.removeItem("token");
}

function setUsername(username) {
    state.username = username || null;
    if (username) localStorage.setItem("username", username);
    else localStorage.removeItem("username");
}

export function useAuth() {
    return {
        state,
        isLoggedIn: computed(() => !!state.token),
        login,
        logout,
        setUsername,
    };
}
