#!/bin/bash

# Script de despliegue para Ubuntu Server
# Generador PDF SDT

set -e

# Configuración
REPO_URL="https://github.com/tyrion21/ms_pdf_despachos.git"
APP_NAME="sdt-pdf-generator"
APP_DIR="/opt/$APP_NAME"

echo "🚀 Iniciando despliegue del generador PDF SDT..."

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Funciones de logging
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar si Docker está instalado
if ! command -v docker &> /dev/null; then
    log_error "Docker no está instalado. Instalando Docker..."
    
    # Actualizar sistema
    sudo apt update
    sudo apt install -y apt-transport-https ca-certificates curl gnupg lsb-release
    
    # Agregar clave GPG oficial de Docker
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
    
    # Agregar repositorio
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    # Instalar Docker
    sudo apt update
    sudo apt install -y docker-ce docker-ce-cli containerd.io
    
    # Agregar usuario actual al grupo docker
    sudo usermod -aG docker $USER
    
    log_info "Docker instalado correctamente. Por favor, reinicia la sesión para aplicar los cambios de grupo."
fi

# Verificar si Docker Compose está instalado
if ! command -v docker-compose &> /dev/null; then
    log_warn "Docker Compose no está instalado. Instalando..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    log_info "Docker Compose instalado correctamente."
fi

# Verificar si Git está instalado
if ! command -v git &> /dev/null; then
    log_warn "Git no está instalado. Instalando..."
    sudo apt update
    sudo apt install -y git
    log_info "Git instalado correctamente."
fi

# Crear directorio de aplicación y clonar repositorio
if [ ! -d "$APP_DIR" ]; then
    log_info "Creando directorio de aplicación: $APP_DIR"
    sudo mkdir -p $APP_DIR
    sudo chown $USER:$USER $APP_DIR
    
    log_info "Clonando repositorio desde: $REPO_URL"
    git clone $REPO_URL $APP_DIR
else
    log_info "Directorio existe. Verificando repositorio..."
    cd $APP_DIR
    
    # Verificar si es un repositorio git válido
    if git rev-parse --git-dir > /dev/null 2>&1; then
        log_info "Actualizando código existente..."
        git pull origin main
    else
        log_warn "Directorio existe pero no es un repositorio git válido."
        log_warn "¿Quieres eliminar el directorio y clonar de nuevo? (y/n)"
        read -r response
        if [[ $response =~ ^[Yy]$ ]]; then
            # Backup de .env si existe
            if [ -f ".env" ]; then
                log_info "Guardando configuración existente..."
                cp .env /tmp/sdt-pdf-generator.env.backup
            fi
            
            cd ..
            sudo rm -rf $APP_DIR
            sudo mkdir -p $APP_DIR
            sudo chown $USER:$USER $APP_DIR
            
            log_info "Clonando repositorio desde: $REPO_URL"
            git clone $REPO_URL $APP_DIR
            cd $APP_DIR
            
            # Restaurar .env si existía
            if [ -f "/tmp/sdt-pdf-generator.env.backup" ]; then
                log_info "Restaurando configuración anterior..."
                cp /tmp/sdt-pdf-generator.env.backup .env
            fi
        else
            log_error "No se puede continuar sin un repositorio git válido."
            exit 1
        fi
    fi
fi

# Ir al directorio de la aplicación
cd $APP_DIR

# Crear directorios necesarios
mkdir -p generated-pdf
mkdir -p logs
mkdir -p templates
mkdir -p nginx/ssl

# Configurar permisos
sudo chown -R $USER:$USER $APP_DIR

# Configurar variables de entorno
log_info "Configurando variables de entorno..."
if [ ! -f ".env" ]; then
    cp .env.example .env
    log_warn "Archivo .env creado. Por favor, edítalo con tu configuración específica:"
    log_warn "  nano .env"
    log_warn "Presiona Enter para continuar después de configurar .env..."
    read -r
fi

log_info "Construyendo imagen Docker..."
docker build -t sdt-pdf-generator .

log_info "Desplegando servicios..."
docker-compose up -d

# Verificar que los servicios están corriendo
log_info "Verificando servicios..."
sleep 10

if docker-compose ps | grep -q "Up"; then
    log_info "✅ Servicios desplegados correctamente!"
    log_info "La aplicación estará disponible en:"
    log_info "  - Frontend: http://$(hostname -I | awk '{print $1}'):3010"
    log_info "  - Backend: http://$(hostname -I | awk '{print $1}'):8090"
    log_info "  - Nginx (Full App): http://$(hostname -I | awk '{print $1}'):80"
else
    log_error "❌ Error en el despliegue. Verificando logs..."
    docker-compose logs
fi

# Configurar systemd service (opcional)
log_info "¿Deseas configurar el servicio para iniciar automáticamente? (y/n)"
read -r response
if [[ $response =~ ^[Yy]$ ]]; then
    sudo tee /etc/systemd/system/sdt-pdf-generator.service > /dev/null <<EOF
[Unit]
Description=SDT PDF Generator
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$APP_DIR
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
User=$USER

[Install]
WantedBy=multi-user.target
EOF

    sudo systemctl daemon-reload
    sudo systemctl enable sdt-pdf-generator.service
    log_info "Servicio systemd configurado. La aplicación se iniciará automáticamente al reiniciar."
fi

# Mostrar información final
echo ""
log_info "🎉 Despliegue completado!"
echo ""
log_info "Comandos útiles:"
log_info "  Ver logs:          docker-compose logs -f"
log_info "  Reiniciar:         docker-compose restart"
log_info "  Parar servicios:   docker-compose down"
log_info "  Actualizar:        docker-compose pull && docker-compose up -d"
echo ""
log_warn "Recuerda:"
log_warn "  1. Configurar el archivo .env con tu configuración de base de datos"
log_warn "  2. Abrir los puertos 80, 3010 y 8090 en el firewall si es necesario"
log_warn "  3. Configurar SSL si planeas usar HTTPS"