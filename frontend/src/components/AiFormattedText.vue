<script setup lang="ts">
import { computed } from "vue"

type AiCard = {
  title: string
  score?: string
  lines: string[]
}

type AiSection = {
  step: string
  title: string
  cards: AiCard[]
  notes: string[]
}

const props = withDefaults(defineProps<{ content: string; compact?: boolean }>(), {
  compact: false,
})

function clean(text: string) {
  return text
    .replace(/^#{1,6}\s*/, "")
    .replace(/\*\*(.+?)\*\*/g, "$1")
    .replace(/\s{2,}/g, " ")
    .trim()
}

function isStep(line: string) {
  return /^\[?\s*Step\s*\d+\s*\]?/i.test(line.trim())
}

function parseStep(line: string) {
  const match = clean(line).match(/^(Step\s*(\d+))\s*(.+)?$/i)
  return {
    step: match?.[1]?.replace(/\s+/g, " ") || "Step",
    title: clean(match?.[3] || line),
  }
}

function parseTitleScore(text: string) {
  let title = clean(text)
  const scoreMatch = title.match(/(\d+(?:\.\d+)?)\s*점/)
  const score = scoreMatch ? `${scoreMatch[1]}점` : undefined
  if (score) {
    title = title
      .replace(/\(?\d+(?:\.\d+)?\s*점\)?/g, "")
      .replace(/[:：]\s*$/g, "")
      .trim()
  }
  return { title, score }
}

function pushCard(section: AiSection | null, card: AiCard | null) {
  if (section && card && (card.title || card.lines.length)) {
    section.cards.push({ ...card, lines: card.lines.filter(Boolean) })
  }
}

const parsed = computed(() => {
  const lines = props.content.replace(/\r\n/g, "\n").split("\n").map((line) => line.trim())
  const intro: string[] = []
  const closing: string[] = []
  const sections: AiSection[] = []
  let title = ""
  let currentSection: AiSection | null = null
  let currentCard: AiCard | null = null

  for (const rawLine of lines) {
    if (!rawLine || /^---+$/.test(rawLine)) continue

    if (!title && rawLine.startsWith("# ")) {
      title = clean(rawLine)
      continue
    }

    if (isStep(rawLine)) {
      pushCard(currentSection, currentCard)
      currentCard = null
      const step = parseStep(rawLine)
      currentSection = { ...step, cards: [], notes: [] }
      sections.push(currentSection)
      continue
    }

    const headingMatch = rawLine.match(/^#{2,6}\s+(.+)$/)
    if (headingMatch) {
      pushCard(currentSection, currentCard)
      const parsedHeading = parseTitleScore(headingMatch[1])
      currentCard = { title: parsedHeading.title, score: parsedHeading.score, lines: [] }
      continue
    }

    const bulletMatch = rawLine.match(/^[-*]\s+(.+)$/)
    if (bulletMatch) {
      const bullet = clean(bulletMatch[1])
      const bulletTitle = bullet.match(/^(.+?)(?:[:：]\s+)(.+)$/)

      if (!currentSection) {
        intro.push(bullet)
        continue
      }

      if (!currentCard) {
        const first = parseTitleScore(bulletTitle ? bulletTitle[1] : bullet)
        currentCard = {
          title: first.title,
          score: first.score,
          lines: bulletTitle ? [bulletTitle[2]] : [],
        }
      } else {
        currentCard.lines.push(bullet)
      }
      continue
    }

    const numberedMatch = rawLine.match(/^\d+[.)]\s+(.+)$/)
    if (numberedMatch) {
      if (!currentSection) intro.push(clean(numberedMatch[1]))
      else if (currentCard) currentCard.lines.push(clean(numberedMatch[1]))
      else currentSection.notes.push(clean(numberedMatch[1]))
      continue
    }

    const text = clean(rawLine)
    if (!currentSection) intro.push(text)
    else if (currentCard) currentCard.lines.push(text)
    else if (sections.length) currentSection.notes.push(text)
    else closing.push(text)
  }

  pushCard(currentSection, currentCard)

  return { title, intro, sections, closing }
})

function sectionClass(title: string) {
  if (/진단|결과/.test(title)) return "border-sky-200 bg-sky-50"
  if (/시술|추천/.test(title)) return "border-violet-200 bg-violet-50"
  if (/PubMed|근거|학술/.test(title)) return "border-emerald-200 bg-emerald-50"
  return "border-primary/20 bg-primary/5"
}

function cardAccent(title: string) {
  if (/PubMed|근거|학술/.test(title)) return "bg-emerald-500"
  if (/시술|추천/.test(title)) return "bg-violet-500"
  return "bg-primary"
}
</script>

<template>
  <article :class="compact ? 'space-y-3 text-sm' : 'space-y-5 text-sm'">
    <header v-if="parsed.title || parsed.intro.length" class="space-y-2">
      <h4 v-if="parsed.title && !compact" class="text-base font-bold text-foreground">
        {{ parsed.title }}
      </h4>
      <p v-for="(line, index) in parsed.intro" :key="index" class="leading-relaxed text-muted-foreground">
        {{ line }}
      </p>
    </header>

    <section
      v-for="section in parsed.sections"
      :key="`${section.step}-${section.title}`"
      :class="['rounded-xl border p-4 shadow-sm', compact ? 'space-y-3' : 'space-y-4', sectionClass(section.title)]"
    >
      <div class="flex items-center gap-2">
        <span class="rounded-full bg-white/80 px-2.5 py-1 text-[11px] font-bold text-primary shadow-sm">
          {{ section.step }}
        </span>
        <h5 class="font-bold text-foreground">{{ section.title }}</h5>
      </div>

      <p v-for="(note, index) in section.notes" :key="index" class="leading-relaxed text-foreground/80">
        {{ note }}
      </p>

      <div v-if="section.cards.length" class="grid gap-3">
        <div
          v-for="(card, index) in section.cards"
          :key="`${card.title}-${index}`"
          class="overflow-hidden rounded-lg border border-border/70 bg-card shadow-sm"
        >
          <div class="flex">
            <div :class="['w-1 shrink-0', cardAccent(section.title)]" />
            <div class="min-w-0 flex-1 px-4 py-3">
              <div class="mb-2 flex items-start justify-between gap-3">
                <h6 class="min-w-0 font-semibold leading-snug text-foreground">
                  {{ card.title || "요약" }}
                </h6>
                <span v-if="card.score" class="shrink-0 rounded-full bg-primary/10 px-2.5 py-1 text-xs font-bold text-primary">
                  {{ card.score }}
                </span>
              </div>

              <ul v-if="card.lines.length" class="space-y-1.5">
                <li v-for="(line, lineIndex) in card.lines" :key="lineIndex" class="flex gap-2 leading-relaxed text-foreground/85">
                  <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary/60" />
                  <span>{{ line }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </section>

    <footer v-if="parsed.closing.length" class="space-y-2 rounded-xl bg-secondary px-4 py-3 text-foreground/80">
      <p v-for="(line, index) in parsed.closing" :key="index" class="leading-relaxed">
        {{ line }}
      </p>
    </footer>
  </article>
</template>
