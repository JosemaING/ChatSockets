package es.ubu.lsi.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.MessageType;

/**
 * Clase ChatServerlmpl. Implementación del servidor del chat.
 * 
 * @author Jose Maria Santos
 * @version 1.0
 */
public class ChatServerImpl implements ChatServer {

	/** Constante, puerto por defecto. */
	private static final int DEFAULT_PORT = 1500;

	/** ID del cliente, también sirve como contador de clientes conectados. */
	private static int clientId = 0;

	/** Formato de fecha. */
	private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	/** Puerto. */
	private int port;

	/** Booleano para saber si el hilo sigue vivo. */
	private boolean alive;

	/** Mapa con los usuarios de clientes. */
	Map<String, ServerThreadForClient> clientsMap = new HashMap<String, ServerThreadForClient>();

	/** Mapa con los ids de clientes. */
	Map<Integer, String> clientsIdMap = new HashMap<Integer, String>();

	/** Mapa con los usuarios baneados. */
	Map<String, Boolean> bannedUsers = new HashMap<String, Boolean>();

	/** Servidor socket. */
	ServerSocket server;

	/**
	 * Constructor con el puerto 1500 por defecto.
	 */

	public ChatServerImpl() {
		this(DEFAULT_PORT);
	}

	/**
	 * Constructor con el puerto como parámetro de argumento.
	 *
	 * @param port the port
	 */
	public ChatServerImpl(int port) {
		this.alive = true;
		this.port = port;
	}

	/**
	 * Incrementa y devuelve el siguiente ID del cliente. El metodo es synchronized
	 * para evitar problemas de acceso concurrente y tener IDs unicos.
	 *
	 * @return id del cliente, unico.
	 */
	private synchronized int getNextId() {
		return clientId++;
	}

	/**
	 * Método que muestra los mapas por pantalla, ha sido utlizado para pruebas.
	 */
	public void mostrarMapas() {
		System.out.println("Contenido de clientsMap (Username -> Thread):");
		for (Map.Entry<String, ServerThreadForClient> entry : clientsMap.entrySet()) {
			System.out.println("Username: " + entry.getKey() + ", Thread: " + entry.getValue().toString());
		}

		System.out.println("\nContenido de clientsIdMap (ID -> Username):");
		for (Map.Entry<Integer, String> entry : clientsIdMap.entrySet()) {
			System.out.println("ID: " + entry.getKey() + ", Username: " + entry.getValue());
		}
	}

	/**
	 * Inicia la conexion a traves del puerto indicado y comienza a escuchar las
	 * peticiones.
	 */
	public void startup() {
		try {
			this.server = new ServerSocket(this.port);
			System.out.println("[" + getDateString() + "] Server started in port: " + this.port);
		} catch (IOException e) {
			System.err.println("ERROR: Unable to connect to server");
			System.exit(1);
		}
		while (alive) {
			System.out.println("Listening for connections at " + server.getInetAddress() + ":" + server.getLocalPort());
			// mostrarMapas();
			try {
				// Al aceptar conexiones inicia el hilo de servidor para ese cliente
				Socket client = server.accept();
				ServerThreadForClient clientThread = new ServerThreadForClient(client);
				clientThread.start();
			} catch (IOException e) {
				System.err.println("ERROR: Could not accept connection! Shutting down server...");
			}
		}
	}

	/**
	 * Método que finaliza y cierra el servidor, incluidas todas las conexiones con
	 * los clientes.
	 */
	public void shutdown() {
		alive = false;
		try {
			// Cierra todas las conexiones de clientes
			for (ServerThreadForClient client : clientsMap.values()) {
				client.shutdownClient();
			}
			// Cierra el socket del servidor
			if (server != null && !server.isClosed()) {
				server.close();
			}
		} catch (IOException e) {
			System.err.println("[" + getDateString() + "] Error shutting down server");
		}
	}

