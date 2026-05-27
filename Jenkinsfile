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
    }
  }
}
