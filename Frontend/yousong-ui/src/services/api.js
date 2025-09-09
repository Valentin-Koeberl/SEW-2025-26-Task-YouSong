import axios from "axios";

const api = axios.create({
    baseURL: "",
    timeout: 10000,
});

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("token");
        if (token) config.headers.Authorization = `Bearer ${token}`;
        const method = (config.method || "get").toUpperCase();
        const fullUrl = (config.baseURL || "") + (config.url || "");
        console.debug("[API] ->", method, fullUrl, { params: config.params, hasBody: !!config.data });
        return config;
    },
    (error) => {
        console.error("[API][request error]", error);
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (res) => {
        console.debug("[API] <-", res.status, res.config?.url);
        return res;
    },
    (error) => {
        if (error.response) {
            console.error("[API][response error]", error.response.status, error.config?.url, error.response.data);
        } else if (error.request) {
            console.error("[API][network error] No response", error.config?.url, error.message);
        } else {
            console.error("[API][setup error]", error.message);
        }
        return Promise.reject(error);
    }
);

export default api;
