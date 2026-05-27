pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timestamps()
  }

  triggers {
    pollSCM('H/2 * * * *')
  }

  environment {
    CI = 'true'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Install') {
      steps {
        dir('frontend') {
          script {
            if (isUnix()) {
              sh 'corepack enable'
              sh 'corepack pnpm install --frozen-lockfile'
            } else {
              bat 'corepack enable'
              bat 'corepack pnpm install --frozen-lockfile'
            }
          }
        }
      }
    }

    stage('Lint') {
      steps {
        dir('frontend') {
          script {
            if (isUnix()) {
              sh 'corepack pnpm lint'
            } else {
              bat 'corepack pnpm lint'
            }
          }
        }
      }
    }

    stage('Build') {
      steps {
        dir('frontend') {
          script {
            if (isUnix()) {
              sh 'corepack pnpm build'
            } else {
              bat 'corepack pnpm build'
            }
          }
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'frontend/dist/**', allowEmptyArchive: true
      script {
        def status = currentBuild.currentResult ?: 'UNKNOWN'
        def label = status == 'SUCCESS' ? '[SUCCESS]' : '[FAILED]'
        def payload = groovy.json.JsonOutput.toJson([
          content: "${label} Jenkins ${env.JOB_NAME} #${env.BUILD_NUMBER} ${status}\n${env.BUILD_URL}"
        ])

        writeFile file: '.discord-payload.json', text: payload

        catchError(buildResult: status, stageResult: 'UNSTABLE') {
          withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'DISCORD_WEBHOOK_URL')]) {
            if (isUnix()) {
              sh 'status=$(curl -sS -o /dev/null -w "%{http_code}" -H "Content-Type: application/json" -d @.discord-payload.json "$DISCORD_WEBHOOK_URL"); echo "Discord webhook returned HTTP $status"; test "$status" = "204"'
            } else {
              powershell '''
                $response = Invoke-WebRequest -Uri $env:DISCORD_WEBHOOK_URL -Method Post -ContentType "application/json" -InFile ".discord-payload.json" -UseBasicParsing
                Write-Host "Discord webhook returned HTTP $($response.StatusCode)"
                if ($response.StatusCode -ne 204) {
                  throw "Unexpected Discord webhook status: $($response.StatusCode)"
                }
              '''
            }
          }
        }
      }
    }
  }
}
