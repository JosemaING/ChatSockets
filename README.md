# CHAT TCP EN JAVA

## AUTOR

- **Nombre del Autor:** José María Santos
- **Email:** jsr1002@alu.ubu.es
- **Fecha:** 11/03/2024

## DESCRIPCIÓN

Este proyecto incluye un servidor y cliente de chat TCP implementado en Java. Se proporcionan los comandos para facilitar la ejecución tanto del servidor como del cliente en modo consola.

Es importante no modificar el fichero `pom.xml` para asegurar el correcto funcionamiento del proyecto.

Para iniciar el servidor y conectar clientes, se deben seguir las instrucciones detalladas en la sección de ejecución. El cliente con el username "admin" es el único que puede apagar el servidor, con el comando `shutdown`.

## REQUISITOS

1. Maven instalado y configurado correctamente.
2. Java JDK versión 8 o superior.
3. No modificar el fichero `pom.xml` para evitar problemas en la ejecución.

## EJECUCIÓN

Utilice los siguientes comandos en la terminal dentro del directorio del proyecto:

- **INICIAR SERVIDOR TCP**
mvn exec:java -Dexec.mainClass="es.ubu.lsi.server.ChatServerImpl"

- **CONECTAR CLIENTE TCP (Sin parametros de argumento iniciamos como anonimo)**
mvn exec:java -Dexec.mainClass="es.ubu.lsi.client.ChatClientImpl"

- **CONECTAR CLIENTE TCP (como administrador)**
mvn exec:java -Dexec.mainClass="es.ubu.lsi.client.ChatClientImpl" -Dexec.args="localhost admin"

## COMANDOS

- `shutdown`: Apaga el servidor (solo "admin").
- `logout`: Cierra la sesión del cliente.
- `drop <username>`: Desconecta a un usuario especifico "username".
- `ban <username>`: Banea a un usuario especificado "username".
- `unban <username>`: Desbanea a un usuario especificado "username".

## SUGERENCIAS

- Se ha realizado una limpieza del proyecto con Maven `mvn clean`. Se sugiere no realizar modificaciones en el archivo `pom.xml` para mantener la estabilidad del proyecto.

- Se ha realizado la ejecución con ANT `ant`, que compila, empaqueta y documenta. Se sugiere no realizar modificaciones en el archivo `build.xml` para mantener la estabilidad del proyecto.
