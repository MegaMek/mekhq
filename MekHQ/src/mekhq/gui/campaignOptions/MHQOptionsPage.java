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
package mekhq.gui.campaignOptions;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.sendTipToDetailsPanel;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComponent;

import jakarta.annotation.Nullable;
import megamek.client.ui.buttons.ColourSelectorButton;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * Base class for the individual MekHQ Client Options pages (Display, Colours, Fonts, Save, New Day, Reminders,
 * Advanced). Each concrete page owns its own controls, builds its UI in {@link #createPage()}, and copies its controls
 * back into the shared {@link MHQOptionsModel} in {@link #writeToModel()} - mirroring the self-contained page pattern
 * used by the Campaign Options pages (for example {@code AttributesAndTraitsPage}).
 *
 * <p>The shared page-building helpers here are {@code static} so the coordinating {@link MHQOptionsPane} and every
 * page can use them the same way. They resolve their text from the {@code GUI.properties} bundle and render each page
 * with a per-page faction emblem header, matching the Campaign Options pages. This class lives in the
 * {@code campaignOptions} package so it can use the framework's package-private classes; it will move with
 * {@link MHQOptionsPane} once the framework is extracted.</p>
 */
abstract class MHQOptionsPage {
    static final String RESOURCE_BUNDLE = "mekhq.resources.GUI";
    static final int FORM_LABEL_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    static final int FORM_CONTROL_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

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
     * @return the page component (a {@link CampaignOptionsPagePanel})
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
    static CampaignOptionsPagePanel.Builder pageBuilder(String pageName, boolean showDetailsPanel) {
        return CampaignOptionsPagePanel.builder(pageName, pageName, getImageDirectory() + factionLogo(pageName))
                     .resourceBundle(RESOURCE_BUNDLE)
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
        CampaignOptionsPagePanel.Builder builder = pageBuilder(pageName, registerDetailsTips(content))
                                                         .sectionsExpandedByDefault(true);
        if (introKey != null) {
            builder.intro(introKey);
        }
        return builder.section(sectionTitleKey, sectionSummaryKey, content).build();
    }

    /**
     * Creates a {@link CampaignOptionsCheckBox} whose text/tooltip come from {@code resourceName} in the GUI bundle and
     * sets its initial state to {@code selected}. The value is read back into the model by the owning page's
     * {@code writeToModel} method.
     */
    static CampaignOptionsCheckBox checkBox(String resourceName, boolean selected) {
        return checkBox(resourceName, selected, null);
    }

    /**
     * Creates a {@link CampaignOptionsCheckBox} as {@link #checkBox(String, boolean)} does, but with badge metadata
     * (such as the "important information" flag) shown after the text.
     */
    static CampaignOptionsCheckBox checkBox(String resourceName, boolean selected,
          @Nullable CampaignOptionsMetadata metadata) {
        CampaignOptionsCheckBox checkBox = new CampaignOptionsCheckBox(RESOURCE_BUNDLE, resourceName, metadata);
        checkBox.setSelected(selected);
        return checkBox;
    }

    /**
     * Creates a {@link ColourSelectorButton} whose text comes from {@code key} in the GUI bundle and whose initial
     * colour is {@code colour}. The chosen colour is read back into the model by the owning page's
     * {@code writeToModel} method.
     */
    static ColourSelectorButton colourButton(String key, Color colour) {
        ColourSelectorButton button = new ColourSelectorButton(getTextAt(RESOURCE_BUNDLE, key + ".text"));
        button.setName("btn" + key);
        button.setColour(colour);
        return button;
    }

    /**
     * Recursively wires every tip-bearing control under {@code component} to the shared "Option Details" help box: on
     * mouse-over the control sends its tooltip text there, and its floating Swing tooltip is removed so the help shows
     * only in the box, mirroring the Campaign Options behaviour. Buttons are skipped so their action tooltips (such as
     * the user-directory chooser and help buttons) keep working as ordinary tooltips.
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
                String detailsText = detailsTextFor(jComponent, tooltip);
                jComponent.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent event) {
                        sendTipToDetailsPanel(detailsText);
                    }
                });
                jComponent.setToolTipText(null);
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
     * Resolves the text a control should show in the "Option Details" box. The shared checkbox, spinner, and label
     * components word-wrap their Swing tooltip at a fixed character count; for those the raw bundle text is re-resolved
     * from the control's name so the box can soft-wrap it to its own width (and so intentional line breaks are kept),
     * matching Campaign Options. Other controls (combo boxes, text fields) already carry raw tooltip text, so their
     * tooltip is used as-is.
     *
     * @param component the tip-bearing control
     * @param tooltip   the control's current Swing tooltip, used as-is when the raw text cannot be re-resolved
     *
     * @return the help text to show in the details box
     */
    static String detailsTextFor(JComponent component, String tooltip) {
        boolean wordWrapsTooltip = component instanceof CampaignOptionsCheckBox
              || component instanceof CampaignOptionsSpinner
              || component instanceof CampaignOptionsLabel;
        if (wordWrapsTooltip) {
            // These components set their Swing name to a 3-character prefix ("chk"/"spn"/"lbl") plus the resource base
            // name, so stripping the prefix recovers the base used for the ".tooltip"/".toolTipText" keys.
            String name = component.getName();
            if (name != null && name.length() > 3) {
                String base = name.substring(3);
                String raw = getTextAt(RESOURCE_BUNDLE, base + ".tooltip");
                if (!isResourceKeyValid(raw)) {
                    raw = getTextAt(RESOURCE_BUNDLE, base + ".toolTipText");
                }
                if (isResourceKeyValid(raw)) {
                    return raw;
                }
            }
        }
        return tooltip;
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
}
