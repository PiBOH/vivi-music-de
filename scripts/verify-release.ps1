#requires -version 5.1
<#
.SYNOPSIS
    Repeatable release verification for VIVI Music DE on Windows.

.DESCRIPTION
    Checks, in order:

      1. the version the setup carries against version.txt;
      2. the payload of the jpackage app image (JavaFX Web, icudtl.dat, libmpv,
         the innertube and lyrics jars, no database driver);
      3. the packaged WebView, by running VIVIMusic.exe --webview-self-test
         from the image and reading the page back;
      4. the exact setup.exe, installed silently into a temporary folder with
         /NOICONS;
      5. the installed payload, compared file by file with the app image;
      6. the installed app starting (--benchmark-first-frame) on a temporary
         user profile;
      7. the uninstaller, which must remove the program and keep only the
         newest .vivide.backup and device-sync.json;
      8. the kept restore point, by starting the app again on it.

    The test never touches the real installation or the real profile: the
    installer runs into a temporary folder, the app runs with user.home and
    USERPROFILE pointed at a temporary home, and the host's own registration
    (uninstall key, App Paths, Start Menu folder) is snapshotted before the
    install and restored afterwards, also when a stage fails.

    The installer stage needs an elevated shell. Without one the script asks
    Windows for elevation and runs itself again, or stops with an explanation
    when -NoElevate is given.

.PARAMETER Installer
    The setup.exe to verify. Default: build\installer\VIVIMusic-<version>-setup.exe

.PARAMETER AppImage
    The jpackage app image. Default: desktop\build\compose\binaries\main\app\VIVIMusic

.PARAMETER WebViewTimeoutSeconds
    How long the packaged WebView may take to load. Default 120.

.PARAMETER StartupTimeoutSeconds
    How long the installed app may take to paint its first frame. Default 120.

.PARAMETER SkipWebView
    Skip the packaged WebView stage (for a machine with no network).

.PARAMETER SkipInstaller
    Check the app image only. Needs no administrator rights and installs nothing.

.PARAMETER KeepTemp
    Keep the temporary folder, its logs and its frames.

.PARAMETER NoElevate
    Never ask for elevation. The installer stage then fails in a non-elevated
    shell instead of restarting the script.

.PARAMETER ReportPath
    Base path of the report, without an extension. Default:
    build\release-verification-<version>

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\verify-release.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\verify-release.ps1 -SkipInstaller
#>
[CmdletBinding()]
param(
    [string]$Installer,
    [string]$AppImage,
    [int]$WebViewTimeoutSeconds = 120,
    [int]$StartupTimeoutSeconds = 120,
    [switch]$SkipWebView,
    [switch]$SkipInstaller,
    [switch]$KeepTemp,
    [switch]$NoElevate,
    [string]$ReportPath
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# Version, paths and defaults
# ---------------------------------------------------------------------------

$repoRoot = Split-Path -Parent $PSScriptRoot
$versionFile = Join-Path $repoRoot 'version.txt'
if (-not (Test-Path -LiteralPath $versionFile)) { throw "version.txt was not found at $versionFile" }

$versionLines = @()
foreach ($line in Get-Content -LiteralPath $versionFile) {
    $trimmed = $line.Trim()
    if ($trimmed -ne '' -and -not $trimmed.StartsWith('#')) { $versionLines += $trimmed }
}
if ($versionLines.Count -lt 6) { throw 'version.txt does not carry the six release values' }

$mobileVersion = $versionLines[0]
$deVersion = $versionLines[3]
$releaseChannel = $versionLines[5]
$fullVersion = "${mobileVersion}_DE-$deVersion"
if ($releaseChannel -ne 'stable') { $fullVersion = "$fullVersion-$releaseChannel" }

if (-not $Installer) { $Installer = Join-Path $repoRoot "build\installer\VIVIMusic-$fullVersion-setup.exe" }
if (-not $AppImage) { $AppImage = Join-Path $repoRoot 'desktop\build\compose\binaries\main\app\VIVIMusic' }
if (-not $ReportPath) { $ReportPath = Join-Path $repoRoot "build\release-verification-$fullVersion" }

$Installer = [IO.Path]::GetFullPath($Installer)
$AppImage = [IO.Path]::GetFullPath($AppImage)
$ReportPath = [IO.Path]::GetFullPath($ReportPath)

# ---------------------------------------------------------------------------
# Elevation: the setup writes machine-wide registration, so it needs admin
# ---------------------------------------------------------------------------

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin -and -not $SkipInstaller) {
    if ($NoElevate) {
        throw 'The installer stage needs an elevated shell. Start PowerShell as administrator, or drop -NoElevate so the script can ask Windows for elevation.'
    }
    Write-Host 'The installer stage needs administrator rights. Asking Windows for elevation...' -ForegroundColor Yellow
    $forward = @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-File', ('"' + $PSCommandPath + '"'),
        '-Installer', ('"' + $Installer + '"'),
        '-AppImage', ('"' + $AppImage + '"'),
        '-WebViewTimeoutSeconds', $WebViewTimeoutSeconds,
        '-StartupTimeoutSeconds', $StartupTimeoutSeconds,
        '-NoElevate',
        '-ReportPath', ('"' + $ReportPath + '"')
    )
    if ($SkipWebView) { $forward += '-SkipWebView' }
    if ($KeepTemp) { $forward += '-KeepTemp' }

    $powershell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
    $child = Start-Process -FilePath $powershell -Verb RunAs -Wait -PassThru -ArgumentList ($forward -join ' ')
    $code = 1
    if (Test-Path -LiteralPath "$ReportPath.txt") {
        $reportText = @(Get-Content -LiteralPath "$ReportPath.txt")
        $reportText | Write-Host
        # The elevated child's own exit code is not readable through
        # Start-Process here, so the report it wrote is the answer.
        if ($reportText -match '^RESULT: PASS$') { $code = 0 }
    } elseif ($child -ne $null -and $child.ExitCode -ne $null) {
        $code = $child.ExitCode
    }
    exit $code
}

