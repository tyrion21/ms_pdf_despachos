#!/bin/bash

# Script de despliegue rápido desde repositorio
# Uso: curl -sSL https://raw.githubusercontent.com/tu-usuario/sdt-pdf-generator/main/quick-deploy.sh | bash

set -e

# Configuración
REPO_URL="https://github.com/tyrion21/ms_pdf_despachos.git"  # Cambiar por tu repositorio
APP_NAME="sdt-pdf-generator"
APP_DIR="/opt/$APP_NAME"
BRANCH="main"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "🚀 Despliegue rápido del generador PDF SDT"
echo "Repositorio: $REPO_URL"
echo ""

# Verificar permisos de sudo
if ! sudo -v; then
    log_error "Este script requiere permisos de sudo"
    exit 1
fi

# Instalar dependencias básicas
log_info "Instalando dependencias básicas..."
sudo apt update
sudo apt install -y curl git apt-transport-https ca-certificates gnupg lsb-release

# Instalar Docker si no está instalado
if ! command -v docker &> /dev/null; then
    log_info "Instalando Docker..."
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt update
    sudo apt install -y docker-ce docker-ce-cli containerd.io
    sudo usermod -aG docker $USER
    log_info "Docker instalado. Reinicia la sesión para aplicar cambios de grupo."
fi

# Instalar Docker Compose
if ! command -v docker-compose &> /dev/null; then
    log_info "Instalando Docker Compose..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

# Clonar o actualizar repositorio
if [ ! -d "$APP_DIR" ]; then
    log_info "Clonando repositorio..."
    sudo mkdir -p $APP_DIR
    sudo chown $USER:$USER $APP_DIR
    git clone -b $BRANCH $REPO_URL $APP_DIR
else
    log_info "Actualizando repositorio existente..."
    cd $APP_DIR
    git pull origin $BRANCH
fi

cd $APP_DIR

# Configurar permisos
sudo chown -R $USER:$USER $APP_DIR

# Crear directorios necesarios
mkdir -p generated-pdf logs templates nginx/ssl

# Configurar variables de entorno
if [ ! -f ".env" ]; then
    log_info "Creando archivo de configuración..."
    cp .env.example .env
    
    # Configuración interactiva básica
    log_warn "Configuración de base de datos requerida:"
    
    read -p "Host de base de datos [192.168.1.3]: " db_host
    db_host=${db_host:-192.168.1.3}
    
    read -p "Puerto de base de datos [1433]: " db_port
    db_port=${db_port:-1433}
    
    read -p "Nombre de base de datos [Erpfrusys]: " db_name
    db_name=${db_name:-Erpfrusys}
    
    read -p "Usuario de base de datos [jason]: " db_user
    db_user=${db_user:-jason}
    
    read -s -p "Contraseña de base de datos: " db_pass
    echo ""
    
    # Actualizar archivo .env
    sed -i "s/DB_HOST=.*/DB_HOST=$db_host/" .env
    sed -i "s/DB_PORT=.*/DB_PORT=$db_port/" .env
    sed -i "s/DB_NAME=.*/DB_NAME=$db_name/" .env
    sed -i "s/DB_USERNAME=.*/DB_USERNAME=$db_user/" .env
    sed -i "s/DB_PASSWORD=.*/DB_PASSWORD=$db_pass/" .env
    
    log_info "Configuración guardada en .env"
fi

# Construir y desplegar
log_info "Construyendo y desplegando aplicación..."
docker-compose build
docker-compose up -d

# Verificar despliegue
log_info "Verificando servicios..."
sleep 15

if docker-compose ps | grep -q "Up"; then
    echo ""
    log_info "✅ ¡Despliegue exitoso!"
    log_info ""
    log_info "🌐 URLs disponibles:"
    log_info "  Frontend:     http://$(hostname -I | awk '{print $1}'):3010"
    log_info "  Backend API:  http://$(hostname -I | awk '{print $1}'):8090"
    log_info "  App completa: http://$(hostname -I | awk '{print $1}'):80"
    echo ""
    log_info "📋 Comandos útiles:"
    log_info "  Ver logs:     cd $APP_DIR && docker-compose logs -f"
    log_info "  Reiniciar:    cd $APP_DIR && docker-compose restart"
    log_info "  Actualizar:   cd $APP_DIR && git pull && docker-compose up -d --build"
    echo ""
else
    log_error "❌ Error en el despliegue"
    log_info "Revisando logs..."
    docker-compose logs --tail=50
    exit 1
fi

# Configurar firewall (opcional)
log_warn "¿Configurar firewall para abrir puertos necesarios? (y/n)"
read -r response
if [[ $response =~ ^[Yy]$ ]]; then
    sudo ufw allow 80/tcp
    sudo ufw allow 3010/tcp
    sudo ufw allow 8090/tcp
    log_info "Puertos abiertos en firewall"
fi

echo ""
log_info "🎉 Instalación completada!"
log_warn "Si este es el primer uso de Docker, reinicia la sesión para aplicar permisos de grupo."