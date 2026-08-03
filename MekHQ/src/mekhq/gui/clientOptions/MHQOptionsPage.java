/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.clientOptions;

import static java.awt.Color.BLACK;
import static megamek.utilities.ImageUtilities.addTintToImageIcon;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.settingsBadges;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;

import jakarta.annotation.Nullable;
import megamek.client.ui.buttons.ColourSelectorButton;
import megamek.client.ui.settings.SettingsCheckBox;
import megamek.client.ui.settings.SettingsFormPanel;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.settings.SettingsTextProvider;
import mekhq.gui.campaignOptions.CampaignOptionsMetadata;
import mekhq.utilities.MHQInternationalization;

/**
 * Base class for the individual MekHQ Client Options pages (Display, Colours, Fonts, Save, New Day, Reminders,
 * Advanced). Each concrete page owns its own controls, builds its UI in {@link #createPage()}, and copies its controls
 * back into the shared {@link MHQOptionsModel} in {@link #writeToModel()} - mirroring the self-contained page pattern
 * used by the Campaign Options pages (for example {@code AttributesAndTraitsPage}).
 *
 * <p>The shared page-building helpers here are {@code static} so the coordinating {@link MHQOptionsPane} and every
 * page can use them the same way. They adapt MekHQ's {@code GUI.properties} bundle and option metadata to the shared
 * MegaMek settings framework and render each page with a fixed faction emblem header.</p>
 */
abstract class MHQOptionsPage {
    static final SettingsTextProvider TEXT_PROVIDER = new SettingsTextProvider() {
        @Override
        public boolean containsKey(String key) {
            return isResourceKeyValid(MHQInternationalization.getText(key));
        }

        @Override
        public String getText(String key) {
            return MHQInternationalization.getText(key);
        }

        @Override
        public String getFormattedText(String key, Object... arguments) {
            return arguments.length == 0
                  ? getText(key)
                  : MHQInternationalization.getFormattedText(key, arguments);
        }
    };
    static final int FORM_LABEL_WIDTH = SettingsFormPanel.DEFAULT_LABEL_WIDTH;
    static final int FORM_CONTROL_WIDTH = SettingsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int HEADER_IMAGE_SIZE = 80;
    private static final Map<String, Icon> PAGE_HEADER_ICONS = new HashMap<>();

    /**
     * Faction emblem shown in each page's header, hardcoded per page (chosen arbitrarily) so a page always shows the
     * same logo - mirroring how the Campaign Options pages each use a fixed faction logo instead of the MekHQ logo.
     */
    private static final Map<String, String> PAGE_FACTION_LOGOS = Map.ofEntries(
          Map.entry("MHQDisplayPage", "logo_federated_suns.png"),
          Map.entry("MHQColoursPage", "logo_taurian_concordat.png"),
          Map.entry("MHQFontsPage", "logo_rasalhague_dominion.png"),
          Map.entry("MHQSaveOptionsPage", "logo_clan_ghost_bear.png"),
          Map.entry("MHQNewDayPage", "logo_outworld_alliance.png"),
          Map.entry("MHQRemindersPage", "logo_rim_worlds_republic.png"),
          Map.entry("MHQAdvancedPage", "logo_republic_of_the_sphere.png"));

    protected final MHQOptionsModel model;

    /**
     * Set once {@link #createPage()} has run. Until then the page's controls are null, so {@link #writeToModel()} is a
     * no-op and the model keeps the values it was constructed with (a page the user never opened is not written back).
     */
    protected boolean created;

    protected MHQOptionsPage(MHQOptionsModel model) {
        this.model = model;
    }

    /**
     * Builds this page's UI. Implementations must set {@link #created} to {@code true} before returning so the page's
     * controls are subsequently written back on save.
     *
    * @return the page component (a {@link SettingsPagePanel})
     */
    abstract Component createPage();

    /** Copies this page's controls into the shared {@link #model}. A no-op until {@link #createPage()} has run. */
    abstract void writeToModel();

    /**
     * Creates the shared page builder used by every MekHQ option page: the per-page faction emblem header (matching
     * Campaign Options), the GUI resource bundle, whether to show the "Option Details" help box, and sections collapsed
     * by default. Multi-section pages keep that collapsed default; the single-section {@link #buildMHQPage} wrapper
     * re-expands its lone section. Callers add their section(s) and call {@code build()}.
     */
    static SettingsPagePanel.Builder pageBuilder(String pageName, boolean showDetailsPanel) {
        return SettingsPagePanel.builder(pageName, TEXT_PROVIDER, "lbl" + pageName + ".text", pageHeaderIcon(pageName))
                     .showDetailsPanel(showDetailsPanel)
                     .sectionsExpandedByDefault(false);
    }