	/**
	 * Recibe un mensaje de un cliente y lo reenvia al resto de clientes conectados,
	 * se utilzia el id del mensaje para saber el emisor del mensaje.
	 *
	 * @param message mensaje a enviar
	 */
	public void broadcast(ChatMessage message) {
		String senderUsername = getUsernameById(message.getId());
		if (bannedUsers.getOrDefault(senderUsername, false)) {
			// Si el usuario está baneado, no hacer broadcast de su mensaje.
			return;
		}

		String time = "[" + getDateString() + "]";
		for (ServerThreadForClient handler : clientsMap.values()) {
			// Creamos un nuevo mensaje para cada cliente y lo enviamos.
			ChatMessage newMsg = new ChatMessage(message.getId(), message.getType(),
					time + " " + getUsernameById(message.getId()) + ": " + message.getMessage());
			try {
				handler.output.writeObject(newMsg);
			} catch (IOException e) {
				System.err.println("ERROR: Could not send message to client " + handler.getUsername());
				remove(handler.id);
			}
		}
	}

	/**
	 * Devuelve el username del cliente, utilizando como parametro de argumento el
	 * id del cliente. Para ello se ha implementado un mapa adiccional llamado
	 * 'clientsIdMap'.
	 *
	 * @param id del cliente
	 * @return username del cliente
	 */
	public String getUsernameById(int id) {
		return clientsIdMap.get(id);
	}

	/**
	 * Desconecta y elimina al cliente del mapa.
	 *
	 * @param id id del cliente
	 */
	@Override
	public void remove(int id) {
		// Si se encontró un nombre de usuario, proceder a removerlo
		String usernameToDelete = getUsernameById(id);
		if (usernameToDelete != null) {
			// Recupera y elimina el cliente del mapa
			ServerThreadForClient client = clientsMap.remove(usernameToDelete);
			if (client != null) {
				client.shutdownClient(); // cerramos la conexión correctamente
				clientsIdMap.remove(id);
				// mostramos mensajes informativos
				System.out.println("[" + getDateString() + "] Client " + usernameToDelete + " removed.");
				System.out.println("Connected clients: " + clientsMap.size());
			}
		} else {
			System.out.println("[" + getDateString() + "] No client found with ID: " + usernameToDelete);
		}
	}

	/**
	 * Devuelve la hora exacta en formato texto, este metodo es utilizado para
	 * mostrar por pantalla los mensajes.
	 *
	 * @return fecha en formato texto
	 */
	public String getDateString() {
		return sdf.format(new Date());
	}

	/**
	 * Metodo principal, inicia el servidor.
	 *
	 * @param args Argumentos del main
	 */
	public static void main(String[] args) {

		new ChatServerImpl().startup();
	}

	/**
	 * Clase interna ServerThreadForClient Hilo que gestiona la comunicación entre
	 * el cliente y el servidor.
	 */
	class ServerThreadForClient extends Thread {

		/** Id del cliente. */
		private int id;

		/** Boleano, indica si el hilo esta corriendo. */
		private boolean running;

		/** Username del cliente. */
		private String username;

		/** Socket del cliente. */
		private Socket socket;

		/** Input. */
		private ObjectInputStream input;

		/** Output. */
		private ObjectOutputStream output;

		/**
		 * Constructor.
		 *
		 * @param socket Socket
		 */
		public ServerThreadForClient(Socket socket) {
			// inicializa el socket, la bandera y socket entrada y salida
			this.socket = socket;
			this.running = true;
			try {
				output = new ObjectOutputStream(socket.getOutputStream());
				input = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				System.err.println("ERROR: Could not create connection handler thread!");
			}
		}

