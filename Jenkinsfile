// ============================================================================
// Jenkinsfile — Health Camp Registration and Deployment Platform
// Declarative Pipeline (Windows agents: LocalSystem service account)
//
// Continuous Delivery & Deployment Architecture:
//   Stage 1: Checkout              — SCM pull from branch */development
//   Stage 2: Docker Diagnostic     — Verify Docker CLI and daemon access from LocalSystem
//   Stage 3: Test & Build          — Maven compile, JUnit 5 + Selenium tests, publish Surefire XML
//   Stage 4: Package Verification  — Verify built Spring Boot executable JAR exists
//   Stage 5: Docker Build          — Multi-stage build with immutable tag build-${BUILD_NUMBER}
//   Stage 6: Docker Push           — Authenticate via dockerhub-creds and publish to Docker Hub
//   Stage 7: Deploy (JAR Port 8081)— Preserves original standalone JAR deployment (Task 8)
//   Stage 8: Docker Deploy (8082)  — Deploy container to port 8082 with automated rollback
// ============================================================================

pipeline {

    agent any

    parameters {
        string(
            name: 'DEPLOY_PORT',
            defaultValue: '8081',
            description: 'HTTP port for original standalone Spring Boot JAR deployment.'
        )
        string(
            name: 'DEPLOY_DIR',
            defaultValue: 'C:\\Jenkins\\deployments\\health-camp',
            description: 'Local Windows directory where the standalone JAR is copied and run from.'
        )
        string(
            name: 'DOCKER_PORT',
            defaultValue: '8082',
            description: 'Host port for continuous deployment Docker container.'
        )
        string(
            name: 'DB_USER',
            defaultValue: 'root',
            description: 'MySQL database username.'
        )
    }

    environment {
        // Prepend Docker CLI path for Windows Jenkins service account (LocalSystem)
        DOCKER_BIN_DIR = 'C:\\Users\\Asus\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
        PATH           = "${env.DOCKER_BIN_DIR};${env.PATH}"
        JAR_NAME       = 'health-camp-registration-platform-0.0.1-SNAPSHOT.jar'
        JAR_PATH       = "target\\${JAR_NAME}"
        DOCKER_REPO    = 'nanawareom7/health-camp-registration'
        CONTAINER_NAME = 'health-camp-app-cd'
        STARTUP_WAIT   = '60'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 25, unit: 'MINUTES')
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
        // Stage 2 — Docker Environment Diagnostic
        // ─────────────────────────────────────────────────────────────────────
        stage('Docker Diagnostic') {
            steps {
                echo '==> Verifying Docker CLI path and daemon access from Jenkins LocalSystem context...'
                bat '''
                    @echo off
                    echo [1/3] Checking Docker executable on PATH:
                    where docker
                    if errorlevel 1 (
                        echo ERROR: docker.exe not found on PATH! Verify DOCKER_BIN_DIR configuration.
                        exit /b 1
                    )
                    echo.
                    echo [2/3] Verifying Docker client and daemon engine versions:
                    docker version
                    if errorlevel 1 (
                        echo ERROR: docker version failed! Docker daemon is unreachable from LocalSystem context.
                        exit /b 1
                    )
                    echo.
                    echo [3/3] Verifying Docker system info:
                    docker info
                    if errorlevel 1 (
                        echo ERROR: docker info failed!
                        exit /b 1
                    )
                '''
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Stage 3 — Test & Build (compile + JUnit 5 & Selenium tests)
        // ─────────────────────────────────────────────────────────────────────
        stage('Test & Build') {
            steps {
                echo '==> Running mvn clean verify (compiles + unit & Selenium tests against H2)'
                bat 'mvn clean verify --batch-mode --no-transfer-progress'
            }
            post {
                always {
                    echo '==> Publishing JUnit / Surefire test reports (strict: allowEmptyResults = false)...'
                    junit allowEmptyResults: false, testResults: 'target/surefire-reports/*.xml'
                }
                failure {
                    echo 'BUILD FAILED — Tests failed or compile errors encountered. Halting pipeline to prevent deployment.'
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Stage 4 — Package Verification
        // ─────────────────────────────────────────────────────────────────────
        stage('Package Verification') {
            steps {
                echo "==> Verifying built JAR exists: ${env.JAR_PATH}"
                bat """
                    IF NOT EXIST "%WORKSPACE%\\${env.JAR_PATH}" (
                        echo ERROR: JAR not found at %WORKSPACE%\\${env.JAR_PATH}
                        exit /b 1
                    )
                    dir "%WORKSPACE%\\${env.JAR_PATH}"
                """
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Stage 5 — Docker Build (Ahead of Deployment)
        // ─────────────────────────────────────────────────────────────────────
        stage('Docker Build') {
            steps {
                script {
                    def imageTag  = "${env.DOCKER_REPO}:build-${env.BUILD_NUMBER}"
                    def latestTag = "${env.DOCKER_REPO}:latest"
                    echo "==> Building Docker image: ${imageTag} and ${latestTag}"
                    bat "docker build -t \"${imageTag}\" -t \"${latestTag}\" ."
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Stage 6 — Docker Push (Ahead of Deployment)
        // ─────────────────────────────────────────────────────────────────────
        stage('Docker Push') {
            steps {
                script {
                    def imageTag  = "${env.DOCKER_REPO}:build-${env.BUILD_NUMBER}"
                    def latestTag = "${env.DOCKER_REPO}:latest"
                    echo "==> Authenticating to Docker Hub via credential 'dockerhub-creds'..."
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DH_USER',
                        passwordVariable: 'DH_PASS'
                    )]) {
                        bat '''
                            @echo off
                            echo Logging in to Docker Hub as %DH_USER%...
                            echo %DH_PASS% | docker login -u %DH_USER% --password-stdin
                            if errorlevel 1 (
                                echo ERROR: Docker Hub authentication failed!
                                exit /b 1
                            )
                        '''
                        echo "==> Pushing immutable version: ${imageTag}"
                        bat "docker push \"${imageTag}\""
                        echo "==> Pushing floating tag: ${latestTag}"
                        bat "docker push \"${latestTag}\""
                        bat 'docker logout'
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Stage 7 — Deploy (Standalone JAR Port 8081) — Preserved from Task 8
        // ─────────────────────────────────────────────────────────────────────
        stage('Deploy (JAR Port 8081)') {
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
                    def launchBat  = "${deployDir}\\launch.bat"
                    def logFile    = "${deployDir}\\app.log"
                    def logFileErr = "${deployDir}\\app-err.log"
                    def pidFile    = "${deployDir}\\app.pid"

                    echo "==> Creating deployment directory: ${deployDir}"
                    bat "IF NOT EXIST \"${deployDir}\" mkdir \"${deployDir}\""

                    echo "==> Stopping any previous standalone instance on port ${deployPort}"
                    withEnv([
                        "TARGET_PORT=${deployPort}",
                        "TARGET_PID_FILE=${pidFile}",
                        "TARGET_JAR=${jarName}"
                    ]) {
                        powershell(script: '''
                            $port    = [int]$env:TARGET_PORT
                            $pidFile = $env:TARGET_PID_FILE
                            $jarName = $env:TARGET_JAR

                            if (Test-Path $pidFile) {
                                $stored = (Get-Content $pidFile -Raw).Trim()
                                if ($stored -match '^[0-9]+$') {
                                    $oldPid = [int]$stored
                                    $p = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
                                    if ($p -and $p.Name -eq 'java') {
                                        Write-Host "Stopping previous java.exe PID $oldPid from pid file"
                                        Stop-Process -Id $oldPid -Force -ErrorAction SilentlyContinue
                                        Start-Sleep -Seconds 2
                                    }
                                }
                                Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
                            }

                            $listeners = netstat -ano | Select-String ("TCP.*:$port\\s.*LISTEN")
                            foreach ($line in $listeners) {
                                if ($line.Line -match '([0-9]+)$') {
                                    $netPid = [int]$Matches[1]
                                    $proc = Get-Process -Id $netPid -ErrorAction SilentlyContinue
                                    if ($proc) {
                                        $cim = Get-CimInstance Win32_Process -Filter "ProcessId = $netPid" -ErrorAction SilentlyContinue
                                        if ($cim -and $cim.CommandLine -like "*$jarName*") {
                                            Write-Host "Stopping verified previous instance java.exe PID $netPid on port $port"
                                            Stop-Process -Id $netPid -Force -ErrorAction SilentlyContinue
                                            Start-Sleep -Seconds 2
                                        }
                                    }
                                }
                            }
                            Write-Host "Port $port is clear."
                        ''')
                    }

                    echo "==> Copying JAR to ${jarDest}"
                    bat "copy /Y \"${jarSrc}\" \"${jarDest}\""

                    echo "==> Launching standalone Spring Boot service in background"
                    withEnv([
                        "DEPLOY_DIR=${deployDir}",
                        "JAR_DEST=${jarDest}",
                        "LAUNCH_BAT=${launchBat}",
                        "LOG_FILE=${logFile}",
                        "LOG_ERR=${logFileErr}",
                        "DEPLOY_PORT=${deployPort}"
                    ]) {
                        powershell(script: '''
                            $deployDir  = $env:DEPLOY_DIR
                            $jarDest    = $env:JAR_DEST
                            $launchBat  = $env:LAUNCH_BAT
                            $logFile    = $env:LOG_FILE
                            $logFileErr = $env:LOG_ERR
                            $port       = $env:DEPLOY_PORT

                            if (Test-Path $logFile)    { Move-Item $logFile    "$logFile.prev"    -Force }
                            if (Test-Path $logFileErr) { Move-Item $logFileErr "$logFileErr.prev" -Force }

                            $argsStr = "-jar `"$jarDest`" --server.port=$port"
                            if ($env:DB_URL)      { $argsStr += " --spring.datasource.url=`"$($env:DB_URL)`"" }
                            if ($env:DB_USERNAME) { $argsStr += " --spring.datasource.username=`"$($env:DB_USERNAME)`"" }
                            if ($env:DB_PASSWORD) { $argsStr += " --spring.datasource.password=`"$($env:DB_PASSWORD)`"" }

                            $batScript = "@echo off`r`njava $argsStr > `"$logFile`" 2> `"$logFileErr`""
                            [System.IO.File]::WriteAllText($launchBat, $batScript, [System.Text.Encoding]::ASCII)

                            $proc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c `"$launchBat`"" -WorkingDirectory $deployDir -WindowStyle Hidden -PassThru
                            if (-not ($proc -and $proc.Id)) {
                                throw "Failed to start background launch process."
                            }
                        ''')
                    }

                    echo "==> Bounded health check on http://localhost:${deployPort}/..."
                    withEnv([
                        "DEPLOY_PORT=${deployPort}",
                        "STARTUP_WAIT=${env.STARTUP_WAIT}",
                        "PID_FILE=${pidFile}",
                        "LOG_FILE=${logFile}",
                        "LOG_ERR=${logFileErr}"
                    ]) {
                        powershell(script: '''
                            $port     = [int]$env:DEPLOY_PORT
                            $maxWait  = [int]$env:STARTUP_WAIT
                            $interval = 3
                            $elapsed  = 0
                            $ready    = $false
                            $appPid   = 0

                            while ($elapsed -lt $maxWait) {
                                Start-Sleep -Seconds $interval
                                $elapsed += $interval

                                $listeners = netstat -ano | Select-String ("TCP.*:$port\\s.*LISTEN")
                                foreach ($line in $listeners) {
                                    if ($line.Line -match '([0-9]+)$') {
                                        $netPid = [int]$Matches[1]
                                        if ($netPid -gt 0 -and $netPid -ne $appPid) {
                                            $appPid = $netPid
                                            $appPid | Out-File $env:PID_FILE -Encoding ASCII -Force
                                        }
                                    }
                                }

                                $curlOut = & curl.exe -s -o NUL -w "%{http_code}" --max-time 4 --connect-timeout 3 "http://localhost:$port/" 2>&1
                                if ($LASTEXITCODE -eq 0 -and $curlOut -eq "200") {
                                    Write-Host "  HTTP 200 OK received on port $port — Standalone UI is operational."
                                    $ready = $true
                                    break
                                }
                                Write-Host "  Waiting for standalone app ($elapsed / $maxWait s)..."
                            }

                            if (-not $ready) {
                                Write-Host "--- stdout log ---"
                                if (Test-Path $env:LOG_FILE) { Get-Content $env:LOG_FILE -Tail 50 }
                                Write-Host "--- stderr log ---"
                                if (Test-Path $env:LOG_ERR)  { Get-Content $env:LOG_ERR  -Tail 20 }
                                throw "Standalone app failed readiness check on port $port."
                            }
                        ''')
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Stage 8 — Docker Deploy (Container Port 8082 with Automated Rollback)
        // ─────────────────────────────────────────────────────────────────────
        stage('Docker Deploy (8082)') {
            steps {
                script {
                    def imageTag      = "${env.DOCKER_REPO}:build-${env.BUILD_NUMBER}"
                    def containerName = env.CONTAINER_NAME
                    def hostPort      = params.DOCKER_PORT?.trim() ?: env.DOCKER_PORT
                    def dbUser        = params.DB_USER?.trim() ?: 'root'

                    echo "==> Deploying container '${containerName}' using image '${imageTag}' on host port ${hostPort}..."
                    withCredentials([string(
                        credentialsId: 'mysql-password',
                        variable: 'MYSQL_PWD_SECRET'
                    )]) {
                        withEnv([
                            "DEPLOY_CONTAINER=${containerName}",
                            "DEPLOY_IMAGE=${imageTag}",
                            "DEPLOY_PORT=${hostPort}",
                            "DEPLOY_DB_USER=${dbUser}",
                            "STARTUP_WAIT=${env.STARTUP_WAIT}"
                        ]) {
                            powershell(script: '''
                                $container = $env:DEPLOY_CONTAINER
                                $image     = $env:DEPLOY_IMAGE
                                $port      = [int]$env:DEPLOY_PORT
                                $dbUser    = $env:DEPLOY_DB_USER
                                $maxWait   = [int]$env:STARTUP_WAIT

                                # 1. Record previously running image for automated rollback if needed
                                $prevImage = ""
                                $running = docker ps --filter "name=^${container}$" --format "{{.Image}}"
                                if ($running) {
                                    $prevImage = $running.Trim()
                                    Write-Host "Recorded active previous container image for rollback: $prevImage"
                                    docker stop $container
                                    docker rm $container
                                } else {
                                    # Check exited container with same name
                                    $exited = docker ps -a --filter "name=^${container}$" --format "{{.ID}}"
                                    if ($exited) { docker rm $container }
                                }

                                Write-Host "Launching candidate container '$container' with image '$image' on port $port..."
                                $env:DB_PASSWORD = $env:MYSQL_PWD_SECRET

                                docker run -d `
                                    --name $container `
                                    -p "${port}:8080" `
                                    -e SERVER_PORT=8080 `
                                    -e DB_URL="jdbc:mysql://host.docker.internal:3306/health_camp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
                                    -e "DB_USERNAME=$dbUser" `
                                    -e DB_PASSWORD `
                                    $image

                                $env:DB_PASSWORD = $null

                                if ($LASTEXITCODE -ne 0) {
                                    throw "Failed to launch candidate container."
                                }

                                # 2. Bounded readiness probe on http://localhost:<port>/
                                Write-Host "Verifying candidate container readiness on http://localhost:${port}/..."
                                $interval = 3
                                $elapsed  = 0
                                $ready    = $false

                                while ($elapsed -lt $maxWait) {
                                    Start-Sleep -Seconds $interval
                                    $elapsed += $interval

                                    $curlOut = & curl.exe -s -o NUL -w "%{http_code}" --max-time 4 --connect-timeout 3 "http://localhost:$port/" 2>&1
                                    if ($LASTEXITCODE -eq 0 -and $curlOut -eq "200") {
                                        Write-Host "  HTTP 200 OK received from http://localhost:$port/ — Candidate Registration UI verified operational!"
                                        $ready = $true
                                        break
                                    }
                                    Write-Host "  Waiting for container readiness ($elapsed / $maxWait s)..."
                                }

                                # 3. Rollback handling if candidate fails readiness check
                                if (-not $ready) {
                                    Write-Host ""
                                    Write-Host "=============================================="
                                    Write-Host " CANDIDATE READINESS FAILED — INITIATING ROLLBACK"
                                    Write-Host "=============================================="
                                    Write-Host "--- Candidate Container Logs ---"
                                    docker logs --tail 60 $container

                                    docker stop $container
                                    docker rm $container

                                    if ($prevImage -and $prevImage -ne "") {
                                        Write-Host "Restoring previous working image: $prevImage..."
                                        $env:DB_PASSWORD = $env:MYSQL_PWD_SECRET
                                        docker run -d `
                                            --name $container `
                                            -p "${port}:8080" `
                                            -e SERVER_PORT=8080 `
                                            -e DB_URL="jdbc:mysql://host.docker.internal:3306/health_camp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
                                            -e "DB_USERNAME=$dbUser" `
                                            -e DB_PASSWORD `
                                            $prevImage
                                        $env:DB_PASSWORD = $null
                                        Write-Host "Previous working container restored."
                                    } else {
                                        Write-Host "No previous working image was active to restore."
                                    }

                                    throw "Candidate container failed readiness check on port $port."
                                }

                                Write-Host ""
                                Write-Host "=============================================="
                                Write-Host " DOCKER CD DEPLOYMENT SUCCESSFUL"
                                Write-Host " Container : $container"
                                Write-Host " Image     : $image"
                                Write-Host " URL       : http://localhost:$port/"
                                Write-Host " API       : http://localhost:$port/api/v1/registrations"
                                Write-Host "=============================================="
                            ''')
                        }
                    }
                }
            }
            post {
                failure {
                    echo 'DOCKER DEPLOY FAILED — Candidate failed readiness check. Automated rollback was executed if previous image existed.'
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline completed with result: ${currentBuild.currentResult}"
        }
        success {
            echo "SUCCESS — Standalone (port ${params.DEPLOY_PORT}) and Docker (port ${params.DOCKER_PORT ?: '8082'}) deployments verified."
        }
        failure {
            echo 'FAILED — Review stage logs above.'
        }
    }
}
