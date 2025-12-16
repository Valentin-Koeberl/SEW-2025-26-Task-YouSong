import axios from "axios";

const api = axios.create({
    baseURL: (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, ""),
    withCredentials: true,
});

function getCookie(name) {
    const m = document.cookie.match(new RegExp("(^|;\\s*)" + name + "=([^;]*)"));
    return m ? decodeURIComponent(m[2]) : null;
}

api.interceptors.request.use((config) => {
    const method = (config.method || "get").toLowerCase();
    const needsCsrf = ["post", "put", "patch", "delete"].includes(method);

    if (needsCsrf) {
        const token = getCookie("XSRF-TOKEN");
        if (token) {
            config.headers = config.headers || {};
            config.headers["X-XSRF-TOKEN"] = token;
        }
    }
    return config;
});

export default api;
