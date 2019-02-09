package mekhq.gui.stratcon;

import javax.swing.JInternalFrame;
import javax.swing.JLabel;

public class InfoFrame extends JInternalFrame {
        /**
     * 
     */
    private static final long serialVersionUID = 5314122796511795555L;
    private JLabel infoLabel = new JLabel();

    /**
     * Constructor. Sets up a JInternalFrame that's resizable and closable but can't be mini/maximized or turned into an icon (?).
     */
    public InfoFrame() {
        super("Detailed Info", true, true, false, false);

        this.add(infoLabel);
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    /**
     * Displays an arbitrary piece of text on the embedded label.
     * @param text
     */
    public void displayInfo(String text) {
        infoLabel.setText(text);
        this.validate();
    }
}