# SDT PDF Generator - Despliegue en Ubuntu con Docker

Este documento describe cómo desplegar la aplicación SDT PDF Generator en un servidor Ubuntu usando Docker.

## 🚀 Despliegue Rápido

### Requisitos
- Ubuntu 18.04 o superior
- Acceso sudo
- Conexión a internet

### Opción 1: Despliegue desde repositorio (Recomendado)
```bash
# Descargar y ejecutar script de instalación
curl -sSL https://raw.githubusercontent.com/tu-usuario/sdt-pdf-generator/main/quick-deploy.sh | bash
```

### Opción 2: Despliegue local
```bash
# Si ya tienes el código clonado
chmod +x deploy.sh
./deploy.sh
```

## 📋 Instalación Manual

### 1. Instalar Docker y Docker Compose

```bash
# Actualizar sistema
sudo apt update
sudo apt install -y apt-transport-https ca-certificates curl gnupg lsb-release

# Agregar clave GPG de Docker
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Agregar repositorio
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Instalar Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io

# Instalar Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Agregar usuario al grupo docker
sudo usermod -aG docker $USER
```

### 2. Configurar la Aplicación

```bash
# Crear directorio
sudo mkdir -p /opt/sdt-pdf-generator
sudo chown $USER:$USER /opt/sdt-pdf-generator
cd /opt/sdt-pdf-generator

# Copiar archivos de proyecto
cp /ruta/a/tu/proyecto/* .

# Configurar variables de entorno
cp .env.example .env
nano .env  # Editar con tu configuración
```

### 3. Construir y Desplegar

```bash
# Construir imagen
docker build -t sdt-pdf-generator .

# Iniciar servicios
docker-compose up -d
```

## 🔧 Configuración

### Variables de Entorno (.env)

```env
# Base de datos
DB_HOST=192.168.1.3
DB_PORT=1433
DB_NAME=Erpfrusys
DB_USERNAME=jason
DB_PASSWORD=tu_password_seguro

# Servidor
SERVER_PORT=8080
JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC

# SSL (opcional)
SSL_ENABLED=false
SSL_KEYSTORE_PATH=/app/ssl/keystore.p12
SSL_KEYSTORE_PASSWORD=changeit
```

### Configuración de Nginx

El archivo `nginx/nginx.conf` está preconfigurado para:
- Proxy reverso hacia la aplicación Spring Boot
- Compresión gzip
- Cacheo de archivos estáticos
- Health checks

## 📊 Monitoreo y Mantenimiento

### Comandos Útiles

```bash
# Ver estado de contenedores
docker-compose ps

# Ver logs
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f sdt-pdf-generator

# Reiniciar servicios
docker-compose restart

# Actualizar aplicación
docker-compose pull
docker-compose up -d

# Parar servicios
docker-compose down
```

### Health Check

La aplicación incluye health checks automáticos:
- URL: `http://servidor:8090/actuator/health`
- Intervalo: cada 30 segundos
- Timeout: 10 segundos

### Logs

Los logs se almacenan en:
- Aplicación: `/opt/sdt-pdf-generator/logs/`
- PDFs generados: `/opt/sdt-pdf-generator/generated-pdf/`

## 🔒 Seguridad

### Firewall
```bash
# Permitir puertos necesarios
sudo ufw allow 80/tcp
sudo ufw allow 3010/tcp  # Frontend
sudo ufw allow 8090/tcp  # Backend
sudo ufw allow 443/tcp   # Si usas HTTPS
```

### SSL/HTTPS (Opcional)

Para habilitar HTTPS:

1. Obtén certificados SSL (Let's Encrypt recomendado)
2. Coloca los certificados en `nginx/ssl/`
3. Descomenta la configuración HTTPS en `nginx/nginx.conf`
4. Reinicia Nginx: `docker-compose restart nginx`

## 🚨 Troubleshooting

### Problemas Comunes

**Error de conexión a base de datos:**
```bash
# Verificar conectividad
docker-compose exec sdt-pdf-generator ping DB_HOST
```

**Contenedor no inicia:**
```bash
# Ver logs detallados
docker-compose logs sdt-pdf-generator
```

**Permisos de archivos:**
```bash
# Corregir permisos
sudo chown -R $USER:$USER /opt/sdt-pdf-generator
chmod 755 /opt/sdt-pdf-generator/generated-pdf
```

### Recursos del Sistema

**Requisitos mínimos:**
- RAM: 2GB
- CPU: 2 cores
- Disco: 10GB libres

**Configuración recomendada:**
- RAM: 4GB+
- CPU: 4 cores+
- Disco: 50GB+ (para PDFs generados)

## 📈 Escalabilidad

Para entornos de alta demanda:

1. **Usar Docker Swarm o Kubernetes**
2. **Configurar load balancer**
3. **Separar base de datos**
4. **Implementar cache Redis**

## 🔄 Backup

### Script de Backup Automático

```bash
#!/bin/bash
# backup.sh
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/sdt-pdf-generator"
mkdir -p $BACKUP_DIR

# Backup de PDFs generados
tar -czf $BACKUP_DIR/pdfs_$DATE.tar.gz /opt/sdt-pdf-generator/generated-pdf

# Backup de configuración
tar -czf $BACKUP_DIR/config_$DATE.tar.gz /opt/sdt-pdf-generator/.env /opt/sdt-pdf-generator/nginx

# Limpiar backups antiguos (más de 30 días)
find $BACKUP_DIR -name "*.tar.gz" -mtime +30 -delete
```

## 📞 Soporte

Para problemas o consultas:
1. Revisar logs: `docker-compose logs`
2. Verificar configuración de red y base de datos
3. Consultar documentación de Spring Boot y Next.js