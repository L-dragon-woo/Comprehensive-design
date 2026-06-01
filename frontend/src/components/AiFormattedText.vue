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

function shouldSkipSection(section: AiSection | null) {
  if (!section) return false
  const label = `${section.step} ${section.title}`.toLowerCase()
  return /step\s*4/.test(label) || /q\s*&?\s*a|qna|faq|질문|답변/.test(label)
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
  if (section && !shouldSkipSection(section) && card && (card.title || card.lines.length)) {
    section.cards.push({ ...card, lines: card.lines.filter(Boolean) })
  }
}

function parsePlainReport(lines: string[], fallbackTitle: string) {
  const parsedTitle = fallbackTitle || (/리포트|분석|진단|추천/.test(lines[0] || "") ? clean(lines.shift() || "") : "")
  const intro: string[] = []
  const diagnosis: AiSection = { step: "Step 1", title: "피부 진단 결과", cards: [], notes: [] }
  const treatments: AiSection = { step: "Step 2", title: "추천 시술 및 관리 방향", cards: [], notes: [] }
  const evidence: AiSection = { step: "Step 3", title: "학술 근거", cards: [], notes: [] }
  const closing: string[] = []

  let currentTreatment: AiCard | null = null

  for (const rawLine of lines) {
    const line = clean(rawLine)
    if (!line) continue

    const metricMatch = line.match(/^(.+?)\s*:\s*(\d+(?:\.\d+)?)\s*점\s*[-–]\s*(.+)$/)
    if (metricMatch) {
      diagnosis.cards.push({
        title: clean(metricMatch[1]),
        score: `${metricMatch[2]}점`,
        lines: [clean(metricMatch[3])],
      })
      continue
    }

    const treatmentMatch = line.match(/^권장\s*시술\s*:\s*(.+)$/)
    if (treatmentMatch) {
      if (currentTreatment) treatments.cards.push(currentTreatment)
      currentTreatment = { title: clean(treatmentMatch[1]), lines: [] }
      continue
    }

    const treatmentFeatureMatch = line.match(/^시술\s*특징\s*:\s*(.+)$/)
    if (treatmentFeatureMatch) {
      if (!currentTreatment) currentTreatment = { title: "시술 특징", lines: [] }
      currentTreatment.lines.push(clean(treatmentFeatureMatch[1]))
      continue
    }

    const evidenceMatch = line.match(/^(.+?)\s*:\s*(.+PMID\s*\d+.*)$/i)
    if (evidenceMatch) {
      evidence.cards.push({
        title: clean(evidenceMatch[1]),
        lines: [clean(evidenceMatch[2])],
      })
      continue
    }

    if (/^다음 단계|^\*?본 진단|최종 시술 플랜/.test(line)) {
      closing.push(line.replace(/^\*|\*$/g, ""))
      continue
    }

    if (diagnosis.cards.length || treatments.cards.length || currentTreatment || evidence.cards.length) {
      if (evidence.cards.length) evidence.notes.push(line)
      else if (currentTreatment) currentTreatment.lines.push(line)
      else treatments.notes.push(line)
    } else {
      intro.push(line)
    }
  }

  if (currentTreatment) treatments.cards.push(currentTreatment)

  return {
    title: parsedTitle,
    intro,
    sections: [diagnosis, treatments, evidence].filter((section) => section.cards.length || section.notes.length),
    closing,
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
      if (!shouldSkipSection(currentSection)) sections.push(currentSection)
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

      if (shouldSkipSection(currentSection)) continue

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
      if (shouldSkipSection(currentSection)) continue
      if (!currentSection) intro.push(clean(numberedMatch[1]))
      else if (currentCard) currentCard.lines.push(clean(numberedMatch[1]))
      else currentSection.notes.push(clean(numberedMatch[1]))
      continue
    }

    const text = clean(rawLine)
    if (shouldSkipSection(currentSection)) continue
    if (!currentSection) intro.push(text)
    else if (currentCard) currentCard.lines.push(text)
    else if (sections.length) currentSection.notes.push(text)
    else closing.push(text)
  }

  pushCard(currentSection, currentCard)

  if (!sections.length && intro.length) {
    return parsePlainReport([...intro], title)
  }

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
  <article :class="['max-w-full overflow-hidden text-sm [overflow-wrap:anywhere]', compact ? 'space-y-3' : 'space-y-5']">
    <header v-if="parsed.title || parsed.intro.length" class="space-y-2">
      <h4 v-if="parsed.title && !compact" class="text-base font-bold text-foreground">
        {{ parsed.title }}
      </h4>
      <p v-for="(line, index) in parsed.intro" :key="index" class="max-w-full break-words leading-relaxed text-muted-foreground">
        {{ line }}
      </p>
    </header>

    <section
      v-for="section in parsed.sections"
      :key="`${section.step}-${section.title}`"
      :class="['max-w-full overflow-hidden rounded-xl border p-4 shadow-sm', compact ? 'space-y-3' : 'space-y-4', sectionClass(section.title)]"
    >
      <div class="flex min-w-0 items-start gap-2">
        <span class="rounded-full bg-white/80 px-2.5 py-1 text-[11px] font-bold text-primary shadow-sm">
          {{ section.step }}
        </span>
        <h5 class="min-w-0 flex-1 break-words font-bold leading-snug text-foreground">{{ section.title }}</h5>
      </div>

      <p v-for="(note, index) in section.notes" :key="index" class="max-w-full break-words leading-relaxed text-foreground/80">
        {{ note }}
      </p>

      <div v-if="section.cards.length" class="grid gap-3">
        <div
          v-for="(card, index) in section.cards"
          :key="`${card.title}-${index}`"
          class="max-w-full overflow-hidden rounded-lg border border-border/70 bg-card shadow-sm"
        >
          <div class="flex min-w-0">
            <div :class="['w-1 shrink-0', cardAccent(section.title)]" />
            <div class="min-w-0 flex-1 px-4 py-3">
              <div class="mb-2 flex min-w-0 flex-wrap items-start justify-between gap-2">
                <h6 class="min-w-0 flex-1 basis-40 break-words font-semibold leading-snug text-foreground">
                  {{ card.title || "요약" }}
                </h6>
                <span v-if="card.score" class="shrink-0 rounded-full bg-primary/10 px-2.5 py-1 text-xs font-bold text-primary">
                  {{ card.score }}
                </span>
              </div>

              <ul v-if="card.lines.length" class="space-y-1.5">
                <li v-for="(line, lineIndex) in card.lines" :key="lineIndex" class="flex gap-2 leading-relaxed text-foreground/85">
                  <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary/60" />
                  <span class="min-w-0 flex-1 break-words">{{ line }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </section>

    <footer v-if="parsed.closing.length" class="max-w-full space-y-2 overflow-hidden rounded-xl bg-secondary px-4 py-3 text-foreground/80">
      <p v-for="(line, index) in parsed.closing" :key="index" class="break-words leading-relaxed">
        {{ line }}
      </p>
    </footer>
  </article>
</template>
