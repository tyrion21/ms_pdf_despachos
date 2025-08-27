#!/bin/bash

# Script para arreglar problemas de despliegue
# Limpia y redespliega la aplicación

set -e

APP_DIR="/opt/sdt-pdf-generator"
REPO_URL="https://github.com/tyrion21/ms_pdf_despachos.git"

# Colores
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

echo "🔧 Arreglando problemas de despliegue..."

# Parar contenedores si están corriendo
if [ -d "$APP_DIR" ]; then
    log_info "Parando contenedores existentes..."
    cd $APP_DIR
    docker-compose down 2>/dev/null || true
fi

# Hacer backup de archivos importantes
if [ -f "$APP_DIR/.env" ]; then
    log_info "Guardando configuración existente..."
    cp $APP_DIR/.env /tmp/sdt-pdf-generator.env.backup
    log_info "Backup guardado en: /tmp/sdt-pdf-generator.env.backup"
fi

# Eliminar directorio corrupto
if [ -d "$APP_DIR" ]; then
    log_warn "Eliminando directorio corrupto: $APP_DIR"
    sudo rm -rf $APP_DIR
fi

# Crear directorio limpio
log_info "Creando directorio limpio..."
sudo mkdir -p $APP_DIR
sudo chown $USER:$USER $APP_DIR

# Clonar repositorio
log_info "Clonando repositorio desde: $REPO_URL"
git clone $REPO_URL $APP_DIR

cd $APP_DIR

# Restaurar configuración si existe backup
if [ -f "/tmp/sdt-pdf-generator.env.backup" ]; then
    log_info "Restaurando configuración anterior..."
    cp /tmp/sdt-pdf-generator.env.backup .env
else
    log_info "Creando nueva configuración..."
    cp .env.example .env
    log_warn "IMPORTANTE: Debes editar el archivo .env con tu configuración:"
    log_warn "  nano $APP_DIR/.env"
fi

# Crear directorios necesarios
mkdir -p generated-pdf logs templates nginx/ssl

# Configurar permisos
sudo chown -R $USER:$USER $APP_DIR

log_info "✅ Repositorio arreglado. Ahora puedes ejecutar:"
log_info "  cd $APP_DIR"
log_info "  nano .env  # Configurar si es necesario"
log_info "  docker-compose up -d"

echo ""
log_warn "¿Quieres desplegar la aplicación ahora? (y/n)"
read -r response
if [[ $response =~ ^[Yy]$ ]]; then
    log_info "Desplegando aplicación..."
    
    # Intentar con docker-compose normal primero
    if docker-compose build && docker-compose up -d; then
        sleep 10
        if docker-compose ps | grep -q "Up"; then
            echo ""
            log_info "✅ ¡Despliegue exitoso!"
            log_info "URLs disponibles:"
            log_info "  Frontend:     http://$(hostname -I | awk '{print $1}'):3010"
            log_info "  Backend API:  http://$(hostname -I | awk '{print $1}'):8090"
            log_info "  App completa: http://$(hostname -I | awk '{print $1}'):80"
        else
            log_error "Error en servicios completos. Intentando versión simplificada..."
            docker-compose down
            log_info "Desplegando solo backend con docker-compose.simple.yml..."
            docker-compose -f docker-compose.simple.yml build
            docker-compose -f docker-compose.simple.yml up -d
            
            sleep 10
            if docker-compose -f docker-compose.simple.yml ps | grep -q "Up"; then
                log_info "✅ Backend desplegado exitosamente!"
                log_info "URL Backend: http://$(hostname -I | awk '{print $1}'):8090"
                log_warn "Frontend no desplegado. Puedes ejecutarlo manualmente con:"
                log_warn "  cd frontend && npm install && npm run dev"
            else
                log_error "Error en el despliegue. Revisa los logs:"
                docker-compose -f docker-compose.simple.yml logs
            fi
        fi
    else
        log_error "Error en la construcción. Intentando versión simplificada..."
        log_info "Usando Dockerfile.simple..."
        docker-compose -f docker-compose.simple.yml build
        docker-compose -f docker-compose.simple.yml up -d
        
        sleep 10
        if docker-compose -f docker-compose.simple.yml ps | grep -q "Up"; then
            log_info "✅ Backend desplegado con versión simplificada!"
            log_info "URL Backend: http://$(hostname -I | awk '{print $1}'):8090"
        else
            log_error "Error en el despliegue simplificado. Revisa los logs:"
            docker-compose -f docker-compose.simple.yml logs
        fi
    fi
fi