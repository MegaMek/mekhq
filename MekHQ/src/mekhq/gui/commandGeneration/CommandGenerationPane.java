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
package mekhq.gui.commandGeneration;

import static mekhq.gui.commandGeneration.components.CommandGenerationUtilities.getCommandGenerationResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ManeiDominiCrewAugmentor;
import megamek.common.annotations.Nullable;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.commandGeneration.contents.ForceGeneratorTab;
import mekhq.gui.commandGeneration.contents.SetupTab;
import mekhq.gui.commandGeneration.contents.SparesAndFinancesTab;

/**
 * Top-level {@link AbstractMHQTabbedPane} for the Company Generation dialog.
 *
 * <p>Hosts four tabs in workflow order:</p>
 * <ol>
 *   <li><b>Setup</b> — force-shape, support-personnel percentages, officer / naming / rank flags,
 *       random origin</li>
 *   <li><b>Force Generator</b> — the embedded MegaMek Force Generator panel; the actual generation
 *       inputs (faction / echelon / unit type / weight / rating / experience / transport %)</li>
 *   <li><b>Spares &amp; Finances</b> — the AutoLogistics restock percentages (bound to
 *       {@code CampaignOptions.getAutoLogistics*()}) alongside contracts, finances, and the
 *       starting simulation</li>
 * </ol>
 *
 * <p>This Pane is the spiritual counterpart of {@code CampaignOptionsPane}, scaled down to the three
 * tabs the Command Generator needs. The Tab classes themselves live in
 * {@link mekhq.gui.commandGeneration.contents} and are plain Java classes following the same
 * "constructor + {@code createTab()} + {@code loadValuesFromOptions()}" convention used in the
 * Campaign Options package.</p>
 */
public class CommandGenerationPane extends AbstractMHQTabbedPane {
    private static final MMLogger LOGGER = MMLogger.create(CommandGenerationPane.class);

    /**
     * The non-Clan factions that name their formations with the Greek alphabet. Sub-commands of these
     * are matched too, so the Word of Blake Shadow Divisions follow the Word of Blake.
     */
    private static final List<String> GREEK_NAMED_FACTION_KEYS = List.of("CS", "WOB");

    /**
     * The faction the Force Generator panel last reported, kept so the settings that follow from it can
     * be re-applied after the tabs are populated from saved options.
     */
    private FactionRecord lastFactionRecord;


    private final Campaign campaign;
    private final CommandGenerationOptions options;

    private SetupTab setupTab;
    private ForceGeneratorTab forceGeneratorTab;
    private SparesAndFinancesTab sparesAndFinancesTab;

    /**
     * @param frame    the parent {@link JFrame} for this pane
     * @param campaign the {@link Campaign} the dialog is generating into
     * @param options  the {@link CommandGenerationOptions} preset to round-trip through the tabs; may be
     *                 {@code null}, in which case the tabs supply their own defaults
     */
    public CommandGenerationPane(final JFrame frame, final Campaign campaign,
          final CommandGenerationOptions options) {
        super(frame, ResourceBundle.getBundle(getCommandGenerationResourceBundle()),
              "commandGenerationDialog");
        this.campaign = campaign;
        this.options = options;
        initialize();
    }

    /**
     * Builds and attaches the three tabs. Each tab content is wrapped in a {@link JScrollPane} so dense
     * sub-sections (Setup in particular) can scroll independently of the dialog window size.
     */
    @Override
    protected void initialize() {
        setupTab = new SetupTab(campaign, options);
        forceGeneratorTab = new ForceGeneratorTab(getFrame(), campaign, options);
        sparesAndFinancesTab = new SparesAndFinancesTab(campaign, options,
              forceGeneratorTab::getGeneratedForce);

        addTab(tabTitle("setupTab"), wrap(setupTab.createTab()));
        // Spares & Finances before the Force Generator: the generator is where the player ends up, building
        // and previewing the command, so the settings tabs come first and it sits last.
        addTab(tabTitle("sparesAndFinancesTab"), wrap(sparesAndFinancesTab.createTab()));
        addTab(tabTitle("forceGeneratorTab"), wrap(forceGeneratorTab.createTab()));

        // The preview tree shows the final TOE callsigns, which depend on the Setup tab's Formation
        // Naming Method: read the combo live (options only get the value on OK) and refresh the
        // preview names whenever it changes.
        forceGeneratorTab.setNamingMethodSupplier(setupTab::getSelectedForceNamingMethod);
        forceGeneratorTab.setNumberRegimentsSupplier(setupTab::isAlwaysNumberRegimentsSelected);
        setupTab.setNamingMethodChangeListener(forceGeneratorTab::invalidatePreviewNames);

        // The Clans name their formations with the Greek alphabet, so picking a Clan faction sets the
        // naming method to match. Only Clans are switched: a non-Clan selection leaves whatever the
        // user chose alone rather than overwriting a deliberate choice.
        forceGeneratorTab.setFactionChangeListener(factionRecord -> {
            lastFactionRecord = factionRecord;
            applyFactionDrivenDefaults();
        });

        // The starting-cash preview prices the Force Generator tab's current model, which can change
        // while this tab is hidden - recompute it whenever the user switches onto it.
        addChangeListener(event -> {
            if (getSelectedIndex() == indexOfTab(tabTitle("sparesAndFinancesTab"))) {
                sparesAndFinancesTab.refreshStartingCashPreview();
            }
        });
    }

