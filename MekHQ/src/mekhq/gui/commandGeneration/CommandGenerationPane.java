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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.AMBER;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.GROUND;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.ManeiDominiCrewAugmentor;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;
import mekhq.gui.baseComponents.hud.HudModeSelector;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.commandGeneration.components.CommandGenerationUtilities;
import mekhq.gui.commandGeneration.contents.ForceGeneratorTab;
import mekhq.gui.commandGeneration.designer.DesignerPage;
import mekhq.gui.commandGeneration.designer.ForcePage;
import mekhq.gui.commandGeneration.designer.ForceSummary;
import mekhq.gui.commandGeneration.designer.LogisticsPage;
import mekhq.gui.commandGeneration.designer.PeoplePage;
import mekhq.gui.commandGeneration.designer.ReviewPage;

/**
 * The Command Designer: the Command Generation dialog's content, drawn as a heads-up display in the style of the
 * Oracle Console.
 *
 * <p>A command bar names the campaign and holds four live tiles - combat units, personnel, starting cash and lift -
 * that track what the build will produce as settings change. Below it a selector switches between four pages, each
 * with a guide that slides over it: {@link ForcePage Force}, {@link PeoplePage People}, {@link LogisticsPage
 * Logistics} and {@link ReviewPage Review}, from which the command is built.</p>
 */
