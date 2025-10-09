# Script para probar descarga con autenticación
Write-Host "Iniciando sesión..." -ForegroundColor Green
$loginResponse = curl -X POST http://localhost:8080/ServicioSOAP -H "Content-Type: text/xml;charset=UTF-8" -d '@iniciarSesion_request.xml' -s

# Extraer token
$tokenMatch = [regex]::Match($loginResponse, '<token>(.*?)</token>')
if ($tokenMatch.Success) {
    $token = $tokenMatch.Groups[1].Value
    Write-Host "Token obtenido: $token" -ForegroundColor Yellow
    
    # Crear archivo temporal con token
    $content = Get-Content "descargarArchivo_request.xml" -Raw
    $contentWithToken = $content -replace 'TOKEN_PLACEHOLDER', $token
    $contentWithToken | Out-File -FilePath "descargarArchivo_temp.xml" -Encoding UTF8
    
    # Descargar archivo
    Write-Host "Descargando archivo..." -ForegroundColor Cyan
    $downloadResponse = curl -X POST http://localhost:8080/ServicioSOAP -H "Content-Type: text/xml;charset=UTF-8" -d '@descargarArchivo_temp.xml' -s
    
    Write-Host "Respuesta de descarga:" -ForegroundColor Green
    Write-Host $downloadResponse
    
    # Limpiar archivo temporal
    Remove-Item "descargarArchivo_temp.xml"
    
} else {
    Write-Host "Error: No se pudo extraer el token" -ForegroundColor Red
    Write-Host "Respuesta de login: $loginResponse"
}