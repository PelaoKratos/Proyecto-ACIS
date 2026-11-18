param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('usuarios', 'operaciones')]
    [string]$Servicio
)
$ErrorActionPreference = 'Stop'
$archivoEntornoAcis = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path -LiteralPath $archivoEntornoAcis)) {
    throw 'Copia .env.example como .env y completa las contraseñas locales.'
}
foreach ($lineaAcis in Get-Content -LiteralPath $archivoEntornoAcis) {
    if ($lineaAcis.Trim() -eq '' -or $lineaAcis.TrimStart().StartsWith('#')) { continue }
    $parAcis = $lineaAcis.Split('=', 2)
    if ($parAcis.Length -ne 2 -or $parAcis[0].Trim() -notmatch '^ACIS_[A-Z0-9_]+$') {
        throw 'Formato no válido en .env. Usa ACIS_VARIABLE=valor.'
    }
    $nombreAcis = $parAcis[0].Trim()
    $valorAcis = $parAcis[1].Trim()
    if ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($nombreAcis, 'Process'))) {
        [Environment]::SetEnvironmentVariable($nombreAcis, $valorAcis, 'Process')
    }
}
$moduloAcis = "$Servicio-service"
$jarAcis = Join-Path $PSScriptRoot "$moduloAcis/target/$moduloAcis-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path -LiteralPath $jarAcis)) { throw 'Primero ejecuta .\mvnw.cmd verify desde backend.' }
if ([string]::IsNullOrEmpty($env:JAVA_HOME)) { throw 'Configura JAVA_HOME con un JDK 17 o compatible.' }
$javaAcis = Join-Path $env:JAVA_HOME 'bin/java.exe'
& $javaAcis -jar $jarAcis
exit $LASTEXITCODE
