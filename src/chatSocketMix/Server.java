package chatSocketMix;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

public class Server {
	
	//Vamos a necesitar un serverSocket	
	private ServerSocket serverSocket;
	
	//Cuando creemos el servidor, deberemos definir el serverSocket
	public Server(ServerSocket serverSocket) {
		super();
		this.serverSocket = serverSocket;
	}
	
	
	//MAIN DEL SERVER
	public static void main(String args []) throws IOException {
		
		ServerSocket serverSocket = new ServerSocket(32000);
		Server server = new Server(serverSocket);
		server.startServer();
	}
	


	public void startServer() {
		
		System.out.println("SERVER: He iniciado");
		try {
			//IP SERVER
			String myIp = InetAddress.getLocalHost().getHostAddress();
			System.out.println("SERVER IP: " + myIp);
			//
			
			//Queremos que acepte las conexiones infinitamente en segundo plano (hilo)
			while(!serverSocket.isClosed()) {
				
				Socket clientSocket = serverSocket.accept();//acepta conexion de nuevo cliente
				System.out.println("A new client has connected");
				
				ClientHandler clientHandler = new ClientHandler(clientSocket); //gestiona cada nuevo cliente
				Thread thread = new Thread(clientHandler); //y cada nuevo cliente será gestionado por un hilo
				thread.start();

					
			}
		}catch(IOException e) {
		  closeServerSocket();		
		}
	}
	
	public void closeServerSocket() {
		try {
			if (serverSocket != null) {
				serverSocket.close();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}



class ClientHandler implements Runnable{
	
	public static java.util.List<ClientHandler> listClients = Collections.synchronizedList(new ArrayList<ClientHandler>());
	private Socket clientSocket;
	private String nickname;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	
	
	public ClientHandler(Socket socket) {
		
		try {
			this.clientSocket = socket;					//Asociamos el socket del cliente
			
			this.input = new ObjectInputStream(clientSocket.getInputStream()); 		//Creamos los Stream para comunicarse con el cliente
			this.output = new ObjectOutputStream(clientSocket.getOutputStream());
			
			Paquete paquete = (Paquete) input.readObject();		//Estructura que se usará para la comunicación
			this.nickname = paquete.getNick();					//Recibe el nick elegido por el cliente
			
			listClients.add(this);								//Añade el cliente a la lista de clientes
			System.out.println("\n" + nickname + " is connected!");
			paquete.setMsg(nickname + " has entered the chat!");
			paquete.setNick("Connection Advise");				//Envía un mensaje especial avisando a todos los clientes
			broadcastMessage(paquete);							//que un nuevo cliente se ha conectado
		
		} catch (IOException | ClassNotFoundException e) {
			closeEverything(this);		
		}
	}

	@Override
	public void run() {
				
		while (!clientSocket.isClosed()) { //TODO modifico .isConnected por .isClosed
			try {						  //Va recibiendo mensaje de los clientes y los reenvía a todos
				Paquete received = (Paquete) input.readObject();
				System.out.println("\n" + received.getNick() + " says: "+ received.getMsg());
				broadcastMessage(received);		
				
			} catch (IOException | ClassNotFoundException e) {
			
				closeEverything(this);
			}
		}	
	}
	
	public void broadcastMessage(Paquete p) {
		
		for (ClientHandler client: listClients) {	//Reenvía a todos los clientes los mensajes de los demás
			try {
				if(!client.nickname.equals(nickname)) {
					client.output.writeObject(p);	
				}else if (p.getNick().equals("Connection Advise")) { //Avisa al cliente de que se ha conectado correctamente
					String newMSG = "Bienvenido al chat!" + "IP: [" + InetAddress.getLocalHost().getHostAddress() + "]";
					p.setMsg(newMSG);
					client.output.writeObject(p);
				}
			} catch(IOException e) {
				closeEverything(client);
			}
		}
	}
	
	public void removeClientHandler(ClientHandler c) { //Cuando un cliente se desconecta, avisa a todos los demás
		listClients.remove(c);
		
		Paquete disconnection = new Paquete();
		disconnection.setNick("Connection Advise");
		disconnection.setMsg(nickname + " has left the chat!");
		System.out.println("\n" + disconnection.getMsg());
		broadcastMessage(disconnection);
		
	}
	
	public void closeEverything(ClientHandler c) { //Cierra todo cundo un cliente se desconecta
		
		removeClientHandler(c);
		try {
			if(c.output != null) {
				c.output.close();
			}
			if(c.input != null) {
				c.input.close();
			}
			c.clientSocket.close();
			
			
		}catch(IOException e) {
			e.printStackTrace();
		}		
	}	
}