# ---------------------------------------------------------------------------
# Result collection
# ---------------------------------------------------------------------------

$script:results = @()
$script:sha256 = ''
$script:errorMessage = ''
$startedAt = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function ConvertTo-RegPath {
    param([string]$Key)
    if ($Key.StartsWith('HKLM\')) { return 'HKLM:\' + $Key.Substring(5) }
    if ($Key.StartsWith('HKCU\')) { return 'HKCU:\' + $Key.Substring(5) }
    return $Key
}

# reg.exe writes "key not found" to stderr, and PowerShell 5.1 turns redirected
# native stderr into a terminating error under $ErrorActionPreference = 'Stop'.
# Every call therefore goes through Start-Process, which only reports an exit code.
function Invoke-Reg {
    param([string]$Arguments)
    $reg = Join-Path $env:SystemRoot 'System32\reg.exe'
    $process = Start-Process -FilePath $reg -ArgumentList $Arguments -Wait -PassThru -WindowStyle Hidden
    return $process.ExitCode
}

function Invoke-Stage {
    param([string]$Name, [scriptblock]$Body)
    Write-Host ''
    Write-Host "== $Name" -ForegroundColor Cyan
    $watch = [Diagnostics.Stopwatch]::StartNew()
    try {
        $detail = & $Body
        $watch.Stop()
        $seconds = [math]::Round($watch.Elapsed.TotalSeconds, 1)
        $script:results += [pscustomobject]@{
            stage = $Name; ok = $true; seconds = $seconds; detail = [string]$detail
        }
        Write-Host ("PASS {0} ({1}s): {2}" -f $Name, $seconds, $detail) -ForegroundColor Green
    } catch {
        $watch.Stop()
        $seconds = [math]::Round($watch.Elapsed.TotalSeconds, 1)
        $script:results += [pscustomobject]@{
            stage = $Name; ok = $false; seconds = $seconds; detail = $_.Exception.Message
        }
        Write-Host ("FAIL {0} ({1}s): {2}" -f $Name, $seconds, $_.Exception.Message) -ForegroundColor Red
        throw
    }
}

$work = Join-Path ([IO.Path]::GetTempPath()) ("vivi-release-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + "-" + $PID)
$homeDir = Join-Path $work 'home'
$restoredHome = Join-Path $work 'home-restored'
$installDir = Join-Path $work 'install'
$logDir = Join-Path $work 'logs'
$snapshotDir = Join-Path $work 'host-snapshot'
New-Item -ItemType Directory -Force -Path $work, $homeDir, $restoredHome, $installDir, $logDir, $snapshotDir | Out-Null

$installerLog = Join-Path $logDir 'installer.log'
$uninstallLog = Join-Path $logDir 'uninstall.log'
$appPathsKey = 'HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\VIVIMusic.exe'
$startMenu = Join-Path $env:ProgramData 'Microsoft\Windows\Start Menu\Programs\VIVI Music DE'

# ---------------------------------------------------------------------------
# Host registration snapshot: the exact setup is installed on this machine
# too, so its App Paths key, its uninstall entry and its Start Menu folder
# would otherwise be replaced by the temporary test installation.
# ---------------------------------------------------------------------------

$regKeys = @(
    'HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{com.vivi.vivimusic.desktop}_is1',
    'HKLM\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\{com.vivi.vivimusic.desktop}_is1',
    $appPathsKey
)
$keyState = @()
for ($i = 0; $i -lt $regKeys.Count; $i++) {
    $key = $regKeys[$i]
    $backup = Join-Path $snapshotDir "key-$i.reg"
    $existed = Test-Path -LiteralPath (ConvertTo-RegPath $key)
    if ($existed) {
        $code = Invoke-Reg ('export "' + $key + '" "' + $backup + '" /y')
        if ($code -ne 0) { throw "Could not export $key before the test" }
    }
    $keyState += [pscustomobject]@{ Key = $key; Existed = $existed; File = $backup }
}
$appPathsPs = ConvertTo-RegPath $appPathsKey
$appPathsBefore = $null
if (Test-Path -LiteralPath $appPathsPs) {
    $value = (Get-ItemProperty -LiteralPath $appPathsPs -ErrorAction SilentlyContinue).'(default)'
    if ($value) { $appPathsBefore = [string]$value }
}
$startMenuExisted = Test-Path -LiteralPath $startMenu
$startMenuBackup = Join-Path $snapshotDir 'start-menu'
if ($startMenuExisted) {
    Copy-Item -LiteralPath $startMenu -Destination $startMenuBackup -Recurse -Force
}

function Restore-HostRegistration {
    foreach ($entry in $keyState) {
        if ($entry.Existed) {
            Invoke-Reg ('import "' + $entry.File + '"') | Out-Null
        } else {
            Invoke-Reg ('delete "' + $entry.Key + '" /f') | Out-Null
        }
    }
    if ($startMenuExisted) {
        if (Test-Path -LiteralPath $startMenu) {
            Remove-Item -LiteralPath $startMenu -Recurse -Force -ErrorAction SilentlyContinue
        }
        if (Test-Path -LiteralPath $startMenuBackup) {
            Copy-Item -LiteralPath $startMenuBackup -Destination $startMenu -Recurse -Force -ErrorAction SilentlyContinue
        }
    } elseif (Test-Path -LiteralPath $startMenu) {
        Remove-Item -LiteralPath $startMenu -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

function Invoke-Child {
    param(
        [string]$FilePath,
        [string]$Arguments,
        [string]$StandardOut,
        [string]$StandardError,
        [hashtable]$Environment,
        [int]$TimeoutSeconds = 0,
        [string]$WorkingDirectory
    )
    # Start-Process -PassThru leaves ExitCode empty on PowerShell 5.1, so the
    # child is started through .NET: it reports the real exit code and the two
    # streams are drained asynchronously (a full pipe would otherwise deadlock).
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $FilePath
    $info.Arguments = $Arguments
    $info.UseShellExecute = $false
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    if ($WorkingDirectory) { $info.WorkingDirectory = $WorkingDirectory }
    if ($Environment) {
        foreach ($name in $Environment.Keys) {
            $info.EnvironmentVariables[$name] = [string]$Environment[$name]
        }
    }

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $info
    $null = $process.Start()
    $outTask = $process.StandardOutput.ReadToEndAsync()
    $errTask = $process.StandardError.ReadToEndAsync()
    if ($TimeoutSeconds -gt 0) {
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            try { $process.Kill() } catch { }
            throw "$([IO.Path]::GetFileName($FilePath)) did not finish in $TimeoutSeconds s"
        }
    } else {
        $process.WaitForExit()
    }
    $stdoutText = $outTask.Result
    $stderrText = $errTask.Result
    if ($StandardOut) { [IO.File]::WriteAllText($StandardOut, $stdoutText) }
    if ($StandardError) { [IO.File]::WriteAllText($StandardError, $stderrText) }
    return $process.ExitCode
}

function Get-TreeMap {
    param([string]$Root, [switch]$SkipUninstaller)
    $map = @{}
    Get-ChildItem -LiteralPath $Root -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
        $relative = $_.FullName.Substring($Root.Length).TrimStart('\')
        if ($SkipUninstaller -and $relative -like 'unins000.*') { return }
        $map[$relative] = $_.Length
    }
    return $map
}

function Test-ImagePayload {
    param([string]$Image)
    Assert-True (Test-Path -LiteralPath (Join-Path $Image 'VIVIMusic.exe')) "VIVIMusic.exe is missing from $Image"
    $appDir = Join-Path $Image 'app'
    Assert-True (Test-Path -LiteralPath $appDir) "app\ is missing from $Image"
    Assert-True (Test-Path -LiteralPath (Join-Path $Image 'runtime\bin\server\jvm.dll')) 'the jlink runtime is missing (runtime\bin\server\jvm.dll)'

    $jars = @(Get-ChildItem -LiteralPath $appDir -Filter '*.jar' -File)
    Assert-True ($jars.Count -gt 0) 'the image carries no jars'
    $names = @($jars | ForEach-Object { $_.Name })

    Assert-True (@($names | Where-Object { $_ -like '*innertube*' }).Count -gt 0) 'the innertube jar is not packaged'
    Assert-True (@($names | Where-Object { $_ -like '*lyricsProvider*' }).Count -gt 0) 'the lyricsProvider jar is not packaged'
    $sqlite = @($names | Where-Object { $_ -like 'sqlite-*' -or $_ -like '*sqlite-jdbc*' })
    Assert-True ($sqlite.Count -eq 0) ("the image carries a database driver: " + ($sqlite -join ', '))

    $webJar = @($jars | Where-Object { $_.Name -like 'javafx-web-*.jar' })
    Assert-True ($webJar.Count -eq 1) 'the javafx-web jar is not packaged exactly once'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($webJar[0].FullName)
    try {
        $entries = @($zip.Entries | ForEach-Object { $_.FullName })
    } finally {
        $zip.Dispose()
    }
    Assert-True (@($entries | Where-Object { $_ -like '*jfxwebkit.dll' }).Count -gt 0) 'javafx-web carries no jfxwebkit.dll'
    $icu = @(Get-ChildItem -LiteralPath $Image -Recurse -File -Filter 'icudtl.dat')
    Assert-True ($icu.Count -ge 1) 'icudtl.dat is not packaged'

    $mpv = @(Get-ChildItem -LiteralPath $Image -Recurse -File -Filter 'libmpv-2.dll')
    Assert-True ($mpv.Count -eq 1) 'libmpv-2.dll is not packaged exactly once'
    $vulkan = @(Get-ChildItem -LiteralPath $Image -Recurse -File -Filter 'vulkan-1.dll')
    Assert-True ($vulkan.Count -eq 1) 'vulkan-1.dll is not packaged exactly once'

    $files = @(Get-ChildItem -LiteralPath $Image -Recurse -File)
    $bytes = ($files | Measure-Object -Property Length -Sum).Sum
    return ("{0} files, {1} MB, innertube + lyricsProvider + javafx-web + libmpv + vulkan, no sqlite" -f $files.Count, [math]::Round($bytes / 1MB))
}

function Invoke-WebViewSelfTest {
    param([string]$Image, [int]$TimeoutSeconds)
    $exe = Join-Path $Image 'VIVIMusic.exe'
    Assert-True (Test-Path -LiteralPath $exe) "VIVIMusic.exe is missing from $Image"
    $stdout = Join-Path $logDir 'webview.out.txt'
    $stderr = Join-Path $logDir 'webview.err.txt'
    $frame = Join-Path $logDir 'webview-self-test.png'
    $arguments = '--webview-self-test --webview-timeout=' + $TimeoutSeconds + ' --webview-snapshot="' + $frame + '"'
    $code = Invoke-Child -FilePath $exe -Arguments $arguments -StandardOut $stdout -StandardError $stderr `
        -Environment @{ '_JAVA_OPTIONS' = ('-Duser.home="' + $homeDir + '"') } `
        -TimeoutSeconds ($TimeoutSeconds + 60)
    $text = ''
    if (Test-Path -LiteralPath $stdout) { $text = Get-Content -LiteralPath $stdout -Raw }
    $errors = ''
    if (Test-Path -LiteralPath $stderr) { $errors = Get-Content -LiteralPath $stderr -Raw }

    Assert-True ($code -eq 0) "the packaged WebView check exited $code. Output: $text Errors: $errors"
    Assert-True ($text -match 'RESULT: PASS') 'the packaged WebView check did not print RESULT: PASS'
    Assert-True ($text -match 'state=SUCCEEDED') 'the packaged WebView did not reach SUCCEEDED'
    Assert-True ($text -match 'chars=([1-9][0-9]*)') 'the packaged WebView loaded a page with no text'
    Assert-True ($text.IndexOf($Image, [StringComparison]::OrdinalIgnoreCase) -ge 0) 'the WebView did not come from the packaged image'
    Assert-True (Test-Path -LiteralPath $frame) 'the packaged WebView wrote no frame'
    $frameBytes = (Get-Item -LiteralPath $frame).Length
    Assert-True ($frameBytes -gt 10000) "the WebView frame is only $frameBytes bytes"

    $title = ''
    $match = [regex]::Match($text, 'title=(.*?) readyState=')
    if ($match.Success) { $title = $match.Groups[1].Value }
    return "page '$title' rendered in the packaged engine, frame $frameBytes bytes"
}

function Get-RestorePointState {
    # Not $Home: PowerShell reserves that name for the automatic variable.
    param([string]$ProfileHome)
    $vivi = Join-Path $ProfileHome '.vivimusic'
    return [pscustomobject]@{
        Root = $vivi
        Store = (Join-Path $vivi 'device-sync.json')
        NewestBackup = (Join-Path $vivi 'backups\auto_backup_20991231_235959.vivide.backup')
        OldBackup = (Join-Path $vivi 'backups\auto_backup_20200101_000000.vivide.backup')
        AudioCache = (Join-Path $vivi 'audio')
        LogCache = (Join-Path $vivi 'logs')
    }
}

function New-SandboxProfile {
    param([string]$ProfileHome)
    $point = Get-RestorePointState -ProfileHome $ProfileHome
    New-Item -ItemType Directory -Force -Path $point.Root, (Split-Path -Parent $point.NewestBackup), $point.AudioCache, $point.LogCache | Out-Null
    [IO.File]::WriteAllText($point.Store, '{"marker":"vivi-release-verification","localLibrary":{"songs":[]}}')
    [IO.File]::WriteAllText($point.OldBackup, 'old-backup')
    [IO.File]::WriteAllText($point.NewestBackup, 'newest-backup')
    [IO.File]::WriteAllText((Join-Path $point.AudioCache 'cache.bin'), 'audio-cache')
    return $point
}

# ---------------------------------------------------------------------------
# The verification
# ---------------------------------------------------------------------------

$exitCode = 0
try {
    if (-not $SkipInstaller) {
        Invoke-Stage 'installer-metadata' {
            Assert-True (Test-Path -LiteralPath $Installer) "the setup was not found: $Installer"
            $item = Get-Item -LiteralPath $Installer
            $info = $item.VersionInfo
            # Inno's version strings are fixed width and arrive space padded.
            $productName = $info.ProductName.Trim()
            $companyName = $info.CompanyName.Trim()
            $productVersion = $info.ProductVersion.Trim()
            Assert-True ($productName -eq 'VIVI Music DE') "installer ProductName is '$productName'"
            Assert-True ($companyName -eq 'PiBOH') "installer CompanyName is '$companyName'"
            Assert-True ($productVersion -eq $fullVersion) "installer ProductVersion is '$productVersion', expected '$fullVersion'"
            $script:sha256 = (Get-FileHash -LiteralPath $Installer -Algorithm SHA256).Hash
            $size = [math]::Round($item.Length / 1MB)
            return ("VIVI Music DE $fullVersion, {0} MB, sha256 {1}" -f $size, $script:sha256)
        } | Out-Null
    }

    Invoke-Stage 'image-payload' {
        return (Test-ImagePayload -Image $AppImage)
    } | Out-Null

    if (-not $SkipWebView) {
        Invoke-Stage 'packaged-webview' {
            return (Invoke-WebViewSelfTest -Image $AppImage -TimeoutSeconds $WebViewTimeoutSeconds)
        } | Out-Null
    } else {
        Write-Host 'Skipped packaged-webview (-SkipWebView).' -ForegroundColor Yellow
    }

    if (-not $SkipInstaller) {
        $point = New-SandboxProfile -ProfileHome $homeDir

        Invoke-Stage 'install' {
            $arguments = '/VERYSILENT /SUPPRESSMSGBOXES /NORESTART /SP- /NOICONS /LANG=english /DIR="' +
                $installDir + '" /LOG="' + $installerLog + '"'
            $code = Invoke-Child -FilePath $Installer -Arguments $arguments `
                -StandardOut (Join-Path $logDir 'installer.out.txt') `
                -StandardError (Join-Path $logDir 'installer.err.txt') `
                -Environment @{ 'USERPROFILE' = $homeDir; 'HOME' = $homeDir } `
                -TimeoutSeconds 900
            Assert-True ($code -eq 0 -or $code -eq 3010) "the installer exited $code; log: $installerLog"
            Assert-True (Test-Path -LiteralPath (Join-Path $installDir 'VIVIMusic.exe')) 'the installer produced no VIVIMusic.exe'
            Assert-True (Test-Path -LiteralPath (Join-Path $installDir 'unins000.exe')) 'the installer produced no uninstaller'

            $imageExe = Join-Path $AppImage 'VIVIMusic.exe'
            $installedExe = Join-Path $installDir 'VIVIMusic.exe'
            Assert-True ((Get-FileHash -LiteralPath $installedExe).Hash -eq (Get-FileHash -LiteralPath $imageExe).Hash) 'the installed VIVIMusic.exe differs from the app image'

            $imageMap = Get-TreeMap -Root $AppImage
            $installedMap = Get-TreeMap -Root $installDir -SkipUninstaller
            $missing = @($imageMap.Keys | Where-Object { -not $installedMap.ContainsKey($_) })
            $extra = @($installedMap.Keys | Where-Object { -not $imageMap.ContainsKey($_) })
            $sizeDiff = @($imageMap.Keys | Where-Object {
                $installedMap.ContainsKey($_) -and $installedMap[$_] -ne $imageMap[$_]
            })
            Assert-True ($missing.Count -eq 0) ("files missing after install: " + ($missing -join ', '))
            Assert-True ($extra.Count -eq 0) ("files the image does not carry were installed: " + ($extra -join ', '))
            Assert-True ($sizeDiff.Count -eq 0) ("files installed at a different size: " + ($sizeDiff -join ', '))

            Assert-True (Test-Path -LiteralPath $appPathsPs) 'the installer did not register App Paths'
            $registeredExe = (Get-ItemProperty -LiteralPath $appPathsPs -ErrorAction SilentlyContinue).'(default)'
            Assert-True ($registeredExe -eq $installedExe) "App Paths points at '$registeredExe' instead of the temporary install"
            return ("temporary install, $($installedMap.Count) files identical to the image, App Paths registered")
        } | Out-Null

        Invoke-Stage 'startup' {
            $exe = Join-Path $installDir 'VIVIMusic.exe'
            $stdout = Join-Path $logDir 'startup.out.txt'
            $stderr = Join-Path $logDir 'startup.err.txt'
            $code = Invoke-Child -FilePath $exe -Arguments '--benchmark-first-frame' `
                -StandardOut $stdout -StandardError $stderr -WorkingDirectory $installDir `
                -Environment @{
                    'USERPROFILE' = $homeDir
                    'HOME' = $homeDir
                    '_JAVA_OPTIONS' = ('-Duser.home="' + $homeDir + '"')
                } `
                -TimeoutSeconds $StartupTimeoutSeconds
            $text = ''
            if (Test-Path -LiteralPath $stdout) { $text = Get-Content -LiteralPath $stdout -Raw }
            $errors = ''
            if (Test-Path -LiteralPath $stderr) { $errors = Get-Content -LiteralPath $stderr -Raw }
            Assert-True ($code -eq 0) "the installed app exited $code. Output: $text Errors: $errors"
            Assert-True ($text -match 'TIME_TO_FIRST_FRAME:\s*([0-9]+)ms') 'the installed app did not print TIME_TO_FIRST_FRAME'
            $ms = [regex]::Match($text, 'TIME_TO_FIRST_FRAME:\s*([0-9]+)ms').Groups[1].Value
            Assert-True (Test-Path -LiteralPath $point.Store) 'the sandbox profile is not the one the app used'
            return "installed app painted its first frame in $ms ms"
        } | Out-Null

        Invoke-Stage 'uninstall' {
            $uninstaller = Join-Path $installDir 'unins000.exe'
            $arguments = '/VERYSILENT /SUPPRESSMSGBOXES /NORESTART /LOG="' + $uninstallLog + '"'
            $code = Invoke-Child -FilePath $uninstaller -Arguments $arguments `
                -StandardOut (Join-Path $logDir 'uninstall.out.txt') `
                -StandardError (Join-Path $logDir 'uninstall.err.txt') `
                -Environment @{ 'USERPROFILE' = $homeDir; 'HOME' = $homeDir } `
                -TimeoutSeconds 300
            Assert-True ($code -eq 0) "the uninstaller exited $code; log: $uninstallLog"

            $deadline = (Get-Date).AddSeconds(180)
            while ((Test-Path -LiteralPath (Join-Path $installDir 'unins000.exe')) -and (Get-Date) -lt $deadline) {
                Start-Sleep -Milliseconds 500
            }
            Assert-True (-not (Test-Path -LiteralPath (Join-Path $installDir 'VIVIMusic.exe'))) 'the uninstaller left the app in place'
            Assert-True (-not (Test-Path -LiteralPath (Join-Path $installDir 'unins000.exe'))) 'the uninstaller left itself in place'
            Assert-True (-not (Test-Path -LiteralPath $appPathsPs)) 'the uninstaller left the App Paths key behind'

            Assert-True (Test-Path -LiteralPath $point.Store) 'the uninstaller removed device-sync.json'
            Assert-True (Test-Path -LiteralPath $point.NewestBackup) 'the newest backup did not survive the uninstall'
            Assert-True (-not (Test-Path -LiteralPath $point.OldBackup)) 'an older backup survived the uninstall'
            Assert-True (-not (Test-Path -LiteralPath $point.AudioCache)) 'a cache directory survived the uninstall'
            Assert-True (-not (Test-Path -LiteralPath $point.LogCache)) 'the logs directory survived the uninstall'
            $kept = @(Get-ChildItem -LiteralPath $point.Root -Recurse -File)
            return ("program removed, restore point kept: $($kept.Count) file(s), newest .vivide.backup + device-sync.json")
        } | Out-Null

        Invoke-Stage 'restore' {
            $restored = Get-RestorePointState -ProfileHome $restoredHome
            New-Item -ItemType Directory -Force -Path $restored.Root, (Split-Path -Parent $restored.NewestBackup) | Out-Null
            Copy-Item -LiteralPath $point.Store -Destination $restored.Store -Force
            Copy-Item -LiteralPath $point.NewestBackup -Destination $restored.NewestBackup -Force
            $parsed = Get-Content -LiteralPath $restored.Store -Raw | ConvertFrom-Json
            Assert-True ($parsed.marker -eq 'vivi-release-verification') 'the kept store lost its marker'

            $exe = Join-Path $AppImage 'VIVIMusic.exe'
            $stdout = Join-Path $logDir 'restore.out.txt'
            $stderr = Join-Path $logDir 'restore.err.txt'
            $code = Invoke-Child -FilePath $exe -Arguments '--benchmark-first-frame' `
                -StandardOut $stdout -StandardError $stderr -WorkingDirectory $AppImage `
                -Environment @{
                    'USERPROFILE' = $restoredHome
                    'HOME' = $restoredHome
                    '_JAVA_OPTIONS' = ('-Duser.home="' + $restoredHome + '"')
                } `
                -TimeoutSeconds $StartupTimeoutSeconds
            $text = ''
            if (Test-Path -LiteralPath $stdout) { $text = Get-Content -LiteralPath $stdout -Raw }
            Assert-True ($code -eq 0) "the app exited $code on the restored profile"
            Assert-True ($text -match 'TIME_TO_FIRST_FRAME') 'the app did not start on the restored profile'
            Assert-True (Test-Path -LiteralPath $restored.Store) 'the restored store disappeared'
            return 'the kept backup and store start the app again'
        } | Out-Null
    } else {
        Write-Host 'Skipped install, startup, uninstall and restore (-SkipInstaller).' -ForegroundColor Yellow
    }
} catch {
    $exitCode = 1
    $script:errorMessage = $_.Exception.Message
} finally {
    # Never leave the temporary installation registered on this machine.
    try {
        $leftoverUninstaller = Join-Path $installDir 'unins000.exe'
        if (Test-Path -LiteralPath $leftoverUninstaller) {
            Write-Host 'Cleaning up the temporary installation...' -ForegroundColor Yellow
            Invoke-Child -FilePath $leftoverUninstaller `
                -Arguments ('/VERYSILENT /SUPPRESSMSGBOXES /NORESTART /LOG="' + $uninstallLog + '"') `
                -StandardOut (Join-Path $logDir 'cleanup.out.txt') `
                -StandardError (Join-Path $logDir 'cleanup.err.txt') `
                -Environment @{ 'USERPROFILE' = $homeDir; 'HOME' = $homeDir } `
                -TimeoutSeconds 300 | Out-Null
            $deadline = (Get-Date).AddSeconds(180)
            while ((Test-Path -LiteralPath $leftoverUninstaller) -and (Get-Date) -lt $deadline) {
                Start-Sleep -Milliseconds 500
            }
        }
    } catch {
        Write-Warning "The temporary installation could not be removed: $($_.Exception.Message)"
    }

    try {
        Restore-HostRegistration
        $registrationOk = $true
        $registrationDetail = 'host registration restored'
        if ($appPathsBefore) {
            $appPathsAfter = $null
            if (Test-Path -LiteralPath $appPathsPs) {
                $appPathsAfter = (Get-ItemProperty -LiteralPath $appPathsPs -ErrorAction SilentlyContinue).'(default)'
            }
            if ($appPathsAfter -ne $appPathsBefore) {
                $registrationOk = $false
                $registrationDetail = "App Paths reads '$appPathsAfter' instead of '$appPathsBefore'"
            }
        }
        if (-not $registrationOk) { $exitCode = 1 }
        $script:results += [pscustomobject]@{
            stage = 'host-registration'
            ok = $registrationOk
            seconds = 0
            detail = $registrationDetail
        }
    } catch {
        $exitCode = 1
        $script:results += [pscustomobject]@{
            stage = 'host-registration'; ok = $false; seconds = 0; detail = $_.Exception.Message
        }
    }

    $result = 'PASS'
    if ($exitCode -ne 0) { $result = 'FAIL' }
    if ($script:errorMessage) { Write-Host ''; Write-Host "FAILED: $($script:errorMessage)" -ForegroundColor Red }

    $jsonPath = "$ReportPath.json"
    $textPath = "$ReportPath.txt"
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $jsonPath) | Out-Null

    $report = [pscustomobject]@{
        version = $fullVersion
        started = $startedAt
        finished = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
        installer = $Installer
        appImage = $AppImage
        sha256 = $script:sha256
        result = $result
        error = $script:errorMessage
        workDir = $work
        stages = $script:results
    }
    $report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

    $lines = @()
    $lines += 'VIVI Music DE release verification'
    $lines += "version:   $fullVersion"
    $lines += "installer: $Installer"
    $lines += "sha256:    $($script:sha256)"
    $lines += "started:   $startedAt"
    $lines += ''
    foreach ($stage in $script:results) {
        $mark = 'PASS'
        if (-not $stage.ok) { $mark = 'FAIL' }
        $lines += ('{0,-18} {1,-5} {2,6}s  {3}' -f $stage.stage, $mark, $stage.seconds, $stage.detail)
    }
    $lines += ''
    $lines += "RESULT: $result"
    if ($script:errorMessage) { $lines += "error:  $($script:errorMessage)" }
    $lines += "report: $textPath"
    $lines += "json:   $jsonPath"
    if ($KeepTemp) { $lines += "temp:   $work" }
    $lines | Set-Content -LiteralPath $textPath -Encoding UTF8

    Write-Host ''
    $lines | ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0) { Write-Host 'RESULT: FAIL' -ForegroundColor Red } else { Write-Host 'RESULT: PASS' -ForegroundColor Green }

    if (-not $KeepTemp) {
        Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
    } else {
        Write-Host "Temporary folder kept: $work" -ForegroundColor Yellow
    }
}

exit $exitCode
