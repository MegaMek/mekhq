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

public class TransitMutinyBattleConditionDialog extends JDialog {
    /**
     * Displays a dialog for the onset of a mutiny while in transit.
     *
     * @param resources the resource bundle containing the dialog text and options
     */
    public static void transitMutinyBattleConditionDialog(ResourceBundle resources, Random random,
                                                          int loyalistGraveyardSize, int mutineerGraveyardSize) {
        StringBuilder conditionDescription = new StringBuilder(randomBattleCondition(resources, random));

        conditionDescription.append(String.format(resources.getString("abstractBattleDescriptionCasualties.text"),
                loyalistGraveyardSize,
                mutineerGraveyardSize));

        JOptionPane pane = new JOptionPane(conditionDescription.toString(),
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
                "abstractBattleDescription01.text",
                "abstractBattleDescription02.text",
                "abstractBattleDescription03.text",
                "abstractBattleDescription04.text",
                "abstractBattleDescription05.text",
                "abstractBattleDescription06.text",
                "abstractBattleDescription07.text",
                "abstractBattleDescription08.text",
                "abstractBattleDescription09.text",
                "abstractBattleDescription10.text",
                "abstractBattleDescription11.text",
                "abstractBattleDescription12.text",
                "abstractBattleDescription13.text",
                "abstractBattleDescription14.text",
                "abstractBattleDescription15.text",
                "abstractBattleDescription16.text",
                "abstractBattleDescription17.text",
                "abstractBattleDescription18.text",
                "abstractBattleDescription19.text",
                "abstractBattleDescription20.text",
                "abstractBattleDescription21.text",
                "abstractBattleDescription22.text",
                "abstractBattleDescription23.text",
                "abstractBattleDescription24.text",
                "abstractBattleDescription25.text",
                "abstractBattleDescription26.text",
                "abstractBattleDescription27.text",
                "abstractBattleDescription28.text",
                "abstractBattleDescription29.text",
                "abstractBattleDescription30.text"
        );

        return String.format(resources.getString(conditions.get(random.nextInt(conditions.size()))), attacker, defender);
    }
}