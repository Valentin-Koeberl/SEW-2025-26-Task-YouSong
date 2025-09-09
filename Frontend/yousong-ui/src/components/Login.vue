<template>
  <div class="login-wrap">
    <form class="login-card" @submit.prevent="onSubmit" novalidate>
      <h1 class="title">Welcome back</h1>
      <p class="subtitle">Sign in to continue to YouSong</p>

      <label class="lbl">Username</label>
      <input
          v-model.trim="username"
          class="inp"
          autocomplete="username"
          placeholder="hugo"
          @keyup.enter="focusPassword"
      />

      <label class="lbl">Password</label>
      <div class="pass-row">
        <input
            ref="pwRef"
            :type="showPw ? 'text' : 'password'"
            v-model="password"
            class="inp"
            autocomplete="current-password"
            placeholder="••••••••"
        />
        <button class="ghost" type="button" @click="showPw = !showPw" :aria-pressed="showPw ? 'true' : 'false'">
          {{ showPw ? 'Hide' : 'Show' }}
        </button>
      </div>

      <button class="btn primary" type="submit" :disabled="submitting">
        <span v-if="submitting">Signing in…</span>
        <span v-else>Sign in</span>
      </button>

      <p v-if="error" class="error">{{ error }}</p>

      <div class="sep"><span>or</span></div>

      <div class="reg">
        <h2 class="reg-title">Create an account</h2>
        <label class="lbl">Username</label>
        <input v-model.trim="regUser" class="inp" placeholder="yourname" autocomplete="off" />
        <label class="lbl">Password</label>
        <input v-model="regPass" class="inp" type="password" placeholder="Choose a password" autocomplete="new-password" />
        <button class="btn" type="button" @click="onRegister" :disabled="regSubmitting">
          <span v-if="regSubmitting">Creating…</span>
          <span v-else>Create account</span>
        </button>
        <p v-if="regMsg" class="ok">{{ regMsg }}</p>
        <p v-if="regErr" class="error">{{ regErr }}</p>
      </div>
    </form>
  </div>
</template>

<script setup>
import api from "../services/api";
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuth } from "../composables/useAuth";

const router = useRouter();
const { login: authLogin } = useAuth();

const username = ref("");
const password = ref("");
const showPw = ref(false);
const submitting = ref(false);
const error = ref("");
const pwRef = ref(null);

const regUser = ref("");
const regPass = ref("");
const regSubmitting = ref(false);
const regErr = ref("");
const regMsg = ref("");

function focusPassword() {
  pwRef.value?.focus();
}

function extractToken(res) {
  const hdr = res?.headers?.authorization || res?.headers?.Authorization;
  if (hdr && /^Bearer\s+/i.test(hdr)) return hdr.split(/\s+/)[1];
  if (res?.data?.token) return res.data.token;
  return null;
}

async function onSubmit() {
  error.value = "";
  if (!username.value || !password.value) {
    error.value = "Please enter username and password.";
    return;
  }

  submitting.value = true;
  try {
    const body = new URLSearchParams();
    body.append("username", username.value);
    body.append("password", password.value);

    const res = await api.post("/login", body, {
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
    });

    const token = extractToken(res);
    const name = res?.data?.username || username.value;

    authLogin({ username: name, token });

    router.push({ name: "songs" });
  } catch (e) {
    const status = e?.response?.status;
    if (status === 401) error.value = "Invalid credentials.";
    else if (status === 415) error.value = "Unsupported media type.";
    else error.value = "Login failed. Please try again.";
  } finally {
    submitting.value = false;
  }
}

async function onRegister() {
  regErr.value = "";
  regMsg.value = "";
  if (!regUser.value || !regPass.value) {
    regErr.value = "Please enter a username and password.";
    return;
  }
  regSubmitting.value = true;
  try {
    await api.post("/api/users", {
      username: regUser.value,
      password: regPass.value,
    });

    regMsg.value = "Account created! Signing you in…";

    username.value = regUser.value;
    password.value = regPass.value;
    await onSubmit();
  } catch (e) {
    const status = e?.response?.status;
    if (status === 409) regErr.value = "Username already exists.";
    else if (status === 400) regErr.value = e?.response?.data?.message || "Validation failed.";
    else regErr.value = "Registration failed.";
  } finally {
    regSubmitting.value = false;
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 24px 16px;
  background: radial-gradient(1200px 600px at 50% -10%, #eaf7f1 10%, transparent 60%),
  radial-gradient(900px 500px at 110% 10%, #eef3ff 10%, transparent 60%),
  var(--bg);
}

.login-card {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(0,0,0,.08);
  padding: 24px;
  display: grid;
  gap: 12px;
}

.title { margin: 0; font-size: 1.6rem; color: #111; }
.subtitle { margin: -4px 0 6px; color: #666; font-size: .95rem; }

.lbl { font-weight: 600; color: #222; margin-top: 8px; }

.inp {
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #f9fafb;
  font-size: 1rem;
  transition: border-color .2s, box-shadow .2s, background .2s;
}
.inp:focus {
  outline: none;
  border-color: var(--brand);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(66,185,131,.15);
}

.pass-row { display: grid; grid-template-columns: 1fr auto; gap: 8px; align-items: center; }

.ghost {
  padding: 10px 12px;
  background: #f3f4f6;
  border: 1px solid var(--border);
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
}
.ghost:hover { background: #fff; }

.btn {
  margin-top: 6px;
  padding: 12px 16px;
  border: none;
  border-radius: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: transform .06s ease, box-shadow .2s ease, background .2s ease;
}
.btn.primary { background: var(--brand); color: #fff; box-shadow: 0 8px 18px rgba(66,185,131,.25); }
.btn.primary:hover { transform: translateY(-1px); }
.btn:disabled { opacity: .6; cursor: not-allowed; }

.sep { display: grid; place-items: center; color: #888; margin: 8px 0 2px; }
.sep span { font-size: .85rem; }

.reg { margin-top: 2px; padding-top: 8px; border-top: 1px dashed var(--border); display: grid; gap: 10px; }
.reg-title { margin: 0; font-size: 1.05rem; color: #222; }

.error { color: #e74c3c; font-weight: 600; text-align: center; margin-top: 4px; }
.ok { color: #1f8f5f; font-weight: 600; text-align: center; margin-top: 4px; }
</style>
