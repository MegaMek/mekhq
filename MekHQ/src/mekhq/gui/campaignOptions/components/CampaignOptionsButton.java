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
package mekhq.gui.campaignOptions.components;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.common.annotations.Nullable;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.campaignOptions.CampaignOptionsMetadata;
import mekhq.gui.campaignOptions.CampaignOptionsUtilities;

/**
 * A specialized {@link RoundedJButton} used in the campaign options dialog.
 * <p>
 * This button's text and tooltip are dynamically loaded from a resource bundle based on a given name. The tooltip can
 * optionally include word wrapping with a configurable wrap size.
 * <p>
 * The button also supports font scaling adjustments.
 */
public class CampaignOptionsButton extends RoundedJButton {
    /**
     * Constructs a new instance of {@link CampaignOptionsButton} with the specified name.
     * <p>
     * The name is used to determine the button's visible text and tooltip, as well as to generate its unique internal
     * name.
     * <p>
     * The text is located in the resource bundle key {@code "lbl" + name + ".text"}. The tooltip is located in the
     * resource bundle key {@code "lbl" + name + ".tooltip"}.
     *
     * @param name the name used to fetch the button's text and tooltip and to set its name
     */
    public CampaignOptionsButton(String name) {
        this(name, null, null);
    }

    /**
     * Constructs a new instance of {@link CampaignOptionsButton} with the specified name and a custom tooltip wrap
     * size.
     * <p>
     * The name is used to determine the button's visible text and tooltip, as well as to generate its unique internal
     * name. The text and tooltip are fetched from the resource bundle, located at keys {@code "lbl" + name + ".text"}
     * and {@code "lbl" + name + ".tooltip"} respectively.
     * <p>
     * If a custom wrap size is provided, the tooltip text will be word-wrapped accordingly. If {@code customWrapSize}
     * is {@code null}, a default wrap size is used.
     *
     * @param name           the name used to fetch the button's text and tooltip and to set its name
     * @param customWrapSize the maximum number of characters per tooltip line, or {@code null} for the default wrap
     *                       size
     */
    public CampaignOptionsButton(String name, @Nullable Integer customWrapSize) {
        this(name, customWrapSize, null);
    }

    /**
     * Constructs a new instance of {@link CampaignOptionsButton} with the specified name and metadata.
     * <p>
     * The metadata is used to display version badges and special flag symbols alongside the button text.
     *
     * @param name     the name used to fetch the button's text and tooltip and to set its name
     * @param metadata version and flag metadata for displaying badges, or {@code null} for no badges
     */
    public CampaignOptionsButton(String name, @Nullable CampaignOptionsMetadata metadata) {
        this(name, null, metadata);
    }

    /**
     * Constructs a new instance of {@link CampaignOptionsButton} with the specified name, custom tooltip wrap size,
     * and metadata.
     * <p>
     * The name is used to determine the button's visible text and tooltip, as well as to generate its unique internal
     * name. The text and tooltip are fetched from the resource bundle, located at keys {@code "lbl" + name + ".text"}
     * and {@code "lbl" + name + ".tooltip"} respectively.
     * <p>
     * If a custom wrap size is provided, the tooltip text will be word-wrapped accordingly. If {@code customWrapSize}
     * is {@code null}, a default wrap size is used.
     * <p>
     * The metadata is used to display version badges and special flag symbols alongside the button text.
     *
     * @param name           the name used to fetch the button's text and tooltip and to set its name
     * @param customWrapSize the maximum number of characters per tooltip line, or {@code null} for the default wrap
     *                       size
     * @param metadata       version and flag metadata for displaying badges, or {@code null} for no badges
     */
    public CampaignOptionsButton(String name, @Nullable Integer customWrapSize,
                                 @Nullable CampaignOptionsMetadata metadata) {
        // Fetch base text and append badges & sets the button's text from the resource bundle
        super(getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".text") +
                    CampaignOptionsUtilities.formatBadges(metadata));

        // Sets the button's tooltip, applying word wrapping based on customWrapSize
        String tooltipText = getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".tooltip");
        if (!tooltipText.isEmpty()) {
            setToolTipText(wordWrap(tooltipText, processWrapSize(customWrapSize)));
        }

        // Sets the button's internal name
        setName("btn" + name);

        // Applies font scaling with default scaling disabled
        setFontScaling(this, false, 1);
    }
}
