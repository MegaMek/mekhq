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

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;
import javax.swing.ImageIcon;

import megamek.utilities.ImageUtilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * {@code FullGlossaryDialog} displays a dialog window containing the full glossary to the user.
 *
 * <p>This class constructs and presents an {@link ImmersiveDialogSimple} instance tailored to display glossary
 * information in the context of the current campaign.</p>
 */
@Deprecated(since = "0.50.07", forRemoval = true)
public class FullGlossaryDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FullGlossaryDialog";

    /**
     * Constructs a new {@link ImmersiveDialogSimple} and shows the glossary dialog to the user.
     *
     * @param campaign the {@link Campaign} context for this glossary dialog
     */
    public FullGlossaryDialog(Campaign campaign) {
        new ImmersiveDialogSimple(campaign,
              null,
              null,
              getTextAt(RESOURCE_BUNDLE, "FullGlossaryDialog.message.inCharacter"),
              List.of(getTextAt(RESOURCE_BUNDLE, "FullGlossaryDialog.button.exit")),
              getTextAt(RESOURCE_BUNDLE, "FullGlossaryDialog.message.outOfCharacter"),
              getImage(),
              false);
    }

    /**
     * Retrieves the icon image to be used in the glossary dialog window.
     *
     * <p>Uses a default year, as this does not impact glossary display.</p>
     *
     * @return a scaled {@link ImageIcon} for display in the dialog
     */
    private static ImageIcon getImage() {
        // game year is largely irrelevant for the glossary dialog
        ImageIcon baseImage = Factions.getFactionLogo(3025, "CS");

        return ImageUtilities.scaleImageIcon(baseImage, 100, false);
    }
}
