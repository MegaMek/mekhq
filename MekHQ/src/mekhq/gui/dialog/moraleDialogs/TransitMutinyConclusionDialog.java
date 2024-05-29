package mekhq.gui.dialog.moraleDialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

public class TransitMutinyConclusionDialog extends JDialog {
    /**
     * Displays a dialog for the onset of a mutiny while in transit.
     *
     * @param resources the resource bundle containing the dialog text and options
     */
    public static void transitMutinyConclusionDialog(ResourceBundle resources, int victor) {
        String title;
        String description;

        switch (victor) {
            case -1:
                // mutual destruction
                title = resources.getString("abstractMutinyConclusionNoVictor.title");
                description = resources.getString("abstractMutinyConclusionNoVictorDescription.text");
                break;
            case 0:
                // loyalist
                title = resources.getString("abstractMutinyConclusionLoyalistVictory.title");
                description = resources.getString("abstractMutinyConclusionLoyalistVictory.text");
                break;
            case 1:
                // mutineer
                title = resources.getString("abstractMutinyConclusionMutineerVictory.title");
                description = resources.getString("abstractMutinyConclusionMutineerVictory.text");
                break;
            default:
                throw new IllegalStateException("Unexpected value in transitMutinyConclusionDialog: " + victor);
        }

        JOptionPane pane = new JOptionPane(
                description,
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                new Object[]{},
                null
        );

        Object[] options = {
                resources.getString("abstractMutinyButtonConfirm.text")
        };

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        for (Object option : options){
            JButton button = new JButton(option.toString());
            button.addActionListener(e -> {
                Window window = SwingUtilities.getWindowAncestor(button);
                if (window != null) {
                    window.setVisible(false);
                }
            });

            buttonPanel.add(button);
            buttonPanel.add(Box.createHorizontalStrut(15));
        }

        pane.setOptions(new Object[]{buttonPanel});

        JDialog dialog = pane.createDialog(null, title);

        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setResizable(false);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Do nothing
            }
        });

        dialog.setVisible(true);
    }
}