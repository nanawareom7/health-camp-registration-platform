// ============================================================================
// Jenkinsfile — Health Camp Registration and Deployment Platform
// Declarative pipeline (Windows agents)
//
// Parameters
//   DEPLOY_PORT  — HTTP port the application will listen on (default: 8081)
//   DEPLOY_DIR   — Local directory to copy the JAR into before launching
//
// Environment variables to configure in Jenkins (Manage Jenkins → Credentials
// or Global properties → Environment variables). Never hard-coded here:
//   DB_URL        — JDBC connection string for MySQL
//   DB_USERNAME   — Database username
//   DB_PASSWORD   — Database password
//
// ─── PROCESS SURVIVAL ON WINDOWS AGENTS ─────────────────────────────────────
// Jenkins on Windows terminates processes spawned during builds when a build
// finishes via its Process Tree Killer. To ensure the deployed background
// Spring Boot service survives past the completion of the Jenkins pipeline:
//   1. JENKINS_NODE_COOKIE and BUILD_ID are set to 'dontKillMe' in the Deploy
//      stage environment and passed into PowerShell.
//   2. PowerShell's `Start-Process` is executed with `-PassThru`, working directory,
//      and redirected stdout/stderr to start an independent background process.
//
// ─── HEALTH CHECK STRATEGY ──────────────────────────────────────────────────
// The application exposes REST endpoints under /api/v1/registrations.
// Spring Boot DispatcherServlet responds to HTTP requests on this path.
// During startup, curl.exe polls http://localhost:<port>/api/v1/registrations.
// Any valid HTTP response code (100–599) confirms that Tomcat has bound the port
// and the DispatcherServlet is operational.
// ============================================================================

