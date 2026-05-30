import type { UserProfile } from "./api"
import type { AnalysisResult } from "./skinai"

export interface PdfOptions {
  analysis: AnalysisResult
  user: UserProfile | null
  notes?: string
  capturedImageDataUrl?: string
}

function scoreStatusKr(score: number): string {
  if (score >= 80) return "양호"
  if (score >= 60) return "보통"
  if (score >= 40) return "관리 필요"
  return "집중 관리"
}

function buildPrintHtml(options: PdfOptions): string {
  const { analysis, user, notes, capturedImageDataUrl } = options
  const date = analysis.date || new Intl.DateTimeFormat("ko-KR").format(new Date())

  const metricsRows = (analysis.metrics || [])
    .map(
      (m) => `
      <tr>
        <td>${m.title}</td>
        <td>${m.score}점</td>
        <td class="status-${scoreStatusKr(m.score).replace(" ", "-")}">${m.status || scoreStatusKr(m.score)}</td>
        <td>${m.description}</td>
      </tr>`,
    )
    .join("")

  const treatmentRows = (analysis.treatments || [])
    .map(
      (t) => `
      <div class="treatment-item">
        <div class="treatment-name">${t.name} <span class="badge">추천</span></div>
        <div class="treatment-reason">${t.reason}</div>
        <div class="treatment-note">${t.note}</div>
      </div>`,
    )
    .join("")

  const userSection = user
    ? `
    <div class="section">
      <h2>환자 정보</h2>
      <table class="info-table">
        <tr><td>이름</td><td>${user.displayName}</td></tr>
        ${user.gender ? `<tr><td>성별</td><td>${user.gender === "male" ? "남성" : "여성"}</td></tr>` : ""}
        ${user.age ? `<tr><td>나이</td><td>${user.age}세</td></tr>` : ""}
        ${user.hasAllergy && user.allergyDetails ? `<tr><td>알레르기</td><td>${user.allergyDetails}</td></tr>` : ""}
        ${user.hasDisease && user.diseaseDetails ? `<tr><td>질환 이력</td><td>${user.diseaseDetails}</td></tr>` : ""}
        ${user.skinTreatmentHistory ? `<tr><td>시술 이력</td><td>${user.skinTreatmentHistory}</td></tr>` : ""}
      </table>
    </div>`
    : ""

  const notesSection = notes?.trim()
    ? `
    <div class="section">
      <h2>메모</h2>
      <div class="notes-box">${notes.replace(/\n/g, "<br>")}</div>
    </div>`
    : ""

  const photoSection = capturedImageDataUrl
    ? `
    <div class="section photo-section">
      <h2>분석 사진</h2>
      <img src="${capturedImageDataUrl}" alt="분석 사진" class="analysis-photo" />
    </div>`
    : ""

  return `<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>피부 분석 결과지 - SkinAI</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: "Apple SD Gothic Neo", "Malgun Gothic", "Nanum Gothic", sans-serif; color: #1a1a1a; background: #fff; padding: 20mm 15mm; font-size: 11pt; }
  .header { text-align: center; border-bottom: 2px solid #7c3aed; padding-bottom: 12px; margin-bottom: 20px; }
  .header h1 { font-size: 20pt; color: #7c3aed; margin-bottom: 4px; }
  .header .subtitle { color: #666; font-size: 9pt; }
  .score-box { display: flex; align-items: center; gap: 20px; background: #f5f3ff; border-radius: 12px; padding: 16px 20px; margin-bottom: 16px; }
  .score-circle { width: 80px; height: 80px; border-radius: 50%; background: conic-gradient(#7c3aed ${(analysis.overallScore || 0) * 3.6}deg, #e5e7eb 0deg); display: flex; align-items: center; justify-content: center; position: relative; flex-shrink: 0; }
  .score-inner { width: 60px; height: 60px; border-radius: 50%; background: #f5f3ff; display: flex; flex-direction: column; align-items: center; justify-content: center; }
  .score-value { font-size: 16pt; font-weight: 700; color: #7c3aed; line-height: 1; }
  .score-label { font-size: 7pt; color: #666; }
  .score-info h2 { font-size: 14pt; margin-bottom: 4px; }
  .score-info p { color: #666; font-size: 9pt; }
  .concerns { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
  .concern-tag { background: #ede9fe; color: #6d28d9; border-radius: 100px; padding: 2px 10px; font-size: 8pt; }
  .section { margin-bottom: 20px; }
  .section h2 { font-size: 12pt; font-weight: 700; color: #374151; border-left: 3px solid #7c3aed; padding-left: 8px; margin-bottom: 10px; }
  table { width: 100%; border-collapse: collapse; font-size: 9.5pt; }
  table th { background: #f3f4f6; padding: 6px 8px; text-align: left; font-weight: 600; border-bottom: 1px solid #e5e7eb; }
  table td { padding: 6px 8px; border-bottom: 1px solid #f3f4f6; }
  .info-table td:first-child { font-weight: 600; width: 25%; color: #374151; }
  .status-양호 { color: #16a34a; font-weight: 600; }
  .status-보통 { color: #7c3aed; font-weight: 600; }
  .status-관리-필요 { color: #d97706; font-weight: 600; }
  .status-집중-관리 { color: #dc2626; font-weight: 600; }
  .treatment-item { background: #f9fafb; border-radius: 8px; padding: 10px 12px; margin-bottom: 8px; }
  .treatment-name { font-weight: 700; font-size: 10.5pt; margin-bottom: 4px; }
  .badge { background: #d1fae5; color: #065f46; font-size: 7.5pt; padding: 1px 6px; border-radius: 100px; font-weight: 600; }
  .treatment-reason { color: #4b5563; font-size: 9pt; margin-bottom: 3px; }
  .treatment-note { color: #6b7280; font-size: 8.5pt; font-style: italic; }
  .notes-box { background: #fefce8; border: 1px solid #fde68a; border-radius: 8px; padding: 12px; font-size: 10pt; line-height: 1.6; white-space: pre-wrap; }
  .analysis-photo { max-width: 180px; border-radius: 12px; border: 1px solid #e5e7eb; }
  .footer { text-align: center; font-size: 8pt; color: #9ca3af; border-top: 1px solid #e5e7eb; padding-top: 10px; margin-top: 24px; }
  @media print { body { padding: 0; } }
</style>
</head>
<body>
  <div class="header">
    <h1>피부 분석 결과지</h1>
    <div class="subtitle">SkinAI · AI 기반 피부 분석 서비스 · ${date}</div>
  </div>

  <div class="score-box">
    <div class="score-circle">
      <div class="score-inner">
        <div class="score-value">${analysis.overallScore || 0}</div>
        <div class="score-label">점</div>
      </div>
    </div>
    <div class="score-info">
      <h2>${analysis.skinType || "피부 타입 분석"}</h2>
      <p>AI 분석 결과를 기반으로 종합 평가하였습니다.</p>
      <div class="concerns">
        ${(analysis.concerns || []).map((c) => `<span class="concern-tag">${c}</span>`).join("")}
      </div>
    </div>
  </div>

  ${userSection}

  ${
    analysis.metrics?.length
      ? `
  <div class="section">
    <h2>피부 지표</h2>
    <table>
      <thead><tr><th>항목</th><th>점수</th><th>상태</th><th>설명</th></tr></thead>
      <tbody>${metricsRows}</tbody>
    </table>
  </div>`
      : ""
  }

  ${
    analysis.treatments?.length
      ? `
  <div class="section">
    <h2>추천 시술</h2>
    ${treatmentRows}
  </div>`
      : ""
  }

  ${
    analysis.recommendations?.length
      ? `
  <div class="section">
    <h2>관리 추천</h2>
    <table>
      <tbody>
        ${(analysis.recommendations || []).map((r, i) => `<tr><td style="width:24px;color:#7c3aed;font-weight:700;">${i + 1}</td><td>${r}</td></tr>`).join("")}
      </tbody>
    </table>
  </div>`
      : ""
  }

  ${notesSection}
  ${photoSection}

  <div class="footer">
    본 결과지는 AI 분석으로 생성되었으며 의료 진단을 대체하지 않습니다. 실제 시술 여부는 전문가 상담을 통해 결정하세요.
  </div>
</body>
</html>`
}

export function openPdfPreview(options: PdfOptions): void {
  const html = buildPrintHtml(options)
  const win = window.open("", "_blank", "width=800,height=900")
  if (!win) return
  win.document.write(html)
  win.document.close()
  win.onload = () => {
    win.focus()
    win.print()
  }
}

export function getPdfHtml(options: PdfOptions): string {
  return buildPrintHtml(options)
}
