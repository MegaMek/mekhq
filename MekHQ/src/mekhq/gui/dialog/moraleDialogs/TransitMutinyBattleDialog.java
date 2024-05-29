package mekhq.gui.dialog.moraleDialogs;

import megamek.common.Compute;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public class TransitMutinyBattleDialog extends JDialog {
    /**
     * Displays a dialog for the onset of a mutiny while in transit.
     *
     * @param resources the resource bundle containing the dialog text and options
     */
    public static void transitMutinyBattleConditionDialog(ResourceBundle resources, Random random) {
        JOptionPane pane = new JOptionPane(
                randomBattleCondition(resources, random),
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
    }

    /**
     * Generates a random battle condition using the provided resources and random number generator.
     *
     * @param resources the ResourceBundle containing the battle condition texts
     * @param random the Random object used to generate random numbers
     * @return a randomly selected battle condition as a formatted String
     * @throws IllegalStateException if the generated random number is out of range
     */
    private static String randomBattleCondition(ResourceBundle resources, Random random) {
        String attacker;
        String defender;

        int roll = Compute.randomInt(2);

        switch (roll) {
            case 0:
                attacker = resources.getString("abstractBattleFactionLoyalists.text");
                defender = resources.getString("abstractBattleFactionMutineers.text");
                break;
            case 1:
                attacker = resources.getString("abstractBattleFactionMutineers.text");
                defender = resources.getString("abstractBattleFactionLoyalists.text");
                break;
            default:
                throw new IllegalStateException("Unexpected value in randomBattleCondition " + roll);
        }

        List<String> conditions = Arrays.asList(
                String.format(resources.getString("abstractBattleDescription01.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription02.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription03.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription04.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription05.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription06.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription07.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription08.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription09.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription10.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription11.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription12.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription13.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription14.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription15.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription16.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription17.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription18.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription19.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription20.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription21.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription22.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription23.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription24.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription25.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription26.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription27.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription28.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription29.text"), attacker, defender),
                String.format(resources.getString("abstractBattleDescription30.text"), attacker, defender)
        );

        return conditions.get(random.nextInt(conditions.size()));
    }
}