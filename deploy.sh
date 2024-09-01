#!/bin/bash

# Abilita l'uscita immediata in caso di errore
set -e

# Configurazioni
JAR_FILE="videoconference-p2p-0.0.1-SNAPSHOT.jar"
DOCKER_IMAGE_NAME="videoconference-p2p"
VM_HOST="vagrant@localhost"
VM_PORT="2222"  
SSH_KEY_PATH="/home/andreadelorenzis/Desktop/infrastruttura_progetto_SD/.vagrant/machines/default/virtualbox/private_key"
APP_DIR="/home/vagrant/app"
NGINX_CONF_PATH="/etc/nginx/sites-available/default"

# 1) Build dell'applicazion
echo "Building the Java application..."
mvn clean package -DskipTests

# 2) Creazione immagine Docker
echo "Building Docker image..."
sudo docker build -t $DOCKER_IMAGE_NAME .

# 3) Copia immagine docker
sudo docker save $DOCKER_IMAGE_NAME | ssh -i $SSH_KEY_PATH -p $VM_PORT $VM_HOST "sudo docker load"

# Step 4: Eseguire l'applicazione nella VM
ssh -i $SSH_KEY_PATH -p $VM_PORT $VM_HOST << EOF
  set -e
  echo "Stopping old Docker container..."
  sudo docker stop $DOCKER_IMAGE_NAME || true
  sudo docker rm $DOCKER_IMAGE_NAME || true
  
  echo "Running Docker container..."
  sudo docker run -d --name $DOCKER_IMAGE_NAME -p 8080:8080 $DOCKER_IMAGE_NAME

  echo "Updating Nginx configuration..."
  sudo tee $NGINX_CONF_PATH <<EOT
server {
    listen 80 default_server;
    listen [::]:80 default_server;

    root /var/www/html;

    # Add index.php to the list if you are using PHP
    index index.html index.htm index.nginx-debian.html;


    server_name _;

    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOT

  echo "Reloading Nginx..."
  sudo systemctl reload nginx
EOF

echo "Deployment completed successfully!"