package chat;

/*
 * Code taken from http://introcs.cs.princeton.edu/java/84network/
 */

import java.net.Socket;

public class Connection extends Thread {
    private Socket socket;
    private Out out;
    private In in;
    private String message;     // one line buffer

    public Connection(Socket socket) {
        in  = new In(socket);
        out = new Out(socket);
        this.socket = socket;
    }

    public void println(String s) { out.println(s); }

    public void run() {
        String s;
        while ((s = in.readLine()) != null) {
            setMessage(s);
        }
        out.close();
        in.close();
        try                 { socket.close();      }
        catch (Exception e) { e.printStackTrace(); }
        System.err.println("closing socket");
    }


   /*********************************************************************
    *  The methods getMessage() and setMessage() are synchronized
    *  so that the thread in Connection doesn't call setMessage()
    *  while the ConnectionListener thread is calling getMessage().
    *********************************************************************/
    public synchronized String getMessage() {
        if (message == null) return null;
        String temp = message;
        message = null;
        notifyAll();
        return temp;
    }

    public synchronized void setMessage(String s) {
        if (message != null) {
            try                  { wait();               }
            catch (Exception ex) { ex.printStackTrace(); }
        }
        message = s;
    }

}
