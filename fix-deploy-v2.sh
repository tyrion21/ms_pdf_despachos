#!/bin/bash

# Script mejorado para arreglar problemas de despliegue
# Maneja mejor los permisos y directorios

set -e

APP_DIR="/opt/sdt-pdf-generator"
REPO_URL="https://github.com/tyrion21/ms_pdf_despachos.git"
TEMP_DIR="/tmp/sdt-clone-temp"

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

echo "🔧 Arreglando problemas de despliegue (v2)..."

# Cambiar a directorio seguro
cd /tmp

# Parar contenedores si están corriendo
if [ -d "$APP_DIR" ]; then
    log_info "Parando contenedores existentes..."
    cd $APP_DIR 2>/dev/null || true
    sudo docker-compose down 2>/dev/null || true
    sudo docker-compose -f docker-compose.simple.yml down 2>/dev/null || true
fi

# Hacer backup de archivos importantes
if [ -f "$APP_DIR/.env" ]; then
    log_info "Guardando configuración existente..."
    sudo cp $APP_DIR/.env /tmp/sdt-pdf-generator.env.backup 2>/dev/null || true
    log_info "Backup guardado en: /tmp/sdt-pdf-generator.env.backup"
fi

# Eliminar directorio corrupto
if [ -d "$APP_DIR" ]; then
    log_warn "Eliminando directorio corrupto: $APP_DIR"
    sudo rm -rf $APP_DIR
fi

# Limpiar directorio temporal si existe
if [ -d "$TEMP_DIR" ]; then
    log_info "Limpiando directorio temporal..."
    sudo rm -rf $TEMP_DIR
fi

# Crear directorio temporal y clonar ahí primero
log_info "Clonando repositorio en directorio temporal..."
git clone $REPO_URL $TEMP_DIR

# Crear directorio de aplicación con permisos correctos
log_info "Creando directorio de aplicación con permisos correctos..."
sudo mkdir -p $APP_DIR
sudo chown -R $USER:$USER $APP_DIR

# Mover archivos del temporal al directorio final
log_info "Moviendo archivos al directorio final..."
cp -r $TEMP_DIR/* $APP_DIR/
cp -r $TEMP_DIR/.* $APP_DIR/ 2>/dev/null || true  # Incluir archivos ocultos

# Limpiar directorio temporal
rm -rf $TEMP_DIR

# Ir al directorio de aplicación
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
chmod +x *.sh 2>/dev/null || true

# Verificar que estamos en un repositorio git válido
if git rev-parse --git-dir > /dev/null 2>&1; then
    log_info "✅ Repositorio git configurado correctamente"
else
    log_warn "Reinicializando repositorio git..."
    git init
    git remote add origin $REPO_URL
fi

log_info "✅ Repositorio arreglado correctamente!"
log_info "Ubicación: $APP_DIR"

echo ""
log_warn "¿Quieres desplegar la aplicación ahora? (y/n)"
read -r response
if [[ $response =~ ^[Yy]$ ]]; then
    log_info "Desplegando aplicación..."
    
    # Intentar con docker-compose normal primero
    log_info "Intentando despliegue completo..."
    if timeout 300 docker-compose build; then
        log_info "Build exitoso, iniciando servicios..."
        if docker-compose up -d; then
            sleep 15
            if docker-compose ps | grep -q "Up"; then
                echo ""
                log_info "✅ ¡Despliegue completo exitoso!"
                log_info "URLs disponibles:"
                log_info "  Frontend:     http://$(hostname -I | awk '{print $1}'):3010"
                log_info "  Backend API:  http://$(hostname -I | awk '{print $1}'):8090"
                log_info "  App completa: http://$(hostname -I | awk '{print $1}'):80"
            else
                log_error "Servicios no iniciaron correctamente"
                docker-compose logs --tail=20
            fi
        else
            log_error "Error al iniciar servicios"
        fi
    else
        log_error "Error en build completo. Intentando versión simplificada..."
        docker-compose down 2>/dev/null || true
        
        log_info "Usando docker-compose.simple.yml..."
        if timeout 300 docker-compose -f docker-compose.simple.yml build; then
            log_info "Build simple exitoso, iniciando backend..."
            if docker-compose -f docker-compose.simple.yml up -d; then
                sleep 10
                if docker-compose -f docker-compose.simple.yml ps | grep -q "Up"; then
                    log_info "✅ Backend desplegado exitosamente!"
                    log_info "URL Backend: http://$(hostname -I | awk '{print $1}'):8090"
                    log_info "Health Check: http://$(hostname -I | awk '{print $1}'):8090/actuator/health"
                    log_warn "Frontend no desplegado. Ejecuta manualmente:"
                    log_warn "  cd $APP_DIR/frontend && npm install && npm start"
                else
                    log_error "Backend no se inició correctamente"
                    docker-compose -f docker-compose.simple.yml logs --tail=20
                fi
            else
                log_error "Error al iniciar backend"
            fi
        else
            log_error "Error en build simplificado también"
        fi
    fi
fi

echo ""
log_info "📋 Comandos útiles:"
log_info "  Ver logs completos:    cd $APP_DIR && docker-compose logs -f"
log_info "  Ver logs backend:      cd $APP_DIR && docker-compose -f docker-compose.simple.yml logs -f"
log_info "  Reiniciar:            cd $APP_DIR && docker-compose restart"
log_info "  Parar:                cd $APP_DIR && docker-compose down"