		/**
		 * Realiza las acciones necesarias para conectar y comunicar con un cliente,
		 * depende el tipo de mensaje de, el servidor lo realizará unas acciones u
		 * otras.
		 *
		 * @see MessageType
		 */
		@Override
		public void run() {
			try {
				loginUser();
				while (running) {
					ChatMessage message = (ChatMessage) input.readObject();
					switch (message.getType()) {
					case MESSAGE:
						showTypeMessage(message);
						// System.out.println("[" + getDateString() + "] Message received from client #"
						// + message.getId()
						// + ": " + message.getMessage());
						break;
					case LOGOUT:
						remove(id);
						shutdownClient();
						System.out.println("[" + getDateString() + "] Disconnected user: " + getUsername());
						running = false;
						break;
					case SHUTDOWN:
						if (this.username.equalsIgnoreCase("ADMIN")) {
							System.out.println("[" + getDateString()
									+ "] SHUTDOWN command received from admin. Shutting down server...");
							ChatServerImpl.this.shutdown(); // Llama al método shutdown del servidor
							return; // Sale del bucle y termina este hilo
						} else {
							System.out
									.println("[" + getDateString() + "] SHUTDOWN command received from non-admin user: "
											+ getUsername() + ". Ignoring.");
						}
						break;
					default:
						break;
					}
				}

			} catch (ClassNotFoundException | IOException e) {
				System.err.println("ERROR: Connection lost with client " + getUsername() + "\n");
				remove(id); // Si el usuario ha sido expulsado por otro, se eliminará antes de este remove
				shutdownClient(); // finaliza el cliente
			}
		}

		/**
		 * Comprueba el mensaje y si comienza por algun comando reconocido, se puede
		 * tirar la conexión a otro usuario, banear o desbanear, cualquier usuario puede
		 * realizar estas acciones mientras no esté baneado del servidor
		 *
		 * @param message mensaje a comprobar y enviar
		 */
		private void showTypeMessage(ChatMessage message) {
			// Si el usuario está baneado, no se aceptan mensajes suyos ni comandos
			if (bannedUsers.getOrDefault(this.username, false)) {
				return;
			}

			List<String> commandAndUser = extractCommandAndUser(message.getMessage());
			// si se ha reconococido un comando...
			if (commandAndUser != null) {
				String command = commandAndUser.get(0);
				String username = commandAndUser.get(1);
				switch (command) {
				case "drop":
					dropUser(username); // drop username by other client
					break;
				case "ban":
					banUser(username, true); // ban username by other client
					break;
				case "unban":
					banUser(username, false); // unban username by other client
					break;
				default:
					break;
				}
			} else {
				broadcast(message); // emitimos el mensaje
			}
		}

		/**
		 * Extrae el comando y el usuario de un mensaje, y devuelve una lista de dos
		 * elementos.
		 *
		 * @param mensaje a analizar en modo texto
		 * @return lista de dos elementos, el primero el comando y el segundo el
		 *         username
		 */
		public List<String> extractCommandAndUser(String mensaje) {
			List<String> commands = Arrays.asList("drop", "ban", "unban");
			// Divide el mensaje en 2 partes, usando el espacio.
			String[] parts = mensaje.split(" ", 2);

			// Comprobamos que hay dos partes en el mensaje
			if (parts.length == 2) {
				String command = parts[0].toLowerCase(); // la primera palabra es el comando
				if (commands.contains(command)) {
					return Arrays.asList(command, parts[1]); // la segundda palabra el username
				}
			}
			return null; // devuelve null si no es un comando valido
		}

		/**
		 * Método login, se utilzia para iniciar la conexión con el cliente, lee el
		 * primer mensaje y establece el id y username, los almacena en los mapas.
		 *
		 * @throws IOException            Signals that an I/O exception has occurred.
		 * @throws ClassNotFoundException the class not found exception
		 */
		private void loginUser() throws IOException, ClassNotFoundException {
			// Lee el primer mensaje que contiene el nombre de usuario
			ChatMessage loginMessage = (ChatMessage) input.readObject();
			if (loginMessage.getType() == MessageType.MESSAGE) {
				this.username = loginMessage.getMessage();
				// Verificamos si el username ya existe
				if (!checkUsername(getUsername())) {
					// Si el nombre de usuario ya existe, cierra la conexión y sale.
					shutdownClient(); // cerramos correctamente la conexión aquí
					System.err.println("[" + getDateString() + "] Connection terminated for client " + getUsername()
							+ ". This username already exists.");
					return; // Salir del método run sin agregar al cliente al mapa
				} else {
					// Si el nombre de usuario es único, procede como de costumbre
					clientsMap.put(getUsername(), this);
					this.id = getNextId();
					clientsIdMap.put(id, getUsername());
					sendInitialConnectionMessage();
					System.out.println("Connected clients: " + clientsMap.size());
				}
			} else {
				// Si el primer mensaje no es del tipo esperado
				System.err.println("ERROR: Expected username message, received something else. Closing connection.");
				shutdownClient();
				return;
			}
		}

