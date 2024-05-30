package mekhq.gui.dialog.moraleDialogs;

import mekhq.campaign.personnel.Person;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class TransitMutinyToeDialog extends JDialog {
    /**
     * Displays a dialog for the onset of a mutiny while in transit.
     *
     * @param resources the resource bundle containing the dialog text and options
     */
    public static int transitMutinyToeDialog(ResourceBundle resources,
                                              Person loyalistLeader, int loyalistCount, int loyalistAttackPower, int loyalistDefensePower,
                                              Person mutineerLeader, int mutineerCount, int mutineerAttackPower, int mutineerDefensePower) {
        // this builds the text for the dialog
        StringBuilder toeDescription = new StringBuilder("<html>");

        toeDescription.append(String.format(resources.getString("abstractBattleToe.text"),
                loyalistLeader.getFullTitle(),
                resources.getString("abstractBattleFactionLoyalists.text"),
                loyalistCount,
                loyalistAttackPower,
                loyalistDefensePower,
                loyalistAttackPower - (mutineerDefensePower / 6)));

        toeDescription.append(String.format(resources.getString("abstractBattleToe.text"),
                mutineerLeader.getFullTitle(),
                resources.getString("abstractBattleFactionMutineers.text"),
                mutineerCount,
                mutineerAttackPower,
                mutineerDefensePower,
                mutineerAttackPower - (loyalistDefensePower / 6)));

        toeDescription.append("</html>");

        // this builds the dialog
        AtomicInteger choice = new AtomicInteger(-1);
        JOptionPane pane = new JOptionPane(
                toeDescription,
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                new Object[]{},
                null
        );

        Object[] options = {
                resources.getString("abstractBattleFactionSupportLoyalists.text"),
                resources.getString("abstractBattleFactionSupportMutineers.text"),
                resources.getString("abstractBattleFactionSupportVictor.text"),
        };

        String[] tooltips = {
                resources.getString("abstractBattleFactionSupportLoyalists.toolTip"),
                resources.getString("abstractBattleFactionSupportMutineers.toolTip"),
                resources.getString("abstractBattleFactionSupportVictor.toolTip"),
        };

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        for (int index = 0; index < options.length; index++) {
            JButton button = new JButton(options[index].toString());
            button.setToolTipText(tooltips[index]);
            int finalI = index;
            button.addActionListener(e -> {
                choice.set(finalI);

                Window window = SwingUtilities.getWindowAncestor(button);
                if (window != null) {
                    window.setVisible(false);
                }
            });

            buttonPanel.add(button);
            buttonPanel.add(Box.createHorizontalStrut(15));
        }

        pane.setOptions(new Object[]{buttonPanel});

        JDialog dialog = pane.createDialog(null, resources.getString("abstractMutiny.title"));

        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setResizable(false);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Do nothing
            }
        });

        dialog.setVisible(true);

        return choice.get();
    }
}