    /**
     * Builds a standard single-section MekHQ option page: a per-page faction emblem header (matching the Campaign
     * Options pages), a shared "Option Details" help box for its tip-bearing controls, and one collapsible section
     * wrapping {@code content}. The lone section starts expanded, since collapsing the only section on a page would
     * hide everything for no benefit.
     */
    static Component buildMHQPage(String pageName, String sectionTitleKey, String sectionSummaryKey,
          JComponent content) {
        return buildMHQPage(pageName, null, sectionTitleKey, sectionSummaryKey, content);
    }

    /**
     * Builds a single-section MekHQ option page as {@link #buildMHQPage(String, String, String, JComponent)} does, but
     * with an intro paragraph (resolved from {@code introKey}) shown above the section - used for pages that need a
     * note at the top, such as the colours disclaimer.
     */
    static Component buildMHQPage(String pageName, @Nullable String introKey, String sectionTitleKey,
          String sectionSummaryKey, JComponent content) {
        // Route each control's tooltip to the shared "Option Details" box (like Campaign Options) and drop the floating
        // tooltip. Only pages that actually have tip-bearing controls get the box, so tooltip-free pages (the colour
        // grids) are not saddled with an empty details area.
        SettingsPagePanel.Builder builder = pageBuilder(pageName, registerDetailsTips(content))
                                                  .sectionsExpandedByDefault(true);
        if (introKey != null) {
            builder.intro(introKey + ".intro");
        }
        return builder.section(sectionTitleKey, sectionSummaryKey, content).build();
    }

    /**
    * Creates a {@link SettingsCheckBox} whose text/tooltip come from {@code resourceName} in the GUI bundle and
     * sets its initial state to {@code selected}. The value is read back into the model by the owning page's
     * {@code writeToModel} method.
     */
    static SettingsCheckBox checkBox(String resourceName, boolean selected) {
        return checkBox(resourceName, selected, null);
    }

    /**
    * Creates a {@link SettingsCheckBox} as {@link #checkBox(String, boolean)} does, but with badge metadata
     * (such as the "important information" flag) shown after the text.
     */
    static SettingsCheckBox checkBox(String resourceName, boolean selected,
          @Nullable CampaignOptionsMetadata metadata) {
        SettingsCheckBox checkBox = new SettingsCheckBox(TEXT_PROVIDER, resourceName, settingsBadges(metadata));
        checkBox.setSelected(selected);
        return checkBox;
    }

    /**
     * Creates a {@link ColourSelectorButton} whose text comes from {@code key} in the GUI bundle and whose initial
     * colour is {@code colour}. The chosen colour is read back into the model by the owning page's
     * {@code writeToModel} method.
     */
    static ColourSelectorButton colourButton(String key, Color colour) {
        ColourSelectorButton button = new ColourSelectorButton(MHQInternationalization.getText(key + ".text"));
        button.setName("btn" + key);
        button.setColour(colour);
        return button;
    }

    /**
    * Recursively detects tip-bearing controls under {@code component}. The shared content host performs the actual
    * instance-owned help routing when the page is mounted. Buttons are skipped so action tooltips remain floating.
     *
     * @param component the subtree to process
     *
     * @return {@code true} if at least one control was wired, so the caller shows the details box only when the page
     *       actually has tips
     */
    static boolean registerDetailsTips(Component component) {
        boolean anyTip = false;
        if (component instanceof JComponent jComponent && !(component instanceof JButton)) {
            String tooltip = jComponent.getToolTipText();
            if (tooltip != null && !tooltip.isBlank()) {
                anyTip = true;
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                anyTip |= registerDetailsTips(child);
            }
        }
        return anyTip;
    }

    /**
     * Sizes every component in {@code components} to the widest one's preferred width (heights unchanged) so they line
     * up in even columns when laid out in a grid - used across a page's colour buttons at once so their columns match.
     */
    static void setUniformWidth(List<? extends JComponent> components) {
        int width = 0;
        for (JComponent component : components) {
            width = Math.max(width, component.getPreferredSize().width);
        }
        for (JComponent component : components) {
            component.setPreferredSize(new Dimension(width, component.getPreferredSize().height));
        }
    }

    /**
     * Returns the faction emblem file name hardcoded for {@code pageName} (see {@link #PAGE_FACTION_LOGOS}), falling
     * back to a default so a page without an explicit mapping still shows a faction logo rather than failing.
     */
    private static String factionLogo(String pageName) {
        return PAGE_FACTION_LOGOS.getOrDefault(pageName, "logo_star_league.png");
    }

    private static Icon pageHeaderIcon(String pageName) {
        return PAGE_HEADER_ICONS.computeIfAbsent(pageName, ignored -> {
            String imageAddress = getImageDirectory() + factionLogo(pageName);
            ImageIcon icon = scaleImageIcon(new ImageIcon(imageAddress), HEADER_IMAGE_SIZE, true);
            return addTintToImageIcon(icon.getImage(), BLACK);
        });
    }
}
