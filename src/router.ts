import { createRouter, createWebHistory } from "vue-router"
import HomeView from "@/views/HomeView.vue"
import CaptureView from "@/views/CaptureView.vue"
import LoadingView from "@/views/LoadingView.vue"
import ResultView from "@/views/ResultView.vue"
import ChatView from "@/views/ChatView.vue"
import HistoryView from "@/views/HistoryView.vue"
import HospitalView from "@/views/HospitalView.vue"

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: HomeView },
    { path: "/capture", component: CaptureView },
    { path: "/loading", component: LoadingView },
    { path: "/result", component: ResultView },
    { path: "/chat", component: ChatView },
    { path: "/hospitals", component: HospitalView },
    { path: "/history", component: HistoryView },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
