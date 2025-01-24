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
package mekhq.gui.dialog.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;

public class PrisonerRansomEventDialog extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.PrisonerRansomEvent";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    public PrisonerRansomEventDialog(Campaign campaign, List<Person> prisoners, Money payment, boolean isFriendlyPOWs) {
        super(campaign, getSpeaker(campaign), null, createInCharacterMessage(campaign,
                payment, prisoners, isFriendlyPOWs), createButtons(), createOutOfCharacterMessage(isFriendlyPOWs),
            null, null, null);

        setModal(false);
        setAlwaysOnTop(true);
    }

    /**
     * Handles the hyperlink click event in the dialog.
     *
     * <p>This method parses the hyperlink reference to focus on the personnel record identified by
     * the provided UUID in the campaign's graphical user interface.</p>
     *
     * @param campaign the {@link Campaign} containing relevant personnel data
     * @param hyperlinkReference     the hyperlink reference containing the UUID of the selected character
     */
    @Override
    protected void handleHyperlinkClick(Campaign campaign, String hyperlinkReference) {
        CampaignGUI campaignGUI = campaign.getApp().getCampaigngui();

        final UUID id = UUID.fromString(hyperlinkReference.split(":")[1]);
        campaignGUI.focusOnPerson(id);
    }

    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnDecline = new ButtonLabelTooltipPair(
            resources.getString("decline.button"), null);
        ButtonLabelTooltipPair btnAccept = new ButtonLabelTooltipPair(
            resources.getString("accept.button"), null);

        return List.of(btnDecline, btnAccept);
    }

    private static @Nullable Person getSpeaker(Campaign campaign) {
        return campaign.getSeniorAdminPerson(COMMAND);
    }

    private static String createInCharacterMessage(Campaign campaign, Money payment,
                                                   List<Person> prisoners, boolean isFriendlyPOWs) {
        String commanderAddress = campaign.getCommanderAddress(false);
        StringBuilder message = new StringBuilder();
        String key = isFriendlyPOWs ? "pows" : "prisoners";
        message.append(String.format(resources.getString(key + ".message"), commanderAddress, payment));

        // Create a table to hold the personnel
        message.append("<br><table style='width:100%; text-align:left;'>");

        for (int i = 0; i < prisoners.size(); i++) {
            if (i % 2 == 0) {
                message.append("<tr>");
            }

            // Add the person in a column
            Person person = prisoners.get(i);
            message.append("<td>- ").append(person.getHyperlinkedFullTitle()).append("</td>");

            if ((i + 1) % 2 == 0 || i == prisoners.size() - 1) {
                message.append("</tr>");
            }
        }

        message.append("</table>");
        return message.toString();
    }

    private static String createOutOfCharacterMessage(boolean isFriendlyPOWs) {
        String key = isFriendlyPOWs ? "pows" : "prisoners";
        return resources.getString(key + ".ooc");
    }
}
