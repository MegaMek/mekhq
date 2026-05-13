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
package mekhq.gui.companyGeneration;

import static mekhq.gui.companyGeneration.components.CompanyGenerationUtilities.getCompanyGenerationResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.companyGeneration.contents.ForceGeneratorTab;
import mekhq.gui.companyGeneration.contents.OtherTab;
import mekhq.gui.companyGeneration.contents.SetupTab;
import mekhq.gui.companyGeneration.contents.SparesTab;

/**
 * Top-level {@link AbstractMHQTabbedPane} for the Company Generation dialog.
 *
 * <p>Hosts four tabs in workflow order:</p>
 * <ol>
 *   <li><b>Setup</b> — force-shape, support-personnel percentages, officer / naming / rank flags,
 *       random origin</li>
 *   <li><b>Force Generator</b> — the embedded MegaMek Force Generator panel; the actual generation
 *       inputs (faction / echelon / unit type / weight / rating / experience / transport %)</li>
 *   <li><b>Spares</b> — twelve percentages bound to {@code CampaignOptions.getAutoLogistics*()}</li>
 *   <li><b>Other</b> — finances, contracts, starting simulation, surprises, unit extras</li>
 * </ol>
 *
 * <p>This Pane is the spiritual counterpart of {@code CampaignOptionsPane}, scaled down to the four
 * tabs the Company Generation dialog needs. The Tab classes themselves live in
 * {@link mekhq.gui.companyGeneration.contents} and are plain Java classes following the same
 * "constructor + {@code createTab()} + {@code loadValuesFromOptions()}" convention used in the
 * Campaign Options package.</p>
 *
 * <p>Construction is currently scaffolding: the four tabs render placeholder content. Tab content
 * migrates over from {@code CompanyGenerationOptionsPanel} in subsequent commits per the Force
 * Generator integration plan.</p>
 */
public class CompanyGenerationPane extends AbstractMHQTabbedPane {

    private final Campaign campaign;
    private final CompanyGenerationOptions options;

    private SetupTab setupTab;
    private ForceGeneratorTab forceGeneratorTab;
    private SparesTab sparesTab;
    private OtherTab otherTab;

    /**
     * @param frame    the parent {@link JFrame} for this pane
     * @param campaign the {@link Campaign} the dialog is generating into
     * @param options  the {@link CompanyGenerationOptions} preset to round-trip through the tabs; may be
     *                 {@code null}, in which case the tabs supply their own defaults
     */
    public CompanyGenerationPane(final JFrame frame, final Campaign campaign,
          final CompanyGenerationOptions options) {
        super(frame, ResourceBundle.getBundle(getCompanyGenerationResourceBundle()),
              "companyGenerationDialog");
        this.campaign = campaign;
        this.options = options;
        initialize();
    }

    /**
     * Builds and attaches the four tabs. Each tab content is wrapped in a {@link JScrollPane} so dense
     * sub-sections (Setup in particular) can scroll independently of the dialog window size.
     */
    @Override
    protected void initialize() {
        setupTab = new SetupTab(campaign, options);
        forceGeneratorTab = new ForceGeneratorTab(campaign, options);
        sparesTab = new SparesTab(campaign, options);
        otherTab = new OtherTab(campaign, options);

        addTab(tabTitle("setupTab"), wrap(setupTab.createTab()));
        addTab(tabTitle("forceGeneratorTab"), wrap(forceGeneratorTab.createTab()));
        addTab(tabTitle("sparesTab"), wrap(sparesTab.createTab()));
        addTab(tabTitle("otherTab"), wrap(otherTab.createTab()));
    }

    public SetupTab getSetupTab() {
        return setupTab;
    }

    public ForceGeneratorTab getForceGeneratorTab() {
        return forceGeneratorTab;
    }

    public SparesTab getSparesTab() {
        return sparesTab;
    }

    public OtherTab getOtherTab() {
        return otherTab;
    }

    private static String tabTitle(String resourceKey) {
        // Matches the {namespace}.title convention used in CampaignOptionsDialog.properties (no "lbl" prefix).
        return getTextAt(getCompanyGenerationResourceBundle(), resourceKey + ".title");
    }

    private static JScrollPane wrap(JPanel content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        return scroll;
    }
}
