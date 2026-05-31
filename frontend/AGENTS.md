<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-31 | Updated: 2026-05-31 -->

# frontend

## Purpose
Vue 3 + Vite SPA for the SkinAI skin-analysis service. Handles face capture via the browser camera API, streams captured frames through face-detection, submits images to the Spring Boot backend for AI analysis, and renders results with score rings, metric cards, a PDF export, an AI beauty chat, a Kakao Maps hospital finder, and an analysis history list. The app is a mobile-first, fixed-height layout served on port 5173; in development the Vite dev server proxies `/api` to the backend on `:8080`.

## Key Files
| File | Description |
|------|-------------|
| `package.json` | pnpm 10.24.0 workspace; declares Vue 3.5, vue-router 4, jspdf 4, lucide-vue-next, tailwindcss 4, and Vite 7 dev dependencies |
| `vite.config.ts` | Registers `@vitejs/plugin-vue` and `@tailwindcss/vite`; binds dev server to `0.0.0.0:5173`; proxies `/api` to `VITE_API_PROXY_TARGET` (default `http://localhost:8080`); maps `@/` to `src/` |
| `tsconfig.json` | `ES2022` target, strict mode, bundler module resolution, `@/*` path alias pointing at `src/` |
| `index.html` | PWA-ready HTML shell in Korean (`lang="ko"`); mounts `#app`; sets mobile viewport with `user-scalable=no` |
| `Dockerfile` | Node 22 Alpine image; enables corepack/pnpm 10.24.0; runs `pnpm dev --host 0.0.0.0` on port 5173 |
| `.dockerignore` | Excludes `node_modules/`, `dist/`, and `*.log` from Docker context |
| `apiList.md` | Documents all planned backend REST endpoints, classifies them by AI-dependency, and lists the recommended backend-first implementation order |

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `src/` | Application source: entry point, router, global styles, and feature sub-trees (see `src/AGENTS.md`) |
| `public/` | Static assets served at `/`: SVG app icon, PNG apple-touch icon, placeholder images used during development |

## For AI Agents
### Working In This Directory
- The package manager is **pnpm** via corepack. Always use `corepack pnpm <command>` or `pnpm <command>`; never `npm` or `yarn`.
- The `@/` import alias resolves to `src/`. Use it for all intra-`src` imports.
- `VITE_KAKAO_JAVASCRIPT_KEY` (or `VITE_KAKAO_MAP_APP_KEY`) and `VITE_API_PROXY_TARGET` must be set in a `.env` file for hospital-map and proxy features to work locally.
- Dev commands:
  - `corepack pnpm dev` — start dev server on port 5173
  - `corepack pnpm build` — type-check (`vue-tsc -b`) then bundle
  - `corepack pnpm lint` — type-check only (`vue-tsc -b --pretty false`)
  - `corepack pnpm preview` — preview production build

### Testing Requirements
- There is no automated test suite in this directory. Validation is done via TypeScript type-checking (`pnpm lint`).
- After any change, run `corepack pnpm lint` and confirm zero errors before committing.

### Common Patterns
- Tailwind CSS v4 is used via the Vite plugin; there is no `tailwind.config` file — all theme tokens are defined in `src/styles.css` under `@theme inline`.
- lucide-vue-next provides all icons; import individual icons by name from the package.

## Dependencies
### Internal
- `src/` — all application code

### External
- `vue` ^3.5.25 — reactivity, `<script setup>`, Composition API
- `vue-router` ^4.6.4 — `createWebHistory` SPA routing with `requiresAuth` navigation guards
- `tailwindcss` ^4.2.0 + `@tailwindcss/vite` ^4.2.0 — utility-first CSS via Vite plugin
- `jspdf` ^4.2.1 — PDF generation (used in `src/lib/pdf.ts` via browser print window)
- `lucide-vue-next` ^0.564.0 — SVG icon components
- `vite` ^7.3.0 — build tool and dev server
- `@vitejs/plugin-vue` ^6.0.3 — Vue SFC compilation
- `vue-tsc` ^3.1.8 — TypeScript type-checking for `.vue` files
- `typescript` ^5.7.3 — language toolchain
- Kakao Maps JavaScript SDK — loaded dynamically at runtime in `HospitalView.vue` using `VITE_KAKAO_MAP_APP_KEY`

<!-- MANUAL: Any manually added notes below this line are preserved on regeneration -->
