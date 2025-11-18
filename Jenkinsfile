// Jenkinsfile for agentInterface deployment to separate backend/frontend VMs
// Place this Jenkinsfile at the repository root. It assumes you will create the following Jenkins Credentials:
//  - SSH credential (Kind: SSH Username with private key) id: deploy-backend-key
//  - SSH credential id: deploy-frontend-key
//  - Secret text credential id: neon-db-url (the JDBC URL)
//  - Username/password credential id: neon-db-creds (DB user + password) OR separate secret text credentials neon-db-user and neon-db-pass
//  - Secret text credential id: jwt-secret
// Adjust credential ids if you use different ones.

pipeline {
  agent any
  options {
    timestamps()
    ansiColor('xterm')
  }
  parameters {
    string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch to build')
    string(name: 'BACKEND_HOST', defaultValue: '3.253.33.66', description: 'Hostname or IP of backend VM (required)')
    string(name: 'FRONTEND_HOST', defaultValue: '"3.250.8.136', description: 'Hostname or IP of frontend VM (required)')
    booleanParam(name: 'SKIP_FRONTEND_BUILD', defaultValue: false, description: 'Skip building frontend (if packaged separately)')
    // Credential IDs provided at job run time (avoid static secret IDs in environment)
    string(name: 'BACKEND_SSH_CRED_ID', defaultValue: 'deploy-backend-key', description: 'Jenkins credential id for backend SSH (SSH Username with private key)')
    string(name: 'FRONTEND_SSH_CRED_ID', defaultValue: 'deploy-frontend-key', description: 'Jenkins credential id for frontend SSH (SSH Username with private key)')
    string(name: 'NEON_DB_URL_CRED_ID', defaultValue: 'neon-db-url', description: 'Jenkins secret text id for Neon JDBC URL')
    string(name: 'NEON_DB_CREDS_ID', defaultValue: 'neon-db-creds', description: 'Jenkins username/password credential id for DB user/pass')
    string(name: 'JWT_SECRET_CRED_ID', defaultValue: 'jwt-secret', description: 'Jenkins secret text id for JWT secret')
    // Non-secret deployment targets supplied as parameters to avoid static env declarations
    string(name: 'BACKEND_DEST_DIR', defaultValue: '/opt/agentinterface/backend', description: 'Destination directory on backend host')
    string(name: 'FRONTEND_DEST_DIR', defaultValue: '/var/www/agentinterface', description: 'Destination directory on frontend host')
    string(name: 'BACKEND_JAR_NAME', defaultValue: 'agentinterface-backend.jar', description: 'Name to use for backend jar on the remote host')
  }



  stages {
    stage('Checkout') {
      steps {
        echo "Checking out branch ${params.BRANCH}"
        checkout([ $class: 'GitSCM', branches: [[name: params.BRANCH]], userRemoteConfigs: [[url: scm.userRemoteConfigs[0].url]] ])
        sh 'git rev-parse --short HEAD > .git-commit'
        script { env.GIT_COMMIT = readFile('.git-commit').trim() }
      }
    }

    stage('Build Backend') {
      steps {
        echo 'Building backend (Maven)'
        sh '''
          chmod +x ./mvnw || true
          ./mvnw -B -DskipTests=true clean package
        '''
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Build Frontend') {
      when { expression { return !params.SKIP_FRONTEND_BUILD } }
      steps {
        echo 'Building frontend (Node)'
        dir('frontend') {
          // Try npm build; agent must have node/npm installed
          sh '''
            if [ -f package.json ]; then
              if command -v npm >/dev/null 2>&1; then
                npm ci || npm install
                npm run build || npm run build:prod || true
              else
                echo "npm is not available on the agent; set SKIP_FRONTEND_BUILD=true or install node/npm"
                exit 1
              fi
            else
              echo "No frontend package.json found; skipping frontend build"
            fi
          '''
        }
        archiveArtifacts artifacts: 'frontend/dist/**', fingerprint: true
      }
    }

    stage('Deploy Backend') {
      when { expression { return params.BACKEND_HOST?.trim() != '' } }
      steps {
        echo "Deploying backend to ${params.BACKEND_HOST}"
        // Fetch required secrets from Jenkins Credentials (DB URL, DB user/pass, JWT secret) and SSH key
        withCredentials([
          sshUserPrivateKey(credentialsId: params.BACKEND_SSH_CRED_ID, keyFileVariable: 'BACKEND_SSH_KEY', usernameVariable: 'BACKEND_SSH_USER'),
          string(credentialsId: params.NEON_DB_URL_CRED_ID, variable: 'NEON_DATABASE_URL'),
          usernamePassword(credentialsId: params.NEON_DB_CREDS_ID, usernameVariable: 'NEON_DB_USER', passwordVariable: 'NEON_DB_PASS'),
          string(credentialsId: params.JWT_SECRET_CRED_ID, variable: 'JWT_SECRET')
        ]) {
          script {
            def jar = sh(script: "ls target/*.jar | head -n 1", returnStdout: true).trim()
            if (!jar) { error 'Backend jar not found in target/' }
            sh "scp -i $BACKEND_SSH_KEY -o StrictHostKeyChecking=no $jar $BACKEND_SSH_USER@${params.BACKEND_HOST}:~/"

            // Remote commands: prepare dir, move jar, write .env, run service
            def remoteCmd = """
              set -e
              sudo mkdir -p ${params.BACKEND_DEST_DIR}
              sudo mv ~/""" + jar.tokenize('/').last() + """ ${params.BACKEND_DEST_DIR}/${params.BACKEND_JAR_NAME}
              sudo chown -R $USER:$USER ${params.BACKEND_DEST_DIR}
              # write env file for the backend
              cat > /tmp/agentinterface-backend.env <<EOD
SPRING_DATASOURCE_URL=${NEON_DATABASE_URL}
SPRING_DATASOURCE_USERNAME=${NEON_DB_USER}
SPRING_DATASOURCE_PASSWORD=${NEON_DB_PASS}
JWT_SECRET=${JWT_SECRET}
SPRING_JPA_HIBERNATE_DDL_AUTO=update
EOD
              sudo mv /tmp/agentinterface-backend.env ${params.BACKEND_DEST_DIR}/.env

              # stop existing process (if any)
              if pgrep -f ${params.BACKEND_JAR_NAME} >/dev/null 2>&1; then
                echo "Stopping existing backend process"
                pkill -f ${params.BACKEND_JAR_NAME} || true
                sleep 2
              fi

              # launch jar in background using nohup; logs to /var/log/agentinterface-backend.log
              sudo nohup java -jar ${params.BACKEND_DEST_DIR}/${params.BACKEND_JAR_NAME} --spring.config.additional-location=${params.BACKEND_DEST_DIR}/.env > /var/log/agentinterface-backend.log 2>&1 &
            """

            // Execute remote command via ssh
            sh "ssh -i $BACKEND_SSH_KEY -o StrictHostKeyChecking=no $BACKEND_SSH_USER@${params.BACKEND_HOST} '${remoteCmd.replace("'","'\\''")}'"
          }
        }
      }
    }

    stage('Deploy Frontend') {
      when { allOf { expression { return !params.SKIP_FRONTEND_BUILD }, expression { return params.FRONTEND_HOST?.trim() != '' } } }
      steps {
        echo "Deploying frontend to ${params.FRONTEND_HOST}"
        withCredentials([
          sshUserPrivateKey(credentialsId: params.FRONTEND_SSH_CRED_ID, keyFileVariable: 'FRONTEND_SSH_KEY', usernameVariable: 'FRONTEND_SSH_USER')
        ]) {
          script {
            // Determine frontend build directory: common dist/ or build/
            def buildDir = ''
            dir('frontend') {
              buildDir = sh(script: 'if [ -d dist ]; then echo dist; elif [ -d build ]; then echo build; else echo ""; fi', returnStdout: true).trim()
            }
            if (!buildDir) { error 'Frontend build output not found (expected frontend/dist or frontend/build)' }

            // Use rsync if available to sync files
            sh "rsync -avz -e \"ssh -i $FRONTEND_SSH_KEY -o StrictHostKeyChecking=no\" frontend/${buildDir}/ ${FRONTEND_SSH_USER}@${params.FRONTEND_HOST}:${params.FRONTEND_DEST_DIR}/"

            // Remote commands: ensure dir exists and reload nginx
            def remoteCmd = """
              set -e
              sudo mkdir -p ${params.FRONTEND_DEST_DIR}
              sudo chown -R $USER:$USER ${params.FRONTEND_DEST_DIR}
              # ensure nginx is installed and reload if present
              if command -v nginx >/dev/null 2>&1; then
                sudo systemctl reload nginx || true
              fi
            """
            sh "ssh -i $FRONTEND_SSH_KEY -o StrictHostKeyChecking=no $FRONTEND_SSH_USER@${params.FRONTEND_HOST} '${remoteCmd.replace("'","'\\''")}'"
          }
        }
      }
    }

    stage('Smoke Tests') {
      steps {
        echo 'Running smoke tests against backend health endpoint'
        script {
          def backendUrl = "http://${params.BACKEND_HOST ?: 'localhost'}:8080/api/health"
          sh "curl -fS --silent ${backendUrl} || (echo 'Health check failed' && exit 1)"
        }
      }
    }
  }

  post {
    success {
      echo "Deployment successful: backend=${params.BACKEND_HOST} frontend=${params.FRONTEND_HOST} commit=${env.GIT_COMMIT}"
    }
    failure {
      echo 'Deployment failed'
    }
  }
}
