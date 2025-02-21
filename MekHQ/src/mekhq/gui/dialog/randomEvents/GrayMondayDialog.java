/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog.randomEvents;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.campaign.randomEvents.GrayMonday.EVENT_DATE_GRAY_MONDAY;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public class GrayMondayDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.GrayMonday";

    public GrayMondayDialog(Campaign campaign, Person speaker, boolean isClarionNote, int eventIndex) {
        super(campaign, speaker, null,
            createInCharacterMessage(campaign, isClarionNote, eventIndex),
            createButtons(), createOutOfCharacterMessage(), null);
    }

    /**
     * Creates the list of buttons to be displayed in the dialog.
     *
     * <p>The dialog includes only a confirmation button for this purpose, allowing
     * the user to acknowledge the information provided.</p>
     *
     * @return a list of {@link ButtonLabelTooltipPair} representing the dialog's buttons
     */
    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnConfirm = new ButtonLabelTooltipPair(
            getFormattedTextAt(RESOURCE_BUNDLE, "confirm.button"), null);

        return List.of(btnConfirm);
    }

    /**
     * Retrieves the left-side speaker for the dialog.
     *
     * <p>The speaker is determined as the senior administrator personnel with the Logistics
     * specialization within the campaign. If no such person exists, this method returns {@code null}.</p>
     *
     * @param campaign the {@link Campaign} containing personnel data
     * @return a {@link Person} representing the left speaker, or {@code null} if no suitable speaker is available
     */
    private static @Nullable Person getSpeaker(Campaign campaign) {
        return campaign.getSeniorAdminPerson(LOGISTICS);
    }

    private static String createInCharacterMessage(Campaign campaign, boolean isClarionNote, int eventIndex) {
        String commanderAddress = campaign.getCommanderAddress(false);
        String eventType = isClarionNote ? "clarionNote" : "grayMonday";

        return getFormattedTextAt(RESOURCE_BUNDLE, eventType + "Event" + eventIndex + ".message",
            commanderAddress);
    }


    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "dialog.ooc");
    }

    @Override
    protected JPanel buildSpeakerPanel(@Nullable Person speaker, Campaign campaign) {
        JPanel speakerBox = new JPanel();
        speakerBox.setLayout(new BoxLayout(speakerBox, BoxLayout.Y_AXIS));
        speakerBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        speakerBox.setMaximumSize(new Dimension(IMAGE_WIDTH, Integer.MAX_VALUE));

        AtBContract chosenContract = null;
        for (AtBContract contract : campaign.getAtBContracts()) {
            LocalDate startDate = contract.getStartDate();
            if (!startDate.isBefore(campaign.getLocalDate())) {
                chosenContract = contract;
                break;
            }
        }

        // Get speaker details
        String speakerName = speaker.getFullTitle();
        if (campaign.getLocalDate().equals(EVENT_DATE_GRAY_MONDAY.plusDays(3))) {
            if (chosenContract != null) {
                speakerName = chosenContract.getEmployerName(campaign.getGameYear());
            }
        }

        // Add speaker image (icon)
        ImageIcon speakerIcon = getSpeakerIcon(campaign, speaker);

        if (campaign.getLocalDate().equals(EVENT_DATE_GRAY_MONDAY.plusDays(3))) {
            if (chosenContract != null) {
                String employerCode = chosenContract.getEmployerCode();
                speakerIcon = Factions.getFactionLogo(campaign, employerCode, true);
            }
        }

        if (speakerIcon != null) {
            speakerIcon = scaleImageIconToWidth(speakerIcon, IMAGE_WIDTH);
        }
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(speakerIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        StringBuilder speakerDescription = getSpeakerDescription(campaign, speaker, speakerName);
        JLabel leftDescription = new JLabel(
            String.format("<html><div style='width:%dpx; text-align:center;'>%s</div></html>",
                IMAGE_WIDTH, speakerDescription));
        leftDescription.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add the image and description to the speakerBox
        speakerBox.add(imageLabel);
        speakerBox.add(leftDescription);

        return speakerBox;
    }
}
