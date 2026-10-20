/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static megamek.common.options.PilotOptions.MD_ADVANTAGES;
import static megamek.common.units.Crew.DEATH;
import static mekhq.campaign.enums.DailyReportType.MEDICAL;
import static mekhq.campaign.enums.DailyReportType.SKILL_CHECKS;
import static mekhq.campaign.personnel.PersonnelOptions.COMPULSION_BIONIC_HATE;
import static mekhq.campaign.personnel.PersonnelOptions.EDGE_ADVANCED_SURGERY;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_BIOLOGICAL_MACHINIST;
import static mekhq.campaign.personnel.medical.BodyLocation.*;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.FAILED_SURGERY_RECOVERY;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.COSMETIC_SURGERY;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jakarta.annotation.Nullable;
import megamek.client.ui.WrapLayout;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternate;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.SurgeryRiskCalculator;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.SurgeryRiskCalculator.Risk;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.SurgeryRiskCalculator.SurgeryOdds;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.roleplay.CheckOdds;
import mekhq.campaign.roleplay.CheckTarget;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.ActionScheduler;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.dialog.glossary.GlossaryEntryDialog;

/**
 * Dialog for planning and performing advanced surgeries - replacement limbs, prosthetics, implants and the like - on a
 * single patient.
 *
 * <p>The dialog is laid out to stay narrow: a slim header carries the patient, their injury load, the surgeon and the
 * funds; a rail on the left lists the patient's injuries (each one narrows the catalogue to what treats it) above a
 * small body map; the catalogue fills the middle, and each procedure expands in place to show what it fixes, what
 * success and failure mean, its full details, and whether the patient can have it; and a dock along the bottom holds
 * today's surgical plan, its risk of death, and the confirm action. Each surgery leaves the patient with a recovery
 * injury whether it succeeds or not, so a plan with too many surgeries can kill the patient; the dock shows that
 * chance, explains it, and can move surgeries to later to make the plan safe.</p>
 *
 * <p>This dialog is modal and is shown immediately from its constructor when a non-{@code null} patient is
 * supplied.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class AdvancedSurgeriesDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(AdvancedSurgeriesDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AdvancedSurgeriesDialog";

    private static final int BODY_MAP_WIDTH = 120;
    private static final int BODY_MAP_HEIGHT = 225;
    private static final int RAIL_WIDTH = 220;
    /** Text width inside the rail, leaving room for its padding and scrollbar. */
    private static final int RAIL_TEXT_WIDTH = RAIL_WIDTH - 40;
    /**
     * Text width of an injury row, leaving room for the rail's padding and scrollbar, the row's border and padding,
     * and the injury's marker.
     */
    private static final int RAIL_INJURY_TEXT_WIDTH = RAIL_WIDTH - 80;
    private static final int RISK_WIDTH = 300;
    /** The narrowest the dock's plan column may become before the risk and action columns are squeezed. */
    private static final int PLAN_MINIMUM_WIDTH = 200;
    private static final int ACTION_WIDTH = 240;
    /** Text width for one half of an expanded procedure. */
    private static final int HALF_TEXT_WIDTH = 330;
    /** Text width beside a mark (a tick, cross or warning) in one half of an expanded procedure. */
    private static final int MARKED_TEXT_WIDTH = HALF_TEXT_WIDTH - 20;
    /** Text width across a whole expanded procedure. */
    private static final int FULL_TEXT_WIDTH = 540;
    /**
     * Horizontal space around an expanded procedure's details, beyond the action button: the catalogue's padding, the
     * row's border, and the expanded body's padding.
     */
    private static final int DETAILS_HORIZONTAL_CHROME = 12 + 12 + 2 + 30 + 10;
    private static final int DETAILS_GAP = 12;
    /** How far the catalogue's width must change before an expanded procedure's text is re-wrapped. */
    private static final int REWRAP_THRESHOLD = 16;
    /** Ordinary text, for measuring the average width of a character. */
    private static final String CHARACTER_WIDTH_SAMPLE = "Requires the Pilot Implants game option, and a surgeon.";
    /** The fewest characters on a wrapped line, however narrow the space. */
    private static final int MINIMUM_WRAP_CHARACTERS = 12;
    /** The largest text size, relative to the label font, that {@link #wrap(String, int)} is used for. */
    private static final float WRAP_FONT_SIZE = 0.92f;
    /** Stands in for spaces inside markup tags while {@code wordWrap} runs, so it can't break a tag in two. */
    private static final String TAG_SPACE = "\u0001";
    private static final Pattern MARKUP_TAG = Pattern.compile("<[^>]*>");
    /** The average character width of each font {@link #wrapToPixels(String, int, Font)} has measured. */
    private static final Map<Font, Double> AVERAGE_CHARACTER_WIDTHS = new HashMap<>();
    /** How long typing must pause before the catalogue is narrowed to the search, in milliseconds. */
    private static final int SEARCH_DELAY = 150;
    private static final int LOCAL_SURGEON_COST_MULTIPLIER = 10;
    private static final int MACHINIST_MODIFIER = -2;

    /**
     * Valid body locations that can be targeted by replacement or prosthetic treatments. The order of this list
     * controls the ordering in the GUI.
     */
    private static final List<BodyLocation> VALID_BODY_LOCATIONS = List.of(
          BRAIN,
          SKULL,
          FACE,
          MOUTH,
          JAW,
          EYES,
          EARS,
          LEFT_ARM,
          RIGHT_ARM,
          LEFT_HAND,
          RIGHT_HAND,
          CHEST,
          RIBS,
          SPINE,
          HEART,
          LUNGS,
          ORGANS,
          ABDOMEN,
          GROIN,
          RUMP,
          LEFT_LEG,
          RIGHT_LEG,
          LEFT_FOOT,
          RIGHT_FOOT,
          BONES,
          INTERNAL
    );

    /**
     * Sub-locations that sit beneath a hand or foot in the {@link BodyLocation} hierarchy, but are part of the lower
     * arm or leg. Replacing a hand or foot shouldn't fix injuries here; only replacing the whole arm or leg should.
     */
    private static final List<BodyLocation> LOCATIONS_BEYOND_HAND_AND_FOOT = List.of(
          LEFT_FOREARM,
          RIGHT_FOREARM,
          LEFT_KNEE,
          RIGHT_KNEE,
          LEFT_SHIN,
          RIGHT_SHIN,
          LEFT_CALF,
          RIGHT_CALF
    );

    /** The broad body regions the catalogue can be narrowed to. */
    private enum Region {
        ALL, HEAD, ARMS, TORSO, LEGS, WHOLE_BODY;

        private String getLabel() {
            return getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.region." + name());
        }

        private static Region of(BodyLocation location) {
            BodyLocation primary = location.getPrimaryLocation();
            if (primary == null) {
                return WHOLE_BODY;
            }
            return switch (primary) {
                case HEAD -> HEAD;
                case LEFT_ARM, RIGHT_ARM, LEFT_HAND, RIGHT_HAND -> ARMS;
                case CHEST, ABDOMEN -> TORSO;
                case LEFT_LEG, RIGHT_LEG, LEFT_FOOT, RIGHT_FOOT -> LEGS;
                default -> WHOLE_BODY;
            };
        }
    }

    /** Whether a requirement for a procedure is met, and if not, whether the player can do anything about it. */
    private enum RequirementStatus {
        /** The requirement is met. */
        MET,
        /** Worth knowing, but doesn't stop the procedure (such as needing a local surgeon). */
        NOTE,
        /** Not met, but the player can change that (such as by travelling to a better-equipped world). */
        FIXABLE,
        /** Not met, and there is nothing the player can do about it. */
        BLOCKED;

        private boolean isBlocking() {
            return this == FIXABLE || this == BLOCKED;
        }
    }

    /**
     * One line of a procedure's "can the patient have this?" checklist.
     *
     * @param status whether the requirement is met
     * @param title  what is being checked
     * @param text   the explanation
     */
    private record Requirement(RequirementStatus status, String title, String text) {}

    /**
     * A procedure that can be performed at a specific body location; one row of the catalogue.
     *
     * @param type     the procedure
     * @param location where it would be performed
     */
    private record Treatment(ProstheticType type, BodyLocation location) {}

    /** A procedure in the plan, which may be set aside for a later session. */
    private static final class PlanEntry {
        private final ProstheticType type;
        private boolean isDeferred;

        private PlanEntry(ProstheticType type) {
            this.type = type;
        }
    }

    /**
     * Represents a planned surgery consisting of a prosthetic type and a specific body location.
     *
     * @param type     the prosthetic type to be installed
     * @param location the body location being treated
     *
     * @author Illiani
     * @since 0.50.10
     */
    private record PlannedSurgery(ProstheticType type, BodyLocation location) {
        /**
         * Builds a simple label combining the prosthetic display name and the location name, for use in reports.
         *
         * @return a human-readable label for the planned surgery
         *
         * @author Illiani
         * @since 0.50.10
         */
        public String getLabel() {
            return type.toString() + "-" + location.locationName();
        }
    }

    /**
     * What a planned surgery will do, as the risk of death sees it.
     *
     * @param surgery       the surgery
     * @param hitsCleared   the hits of the injuries a success removes, not counting any already removed by an earlier
     *                      surgery in the plan
     * @param successChance the chance of the surgery succeeding, from 0 to 1
     */
    private record SurgeryPreview(PlannedSurgery surgery, int hitsCleared, double successChance) {
        /** @return this surgery as the risk calculator sees it, including any hit its marker injury adds */
        SurgeryOdds toOdds() {
            return new SurgeryOdds(hitsCleared, SurgeryRiskCalculator.getMarkerHits(surgery.type()), successChance);
        }
    }

    /**
     * A clickable row in the injury rail, and the filter it applies.
     *
     * @param row       the row
     * @param locations the body locations it narrows the catalogue to, or {@code null} for every location
     */
    private record RailRow(JPanel row, @Nullable Set<BodyLocation> locations) {}

    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final Person patient;
    private final AbstractLocation currentLocation;
    private final boolean isGMMode;
    private Person surgeon; // can be null

    private final Map<BodyLocation, List<Injury>> relevantInjuries = new HashMap<>();
    private final List<Treatment> catalogue = new ArrayList<>();
    private final Map<BodyLocation, PlanEntry> plan = new LinkedHashMap<>();
    private final Map<Integer, Person> localSurgeons = new HashMap<>();
    // The patient, their injuries and the requirements don't change while the dialog is open, and every catalogue
    // rebuild asks for these
    private final Map<Treatment, List<Requirement>> requirementCache = new HashMap<>();
    private final Map<Treatment, List<Injury>> clearedInjuryCache = new HashMap<>();
    private final Map<ProstheticType, Optional<Money>> costCache = new EnumMap<>(ProstheticType.class);

    // Catalogue filters
    private Region region = Region.ALL;
    private String query = "";
    private boolean isTreatsOnly;
    private boolean isHideUnavailable;
    private @Nullable Set<BodyLocation> locationFilter;
    private String locationFilterLabel = "";
    private @Nullable Treatment expanded;
    /** The catalogue viewport's width when its rows were last built, or 0 before it has been laid out. */
    private int catalogueWidth;

    // Current plan totals, refreshed with the plan
    private Money todayCost = Money.zero();
    private Risk todayRisk;
    private List<SurgeryPreview> todayPreviews = List.of();

    // Components
    private BodyMap bodyMap;
    private final List<RailRow> railRows = new ArrayList<>();
    private final ActionScheduler searchScheduler = new ActionScheduler(this::refreshCatalogue, SEARCH_DELAY);
    private final Map<Region, HudChip> regionChips = new EnumMap<>(Region.class);
    private HudChip treatsChip;
    private HudChip hideUnavailableChip;
    private HudChip locationChip;
    private final JLabel resultLine = Hud.hint("");
    private final JPanel catalogueRows = column();
    private final JLabel fundsLabel = new JLabel();
    private final JPanel dock = new JPanel(new GridBagLayout());
    private final HudCheckBox acknowledgeRisk = new HudCheckBox("");
    private HudButton beginButton;
    private HudButton gmSkipButton;
    private HudGuideDrawer guideDrawer;

    /**
     * Creates and displays a new {@link AdvancedSurgeriesDialog} for the given patient.
     *
     * <p>If the patient is {@code null}, the dialog performs no initialization and returns without being shown.</p>
     *
     * @param campaign    the active campaign context
     * @param campaignGUI the gui, for relaunching the dialog in or out of GM Mode
     * @param patient     the patient undergoing treatment, or {@code null}
     * @param isGMMode    whether the dialog has been launched in GM Mode (bypassing all restrictions)
     *
     * @author Illiani
     * @since 0.50.10
     */
    public AdvancedSurgeriesDialog(Campaign campaign, CampaignGUI campaignGUI, @Nullable Person patient,
          boolean isGMMode) {
        this.patient = patient;
        this.campaign = campaign;
        this.currentLocation = patient == null ?
                                     campaign.getPlayerForce().getForceDetachment().getCurrentLocation() :
                                     patient.getCurrentLocation();
        this.campaignOptions = campaign.getCampaignOptions();
        this.isGMMode = isGMMode;

        if (patient == null) {
            return;
        }

        if (currentLocation == null) {
            LOGGER.error("Failed to fetch a location for patient and player force fallback.");
            return;
        }

        surgeon = getSurgeon(currentLocation); // can return null

        gatherRelevantInjuries(patient.getInjuries());
        gatherCatalogue();
        isTreatsOnly = catalogue.stream().anyMatch(treatment -> !getTreatedInjuries(treatment).isEmpty());
        expanded = catalogue.stream()
                         .filter(treatment -> !isTreatsOnly || !getTreatedInjuries(treatment).isEmpty())
                         .findFirst()
                         .orElse(null);

        setTitle(getText("accessingTerminal.title"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initializeUI(campaignGUI);
        refreshAll();
        setMinimumSize(scaleForGUI(1100, 720));
        pack();
        setLocationRelativeTo(null);
        setModal(true);
        setPreferences(this); // Must be before setVisible
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                maybeOpenGuide();
            }
        });
        setVisible(true);
    }

    // region Layout

    /**
     * Assembles the header, the injury rail, the catalogue, and the plan dock.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void initializeUI(CampaignGUI gui) {
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(buildRail(), BorderLayout.WEST);
        body.add(buildCatalogue(), BorderLayout.CENTER);

        dock.setOpaque(true);
        dock.setBackground(SURFACE_DEEP);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildHeader(gui), BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        content.add(dock, BorderLayout.SOUTH);
        setContentPane(content);
        buildGuide();

        acknowledgeRisk.setText(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.acknowledge",
              patient.getGivenName()));
        acknowledgeRisk.addActionListener(event -> refreshActions());
        beginButton = new HudButton("", true, true);
        beginButton.addActionListener(this::onConfirm);
    }

    /** The patient on the left; their injury load, the surgeon, the funds and the utility buttons on the right. */
    private JPanel buildHeader(CampaignGUI gui) {
        JLabel name = new JLabel(patient.getFullTitle().toUpperCase(Locale.ROOT));
        name.setForeground(ACCENT_BRIGHT);
        name.setFont(hudFont(Font.BOLD, 1.1f, 0.1f));
        // A long name pushes the subtitle onto a second line, and WrapLayout (unlike FlowLayout) makes room for it
        JPanel identity = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(12), scaleForGUI(2)));
        identity.add(name);
        identity.add(Hud.hint(describeWhereabouts()));

        int hits = patient.getTotalInjurySeverity();
        JLabel loadValue = new JLabel(hits + "/" + DEATH);
        loadValue.setForeground(hits >= DEATH - 1 ? DANGER : AMBER);
        loadValue.setFont(hudFont(Font.BOLD, 0.92f, 0.0f));
        HitMeter loadMeter = new HitMeter(hits, hits >= DEATH - 1 ? DANGER : AMBER);
        loadMeter.setPreferredSize(scaleForGUI(60, 10));
        loadMeter.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.header.load.toolTipText",
              hits, DEATH));

        String surgeonText = surgeon == null ?
                                   getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.header.surgeon.none") :
                                   getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.header.surgeon",
                                         surgeon.getFullName(), getSurgeryLevel(surgeon),
                                         getCheckTarget(surgeon, null).describe());
        fundsLabel.setForeground(TEXT_MUTED);
        fundsLabel.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));

        HudButton guideButton = new HudButton("?  " + upper("AdvancedSurgeriesDialog.guide.button"), false, true);
        guideButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.guide.button.toolTipText"));
        guideButton.addActionListener(event -> openGuide());

        JPanel stats = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(8), 0));
        stats.add(Hud.hint(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.header.load")));
        stats.add(loadMeter);
        stats.add(loadValue);
        stats.add(divider());
        stats.add(Hud.hint(surgeonText));
        stats.add(divider());
        stats.add(fundsLabel);
        stats.add(guideButton);
        if (campaign.isGM()) {
            HudButton modeButton = new HudButton(upper(isGMMode ?
                                                             "AdvancedSurgeriesDialog.button.normalMode" :
                                                             "AdvancedSurgeriesDialog.button.gmMode"), false, true);
            modeButton.addActionListener(event -> {
                dispose();
                new AdvancedSurgeriesDialog(campaign, gui, patient, !isGMMode);
            });
            gmSkipButton = new HudButton(upper("AdvancedSurgeriesDialog.button.gm"), false, true);
            gmSkipButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.button.gm.toolTipText"));
            gmSkipButton.addActionListener(this::onGMConfirm);
            stats.add(modeButton);
            stats.add(gmSkipButton);
        }

        JPanel header = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        header.setOpaque(true);
        header.setBackground(GROUND);
        header.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(12), scaleForGUI(8), scaleForGUI(12))));
        header.add(identity, BorderLayout.CENTER);
        header.add(stats, BorderLayout.EAST);
        return header;
    }

    /** The body map and its key, above the injuries; each part or injury narrows the catalogue to what treats it. */
    private JComponent buildRail() {
        JPanel rail = column();
        rail.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(10), scaleForGUI(10),
              scaleForGUI(10)));

        bodyMap = new BodyMap(this::onBodyPartChosen);
        bodyMap.setPreferredSize(scaleForGUI(BODY_MAP_WIDTH, BODY_MAP_HEIGHT));
        JPanel bodyHolder = Hud.transparentPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bodyHolder.add(bodyMap);
        rail.add(leftAligned(bodyHolder));
        rail.add(Box.createVerticalStrut(scaleForGUI(8)));
        JPanel legend = Hud.transparentPanel(new FlowLayout(FlowLayout.CENTER, scaleForGUI(12), 0));
        legend.add(legendLine(translucent(AMBER, 160),
              getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.legend.injured")));
        legend.add(legendLine(translucent(ACCENT, 160),
              getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.legend.planned")));
        rail.add(leftAligned(legend));
        rail.add(Box.createVerticalStrut(scaleForGUI(4)));
        JLabel clickHint = Hud.hint(wrap("<center>" + getTextAt(RESOURCE_BUNDLE,
              "AdvancedSurgeriesDialog.legend.click") + "</center>", RAIL_TEXT_WIDTH));
        JPanel hintHolder = Hud.transparentPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        hintHolder.add(clickHint);
        rail.add(leftAligned(hintHolder));

        rail.add(Box.createVerticalStrut(scaleForGUI(10)));
        JPanel injuriesHeading = Hud.transparentPanel(new BorderLayout());
        injuriesHeading.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), 0, 0, 0)));
        injuriesHeading.add(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.injuries")),
              BorderLayout.WEST);
        rail.add(leftAligned(injuriesHeading));
        rail.add(Box.createVerticalStrut(scaleForGUI(6)));

        JLabel allLabel = new JLabel(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.rail.all"));
        allLabel.setForeground(TEXT);
        allLabel.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        addRailRow(rail, allLabel, null, getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.rail.all"));

        List<Injury> injuries = patient.getInjuries()
                                      .stream()
                                      .filter(injury -> !injury.getSubType().isPermanentModification())
                                      .sorted(Comparator.comparing(Injury::getName))
                                      .toList();
        if (injuries.isEmpty()) {
            rail.add(leftAligned(wrappedHint(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.injuries.none"),
                  RAIL_TEXT_WIDTH)));
        }
        for (Injury injury : injuries) {
            Set<BodyLocation> treatableAt = new LinkedHashSet<>();
            for (BodyLocation location : VALID_BODY_LOCATIONS) {
                if (relevantInjuries.getOrDefault(location, List.of()).contains(injury)) {
                    treatableAt.add(location);
                }
            }
            String where = Utilities.capitalize(injury.getLocation().locationName());
            String healing = injury.isPermanent() ?
                                   getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.injuries.permanent") :
                                   getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.injuries.heals",
                                         injury.getTime());
            // The name and the line beneath are separate labels, so wordWrap only ever counts plain text
            JLabel name = new JLabel(wrap(injury.getName(), RAIL_INJURY_TEXT_WIDTH));
            name.setForeground(TEXT);
            name.setFont(hudFont(Font.BOLD, 0.88f, 0.0f));
            JLabel sub = new JLabel(wrap(joined(where, healing), RAIL_INJURY_TEXT_WIDTH));
            sub.setForeground(TEXT_MUTED);
            sub.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));
            JPanel text = column();
            text.add(leftAligned(name));
            text.add(leftAligned(sub));
            JLabel marker = new JLabel(new SquareIcon(AMBER));
            marker.setVerticalAlignment(SwingConstants.TOP);
            JPanel content = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
            content.add(marker, BorderLayout.WEST);
            content.add(text, BorderLayout.CENTER);
            if (treatableAt.isEmpty()) {
                // Nothing in the catalogue treats it (such as a recovery injury), so there is nothing to narrow to
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(true);
                row.setBackground(GROUND);
                row.setBorder(rowBorder(DIVIDER));
                row.setToolTipText(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.rail.untreatable.toolTipText"));
                row.add(content, BorderLayout.CENTER);
                rail.add(leftAligned(row));
                rail.add(Box.createVerticalStrut(scaleForGUI(4)));
            } else {
                addRailRow(rail, content, treatableAt, where);
            }
        }

        JScrollPane scroll = scroll(rail, GROUND);
        scroll.setPreferredSize(new Dimension(scaleForGUI(RAIL_WIDTH), 0));
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, scaleForGUI(1), BORDER));
        return scroll;
    }

    private void addRailRow(JPanel rail, JComponent content, @Nullable Set<BodyLocation> locations, String label) {
        JPanel row = clickable(content, () -> {
            if (locations == null) {
                clearLocationFilter();
            } else {
                setLocationFilter(locations, label);
            }
        });
        railRows.add(new RailRow(row, locations));
        rail.add(leftAligned(row));
        rail.add(Box.createVerticalStrut(scaleForGUI(4)));
    }

    /** The search field and filters above the catalogue, which expands each procedure in place. */
    private JComponent buildCatalogue() {
        JTextField search = new JTextField(16);
        Hud.styleField(search);
        search.setToolTipText(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.search.toolTipText"));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                onSearch(search.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                onSearch(search.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                onSearch(search.getText());
            }
        });

        JPanel toolbar = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(4), scaleForGUI(4)));
        toolbar.add(search);
        for (Region value : Region.values()) {
            HudChip chip = new HudChip(value.getLabel(), () -> {
                region = value;
                // A region and a body location rarely overlap, so choosing a region replaces the location filter
                clearLocationFilter();
            });
            regionChips.put(value, chip);
            toolbar.add(chip);
        }
        toolbar.add(divider());
        treatsChip = new HudChip(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.filter.treats"), () -> {
            isTreatsOnly = !isTreatsOnly;
            refreshCatalogue();
        });
        treatsChip.setToolTipText(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.filter.treats.toolTipText"));
        hideUnavailableChip = new HudChip(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.filter.hideUnavailable"),
              () -> {
                  isHideUnavailable = !isHideUnavailable;
                  refreshCatalogue();
              });
        locationChip = new HudChip("", this::clearLocationFilter);
        locationChip.setToolTipText(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.filter.location.toolTipText"));
        toolbar.add(treatsChip);
        toolbar.add(hideUnavailableChip);
        toolbar.add(locationChip);

        JPanel top = column();
        top.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(10), scaleForGUI(6), scaleForGUI(10))));
        top.add(leftAligned(toolbar));
        top.add(leftAligned(resultLine));

        catalogueRows.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(12), scaleForGUI(12), scaleForGUI(12)));
        JPanel catalogue = new JPanel(new BorderLayout());
        catalogue.setOpaque(true);
        catalogue.setBackground(SURFACE_DEEP);
        catalogue.add(top, BorderLayout.NORTH);
        JScrollPane catalogueScroll = scroll(catalogueRows, SURFACE_DEEP);
        // An expanded procedure's details wrap to the catalogue's width, so they are re-wrapped when it changes
        catalogueScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                int width = catalogueScroll.getViewport().getWidth();
                if (Math.abs(width - catalogueWidth) >= scaleForGUI(REWRAP_THRESHOLD)) {
                    catalogueWidth = width;
                    if (expanded != null) {
                        refreshCatalogue();
                    }
                }
            }
        });
        catalogue.add(catalogueScroll, BorderLayout.CENTER);
        return catalogue;
    }

    // endregion Layout

    // region Refresh

    private void refreshAll() {
        refreshPlan();
        refreshCatalogue();
    }

    private void onPlanChanged() {
        refreshPlan();
        refreshCatalogue();
    }

    /** Rebuilds everything that depends on the plan: the risk, the dock, the body map and the funds. */
    private void refreshPlan() {
        todayPreviews = previewToday();
        todayRisk = SurgeryRiskCalculator.calculate(patient.getTotalInjurySeverity(),
              todayPreviews.stream().map(SurgeryPreview::toOdds).toList());
        todayCost = getCost(getTodaySurgeries());

        Money funds = campaign.getPlayerForce().getFunds();
        fundsLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.header.funds",
              funds.toAmountString()));

        acknowledgeRisk.setSelected(false);
        rebuildDock();
        refreshBodyMap();
    }

    private void refreshRail() {
        for (RailRow railRow : railRows) {
            // Each row owns its set of locations, so comparing by identity tells apart rows with equal sets (such
            // as two injuries on one arm), and never matches a filter set from the body map
            boolean isActive = railRow.locations() == locationFilter;
            railRow.row().setBackground(isActive ? SURFACE_HIGHLIGHT : GROUND);
            railRow.row().setBorder(rowBorder(isActive ? ACCENT : DIVIDER));
        }
        BodyPart selectedPart = null;
        for (BodyPart part : BodyPart.values()) {
            if (locationFilter != null && locationFilter.equals(getLocationsIn(part))) {
                selectedPart = part;
            }
        }
        bodyMap.setSelected(selectedPart);
    }

    private void refreshCatalogue() {
        regionChips.forEach((value, chip) -> chip.setActive(value == region));
        treatsChip.setActive(isTreatsOnly);
        hideUnavailableChip.setActive(isHideUnavailable);
        locationChip.setVisible(locationFilter != null);
        locationChip.setText(locationFilterLabel + "  ✕");
        locationChip.setActive(true);
        refreshRail();

        String lowerQuery = query.strip().toLowerCase(Locale.ROOT);
        int hidden = 0;
        List<Treatment> shown = new ArrayList<>();
        for (Treatment treatment : catalogue) {
            if (locationFilter != null && !locationFilter.contains(treatment.location())) {
                continue;
            }
            if (region != Region.ALL && Region.of(treatment.location()) != region) {
                continue;
            }
            if (isTreatsOnly && getTreatedInjuries(treatment).isEmpty()) {
                continue;
            }
            if (!lowerQuery.isEmpty() && !matchesQuery(treatment, lowerQuery)) {
                continue;
            }
            if (isHideUnavailable && isBlocked(treatment)) {
                hidden++;
                continue;
            }
            shown.add(treatment);
        }

        catalogueRows.removeAll();
        JComponent expandedRow = null;
        BodyLocation group = null;
        for (Treatment treatment : shown) {
            if (treatment.location() != group) {
                group = treatment.location();
                catalogueRows.add(leftAligned(groupHeader(group)));
            }
            JComponent row = catalogueRow(treatment);
            if (treatment.equals(expanded)) {
                expandedRow = row;
            }
            catalogueRows.add(leftAligned(row));
            catalogueRows.add(Box.createVerticalStrut(scaleForGUI(3)));
        }
        catalogueRows.revalidate();
        catalogueRows.repaint();
        if (expandedRow != null) {
            JComponent target = expandedRow;
            SwingUtilities.invokeLater(() -> target.scrollRectToVisible(new Rectangle(target.getSize())));
        }

        String line = locationFilter == null ?
                            getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.results", shown.size()) :
                            getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.results.location",
                                  shown.size(), locationFilterLabel);
        if (hidden > 0) {
            line = joined(line, getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.results.hidden",
                  hidden));
        }
        line = joined(line, getTextAt(RESOURCE_BUNDLE, shown.isEmpty() ?
                                                             (isTreatsOnly ?
                                                                    "AdvancedSurgeriesDialog.results.none.treats" :
                                                                    "AdvancedSurgeriesDialog.results.none") :
                                                             "AdvancedSurgeriesDialog.results.hint"));
        resultLine.setText(line);
    }

    private boolean matchesQuery(Treatment treatment, String lowerQuery) {
        String haystack = (treatment.type().toString() + " " + treatment.location().locationName() + " " +
                                 Region.of(treatment.location()).getLabel()).toLowerCase(Locale.ROOT);
        return haystack.contains(lowerQuery);
    }

    private void onSearch(String text) {
        query = text;
        searchScheduler.schedule();
    }

    private void setLocationFilter(Set<BodyLocation> locations, String label) {
        locationFilter = locations;
        locationFilterLabel = label;
        region = Region.ALL;
        // Open the first procedure that treats something there, so the player sees their options straight away
        expanded = catalogue.stream()
                         .filter(treatment -> locations.contains(treatment.location()))
                         .filter(treatment -> !isTreatsOnly || !getTreatedInjuries(treatment).isEmpty())
                         .findFirst()
                         .orElse(expanded);
        refreshCatalogue();
    }

    private void clearLocationFilter() {
        locationFilter = null;
        locationFilterLabel = "";
        refreshCatalogue();
    }

    // endregion Refresh

    // region Catalogue rows

    private JComponent groupHeader(BodyLocation location) {
        long injuries = relevantInjuries.getOrDefault(location, List.of())
                              .stream()
                              .filter(injury -> !injury.getSubType().isPermanentModification())
                              .count();
        JLabel name = Hud.eyebrow(location.locationName());
        name.setForeground(TEXT_MUTED);
        JLabel count = new JLabel(injuries == 0 ?
                                        getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.group.noInjuries") :
                                        getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.group.injuries",
                                              injuries));
        count.setForeground(injuries == 0 ? TEXT_FAINT : AMBER);
        count.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
        JPanel header = Hud.transparentPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(2), scaleForGUI(3),
              scaleForGUI(2)));
        header.add(name, BorderLayout.WEST);
        header.add(count, BorderLayout.EAST);
        return header;
    }

    /** One procedure: a one-line summary that expands in place to show everything about it. */
    private JComponent catalogueRow(Treatment treatment) {
        ProstheticType type = treatment.type();
        boolean isExpanded = treatment.equals(expanded);
        PlanEntry entry = plan.get(treatment.location());
        boolean isPlanned = entry != null && entry.type == type;
        RequirementStatus worst = getWorstRequirement(treatment);
        int treated = getTreatedInjuries(treatment).size();

        Color ring = isPlanned ? ACCENT : worst == RequirementStatus.BLOCKED ? DANGER :
                                                worst == RequirementStatus.FIXABLE ? AMBER :
                                                      treated > 0 ? READY : TEXT_FAINT;
        JLabel name = new JLabel(type.toString());
        name.setForeground(worst.isBlocking() ? TEXT_MUTED : TEXT);
        name.setFont(hudFont(Font.BOLD, 0.95f, 0.0f));
        name.setIcon(new DotIcon(ring));
        name.setIconTextGap(scaleForGUI(10));

        JPanel tags = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(4), 0));
        tags.add(Hud.hint(describeTier(type)));
        if (isPlanned) {
            tags.add(tag(getTextAt(RESOURCE_BUNDLE, entry.isDeferred ?
                                                          "AdvancedSurgeriesDialog.tag.later" :
                                                          "AdvancedSurgeriesDialog.tag.planned"), ACCENT_BRIGHT));
        }
        if (treated > 0) {
            tags.add(tag(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.tag.treats", treated), READY));
        }
        if (worst == RequirementStatus.BLOCKED) {
            tags.add(tag(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.tag.blocked"), DANGER));
        } else if (worst == RequirementStatus.FIXABLE) {
            tags.add(tag(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.tag.fixable"), AMBER));
        }

        Money cost = getCost(type);
        JLabel costLabel = new JLabel(cost == null ? "—" : cost.toAmountString());
        costLabel.setForeground(TEXT);
        costLabel.setFont(hudFont(Font.BOLD, 0.88f, 0.0f));
        JLabel chevron = new JLabel(getTextAt(RESOURCE_BUNDLE, isExpanded ?
                                                                     "AdvancedSurgeriesDialog.row.close" :
                                                                     "AdvancedSurgeriesDialog.row.details"));
        chevron.setForeground(ACCENT_BRIGHT);
        chevron.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));

        JPanel summary = Hud.transparentPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, scaleForGUI(10));
        summary.add(name, constraints);
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        summary.add(tags, constraints);
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        summary.add(costLabel, constraints);
        constraints.insets = new Insets(0, 0, 0, 0);
        summary.add(chevron, constraints);

        JPanel header = clickable(summary, () -> {
            expanded = isExpanded ? null : treatment;
            refreshCatalogue();
        });
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(7), scaleForGUI(10), scaleForGUI(7),
              scaleForGUI(10)));

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(isExpanded ? SURFACE : SURFACE_DEEP);
        row.setBorder(BorderFactory.createLineBorder(isExpanded ? ACCENT : DIVIDER, scaleForGUI(1)));
        row.add(header, BorderLayout.NORTH);
        if (isExpanded) {
            row.add(buildExpanded(treatment), BorderLayout.CENTER);
        }
        return row;
    }

    /**
     * Everything about a procedure: what it fixes and what success and failure mean on the left, whether the patient
     * can have it on the right, and its full details and the plan action beneath.
     */
    private JComponent buildExpanded(Treatment treatment) {
        ProstheticType type = treatment.type();

        // Left: what it fixes, success and failure
        JPanel left = column();
        left.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.detail.fixes"))));
        left.add(Box.createVerticalStrut(scaleForGUI(4)));
        List<Injury> treated = getTreatedInjuries(treatment);
        List<Injury> replaced = getInjuriesClearedBy(treatment)
                                      .stream()
                                      .filter(injury -> injury.getSubType().isPermanentModification())
                                      .toList();
        for (Injury injury : treated) {
            left.add(leftAligned(markedLine("✓", READY, joined(injury.getName(),
                  Utilities.capitalize(injury.getLocation().locationName())))));
        }
        for (Injury injury : replaced) {
            left.add(leftAligned(markedLine("↻", ACCENT, getFormattedTextAt(RESOURCE_BUNDLE,
                  "AdvancedSurgeriesDialog.detail.replaces", injury.getName()))));
        }
        if (treated.isEmpty() && replaced.isEmpty()) {
            left.add(leftAligned(wrappedHint(getFormattedTextAt(RESOURCE_BUNDLE,
                  type.getInjuryType().getSubType().isImplant() ?
                        "AdvancedSurgeriesDialog.detail.fixes.elective" :
                        "AdvancedSurgeriesDialog.detail.fixes.none", patient.getGivenName()), HALF_TEXT_WIDTH)));
        }
        left.add(Box.createVerticalStrut(scaleForGUI(8)));

        double chance = getPlannedSuccessChance(treatment);
        int hitsCleared = getHitsCleared(treatment);
        int net = SurgeryRiskCalculator.RECOVERY_INJURY_HITS + SurgeryRiskCalculator.getMarkerHits(type) - hitsCleared;
        HudStatTile success = new HudStatTile(getFormattedTextAt(RESOURCE_BUNDLE,
              "AdvancedSurgeriesDialog.detail.success", formatChance(chance)),
              hitsCleared > 0 ?
                    getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.detail.success.sub", hitsCleared) :
                    getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.detail.success.sub.none"));
        success.setValue(formatHitChange(net), net < 0 ? READY : TEXT);
        HudStatTile failure = new HudStatTile(getFormattedTextAt(RESOURCE_BUNDLE,
              "AdvancedSurgeriesDialog.detail.failure", formatChance(1 - chance)),
              getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.detail.failure.sub"));
        failure.setValue(formatHitChange(SurgeryRiskCalculator.RECOVERY_INJURY_HITS), AMBER);
        left.add(leftAligned(Hud.tileRow(success, failure)));

        // Right: requirements
        JPanel right = column();
        right.add(leftAligned(Hud.eyebrow(getFormattedTextAt(RESOURCE_BUNDLE,
              "AdvancedSurgeriesDialog.detail.requirements", patient.getGivenName()))));
        right.add(Box.createVerticalStrut(scaleForGUI(4)));
        if (isGMMode) {
            right.add(leftAligned(wrappedHint(getTextAt(RESOURCE_BUNDLE,
                  "AdvancedSurgeriesDialog.detail.requirements.gm"), HALF_TEXT_WIDTH)));
        }
        for (Requirement requirement : getRequirements(treatment)) {
            right.add(leftAligned(requirementLine(requirement)));
            right.add(Box.createVerticalStrut(scaleForGUI(3)));
        }

        JPanel halves = Hud.transparentPanel(new GridLayout(1, 2, scaleForGUI(16), 0));
        halves.add(left);
        halves.add(right);

        // Bottom: full details and the plan action
        double healingTimeMultiplier = campaignOptions.get(CampaignOption.ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER);
        List<String> lines = type.getDetailLines(getFaction(), campaign.getGameYear(), healingTimeMultiplier,
              treatment.location());
        JComponent action = buildRowAction(treatment);
        JLabel details = new JLabel();
        details.setForeground(TEXT_MUTED);
        details.setFont(hudFont(Font.PLAIN, 0.84f, 0.0f));
        String bullet = "&bull;&nbsp;";
        details.setText(wrapToPixels(bullet + String.join("<br>" + bullet, lines),
              getDetailsWidth(action.getPreferredSize().width), details.getFont()));

        JPanel bottom = Hud.transparentPanel(new BorderLayout(scaleForGUI(DETAILS_GAP), 0));
        bottom.add(details, BorderLayout.CENTER);
        JPanel actionHolder = Hud.transparentPanel(new BorderLayout());
        actionHolder.add(action, BorderLayout.SOUTH);
        bottom.add(actionHolder, BorderLayout.EAST);

        JPanel body = column();
        body.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(30), scaleForGUI(10), scaleForGUI(10))));
        body.add(leftAligned(halves));
        body.add(Box.createVerticalStrut(scaleForGUI(10)));
        body.add(leftAligned(bottom));
        return body;
    }

    /** The plan action for a procedure: add, replace, or remove it, or say why it can't be added. */
    private JComponent buildRowAction(Treatment treatment) {
        PlanEntry entry = plan.get(treatment.location());
        HudButton button;
        if (entry != null && entry.type == treatment.type()) {
            button = new HudButton(upper("AdvancedSurgeriesDialog.detail.remove"), false, true);
            button.addActionListener(event -> {
                plan.remove(treatment.location());
                onPlanChanged();
            });
        } else if (isBlocked(treatment)) {
            button = new HudButton(upper("AdvancedSurgeriesDialog.detail.unavailable"), true, true);
            button.setArmed(false);
        } else {
            String label = entry == null ?
                                 upper("AdvancedSurgeriesDialog.detail.add") :
                                 getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.detail.replace",
                                       entry.type.toString()).toUpperCase(Locale.ROOT);
            button = new HudButton(label, true, true);
            button.addActionListener(event -> {
                plan.put(treatment.location(), new PlanEntry(treatment.type()));
                onPlanChanged();
            });
        }
        return button;
    }

    private JComponent requirementLine(Requirement requirement) {
        Color color = switch (requirement.status()) {
            case MET -> READY;
            case NOTE, FIXABLE -> AMBER;
            case BLOCKED -> DANGER;
        };
        String mark = switch (requirement.status()) {
            case MET -> "✓";
            case NOTE, FIXABLE -> "!";
            case BLOCKED -> "✕";
        };
        // The title and explanation are separate labels, so wordWrap only ever counts plain text
        JLabel title = new JLabel(wrap(requirement.title(), MARKED_TEXT_WIDTH));
        title.setForeground(requirement.status() == RequirementStatus.MET ? TEXT : color);
        title.setFont(hudFont(Font.BOLD, 0.86f, 0.0f));
        JLabel text = new JLabel(wrap(requirement.text(), MARKED_TEXT_WIDTH));
        text.setForeground(TEXT_MUTED);
        text.setFont(hudFont(Font.PLAIN, 0.84f, 0.0f));
        JPanel content = column();
        content.add(leftAligned(title));
        content.add(leftAligned(text));
        return markedLine(mark, color, content);
    }

    private static JComponent markedLine(String mark, Color markColor, String text) {
        JLabel textLabel = new JLabel(wrap(text, MARKED_TEXT_WIDTH));
        textLabel.setForeground(TEXT);
        textLabel.setFont(hudFont(Font.PLAIN, 0.86f, 0.0f));
        return markedLine(mark, markColor, textLabel);
    }

    private static JComponent markedLine(String mark, Color markColor, JComponent content) {
        JLabel markLabel = new JLabel(mark);
        markLabel.setForeground(markColor);
        markLabel.setFont(hudFont(Font.BOLD, 0.9f, 0.0f));
        markLabel.setVerticalAlignment(SwingConstants.TOP);
        markLabel.setPreferredSize(new Dimension(scaleForGUI(14), markLabel.getPreferredSize().height));
        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(4), 0));
        line.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(1), 0, scaleForGUI(1), 0));
        line.add(markLabel, BorderLayout.WEST);
        line.add(content, BorderLayout.CENTER);
        return line;
    }

    private static JLabel tag(String text, Color color) {
        JLabel tag = new JLabel(text.toUpperCase(Locale.ROOT));
        tag.setForeground(color);
        tag.setFont(hudFont(Font.BOLD, 0.68f, 0.08f));
        tag.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(translucent(color, 160), scaleForGUI(1)),
              BorderFactory.createEmptyBorder(0, scaleForGUI(4), 0, scaleForGUI(4))));
        return tag;
    }

    private String describeTier(ProstheticType type) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.tier", type.getProstheticType(),
              type.getSurgeryLevel());
    }

    // endregion Catalogue rows

    // region Dock

    /** Rebuilds the dock: today's plan, its risk of death, and the totals and confirm action. */
    private void rebuildDock() {
        dock.removeAll();
        boolean isEmpty = todayPreviews.isEmpty();
        boolean isLethal = !isEmpty && todayRisk.isLethal();
        Color riskColor = isEmpty ? BORDER : isLethal ? DANGER : READY;
        dock.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(2), 0, 0, 0, riskColor),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        // Label Begin before the sections are measured, so its row has a height
        refreshActions();

        // GridBagLayout drops every column to its minimum width once their preferred widths don't fit, so the plan
        // may shrink as far as it needs to, while the risk and actions keep the width their text was wrapped to
        dock.add(widthLocked(buildPlanSection(), -1, scaleForGUI(PLAN_MINIMUM_WIDTH)), constraints);

        constraints.weightx = 0;
        constraints.insets = new Insets(0, scaleForGUI(14), 0, 0);
        dock.add(widthLocked(buildRiskSection(isEmpty, isLethal), scaleForGUI(RISK_WIDTH), scaleForGUI(RISK_WIDTH)),
              constraints);
        dock.add(widthLocked(buildActionSection(isLethal), scaleForGUI(ACTION_WIDTH), scaleForGUI(ACTION_WIDTH)),
              constraints);

        dock.revalidate();
        dock.repaint();
    }

    /**
     * Holds a dock section at a set width, while its height always follows its content.
     *
     * @param content        the section
     * @param preferredWidth the width it should have, or -1 for its own preferred width
     * @param minimumWidth   the narrowest it may become
     *
     * @return the holder
     */
    private static JComponent widthLocked(JComponent content, int preferredWidth, int minimumWidth) {
        JPanel holder = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension natural = super.getPreferredSize();
                return new Dimension(preferredWidth < 0 ? natural.width : preferredWidth, natural.height);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(minimumWidth, super.getPreferredSize().height);
            }
        };
        holder.setOpaque(false);
        holder.add(content, BorderLayout.CENTER);
        return holder;
    }

    private JComponent buildPlanSection() {
        JPanel section = column();
        String count = todayPreviews.isEmpty() ? "" :
                             getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.separator") + todayPreviews.size();
        section.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.plan") + count)));
        section.add(Box.createVerticalStrut(scaleForGUI(6)));

        JPanel chips = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, 0, scaleForGUI(4)));
        if (todayPreviews.isEmpty()) {
            chips.add(Hud.hint(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.plan.empty")));
        }
        for (SurgeryPreview preview : todayPreviews) {
            chips.add(planChip(preview));
            chips.add(Box.createHorizontalStrut(scaleForGUI(6)));
        }
        section.add(leftAligned(chips));

        List<Map.Entry<BodyLocation, PlanEntry>> later = plan.entrySet()
                                                               .stream()
                                                               .filter(entry -> entry.getValue().isDeferred)
                                                               .toList();
        if (!later.isEmpty()) {
            JPanel laterRow = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(4), scaleForGUI(2)));
            JLabel laterLabel = Hud.hint(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.later"));
            laterLabel.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.later.hint",
                  patient.getGivenName()));
            laterRow.add(laterLabel);
            for (Map.Entry<BodyLocation, PlanEntry> entry : later) {
                HudButton restore = new HudButton(getFormattedTextAt(RESOURCE_BUNDLE,
                      "AdvancedSurgeriesDialog.later.restore", entry.getValue().type.toString()), false, true);
                restore.setToolTipText(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.later.restore.toolTipText"));
                restore.addActionListener(event -> {
                    entry.getValue().isDeferred = false;
                    onPlanChanged();
                });
                laterRow.add(restore);
            }
            section.add(Box.createVerticalStrut(scaleForGUI(4)));
            section.add(leftAligned(laterRow));
        }
        return section;
    }

    /** One of today's surgeries: where, what, its effect and odds, and buttons to set it aside or remove it. */
    private JComponent planChip(SurgeryPreview preview) {
        PlannedSurgery surgery = preview.surgery();
        JLabel text = new JLabel("<html><font color='" + hex(ACCENT_BRIGHT) + "'>" +
                                       surgery.location().locationName().toUpperCase(Locale.ROOT) + "</font><br><b>" +
                                       surgery.type() + "</b></html>");
        text.setForeground(TEXT);
        text.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));
        String effect = joined(preview.hitsCleared() > 0 ?
                                     formatHitChange(-preview.hitsCleared()) :
                                     getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.plan.clearsNothing"),
              formatChance(preview.successChance()));
        JLabel effectLabel = new JLabel(effect);
        effectLabel.setForeground(preview.hitsCleared() > 0 ? READY : TEXT_MUTED);
        effectLabel.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
        effectLabel.setToolTipText(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.plan.effect.toolTipText"));

        HudButton defer = new HudButton(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.plan.defer"), false, true);
        defer.setToolTipText(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.plan.defer.toolTipText"));
        defer.addActionListener(event -> {
            plan.get(surgery.location()).isDeferred = true;
            onPlanChanged();
        });
        HudButton remove = new HudButton("✕", false, true);
        remove.setToolTipText(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.plan.remove"));
        remove.addActionListener(event -> {
            plan.remove(surgery.location());
            onPlanChanged();
        });

        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(6), 0));
        chip.setOpaque(true);
        chip.setBackground(SURFACE);
        chip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(4), scaleForGUI(3), scaleForGUI(2))));
        chip.add(text);
        chip.add(effectLabel);
        chip.add(defer);
        chip.add(remove);
        return chip;
    }

    /** The risk of death: the chance, what it means, and the possible outcomes. */
    private JComponent buildRiskSection(boolean isEmpty, boolean isLethal) {
        String name = patient.getGivenName();
        int startingHits = patient.getTotalInjurySeverity();
        Color color = isEmpty ? TEXT_MUTED : isLethal ? DANGER : READY;

        JPanel section = column();
        section.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, DIVIDER),
              BorderFactory.createEmptyBorder(0, scaleForGUI(14), 0, 0)));

        JLabel big = new JLabel(isEmpty ? "—" : isLethal ? formatChance(todayRisk.deathChance()) : "0%");
        big.setForeground(color);
        big.setFont(hudFont(Font.BOLD, 1.7f, 0.0f));
        String headlineKey = isEmpty ? "AdvancedSurgeriesDialog.risk.headline.empty" :
                                   isLethal ? "AdvancedSurgeriesDialog.risk.headline.lethal" :
                                         "AdvancedSurgeriesDialog.risk.headline.safe";
        JLabel headline = new JLabel(wrap(getFormattedTextAt(RESOURCE_BUNDLE, headlineKey, name), RISK_WIDTH - 110));
        headline.setForeground(TEXT);
        headline.setFont(hudFont(Font.PLAIN, 0.86f, 0.0f));
        JPanel bigRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        bigRow.add(big, BorderLayout.WEST);
        bigRow.add(headline, BorderLayout.CENTER);
        section.add(leftAligned(bigRow));
        section.add(Box.createVerticalStrut(scaleForGUI(4)));

        String detail;
        if (isEmpty) {
            detail = getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.risk.detail.empty", name,
                  startingHits, DEATH);
        } else if (isLethal) {
            Integer failures = todayRisk.fewestFatalFailures();
            if (failures == null || failures == 0) {
                detail = getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.risk.detail.always", name);
            } else if (failures == 1) {
                detail = getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.risk.detail.one", name);
            } else {
                detail = getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.risk.detail.many", name,
                      failures);
            }
        } else {
            detail = getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.risk.detail.safe", name,
                  todayRisk.worstCaseHits(), DEATH, todayRisk.bestCaseHits());
        }
        JLabel detailLabel = new JLabel(wrap(detail, RISK_WIDTH - 30));
        detailLabel.setForeground(TEXT_MUTED);
        detailLabel.setFont(hudFont(Font.PLAIN, 0.84f, 0.0f));
        detailLabel.setToolTipText(wrap(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.risk.rule",
              DEATH), 300));
        section.add(leftAligned(detailLabel));
        section.add(Box.createVerticalStrut(scaleForGUI(6)));

        section.add(leftAligned(meterRow(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.risk.now"),
              startingHits, AMBER)));
        if (!isEmpty) {
            section.add(Box.createVerticalStrut(scaleForGUI(3)));
            section.add(leftAligned(meterRow(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.risk.best"),
                  todayRisk.bestCaseHits(), READY)));
            section.add(Box.createVerticalStrut(scaleForGUI(3)));
            section.add(leftAligned(meterRow(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.risk.worst"),
                  todayRisk.worstCaseHits(), isLethal ? DANGER : AMBER)));
        }
        return section;
    }

    private JComponent meterRow(String label, int hits, Color color) {
        JLabel name = Hud.hint(label);
        name.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
        name.setPreferredSize(new Dimension(scaleForGUI(80), name.getPreferredSize().height));
        JLabel value = new JLabel(hits >= DEATH ?
                                        getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.risk.dead",
                                              hits) :
                                        hits + "/" + DEATH);
        value.setForeground(hits >= DEATH ? DANGER : TEXT);
        value.setFont(hudFont(Font.BOLD, 0.8f, 0.0f));
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        value.setPreferredSize(new Dimension(scaleForGUI(56), value.getPreferredSize().height));
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(6), 0));
        row.add(name, BorderLayout.WEST);
        row.add(new HitMeter(hits, color), BorderLayout.CENTER);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    /** The totals, the way to make a lethal plan safe, the risk acknowledgement, and Cancel and Begin. */
    private JComponent buildActionSection(boolean isLethal) {
        JPanel section = column();
        section.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, DIVIDER),
              BorderFactory.createEmptyBorder(0, scaleForGUI(14), 0, 0)));

        List<PlannedSurgery> today = getTodaySurgeries();
        int longest = today.stream().mapToInt(this::getRecoveryDays).max().orElse(0);
        String totals = today.isEmpty() ?
                              getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.total.none") :
                              getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.total",
                                    todayCost.toAmountString(), longest);
        JLabel totalsLabel = new JLabel(totals);
        totalsLabel.setForeground(TEXT);
        totalsLabel.setFont(hudFont(Font.PLAIN, 0.86f, 0.0f));
        if (!today.isEmpty()) {
            totalsLabel.setToolTipText(wrap(describeOperatingSurgeon(), 300));
        }
        section.add(leftAligned(totalsLabel));
        if (!today.isEmpty() && isUseLocalSurgeon(getTodaySurgeryLevel())) {
            JLabel local = Hud.hint(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.total.local",
                  LOCAL_SURGEON_COST_MULTIPLIER));
            local.setForeground(AMBER);
            local.setToolTipText(wrap(describeOperatingSurgeon(), 300));
            section.add(leftAligned(local));
        }
        if (!today.isEmpty() && !campaign.getPlayerForce().getFunds().isGreaterOrEqualThan(todayCost)) {
            JLabel funds = Hud.hint(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.status.funds"));
            funds.setForeground(DANGER);
            section.add(leftAligned(funds));
        }
        section.add(Box.createVerticalStrut(scaleForGUI(6)));

        if (isLethal) {
            int safeCount = SurgeryRiskCalculator.getSafeSurgeryCount(patient.getTotalInjurySeverity(),
                  getMostHelpfulFirst().stream().map(SurgeryPreview::toOdds).toList());
            if (safeCount == 0) {
                section.add(leftAligned(wrappedHint(getFormattedTextAt(RESOURCE_BUNDLE,
                      "AdvancedSurgeriesDialog.risk.heal", patient.getGivenName()), ACTION_WIDTH - 30)));
            } else {
                int toMove = todayPreviews.size() - safeCount;
                HudButton makeSafe = new HudButton(getFormattedTextAt(RESOURCE_BUNDLE,
                      "AdvancedSurgeriesDialog.risk.makeSafe", toMove).toUpperCase(Locale.ROOT), true, true);
                makeSafe.setToolTipText(wrap(getTextAt(RESOURCE_BUNDLE,
                      "AdvancedSurgeriesDialog.risk.makeSafe.toolTipText"), 300));
                makeSafe.addActionListener(event -> makeSafe(safeCount));
                section.add(leftAligned(makeSafe));
            }
            section.add(Box.createVerticalStrut(scaleForGUI(4)));
            section.add(leftAligned(acknowledgeRisk));
        }
        section.add(Box.createVerticalStrut(scaleForGUI(6)));

        // Begin's label carries the risk, so it gets a row of its own rather than being pushed out beside Cancel
        section.add(leftAligned(beginButton));
        section.add(Box.createVerticalStrut(scaleForGUI(6)));
        HudButton cancel = new HudButton(upper("AdvancedSurgeriesDialog.button.cancel"), false, true);
        cancel.addActionListener(event -> dispose());
        section.add(leftAligned(cancel));
        return section;
    }

    /** Arms Begin (and the GM skip) according to the plan, the funds, and the risk acknowledgement. */
    private void refreshActions() {
        int count = todayPreviews.size();
        boolean isLethal = count > 0 && todayRisk.isLethal();
        boolean canAfford = campaign.getPlayerForce().getFunds().isGreaterOrEqualThan(todayCost);

        if (count == 0) {
            beginButton.setText(upper("AdvancedSurgeriesDialog.button.confirm"));
        } else if (isLethal) {
            beginButton.setText(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.button.confirm.risk",
                  formatChance(todayRisk.deathChance())).toUpperCase(Locale.ROOT));
        } else {
            beginButton.setText(getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.button.confirm.count",
                  count).toUpperCase(Locale.ROOT));
        }
        beginButton.setArmed(count > 0 && canAfford && (!isLethal || acknowledgeRisk.isSelected()));
        if (gmSkipButton != null) {
            gmSkipButton.setArmed(count > 0);
        }
    }

    /** @return today's surgeries, those that clear the most injuries first */
    private List<SurgeryPreview> getMostHelpfulFirst() {
        List<SurgeryPreview> mostHelpfulFirst = new ArrayList<>(todayPreviews);
        mostHelpfulFirst.sort(Comparator.comparingInt(SurgeryPreview::hitsCleared).reversed());
        return mostHelpfulFirst;
    }

    /**
     * Keeps as many of today's surgeries as can be performed without any outcome killing the patient, favouring those
     * that clear the most injuries, and sets the rest aside for later.
     */
    private void makeSafe(int safeCount) {
        List<SurgeryPreview> mostHelpfulFirst = getMostHelpfulFirst();
        for (int index = safeCount; index < mostHelpfulFirst.size(); index++) {
            plan.get(mostHelpfulFirst.get(index).surgery().location()).isDeferred = true;
        }
        onPlanChanged();
    }

    // endregion Dock

    // region Body map

    /** @return the catalogue's body locations that sit within a part of the body map */
    private static Set<BodyLocation> getLocationsIn(BodyPart part) {
        Set<BodyLocation> locations = new LinkedHashSet<>();
        for (BodyLocation location : VALID_BODY_LOCATIONS) {
            if (part.contains(location)) {
                locations.add(location);
            }
        }
        return locations;
    }

    private void onBodyPartChosen(BodyPart part) {
        setLocationFilter(getLocationsIn(part), part.getLabel());
    }

    /** Colours the body map: cyan where surgery is planned today, amber where the patient is injured. */
    private void refreshBodyMap() {
        List<PlannedSurgery> today = getTodaySurgeries();
        Map<BodyPart, Color> fills = new EnumMap<>(BodyPart.class);
        Set<BodyPart> missing = EnumSet.noneOf(BodyPart.class);
        for (BodyPart part : BodyPart.values()) {
            boolean isPlanned = today.stream().anyMatch(surgery -> part.contains(surgery.location()));
            boolean isInjured = patient.getInjuries()
                                      .stream()
                                      .filter(injury -> !injury.getSubType().isPermanentModification())
                                      .anyMatch(injury -> part.contains(injury.getLocation()));
            if (isPlanned) {
                fills.put(part, translucent(ACCENT, 130));
            } else if (isInjured) {
                fills.put(part, translucent(AMBER, 150));
            }
            if (patient.isLocationMissing(part.getMainLocation())) {
                missing.add(part);
            }
        }
        bodyMap.setFills(fills, missing);
    }

    // endregion Body map

    // region Catalogue data

    /**
     * Builds every procedure the patient could have, at every body location it can be performed at, in display order.
     */
    private void gatherCatalogue() {
        for (BodyLocation location : VALID_BODY_LOCATIONS) {
            List<ProstheticType> eligible = new ArrayList<>(getEligibleTreatments(location));
            eligible.sort(Comparator.comparingInt(ProstheticType::getProstheticType)
                                .thenComparing(ProstheticType::toString));
            for (ProstheticType type : eligible) {
                catalogue.add(new Treatment(type, location));
            }
        }
    }

    /**
     * Determines which prosthetic types are eligible for a given body location based on their configured allowed
     * locations.
     *
     * @param injuryLocation the location being evaluated
     *
     * @return a list of prosthetic types that can treat the location
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static List<ProstheticType> getEligibleTreatments(BodyLocation injuryLocation) {
        List<ProstheticType> eligibleTreatments = new ArrayList<>();
        for (ProstheticType type : ProstheticType.values()) {
            if (type.getEligibleLocations().contains(injuryLocation)) {
                eligibleTreatments.add(type);
            }
        }
        return eligibleTreatments;
    }

    /**
     * @return the injuries a successful surgery would remove from the patient, as they stand while the dialog is open
     */
    private List<Injury> getInjuriesClearedBy(Treatment treatment) {
        return clearedInjuryCache.computeIfAbsent(treatment,
              key -> findInjuriesClearedBy(key.type(), key.location()));
    }

    /**
     * Works out the injuries a successful surgery would remove, from the patient's injuries as they are now. Surgery
     * calls this directly, as each surgery changes the injuries the next one finds.
     *
     * @return the injuries a successful surgery of this type at this location removes from the patient: for an
     *       implant, any identical or mutually exclusive implant it replaces; otherwise the injuries at that location
     *       (only burns, for surgeries that only remove burns)
     */
    private List<Injury> findInjuriesClearedBy(ProstheticType type, BodyLocation location) {
        InjuryType surgeryInjuryType = type.getInjuryType();
        InjurySubType subType = surgeryInjuryType.getSubType();

        // Implants remove any other instances of the same implant, otherwise no removal occurs. Non-implants remove
        // all relevant injuries.
        if (subType.isImplant()) { // Includes VDNI implants and wings
            List<Injury> replaced = new ArrayList<>();
            for (Injury injury : patient.getInjuries()) {
                InjurySubType injurySubType = injury.getSubType();
                // We only remove implants if we're adding an identical implant, or another of a mutually exclusive
                // kind (VDNI implants, wings)
                if (injurySubType.isImplant() &&
                          ((subType.isMutuallyExclusiveImplant() && injurySubType == subType) ||
                                 (injury.getType() == surgeryInjuryType))) {
                    replaced.add(injury);
                }
            }
            return replaced;
        }

        boolean isBurnRemovalOnly = type.isBurnRemoveOnly();
        List<Injury> cleared = new ArrayList<>();
        for (Injury injury : relevantInjuries.getOrDefault(location, List.of())) {
            if (injury != null && (!isBurnRemovalOnly || injury.getSubType().isBurn())) {
                cleared.add(injury);
            }
        }
        return cleared;
    }

    /** @return the injuries (as opposed to existing prosthetics) a procedure would fix */
    private List<Injury> getTreatedInjuries(Treatment treatment) {
        return getInjuriesClearedBy(treatment)
                     .stream()
                     .filter(injury -> !injury.getSubType().isPermanentModification())
                     .toList();
    }

    private int getHitsCleared(Treatment treatment) {
        return getInjuriesClearedBy(treatment)
                     .stream()
                     .mapToInt(Injury::getHits)
                     .sum();
    }

    /**
     * Works out whether the patient can have a procedure, and why not. This method evaluates all restriction
     * layers - faction access, era availability, planetary tech, hospital facilities, personal prohibitions, and the
     * surgeon's skill.
     *
     * @return the checklist, in display order
     */
    private List<Requirement> getRequirements(Treatment treatment) {
        return requirementCache.computeIfAbsent(treatment, this::buildRequirements);
    }

    private List<Requirement> buildRequirements(Treatment treatment) {
        ProstheticType type = treatment.type();
        Faction faction = getFaction();
        LocalDate today = campaign.getLocalDate();
        int year = campaign.getGameYear();
        boolean isOnPlanet = currentLocation.isOnPlanet();
        boolean needsFacilities = type.getProstheticType() > 2;
        List<Requirement> requirements = new ArrayList<>();

        // Faction access
        if (type.isAvailableToFaction(faction, today)) {
            requirements.add(requirement(RequirementStatus.MET, "faction",
                  getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement.faction.met",
                        faction.getFullName(year))));
        } else {
            String key = type.isClanOnly() ? "clan" : type.isComStarOnly() ? "comstar" :
                                                            type.isWordOfBlakeOnly() ? "wob" : "generic";
            requirements.add(requirement(RequirementStatus.BLOCKED, "faction",
                  getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement.faction." + key)));
        }

        // Era
        if (getCost(type) != null) {
            requirements.add(requirement(RequirementStatus.MET, "year",
                  getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement.year.met", year)));
        } else {
            requirements.add(requirement(RequirementStatus.FIXABLE, "year",
                  getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement.year.unmet", year)));
        }

        // Planetary tech
        String needed = type.getRequiredPlanetarySophistication().getName();
        String here = describeLocalSophistication();
        boolean isTechAvailable = type.isAvailableInCurrentLocation(currentLocation, today);
        requirements.add(requirement(isTechAvailable ? RequirementStatus.MET : RequirementStatus.FIXABLE, "tech",
              getFormattedTextAt(RESOURCE_BUNDLE, isTechAvailable ?
                                                        "AdvancedSurgeriesDialog.requirement.tech.met" :
                                                        "AdvancedSurgeriesDialog.requirement.tech.unmet",
                    needed, here)));

        // Hospital facilities
        if (!needsFacilities) {
            requirements.add(requirement(RequirementStatus.MET, "facilities",
                  getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement.facilities.notNeeded")));
        } else if (isOnPlanet) {
            requirements.add(requirement(RequirementStatus.MET, "facilities",
                  getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement.facilities.met")));
        } else {
            requirements.add(requirement(RequirementStatus.FIXABLE, "facilities",
                  getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement.facilities.unmet")));
        }

        // Patient consent
        boolean hasHatredOfBionics = patient.getOptions().booleanOption(COMPULSION_BIONIC_HATE);
        if (hasHatredOfBionics && needsFacilities) {
            requirements.add(requirement(RequirementStatus.BLOCKED, "consent",
                  getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement.consent.unmet",
                        patient.getGivenName())));
        } else {
            requirements.add(requirement(RequirementStatus.MET, "consent",
                  getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement.consent.met",
                        patient.getGivenName())));
        }

        // Surgeon
        int level = type.getSurgeryLevel();
        if (!isUseLocalSurgeon(level)) {
            requirements.add(requirement(RequirementStatus.MET, "surgeon",
                  getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement.surgeon.met", level,
                        surgeon.getFullName(), getSurgeryLevel(surgeon))));
        } else {
            requirements.add(requirement(RequirementStatus.NOTE, "surgeon",
                  getFormattedTextAt(RESOURCE_BUNDLE, isOnPlanet ?
                                                            "AdvancedSurgeriesDialog.requirement.surgeon.local" :
                                                            "AdvancedSurgeriesDialog.requirement.surgeon.transit",
                        level, LOCAL_SURGEON_COST_MULTIPLIER)));
        }
        return requirements;
    }

    private static Requirement requirement(RequirementStatus status, String key, String text) {
        return new Requirement(status, getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.requirement." + key),
              text);
    }

    /** @return the least-met requirement, ignoring requirements entirely in GM Mode */
    private RequirementStatus getWorstRequirement(Treatment treatment) {
        if (isGMMode) {
            return RequirementStatus.MET;
        }
        RequirementStatus worst = RequirementStatus.MET;
        for (Requirement requirement : getRequirements(treatment)) {
            if (requirement.status().ordinal() > worst.ordinal()) {
                worst = requirement.status();
            }
        }
        return worst;
    }

    /** @return {@code true} if the patient can't have the procedure right now; never, in GM Mode */
    private boolean isBlocked(Treatment treatment) {
        return getWorstRequirement(treatment).isBlocking();
    }

    private String describeLocalSophistication() {
        if (!currentLocation.isOnPlanet()) {
            return getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.inTransit");
        }
        Planet planet = currentLocation.getPlanet();
        PlanetarySophistication sophistication = planet.getSocioIndustrial(campaign.getLocalDate()).tech;
        return joined(planet.getName(campaign.getLocalDate()),
              sophistication == null ? PlanetarySophistication.F.getName() : sophistication.getName());
    }

    private String describeWhereabouts() {
        return joined(patient.getPrimaryRoleDesc(), describeLocalSophistication(),
              MekHQ.getMHQOptions().getDisplayFormattedDate(campaign.getLocalDate()));
    }

    // endregion Catalogue data

    // region Plan data

    /** @return today's surgeries (those not set aside), ordered so the most important ones are rolled first */
    private List<PlannedSurgery> getTodaySurgeries() {
        return getTodaySurgeries(null);
    }

    /**
     * @param candidate a surgery to put in today's plan in place of whatever is planned at its location, as adding it
     *                  would, or {@code null} for the plan as it is
     *
     * @return today's surgeries (those not set aside), ordered so the most important ones are rolled first
     */
    private List<PlannedSurgery> getTodaySurgeries(@Nullable PlannedSurgery candidate) {
        List<PlannedSurgery> surgeries = new ArrayList<>();
        boolean isCandidatePlaced = false;
        for (Map.Entry<BodyLocation, PlanEntry> entry : plan.entrySet()) {
            // Adding a surgery where one is already planned replaces it in place, so the candidate takes its turn
            if (candidate != null && entry.getKey() == candidate.location()) {
                surgeries.add(candidate);
                isCandidatePlaced = true;
            } else if (!entry.getValue().isDeferred) {
                surgeries.add(new PlannedSurgery(entry.getValue().type, entry.getKey()));
            }
        }
        if (candidate != null && !isCandidatePlaced) {
            surgeries.add(candidate);
        }
        // Prioritize surgeries based on difficulty and expense. This is so that any available Edge is used on the
        // more important surgeries first.
        surgeries.sort(this::compareCosts);
        return surgeries;
    }

    private static int getHighestSurgeryLevel(List<PlannedSurgery> surgeries) {
        return surgeries.stream().mapToInt(surgery -> surgery.type().getSurgeryLevel()).max().orElse(0);
    }

    private int getTodaySurgeryLevel() {
        int level = 0;
        for (PlanEntry entry : plan.values()) {
            if (!entry.isDeferred) {
                level = Math.max(level, entry.type.getSurgeryLevel());
            }
        }
        return level;
    }

    /**
     * Works out what each of today's surgeries will do. Each injury is credited to the first surgery that would clear
     * it, and Edge re-rolls are credited to the first surgeries rolled, so the risk shown is never lower than the real
     * risk.
     */
    private List<SurgeryPreview> previewToday() {
        return preview(getTodaySurgeries());
    }

    /**
     * Works out what each surgery in a plan will do, as {@link #previewToday()} does for today's plan.
     *
     * @param surgeries the plan's surgeries, in the order they are rolled
     *
     * @return what each surgery will do, in the same order
     */
    private List<SurgeryPreview> preview(List<PlannedSurgery> surgeries) {
        if (surgeries.isEmpty()) {
            return List.of();
        }
        Person operatingSurgeon = getOperatingSurgeon(getHighestSurgeryLevel(surgeries));
        int edge = isUseEdge(operatingSurgeon) ? operatingSurgeon.getCurrentEdge() : 0;
        Set<Injury> claimed = Collections.newSetFromMap(new IdentityHashMap<>());

        List<SurgeryPreview> previews = new ArrayList<>();
        for (PlannedSurgery surgery : surgeries) {
            int hitsCleared = 0;
            for (Injury injury : getInjuriesClearedBy(new Treatment(surgery.type(), surgery.location()))) {
                if (claimed.add(injury)) {
                    hitsCleared += injury.getHits();
                }
            }
            double chance = CheckOdds.chance(getCheckTarget(operatingSurgeon, surgery.type()), edge > 0);
            edge = Math.max(0, edge - 1);
            previews.add(new SurgeryPreview(surgery, hitsCleared, chance));
        }
        return previews;
    }

    /**
     * @return the chance of a procedure succeeding if it were in today's plan: the same surgeon, surgery level and
     *       share of Edge the dock would show once it is added
     */
    private double getPlannedSuccessChance(Treatment treatment) {
        PlannedSurgery candidate = new PlannedSurgery(treatment.type(), treatment.location());
        return preview(getTodaySurgeries(candidate)).stream()
                     .filter(preview -> preview.surgery().equals(candidate))
                     .mapToDouble(SurgeryPreview::successChance)
                     .findFirst()
                     .orElse(0);
    }

    private Money getCost(List<PlannedSurgery> surgeries) {
        Money total = Money.zero();
        for (PlannedSurgery surgery : surgeries) {
            Money cost = getCost(surgery.type());
            if (cost != null) {
                total = total.plus(cost);
            }
        }
        if (!surgeries.isEmpty() && isUseLocalSurgeon(getTodaySurgeryLevel())) {
            total = total.multipliedBy(LOCAL_SURGEON_COST_MULTIPLIER);
        }
        return total;
    }

    /** @return the days a successful surgery keeps the patient out of action, from the recovery injury it leaves */
    private int getRecoveryDays(PlannedSurgery surgery) {
        double multiplier = campaignOptions.get(CampaignOption.ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER);
        return surgery.type().getRecoveryDays(surgery.location(), multiplier);
    }

    /**
     * @return the procedure's cost to the campaign's faction this year, or {@code null} if it can't be bought this
     *       year
     */
    private @Nullable Money getCost(ProstheticType type) {
        return costCache.computeIfAbsent(type,
              key -> Optional.ofNullable(key.getCost(getFaction(), campaign.getGameYear()))).orElse(null);
    }

    private int compareCosts(PlannedSurgery s1, PlannedSurgery s2) {
        int surgeryLevelComparison = Integer.compare(s2.type().getSurgeryLevel(), s1.type().getSurgeryLevel());
        if (surgeryLevelComparison != 0) {
            return surgeryLevelComparison;
        }

        // Cost can be null when the prosthetic isn't available this year, which GM Mode allows to be selected
        Money cost1 = Objects.requireNonNullElse(getCost(s1.type()), Money.zero());
        Money cost2 = Objects.requireNonNullElse(getCost(s2.type()), Money.zero());
        return cost2.compareTo(cost1);
    }

    private Faction getFaction() {
        return campaign.getPlayerForce().getFaction();
    }

    // endregion Plan data

    // region Surgeons

    /**
     * Determines the senior surgeon at the given location, based on doctor status and rank/skill tiebreakers.
     *
     * @param currentLocation the current location
     *
     * @return the most senior surgeon, or {@code null} if none are available
     *
     * @author Illiani
     * @since 0.50.10
     */
    private @Nullable Person getSurgeon(AbstractLocation currentLocation) {
        Person seniorSurgeon = null;

        for (Person person : currentLocation.fetchPersonnelAtLocation()) {
            if (person.isActive() && person.isDoctor()) {
                if (seniorSurgeon == null) {
                    seniorSurgeon = person;
                    continue;
                }

                if (person.outRanksUsingSkillTiebreaker(campaignOptions,
                      campaign.getPlayerForce().isClanForce(),
                      campaign.getLocalDate(),
                      seniorSurgeon)) {
                    seniorSurgeon = person;
                }
            }
        }

        return seniorSurgeon;
    }

    private static int getSurgeryLevel(Person person) {
        Skill skill = person.getSkill(S_SURGERY);
        return skill == null ? -1 : skill.getTotalSkillLevel(person.getSkillModifierData());
    }

    /** @return {@code true} if the campaign's surgeon can't perform surgery of this level, so a local is hired */
    private boolean isUseLocalSurgeon(int surgeryLevelNeeded) {
        return surgeon == null || getSurgeryLevel(surgeon) < surgeryLevelNeeded;
    }

    /**
     * @return the surgeon who will operate at this level: the campaign's surgeon if they are skilled enough, otherwise
     *       a temporary local surgeon
     */
    private Person getOperatingSurgeon(int surgeryLevelNeeded) {
        if (!isUseLocalSurgeon(surgeryLevelNeeded)) {
            return surgeon;
        }
        return localSurgeons.computeIfAbsent(surgeryLevelNeeded, level -> {
            Person localSurgeon = new Person(campaign);
            localSurgeon.addSkill(S_SURGERY, level + 1, 0);
            localSurgeon.setEdge(1); // We don't want Local Surgeons to be completely ineffective
            localSurgeon.setCurrentEdge(1);
            return localSurgeon;
        });
    }

    private boolean isUseEdge(Person operatingSurgeon) {
        return campaignOptions.get(CampaignOption.USE_EDGE) &&
                     operatingSurgeon.getOptions().booleanOption(EDGE_ADVANCED_SURGERY);
    }

    private static int getMachinistModifier(Person operatingSurgeon, ProstheticType type) {
        boolean hasMachinistSPA = operatingSurgeon.getOptions().booleanOption(UNOFFICIAL_BIOLOGICAL_MACHINIST);
        return type != COSMETIC_SURGERY && hasMachinistSPA ? MACHINIST_MODIFIER : 0;
    }

    /** @return the number the surgeon must roll for a surgery of this type, as the real check works it out */
    private CheckTarget getCheckTarget(Person operatingSurgeon, @Nullable ProstheticType type) {
        int modifier = type == null ? 0 : getMachinistModifier(operatingSurgeon, type);
        return operatingSurgeon.checkSkill(S_SURGERY, campaign).withMiscModifier(modifier).getCheckTarget();
    }

    private String describeOperatingSurgeon() {
        int level = getTodaySurgeryLevel();
        if (!isUseLocalSurgeon(level)) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.plan.surgeon", surgeon.getFullName(),
                  getSurgeryLevel(surgeon), level);
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, currentLocation.isOnPlanet() ?
                                                         "AdvancedSurgeriesDialog.plan.surgeon.local" :
                                                         "AdvancedSurgeriesDialog.plan.surgeon.transit", level,
              LOCAL_SURGEON_COST_MULTIPLIER);
    }

    // endregion Surgeons

    // region Guide

    /**
     * Builds the guide drawer, which floats over the dialog above a scrim; Escape or a click on the scrim closes it.
     */
    private void buildGuide() {
        AdvancedSurgeriesGuidePreferences preferences = AdvancedSurgeriesGuidePreferences.getInstance();
        HudCheckBox alwaysOpen = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.guide.alwaysOpen"));
        alwaysOpen.setSelected(preferences.isGuideAlwaysOpen());
        alwaysOpen.addActionListener(event -> preferences.setGuideAlwaysOpen(alwaysOpen.isSelected()));
        guideDrawer = new HudGuideDrawer(List.of(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.guide.tab.steps"),
              getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.guide.tab.example"),
              getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.guide.tab.terms")), alwaysOpen,
              getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.guide.close"), this::closeGuide);
        guideDrawer.install(getRootPane());
    }

    /** Opens the guide if the player hasn't seen it yet, or has asked to see it every time. */
    private void maybeOpenGuide() {
        AdvancedSurgeriesGuidePreferences preferences = AdvancedSurgeriesGuidePreferences.getInstance();
        if (!preferences.isGuideSeen() || preferences.isGuideAlwaysOpen()) {
            openGuide();
        }
    }

    private void openGuide() {
        AdvancedSurgeriesGuidePreferences.getInstance().setGuideSeen(true);
        guideDrawer.open(createGuide());
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    /**
     * Builds the guide from the resource bundle. The steps, example lines and terms are numbered keys
     * ({@code AdvancedSurgeriesDialog.guide.step.1.title}, {@code .step.2.title}, ...), read until the next number is
     * missing, so text can be added or reworded without code changes.
     *
     * @return the guide
     */
    private Guide createGuide() {
        String prefix = "AdvancedSurgeriesDialog.guide.";

        List<Step> steps = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(guideText(prefix + "step." + number + ".title")); number++) {
            steps.add(new Step(guideText(prefix + "step." + number + ".title"),
                  guideText(prefix + "step." + number + ".text")));
        }
        List<ExampleLine> example = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(guideText(prefix + "example." + number + ".who")); number++) {
            example.add(new ExampleLine(guideText(prefix + "example." + number + ".who"),
                  guideText(prefix + "example." + number + ".text")));
        }
        List<Term> terms = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(guideText(prefix + "term." + number + ".name")); number++) {
            terms.add(new Term(guideText(prefix + "term." + number + ".name"),
                  guideText(prefix + "term." + number + ".text")));
        }

        JPanel glossary = column();
        glossary.add(leftAligned(Hud.eyebrow(guideText(prefix + "glossary"))));
        glossary.add(Box.createVerticalStrut(scaleForGUI(4)));
        glossary.add(leftAligned(Hud.link(GlossaryEntry.PROSTHETICS.getTitle(), this::openDocumentation)));

        return new Guide(guideText(prefix + "eyebrow"), guideText(prefix + "title"), steps, null,
              guideText(prefix + "exampleIntro"), example, terms, glossary);
    }

    private static String guideText(String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    // endregion Guide

    // region Actions

    /**
     * Opens the glossary entry describing prosthetics, from the guide's glossary link.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void openDocumentation() {
        GlossaryEntry glossaryEntry = GlossaryEntry.PROSTHETICS;

        try {
            new GlossaryEntryDialog(this, glossaryEntry);
        } catch (Exception ex) {
            LOGGER.error("Failed to open Glossary Entry", ex);
        }
    }

    /**
     * Handles the confirm action. This method disposes the dialog, charges the campaign, determines the surgeon,
     * performs surgery skill checks, applies surgery and recovery injuries, and reports results back to the campaign
     * log. Surgeries set aside for later are not performed.
     *
     * @param event the originating action event
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void onConfirm(ActionEvent event) {
        List<PlannedSurgery> prioritizedSurgeries = getTodaySurgeries();
        if (prioritizedSurgeries.isEmpty()) {
            return;
        }
        dispose();

        // Pay for everything
        payForSurgeries();

        // Get a local surgeon (if applicable)
        Person operatingSurgeon = getOperatingSurgeon(getTodaySurgeryLevel());

        // Then perform the surgery skill checks
        List<PlannedSurgery> successfulSurgeries = new ArrayList<>();
        List<PlannedSurgery> unsuccessfulSurgeries = new ArrayList<>();
        performSurgerySkillChecks(operatingSurgeon, prioritizedSurgeries, successfulSurgeries, unsuccessfulSurgeries);

        // Then perform the actual surgeries
        for (PlannedSurgery surgery : prioritizedSurgeries) {
            performSurgery(surgery, successfulSurgeries);
        }

        // Notify the player of the results
        if (!successfulSurgeries.isEmpty()) {
            campaign.addReport(MEDICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "AdvancedSurgeriesDialog.report.successful",
                  spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG,
                  String.join(", ", successfulSurgeries.stream().map(PlannedSurgery::getLabel).toList())));
        }

        if (!unsuccessfulSurgeries.isEmpty()) {
            campaign.addReport(MEDICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "AdvancedSurgeriesDialog.report.unsuccessful",
                  spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG,
                  String.join(", ", unsuccessfulSurgeries.stream().map(PlannedSurgery::getLabel).toList())));
        }

        // Check to see if the patient died on the operating table
        checkForDeath();
    }

    /**
     * Handles the GM confirm action by applying today's planned surgeries without performing any rolls or charging the
     * campaign. This is intended for manual GM adjudication.
     *
     * <p>For each planned surgery this method removes applicable injuries, records a marker injury, and grants any
     * associated implants and abilities before notifying the campaign that the patient has been updated.</p>
     *
     * @param event the originating action event
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void onGMConfirm(ActionEvent event) {
        if (!campaign.isGM()) {
            return;
        }
        dispose();

        for (PlannedSurgery surgery : getTodaySurgeries()) {
            processOldInjuryRemoval(surgery.type(), surgery.location());
            addMarkerInjury(surgery.type(), surgery.location());
            addImplantsAndAbilities(surgery.type());
        }

        campaign.getPlayerForce().getHumanResources().personUpdated(campaign, patient);
    }

    /**
     * Debits the campaign finances for the total cost of today's surgeries.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void payForSurgeries() {
        Finances finances = campaign.getPlayerForce().getFinances();
        finances.debit(TransactionType.MEDICAL_EXPENSES, campaign.getLocalDate(), todayCost,
              getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.transaction", patient.getFullTitle()));
    }

    /**
     * Applies the outcome of a single planned surgery. On success, removes applicable injuries, adds a record injury,
     * and adds recovery injuries. On failure, adds a failed-surgery recovery injury.
     *
     * @param surgery             the surgery being performed
     * @param successfulSurgeries the list of surgeries that passed their skill checks
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void performSurgery(PlannedSurgery surgery, List<PlannedSurgery> successfulSurgeries) {
        BodyLocation location = surgery.location();
        ProstheticType type = surgery.type();

        if (successfulSurgeries.contains(surgery)) {
            // Remove injuries based on the surgery type
            processOldInjuryRemoval(type, location);

            // Add the new surgery 'injury'. This is a semi-permanent record of the surgery on the character
            addMarkerInjury(type, location);

            // Add recovery period injuries
            InjuryType recoveryInjuryType = type.getRecoveryInjuryType(location);
            Injury recoveryInjury = recoveryInjuryType.newInjury(campaign, patient, GENERIC, 1);
            patient.addInjury(recoveryInjury);

            addImplantsAndAbilities(type);
        } else {
            // Add failed surgery injury
            Injury recoveryInjury = FAILED_SURGERY_RECOVERY.newInjury(campaign, patient, GENERIC, 1);
            patient.addInjury(recoveryInjury);
        }

        campaign.getPlayerForce().getHumanResources().personUpdated(campaign, patient);
    }

    /**
     * Grants any pilot and personnel abilities associated with the given prosthetic type, honoring the campaign options
     * that govern implants and SPAs.
     *
     * @param type the prosthetic type being applied to the patient
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void addImplantsAndAbilities(ProstheticType type) {
        if (campaignOptions.get(CampaignOption.USE_IMPLANTS)) {
            for (String implant : type.getAssociatedPilotOptions()) {
                patient.getOptions().acquireAbility(MD_ADVANTAGES, implant, true);
            }
        }

        if (campaignOptions.get(CampaignOption.USE_ABILITIES)) {
            for (String option : type.getAssociatedPersonnelOptions()) {
                patient.getOptions().acquireAbility(LVL3_ADVANTAGES, option, true);
            }
        }
    }

    /**
     * Adds a zero-severity marker injury representing the installed prosthetic at the specified body location. This
     * serves as a semi-permanent record of the surgery.
     *
     * @param type     the prosthetic type that was installed
     * @param location the body location that was treated
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void addMarkerInjury(ProstheticType type, BodyLocation location) {
        InjuryType injuryType = type.getInjuryType();
        Injury injury = injuryType.newInjury(campaign, patient, location, 0);
        patient.addInjury(injury);
    }

    /**
     * Removes existing injuries at the given location that are resolved by the specified prosthetic type.
     *
     * @param type     the prosthetic type being applied
     * @param location the body location being treated
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void processOldInjuryRemoval(ProstheticType type, BodyLocation location) {
        LocalDate today = campaign.getLocalDate();
        List<Injury> removed = AdvancedMedicalAlternate.getInjuriesRemovedBySurgery(type, patient.getInjuries(),
              relevantInjuries.getOrDefault(location, new ArrayList<>()));
        for (Injury injury : removed) {
            patient.removeInjury(injury, today);
        }
    }

    /**
     * Determines whether accumulated injury hits after surgery results in the patient's death (medical complications)
     * and updates their status accordingly.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void checkForDeath() {
        if (patient.getTotalInjurySeverity() >= DEATH) {
            patient.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MEDICAL_COMPLICATIONS);
        }
    }

    /**
     * Performs surgery skill checks for each planned surgery and populates the lists of successful and unsuccessful
     * surgeries. Each check generates a report entry in the campaign log.
     *
     * @param operatingSurgeon      the surgeon performing the surgeries
     * @param prioritizedSurgeries  the surgeries ordered by importance
     * @param successfulSurgeries   out parameter for successful surgeries
     * @param unsuccessfulSurgeries out parameter for unsuccessful surgeries
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void performSurgerySkillChecks(Person operatingSurgeon, List<PlannedSurgery> prioritizedSurgeries,
          List<PlannedSurgery> successfulSurgeries, List<PlannedSurgery> unsuccessfulSurgeries) {
        boolean isUseEdge = isUseEdge(operatingSurgeon);

        for (PlannedSurgery surgery : prioritizedSurgeries) {
            ActionCheckResult actionCheckResult =
                  operatingSurgeon.checkSkill(S_SURGERY, campaign)
                        .withMiscModifier(getMachinistModifier(operatingSurgeon, surgery.type()))
                        .resolve(isUseEdge, getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.skillCheck"));
            campaign.addReport(SKILL_CHECKS, actionCheckResult.getReport());
            if (actionCheckResult.isSuccess()) {
                successfulSurgeries.add(surgery);
                MedicalLogger.successfulSurgery(patient, campaign.getLocalDate(), surgery.type().toString());
            } else {
                unsuccessfulSurgeries.add(surgery);
                MedicalLogger.failedSurgery(patient, campaign.getLocalDate(), surgery.type().toString());
            }
        }
    }

    // endregion Actions

    // region Injury mapping

    /**
     * Populates the map of injuries relevant to replacement surgery, by the body location a surgery would clear them
     * from, to drive the catalogue.
     *
     * @param injuries the list of injuries on the patient
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void gatherRelevantInjuries(List<Injury> injuries) {
        injuries.sort(Comparator.comparing(Injury::getName)); // consistent order
        for (Injury injury : injuries) {
            BodyLocation location = injury.getLocation();
            for (BodyLocation mappedLocation : BodyLocation.values()) {
                if (VALID_BODY_LOCATIONS.contains(mappedLocation)) {
                    boolean isSameLocation = location.equals(mappedLocation);
                    boolean childOf = location.isChildOf(mappedLocation);

                    // The chest is a special case, as 'prosthetics' used there won't remove all injuries in that
                    // location, just the localized ones
                    boolean locationIsChest = mappedLocation.equals(CHEST);
                    boolean isHandOrFoot = mappedLocation == LEFT_HAND || mappedLocation == RIGHT_HAND
                                                 || mappedLocation == LEFT_FOOT || mappedLocation == RIGHT_FOOT;
                    boolean isBeyondHandOrFoot = isHandOrFoot && LOCATIONS_BEYOND_HAND_AND_FOOT.contains(location);
                    if (isSameLocation || (childOf && !locationIsChest && !isBeyondHandOrFoot)) {
                        // If a BodyLocation is the child of multiple valid locations, we want it added to each, so we
                        // don't break after finding one match
                        relevantInjuries.computeIfAbsent(mappedLocation, k -> new ArrayList<>()).add(injury);
                    }
                }
            }
        }
    }

    // endregion Injury mapping

    // region Helpers

    private static JPanel column() {
        JPanel column = Hud.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        return column;
    }

    /**
     * Wraps content in a row that acts like a button: it highlights on hover, and fires on click or on Space or
     * Enter.
     */
    private static JPanel clickable(JComponent content, Runnable onChoose) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(GROUND);
        row.setBorder(rowBorder(DIVIDER));
        row.setFocusable(true);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.add(content, BorderLayout.CENTER);
        // Labels inside the row have no mouse listeners (so no tooltips either), so their clicks reach the row
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (row.contains(event.getPoint())) {
                    row.requestFocusInWindow();
                    onChoose.run();
                }
            }
        };
        row.addMouseListener(mouse);
        row.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_ENTER) {
                    onChoose.run();
                }
            }
        });
        return row;
    }

    private static javax.swing.border.Border rowBorder(Color color) {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(8), scaleForGUI(6), scaleForGUI(8)));
    }

    private static JLabel divider() {
        JLabel divider = new JLabel("|");
        divider.setForeground(BORDER);
        return divider;
    }

    private static JComponent legendLine(Color color, String text) {
        JLabel label = Hud.hint(text);
        label.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
        label.setIcon(new SquareIcon(color));
        label.setIconTextGap(scaleForGUI(6));
        return label;
    }

    private static JLabel wrappedHint(String text, int width) {
        return Hud.hint(wrap(text, width));
    }

    /**
     * @param actionWidth the width of the button beside the details
     *
     * @return the width, in screen pixels, an expanded procedure's details can fill beside its button
     */
    private int getDetailsWidth(int actionWidth) {
        if (catalogueWidth <= 0) {
            // Not laid out yet; the resize listener re-wraps once the catalogue has a width
            return scaleForGUI(FULL_TEXT_WIDTH);
        }
        int available = catalogueWidth - scaleForGUI(DETAILS_HORIZONTAL_CHROME + DETAILS_GAP) - actionWidth;
        return Math.max(scaleForGUI(FULL_TEXT_WIDTH / 2), available);
    }

    /**
     * Wraps text to a width with {@code wordWrap}, as HTML for a label.
     *
     * @param text  the text; may contain simple markup and {@code <br>} line breaks
     * @param width the width to wrap to, before GUI scaling
     *
     * @return the wrapped text
     */
    private static String wrap(String text, int width) {
        return wrapToPixels(text, scaleForGUI(width), hudFont(Font.PLAIN, WRAP_FONT_SIZE, 0.0f));
    }

    /**
     * Wraps text to a width in screen pixels with {@code wordWrap}. The characters that fit on a line are worked out
     * from the font's bold width, so a line never runs past the edge; markup also counts towards each line, so lines
     * holding markup end a little early.
     *
     * @param text   the text; may contain simple markup and {@code <br>} line breaks
     * @param pixels the width to wrap to
     * @param font   the font the text is drawn in
     *
     * @return the wrapped text, as HTML for a label
     */
    private static String wrapToPixels(String text, int pixels, Font font) {
        double averageCharacterWidth = AVERAGE_CHARACTER_WIDTHS.computeIfAbsent(font, key -> {
            FontMetrics metrics = new JLabel().getFontMetrics(key.deriveFont(Font.BOLD));
            return metrics.stringWidth(CHARACTER_WIDTH_SAMPLE) / (double) CHARACTER_WIDTH_SAMPLE.length();
        });
        int characters = Math.max(MINIMUM_WRAP_CHARACTERS,
              (int) (pixels / Math.max(1.0, averageCharacterWidth)));
        // wordWrap breaks lines at spaces, so spaces inside tags are hidden from it and put back afterwards
        String guarded = MARKUP_TAG.matcher(text)
                               .replaceAll(tag -> Matcher.quoteReplacement(tag.group().replace(" ", TAG_SPACE)));
        String wrapped = wordWrap(guarded, characters).replace(TAG_SPACE, " ");
        // wordWrap ends every line with a break, which would leave an empty line at the bottom
        String trailingBreak = "<br></html>";
        return wrapped.endsWith(trailingBreak) ?
                     wrapped.substring(0, wrapped.length() - trailingBreak.length()) + "</html>" :
                     wrapped;
    }

    private static String joined(String... parts) {
        return String.join(getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.separator"), parts);
    }

    private static String upper(String key) {
        return getTextAt(RESOURCE_BUNDLE, key).toUpperCase(Locale.ROOT);
    }

    private static String formatChance(double chance) {
        if (chance > 0 && chance < 0.005) {
            return "<1%";
        }
        if (chance < 1 && chance > 0.995) {
            return ">99%";
        }
        return Math.round(chance * 100) + "%";
    }

    private static String formatHitChange(int change) {
        String sign = change > 0 ? "+" : change < 0 ? "−" : "±";
        return getFormattedTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.hitChange", sign, Math.abs(change));
    }

    /** Wraps a column in a vertical-only scroll pane whose content always fits the pane's width. */
    private static JScrollPane scroll(JComponent content, Color background) {
        JPanel holder = new WidthTrackingPanel();
        holder.setOpaque(true);
        holder.setBackground(background);
        holder.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Hud.styleScroll(scroll, background, false);
        return scroll;
    }

    /** A scroll pane's content that always fits the pane's width, so only vertical scrolling is ever needed. */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        private WidthTrackingPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return scaleForGUI(16);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** A small filled square, for injury markers and legends. */
    private record SquareIcon(Color color) implements Icon {
        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            graphics.setColor(color);
            graphics.fillRect(x, y + scaleForGUI(3), getIconWidth(), getIconWidth());
        }

        @Override
        public int getIconWidth() {
            return scaleForGUI(9);
        }

        @Override
        public int getIconHeight() {
            return scaleForGUI(12);
        }
    }

    /** A small filled circle, for a procedure's status. */
    private record DotIcon(Color color) implements Icon {
        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                int size = getIconWidth();
                g2.fillOval(x, y + (getIconHeight() - size) / 2, size, size);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return scaleForGUI(8);
        }

        @Override
        public int getIconHeight() {
            return scaleForGUI(12);
        }
    }

    /**
     * The parts of the body map. Each is drawn in a 160 x 300 space, facing the viewer, so the patient's left side is
     * on the right.
     */
    private enum BodyPart {
        HEAD(BodyLocation.HEAD, Set.of(BodyLocation.HEAD), new Ellipse2D.Double(58, 12, 44, 44)),
        TORSO(BodyLocation.CHEST, Set.of(BodyLocation.CHEST, BodyLocation.ABDOMEN),
              new RoundRectangle2D.Double(50, 66, 60, 86, 28, 28)),
        RIGHT_ARM(BodyLocation.RIGHT_ARM, Set.of(BodyLocation.RIGHT_ARM, BodyLocation.RIGHT_HAND),
              new RoundRectangle2D.Double(28, 70, 18, 100, 18, 18)),
        LEFT_ARM(BodyLocation.LEFT_ARM, Set.of(BodyLocation.LEFT_ARM, BodyLocation.LEFT_HAND),
              new RoundRectangle2D.Double(114, 70, 18, 100, 18, 18)),
        RIGHT_LEG(BodyLocation.RIGHT_LEG, Set.of(BodyLocation.RIGHT_LEG, BodyLocation.RIGHT_FOOT),
              new RoundRectangle2D.Double(56, 156, 20, 126, 20, 20)),
        LEFT_LEG(BodyLocation.LEFT_LEG, Set.of(BodyLocation.LEFT_LEG, BodyLocation.LEFT_FOOT),
              new RoundRectangle2D.Double(84, 156, 20, 126, 20, 20));

        private final BodyLocation mainLocation;
        private final Set<BodyLocation> primaryLocations;
        private final Shape shape;

        BodyPart(BodyLocation mainLocation, Set<BodyLocation> primaryLocations, Shape shape) {
            this.mainLocation = mainLocation;
            this.primaryLocations = primaryLocations;
            this.shape = shape;
        }

        /** @return the location that is missing when this whole part is gone */
        private BodyLocation getMainLocation() {
            return mainLocation;
        }

        /** @return {@code true} if a body location sits within this part */
        private boolean contains(BodyLocation location) {
            BodyLocation primary = location.getPrimaryLocation();
            return primary != null && primaryLocations.contains(primary);
        }

        private String getLabel() {
            return this == TORSO ?
                         getTextAt(RESOURCE_BUNDLE, "AdvancedSurgeriesDialog.bodyMap.torso") :
                         Utilities.capitalize(mainLocation.locationName());
        }
    }

    /**
     * A small, clickable outline of the body: each part is filled to show injuries or planned surgery, and clicking
     * one narrows the catalogue to it.
     */
    private static final class BodyMap extends JComponent {
        private static final double VIEW_WIDTH = 160;
        private static final double VIEW_HEIGHT = 300;

        private final transient Consumer<BodyPart> onChoose;
        private Map<BodyPart, Color> fills = Map.of();
        private Set<BodyPart> missing = Set.of();
        private @Nullable BodyPart hovered;
        private @Nullable BodyPart selected;

        private BodyMap(Consumer<BodyPart> onChoose) {
            this.onChoose = onChoose;
            setOpaque(false);
            // Registers with the tooltip manager, so each part can name itself on hover
            setToolTipText("");
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    BodyPart part = partAt(event.getX(), event.getY());
                    if (part != hovered) {
                        hovered = part;
                        setCursor(part == null ? Cursor.getDefaultCursor() :
                                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = null;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    BodyPart part = partAt(event.getX(), event.getY());
                    if (part != null) {
                        onChoose.accept(part);
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        /**
         * @param fills   each part's fill; parts without one are drawn plain
         * @param missing parts the patient has lost, drawn as a dashed outline
         */
        private void setFills(Map<BodyPart, Color> fills, Set<BodyPart> missing) {
            this.fills = fills;
            this.missing = missing;
            repaint();
        }

        /** @param selected the part the catalogue is narrowed to, outlined in the accent colour */
        private void setSelected(@Nullable BodyPart selected) {
            this.selected = selected;
            repaint();
        }

        /** @return the transform from the 160 x 300 drawing space to this component, keeping its proportions */
        private AffineTransform getViewTransform() {
            double scale = Math.min(getWidth() / VIEW_WIDTH, getHeight() / VIEW_HEIGHT);
            AffineTransform transform = new AffineTransform();
            transform.translate((getWidth() - VIEW_WIDTH * scale) / 2, (getHeight() - VIEW_HEIGHT * scale) / 2);
            transform.scale(scale, scale);
            return transform;
        }

        private @Nullable BodyPart partAt(int x, int y) {
            AffineTransform transform = getViewTransform();
            for (BodyPart part : BodyPart.values()) {
                if (transform.createTransformedShape(part.shape).contains(x, y)) {
                    return part;
                }
            }
            return null;
        }

        @Override
        public @Nullable String getToolTipText(MouseEvent event) {
            BodyPart part = partAt(event.getX(), event.getY());
            return part == null ? null : part.getLabel();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                AffineTransform transform = getViewTransform();
                float strokeWidth = scaleForGUI(1.5f);
                for (BodyPart part : BodyPart.values()) {
                    Shape shape = transform.createTransformedShape(part.shape);
                    boolean isHighlighted = part == selected || part == hovered;
                    if (missing.contains(part)) {
                        g2.setColor(isHighlighted ? ACCENT_BRIGHT : TEXT_FAINT);
                        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f,
                              new float[] { 3f * strokeWidth, 3f * strokeWidth }, 0f));
                        g2.draw(shape);
                        continue;
                    }
                    g2.setColor(fills.getOrDefault(part, SURFACE_HIGHLIGHT));
                    g2.fill(shape);
                    g2.setColor(isHighlighted ? ACCENT_BRIGHT : BORDER);
                    g2.setStroke(new BasicStroke(strokeWidth));
                    g2.draw(shape);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** A row of cells, one per hit up to the fatal count; the last cell is outlined in red. */
    private static final class HitMeter extends JComponent {
        private final int hits;
        private final Color color;

        private HitMeter(int hits, Color color) {
            this.hits = hits;
            this.color = color;
            setPreferredSize(new Dimension(scaleForGUI(110), scaleForGUI(10)));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int gap = scaleForGUI(2);
                int cellHeight = scaleForGUI(8);
                int top = (getHeight() - cellHeight) / 2;
                int cellWidth = Math.max(1, (getWidth() - gap * (DEATH - 1)) / DEATH);
                for (int cell = 0; cell < DEATH; cell++) {
                    int x = cell * (cellWidth + gap);
                    g2.setColor(cell < hits ? color : SURFACE_HIGHLIGHT);
                    g2.fillRect(x, top, cellWidth, cellHeight);
                    if (cell == DEATH - 1) {
                        g2.setColor(DANGER);
                        g2.drawRect(x, top, cellWidth - 1, cellHeight - 1);
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // endregion Helpers

    /**
     * Forces the preferences for this dialog to be tracked in MekHQ instead of MegaMek, allowing its position and size
     * to be remembered between runs.
     *
     * @param dialog the dialog whose preferences are being managed
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void setPreferences(JDialog dialog) {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(AdvancedSurgeriesDialog.class);
            dialog.setName("AdvancedSurgeriesDialog");
            preferences.manage(new JWindowPreference(dialog));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