		/**
		 * Banea o desbanea a un usuario, no tiene porqué estar conectado al servidor.
		 * Recibe como parametros el nombre del usuario a banear y true si se quiere
		 * banear o false si se quiere desbanear.
		 *
		 * @param username del cliente a banear
		 * @param ban      true si es ban o false si no
		 */
		private void banUser(String username, boolean ban) {
			bannedUsers.put(username, ban);
			if (ban == true) {
				System.out.println("[" + getDateString() + "] The client " + username + " has been banned by "
						+ this.getUsername());
				broadcast(new ChatMessage(this.id, MessageType.MESSAGE,
						"The client " + username + " has been banned by " + this.getUsername()));
			} else {
				System.out.println("[" + getDateString() + "] The client " + username + " has been unbanned by "
						+ this.getUsername());
				broadcast(new ChatMessage(this.id, MessageType.MESSAGE,
						"The client " + username + " has been unbanned by " + this.getUsername()));
			}
		}

		/**
		 * Drop user, elimina la conexión de un cliente, este metodo es llamado cuando
		 * un usuario quiere tirar la conexión de otro.
		 *
		 * @param username nombre de usuario del cliente a tirar la conexion
		 */
		private void dropUser(String username) {
			ServerThreadForClient clientToDrop = clientsMap.get(username);
			if (clientToDrop != null) {
				System.out.println("[" + getDateString() + "] The client " + username + " has been dropped by "
						+ this.getUsername());
				clientToDrop.shutdownClient(); // Desconecta al cliente.
				remove(clientToDrop.id); // Elimina al cliente del mapa de clientes.
				try {
					output.writeObject(new ChatMessage(this.id, MessageType.MESSAGE, "[" + getDateString()
							+ "] The client " + username + " has been dropped by " + this.getUsername()));
				} catch (IOException e) {
					System.err.println("ERROR: Sending drop failure message to " + getUsername());
				}
			} else {
				try {
					output.writeObject(new ChatMessage(this.id, MessageType.MESSAGE,
							"[" + getDateString() + "] User " + username + " not found."));
				} catch (IOException e) {
					System.err.println("ERROR: Sending drop failure message to " + getUsername());
				}
			}
		}

		/**
		 * Comprueba si el usuario ya existe.
		 *
		 * @param username del cliente
		 * @return true si el usuario no existe y esta libre, false si no
		 */
		private boolean checkUsername(String username) {
			synchronized (clientsMap) {
				if (clientsMap.containsKey(username)) {
					try {
						output.writeObject(new ChatMessage(0, MessageType.LOGOUT, "Username already exists."));
					} catch (IOException e) {
						System.err.println("ERROR: Could not send username exists message to client.");
					}
					return false;
				}
				return true;
			}
		}

		/**
		 * Devuelve el username del cliente, de este cliente.
		 *
		 * @return username del cliente actual
		 */
		public String getUsername() {
			return this.username;
		}

		/**
		 * Muestra un mensaje de bienvenida al usuario, con su id.
		 */
		private void sendInitialConnectionMessage() {
			try {
				String welcomeMessage = String.format("[%s] Welcome, %s! Your ID is %d. Waiting for a message...",
						getDateString(), getUsername(), id);
				output.writeObject(new ChatMessage(id, MessageType.MESSAGE, welcomeMessage));
				System.out.println("[" + getDateString() + "] " + getUsername() + " has just connected to the server");
			} catch (IOException e) {
				System.err.println("ERROR: Could not send initial connection message to client " + getUsername());
			}
		}

		/**
		 * Cierra las conexiones con los clientes.
		 */
		private void shutdownClient() {
			try {
				running = false;
				if (input != null) input.close();
				if (output != null) output.close();
				if (socket != null) socket.close();
			} catch (IOException e) {
				System.err.println("Error closing the connection to client " + getUsername());
			}
		}
	}

}
