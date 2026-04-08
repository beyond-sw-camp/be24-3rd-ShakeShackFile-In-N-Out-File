pipeline {
  agent {
    kubernetes {
      label "gradle-kaniko-helm-${UUID.randomUUID().toString()}"
      defaultContainer 'gradle'
      yaml """
apiVersion: v1
kind: Pod
spec:
  restartPolicy: Never
  serviceAccountName: jenkins-deployer
  containers:
    - name: gradle
      image: gradle:8.9-jdk17
      command: ['cat']
      tty: true
      volumeMounts:
        - name: gradle-cache
          mountPath: /home/gradle/.gradle
    - name: kaniko
      image: gcr.io/kaniko-project/executor:debug
      command: ['/busybox/sh', '-c', 'sleep infinity']
      tty: true
      volumeMounts:
        - name: docker-config
          mountPath: /kaniko/.docker
    - name: helm
      image: alpine/helm:3.14.0
      command: ['cat']
      tty: true
  volumes:
    - name: gradle-cache
      emptyDir: {}
    - name: docker-config
      secret:
        secretName: dockerhub-cred
        items:
          - key: .dockerconfigjson
            path: config.json
"""
    }
  }

  options {
    skipDefaultCheckout(true)
  }

  parameters {
    string(name: 'APP_REPO_URL', defaultValue: 'https://github.com/beyond-sw-camp/be24-3rd-ShakeShackFile-In-N-Out-File.git', description: 'Backend application repository URL.')
    string(name: 'APP_REPO_BRANCH', defaultValue: 'main', description: 'Backend application branch to build.')
    string(name: 'HELM_REPO_URL', defaultValue: 'https://github.com/Lumisia/fullstack_project.git', description: 'Repository that contains the main Helm chart.')
    string(name: 'HELM_REPO_BRANCH', defaultValue: 'main', description: 'Helm repository branch to deploy.')
    string(name: 'HELM_CHART_PATH', defaultValue: 'helm', description: 'Chart path inside the Helm repository.')
    string(name: 'HELM_RELEASE_NAME', defaultValue: 'waffle-release', description: 'Helm release name.')
    string(name: 'K8S_NAMESPACE', defaultValue: 'helm-service', description: 'Kubernetes namespace to deploy into.')
    string(name: 'FRONTEND_IMAGE_REPOSITORY', defaultValue: 'lumisia/frontend', description: 'Frontend image repository used by the Helm chart.')
    string(name: 'FRONTEND_IMAGE_TAG', defaultValue: 'latest', description: 'Frontend image tag used by the Helm chart.')
    string(name: 'WEBSOCKET_IMAGE_REPOSITORY', defaultValue: 'lumisia/websocket-server', description: 'Websocket image repository used by the Helm chart.')
    string(name: 'WEBSOCKET_IMAGE_TAG', defaultValue: 'latest', description: 'Websocket image tag used by the Helm chart.')
    string(name: 'HELM_SECRET_VALUES_CREDENTIAL_ID', defaultValue: '', description: 'Optional Jenkins secret file credential ID for private Helm values.')
    booleanParam(name: 'FORCE_DEPLOY', defaultValue: false, description: 'Deploy even when the Jenkins branch is not main.')
  }

  environment {
    APP_DIR = 'app'
    HELM_SOURCE_DIR = 'helm-source'
    BACKEND_IMAGE_REPOSITORY = 'lumisia/backend'
    BACKEND_IMAGE_TAG = "v${env.BUILD_NUMBER}"
  }

  stages {
    stage('Checkout Sources') {
      steps {
        container('gradle') {
          dir("${env.APP_DIR}") {
            checkout([$class: 'GitSCM',
              branches: [[name: "*/${params.APP_REPO_BRANCH}"]],
              userRemoteConfigs: [[url: params.APP_REPO_URL]]
            ])
          }
          dir("${env.HELM_SOURCE_DIR}") {
            checkout([$class: 'GitSCM',
              branches: [[name: "*/${params.HELM_REPO_BRANCH}"]],
              userRemoteConfigs: [[url: params.HELM_REPO_URL]]
            ])
          }
        }
      }
    }

    stage('Gradle Build') {
      steps {
        container('gradle') {
          dir("${env.APP_DIR}") {
            sh '''
              set -eu
              chmod +x ./gradlew || true
              ./gradlew --no-daemon clean bootJar -x test

              mkdir -p build/docker

              JAR_PATH="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"
              if [ -z "$JAR_PATH" ]; then
                echo "Boot jar not found in build/libs" >&2
                exit 1
              fi

              cp "$JAR_PATH" build/docker/app.jar
              ls -lh build/docker/app.jar
            '''
          }
        }
      }
    }

    stage('Helm Sanity Check') {
      steps {
        container('helm') {
          script {
            withEnv([
              "CHART_DIR=${env.WORKSPACE}/${env.HELM_SOURCE_DIR}/${params.HELM_CHART_PATH}",
              "DEPLOY_NAMESPACE=${params.K8S_NAMESPACE}",
              "RELEASE_NAME=${params.HELM_RELEASE_NAME}"
            ]) {
              sh '''
                set -eu

                RENDERED_FILE="$(mktemp)"
                trap 'rm -f "$RENDERED_FILE"' EXIT

                helm lint "$CHART_DIR"
                helm template "$RELEASE_NAME" "$CHART_DIR" --namespace "$DEPLOY_NAMESPACE" > "$RENDERED_FILE"

                grep -q '^kind: Rollout$' "$RENDERED_FILE"
                grep -q "name: $RELEASE_NAME-wafflebear-backend" "$RENDERED_FILE"
                grep -q "activeService: $RELEASE_NAME-wafflebear-backend" "$RENDERED_FILE"
                grep -q '^kind: StatefulSet$' "$RENDERED_FILE"
                grep -q 'name: redis' "$RENDERED_FILE"
                grep -q 'name: redis-sentinel' "$RENDERED_FILE"
              '''
            }
          }
        }
      }
    }

    stage('Kaniko Build & Push') {
      when {
        expression {
          def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: ''
          return params.FORCE_DEPLOY || branch == '' || branch == 'main' || branch == 'origin/main' || branch.endsWith('/main')
        }
      }
      steps {
        container('kaniko') {
          script {
            withEnv([
              "KANIKO_CONTEXT=${env.WORKSPACE}/${env.APP_DIR}",
              "KANIKO_DOCKERFILE=${env.WORKSPACE}/${env.APP_DIR}/CI-CD/Dockerfile",
              "BACKEND_IMAGE=${env.BACKEND_IMAGE_REPOSITORY}:${env.BACKEND_IMAGE_TAG}",
              "BACKEND_IMAGE_LATEST=${env.BACKEND_IMAGE_REPOSITORY}:latest"
            ]) {
              sh '''
                set -eu
                /kaniko/executor \
                  --context="$KANIKO_CONTEXT" \
                  --dockerfile="$KANIKO_DOCKERFILE" \
                  --destination="$BACKEND_IMAGE" \
                  --destination="$BACKEND_IMAGE_LATEST" \
                  --cache=true \
                  --single-snapshot \
                  --use-new-run \
                  --snapshot-mode=redo
              '''
            }
          }
        }
      }
    }

    stage('Deploy Helm Stack') {
      when {
        expression {
          def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: ''
          return params.FORCE_DEPLOY || branch == '' || branch == 'main' || branch == 'origin/main' || branch.endsWith('/main')
        }
      }
      steps {
        container('helm') {
          script {
            withEnv([
              "CHART_DIR=${env.WORKSPACE}/${env.HELM_SOURCE_DIR}/${params.HELM_CHART_PATH}",
              "DEPLOY_NAMESPACE=${params.K8S_NAMESPACE}",
              "RELEASE_NAME=${params.HELM_RELEASE_NAME}",
              "BACKEND_IMAGE_REPO=${env.BACKEND_IMAGE_REPOSITORY}",
              "BACKEND_IMAGE_TAG=${env.BACKEND_IMAGE_TAG}",
              "WEBSOCKET_IMAGE_REPO=${params.WEBSOCKET_IMAGE_REPOSITORY}",
              "WEBSOCKET_IMAGE_TAG=${params.WEBSOCKET_IMAGE_TAG}",
              "FRONTEND_IMAGE_REPO=${params.FRONTEND_IMAGE_REPOSITORY}",
              "FRONTEND_IMAGE_TAG=${params.FRONTEND_IMAGE_TAG}"
            ]) {
              if (params.HELM_SECRET_VALUES_CREDENTIAL_ID?.trim()) {
                withCredentials([file(credentialsId: params.HELM_SECRET_VALUES_CREDENTIAL_ID.trim(), variable: 'HELM_PRIVATE_VALUES')]) {
                  sh '''
                    set -eu
                    helm upgrade --install "$RELEASE_NAME" "$CHART_DIR" \
                      --namespace "$DEPLOY_NAMESPACE" \
                      --create-namespace \
                      -f "$HELM_PRIVATE_VALUES" \
                      --set-string backend.image.repository="$BACKEND_IMAGE_REPO" \
                      --set-string backend.image.tag="$BACKEND_IMAGE_TAG" \
                      --set-string websocket.image.repository="$WEBSOCKET_IMAGE_REPO" \
                      --set-string websocket.image.tag="$WEBSOCKET_IMAGE_TAG" \
                      --set-string frontend.image.repository="$FRONTEND_IMAGE_REPO" \
                      --set-string frontend.image.tag="$FRONTEND_IMAGE_TAG" \
                      --wait --timeout 15m --atomic
                  '''
                }
              } else {
                sh '''
                  set -eu
                  helm upgrade --install "$RELEASE_NAME" "$CHART_DIR" \
                    --namespace "$DEPLOY_NAMESPACE" \
                    --create-namespace \
                    --set-string backend.image.repository="$BACKEND_IMAGE_REPO" \
                    --set-string backend.image.tag="$BACKEND_IMAGE_TAG" \
                    --set-string websocket.image.repository="$WEBSOCKET_IMAGE_REPO" \
                    --set-string websocket.image.tag="$WEBSOCKET_IMAGE_TAG" \
                    --set-string frontend.image.repository="$FRONTEND_IMAGE_REPO" \
                    --set-string frontend.image.tag="$FRONTEND_IMAGE_TAG" \
                    --wait --timeout 15m --atomic
                '''
              }
            }
          }
        }
      }
    }

    stage('Result') {
      steps {
        script {
          def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: '(manual)'
          echo "Backend image: ${env.BACKEND_IMAGE_REPOSITORY}:${env.BACKEND_IMAGE_TAG}"
          echo "Helm release: ${params.HELM_RELEASE_NAME} in namespace ${params.K8S_NAMESPACE}"
          echo "Helm repo: ${params.HELM_REPO_URL}@${params.HELM_REPO_BRANCH}/${params.HELM_CHART_PATH}"
          echo "Branch context: ${branch}"
        }
      }
    }
  }
}
