<template>
  <div class="select-root" ref="root" :class="{ 'is-open': open }">
    <label v-if="label" class="select-label" :for="inputId">{{ label }}</label>

    <button
        :id="inputId"
        type="button"
        class="select-anchor"
        :aria-expanded="open ? 'true' : 'false'"
        :aria-controls="panelId"
        @click="toggle"
        @keydown="onAnchorKeydown"
    >
      <span class="select-value" :class="{ 'is-placeholder': !selectedItem }">
        {{ selectedItem ? selectedItem[displayKey] : placeholder }}
      </span>
      <span class="select-chevron" aria-hidden="true">▾</span>
    </button>

    <teleport to="body">
      <div
          v-if="open"
          :id="panelId"
          class="select-panel"
          role="listbox"
          :style="panelStyle"
          @keydown.stop.prevent="onPanelKeydown"
      >
        <div class="select-list" ref="listRef">
          <template v-if="items.length">
            <div
                v-for="(item, idx) in items"
                :key="item[valueKey]"
                class="select-option"
                :class="{
                'is-active': idx === activeIndex,
                'is-selected': item[valueKey] === modelValue
              }"
                role="option"
                :aria-selected="item[valueKey] === modelValue ? 'true' : 'false'"
                @mouseenter="activeIndex = idx"
                @mousedown.prevent="select(item)"
            >
              <span class="option-label">{{ item[displayKey] }}</span>
              <span v-if="item[valueKey] === modelValue" class="option-check">✓</span>
            </div>
          </template>
          <div v-else class="select-empty">No options</div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue";

const props = defineProps({
  modelValue: { type: [String, Number], default: "" },
  items: { type: Array, default: () => [] },
  valueKey: { type: String, default: "id" },
  displayKey: { type: String, default: "name" },
  placeholder: { type: String, default: "Select an option" },
  label: { type: String, default: "" }
});
const emit = defineEmits(["update:modelValue", "change"]);

const open = ref(false);
const activeIndex = ref(0);
const root = ref(null);
const listRef = ref(null);

const uid = Math.random().toString(36).slice(2);
const inputId = `select-input-${uid}`;
const panelId = `select-panel-${uid}`;

const selectedItem = computed(() =>
    props.items.find((i) => i[props.valueKey] === props.modelValue)
);

const panelStyle = ref({
  top: "0px",
  left: "0px",
  width: "320px"
});

function updatePanelPosition() {
  const anchor = root.value?.querySelector(".select-anchor");
  if (!anchor) return;
  const rect = anchor.getBoundingClientRect();
  const vw = Math.max(document.documentElement.clientWidth || 0, window.innerWidth || 0);

  const width = Math.min(Math.max(rect.width, 280), Math.min(420, vw - 24));
  panelStyle.value = {
    top: `${rect.bottom + window.scrollY + 6}px`,
    left: `${Math.min(rect.left + window.scrollX, vw - width - 12)}px`,
    width: `${width}px`
  };
}

function openPanel() {
  open.value = true;
  activeIndex.value = Math.max(
      0,
      props.items.findIndex((i) => i[props.valueKey] === props.modelValue)
  );
  updatePanelPosition();
  window.addEventListener("resize", onWindow);
  window.addEventListener("scroll", onWindow, true);
  document.addEventListener("mousedown", onDocumentClick, true);
}

function closePanel() {
  open.value = false;
  window.removeEventListener("resize", onWindow);
  window.removeEventListener("scroll", onWindow, true);
  document.removeEventListener("mousedown", onDocumentClick, true);
}

function toggle() {
  open.value ? closePanel() : openPanel();
}

function onWindow() {
  if (open.value) updatePanelPosition();
}

function onDocumentClick(e) {
  const el = root.value;
  if (!el) return;
  if (!el.contains(e.target)) {
    const panel = document.getElementById(panelId);
    if (panel && !panel.contains(e.target)) {
      closePanel();
    }
  }
}

function select(item) {
  emit("update:modelValue", item[props.valueKey]);
  emit("change", item);
  closePanel();
}

function onAnchorKeydown(e) {
  if (e.key === "ArrowDown" || e.key === "Enter" || e.key === " ") {
    e.preventDefault();
    openPanel();
  }
}

function onPanelKeydown(e) {
  const max = props.items.length - 1;
  if (e.key === "Escape") {
    e.preventDefault();
    closePanel();
    return;
  }
  if (e.key === "ArrowDown") {
    activeIndex.value = Math.min(max, activeIndex.value + 1);
    ensureActiveVisible();
  } else if (e.key === "ArrowUp") {
    activeIndex.value = Math.max(0, activeIndex.value - 1);
    ensureActiveVisible();
  } else if (e.key === "Enter") {
    const item = props.items[activeIndex.value];
    if (item) select(item);
  }
}

function ensureActiveVisible() {
  const list = listRef.value;
  if (!list) return;
  const active = list.querySelector(".select-option.is-active");
  if (!active) return;
  const aTop = active.offsetTop;
  const aBottom = aTop + active.offsetHeight;
  const sTop = list.scrollTop;
  const sBottom = sTop + list.clientHeight;
  if (aTop < sTop) list.scrollTop = aTop;
  if (aBottom > sBottom) list.scrollTop = aBottom - list.clientHeight;
}

watch(() => props.items, () => {
  if (open.value) {
    activeIndex.value = Math.max(
        0,
        props.items.findIndex((i) => i[props.valueKey] === props.modelValue)
    );
  }
});

onMounted(() => {});
onBeforeUnmount(() => {
  window.removeEventListener("resize", onWindow);
  window.removeEventListener("scroll", onWindow, true);
  document.removeEventListener("mousedown", onDocumentClick, true);
});
</script>

<style scoped>
.select-root { width: 100%; }
.select-label { display: inline-block; margin-bottom: 6px; font-weight: 600; color: #222; }

.select-anchor{
  width: 100%;
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #f9fafb;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s, background .2s;
}
.select-anchor:hover { background: #fff; }
.select-root.is-open .select-anchor{
  border-color: #42b983;
  box-shadow: 0 0 0 4px rgba(66,185,131,.15);
  background: #fff;
}
.select-value{ font-size: 1rem; color:#111; text-align: left; }
.select-value.is-placeholder{ color:#888; }
.select-chevron{ margin-left: 12px; color:#888; }

.select-panel{
  position: absolute;
  z-index: 1000;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0,0,0,.12);
  overflow: hidden;
}
.select-list{ max-height: 260px; overflow: auto; padding: 6px; }
.select-option{
  display:flex; align-items:center; justify-content:space-between;
  padding: 10px 12px; border-radius: 10px; cursor: pointer;
  transition: background .15s;
}
.select-option:hover, .select-option.is-active{ background: #f1f5f9; }
.select-option.is-selected{ background: #eaf7f1; }
.option-label{ white-space: nowrap; overflow: hidden; text-overflow: ellipsis; padding-right: 12px; }
.option-check{ color: #2ea673; font-weight: 800; }
.select-empty{ padding: 12px; color:#666; text-align:center; }
</style>
