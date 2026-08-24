<#
.SYNOPSIS
  Prepara il telefono fisico per una sessione di sviluppo: inoltra le porte del PC sul
  telefono con `adb reverse` e verifica che la configurazione sia coerente.

.DESCRIPTION
  Va rilanciato A OGNI SESSIONE. Gli inoltri di `adb reverse` vivono nel server adb e
  nella singola connessione col dispositivo: si perdono staccando il cavo, riavviando il
  telefono o riavviando il server adb. Non e' una svista, e' come funziona adb.

  Perche' `adb reverse` e non l'IP della Wi-Fi: passa dal cavo, quindi non dipende
  dall'IP (assegnato in DHCP, cambia di continuo), non dipende dal firewall di Windows
  e non dipende dal fatto che la rete non isoli i client fra loro. Sono esattamente le
  tre cose che fanno perdere piu' tempo.

  Con l'inoltro attivo, `localhost` SUL TELEFONO diventa il PC: valgono quindi i default
  del progetto (`http://localhost:8081/` e `http://localhost:8090`) e `local.properties`
  non deve dichiarare nessun indirizzo. Lo script lo verifica, perche' una riga rimasta
  li' da una sessione "a IP" rompe tutto in silenzio.

.PARAMETER Serial
  Seriale del dispositivo, come lo stampa `adb devices`. Serve solo se ne hai collegato
  piu' di uno (emulatore acceso incluso).

.PARAMETER Install
  Dopo l'inoltro, ricompila e reinstalla la build di debug (`gradlew installDebug`).
  Gli indirizzi finiscono in BuildConfig a build time: se cambi `local.properties` senza
  reinstallare, sul telefono resta l'APK vecchio con i valori vecchi.

.EXAMPLE
  .\scripts\dev-telefono.ps1
  .\scripts\dev-telefono.ps1 -Install
#>

[CmdletBinding()]
param(
    [string] $Serial,
    [switch] $Install
)

$ErrorActionPreference = 'Stop'

$PORTA_BACKEND  = 8081
$PORTA_KEYCLOAK = 8090
$ISSUER_ATTESO  = "http://localhost:$PORTA_KEYCLOAK/realms/travelapp"

$radice   = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $radice 'travelApp_frontEnd'

$problemi = New-Object System.Collections.Generic.List[string]

function Ok     ($m) { Write-Host "  OK    $m" -ForegroundColor Green }
function Avviso ($m) { Write-Host "  NOTA  $m" -ForegroundColor Yellow }
function Ko     ($m) { Write-Host "  KO    $m" -ForegroundColor Red; $script:problemi.Add($m) }
function Dettaglio ($m) { Write-Host "        $m" -ForegroundColor DarkGray }
function Titolo ($m) { Write-Host ''; Write-Host $m -ForegroundColor Cyan }

