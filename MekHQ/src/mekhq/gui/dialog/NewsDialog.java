/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

/**
 * NewsDialog is a dialog window for displaying news items within the context of a campaign. It includes information
 * about a network, its image, headline, and other relevant details.
 * <p>
 * This dialog is a part of MekHQ and displays immersive content in the game GUI.
 * </p>
 */
public class NewsDialog extends ImmersiveDialogSimple {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.NewsDialog";

    private static final int OPERATION_KLONDIKE = 2822;

    private static final List<NewsNetwork> NEWS_NETWORKS = getNewsNetworks();
    private static final String CHATTERWEB_NETWORK_NAME = "chatterweb";
    private static final String AFFILIATE_NETWORK_NAME = "affiliateNewsNetworks";

    /**
     * Constructs a {@code NewsDialog} that displays a central message to the player within the campaign context.
     *
     * <p>
     * The dialog uses a placeholder speaker to ensure that the immersive dialog interface displays the speaker panel,
     * even though no specific person is associated with the message. Only the provided central message is shown.
     * </p>
     *
     * @param campaign      the current {@link Campaign} context
     * @param centerMessage the main message to display in the dialog
     */
    public NewsDialog(Campaign campaign, String centerMessage) {
        super(campaign, new Person(campaign), // empty person to trick immersive dialog into showing the speaker panel
              null, centerMessage,
              List.of(getTextAt(RESOURCE_BUNDLE, "newsReport.button")),
              null,
              null, false, ImmersiveDialogWidth.MEDIUM);
    }

    @Override
    protected void setTitle() {
        setTitle(getFormattedTextAt(RESOURCE_BUNDLE, "incomingNews.title"));
    }

