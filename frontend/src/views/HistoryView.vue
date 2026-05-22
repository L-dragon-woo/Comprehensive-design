<script setup lang="ts">
import { Calendar, Sparkles } from "lucide-vue-next"
import AppHeader from "@/components/AppHeader.vue"
import BaseButton from "@/components/BaseButton.vue"
import BottomNav from "@/components/BottomNav.vue"
import PageContainer from "@/components/PageContainer.vue"
import { getHistoryData } from "@/lib/skinai"

const historyData = getHistoryData()
</script>

<template>
  <AppHeader title="분석 기록" show-back show-notification />
  <PageContainer>
    <section v-if="historyData.length" class="py-6">
      <div class="space-y-3">
        <div v-for="record in historyData" :key="record.id" class="rounded-2xl border border-border bg-card p-4 shadow-sm">
          <div class="flex items-start gap-4">
            <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-secondary">
              <Calendar class="h-5 w-5 text-muted-foreground" />
            </div>
            <div class="flex-1">
              <p class="font-semibold">{{ record.dateFormatted }}</p>
              <p class="mt-1 text-2xl font-bold">{{ record.score }}점</p>
              <div class="mt-2 flex flex-wrap gap-1.5">
                <span v-for="item in record.improvements" :key="item" class="rounded-full bg-accent px-2.5 py-1 text-xs">{{ item }}</span>
              </div>
              <RouterLink to="/result" class="mt-3 block">
                <BaseButton variant="outline" size="sm" class="w-full">결과 보기</BaseButton>
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div v-else class="py-20 text-center">
      <Sparkles class="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
      <h3 class="mb-2 text-lg font-semibold">아직 분석 기록이 없습니다</h3>
      <p class="mb-6 text-sm text-muted-foreground">첫 피부 분석을 시작해 보세요.</p>
      <RouterLink to="/capture"><BaseButton>분석 시작</BaseButton></RouterLink>
    </div>
  </PageContainer>
  <BottomNav />
</template>
