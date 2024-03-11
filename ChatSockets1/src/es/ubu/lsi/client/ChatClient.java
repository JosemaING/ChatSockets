package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * Interfaz para el ChatClient.
 *
 * @author Jose Maria Santos
 */
public interface ChatClient {

	/**
	 * Método que inicia el cliente y establece la conexión con el servidor.
	 * 
	 * @return true si no hay errores en conexión, false si sí.
	 */
	boolean start();

	/**
	 * Metodo que envía un mensaje al servidor.
	 * 
	 * @param msg El ChatMessage que se va a enviar.
	 */
	void sendMessage(ChatMessage msg);

	/**
	 * Desconecta el cliente del servidor.
	 */
	void disconnect();
}
