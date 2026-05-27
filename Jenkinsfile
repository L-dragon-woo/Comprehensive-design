pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timestamps()
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
        def icon = status == 'SUCCESS' ? '✅' : '❌'
        def payload = groovy.json.JsonOutput.toJson([
          content: "${icon} Jenkins ${env.JOB_NAME} #${env.BUILD_NUMBER} ${status}\n${env.BUILD_URL}"
        ])

        writeFile file: '.discord-payload.json', text: payload

        catchError(buildResult: status, stageResult: 'UNSTABLE') {
          withCredentials([string(credentialsId: 'https://discord.com/api/webhooks/1509240855003922534/7QWmUSaaRN2bBQXa5x3Js2ob5YQAWYIwtRTGaDNk5bojLaX7LmlEvKJszVjzCcDHAvRD', variable: 'DISCORD_WEBHOOK_URL')]) {
            if (isUnix()) {
              sh 'curl -fsS -H "Content-Type: application/json" -d @.discord-payload.json "$DISCORD_WEBHOOK_URL"'
            } else {
              powershell 'Invoke-RestMethod -Uri $env:DISCORD_WEBHOOK_URL -Method Post -ContentType "application/json" -InFile ".discord-payload.json"'
            }
          }
        }
      }
    }
  }
}
