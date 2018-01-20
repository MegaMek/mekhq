package chat;

import java.util.Vector;

/*
 * Code taken from http://introcs.cs.princeton.edu/java/84network/
 */

public class ConnectionListener extends Thread {
    private Vector<Connection> connections;

    public ConnectionListener(Vector<Connection> connections) {
        this.connections = connections;
    }

    // check for incoming messages and broadcast
    public void run() {
        while (true) {
            for (int i = 0; i < connections.size(); i++) {
                Connection ith = connections.get(i);

                // if connection terminated, remove from list of active connections
                if (!ith.isAlive())
                    connections.remove(i);

                // broadcast to everyone
                String message = ith.getMessage();
                if (message != null)
                    for (Connection jth : connections)
                        jth.println(message);

            }

            // don't monopolize processor
            try                 { Thread.sleep(100);   }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

}
