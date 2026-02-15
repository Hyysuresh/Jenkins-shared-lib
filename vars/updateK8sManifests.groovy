#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'GitHubCreds'
    def gitUserName = config.gitUserName ?: 'Hyysuresh'
    def gitUserEmail = config.gitUserEmail ?: 'sghasal5@gmail.com'
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: 'GitHubCreds',
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // Configure Git
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
        """
        
        // Update deployment manifests with new image tags - using proper Linux sed syntax
        sh """
            # Update frontend application deployment - note the correct image name is hyysuresh/food-delivery-frontend
            sed -i "s|image: hyysuresh/food-delivery-frontend:.*|image: hyysuresh/food-delivery-frontend:${imageTag}|g" ${manifestsPath}/02frontend-deployment.yml
            
            # Update backend application deployment - note the correct image name is hyysuresh/food-delivery-backend
            if [ -f "${manifestsPath}/03backend-deployment.yml" ]; then
                sed -i "s|image: hyysuresh/food-delivery-backend:.*|image: hyysuresh/food-delivery-backend:${imageTag}|g" ${manifestsPath}/03backend-deployment.yml
            fi

            # Update admin application deployment - note the correct image name is hyysuresh/food-delivery-backend
            if [ -f "${manifestsPath}/04admin-deployment.yml" ]; then
                sed -i "s|image: hyysuresh/food-delivery-admin:.*|image: hyysuresh/food-delivery-admin:${imageTag}|g" ${manifestsPath}/04admin-deployment.yml
            fi
            # Ensure ingress is using the correct domain
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: jaatnikafood.food|g" ${manifestsPath}/ingress.yaml
            fi
            
            # Check for changes
            if git diff --quiet; then
                echo "No changes to commit"
            else
                # Commit and push changes
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                
                # Set up credentials for push
                git remote set-url origin https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/Hyysuresh/full-stack-food-delivery.git
                git push origin HEAD:\${GIT_BRANCH}
            fi
        """
    }
}
