## Pre-compute embeddings for all knowledge files using Gemini API
## Saves to src/main/resources/knowledge/precomputed-embeddings.json

$apiKey = "AIzaSyBYCIaHhrNyCFNT8E3YTLBnrrl6bbyn2Wg"
$model = "gemini-embedding-001"
$dims = 768
$baseDir = "$PSScriptRoot\..\src\main\resources\knowledge"
$outputFile = "$baseDir\precomputed-embeddings.json"

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
Write-Host "Chunking surcharge-rules.json..."
$rules = Get-Content "$baseDir\surcharge-rules.json" -Raw | ConvertFrom-Json
foreach ($rule in $rules) {
    $content = "Surcharge: $($rule.name). Code: $($rule.code). Description: $($rule.description). Current Rate: $($rule.currentRate). Trigger Rule: $($rule.triggerRule). Business Reason: $($rule.businessReason)."
    $articles += @{ kind = "SURCHARGE"; keyCode = $rule.code; content = $content; sourceDoc = "surcharge-rules.json"; businessReason = $rule.businessReason }
}
Write-Host "  $($rules.Count) surcharge rules"

# --- Charge Explanations ---
Write-Host "Chunking charge-explanations.json..."
$explanations = Get-Content "$baseDir\charge-explanations.json" -Raw | ConvertFrom-Json
foreach ($exp in $explanations) {
    $keyCode = $exp.section.ToUpper().Replace(" ", "_")
    $content = "Invoice Section: $($exp.section). Meaning: $($exp.meaning). Details: $($exp.details)."
    $articles += @{ kind = "CHARGE_EXPLANATION"; keyCode = $keyCode; content = $content; sourceDoc = "charge-explanations.json" }
}
Write-Host "  $($explanations.Count) charge explanations"

# --- Fuel Schedule ---
Write-Host "Chunking fuel-schedule.json..."
$weeks = Get-Content "$baseDir\fuel-schedule.json" -Raw | ConvertFrom-Json
$summary = "Fuel Surcharge Schedule (recent weeks):`n"
foreach ($w in $weeks) {
    $summary += "Week $($w.week): Diesel Index = $($w.dieselIndex), Ground Fuel Surcharge = $($w.groundFuelPct)%, Air Fuel Surcharge = $($w.airFuelPct)%.`n"
}
$summary += "Fuel surcharge percentages are updated weekly based on the national diesel price index."
$articles += @{ kind = "FUEL_SCHEDULE"; keyCode = "WEEKLY_SUMMARY"; content = $summary; sourceDoc = "fuel-schedule.json"; businessReason = "Fuel surcharges offset carrier fuel costs and are adjusted weekly." }
Write-Host "  1 fuel schedule summary"

# --- Markdown Documents ---
$mdFiles = @(
    "Carrier_Services_Agreement_Sample.md",
    "Sanitized_Letter_of_Agreement.md",
    "Save_as_You_Grow_Pricing_Program.md",
    "UPS_Guangdong_Longsys_Letter_of_Agreement.md"
)

foreach ($mdFile in $mdFiles) {
    $filePath = "$baseDir\$mdFile"
    if (-not (Test-Path $filePath)) { Write-Host "  SKIP: $mdFile not found"; continue }
    
    Write-Host "Chunking $mdFile..."
    $text = Get-Content $filePath -Raw
    $lines = $text -split "`n"
    
    $kind = if ($mdFile -match "Agreement|Letter") { "CONTRACT" } elseif ($mdFile -match "Pricing|Grow") { "PRICING_PROGRAM" } else { "DOCUMENT" }
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
    # Last section
    if ($currentBody.Trim().Length -gt 30) {
        $keyCode = ($currentHeading.ToUpper() -replace '[^A-Z0-9\s]', '').Trim().Replace(" ", "_")
        if ($keyCode.Length -gt 50) { $keyCode = $keyCode.Substring(0, 50) }
        if ($keyCode -eq "") { $keyCode = "SECTION" }
        $keyCode = "${keyCode}_${sectionIndex}"
        $chunkText = "Document: $docName. Section: $currentHeading.`n`n$($currentBody.Trim())"
        $articles += @{ kind = $kind; keyCode = $keyCode; content = $chunkText; sourceDoc = $mdFile }
        $sectionIndex++
    }
    Write-Host "  $sectionIndex sections from $mdFile"
}

Write-Host "`nTotal chunks: $($articles.Count)"
Write-Host "Embedding each chunk (768 dimensions)..."

$result = @()
$total = $articles.Count
$i = 0

foreach ($article in $articles) {
    $i++
    Write-Host "  [$i/$total] $($article.kind): $($article.keyCode)..."
    
    try {
        $embedding = Get-Embedding $article.content
        $article["embedding"] = @($embedding)
        $article["tokenCount"] = [math]::Floor($article.content.Length / 4)
        $result += $article
        Write-Host "    OK ($($embedding.Count) dims)"
    } catch {
        Write-Host "    FAILED: $_" -ForegroundColor Red
    }
    
    # Small delay to avoid rate limiting
    Start-Sleep -Milliseconds 200
}

Write-Host "`nSaving $($result.Count) embedded articles to $outputFile..."
$result | ConvertTo-Json -Depth 10 | Set-Content $outputFile -Encoding UTF8
Write-Host "Done! File size: $([math]::Round((Get-Item $outputFile).Length / 1024, 1)) KB"