public class CommandGenerationPane extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(CommandGenerationPane.class);
    private static final String BUNDLE = CommandGenerationUtilities.getCommandGenerationResourceBundle();
    // ComStar and the Word of Blake name their formations with the Greek alphabet, as the Clans do.
    private static final List<String> GREEK_NAMED_FACTION_KEYS = List.of("CS", "WOB");
    private static final int TRACKED_MONTHS = 12;

    private final Campaign campaign;
    private final ForceGeneratorTab engine;
    private final ForcePage forcePage;
    private final PeoplePage peoplePage;
    private final LogisticsPage logisticsPage;
    private final ReviewPage reviewPage;
    private final Map<DesignerPage, JComponent> pageComponents = new EnumMap<>(DesignerPage.class);
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageHolder = new JPanel(pageLayout);
    private final HudModeSelector<DesignerPage> selector;
    private DesignerPage currentPage = DesignerPage.FORCE;

    private final JLabel subtitle = new JLabel();
    private final HudStatTile unitsTile;
    private final HudStatTile personnelTile;
    private final HudStatTile cashTile;
    private final HudStatTile liftTile;
    private final HudButton guideButton = new HudButton("", false, true);
    private final HudGuideDrawer guideDrawer;
    private final CommandDesignerGuidePreferences guidePreferences = CommandDesignerGuidePreferences.getInstance();

    private final List<Runnable> refreshListeners = new ArrayList<>();
    private final List<Consumer<DesignerPage>> pageListeners = new ArrayList<>();
    private @Nullable FactionRecord lastFactionRecord;
    private boolean refreshPending;

    /**
     * @param frame    the main window, which the force generator's own dialogs are shown over
     * @param campaign the campaign the command is built into
     * @param options  the options the designer opens on
     */
    public CommandGenerationPane(final JFrame frame, final Campaign campaign,
          final CommandGenerationOptions options) {
        super(new BorderLayout());
        this.campaign = campaign;

        engine = new ForceGeneratorTab(frame, campaign, options);
        engine.initializeEngine();
        forcePage = new ForcePage(campaign, engine, this::scheduleRefresh);
        Supplier<ForceSummary> summary = forcePage::getSummary;
        peoplePage = new PeoplePage(campaign, summary, engine::previewName, this::scheduleRefresh);
        logisticsPage = new LogisticsPage(campaign, this::scheduleRefresh);
        reviewPage = new ReviewPage(summary, peoplePage, logisticsPage, forcePage::isLiftRolled, this::showPage);

        // The board shows the names the committed TO&E will use, which follow the People page's naming choices:
        // read them live, and redraw when they change.
        engine.setNamingMethodSupplier(peoplePage::getSelectedNamingMethod);
        engine.setNumberRegimentsSupplier(peoplePage::isAlwaysNumberRegimentsSelected);
        peoplePage.setNamingChangeListener(engine::modelChanged);
        engine.setFactionChangeListener(factionRecord -> {
            lastFactionRecord = factionRecord;
            applyFactionDrivenDefaults();
        });

        selector = new HudModeSelector<>(DesignerPage.class, DesignerPage::getLabel, this::showPage);
        // Each page carries a short line saying what it is for, under its name.
        List<Segment<DesignerPage>> segments = new ArrayList<>();
        for (DesignerPage page : DesignerPage.values()) {
            segments.add(new Segment<>(page, page.getLabel(), page.getSubLabel(), true));
        }
        selector.setSegments(segments);
        selector.setSelected(DesignerPage.FORCE);

        unitsTile = new HudStatTile(text("Designer.tile.units"), "");
        personnelTile = new HudStatTile(text("Designer.tile.personnel"), "");
        cashTile = new HudStatTile(text("Designer.tile.cash"), "");
        liftTile = new HudStatTile(text("Designer.tile.lift"), "");
        unitsTile.setToolTipText(text("Designer.tile.units.tooltip"));
        personnelTile.setToolTipText(text("Designer.tile.personnel.tooltip"));
        cashTile.setToolTipText(text("Designer.tile.cash.tooltip"));
        liftTile.setToolTipText(text("Designer.tile.lift.tooltip"));

        HudCheckBox autoOpenGuides = new HudCheckBox(text("Designer.guide.autoOpen"));
        autoOpenGuides.setSelected(guidePreferences.isGuideAutoOpen());
        autoOpenGuides.addActionListener(event -> guidePreferences.setGuideAutoOpen(autoOpenGuides.isSelected()));
        guideDrawer = new HudGuideDrawer(List.of(text("Designer.guide.tab.steps"), text("Designer.guide.tab.example"),
              text("Designer.guide.tab.terms")), autoOpenGuides, text("Designer.guide.close"), this::closeGuide);
        guideButton.addActionListener(event -> openGuide(currentPage));

        buildUi();
        showPage(DesignerPage.FORCE);
    }

    // region Layout

    private void buildUi() {
        setOpaque(true);
        setBackground(GROUND);

        pageComponents.put(DesignerPage.FORCE, forcePage.getComponent());
        pageComponents.put(DesignerPage.PEOPLE, peoplePage.getComponent());
        pageComponents.put(DesignerPage.LOGISTICS, logisticsPage.getComponent());
        pageComponents.put(DesignerPage.REVIEW, reviewPage.getComponent());
        pageHolder.setOpaque(true);
        pageHolder.setBackground(GROUND);
        for (Map.Entry<DesignerPage, JComponent> entry : pageComponents.entrySet()) {
            pageHolder.add(entry.getValue(), entry.getKey().name());
        }

        add(buildCommandBar(), BorderLayout.NORTH);
        add(pageHolder, BorderLayout.CENTER);
    }

    private JPanel buildCommandBar() {
        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.setOpaque(false);
        JLabel eyebrow = new JLabel(text("Designer.eyebrow").toUpperCase(Locale.ROOT));
        eyebrow.setForeground(TEXT_FAINT);
        eyebrow.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        JLabel title = new JLabel(text("Designer.title").toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.35f, 0.16f));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        identity.add(leftAligned(eyebrow));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(title));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));
        identity.add(leftAligned(subtitle));

        JPanel topRow = new JPanel(new BorderLayout(scaleForGUI(24), 0));
        topRow.setOpaque(false);
        topRow.add(identity, BorderLayout.CENTER);
        topRow.add(Hud.tileRow(unitsTile, personnelTile, cashTile, liftTile), BorderLayout.EAST);

        JPanel modesRow = new JPanel(new BorderLayout(scaleForGUI(8), 0));
        modesRow.setOpaque(false);
        modesRow.add(selector, BorderLayout.CENTER);
        modesRow.add(guideButton, BorderLayout.EAST);

        JPanel commandBar = new JPanel(new BorderLayout(0, scaleForGUI(12)));
        commandBar.setOpaque(true);
        commandBar.setBackground(GROUND);
        commandBar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));
        commandBar.add(topRow, BorderLayout.CENTER);
        commandBar.add(modesRow, BorderLayout.SOUTH);
        return commandBar;
    }

    /**
     * Puts the guide drawer over the dialog. Escape then closes an open guide, and does whatever the dialog does with
     * it otherwise.
     *
     * @param rootPane     the dialog's root pane
     * @param escapeAction what Escape does while no guide is open, such as closing the dialog
     */
    public void installGuide(JRootPane rootPane, Runnable escapeAction) {
        guideDrawer.install(rootPane);
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, "designerEscape");
        rootPane.getInputMap(JComponent.WHEN_FOCUSED).put(escape, "designerEscape");
        rootPane.getActionMap().put("designerEscape", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (guideDrawer.isVisible()) {
                    closeGuide();
                } else {
                    escapeAction.run();
                }
            }
        });
    }

    // endregion Layout

    // region Pages and guides

    /**
     * Shows a page.
     *
     * @param page the page
     */
    public void showPage(DesignerPage page) {
        closeGuide();
        currentPage = page;
        selector.setSelected(page);
        pageLayout.show(pageHolder, page.name());
        guideButton.setText(getFormattedTextAt(BUNDLE, "Designer.guide.button", page.getLabel())
                                  .toUpperCase(Locale.ROOT));
        refreshAll();
        for (Consumer<DesignerPage> listener : List.copyOf(pageListeners)) {
            listener.accept(page);
        }
        maybeOpenGuide();
    }

    /**
     * Opens the current page's guide if the player has not seen it yet and has not turned first-visit guides off. Does
     * nothing until the designer is on screen, so the dialog calls this again once it opens.
     */
    public void maybeOpenGuide() {
        if (isShowing() && !guidePreferences.isGuideSeen(currentPage) && guidePreferences.isGuideAutoOpen()) {
            openGuide(currentPage);
        }
    }

    public DesignerPage getCurrentPage() {
        return currentPage;
    }

    /** @param listener told the page whenever another page is shown */
    public void addPageListener(Consumer<DesignerPage> listener) {
        pageListeners.add(listener);
    }

    private void openGuide(DesignerPage page) {
        guidePreferences.setGuideSeen(page, true);
        String prefix = "Designer.guide." + page.name() + ".";
        List<Step> steps = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix + "step." + number + ".title")); number++) {
            steps.add(new Step(text(prefix + "step." + number + ".title"), text(prefix + "step." + number + ".text")));
        }
        List<ExampleLine> example = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix + "example." + number + ".who")); number++) {
            example.add(new ExampleLine(text(prefix + "example." + number + ".who"),
                  text(prefix + "example." + number + ".text")));
        }
        List<Term> terms = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix + "term." + number + ".name")); number++) {
            terms.add(new Term(text(prefix + "term." + number + ".name"), text(prefix + "term." + number + ".text")));
        }
        guideDrawer.open(new Guide(getFormattedTextAt(BUNDLE, "Designer.guide.eyebrow", page.getLabel()),
              text(prefix + "title"), steps, null, text("Designer.guide.exampleIntro"), example, terms, null));
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    // endregion Pages and guides

    // region Refresh

    /** @param listener told after the designer refreshes, such as the dialog's footer */
    public void addRefreshListener(Runnable listener) {
        refreshListeners.add(listener);
    }

    /** Refreshes once, however many changes arrive before it runs. */
    private void scheduleRefresh() {
        if (refreshPending) {
            return;
        }
        refreshPending = true;
        SwingUtilities.invokeLater(() -> {
            refreshPending = false;
            refreshAll();
        });
    }

    /** Refreshes the tiles, the subtitle and every page from the current roll and settings. */
    public void refreshAll() {
        peoplePage.refresh();
        logisticsPage.refresh();
        // Review asks the dialog to read every page back, which is too much work to repeat on every change while it
        // is not on screen; it is brought up to date when it is shown.
        if (currentPage == DesignerPage.REVIEW) {
            reviewPage.refresh();
        }
        refreshTiles();
        for (Runnable listener : List.copyOf(refreshListeners)) {
            listener.run();
        }
    }

    private void refreshTiles() {
        ForceSummary summary = forcePage.getSummary();
        PlanetarySystem system = campaign.getCurrentSystem();
        String date = MekHQ.getMHQOptions().getDisplayFormattedDate(campaign.getLocalDate());
        subtitle.setText((system == null)
              ? getFormattedTextAt(BUNDLE, "Designer.subtitle.noSystem", date, campaign.getPlayerForce().getName())
              : getFormattedTextAt(BUNDLE, "Designer.subtitle", date, campaign.getPlayerForce().getName(),
                    system.getName(campaign.getLocalDate())));

        if (summary.hasRoll()) {
            unitsTile.setValue(Integer.toString(summary.getIncludedUnits()), ACCENT_BRIGHT);
            unitsTile.setSub(getFormattedTextAt(BUNDLE, "Designer.tile.units.sub", topCommandName(summary),
                  summary.getStruckUnits()));
            int people = summary.getCrew() + peoplePage.getEstimatedSupportHires() + peoplePage.getEstimatedReserve();
            personnelTile.setValue(getFormattedTextAt(BUNDLE, "Designer.tile.personnel.value", people), ACCENT_BRIGHT);
            personnelTile.setSub(getFormattedTextAt(BUNDLE, "Designer.tile.personnel.sub",
                  peoplePage.getEstimatedPoolHires()));
        } else {
            unitsTile.setValue(text("Designer.symbol.none"), TEXT_FAINT);
            unitsTile.setSub(text("Designer.tile.units.empty"));
            personnelTile.setValue(text("Designer.symbol.none"), TEXT_FAINT);
            personnelTile.setSub(text("Designer.tile.personnel.empty"));
        }

        if (!logisticsPage.isProcessFinances()) {
            cashTile.setValue(text("Designer.tile.cash.off"), TEXT_FAINT);
            cashTile.setSub(text("Designer.tile.cash.off.sub"));
            cashTile.hideMeter();
        } else if (logisticsPage.isRandomizeStartingCash()) {
            cashTile.setValue(getFormattedTextAt(BUNDLE, "Designer.tile.cash.random", logisticsPage.getDiceCount()),
                  ACCENT_BRIGHT);
            cashTile.setSub(text("Designer.tile.cash.random.sub"));
            cashTile.hideMeter();
        } else {
            int months = logisticsPage.getStartingCashMonths();
            cashTile.setValue(getFormattedTextAt(BUNDLE, "Designer.tile.cash.months", months), ACCENT_BRIGHT);
            cashTile.setSub(text("Designer.tile.cash.months.sub"));
            cashTile.setMeter(months / (double) TRACKED_MONTHS, ACCENT, null);
        }

        if (!forcePage.isLiftRolled()) {
            liftTile.setValue(text("Designer.tile.lift.hire"), AMBER);
            liftTile.setSub(text("Designer.tile.lift.hire.sub"));
        } else {
            liftTile.setValue(Integer.toString(summary.getDropShips()), summary.hasRoll() ? ACCENT_BRIGHT : TEXT_FAINT);
            liftTile.setSub(getFormattedTextAt(BUNDLE, "Designer.tile.lift.sub", summary.getJumpShips()));
        }
    }

    private static String topCommandName(ForceSummary summary) {
        ForceDescriptor top = summary.getTopCommand();
        return (top == null) ? "" : top.parseName();
    }

    /** @return a line summing the roll up, for the dialog's footer */
    public String getFooterSummary() {
        ForceSummary summary = forcePage.getSummary();
        if (!summary.hasRoll()) {
            return text("Designer.footer.empty");
        }
        return getFormattedTextAt(BUNDLE, "Designer.footer.summary", topCommandName(summary),
              summary.getIncludedUnits(), summary.getRolledUnits());
    }

    /** @return {@code true} if there is a command to build */
    public boolean canBuild() {
        return reviewPage.canBuild();
    }

    /**
     * Supplies the Review page with what only the dialog knows.
     *
     * @param warnings        the build's resource warnings, such as a long generation
     * @param settingsChanged whether the settings differ from those the roll was made with
     */
    public void setReviewFacts(Supplier<List<String>> warnings, BooleanSupplier settingsChanged) {
        reviewPage.setDialogFacts(warnings, settingsChanged, peoplePage::isAugmentationVisible);
    }

    // endregion Refresh

    // region Faction-driven defaults

    /**
     * Applies the settings that follow from the faction picked on the Force page: the augmentation choices are only
     * offered to factions whose warriors are augmented, and factions that name their formations in Greek get the
     * Greek alphabet. Only those factions are switched: any other selection leaves the player's own naming choice
     * alone.
     */
    public void applyFactionDrivenDefaults() {
        FactionRecord factionRecord = lastFactionRecord;
        peoplePage.setAugmentationVisible(offersAugmentation(factionRecord));
        if (factionRecord == null) {
            LOGGER.debug("[NamingMethod] no faction reported yet; leaving naming alone");
            return;
        }
        boolean namesFormationsInGreek = usesGreekFormationNames(factionRecord);
        // At INFO because it fires once per faction selection, and because a report that the naming
        // method did not switch cannot be placed without it.
        LOGGER.info("[NamingMethod] faction now {} (isClan={}, ComStar/Word of Blake={}) -> {}",
              factionRecord.getKey(), factionRecord.isClan(), isComStarOrWordOfBlake(factionRecord),
              namesFormationsInGreek ? "defaulting to the Greek alphabet" : "leaving naming alone");
        if (namesFormationsInGreek) {
            peoplePage.setSelectedNamingMethod(ForceNamingMethod.GREEK_ALPHABET);
        }
    }

    /** The sub-faction key for Warrior House Thuggee, the Capellan command whose warriors are augmented. */
    static final String WARRIOR_HOUSE_THUGGEE_FACTION_KEY = "CC.THG";

    /**
     * Whether a faction's command can be generated with augmented warriors: every Clan, the Word of Blake Shadow
     * Divisions and Warrior House Thuggee.
     *
     * @param factionRecord the faction picked on the Force page, or {@code null} for none yet
     *
     * @return {@code true} if the augmentation choices should be offered
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

    static boolean usesGreekFormationNames(FactionRecord factionRecord) {
        return factionRecord.isClan() || isComStarOrWordOfBlake(factionRecord);
    }

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

    // endregion Faction-driven defaults

    // region Options

    public ForceGeneratorTab getEngine() {
        return engine;
    }

    /** Fills every page from a set of options, as on first show and on Restore Defaults. */
    public void loadValuesFromOptions(CommandGenerationOptions options) {
        engine.loadValuesFromOptions(options);
        peoplePage.loadValuesFromOptions(options);
        logisticsPage.loadValuesFromOptions(options);
        forcePage.refreshRail();
        refreshAll();
    }

    /** Reads every page into a set of options. Reading has no side effect on the campaign. */
    public void writeValuesToOptions(CommandGenerationOptions options) {
        peoplePage.writeValuesToOptions(options);
        engine.writeValuesToOptions(options);
        logisticsPage.writeValuesToOptions(options);
    }

    /** Writes the spares percentages, which are campaign options, to the campaign; called when the command is built. */
    public void writeSparesToCampaignOptions() {
        logisticsPage.writeSparesToCampaignOptions();
    }

    // endregion Options

    private static String text(String key) {
        return getTextAt(BUNDLE, key);
    }
}