    /**
     * Builds the speaker panel that consists of the network's image and descriptive text.
     *
     * @param speaker  The {@link Person} representing the speaker (may be {@code null}).
     * @param campaign The {@link Campaign} object providing relevant game details.
     *
     * @return A {@link JPanel} containing the speaker's image and description.
     */
    @Override
    protected JPanel buildSpeakerPanel(@Nullable Person speaker, Campaign campaign) {
        JPanel speakerBox = new JPanel();
        speakerBox.setLayout(new BoxLayout(speakerBox, BoxLayout.Y_AXIS));
        speakerBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        speakerBox.setMaximumSize(new Dimension(IMAGE_WIDTH, Integer.MAX_VALUE));

        final NewsNetwork NETWORK = getNetwork(campaign);

        // Get Network image
        String networkImage = NETWORK.imageAddress;
        ImageIcon networkIcon = new ImageIcon(networkImage);
        networkIcon = scaleImageIcon(networkIcon, IMAGE_WIDTH, true);

        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(networkIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        StringBuilder speakerDescription = getNetworkDescription(campaign, NETWORK);
        JLabel leftDescription = new JLabel(String.format(
              "<html><div style='width:%dpx; text-align:center;'>%s</div></html>",
              IMAGE_WIDTH,
              speakerDescription));
        leftDescription.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add the image and description to the speakerBox
        speakerBox.add(imageLabel);
        speakerBox.add(leftDescription);

        return speakerBox;
    }

    /**
     * Generates a description string for the news network, including its code, name, and slogan.
     *
     * @param campaign The campaign data used to retrieve faction information.
     * @param network  The news network for which the description is generated.
     *
     * @return A {@link StringBuilder} containing the formatted network description.
     */
    private static StringBuilder getNetworkDescription(Campaign campaign, NewsNetwork network) {
        final String NETWORK_NAME = network.name;
        String networkCode = getFormattedTextAt(RESOURCE_BUNDLE, NETWORK_NAME + ".network");
        String networkName = "";
        if (NETWORK_NAME.equals(CHATTERWEB_NETWORK_NAME)) {
            networkName = campaign.getFaction().getFullName(campaign.getGameYear());
        } else if (!NETWORK_NAME.equals(AFFILIATE_NETWORK_NAME)) {
            networkName = getFormattedTextAt(RESOURCE_BUNDLE, NETWORK_NAME + ".name");
        }

        String networkSlogan = "";
        if (!NETWORK_NAME.equals(AFFILIATE_NETWORK_NAME)) {
            networkSlogan = getFormattedTextAt(RESOURCE_BUNDLE, NETWORK_NAME + ".slogan");
        }

        StringBuilder speakerDescription = new StringBuilder();

        speakerDescription.append("<b>").append(networkCode).append("</b>");

        if (!networkName.isEmpty()) {
            speakerDescription.append("<br>").append(networkName);
        }

        if (!networkSlogan.isEmpty()) {
            speakerDescription.append("<br>").append(networkSlogan);
        }

        return speakerDescription;
    }

    /**
     * Determines the most suitable news network for the campaign's current context.
     *
     * @param campaign The campaign context, including the current year and faction information.
     *
     * @return The appropriate {@link NewsNetwork} for the campaign.
     */
    private NewsNetwork getNetwork(Campaign campaign) {
        int currentYear = campaign.getGameYear();

        if (campaign.getFaction().isClan() && currentYear >= OPERATION_KLONDIKE) {
            // After Klondike Chatterweb comes along, and it makes sense for that to be used by the
            // Clans moving forward
            return NEWS_NETWORKS.get(NEWS_NETWORKS.size() - 2); // Chatterweb
        }

        for (NewsNetwork network : NEWS_NETWORKS) {
            int inception = network.inceptionYear;
            int closure = network.closureYear;

            if (currentYear >= inception && currentYear <= closure) {
                return network;
            }
        }

        return NEWS_NETWORKS.get(NEWS_NETWORKS.size() - 1); // Affiliated News Networks
    }

    /**
     * Initializes the list of all available news networks.
     *
     * @return A {@link List} of {@link NewsNetwork} objects, representing all predefined networks.
     */
    private static List<NewsNetwork> getNewsNetworks() {
        // TODO Replace placeholder images
        NewsNetwork terranNewsNetwork = new NewsNetwork("terranNewsNetwork",
              0,
              2314,
              "data/images/force/Pieces/Logos/Inner Sphere/Terran Hegemony.png");
        NewsNetwork hegemonyNewsNetwork = new NewsNetwork("hegemonyNewsNetwork",
              2315,
              2767,
              "data/images/force/Pieces/Logos/Inner Sphere/Terran Hegemony (Alternate, House Cameron).png");
        NewsNetwork starlightBroadcasting = new NewsNetwork("starlightBroadcasting",
              2570,
              2780,
              "data/images/force/Pieces/Logos/Inner Sphere/Star League.png");
        NewsNetwork comStarNewsBureau = new NewsNetwork("comStarNewsBureau",
              2826,
              3061,
              "data/images/universe/factions/logo_comstar.png");
        NewsNetwork interstellarNewsNetwork = new NewsNetwork("interstellarNewsNetwork",
              3062,
              3152,
              "data/images/universe/factions/logo_solaris_VII.png");

        // These two should always be last
        NewsNetwork chatterweb = new NewsNetwork(CHATTERWEB_NETWORK_NAME,
              0,
              0,
              "data/images/universe/factions/logo_clan_generic.png");
        NewsNetwork affiliateNewsNetworks = new NewsNetwork(AFFILIATE_NETWORK_NAME,
              0,
              0,
              "data/images/universe/factions/logo_mercenaries.png");

        return List.of(terranNewsNetwork,
              hegemonyNewsNetwork,
              starlightBroadcasting,
              comStarNewsBureau,
              interstellarNewsNetwork,
              chatterweb,
              affiliateNewsNetworks);
    }

    /**
     * Represents a news network with associated metadata such as its name, inception year, closure year, and the
     * address for its associated image.
     * <p>
     * This record is immutable and provides a compact way to store information about a news network.
     * </p>
     *
     * @param name          The name of the news network.
     * @param inceptionYear The year the news network was established or started broadcasting.
     * @param closureYear   The year the news network ceased operations.
     * @param imageAddress  The path or URL to an image representing the news network.
     */
    private record NewsNetwork(String name, int inceptionYear, int closureYear, String imageAddress) {
    }
}
