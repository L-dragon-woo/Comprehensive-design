$OutputEncoding = [System.Text.UTF8Encoding]::new()
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new()
$sid = $null
Write-Host "SkinAI 상담 콘솔입니다. 종료하려면 exit 입력"
while ($true) {
  $q = Read-Host "You"
  if ($q -in @("exit", "quit")) { break }
  $body = @{ message = $q; sessionId = $sid } | ConvertTo-Json -Compress
  $r = Invoke-RestMethod -Uri "http://localhost:8000/api/chat" -Method Post -ContentType "application/json; charset=utf-8" -Body $body
  $sid = $r.sessionId
  Write-Host "AI> $($r.content)"
}
