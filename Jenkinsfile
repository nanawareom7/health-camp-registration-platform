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
// ─── WHY THE PREVIOUS PIPELINE FAILED ────────────────────────────────────────
// Jenkins on Windows runs as a Windows Service and uses a Win32 Job Object to
// track every process spawned during a build. When the pipeline finishes (or is
// aborted) Jenkins calls its "Process Tree Killer" which terminates ALL processes
// that belong to that Job Object — including children started with
// `cmd /c start /b` or `Start-Process -WindowStyle Hidden`.
//
// The Spring Boot app therefore started, bound port 8081, printed "Started", and
// was then killed by Jenkins the moment the pipeline completed, making the port
// unreachable immediately after the pipeline reported SUCCESS.
//
// ─── FIX: wmic process call create ──────────────────────────────────────────
// `wmic process call create "<command>"` creates a new process that is NOT
// associated with the calling process's Job Object.  Jenkins cannot track or
// kill it.  The application survives after the pipeline finishes.
//
// ─── HEALTH CHECK STRATEGY ──────────────────────────────────────────────────
// The application has NO /actuator/health endpoint (spring-boot-starter-actuator
// is absent from pom.xml). The available endpoint is POST /api/v1/registrations
// (requires a body, writes to DB — not suitable for polling).
//
// Health check used: HTTP GET to the root path ( / ).
// Spring Boot returns 404 for an unmapped root path, but that 404 comes from
// the running application's DispatcherServlet — which proves the server is up.
// curl.exe --fail-with-body would reject it; instead we accept any HTTP response
// (including 404) as proof the server is alive, and only treat a connection-
// refused / timeout as a failure.  This avoids side effects and DB writes.
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

                    // ── 4-a  Ensure deployment directory exists ───────────────
                    echo "==> Creating deployment directory (if needed): ${deployDir}"
                    bat "IF NOT EXIST \"${deployDir}\" mkdir \"${deployDir}\""

                    // ── 4-b  Stop any existing instance ──────────────────────
                    //
                    // Reads app.pid written by a previous run of this pipeline.
                    // Only kills processes whose name is java.exe to avoid
                    // terminating unrelated services.  If the port is held by a
                    // non-Java process the pipeline fails with a clear message.
                    echo "==> Stopping any previous instance on port ${deployPort}"
                    powershell """
\$port    = [int]${deployPort}
\$pidFile = '${pidFile}'

# ── Stop by PID file (our own previous launch) ──────────────────────────────
if (Test-Path \$pidFile) {
    \$stored = (Get-Content \$pidFile -Raw).Trim()
    if (\$stored -match '^[0-9]+\$') {
        \$oldPid = [int]\$stored
        \$p = Get-Process -Id \$oldPid -ErrorAction SilentlyContinue
        if (\$p -and \$p.Name -eq 'java') {
            Write-Host "Stopping previous java.exe PID \$oldPid from pid file"
            Stop-Process -Id \$oldPid -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 3
        }
    }
    Remove-Item \$pidFile -Force -ErrorAction SilentlyContinue
}

# ── Cross-check by port (catches processes started outside Jenkins) ──────────
\$listeners = netstat -ano | Select-String ("TCP.*:\$port\\s.*LISTEN")
foreach (\$line in \$listeners) {
    if (\$line.Line -match '([0-9]+)\$') {
        \$netPid = [int]\$Matches[1]
        \$proc = Get-Process -Id \$netPid -ErrorAction SilentlyContinue
        if (\$proc -and \$proc.Name -eq 'java') {
            Write-Host "Stopping java.exe PID \$netPid on port \$port"
            Stop-Process -Id \$netPid -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
        } elseif (\$proc) {
            Write-Error "Port \$port is held by '\$(\$proc.Name)' PID \$netPid — NOT a Java process. Aborting to avoid killing an unrelated service."
            exit 1
        }
    }
}
Write-Host "Port \$port is clear."
"""

                    // ── 4-c  Copy the freshly built JAR ──────────────────────
                    echo "==> Copying JAR to deployment directory"
                    bat "copy /Y \"${jarSrc}\" \"${jarDest}\""

                    // ── 4-d  Launch as a Jenkins-detached process ─────────────
                    //
                    // ROOT CAUSE OF PREVIOUS FAILURE:
                    // Jenkins on Windows runs as a Windows Service and assigns a
                    // Win32 Job Object to every build.  Any process started with
                    // `Start-Process` or `cmd /c start /b` inherits that Job
                    // Object and is killed by Jenkins' Process Tree Killer when
                    // the pipeline finishes — even if the step has already returned.
                    //
                    // FIX: `wmic process call create` launches the new process
                    // outside the calling process's Job Object.  Jenkins cannot
                    // track or terminate it.  The JVM survives after the pipeline.
                    //
                    // DB credentials are read from Jenkins environment variables.
                    // They are never stored in this file.
                    echo "==> Launching Spring Boot application (detached from Jenkins Job Object)"
                    powershell """
\$deployDir  = '${deployDir}'
\$jarDest    = '${jarDest}'
\$logFile    = '${logFile}'
\$logFileErr = '${logFileErr}'
\$pidFile    = '${pidFile}'
\$port       = ${deployPort}

# Build the argument list for java -jar
\$args = "--server.port=\$port"
if (\$env:DB_URL)      { \$args += " --spring.datasource.url=\$(\$env:DB_URL)" }
if (\$env:DB_USERNAME) { \$args += " --spring.datasource.username=\$(\$env:DB_USERNAME)" }
if (\$env:DB_PASSWORD) { \$args += " --spring.datasource.password=\$(\$env:DB_PASSWORD)" }

# Rotate previous log so each run has a clean log to inspect
if (Test-Path \$logFile)    { Move-Item \$logFile    "\$logFile.prev"    -Force }
if (Test-Path \$logFileErr) { Move-Item \$logFileErr "\$logFileErr.prev" -Force }

# wmic process call create spawns outside the Jenkins Job Object.
# The process survives after the pipeline finishes.
# Quoting: wmic requires the entire command as a single string.
# cmd /c "java -jar ..." lets the shell handle redirection.
\$wmicCmd = 'cmd /c java -jar "' + \$jarDest + '" ' + \$args + ' 1>"' + \$logFile + '" 2>"' + \$logFileErr + '"'
Write-Host "Launching via wmic: \$wmicCmd"

\$result = (wmic process call create \$wmicCmd) | Out-String
Write-Host \$result

# Extract PID from wmic output ("ProcessId = <n>")
if (\$result -match 'ProcessId = ([0-9]+)') {
    \$newPid = \$Matches[1].Trim()
    \$newPid | Out-File \$pidFile -Encoding ASCII -Force
    Write-Host "Process launched with PID \$newPid — recorded to app.pid"
} else {
    throw "wmic did not return a ProcessId. Launch may have failed. wmic output: \$result"
}
"""

                    // ── 4-e  Bounded health-check ─────────────────────────────
                    //
                    // No /actuator/health exists (actuator not on classpath).
                    // Strategy: HTTP GET to / with curl.exe.
                    //   - The embedded Tomcat returns 404 for an unmapped root path.
                    //   - Any HTTP response (including 404) means the server is up.
                    //   - curl.exe exit code 0 means a response was received.
                    //   - curl.exe exit code 7 means connection refused (not up yet).
                    // Additionally: verify the java PID is still alive each iteration
                    // to fail fast if the process crashed.
                    echo "==> Waiting up to ${env.STARTUP_WAIT}s for the application to accept HTTP requests..."
                    powershell """
\$port      = [int]${deployPort}
\$maxWait   = [int]${env.STARTUP_WAIT}
\$interval  = 3
\$elapsed   = 0
\$logFile   = '${logFile}'
\$logErr    = '${logFileErr}'
\$pidFile   = '${pidFile}'
\$ready     = \$false

# Read the PID we just wrote
\$appPid = 0
if (Test-Path \$pidFile) {
    \$raw = (Get-Content \$pidFile -Raw).Trim()
    if (\$raw -match '^[0-9]+\$') { \$appPid = [int]\$raw }
}

while (\$elapsed -lt \$maxWait) {
    Start-Sleep -Seconds \$interval
    \$elapsed += \$interval

    # Fail fast: if the JVM has already exited, no point waiting
    if (\$appPid -gt 0) {
        \$alive = Get-Process -Id \$appPid -ErrorAction SilentlyContinue
        if (-not \$alive) {
            Write-Host "Process PID \$appPid has exited — application crashed."
            break
        }
    }

    # Attempt an HTTP GET to the root path.
    # Any HTTP status (including 404 from DispatcherServlet) means Tomcat is up.
    # curl.exe exit code 7 = connection refused; 0 = response received.
    \$curlOut = & curl.exe -s -o NUL -w "%{http_code}" --max-time 4 --connect-timeout 3 "http://localhost:\$port/" 2>&1
    if (\$LASTEXITCODE -eq 0 -and \$curlOut -match '^[0-9]{3}\$') {
        Write-Host "  HTTP \$curlOut received from http://localhost:\$port/ — server is up."
        \$ready = \$true
        break
    }

    Write-Host "  ... (\$elapsed / \$maxWait s) — last curl exit=\$LASTEXITCODE response='\$curlOut'"
}

if (-not \$ready) {
    Write-Host ""
    Write-Host "══════════════════════════════════════════════"
    Write-Host " APPLICATION FAILED TO BECOME REACHABLE"
    Write-Host "══════════════════════════════════════════════"
    Write-Host "--- stdout log (last 60 lines) ---"
    if (Test-Path \$logFile) { Get-Content \$logFile -Tail 60 }
    Write-Host "--- stderr log (last 30 lines) ---"
    if (Test-Path \$logErr)  { Get-Content \$logErr  -Tail 30 }
    throw "Application did not respond on http://localhost:\$port within \$maxWait seconds."
}

# Re-read PID (may differ from wmic if app respawned)
Write-Host ""
Write-Host "══════════════════════════════════════════════"
Write-Host " DEPLOYMENT SUCCESSFUL"
Write-Host " Base URL : http://localhost:\$port/"
Write-Host " API      : http://localhost:\$port/api/v1/registrations"
Write-Host " Log      : ${logFile}"
Write-Host "══════════════════════════════════════════════"
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
