pipeline {
  agent any
  stages {
    stage('build') {
      parallel {
        stage('build') {
          steps {
            echo 'build'
          }
        }

        stage('verify') {
          steps {
            echo 'verify'
          }
        }

      }
    }

    stage('deploy to test') {
      parallel {
        stage('deploy to test') {
          steps {
            echo 'deploy to test'
          }
        }

        stage('loadtest') {
          steps {
            echo 'loadtest'
          }
        }

      }
    }

    stage('integration test') {
      steps {
        echo 'integration test'
      }
    }

    stage('deploy to production') {
      steps {
        echo 'deploy to production'
      }
    }

    stage('publish') {
      steps {
        echo 'publish'
      }
    }

    stage('end') {
      steps {
        echo 'end'
      }
    }

  }
}