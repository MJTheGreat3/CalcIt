pipeline {
    agent any

    triggers {
        // GitHub webhook (needs the GitHub plugin and a public Jenkins URL, e.g. via ngrok)
        githubPush()
        // Fallback when the webhook cannot reach Jenkins
        pollSCM('H/5 * * * *')
    }

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    environment {
        // Jenkins runs natively on macOS (Homebrew), so point it at the JDK and Docker CLI
        JAVA_HOME     = '/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home'
        PATH          = "${JAVA_HOME}/bin:/usr/local/bin:/opt/homebrew/bin:${env.PATH}"

        // Docker Hub username/password from the Jenkins credentials store
        // (exposes DOCKERHUB_CREDS_USR and DOCKERHUB_CREDS_PSW)
        DOCKERHUB_CREDS = credentials('dockerhub-cred')
        DOCKERHUB_USER  = "${DOCKERHUB_CREDS_USR}"
        IMAGE_NAME      = "${DOCKERHUB_CREDS_USR}/calcit"
        IMAGE_TAG       = "${BUILD_NUMBER}"

        PROXY_PORT    = '8081'
        NOTIFY_EMAIL  = 'mjzeal2005@gmail.com'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Build') {
            steps {
                sh './mvnw -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh './mvnw -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build \
                        -t "$IMAGE_NAME:$IMAGE_TAG" \
                        -t "$IMAGE_NAME:latest" \
                        .
                '''
            }
        }

        stage('Push') {
            environment {
                // Isolated Docker config so the login never touches the macOS keychain
                DOCKER_CONFIG = "${WORKSPACE}/.docker-jenkins"
            }
            steps {
                sh '''
                    mkdir -p "$DOCKER_CONFIG"
                    echo "$DOCKERHUB_CREDS_PSW" | docker login -u "$DOCKERHUB_CREDS_USR" --password-stdin
                    docker push "$IMAGE_NAME:$IMAGE_TAG"
                    docker push "$IMAGE_NAME:latest"
                '''
            }
            post {
                always {
                    sh 'docker logout || true; rm -rf "$DOCKER_CONFIG"'
                }
            }
        }

        stage('Deploy') {
            steps {
                // Same project name every run, so a re-run updates the running stack in place
                sh '''
                    docker compose -p calcit up -d --remove-orphans --wait --wait-timeout 120
                    docker compose -p calcit ps
                '''
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    curl -fsS "http://127.0.0.1:$PROXY_PORT/actuator/health" | grep -q '"status":"UP"'
                    curl -fsS "http://127.0.0.1:$PROXY_PORT/api/v1/add?a=2&b=3" | grep -q '"result":5.0'
                    echo "Deployment healthy: $IMAGE_NAME:$IMAGE_TAG"
                '''
            }
        }
    }

    post {
        success {
            emailext(
                to: "${NOTIFY_EMAIL}",
                subject: "SUCCESS: ${JOB_NAME} #${BUILD_NUMBER}",
                body: """CalcIt pipeline succeeded.

Image: ${IMAGE_NAME}:${IMAGE_TAG}
Build: ${BUILD_URL}
"""
            )
        }
        failure {
            emailext(
                to: "${NOTIFY_EMAIL}",
                subject: "FAILURE: ${JOB_NAME} #${BUILD_NUMBER}",
                body: """CalcIt pipeline failed.

Console output: ${BUILD_URL}console
""",
                attachLog: true
            )
        }
        always {
            // Remove dangling images left behind by rebuilds
            sh 'docker image prune -f || true'
        }
    }
}
