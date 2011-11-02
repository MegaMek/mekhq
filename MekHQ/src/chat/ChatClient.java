package chat;

/*
 * Code taken from http://introcs.cs.princeton.edu/java/84network/
 */

import java.awt.*;
import java.awt.event.*;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.net.Socket;

public class ChatClient extends JPanel implements ActionListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = -7447573101863923187L;

	private String screenName;

    // GUI stuff
    private JTextArea  enteredText = new JTextArea(10, 32);
    private JTextField typedText   = new JTextField(32);

    // socket for connection to chat server
    private Socket socket;

    // for writing to and reading from the server
    private Out out;
    private In in;

    public ChatClient(String screenName, String hostName) {

        // connect to server
        try {
            socket = new Socket(hostName, 4444);
            out    = new Out(socket);
            in     = new In(socket);
        }
        catch (Exception ex) { ex.printStackTrace(); }
        this.screenName = screenName;

    /*    // close output stream  - this will cause listen() to stop and exit
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    out.close();
//                    in.close();
//                    try                   { socket.close();        }
//                    catch (Exception ioe) { ioe.printStackTrace(); }
                }
            }
        );
*/

        // create GUI stuff
        enteredText.setEditable(false);
        enteredText.setBackground(Color.LIGHT_GRAY);
        typedText.addActionListener(this);

        //Container content = getContentPane();
        add(new JScrollPane(enteredText), BorderLayout.CENTER);
        add(typedText, BorderLayout.SOUTH);


        // display the window, with focus on typing box
        //setTitle("MekHQ Chat: [" + screenName + "]");
        //setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        //pack();
        typedText.requestFocusInWindow();
    }

    // process TextField after user hits Enter
    public void actionPerformed(ActionEvent e) {
        out.println("[" + screenName + "]: " + typedText.getText());
        typedText.setText("");
        typedText.requestFocusInWindow();
    }

    // listen to socket and print everything that server broadcasts
    public void listen() {
        String s;
        while ((s = in.readLine()) != null) {
            enteredText.insert(s + "\n", enteredText.getText().length());
            enteredText.setCaretPosition(enteredText.getText().length());
        }
        out.close();
        in.close();
        try                 { socket.close();      }
        catch (Exception e) { e.printStackTrace(); }
        System.err.println("Closed client socket");
    }
}
