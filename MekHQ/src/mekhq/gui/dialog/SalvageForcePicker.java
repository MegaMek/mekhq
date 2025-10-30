package mekhq.gui.dialog;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Warship;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.camOpsSalvage.CamOpsSalvageUtilities;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

public class SalvageForcePicker extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SalvageForcePicker";
    private static final int NUM_COLUMNS = 3;

    public final int SELECTION_CANCELLED = 0;
    public final int SELECTION_CONFIRMED = 1;

    private static Map<JCheckBox, Force> checkboxForceMap;

    /**
     * Checks whether the user confirmed their force selection.
     *
     * @return {@code true} if the user confirmed their selection, {@code false} if they canceled
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean wasConfirmed() {
        return getDialogChoice() == SELECTION_CONFIRMED;
    }

    /**
     * Retrieves the list of forces that were selected by the user.
     *
     * <p>This method examines all checkboxes in the dialog and returns a list of forces corresponding
     * to the checked checkboxes. If no checkboxes are selected or the dialog was canceled, an empty list is
     * returned.</p>
     *
     * @return a list of selected {@link Force} objects, or an empty list if none were selected
     *
     * @author Illiani
     * @since 0.50.10
     */
    public List<Force> getSelectedForces() {
        List<Force> selectedForces = new ArrayList<>();

        if (checkboxForceMap != null) {
            for (Map.Entry<JCheckBox, Force> entry : checkboxForceMap.entrySet()) {
                if (entry.getKey().isSelected()) {
                    selectedForces.add(entry.getValue());
                }
            }
        }

        return selectedForces;
    }

    public SalvageForcePicker(Campaign campaign, Scenario scenario, List<Force> forces) {
        super(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND),
              null,
              getInCharacterMessage(campaign.getCommanderAddress(), !forces.isEmpty()),
              getButtons(!forces.isEmpty()),
              null,
              ImmersiveDialogWidth.SMALL.getWidth(),
              false,
              getSupplementalPanel(scenario.getBoardType() == AtBScenario.T_SPACE, campaign.getHangar(), forces),
              null,
              true);
    }

    /**
     * Generates the in-character message displayed in the dialog.
     *
     * <p>The message varies depending on whether forces are available for deployment.</p>
     *
     * @param commanderAddress the formal address/title of the campaign commander
     * @param hasForces        {@code true} if forces are available, {@code false} otherwise
     *
     * @return the formatted in-character message string
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getInCharacterMessage(String commanderAddress, boolean hasForces) {
        String key = "SalvageForcePicker.inCharacterMessage." + (hasForces ? "normal" : "noForces");
        return getFormattedTextAt(RESOURCE_BUNDLE, key, commanderAddress);
    }

    /**
     * Creates the list of buttons to display in the dialog.
     *
     * <p>Always includes a Cancel button. If forces are available, also includes a Confirm button.</p>
     *
     * @param hasForces {@code true} if forces are available for selection, {@code false} otherwise
     *
     * @return a list of button configurations for the dialog
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static List<ImmersiveDialogCore.ButtonLabelTooltipPair> getButtons(boolean hasForces) {
        List<ImmersiveDialogCore.ButtonLabelTooltipPair> buttons = new ArrayList<>();
        buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(getText("Cancel.text"), null));

        if (hasForces) {
            buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(getText("Confirm.text"), null));
        }

        return buttons;
    }

    /**
     * Creates the supplemental panel containing force selection checkboxes.
     *
     * <p>This panel is displayed below the main dialog message and contains checkboxes arranged in three
     * columns. Each checkbox represents a force that can be selected for salvage operations. The checkboxes are labeled
     * with the force's name.</p>
     *
     * @param isInSpace {@code true} if the scenario is a space scenario
     * @param hangar    the current campaign hangar
     * @param forces    the list of forces to display as checkboxes
     *
     * @return a {@link JPanel} containing the force selection UI with checkboxes arranged in three columns
     *
     * @author Illiani
     * @since 0.50.10
     */
    protected static JPanel getSupplementalPanel(boolean isInSpace, Hangar hangar, List<Force> forces) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblForces = new JLabel(getTextAt(RESOURCE_BUNDLE, "SalvageForcePicker.combo.label"));
        panel.add(lblForces, constraints);

        // Create panel with three columns
        JPanel checkboxPanel = new JPanel(new GridLayout(1, NUM_COLUMNS, 10, 0));
        checkboxPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        JPanel middleColumn = new JPanel();
        middleColumn.setLayout(new BoxLayout(middleColumn, BoxLayout.Y_AXIS));
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));

        checkboxForceMap = new LinkedHashMap<>();

        // Create checkboxes for each force
        for (int i = 0; i < forces.size(); i++) {
            Force force = forces.get(i);
            List<Unit> allUnitsInForce = force.getAllUnitsAsUnits(hangar, false);
            JCheckBox checkbox =
                  new JCheckBox(force.getFullName() + " (" + force.getSalvageUnitCount(hangar, isInSpace) + ")");
            checkbox.setToolTipText(getSalvageTooltip(allUnitsInForce, isInSpace));
            checkbox.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            checkboxForceMap.put(checkbox, force);

            // Distribute checkboxes across three columns
            if (i % NUM_COLUMNS == 0) {
                leftColumn.add(checkbox);
            } else if (i % NUM_COLUMNS == 1) {
                middleColumn.add(checkbox);
            } else {
                rightColumn.add(checkbox);
            }
        }

        checkboxPanel.add(leftColumn);
        checkboxPanel.add(middleColumn);
        checkboxPanel.add(rightColumn);

        constraints.gridy = 1;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(checkboxPanel, constraints);

        return panel;
    }

    private static String getSalvageTooltip(List<Unit> unitsInForce, boolean isInSpace) {
        StringBuilder tooltip = new StringBuilder();

        for (Unit unit : unitsInForce) {
            if (unit.canSalvage(isInSpace)) {
                Entity entity = unit.getEntity();
                if (entity != null) {
                    boolean isLargeVessel = entity instanceof Dropship || entity instanceof Warship;
                    tooltip.append(unit.getName());

                    if (!isLargeVessel) {
                        tooltip.append(" (").append(entity.getTonnage()).append(" tons drag/tow)");
                    }

                    double cargoCapacity = unit.getCargoCapacity();
                    if (!(entity instanceof Mek)) {
                        tooltip.append(" (").append(cargoCapacity).append(" tons cargo)");

                        if (isLargeVessel) {
                            if (CamOpsSalvageUtilities.hasNavalTug(entity)) {
                                tooltip.append(" (Has Naval Tug)");
                            }
                        }
                    }
                }

                tooltip.append("<br>");
            }
        }

        return tooltip.toString();
    }
}
