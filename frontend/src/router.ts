import { createRouter, createWebHistory } from "vue-router"
import HomeView from "@/views/HomeView.vue"
import LoginView from "@/views/LoginView.vue"
import CaptureView from "@/views/CaptureView.vue"
import LoadingView from "@/views/LoadingView.vue"
import ResultView from "@/views/ResultView.vue"
import ChatView from "@/views/ChatView.vue"
import HistoryView from "@/views/HistoryView.vue"
import HospitalView from "@/views/HospitalView.vue"
import MyPageView from "@/views/MyPageView.vue"
import { isAuthenticated } from "@/lib/api"
const router = createRouter({ history: createWebHistory(), routes: [
  { path: "/login", component: LoginView },
  { path: "/", component: HomeView, meta: { requiresAuth: true } },
  { path: "/capture", component: CaptureView, meta: { requiresAuth: true } },
  { path: "/loading", component: LoadingView, meta: { requiresAuth: true } },
  { path: "/result", component: ResultView, meta: { requiresAuth: true } },
  { path: "/chat", component: ChatView, meta: { requiresAuth: true } },
  { path: "/hospitals", component: HospitalView, meta: { requiresAuth: true } },
  { path: "/history", component: HistoryView, meta: { requiresAuth: true } },
  { path: "/mypage", component: MyPageView, meta: { requiresAuth: true } },
], scrollBehavior: () => ({ top: 0 }) })
router.beforeEach((to) => { if (to.meta.requiresAuth && !isAuthenticated()) return { path: "/login", query: { redirect: to.fullPath } }; if (to.path === "/login" && isAuthenticated()) return "/" })
export default router
