package chatSocketMix;
import javax.swing.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.*;
import java.util.ArrayList;

public class Cliente {
	
	public static void main(String args []) {
	
	FrameClient myFrame = new FrameClient();
	myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
}

class FrameClient extends JFrame{ //Interfaz gráfica del chat
	
	public FrameClient() {
		
		setTitle("CHAT");
		setBounds(600, 300, 280, 350);
		BoardClient myBoard = new BoardClient();
		add(myBoard);
		setVisible(true);
	}
}


class BoardClient extends JPanel implements Runnable{ //Gestionada con hilos
	
	private JTextField sendField;
	private JTextArea chatArea;
	private JButton myButton;
	
	private Socket clientSocket;
	private String nickname;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	
	public BoardClient() {
		
		//SOLICITAMOS EL NICK DEL CLIENTE
		
		nickname = "";
		while (nickname.equals("")) {
		 nickname = JOptionPane.showInputDialog("Enter a nickname: "); 
		}
		
		//AVISAMOS AL SERVER DE LA CONEXION
		
		String ip = JOptionPane.showInputDialog("Enter a valid IP: "); 
		int port = 31863;
		
		try {
			clientSocket = new Socket(ip, port);  //IpServer, PortServer
		} catch (UnknownHostException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		} //IpServer, PortServer
		
		
		try {
			//Creamos los Stream para comunicarnos con el servidor
			output =new ObjectOutputStream(clientSocket.getOutputStream());
			input = new ObjectInputStream(clientSocket.getInputStream());
			//Enviamos un primer mensaje especial de conexión
			Paquete connectionAdvise = new Paquete();
			connectionAdvise.setNick(nickname);
			connectionAdvise.setMsg("online");
			
			output.writeObject(connectionAdvise);
			
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		//EMPEZAMOS A CHATEAR
		JLabel myNick = new JLabel("Nick: ");
		add(myNick);
		JLabel nickField = new JLabel(nickname);
		add(nickField);
		
		
		chatArea = new JTextArea(12,20);
		add(chatArea);
		
		sendField = new JTextField(20);
		add(sendField);
		
		myButton = new JButton ("Send");
		SendText myEvent = new SendText();
		myButton.addActionListener(myEvent);
		add(myButton);
		
		Thread clientThread = new Thread(this);
		clientThread.start();
	}
	
	private class SendText implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			System.out.println(sendField.getText());
			
			//Hacemos que aparezca en el chat lo que el propio cliente escribe
			chatArea.append("\nMe: " + sendField.getText());
			
			//Creamos el socket, el cliente envía 
			try {
				
				//Rellenamos paquete que enviamos
				Paquete sendPaquete = new Paquete();
				sendPaquete.setNick(nickname);
				sendPaquete.setMsg(sendField.getText());
				
				//Lo enviamos
				output.writeObject(sendPaquete);
				
			} catch (IOException e) {
				closeEverything(clientSocket, input, output);
			}
		}
		
	}
	

	@Override
	public void run() {
		
		try {
			
			Paquete paqueteReceived;
			
			while(clientSocket.isConnected()) {
				
				//Recibimos el paquete
				paqueteReceived = (Paquete) input.readObject();
				if (paqueteReceived.getNick().equals("Connection Advise")) {
					
					System.out.println("SERVER: " + paqueteReceived.getNick() + ": " + paqueteReceived.getMsg());
					chatArea.append("\n>> SERVER: " + paqueteReceived.getMsg());
					
				} else {
					System.out.println(paqueteReceived.getNick() + ": " + paqueteReceived.getMsg());
					chatArea.append("\n" + paqueteReceived.getNick() + ": " + paqueteReceived.getMsg());
				}			
			}
			
		} catch (IOException | ClassNotFoundException e) {
			closeEverything(clientSocket, input, output);			
		}
	}
	
	public void closeEverything(Socket socket, ObjectInputStream input, ObjectOutputStream output) {
		
		try {
			if(output != null) {
				output.close();
			}
			if(input != null) {
				input.close();
			}
			if(socket != null) {
				socket.close();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		
	}
}

class Paquete implements Serializable {
	private String  nick, msg;

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

}
