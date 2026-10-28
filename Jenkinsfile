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
    // Pipeline-wide environment — references Jenkins credentials by ID.
    // Create a "Secret text" credential in Jenkins for each of the three DB
    // values; set the credential IDs to match the strings below.
    // -------------------------------------------------------------------------
    environment {
        JAR_NAME     = 'health-camp-registration-platform-0.0.1-SNAPSHOT.jar'
        JAR_PATH     = "target\\${JAR_NAME}"
        // Startup wait configuration (seconds)
        STARTUP_WAIT = '60'
    }

    options {
        // Keep the last 10 build logs; discard older ones.
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Abort the build if no progress is made for 15 minutes.
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
                // Jenkins SCM checkout is configured on the job itself (Pipeline
                // from SCM -> Repository URL -> Branch Specifier). The built-in
                // checkout step honours that configuration.
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
                // Fail the pipeline immediately if the JAR is missing.
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
                    def pidFile    = "${deployDir}\\app.pid"

                    // ----------------------------------------------------------
                    // 4-a  Create deployment directory if it does not exist
                    // ----------------------------------------------------------
                    echo "==> Ensuring deployment directory exists: ${deployDir}"
                    bat "IF NOT EXIST \"${deployDir}\" mkdir \"${deployDir}\""

                    // ----------------------------------------------------------
                    // 4-b  Kill any existing instance using the port, or whose
                    //      PID was recorded in app.pid from a prior run.
                    //
                    // NOTE: PowerShell backtick line-continuation is NOT used
                    // inside Groovy triple-quoted strings because Groovy also
                    // treats backslash as an escape character and rejects \`.
                    // All multi-param calls are written on a single line.
                    // ----------------------------------------------------------
                    echo "==> Stopping any existing instance on port ${deployPort}"
                    powershell """
\$port = ${deployPort}
\$pidFile = '${pidFile}'

if (Test-Path \$pidFile) {
    \$storedPid = Get-Content \$pidFile -ErrorAction SilentlyContinue | Select-Object -First 1
    if (\$storedPid -match '^[0-9]+\$') {
        Write-Host "Stopping process from PID file: \$storedPid"
        try { Stop-Process -Id ([int]\$storedPid) -Force -ErrorAction SilentlyContinue; Start-Sleep -Seconds 2 } catch {}
    }
    Remove-Item \$pidFile -Force -ErrorAction SilentlyContinue
}

\$connections = netstat -ano | Select-String ":\$port " | Where-Object { \$_ -match 'LISTENING' }
foreach (\$line in \$connections) {
    if (\$line -match '([0-9]+)\$') {
        \$netPid = [int]\$Matches[1]
        if (\$netPid -gt 0) {
            Write-Host "Killing PID \$netPid listening on port \$port"
            Stop-Process -Id \$netPid -Force -ErrorAction SilentlyContinue
        }
    }
}
Start-Sleep -Seconds 2
Write-Host "Port \$port is now free."
"""

                    // ----------------------------------------------------------
                    // 4-c  Copy the newly built JAR to the deployment directory
                    // ----------------------------------------------------------
                    echo "==> Copying JAR to deployment directory"
                    bat "copy /Y \"${jarSrc}\" \"${jarDest}\""

                    // ----------------------------------------------------------
                    // 4-d  Launch the application as a background process.
                    //      DB credentials are read from the Jenkins environment —
                    //      do NOT hard-code them here.
                    //
                    //      Start-Process is written on one line to avoid the
                    //      Groovy/backslash-escape problem with PowerShell backticks.
                    // ----------------------------------------------------------
                    echo "==> Launching application on port ${deployPort}"
                    powershell """
\$jarDest = '${jarDest}'
\$logFile = '${logFile}'
\$pidFile = '${pidFile}'
\$port    = ${deployPort}

\$dbUrl   = \$env:DB_URL
\$dbUser  = \$env:DB_USERNAME
\$dbPass  = \$env:DB_PASSWORD

\$javaArgs = [System.Collections.Generic.List[string]]::new()
\$javaArgs.Add('-jar')
\$javaArgs.Add(\$jarDest)
\$javaArgs.Add("--server.port=\$port")
if (\$dbUrl)  { \$javaArgs.Add("--spring.datasource.url=\$dbUrl") }
if (\$dbUser) { \$javaArgs.Add("--spring.datasource.username=\$dbUser") }
if (\$dbPass) { \$javaArgs.Add("--spring.datasource.password=\$dbPass") }

\$proc = Start-Process -FilePath 'java' -ArgumentList \$javaArgs -RedirectStandardOutput \$logFile -RedirectStandardError "\$logFile.err" -WorkingDirectory '${deployDir}' -PassThru -WindowStyle Hidden
Write-Host "Application started with PID \$(\$proc.Id)"
\$proc.Id | Out-File \$pidFile -Encoding ASCII
"""

                    // ----------------------------------------------------------
                    // 4-e  Wait for the port to be listening (no health endpoint
                    //      assumed). Poll every 3 s, up to STARTUP_WAIT seconds.
                    // ----------------------------------------------------------
                    echo "==> Waiting up to ${env.STARTUP_WAIT}s for port ${deployPort} to be ready..."
                    powershell """
\$port     = ${deployPort}
\$maxWait  = ${env.STARTUP_WAIT}
\$interval = 3
\$elapsed  = 0
\$logFile  = '${logFile}'
\$ready    = \$false

while (\$elapsed -lt \$maxWait) {
    Start-Sleep -Seconds \$interval
    \$elapsed += \$interval
    \$listening = netstat -ano | Select-String ":\$port " | Where-Object { \$_ -match 'LISTENING' }
    if (\$listening) { \$ready = \$true; break }
    Write-Host "  ... waiting (\$elapsed / \$maxWait s)"
}

if (-not \$ready) {
    Write-Host "=== Last 40 lines of application log ==="
    if (Test-Path \$logFile) { Get-Content \$logFile -Tail 40 }
    Write-Host "=== stderr log ==="
    if (Test-Path "\$logFile.err") { Get-Content "\$logFile.err" -Tail 20 }
    throw "Application did not start within \$maxWait seconds. Port \$port is not listening."
}

Write-Host "SUCCESS: Application is listening on port \$port"
Write-Host "Access URL: http://localhost:\$port"
"""
                }
            }
            post {
                failure {
                    echo 'DEPLOY FAILED — review the PowerShell output and application log above.'
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
            echo "PIPELINE SUCCEEDED. Application is running on port ${params.DEPLOY_PORT} at http://localhost:${params.DEPLOY_PORT}"
        }
        failure {
            echo 'Pipeline FAILED. Review stage logs above for details.'
        }
    }
}