pipeline {

    agent any

    parameters {
        string(
            name: 'DEPLOY_PORT',
            defaultValue: '8081',
            description: 'HTTP port on which the application will listen after deployment.'
        )
        string(
            name: 'DEPLOY_DIR',
            defaultValue: 'C:\\Jenkins\\deployments\\health-camp',
            description: 'Local Windows directory where the JAR will be copied and run from.'
        )
    }

    environment {
        JAR_NAME     = 'health-camp-registration-platform-0.0.1-SNAPSHOT.jar'
        JAR_PATH     = "target\\${JAR_NAME}"
        STARTUP_WAIT = '60'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 20, unit: 'MINUTES')
        timestamps()
    }

    stages {

        // ─────────────────────────────────────────────────────────────────────
        // Stage 1 — Checkout
        // ─────────────────────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                echo "==> Checking out branch: ${env.GIT_BRANCH ?: 'configured branch'}"
                checkout scm
                echo "Workspace: ${env.WORKSPACE}"
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Stage 2 — Build (compile + test)
        // ─────────────────────────────────────────────────────────────────────
        stage('Build') {
            steps {
                echo '==> Running mvn clean verify (compiles + JUnit 5 tests against H2)'
                bat 'mvn clean verify --batch-mode --no-transfer-progress'
            }
            post {
                failure {
                    echo 'BUILD FAILED — check Maven output for compile errors or test failures.'
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Stage 3 — Package verification
        // ─────────────────────────────────────────────────────────────────────
        stage('Package') {
            steps {
                echo "==> Verifying JAR exists: ${env.JAR_PATH}"
                bat """
                    IF NOT EXIST "%WORKSPACE%\\${env.JAR_PATH}" (
                        echo ERROR: JAR not found at %WORKSPACE%\\${env.JAR_PATH}
                        exit /b 1
                    )
                    echo JAR verified:
                    dir "%WORKSPACE%\\${env.JAR_PATH}"
                """
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Stage 4 — Deploy
        // ─────────────────────────────────────────────────────────────────────
        stage('Deploy') {
            environment {
                JENKINS_NODE_COOKIE = 'dontKillMe'
                BUILD_ID            = 'dontKillMe'
            }
            steps {
                script {
                    def deployDir  = params.DEPLOY_DIR
                    def deployPort = params.DEPLOY_PORT.trim()
                    def jarName    = env.JAR_NAME
                    def jarSrc     = "${env.WORKSPACE}\\${env.JAR_PATH}"
                    def jarDest    = "${deployDir}\\${jarName}"
                    def logFile    = "${deployDir}\\app.log"
                    def logFileErr = "${deployDir}\\app-err.log"
                    def pidFile    = "${deployDir}\\app.pid"

                    // ── 4-a Ensure deployment directory exists ────────────────
                    echo "==> Creating deployment directory (if needed): ${deployDir}"
                    bat "IF NOT EXIST \"${deployDir}\" mkdir \"${deployDir}\""

                    // ── 4-b Stop any existing instance ───────────────────────
                    //
                    // Checks app.pid and netstat listeners on deployPort.
                    // Only terminates processes positively verified as this application
                    // to prevent killing unrelated system or user services.
                    echo "==> Stopping any previous instance on port ${deployPort}"
                    powershell """
\$port    = [int]${deployPort}
\$pidFile = '${pidFile}'
\$jarName = '${jarName}'

# Stop by recorded PID file if it exists and belongs to Java
if (Test-Path \$pidFile) {
    \$stored = (Get-Content \$pidFile -Raw).Trim()
    if (\$stored -match '^[0-9]+\$') {
        \$oldPid = [int]\$stored
        \$p = Get-Process -Id \$oldPid -ErrorAction SilentlyContinue
        if (\$p -and \$p.Name -eq 'java') {
            Write-Host "Stopping previous java.exe PID \$oldPid from pid file"
            Stop-Process -Id \$oldPid -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
        }
    }
    Remove-Item \$pidFile -Force -ErrorAction SilentlyContinue
}

# Cross-check listening processes on configured port
\$listeners = netstat -ano | Select-String ("TCP.*:\$port\\s.*LISTEN")
foreach (\$line in \$listeners) {
    if (\$line.Line -match '([0-9]+)\$') {
        \$netPid = [int]\$Matches[1]
        \$proc = Get-Process -Id \$netPid -ErrorAction SilentlyContinue
        if (\$proc) {
            \$cim = Get-CimInstance Win32_Process -Filter "ProcessId = \$netPid" -ErrorAction SilentlyContinue
            \$isOurApp = (\$cim -and \$cim.CommandLine -like "*\$jarName*")
            if (\$isOurApp) {
                Write-Host "Stopping verified previous instance java.exe PID \$netPid on port \$port"
                Stop-Process -Id \$netPid -Force -ErrorAction SilentlyContinue
                Start-Sleep -Seconds 2
            } else {
                Write-Error "Port \$port is occupied by PID \$netPid ('\$(\$proc.Name)') which is NOT this application. Aborting to avoid terminating an unrelated process."
                exit 1
            }
        }
    }
}
Write-Host "Port \$port is clear."
"""

                    // ── 4-c Copy the freshly built JAR ───────────────────────
                    echo "==> Copying JAR to deployment directory"
                    bat "copy /Y \"${jarSrc}\" \"${jarDest}\""

                    // ── 4-d Launch Spring Boot application via Start-Process ─
                    echo "==> Launching Spring Boot application via PowerShell Start-Process"
                    powershell """
\$deployDir  = '${deployDir}'
\$jarDest    = '${jarDest}'
\$logFile    = '${logFile}'
\$logFileErr = '${logFileErr}'
\$pidFile    = '${pidFile}'
\$port       = ${deployPort}

# Instruct Jenkins Process Tree Killer to spare this background process
\$env:JENKINS_NODE_COOKIE = 'dontKillMe'
\$env:BUILD_ID            = 'dontKillMe'

# Build argument array for java.exe
\$javaArgs = @(
    '-jar',
    "`"\$jarDest`"",
    "--server.port=\$port"
)
if (\$env:DB_URL)      { \$javaArgs += "--spring.datasource.url=\$(\$env:DB_URL)" }
if (\$env:DB_USERNAME) { \$javaArgs += "--spring.datasource.username=\$(\$env:DB_USERNAME)" }
if (\$env:DB_PASSWORD) { \$javaArgs += "--spring.datasource.password=\$(\$env:DB_PASSWORD)" }

# Rotate previous logs for fresh inspection
if (Test-Path \$logFile)    { Move-Item \$logFile    "\$logFile.prev"    -Force }
if (Test-Path \$logFileErr) { Move-Item \$logFileErr "\$logFileErr.prev" -Force }

Write-Host "Launching application in background:"
Write-Host "  Executable : java"
Write-Host "  Arguments  : \$javaArgs"
Write-Host "  Directory  : \$deployDir"
Write-Host "  Stdout log : \$logFile"
Write-Host "  Stderr log : \$logFileErr"

\$proc = Start-Process -FilePath "java" -ArgumentList \$javaArgs -WorkingDirectory \$deployDir -RedirectStandardOutput \$logFile -RedirectStandardError \$logFileErr -PassThru

if (\$proc -and \$proc.Id) {
    \$newPid = \$proc.Id
    \$newPid | Out-File \$pidFile -Encoding ASCII -Force
    Write-Host "Application successfully started with PID \$newPid (recorded in \$pidFile)"
} else {
    throw "Start-Process failed to launch the application or did not return a valid Process ID."
}
"""

                    // ── 4-e Bounded health-check ──────────────────────────────
                    echo "==> Waiting up to ${env.STARTUP_WAIT}s for application to respond on port ${deployPort}..."
                    powershell """
\$port      = [int]${deployPort}
\$maxWait   = [int]${env.STARTUP_WAIT}
\$interval  = 3
\$elapsed   = 0
\$logFile   = '${logFile}'
\$logErr    = '${logFileErr}'
\$pidFile   = '${pidFile}'
\$ready     = \$false

\$appPid = 0
if (Test-Path \$pidFile) {
    \$raw = (Get-Content \$pidFile -Raw).Trim()
    if (\$raw -match '^[0-9]+\$') { \$appPid = [int]\$raw }
}

while (\$elapsed -lt \$maxWait) {
    Start-Sleep -Seconds \$interval
    \$elapsed += \$interval

    # Fail fast if process terminated prematurely
    if (\$appPid -gt 0) {
        \$alive = Get-Process -Id \$appPid -ErrorAction SilentlyContinue
        if (-not \$alive) {
            Write-Host "Process PID \$appPid has exited - application terminated prematurely."
            break
        }
    }

    # Query real application endpoint
    \$curlOut = & curl.exe -s -o NUL -w "%{http_code}" --max-time 4 --connect-timeout 3 "http://localhost:\$port/api/v1/registrations" 2>&1
    if (\$LASTEXITCODE -eq 0 -and \$curlOut -match '^[1-5][0-9]{2}\$') {
        Write-Host "  HTTP \$curlOut received from http://localhost:\$port/api/v1/registrations - application is active."
        \$ready = \$true
        break
    }

    Write-Host "  ... waiting for application (\$elapsed / \$maxWait s) [last response: exit=\$LASTEXITCODE code='\$curlOut']"
}

if (-not \$ready) {
    Write-Host ""
    Write-Host "=============================================="
    Write-Host " APPLICATION FAILED TO BECOME REACHABLE"
    Write-Host "=============================================="
    Write-Host "--- stdout log (last 60 lines) ---"
    if (Test-Path \$logFile) { Get-Content \$logFile -Tail 60 }
    Write-Host "--- stderr log (last 30 lines) ---"
    if (Test-Path \$logErr)  { Get-Content \$logErr  -Tail 30 }
    throw "Application did not respond on http://localhost:\$port within \$maxWait seconds."
}

Write-Host ""
Write-Host "=============================================="
Write-Host " DEPLOYMENT SUCCESSFUL"
Write-Host " Base URL : http://localhost:\$port/"
Write-Host " API      : http://localhost:\$port/api/v1/registrations"
Write-Host " Log      : ${logFile}"
Write-Host "=============================================="
"""
                }
            }
            post {
                failure {
                    echo 'DEPLOY FAILED — see PowerShell output and application log above for details.'
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline result: ${currentBuild.currentResult}"
        }
        success {
            echo "SUCCESS — Spring Boot running on http://localhost:${params.DEPLOY_PORT}/"
        }
        failure {
            echo 'FAILED — review stage logs above.'
        }
    }
}
