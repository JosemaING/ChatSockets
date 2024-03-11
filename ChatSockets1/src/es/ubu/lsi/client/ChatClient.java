package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * Interfaz para el ChatClient.
 *
 * @author Jose Maria Santos
 */
public interface ChatClient {

	/**
	 * M�todo que inicia el cliente y establece la conexi�n con el servidor.
	 * 
	 * @return true si no hay errores en conexi�n, false si s�.
	 */
	boolean start();

	/**
	 * Metodo que env�a un mensaje al servidor.
	 * 
	 * @param msg El ChatMessage que se va a enviar.
	 */
	void sendMessage(ChatMessage msg);

	/**
	 * Desconecta el cliente del servidor.
	 */
	void disconnect();
}
