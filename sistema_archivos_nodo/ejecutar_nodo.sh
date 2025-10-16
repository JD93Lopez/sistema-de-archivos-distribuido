#!/bin/bash

# Script para ejecutar nodo RMI distribuido
# ejecutar_nodo.sh

echo "=== Configurando Nodo RMI Distribuido ==="

# Obtener la IP real de la interfaz de red (no localhost)
IP=$(hostname -I | awk '{print $1}')
echo "IP detectada: $IP"

# Verificar que Java esté instalado
if ! command -v java &> /dev/null; then
    echo "❌ Java no está instalado. Instalando..."
    sudo apt update
    sudo apt install -y openjdk-11-jdk
fi

echo "Versión de Java:"
java -version

# Verificar que el puerto 1099 esté disponible
if netstat -tulpn | grep -q ":1099 "; then
    echo "⚠️  Puerto 1099 ya está en uso. Terminando procesos..."
    sudo pkill -f "java.*ServidorRMI"
    sleep 2
fi

echo "Configurando variables de entorno para RMI..."

# Configurar variables de entorno para RMI distribuido
export JAVA_OPTS="-Djava.rmi.server.hostname=$IP"
export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
export JAVA_OPTS="$JAVA_OPTS -Djava.rmi.server.useCodebaseOnly=false"
export JAVA_OPTS="$JAVA_OPTS -Dsun.rmi.transport.tcp.responseTimeout=10000"
export JAVA_OPTS="$JAVA_OPTS -Dsun.rmi.transport.tcp.handshakeTimeout=10000"

echo "Variables configuradas:"
echo "  java.rmi.server.hostname=$IP"
echo "  java.net.preferIPv4Stack=true"

# Verificar que los archivos Java existan
if [ ! -f "nodo/ServidorRMI.java" ]; then
    echo "❌ Archivo ServidorRMI.java no encontrado"
    echo "Asegúrate de estar en el directorio correcto del proyecto"
    exit 1
fi

echo "Compilando archivos Java..."
javac -cp . nodo/*.java central/*.java

if [ $? -ne 0 ]; then
    echo "❌ Error en la compilación"
    exit 1
fi

echo "✅ Compilación exitosa"

echo "Iniciando servidor RMI en IP: $IP puerto: 1099"
echo "Presiona Ctrl+C para detener el servidor"
echo "================================"

# Ejecutar el servidor RMI con las configuraciones
java $JAVA_OPTS -cp . nodo.ServidorRMI