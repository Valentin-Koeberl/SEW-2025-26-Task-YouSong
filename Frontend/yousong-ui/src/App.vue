<template>
  <router-view />
</template>

<script setup>
</script>

<style>
:root {
  --brand: #42b983;
  --ink: #222;
  --border: #e5e7eb;
  --bg: #f7f7f8;
  --card: #fff;
}

html,
body,
#app {
  margin: 0;
  padding: 0;
  height: 100%;
  background: var(--bg);
  color: var(--ink);
  font-family: system-ui, -apple-system, Segoe UI, Roboto, Inter, Arial, sans-serif;
}
</style>
