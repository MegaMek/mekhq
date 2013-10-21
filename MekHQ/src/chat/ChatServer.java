package chat;

/*
 * Code taken from http://introcs.cs.princeton.edu/java/84network/
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class ChatServer {
	
	// Moved this out here and added a die method just to make the warning shut the funk up
	static ServerSocket serverSocket;

    public static void main(String[] args) throws Exception {
        Vector<Connection> connections        = new Vector<Connection>();
        serverSocket             = new ServerSocket(4444);
        ConnectionListener connectionListener = new ConnectionListener(connections);

        // thread that broadcasts messages to clients
        connectionListener.start();

        System.err.println("ChatServer started");

        while (true) {

            // wait for next client connection request
            Socket clientSocket = serverSocket.accept();
            System.err.println("Created socket with client");

            // listen to client in a separate thread
            Connection connection = new Connection(clientSocket);
            connections.add(connection);
            connection.start();
        }
    }
    
    public void die() {
    	// close socket
        try {
            serverSocket.close();
        } catch (IOException ex) {
        }
    }
}