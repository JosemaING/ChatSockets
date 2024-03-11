package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

/**
 * Interfaz para el servidor de chat.
 */
public interface ChatServer {

	/**
	 * Inicia el servidor para aceptar conexiones de clientes.
	 */
	void startup();

	/**
	 * Cierra todas las conexiones y apaga el servidor.
	 */
	void shutdown();

	/**
	 * Envía un mensaje a todos los clientes conectados.
	 *
	 * @param message El mensaje a difundir.
	 */
	void broadcast(ChatMessage message);

	/**
	 * Elimina un cliente del servidor basado en su ID
	 *
	 * @param id El ID del cliente a eliminar.
	 */
	void remove(int id);
}
