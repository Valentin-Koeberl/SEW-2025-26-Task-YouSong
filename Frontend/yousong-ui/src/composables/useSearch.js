import { reactive } from "vue";

const state = reactive({
    query: ""
});

export function useSearch() {
    return { state };
}