# --- 1. Trovare adb -------------------------------------------------------------------
# Ordine: PATH, poi ANDROID_HOME/ANDROID_SDK_ROOT, poi sdk.dir di local.properties (dove
# lo scrive Android Studio da solo), poi il percorso di default su Windows.
function Trova-Adb {
    $daPath = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($daPath) { return $daPath.Source }

    $candidati = @()
    foreach ($v in 'ANDROID_HOME', 'ANDROID_SDK_ROOT') {
        $val = [Environment]::GetEnvironmentVariable($v)
        if ($val) { $candidati += (Join-Path $val 'platform-tools\adb.exe') }
    }

    $lp = Join-Path $frontend 'local.properties'
    if (Test-Path $lp) {
        $riga = Select-String -Path $lp -Pattern '^\s*sdk\.dir\s*=\s*(.+)$' | Select-Object -First 1
        if ($riga) {
            # local.properties e' un .properties Java: ':' e '\' sono scritti con l'escape
            $sdk = $riga.Matches[0].Groups[1].Value.Trim().Replace('\:', ':').Replace('\\', '\')
            $candidati += (Join-Path $sdk 'platform-tools\adb.exe')
        }
    }

    $candidati += (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe')

    foreach ($c in $candidati) {
        if ($c -and (Test-Path $c)) { return $c }
    }
    return $null
}

Titolo 'Ricerca di adb'
$adb = Trova-Adb
if (-not $adb) {
    Write-Host '  KO    adb.exe non trovato.' -ForegroundColor Red
    Dettaglio "Installa 'Android SDK Platform-Tools' da Android Studio:"
    Dettaglio 'Settings -> Languages & Frameworks -> Android SDK -> SDK Tools.'
    exit 1
}
Ok $adb

# --- 2. Individuare il dispositivo ----------------------------------------------------
Titolo 'Dispositivo'
$righe = & $adb devices | Select-Object -Skip 1 | Where-Object { $_.Trim() -ne '' }

$autorizzati = @()
foreach ($r in $righe) {
    $campi = $r -split '\s+'
    if ($campi.Count -lt 2) { continue }
    switch ($campi[1]) {
        'device'       { $autorizzati += $campi[0] }
        'unauthorized' { Avviso "$($campi[0]): in attesa che tu accetti il debug USB sul telefono" }
        'offline'      { Avviso "$($campi[0]): offline (stacca e riattacca il cavo)" }
    }
}

if ($autorizzati.Count -eq 0) {
    Write-Host '  KO    Nessun dispositivo autorizzato.' -ForegroundColor Red
    Dettaglio "Collega il telefono via USB, attiva 'Debug USB' nelle Opzioni sviluppatore"
    Dettaglio 'e accetta il prompt che compare sullo schermo.'
    exit 1
}

if ($Serial) {
    if ($autorizzati -notcontains $Serial) {
        Write-Host "  KO    Il dispositivo '$Serial' non risulta collegato." -ForegroundColor Red
        Dettaglio "Collegati: $($autorizzati -join ', ')"
        exit 1
    }
    $bersaglio = $Serial
}
elseif ($autorizzati.Count -gt 1) {
    Write-Host "  KO    Piu' dispositivi collegati: $($autorizzati -join ', ')" -ForegroundColor Red
    Dettaglio 'Rilancia indicando quale usare, es.:'
    Dettaglio ".\scripts\dev-telefono.ps1 -Serial $($autorizzati[0])"
    exit 1
}
else {
    $bersaglio = $autorizzati[0]
}

$modello = (& $adb -s $bersaglio shell getprop ro.product.model).Trim()
Ok "$bersaglio ($modello)"

# --- 3. Inoltro delle porte -----------------------------------------------------------
# E' il cuore dello script: crea SUL TELEFONO un socket in ascolto che inoltra al PC
# attraverso il cavo. Da quel momento, per qualunque app del telefono (browser di
# sistema compreso -- ed e' lui ad aprire la form di login di Keycloak, perche' AppAuth
# usa una Custom Tab e non una WebView) `localhost:8081` e `localhost:8090` sono il
# backend e Keycloak che girano qui.
Titolo 'Inoltro porte (adb reverse)'
foreach ($porta in @($PORTA_BACKEND, $PORTA_KEYCLOAK)) {
    & $adb -s $bersaglio reverse "tcp:$porta" "tcp:$porta" | Out-Null
    if ($LASTEXITCODE -eq 0) { Ok "telefono localhost:$porta -> PC localhost:$porta" }
    else                     { Ko  "inoltro della porta $porta fallito" }
}

# --- 4. I servizi sul PC sono su? -----------------------------------------------------
# L'inoltro riesce anche se dall'altra parte non c'e' nessuno: adb non verifica che la
# porta sul PC sia in ascolto. Senza questo controllo l'errore si scoprirebbe solo
# dall'app, come un generico "impossibile connettersi".
function Porta-Aperta {
    param([int] $Porta)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $esito = $client.BeginConnect('127.0.0.1', $Porta, $null, $null)
        if (-not $esito.AsyncWaitHandle.WaitOne(1500, $false)) { return $false }
        $client.EndConnect($esito)
        return $true
    }
    catch { return $false }
    finally { $client.Close() }
}

Titolo 'Servizi sul PC'
$keycloakSu = Porta-Aperta $PORTA_KEYCLOAK

if (Porta-Aperta $PORTA_BACKEND) {
    Ok "backend in ascolto sulla $PORTA_BACKEND"
}
else {
    Ko "backend NON in ascolto sulla $PORTA_BACKEND"
    Dettaglio "Avvia la run configuration 'main' in IntelliJ."
}

if ($keycloakSu) {
    Ok "Keycloak in ascolto sulla $PORTA_KEYCLOAK"
}
else {
    Ko "Keycloak NON in ascolto sulla $PORTA_KEYCLOAK"
    Dettaglio 'docker compose up -d keycloak'
}

# --- 5. Coerenza degli indirizzi ------------------------------------------------------
# L'host con cui si raggiunge Keycloak finisce nel claim 'iss' del token, e il backend lo
# confronta con OAUTH2_ISSUER_URI. Se i due non coincidono il login RIESCE e poi ogni
# chiamata a /api risponde 401: e' il sintomo piu' difficile da interpretare, perche'
# sembra un problema di autenticazione mentre e' di configurazione.
Titolo 'Coerenza degli indirizzi'

if ($keycloakSu) {
    try {
        $conf = Invoke-RestMethod -TimeoutSec 5 `
            -Uri "http://localhost:$PORTA_KEYCLOAK/realms/travelapp/.well-known/openid-configuration"
        if ($conf.issuer -eq $ISSUER_ATTESO) {
            Ok "issuer di Keycloak: $($conf.issuer)"
        }
        else {
            Ko "Keycloak annuncia issuer '$($conf.issuer)' invece di '$ISSUER_ATTESO'"
            Dettaglio "Metti KEYCLOAK_PUBLIC_URL=http://localhost:$PORTA_KEYCLOAK nel .env,"
            Dettaglio 'poi ricrea il container: docker compose up -d keycloak'
        }
    }
    catch {
        Ko "realm 'travelapp' non raggiungibile su Keycloak: $($_.Exception.Message)"
    }
}

# local.properties non deve fissare nessun indirizzo: con l'inoltro attivo valgono i
# default localhost. Una riga rimasta da una sessione "a IP della Wi-Fi" fa cercare
# all'app un indirizzo che, dal telefono, non risponde.
$lp = Join-Path $frontend 'local.properties'
if (Test-Path $lp) {
    $fissati = @(Select-String -Path $lp -Pattern '^\s*(backend|keycloak)\.base\.url\s*=')
    $fuoriPosto = @($fissati | Where-Object { $_.Line -notmatch 'localhost' })

    if ($fuoriPosto.Count -gt 0) {
        foreach ($f in $fuoriPosto) {
            Ko "local.properties riga $($f.LineNumber): $($f.Line.Trim())"
        }
        Dettaglio 'Con adb reverse quelle righe vanno commentate. Poi ricompila e reinstalla'
        Dettaglio "(rilancia con -Install): gli indirizzi entrano in BuildConfig a build time."
    }
    elseif ($fissati.Count -gt 0) { Ok 'local.properties: indirizzi su localhost' }
    else { Ok 'local.properties: nessun indirizzo fissato (valgono i default localhost)' }
}

# Trappola gia' vista: un OAUTH2_ISSUER_URI avanzato nella run configuration di IntelliJ
# ha la precedenza sul default di application.properties, e un typo li' dentro non da'
# nessun errore all'avvio -- perche' con jwk-set-uri esplicito Spring non fa la discovery
# e usa issuer-uri solo come stringa da confrontare.
$ws = Join-Path $radice '.idea\workspace.xml'
if (Test-Path $ws) {
    $trovato = Select-String -Path $ws -Pattern 'OAUTH2_ISSUER_URI"\s+value="([^"]*)"' | Select-Object -First 1
    if ($trovato) {
        $valore = $trovato.Matches[0].Groups[1].Value
        if ($valore -ne $ISSUER_ATTESO) {
            Ko "run configuration IntelliJ: OAUTH2_ISSUER_URI = '$valore'"
            Dettaglio "Con adb reverse va RIMOSSA: il default di application.properties e' gia'"
            Dettaglio "$ISSUER_ATTESO. Poi riavvia il backend."
        }
        else { Ok 'run configuration IntelliJ: OAUTH2_ISSUER_URI coerente' }
    }
}

# --- 6. Reinstallazione opzionale -----------------------------------------------------
if ($Install) {
    Titolo 'Ricompilazione e reinstallazione'
    Push-Location $frontend
    try {
        & .\gradlew.bat installDebug "-Pandroid.injected.device.serial=$bersaglio"
        if ($LASTEXITCODE -eq 0) { Ok 'APK di debug reinstallato' }
        else                     { Ko 'gradlew installDebug fallito' }
    }
    finally { Pop-Location }
}

# --- Esito ----------------------------------------------------------------------------
Write-Host ''
if ($problemi.Count -eq 0) {
    Write-Host "Pronto: puoi avviare l'app sul telefono." -ForegroundColor Green
    Write-Host 'Rilancia questo script a ogni riconnessione del cavo.' -ForegroundColor DarkGray
    exit 0
}
else {
    Write-Host "$($problemi.Count) problema/i da sistemare (vedi sopra)." -ForegroundColor Red
    exit 1
}
