## Re-vectorize ALL knowledge files (JSON + all Markdown)
## Run from: billing-simulator/scripts/
## Requires: $env:GEMINI_API_KEY to be set

$apiKey = $env:GEMINI_API_KEY
if (-not $apiKey) { Write-Host "ERROR: Set GEMINI_API_KEY env variable first"; exit 1 }
$model = "gemini-embedding-001"
$dims = 768
$baseDir = "$PSScriptRoot\..\src\main\resources\knowledge"

Set-Location $baseDir

function Get-Embedding($text) {
    $uri = "https://generativelanguage.googleapis.com/v1beta/models/${model}:embedContent?key=$apiKey"
    $body = @{ content = @{ parts = @(@{ text = $text }) }; outputDimensionality = $dims } | ConvertTo-Json -Depth 5
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    $r = Invoke-WebRequest -Uri $uri -Method POST -Body $bytes -ContentType "application/json; charset=utf-8" -UseBasicParsing
    $obj = $r.Content | ConvertFrom-Json
    return $obj.embedding.values
}

$articles = @()

# --- Surcharge Rules ---
$rules = Get-Content "surcharge-rules.json" -Raw | ConvertFrom-Json
foreach ($rule in $rules) {
    $content = "Surcharge: $($rule.name). Code: $($rule.code). Description: $($rule.description). Current Rate: $($rule.currentRate). Trigger Rule: $($rule.triggerRule). Business Reason: $($rule.businessReason)."
    $articles += @{ kind = "SURCHARGE"; keyCode = $rule.code; content = $content; sourceDoc = "surcharge-rules.json" }
}

# --- Charge Explanations ---
$explanations = Get-Content "charge-explanations.json" -Raw | ConvertFrom-Json
foreach ($exp in $explanations) {
    $keyCode = $exp.section.ToUpper().Replace(" ", "_")
    $content = "Invoice Section: $($exp.section). Meaning: $($exp.meaning). Details: $($exp.details)."
    $articles += @{ kind = "CHARGE_EXPLANATION"; keyCode = $keyCode; content = $content; sourceDoc = "charge-explanations.json" }
}

# --- Fuel Schedule ---
$weeks = Get-Content "fuel-schedule.json" -Raw | ConvertFrom-Json
$summary = "Fuel Surcharge Schedule (recent weeks):`n"
foreach ($w in $weeks) { $summary += "Week $($w.week): Diesel=$($w.dieselIndex), Ground=$($w.groundFuelPct)%, Air=$($w.airFuelPct)%.`n" }
$articles += @{ kind = "FUEL_SCHEDULE"; keyCode = "WEEKLY_SUMMARY"; content = $summary; sourceDoc = "fuel-schedule.json" }

Write-Host "JSON chunks: $($articles.Count)"

# --- All Markdown files ---
$mdFiles = Get-ChildItem "*.md" | Select-Object -ExpandProperty Name
foreach ($mdFile in $mdFiles) {
    Write-Host "Chunking $mdFile..."
    $text = Get-Content $mdFile -Raw
    $lines = $text -split "`n"
    
    $kind = if ($mdFile -match "Invoice|SHD") { "INVOICE" } elseif ($mdFile -match "Agreement|Letter|Carrier") { "CONTRACT" } elseif ($mdFile -match "Pricing|Grow|Saving") { "PRICING_PROGRAM" } else { "DOCUMENT" }
    $docName = $mdFile.Replace(".md", "").Replace("_", " ")
    
    $currentHeading = "Introduction"
    $currentBody = ""
    $sectionIndex = 0
    
    foreach ($line in $lines) {
        if ($line -match "^#{1,2}\s+(.+)") {
            if ($currentBody.Trim().Length -gt 30) {
                $keyCode = ($currentHeading.ToUpper() -replace '[^A-Z0-9\s]', '').Trim().Replace(" ", "_")
                if ($keyCode.Length -gt 50) { $keyCode = $keyCode.Substring(0, 50) }
                if ($keyCode -eq "") { $keyCode = "SECTION" }
                $keyCode = "${keyCode}_${sectionIndex}"
                $chunkText = "Document: $docName. Section: $currentHeading.`n`n$($currentBody.Trim())"
                $articles += @{ kind = $kind; keyCode = $keyCode; content = $chunkText; sourceDoc = $mdFile }
                $sectionIndex++
            }
            $currentHeading = $Matches[1].Trim()
            $currentBody = ""
        } else {
            $currentBody += "$line`n"
        }
    }
    if ($currentBody.Trim().Length -gt 30) {
        $keyCode = ($currentHeading.ToUpper() -replace '[^A-Z0-9\s]', '').Trim().Replace(" ", "_")
        if ($keyCode.Length -gt 50) { $keyCode = $keyCode.Substring(0, 50) }
        if ($keyCode -eq "") { $keyCode = "SECTION" }
        $keyCode = "${keyCode}_${sectionIndex}"
        $chunkText = "Document: $docName. Section: $currentHeading.`n`n$($currentBody.Trim())"
        $articles += @{ kind = $kind; keyCode = $keyCode; content = $chunkText; sourceDoc = $mdFile }
        $sectionIndex++
    }
    Write-Host "  $sectionIndex sections"
}

Write-Host "`nTotal chunks: $($articles.Count). Embedding all..."

$result = @()
$total = $articles.Count
for ($i = 0; $i -lt $total; $i++) {
    $article = $articles[$i]
    Write-Host "  [$($i+1)/$total] $($article.kind): $($article.keyCode)"
    try {
        $embedding = Get-Embedding $article.content
        $article["embedding"] = @($embedding)
        $article["tokenCount"] = [math]::Floor($article.content.Length / 4)
        $result += $article
    } catch {
        Write-Host "    FAILED: $_" -ForegroundColor Red
    }
    Start-Sleep -Milliseconds 250
}

Write-Host "`nSaving $($result.Count) articles to precomputed-embeddings.json..."
$result | ConvertTo-Json -Depth 10 | Set-Content "precomputed-embeddings.json" -Encoding UTF8
Write-Host "Done! Size: $([math]::Round((Get-Item 'precomputed-embeddings.json').Length / 1024, 1)) KB"
