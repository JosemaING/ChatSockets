package es.ubu.lsi.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.MessageType;

/**
 * Clase ChatClientImpl Implementacion del cliente de chat. Contiene
 * ChatClientListener como una clase interna.
 * 
 * Quick Setup: https://github.com/JosemaING/ChatSockets.git
 * 
 * @author Jose Maria Santos
 * @version 1.0
 */

public class ChatClientImpl implements ChatClient {

	/** Servidor. */
	private String server;

	/** Nombre del usuario. */
	private String username;

	/** Puerto de conexion. */
	private int port;

	/** ID. */
	private static int id;

	/** Booleano carry on, indica el permiso para leer del canal. */
	private boolean carryOn = true;

	/** Socket del cliente. */
	private Socket socket;

	/** Salida. */
	ObjectOutputStream outputStream;

	/** Entrada */
	ObjectInputStream inputStream;

	/** Entrada por teclado. */
	private Scanner input;

	/**
	 * Constructor de la clase ChatClientImpl.
	 * 
	 * @param server   IP del servidor al que se conecta el cliente.
	 * @param port     Puerto del servidor al que envia las peticiones.
	 * @param username Nombre de usuario con el que se conecta.
	 */
	public ChatClientImpl(String server, int port, String username) {
		this.server = server;
		this.port = port;
		this.username = username;

		try {

			this.socket = new Socket(this.server, this.port);
			outputStream = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			System.err.println("ERROR: Could not launch client! Exiting now...");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Inicia el cliente y conecta este cliente con el servidor.
	 * 
	 * @return true, si no ha habido error.
	 */
	@Override
	public boolean start() {
		try {
			// Conectarse al servidor y enviar el nombre de usuario
			connect();

			// Preparar para leer mensajes del teclado
			input = new Scanner(System.in);
			String text;

			// Bucle principal del cliente
			while (carryOn) {
				text = input.nextLine(); // Lee la entrada del usuario

				if (text.equalsIgnoreCase("LOGOUT")) {
					// Si el usuario quiere desconectarse
					ChatMessage msg = new ChatMessage(id, MessageType.LOGOUT, "");
					sendMessage(msg);
					break; // Salir del bucle después de enviar el mensaje de desconexión
				} else if (text.equalsIgnoreCase("SHUTDOWN")) {
					// Si el usuario envía el comando de apagado
					System.out.println("Sending shutdown command to server...");
					ChatMessage msg = new ChatMessage(id, MessageType.SHUTDOWN, "");
					sendMessage(msg);
					// Considera si quieres que el cliente se detenga después de enviar el comando
					// de apagado
					break;
				} else {
					// Para cualquier otro mensaje
					ChatMessage msg = new ChatMessage(id, MessageType.MESSAGE, text);
					sendMessage(msg);
				}
			}
		} finally {
			// Asegurar que el cliente se desconecte correctamente al salir
			disconnect();
		}
		return true;
	}

	/**
	 * Envia un mensaje al servidor.
	 * 
	 * @param msg Mensaje a enviar.
	 */
	@Override
	public void sendMessage(ChatMessage msg) {
		try {
			outputStream.writeObject(msg); // Envia el mensaje por el canal de salida
		} catch (IOException e) {
			System.err.println("ERROR: Could not send message to server.");
			e.printStackTrace(); // Muestra la traza de la excepcion
			disconnect();
		}
	}

	/**
	 * Desconecta el cliente del servidor.
	 */
	@Override
	public void disconnect() {
		try {
			if (input != null) input.close();
			if (outputStream != null) outputStream.close();
			if (socket != null) socket.close();
			carryOn = false; // Dejamos de leer del canal y aseguramos que el Listener se detenga
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Envia una peticion de login al servidor y se queda a la espera de recibir
	 * respuesta.
	 */
	private void connect() {
		ChatMessage msg = new ChatMessage(0, MessageType.MESSAGE, username);

		try {
			inputStream = new ObjectInputStream(socket.getInputStream());
			sendMessage(msg);
			msg = (ChatMessage) inputStream.readObject();

			System.out.println(msg.getMessage());
			if (msg.getType() == MessageType.LOGOUT) {
				System.out.println("Shutting down client now...");
				disconnect();
				System.exit(0);
			}
			// Establecemos el id del cliente con el id otorgado por el servidor
			id = msg.getId();
			// System.out.println("Cliente: ID del mensaje recibido del server: " + id);
			new Thread(new ChatClientListener(inputStream, id)).start();
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("ERROR: could not get response from server!");
			disconnect();
			System.exit(1);
		}
	}

	/**
	 * Muestra un mensaje deayuda para el uso del programa Cliente.
	 */
	private static void printHelp() {
		System.out.println("HELP:");
		System.out.println("\tjava ChatClientImpl <server_address> <username> <key>");
		System.out.println("\tOR");
		System.out.println("\tjava ChatClientImpl <username> <key> (default server: localhost)");
	}

	/**
	 * Metodo principal de ejecucion del cliente
	 * 
	 * @param args argumentos de entrada del programa cliente.
	 */
	public static void main(String[] args) {

		int port = 1500;
		String server = "localhost";
		String username = "Anonymous";

		if (args.length > 1) {
			server = args[0];
			username = args[1];
		} else if (args.length != 0) {
			System.out.println("Invalid number of arguments!");
			printHelp();
			System.exit(1);
		}

		new ChatClientImpl(server, port, username).start();
	}

	/**
	 * Clase interna ChatClientListener, crea un hilo de escucha continuamente los
	 * mensajes que entran del servidor y los muestra al usuario del cliente del
	 * chat. Implementa la interfaz Runnable.
	 *
	 * @see ChatClientImpl
	 */
	class ChatClientListener implements Runnable {

		/** Input. */
		ObjectInputStream serverInput;

		/**
		 * Constructor.
		 *
		 * @param in Canal de entrada
		 */
		public ChatClientListener(ObjectInputStream in, int id) {
			this.serverInput = in;
		}

		/**
		 * Escucha en el canal de entrada los mensajes que provienen del servidor.
		 */
		@Override
		public void run() {
			try {
				while (carryOn) { // Mientras pueda leer del canal de entrada
					ChatMessage msg = (ChatMessage) serverInput.readObject();
					System.out.println(msg.getMessage());
				}
			} catch (IOException e) {
				System.err.println("ERROR: Server conexion lost.");
				carryOn = false;
			} catch (ClassNotFoundException e) {
				System.err.println("ERROR: Could not receive message. Server is unavailable.");
			} finally {
				try {
					serverInput.close();
					System.out.println("Shutting down client now...");
					disconnect();
				} catch (IOException e2) {
					System.err.println("ERROR: Could not close the input conexion.");
				}
			}
		}
	}
}
