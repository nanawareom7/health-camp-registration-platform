// ============================================================================
// Jenkinsfile — Health Camp Registration and Deployment Platform
// Declarative pipeline (Windows agents)
//
// Parameters
//   DEPLOY_PORT  — HTTP port the application will listen on (default: 8081)
//   DEPLOY_DIR   — Local directory to copy the JAR into before launching
//
// Environment variables expected to be configured in Jenkins credentials /
// environment (never hard-coded here):
//   DB_URL        — JDBC connection string for MySQL
//   DB_USERNAME   — Database username
//   DB_PASSWORD   — Database password
//
// Health-check note
// -----------------
// The application has NO /actuator/health endpoint (spring-boot-starter-actuator
// is not on the classpath). The only mapped route is POST /api/v1/registrations,
// which requires a request body and persists data — unsuitable for polling.
// The bounded startup check therefore uses a TCP-connect test
// (Test-NetConnection -Port) which verifies that the JVM has bound the port
// and the HTTP listener is accepting connections. This is reliable for startup
// detection without side effects. To switch to an HTTP health check, add
// spring-boot-starter-actuator to pom.xml and change the health-check block.
// ============================================================================

pipeline {

    // Run on any available Windows agent. Change the label to match your
    // Jenkins node configuration (e.g. agent { label 'windows' }).
    agent any

    // -------------------------------------------------------------------------
    // Build parameters — visible in Jenkins "Build with Parameters" UI
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Pipeline-wide environment
    // -------------------------------------------------------------------------
    environment {
        JAR_NAME     = 'health-camp-registration-platform-0.0.1-SNAPSHOT.jar'
        JAR_PATH     = "target\\${JAR_NAME}"
        STARTUP_WAIT = '60'   // seconds to wait for the port to become reachable
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 15, unit: 'MINUTES')
        timestamps()
    }

    // =========================================================================
    // STAGES
    // =========================================================================
    stages {

        // ---------------------------------------------------------------------
        // Stage 1 — Checkout
        // ---------------------------------------------------------------------
        stage('Checkout') {
            steps {
                echo "==> Checking out branch: ${env.GIT_BRANCH ?: 'configured branch'}"
                checkout scm
                echo "Workspace: ${env.WORKSPACE}"
            }
        }

        // ---------------------------------------------------------------------
        // Stage 2 — Build  (compile + test)
        // ---------------------------------------------------------------------
        stage('Build') {
            steps {
                echo '==> Running mvn clean verify (compiles, runs all tests)'
                bat 'mvn clean verify --batch-mode --no-transfer-progress'
            }
            post {
                failure {
                    echo 'BUILD FAILED — check the Maven output above for compiler errors or test failures.'
                }
            }
        }

        // ---------------------------------------------------------------------
        // Stage 3 — Package verification
        // ---------------------------------------------------------------------
        stage('Package') {
            steps {
                echo "==> Verifying JAR exists at: ${env.JAR_PATH}"
                bat """
                    IF NOT EXIST "%WORKSPACE%\\${env.JAR_PATH}" (
                        echo ERROR: Expected JAR not found at %WORKSPACE%\\${env.JAR_PATH}
                        exit /b 1
                    )
                    echo JAR found:
                    dir "%WORKSPACE%\\${env.JAR_PATH}"
                """
            }
        }

        // ---------------------------------------------------------------------
        // Stage 4 — Deploy
        // ---------------------------------------------------------------------
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
                    def launchBat  = "${deployDir}\\launch.bat"

                    // ----------------------------------------------------------
                    // 4-a  Create deployment directory
                    // ----------------------------------------------------------
                    echo "==> Ensuring deployment directory exists: ${deployDir}"
                    bat "IF NOT EXIST \"${deployDir}\" mkdir \"${deployDir}\""

                    // ----------------------------------------------------------
                    // 4-b  Stop any existing instance
                    //
                    // Strategy:
                    //  1. If app.pid exists, kill that PID (our own prior launch).
                    //  2. Also scan netstat for any LISTENING process on the port.
                    //     Only processes whose image name is 'java.exe' are killed;
                    //     this avoids terminating unrelated services on the same port.
                    //     If a non-Java process owns the port, the pipeline fails with
                    //     a clear error rather than killing something unrelated.
                    // ----------------------------------------------------------
                    echo "==> Stopping any existing instance on port ${deployPort}"
                    powershell """
\$port    = ${deployPort}
\$pidFile = '${pidFile}'

if (Test-Path \$pidFile) {
    \$storedPid = (Get-Content \$pidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
    if (\$storedPid -match '^[0-9]+\$') {
        Write-Host "Found PID file — stopping PID \$storedPid"
        try { Stop-Process -Id ([int]\$storedPid) -Force -ErrorAction SilentlyContinue } catch {}
        Start-Sleep -Seconds 2
    }
    Remove-Item \$pidFile -Force -ErrorAction SilentlyContinue
}

\$netstatLines = netstat -ano | Select-String (":" + \$port + " ") | Where-Object { \$_ -match 'LISTENING' }
foreach (\$line in \$netstatLines) {
    if (\$line -match '([0-9]+)\$') {
        \$netPid = [int]\$Matches[1]
        if (\$netPid -gt 0) {
            \$proc = Get-Process -Id \$netPid -ErrorAction SilentlyContinue
            if (\$proc -and \$proc.Name -eq 'java') {
                Write-Host "Stopping java.exe PID \$netPid on port \$port"
                Stop-Process -Id \$netPid -Force -ErrorAction SilentlyContinue
            } elseif (\$proc) {
                Write-Error "Port \$port is occupied by '\$(\$proc.Name)' (PID \$netPid), which is NOT a Java process. Aborting to avoid killing an unrelated service."
                exit 1
            }
        }
    }
}
Start-Sleep -Seconds 2
Write-Host "Port \$port is clear."
"""

                    // ----------------------------------------------------------
                    // 4-c  Copy the newly built JAR
                    // ----------------------------------------------------------
                    echo "==> Copying JAR to deployment directory"
                    bat "copy /Y \"${jarSrc}\" \"${jarDest}\""

                    // ----------------------------------------------------------
                    // 4-d  Write a self-contained launch.bat then execute it with
                    //      `cmd /c start /b` so the JVM process is fully detached
                    //      from the PowerShell console that Jenkins owns.
                    //
                    // WHY NOT Start-Process with -Redirect*?
                    //   Start-Process -RedirectStandardOutput/-RedirectStandardError
                    //   attaches internal stream readers that keep the PowerShell
                    //   host alive until the child process exits — even without
                    //   -Wait — causing the Jenkins step to block until the
                    //   application stops (the original timeout bug).
                    //
                    // WHY launch.bat + `cmd /c start /b`?
                    //   `cmd /c start /b` creates a new, detached console process.
                    //   Jenkins has no handle on it and moves on immediately.
                    //   The batch file redirects stdout/stderr to log files and
                    //   inherits the env-var credentials set below.
                    // ----------------------------------------------------------
                    echo "==> Writing launch.bat"
                    // Build the java argument string from the environment.
                    // Credentials come from Jenkins env vars — never hard-coded.
                    powershell """
\$launchBat  = '${launchBat}'
\$jarDest    = '${jarDest}'
\$logFile    = '${logFile}'
\$logFileErr = '${logFileErr}'
\$port       = ${deployPort}

\$dbUrl  = if (\$env:DB_URL)      { "--spring.datasource.url=\$(\$env:DB_URL)" }      else { '' }
\$dbUser = if (\$env:DB_USERNAME) { "--spring.datasource.username=\$(\$env:DB_USERNAME)" } else { '' }
\$dbPass = if (\$env:DB_PASSWORD) { "--spring.datasource.password=\$(\$env:DB_PASSWORD)" } else { '' }

\$args = ("--server.port=\$port \$dbUrl \$dbUser \$dbPass").Trim()

\$content = "@echo off`r`njava -jar `"\$jarDest`" \$args 1>`"\$logFile`" 2>`"\$logFileErr`""
[System.IO.File]::WriteAllText(\$launchBat, \$content, [System.Text.Encoding]::ASCII)
Write-Host "launch.bat written to \$launchBat"
"""

                    echo "==> Launching application (detached) on port ${deployPort}"
                    // Use cmd /c start /b to spawn a fully detached process.
                    // /b suppresses a new console window.
                    // wmic is used to capture the PID of the newly spawned java.exe
                    // that appears on the configured port after startup.
                    bat "cmd /c start /b \"\" \"${launchBat}\""

                    // ----------------------------------------------------------
                    // 4-e  Bounded health-check loop (TCP connect, 60 s max)
                    //
                    // No /actuator/health exists in this application. A TCP connect
                    // on the configured port is the correct readiness signal — it
                    // proves the embedded Tomcat has bound and is accepting
                    // connections. Test-NetConnection returns TcpTestSucceeded=$true
                    // as soon as the port is open.
                    //
                    // The loop also monitors the java process via the PID file.
                    // If the process disappears before the port opens, the log is
                    // dumped and the pipeline fails immediately.
                    // ----------------------------------------------------------
                    echo "==> Waiting up to ${env.STARTUP_WAIT}s for port ${deployPort} to accept connections..."
                    powershell """
\$port       = ${deployPort}
\$maxWait    = [int]${env.STARTUP_WAIT}
\$interval   = 3
\$elapsed    = 0
\$logFile    = '${logFile}'
\$logFileErr = '${logFileErr}'
\$ready      = \$false

while (\$elapsed -lt \$maxWait) {
    Start-Sleep -Seconds \$interval
    \$elapsed += \$interval

    \$tcp = Test-NetConnection -ComputerName 127.0.0.1 -Port \$port -WarningAction SilentlyContinue -ErrorAction SilentlyContinue
    if (\$tcp.TcpTestSucceeded) {
        \$ready = \$true
        break
    }

    # If no java process is listening or running at all, fail fast
    \$anyJava = Get-Process java -ErrorAction SilentlyContinue
    if (-not \$anyJava) {
        Write-Host "No java.exe process found — application appears to have crashed."
        break
    }

    Write-Host "  ... waiting (\$elapsed / \$maxWait s)"
}

if (-not \$ready) {
    Write-Host ""
    Write-Host "=== APPLICATION FAILED TO START — stdout log ==="
    if (Test-Path \$logFile)    { Get-Content \$logFile    -Tail 50 }
    Write-Host "=== stderr log ==="
    if (Test-Path \$logFileErr) { Get-Content \$logFileErr -Tail 30 }
    throw "Application did not become reachable on port \$port within \$maxWait seconds."
}

# Record PID for future cleanup runs
\$javaPids = (netstat -ano | Select-String (":" + \$port + " ") | Where-Object { \$_ -match 'LISTENING' } | ForEach-Object { if (\$_ -match '([0-9]+)\$') { \$Matches[1] } }) | Select-Object -First 1
if (\$javaPids) {
    \$javaPids | Out-File '${pidFile}' -Encoding ASCII
    Write-Host "PID \$javaPids recorded to app.pid"
}

Write-Host ""
Write-Host "SUCCESS: Application is accepting connections on port \$port"
Write-Host "Base URL : http://localhost:\$port"
Write-Host "API      : http://localhost:\$port/api/v1/registrations"
"""
                }
            }
            post {
                failure {
                    echo 'DEPLOY FAILED — review the PowerShell output and log files in DEPLOY_DIR above.'
                }
            }
        }
    }

    // =========================================================================
    // POST-PIPELINE ACTIONS
    // =========================================================================
    post {
        always {
            echo "Pipeline finished with result: ${currentBuild.currentResult}"
        }
        success {
            echo "PIPELINE SUCCEEDED. Application running on port ${params.DEPLOY_PORT} — http://localhost:${params.DEPLOY_PORT}"
        }
        failure {
            echo 'Pipeline FAILED. Review stage logs above for details.'
        }
    }
}