    /**
     * Applies the settings that follow from the selected faction - currently only which alphabet names
     * its formations.
     *
     * <p>Called again once every tab has been populated from its options. Loading those options sets
     * the naming method directly, so applying this only when the faction is first reported would leave
     * it overwritten a moment later.</p>
     */
    public void applyFactionDrivenDefaults() {
        FactionRecord factionRecord = lastFactionRecord;
        setupTab.setAugmentationSectionVisible(offersAugmentation(factionRecord));
        if (factionRecord == null) {
            LOGGER.debug("[NamingMethod] no faction reported yet; leaving naming alone");
            return;
        }
        boolean namesFormationsInGreek = usesGreekFormationNames(factionRecord);
        // At INFO because it fires once per faction selection, and because a report that the naming
        // method did not switch cannot be placed without it.
        LOGGER.info("[NamingMethod] faction now {} (isClan={}, ComStar/Word of Blake={}) -> {}",
              factionRecord.getKey(), factionRecord.isClan(),
              isComStarOrWordOfBlake(factionRecord),
              namesFormationsInGreek ? "defaulting to the Greek alphabet" : "leaving naming alone");
        if (namesFormationsInGreek) {
            setupTab.setSelectedForceNamingMethod(ForceNamingMethod.GREEK_ALPHABET);
        }
    }

    /**
     * The sub-faction key for Warrior House Thuggee, the Capellan command whose warriors are augmented.
     *
     * <p>Kali Liao raised these Houses in secret from her Thuggee cult and the Manei Domini, so they answer to
     * her rather than to the Warrior House Orders. The regular Warrior Houses ({@code CC.WHO}) are not
     * augmented and are deliberately not included here.</p>
     */
    static final String WARRIOR_HOUSE_THUGGEE_FACTION_KEY = "CC.THG";

    /**
     * Whether the selected faction is one the player would want the augmentation controls for, which decides
     * whether the Setup tab shows them at all.
     *
     * <p>Three cases, for two different reasons:</p>
     * <ul>
     *   <li><b>The Word of Blake Shadow Divisions</b>, keyed
     *       {@value ManeiDominiCrewAugmentor#SHADOW_DIVISION_FACTION_KEY} - the only faction the Manei Domini
     *       stage fits implants to.</li>
     *   <li><b>Any Clan</b> - enhanced imaging is the Clans' alone, and its stage reads the same Use Implants
     *       setting. Gating on the Shadow Divisions by themselves would leave a Clan player unable to switch
     *       implants on from here, and enhanced imaging would then never be fitted.</li>
     *   <li><b>Warrior House Thuggee</b>, keyed {@value #WARRIOR_HOUSE_THUGGEE_FACTION_KEY} - raised from the
     *       Thuggee cult and the Manei Domini, so its warriors carry implants. No generation stage fits them
     *       today, but the setting lets the campaign track implants for a command whose warriors have
     *       them.</li>
     * </ul>
     *
     * <p>For anyone else the two settings change nothing, so they are not offered.</p>
     *
     * @param factionRecord the selected faction, or {@code null} when none has been reported yet
     *
     * @return {@code true} if the augmentation controls should be shown for this faction
     */
    static boolean offersAugmentation(@Nullable FactionRecord factionRecord) {
        if (factionRecord == null) {
            return false;
        }
        if (factionRecord.isClan()) {
            return true;
        }
        String key = factionRecord.getKey();
        return ManeiDominiCrewAugmentor.SHADOW_DIVISION_FACTION_KEY.equalsIgnoreCase(key)
                     || WARRIOR_HOUSE_THUGGEE_FACTION_KEY.equalsIgnoreCase(key);
    }

    /**
     * Whether this faction names its formations with the Greek alphabet.
     *
     * <p>The Clans do it for their galaxies. ComStar and the Word of Blake do it throughout: their
     * Level IVs and Level IIIs carry names the ruleset fixes itself ("IV-alpha", "III-beta"), and the
     * Level II beneath them is the one echelon that follows the player's choice - so anything but
     * Greek there puts an "Able" or a "Bravo" inside a Greek-named Level III.</p>
     *
     * <p>Only a switch to such a faction changes the setting. A faction that does not use Greek leaves
     * whatever the player chose alone rather than overwriting a deliberate choice.</p>
     */
    static boolean usesGreekFormationNames(FactionRecord factionRecord) {
        return factionRecord.isClan() || isComStarOrWordOfBlake(factionRecord);
    }

    /**
     * @return {@code true} for ComStar, the Word of Blake, and any sub-command of either - the key of
     *       a sub-faction is its parent's followed by a dot, as in {@code WOB.SD}
     */
    private static boolean isComStarOrWordOfBlake(FactionRecord factionRecord) {
        String key = factionRecord.getKey();
        if (key == null) {
            return false;
        }
        for (String greekNamedFaction : GREEK_NAMED_FACTION_KEYS) {
            if (key.equals(greekNamedFaction) || key.startsWith(greekNamedFaction + ".")) {
                return true;
            }
        }
        return false;
    }

    public SetupTab getSetupTab() {
        return setupTab;
    }

    public ForceGeneratorTab getForceGeneratorTab() {
        return forceGeneratorTab;
    }

    public SparesAndFinancesTab getSparesAndFinancesTab() {
        return sparesAndFinancesTab;
    }

    /** Brings the Force Generator tab to the front, for a caller that needs the player back at the preview. */
    public void showForceGeneratorTab() {
        setSelectedIndex(indexOfTab(tabTitle("forceGeneratorTab")));
    }

    private static String tabTitle(String resourceKey) {
        // Matches the {namespace}.title convention used in CampaignOptionsDialog.properties (no "lbl" prefix).
        return getTextAt(getCommandGenerationResourceBundle(), resourceKey + ".title");
    }

    private static JScrollPane wrap(JPanel content) {
        // FastJScrollPane rather than a plain JScrollPane so the scroll step follows the UI scale,
        // as it does everywhere else in the suite.
        JScrollPane scroll = new FastJScrollPane(content);
        scroll.setBorder(null);
        return scroll;
    }
}
