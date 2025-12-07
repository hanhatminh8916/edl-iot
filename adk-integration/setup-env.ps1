# Set environment variables for ADK
$env:GOOGLE_GENAI_USE_VERTEXAI = "FALSE"
$env:GOOGLE_API_KEY = "YOUR_API_KEY_HERE"  # Replace with your actual key
$env:IOT_BACKEND_URL = "https://edl-safework-iot-bf3ee691c9f6.herokuapp.com"

Write-Host "✅ Environment variables set successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "GOOGLE_GENAI_USE_VERTEXAI = $env:GOOGLE_GENAI_USE_VERTEXAI"
Write-Host "IOT_BACKEND_URL = $env:IOT_BACKEND_URL"
Write-Host "GOOGLE_API_KEY = $(if($env:GOOGLE_API_KEY -ne 'YOUR_API_KEY_HERE'){'****'+$env:GOOGLE_API_KEY.Substring($env:GOOGLE_API_KEY.Length-4)}else{'NOT SET'})"
Write-Host ""
Write-Host "📝 Next steps:" -ForegroundColor Yellow
Write-Host "1. mvn clean compile"
Write-Host "2. mvn exec:java -Dexec.mainClass=`"com.google.adk.web.AdkWebServer`" -Dexec.args=`"--adk.agents.source-dir=.`""
Write-Host "3. Open http://localhost:8080"
