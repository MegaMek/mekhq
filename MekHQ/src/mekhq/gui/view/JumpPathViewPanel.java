/*
 * Copyright (C) 2009-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import static java.lang.Math.ceil;
import static java.text.MessageFormat.format;
import static megamek.client.ui.util.FontHandler.symbolIcon;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import megamek.client.ui.util.UIUtil;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.JumpPathItinerary;
import mekhq.campaign.JumpPathItinerary.CircuitMode;
import mekhq.campaign.JumpPathItinerary.CircuitPlan;
import mekhq.campaign.JumpPathItinerary.Plan;
import mekhq.campaign.JumpPathItinerary.RequiredAcceleration;
import mekhq.campaign.JumpPathItinerary.TimelineEntry;
import mekhq.campaign.JumpPathSchedule;
import mekhq.campaign.JumpPathSchedule.Dwell;
import mekhq.campaign.JumpPathSchedule.Mode;
import mekhq.campaign.JumpPathSchedule.Result;
import mekhq.campaign.NavigationRouteAnalysis;
import mekhq.campaign.NavigationRouteAnalysis.LegAssessment;
import mekhq.campaign.NavigationRouteAnalysis.PathAssessment;
import mekhq.campaign.NavigationRouteAnalysis.Severity;
import mekhq.campaign.PiratePointAnalysis;
import mekhq.campaign.PiratePointAnalysis.DifficultyFacts;
import mekhq.campaign.PiratePointAnalysis.Facts;
import mekhq.campaign.PiratePointAnalysis.Input;
import mekhq.campaign.PiratePointAnalysis.Modifier;
import mekhq.campaign.PiratePointAnalysis.ModifierCategory;
import mekhq.campaign.RouteAlternativesPlanner.AccessStatus;
import mekhq.campaign.RouteAlternativesPlanner.CircuitCoverage;
import mekhq.campaign.RouteAlternativesPlanner.Course;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.utilities.TransportCostCalculations;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.gui.baseComponents.FramedCommandButton;
import mekhq.gui.baseComponents.FramedCommandButtonStyle.ButtonColors;
import mekhq.gui.baseComponents.FramedCommandButtonStyle.ButtonStateColors;
import mekhq.gui.baseComponents.ImmersiveCheckBox;
import mekhq.gui.baseComponents.ImmersiveComboBox;
import mekhq.gui.baseComponents.ImmersiveSpinner;
import mekhq.gui.baseComponents.JScrollablePanel;

/**
 * A custom panel that gets filled in with goodies from a JumpPath record
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class JumpPathViewPanel extends JScrollablePanel {
    private static final Color DOSSIER_BACKGROUND = new Color(7, 16, 27);
    private static final Color DOSSIER_TEXT = new Color(218, 231, 235);
    private static final Color DOSSIER_MUTED_TEXT = new Color(132, 153, 161);
    private static final Color DOSSIER_ACCENT = new Color(65, 210, 224);
    private static final Color DOSSIER_ACTIVE = new Color(235, 166, 66);
    private static final Color DOSSIER_DIVIDER = new Color(35, 66, 82);
    private static final Color DOSSIER_CONTROL_BACKGROUND = new Color(12, 29, 42);
    private static final Color DOSSIER_CONTROL_ACTIVE = new Color(18, 45, 56);
        private static final ButtonColors COURSE_BUTTON_COLORS = new ButtonColors(
            new ButtonStateColors(DOSSIER_CONTROL_BACKGROUND, DOSSIER_TEXT, DOSSIER_DIVIDER),
            new ButtonStateColors(DOSSIER_CONTROL_ACTIVE, DOSSIER_ACCENT, DOSSIER_ACCENT),
            new ButtonStateColors(DOSSIER_DIVIDER, DOSSIER_TEXT, DOSSIER_ACCENT),
            new ButtonStateColors(DOSSIER_CONTROL_BACKGROUND.darker(), DOSSIER_MUTED_TEXT, DOSSIER_DIVIDER.darker()));
    private static final int HORIZONTAL_PADDING = UIUtil.scaleForGUI(14);
        private static final int COURSE_BUTTON_WIDTH = UIUtil.scaleForGUI(112);
        private static final int COURSE_BUTTON_HEIGHT = UIUtil.scaleForGUI(34);
    private static final int WIDE_PLANNER_CONTROL_WIDTH = UIUtil.scaleForGUI(190);
    private static final int DENSE_SELECTOR_WIDTH = UIUtil.scaleForGUI(142);
    private static final int PIRATE_SELECTOR_WIDTH = UIUtil.scaleForGUI(200);
    private static final int DENSE_SPINNER_WIDTH = UIUtil.scaleForGUI(88);
    private static final double MINIMUM_ACCELERATION_G = 0.1;
    private static final double MAXIMUM_ACCELERATION_G = 10.0;
    private static final double ACCELERATION_STEP_G = 0.1;
    private static final double MINIMUM_DESIRED_DAYS = 0.1;
    private static final double MAXIMUM_DESIRED_DAYS = 100000.0;
    private static final int MAXIMUM_DWELL_HOURS = 24000;
    private static final double MILLION_KILOMETERS = 1_000_000.0;
    private static final double MAXIMUM_ASSUMED_DISTANCE_MILLIONS_KM = 1_000_000.0;
    private static final int DEFAULT_MANUAL_TARGET_NUMBER = 8;
    private static final int MINIMUM_TARGET_NUMBER = -100;
    private static final int MAXIMUM_TARGET_NUMBER = 100;
    private static final int MINIMUM_ASSUMED_MODIFIER = -100;
    private static final int MAXIMUM_ASSUMED_MODIFIER = 100;

    private final JumpPath path;
    private final Campaign campaign;
    private final Locale locale;
    private final ResourceBundle resourceMap;
    private final List<Course> routeCourses;
    private final List<PlanetarySystem> requestedStops;
    private final List<Integer> dwellHoursByRequestedStop;
    private final Consumer<Course> courseSelectionHandler;
    private final Runnable jumpFeeSummaryHandler;
    private final Runnable cancelCurrentTripHandler;
    private final boolean advancedPlanningInitiallyExpanded;
    private final Consumer<Boolean> advancedPlanningStateHandler;
    private Plan itineraryPlan;
    private Result schedule;
    private Mode scheduleMode;
    private LocalDateTime earliestFeasibleDeparture;
    private LocalDateTime departureAnchor;
    private LocalDateTime arrivalDeadline;
    private CircuitPlan circuitPlan;
    private PathAssessment navigationAssessment;
    private CircuitPlan customCircuitPlan = CircuitPlan.custom(Set.of());
    private JLabel startingTransitValue;
    private JLabel endingTransitValue;
    private JLabel rechargeValue;
    private JLabel totalTimeValue;
    private JLabel requiredAccelerationValue;
    private JLabel scheduleAnchorLabel;
    private JLabel scheduleAnchorValue;
    private JLabel scheduleDepartureValue;
    private JLabel scheduleArrivalValue;
    private JLabel scheduleDwellValue;
    private JLabel scheduleStatusValue;
    private JPanel itinerarySection;
    private JPanel circuitDepartures;
    private JSpinner accelerationSpinner;
    private JSpinner desiredDurationSpinner;
    private JSpinner scheduleAnchorSpinner;
    private boolean adjustingScheduleAnchor;
    private JComboBox<String> piratePointModeSelector;
    private JComboBox<NavigationSource> navigationSourceSelector;
    private JSpinner piratePointDistanceSpinner;
    private JSpinner detectionRadiusSpinner;
    private JSpinner piratePointTargetSpinner;
    private int manualPiratePointTarget = DEFAULT_MANUAL_TARGET_NUMBER;
    private boolean adjustingPiratePointTarget;
    private final List<ModifierControl> piratePointModifierControls = new ArrayList<>();
    private final List<JLabel> piratePointControlLabels = new ArrayList<>();
    private final List<JLabel> piratePointResultLabels = new ArrayList<>();
    private ApproachLabels standardPointLabels;
    private ApproachLabels piratePointLabels;
    private JLabel piratePointTargetValue;
    private JLabel piratePointOddsValue;
    private JLabel piratePointTransitDifferenceLabel;
    private JLabel piratePointTransitDifferenceValue;
    private JLabel piratePointAdjustedArrivalValue;

    public JumpPathViewPanel(JumpPath p, Campaign c) {
        this(p, c, List.of(), List.of(), course -> { }, null, null, false, expanded -> { });
    }

    public JumpPathViewPanel(JumpPath p, Campaign c, List<Course> routeCourses,
          Consumer<Course> courseSelectionHandler) {
                this(p, c, routeCourses, List.of(), courseSelectionHandler, null, null, false, expanded -> { });
    }

    public JumpPathViewPanel(JumpPath p, Campaign c, List<Course> routeCourses,
          List<PlanetarySystem> requestedStops, Consumer<Course> courseSelectionHandler) {
          this(p, c, routeCourses, requestedStops, courseSelectionHandler, null, null, false, expanded -> { });
        }

        public JumpPathViewPanel(JumpPath p, Campaign c, List<Course> routeCourses,
            List<PlanetarySystem> requestedStops, Consumer<Course> courseSelectionHandler,
            Runnable jumpFeeSummaryHandler, Runnable cancelCurrentTripHandler,
            boolean advancedPlanningInitiallyExpanded,
            Consumer<Boolean> advancedPlanningStateHandler) {
        super();
        this.path = p;
        this.campaign = c;
        this.routeCourses = List.copyOf(routeCourses);
        this.requestedStops = List.copyOf(requestedStops);
        dwellHoursByRequestedStop = new ArrayList<>(requestedStops.size());
        requestedStops.forEach(stop -> dwellHoursByRequestedStop.add(0));
        this.courseSelectionHandler = courseSelectionHandler;
        this.jumpFeeSummaryHandler = jumpFeeSummaryHandler;
        this.cancelCurrentTripHandler = cancelCurrentTripHandler;
        this.advancedPlanningInitiallyExpanded = advancedPlanningInitiallyExpanded;
        this.advancedPlanningStateHandler = advancedPlanningStateHandler;
        locale = MekHQ.getMHQOptions().getLocale();
        resourceMap = ResourceBundle.getBundle("mekhq.resources.JumpPathViewPanel", locale);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(DOSSIER_BACKGROUND);
        setOpaque(true);

        circuitPlan = initialCircuitPlan(path, routeCourses, isUseCommandCircuit());
        itineraryPlan = calculatePlan(JumpPathItinerary.DEFAULT_ACCELERATION_G);
        earliestFeasibleDeparture = campaign.getLocalDate().atStartOfDay();
        departureAnchor = earliestFeasibleDeparture;
        scheduleMode = Mode.DEPART_AT;
        schedule = calculateSchedule();
        arrivalDeadline = schedule.arrival();
        navigationAssessment = calculateNavigationAssessment();
        add(createHeader());
        add(createSummary());
        if (!isActiveRoute()) {
            if (routeCourses.size() > 1) {
                add(createCourseComparison());
            }
        }
        JPanel advancedPlanning = createAdvancedPlanning();
        itinerarySection = createItinerary();
        add(itinerarySection);
        add(advancedPlanning);
    }

    private JPanel createHeader() {
        JPanel header = createBandPanel();
        header.setLayout(new GridBagLayout());
        header.setBorder(BorderFactory.createEmptyBorder(12, HORIZONTAL_PADDING, 10, HORIZONTAL_PADDING));

        boolean activeRoute = isActiveRoute();
        Color routeColor = activeRoute ? DOSSIER_ACTIVE : DOSSIER_ACCENT;

        JLabel eyebrow = new JLabel(resourceMap.getString("dossier.eyebrow.text"));
        eyebrow.setForeground(routeColor);
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, eyebrow.getFont().getSize2D() * 0.85f));
        GridBagConstraints constraints = createFullWidthConstraints(0);
        constraints.insets = new Insets(0, 0, 3, 0);
        header.add(eyebrow, constraints);

        LocalDate currentDate = campaign.getLocalDate();
        String startName = getSystemName(path.getFirstSystem(), currentDate);
        String endName = getSystemName(path.getLastSystem(), currentDate);
        JLabel endpoints = new JLabel(format(resourceMap.getString("dossier.route.format"), startName, endName));
        endpoints.setForeground(DOSSIER_MUTED_TEXT);
          constraints = createFullWidthConstraints(1);
        header.add(endpoints, constraints);
        return header;
    }

    private JPanel createSummary() {
        JPanel summary = createSection("section.summary.text");

        int metricIndex = 0;
        addMetric(summary, metricIndex++, "metric.jumps.text", Integer.toString(path.getJumps()));
        startingTransitValue = addMetric(summary, metricIndex++, "metric.startTransit.text",
              formatDays(itineraryPlan.startingTransitDays()));
        endingTransitValue = addMetric(summary, metricIndex++, "metric.endTransit.text",
              formatDays(itineraryPlan.endingTransitDays()));
          rechargeValue = addMetric(summary, metricIndex++, "metric.recharge.text",
              formatDays(itineraryPlan.rechargeDays()));
        totalTimeValue = addMetric(summary, metricIndex++, "metric.totalTime.text",
              formatDays(itineraryPlan.totalDays()));

        if (campaign.getCampaignOptions().get(CampaignOption.PAY_FOR_TRANSPORT)) {
            TransportCostCalculations calculations = campaign.getTransportCostCalculation(EXP_REGULAR);
            int duration = (int) ceil(itineraryPlan.totalDays());
            Money journeyCost = calculations.calculateJumpCostForEntireJourney(duration, path.getJumps());
            addMetric(summary, metricIndex, "metric.cost.text", journeyCost.toAmountAndSymbolString());
        }
        if (jumpFeeSummaryHandler != null) {
            FramedCommandButton jumpFees = new FramedCommandButton(
                  resourceMap.getString("summary.jumpFees.text"), COURSE_BUTTON_COLORS);
            jumpFees.setMargin(new Insets(UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(10),
                  UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(10)));
            jumpFees.setToolTipText(resourceMap.getString("summary.jumpFees.tooltip"));
            jumpFees.getAccessibleContext().setAccessibleDescription(jumpFees.getToolTipText());
            jumpFees.addActionListener(event -> jumpFeeSummaryHandler.run());
            GridBagConstraints constraints = createFullWidthConstraints(1 + ((metricIndex + 1) / 2));
            constraints.gridwidth = 2;
            constraints.insets = new Insets(5, 0, 1, 0);
            summary.add(jumpFees, constraints);
        }
        if (isActiveRoute() && (cancelCurrentTripHandler != null)) {
            FramedCommandButton cancelTrip = new FramedCommandButton(
                  resourceMap.getString("summary.cancelTrip.text"), COURSE_BUTTON_COLORS);
            cancelTrip.setMargin(new Insets(UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(10),
                  UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(10)));
            cancelTrip.setToolTipText(resourceMap.getString("summary.cancelTrip.tooltip"));
            cancelTrip.getAccessibleContext().setAccessibleDescription(cancelTrip.getToolTipText());
            cancelTrip.addActionListener(event -> cancelCurrentTripHandler.run());
            GridBagConstraints constraints = createFullWidthConstraints(2 + ((metricIndex + 1) / 2));
            constraints.gridwidth = 2;
            constraints.insets = new Insets(5, 0, 1, 0);
            summary.add(cancelTrip, constraints);
        }
        return summary;
    }

    private JPanel createAdvancedPlanning() {
        JPanel advanced = createBandPanel();
        advanced.setLayout(new GridBagLayout());
        advanced.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DOSSIER_DIVIDER));

        JToggleButton toggle = new JToggleButton(resourceMap.getString("advanced.show.text"));
          toggle.setSelected(advancedPlanningInitiallyExpanded);
          toggle.setText(resourceMap.getString(advancedPlanningInitiallyExpanded
              ? "advanced.hide.text"
              : "advanced.show.text"));
          toggle.setIcon(symbolIcon(advancedPlanningInitiallyExpanded ? 0xE5CF : 0xE5CC,
              UIUtil.scaleForGUI(18), DOSSIER_ACCENT));
        toggle.setBackground(DOSSIER_CONTROL_BACKGROUND);
        toggle.setForeground(DOSSIER_ACCENT);
        toggle.setFocusPainted(false);
        toggle.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(10), HORIZONTAL_PADDING,
              UIUtil.scaleForGUI(10), HORIZONTAL_PADDING));
        toggle.setToolTipText(resourceMap.getString("advanced.tooltip"));
        toggle.getAccessibleContext().setAccessibleDescription(toggle.getToolTipText());
        GridBagConstraints constraints = createFullWidthConstraints(0);
        advanced.add(toggle, constraints);

        JPanel content = createBandPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        if (!isActiveRoute()) {
            content.add(createCircuitPlanner());
        }
        content.add(createAccelerationPlanner());
        content.add(createSchedulePlanner());
        content.add(createPiratePointPlanner());
        content.setVisible(advancedPlanningInitiallyExpanded);
        constraints = createFullWidthConstraints(1);
        advanced.add(content, constraints);

        toggle.addActionListener(event -> {
            boolean expanded = toggle.isSelected();
            content.setVisible(expanded);
            advancedPlanningStateHandler.accept(expanded);
            toggle.setText(resourceMap.getString(expanded ? "advanced.hide.text" : "advanced.show.text"));
            toggle.setIcon(symbolIcon(expanded ? 0xE5CF : 0xE5CC,
                UIUtil.scaleForGUI(18), DOSSIER_ACCENT));
            advanced.revalidate();
            advanced.repaint();
        });
        return advanced;
    }

    private JPanel createCourseComparison() {
        JPanel comparison = createSection("section.courses.text");
        int row = 1;
        for (Course course : routeCourses) {
            boolean selected = isCourseSelected(path, course);
            JPanel courseRow = createBandPanel();
            courseRow.setLayout(new GridBagLayout());
            if (row > 1) {
                courseRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DOSSIER_DIVIDER));
            }

            JLabel name = new JLabel(resourceMap.getString(switch (course.kind()) {
                case FASTEST -> "course.fastest.text";
                case FEWEST_JUMPS -> "course.fewestJumps.text";
                case COMMAND_CIRCUIT -> "course.commandCircuit.text";
            }));
            name.setForeground(selected ? DOSSIER_ACCENT : DOSSIER_TEXT);
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(7, 0, 1, 8);
            courseRow.add(name, constraints);

            JLabel facts = new JLabel(format(resourceMap.getString("course.facts.format"), course.jumps(),
                  formatNumber(course.oneGTotalDays(), 2),
                  resourceMap.getString(course.circuitCoverage() == CircuitCoverage.WHOLE
                                              ? "course.circuit.whole.text"
                                              : "course.circuit.none.text"),
                  resourceMap.getString(course.accessStatus() == AccessStatus.CLEAR
                                              ? "course.access.clear.text"
                                              : "course.access.blocked.text")));
            facts.setForeground(course.accessStatus() == AccessStatus.CLEAR ? DOSSIER_MUTED_TEXT : DOSSIER_ACTIVE);
            facts.setFont(facts.getFont().deriveFont(Font.PLAIN, facts.getFont().getSize2D() * 0.78f));
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(1, 0, 7, 8);
            courseRow.add(facts, constraints);

            FramedCommandButton useCourse = new FramedCommandButton(resourceMap.getString(selected
                  ? "course.selected.text"
                  : "course.use.text"), COURSE_BUTTON_COLORS);
            Dimension buttonSize = new Dimension(COURSE_BUTTON_WIDTH, COURSE_BUTTON_HEIGHT);
            useCourse.setPreferredSize(buttonSize);
            useCourse.setMinimumSize(buttonSize);
            useCourse.setMaximumSize(buttonSize);
            useCourse.setEnabled(!selected);
            useCourse.setToolTipText(resourceMap.getString(selected
                  ? "course.selected.tooltip"
                  : "course.use.tooltip"));
            useCourse.getAccessibleContext().setAccessibleDescription(useCourse.getToolTipText());
            useCourse.addActionListener(event -> courseSelectionHandler.accept(course));
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.gridheight = 2;
            constraints.anchor = GridBagConstraints.EAST;
            constraints.insets = new Insets(5, 0, 5, 0);
            courseRow.add(useCourse, constraints);

            constraints = createFullWidthConstraints(row++);
            comparison.add(courseRow, constraints);
        }
        return comparison;
    }

    private JPanel createCircuitPlanner() {
        JPanel planner = createSection("section.circuit.text");
        JComboBox<String> modeSelector = new ImmersiveComboBox<>(new String[] {
              resourceMap.getString("circuit.none.text"),
              resourceMap.getString("circuit.whole.text"),
              resourceMap.getString("circuit.custom.text")
        });
        modeSelector.setSelectedIndex(circuitPlan.mode().ordinal());
        configureComboBox(modeSelector, "circuit.mode.text", "circuit.mode.tooltip", DENSE_SELECTOR_WIDTH);

        JPanel input = createBandPanel();
        input.setLayout(new GridBagLayout());
        JLabel label = createPlannerLabel("circuit.mode.text");
        label.setLabelFor(modeSelector);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        input.add(label, constraints);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        input.add(modeSelector, constraints);
        constraints = createFullWidthConstraints(1);
        constraints.insets = new Insets(3, 0, 4, 0);
        planner.add(input, constraints);

        circuitDepartures = createBandPanel();
        circuitDepartures.setLayout(new GridBagLayout());
        constraints = createFullWidthConstraints(2);
        planner.add(circuitDepartures, constraints);
        populateCircuitDepartures();

        modeSelector.addActionListener(event -> {
            CircuitMode selectedMode = CircuitMode.values()[modeSelector.getSelectedIndex()];
            circuitPlan = switch (selectedMode) {
                case NONE -> CircuitPlan.none();
                case WHOLE -> CircuitPlan.whole();
                case CUSTOM -> customCircuitPlan;
            };
            populateCircuitDepartures();
            refreshCircuitPlan();
        });
        return planner;
    }

    private void populateCircuitDepartures() {
        circuitDepartures.removeAll();
        if (circuitPlan.mode() != CircuitMode.CUSTOM) {
            circuitDepartures.revalidate();
            circuitDepartures.repaint();
            return;
        }

        List<PlanetarySystem> systems = path.getSystems();
        if (systems.size() <= 2) {
            JLabel none = new JLabel(resourceMap.getString("circuit.noDepartures.text"));
            none.setForeground(DOSSIER_MUTED_TEXT);
            none.setFont(none.getFont().deriveFont(Font.PLAIN, none.getFont().getSize2D() * 0.78f));
            circuitDepartures.add(none, createFullWidthConstraints(0));
        } else {
            for (int index = 1; index < systems.size() - 1; index++) {
                PlanetarySystem system = systems.get(index);
                JCheckBox covered = new ImmersiveCheckBox(system.getPrintableName(campaign.getLocalDate()),
                        circuitPlan.usesCircuitAt(index));
                covered.setToolTipText(resourceMap.getString("circuit.departure.tooltip"));
                covered.getAccessibleContext().setAccessibleDescription(covered.getToolTipText());
                int departureIndex = index;
                covered.addActionListener(event -> {
                    customCircuitPlan = customCircuitPlan.withCoverage(departureIndex, covered.isSelected());
                    circuitPlan = customCircuitPlan;
                    refreshCircuitPlan();
                });
                GridBagConstraints constraints = createFullWidthConstraints(index - 1);
                constraints.insets = new Insets(1, 0, 1, 0);
                circuitDepartures.add(covered, constraints);
            }
        }
        circuitDepartures.revalidate();
        circuitDepartures.repaint();
    }

    private void refreshCircuitPlan() {
        double accelerationG = ((Number) accelerationSpinner.getValue()).doubleValue();
        itineraryPlan = calculatePlan(accelerationG);
        navigationAssessment = calculateNavigationAssessment();
        refreshPlanPresentation();
        updateRequiredAcceleration(((Number) desiredDurationSpinner.getValue()).doubleValue());
    }

    private JPanel createAccelerationPlanner() {
        JPanel planner = createSection("section.acceleration.text");
        accelerationSpinner = new ImmersiveSpinner(new SpinnerNumberModel(
              JumpPathItinerary.DEFAULT_ACCELERATION_G, MINIMUM_ACCELERATION_G, MAXIMUM_ACCELERATION_G,
              ACCELERATION_STEP_G));
        configureSpinner(accelerationSpinner, "0.0", "planning.acceleration.text", "planning.acceleration.tooltip");
        setPlannerControlWidth(accelerationSpinner, WIDE_PLANNER_CONTROL_WIDTH);
        addPlannerInput(planner, 1, "planning.acceleration.text", "planning.acceleration.tooltip",
              accelerationSpinner);

        double initialDesiredDays = Math.max(MINIMUM_DESIRED_DAYS,
              Math.min(MAXIMUM_DESIRED_DAYS, itineraryPlan.totalDays()));
        desiredDurationSpinner = new ImmersiveSpinner(new SpinnerNumberModel(initialDesiredDays,
              MINIMUM_DESIRED_DAYS, MAXIMUM_DESIRED_DAYS, 1.0));
        configureSpinner(desiredDurationSpinner, "0.0", "planning.desiredTime.text",
              "planning.desiredTime.tooltip");
          setPlannerControlWidth(desiredDurationSpinner, WIDE_PLANNER_CONTROL_WIDTH);
        addPlannerInput(planner, 2, "planning.desiredTime.text", "planning.desiredTime.tooltip",
              desiredDurationSpinner);

        JPanel result = createBandPanel();
        result.setLayout(new GridBagLayout());
        JLabel resultLabel = createPlannerLabel("planning.requiredAcceleration.text");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        result.add(resultLabel, constraints);

        requiredAccelerationValue = new JLabel();
        requiredAccelerationValue.setFont(requiredAccelerationValue.getFont().deriveFont(Font.BOLD));
        requiredAccelerationValue.setHorizontalAlignment(SwingConstants.TRAILING);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        result.add(requiredAccelerationValue, constraints);
        constraints = createFullWidthConstraints(3);
        constraints.insets = new Insets(4, 0, 2, 0);
        planner.add(result, constraints);

        accelerationSpinner.addChangeListener(event -> {
            itineraryPlan = calculatePlan(((Number) accelerationSpinner.getValue()).doubleValue());
            refreshPlanPresentation();
        });
        desiredDurationSpinner.addChangeListener(event -> updateRequiredAcceleration(
              ((Number) desiredDurationSpinner.getValue()).doubleValue()));
        updateRequiredAcceleration(((Number) desiredDurationSpinner.getValue()).doubleValue());
        return planner;
    }

    private JPanel createSchedulePlanner() {
        JPanel planner = createSection("section.schedule.text");
        JComboBox<String> modeSelector = new ImmersiveComboBox<>(new String[] {
              resourceMap.getString("schedule.departAt.text"),
              resourceMap.getString("schedule.arriveBy.text")
        });
        modeSelector.setSelectedIndex(scheduleMode.ordinal());
          configureComboBox(modeSelector, "schedule.mode.text", "schedule.mode.tooltip",
              WIDE_PLANNER_CONTROL_WIDTH);
        addPlannerControl(planner, 1, resourceMap.getString("schedule.mode.text"),
              resourceMap.getString("schedule.mode.tooltip"), modeSelector);

        scheduleAnchorSpinner = new ImmersiveSpinner(new SpinnerDateModel(toDate(departureAnchor), null, null,
              Calendar.HOUR_OF_DAY));
        configureScheduleAnchorSpinner();
        addPlannerControl(planner, 2, resourceMap.getString("schedule.anchor.text"),
              resourceMap.getString("schedule.anchor.tooltip"), scheduleAnchorSpinner);

        int row = 3;
        for (Dwell dwell : schedule.dwells()) {
            String label = format(resourceMap.getString("schedule.dwell.label.format"),
                  dwell.requestedStopIndex() + 1, dwell.system().getPrintableName(campaign.getLocalDate()));
            String tooltip = resourceMap.getString("schedule.dwell.tooltip");
            JSpinner dwellSpinner = new ImmersiveSpinner(new SpinnerNumberModel(
                dwellHoursByRequestedStop.get(dwell.requestedStopIndex()).intValue(),
                0, MAXIMUM_DWELL_HOURS, 1));
            configureNumberSpinner(dwellSpinner, "0", label, tooltip);
            setPlannerControlWidth(dwellSpinner, WIDE_PLANNER_CONTROL_WIDTH);
            addPlannerControl(planner, row++, label, tooltip, dwellSpinner);
            dwellSpinner.addChangeListener(event -> {
                dwellHoursByRequestedStop.set(dwell.requestedStopIndex(),
                      ((Number) dwellSpinner.getValue()).intValue());
                refreshPlanPresentation();
            });
        }
        if (schedule.dwells().isEmpty()) {
            JLabel none = new JLabel(resourceMap.getString("schedule.noDwells.text"));
            none.setForeground(DOSSIER_MUTED_TEXT);
            none.setFont(none.getFont().deriveFont(Font.PLAIN, none.getFont().getSize2D() * 0.78f));
            GridBagConstraints constraints = createFullWidthConstraints(row++);
            constraints.insets = new Insets(3, 0, 4, 0);
            planner.add(none, constraints);
        }

        scheduleAnchorValue = new JLabel();
        scheduleDepartureValue = new JLabel();
        scheduleArrivalValue = new JLabel();
        scheduleDwellValue = new JLabel();
        scheduleStatusValue = new JLabel();
        scheduleAnchorLabel = addScheduleResult(planner, row++, "schedule.selectedAnchor.text", scheduleAnchorValue);
        addScheduleResult(planner, row++, "schedule.departure.text", scheduleDepartureValue);
        addScheduleResult(planner, row++, "schedule.arrival.text", scheduleArrivalValue);
        addScheduleResult(planner, row++, "schedule.totalDwell.text", scheduleDwellValue);
        addScheduleResult(planner, row, "schedule.status.text", scheduleStatusValue);
        refreshSchedulePresentation();

        modeSelector.addActionListener(event -> {
            Mode selectedMode = Mode.values()[modeSelector.getSelectedIndex()];
            if (selectedMode == scheduleMode) {
                return;
            }
            if (selectedMode == Mode.ARRIVE_BY) {
                arrivalDeadline = schedule.arrival();
            } else {
                departureAnchor = schedule.departure();
            }
            scheduleMode = selectedMode;
            updateScheduleAnchorControl();
            refreshPlanPresentation();
        });
        scheduleAnchorSpinner.addChangeListener(event -> {
            if (adjustingScheduleAnchor) {
                return;
            }
            LocalDateTime selectedAnchor = fromDate((Date) scheduleAnchorSpinner.getValue());
            if (scheduleMode == Mode.DEPART_AT) {
                departureAnchor = selectedAnchor;
            } else {
                arrivalDeadline = selectedAnchor;
            }
            refreshPlanPresentation();
        });
        return planner;
    }

    private JPanel createPiratePointPlanner() {
        JPanel planner = createSection("section.piratePoint.text");
        Planet destination = destinationPlanet(path);
        boolean endpointAvailable = destination != null;
        int row = 1;

        piratePointModeSelector = new ImmersiveComboBox<>(new String[] {
              resourceMap.getString("pirate.mode.standard.text"),
              resourceMap.getString("pirate.mode.assumed.text")
        });
          configureComboBox(piratePointModeSelector, "pirate.mode.text", "pirate.mode.tooltip",
              PIRATE_SELECTOR_WIDTH);
        addPlannerControl(planner, row++, resourceMap.getString("pirate.mode.text"),
              resourceMap.getString("pirate.mode.tooltip"), piratePointModeSelector);

        List<NavigationSource> navigationSources = navigationSources();
        navigationSourceSelector = new ImmersiveComboBox<>(navigationSources.toArray(NavigationSource[]::new));
          configureComboBox(navigationSourceSelector, "pirate.skillSource.text", "pirate.skillSource.tooltip",
              PIRATE_SELECTOR_WIDTH);
        if (navigationSources.size() > 1) {
          piratePointControlLabels.add(addPlannerControl(planner, row++,
              resourceMap.getString("pirate.skillSource.text"),
              resourceMap.getString("pirate.skillSource.tooltip"), navigationSourceSelector));
        }

        piratePointTargetSpinner = new ImmersiveSpinner(new SpinnerNumberModel(DEFAULT_MANUAL_TARGET_NUMBER,
              MINIMUM_TARGET_NUMBER, MAXIMUM_TARGET_NUMBER, 1));
        configureSpinner(piratePointTargetSpinner, "0", "pirate.baseTarget.text", "pirate.baseTarget.tooltip");
          piratePointControlLabels.add(addPlannerInput(planner, row++, "pirate.baseTarget.text",
              "pirate.baseTarget.tooltip", piratePointTargetSpinner));

          double initialPiratePointDistance = initialPiratePointDistanceMillionsKm(endpointAvailable
              ? destination.getDistanceToJumpPoint()
              : 0.0);
          piratePointDistanceSpinner = new ImmersiveSpinner(new SpinnerNumberModel(initialPiratePointDistance,
              0.0, MAXIMUM_ASSUMED_DISTANCE_MILLIONS_KM, 1.0));
        configureSpinner(piratePointDistanceSpinner, "0.0", "pirate.distanceAssumption.text",
              "pirate.distanceAssumption.tooltip");
          piratePointControlLabels.add(addPlannerInput(planner, row++, "pirate.distanceAssumption.text",
              "pirate.distanceAssumption.tooltip", piratePointDistanceSpinner));

        detectionRadiusSpinner = new ImmersiveSpinner(new SpinnerNumberModel(0.0, 0.0,
              MAXIMUM_ASSUMED_DISTANCE_MILLIONS_KM, 1.0));
        configureSpinner(detectionRadiusSpinner, "0.0", "pirate.detectionAssumption.text",
              "pirate.detectionAssumption.tooltip");
          piratePointControlLabels.add(addPlannerInput(planner, row++, "pirate.detectionAssumption.text",
              "pirate.detectionAssumption.tooltip", detectionRadiusSpinner));

        for (ModifierCategory category : ModifierCategory.values()) {
            addPiratePointModifier(planner, row++, category);
        }

        JPanel comparison = createBandPanel();
        comparison.setLayout(new GridBagLayout());
        standardPointLabels = createApproachColumn(comparison, 0, "pirate.standardPoint.text");
        piratePointLabels = createApproachColumn(comparison, 1, "pirate.assumedPoint.text");
        GridBagConstraints constraints = createFullWidthConstraints(row++);
        constraints.insets = new Insets(7, 0, 5, 0);
        planner.add(comparison, constraints);

        piratePointTargetValue = new JLabel();
        piratePointOddsValue = new JLabel();
        piratePointTransitDifferenceValue = new JLabel();
        piratePointAdjustedArrivalValue = new JLabel();
          piratePointResultLabels.add(addScheduleResult(planner, row++, "pirate.target.text",
              piratePointTargetValue));
          piratePointResultLabels.add(addScheduleResult(planner, row++, "pirate.odds.text",
              piratePointOddsValue));
        piratePointTransitDifferenceLabel = addScheduleResult(planner, row++, "pirate.transitSaved.text",
              piratePointTransitDifferenceValue);
          piratePointResultLabels.add(piratePointTransitDifferenceLabel);
          piratePointResultLabels.add(addScheduleResult(planner, row, "pirate.adjustedArrival.text",
              piratePointAdjustedArrivalValue));

        piratePointModeSelector.setEnabled(endpointAvailable);

        piratePointModeSelector.addActionListener(event -> {
            refreshPiratePointPresentation();
            updatePiratePointDetailVisibility(planner);
        });
        navigationSourceSelector.addActionListener(event -> {
            NavigationSource source = (NavigationSource) navigationSourceSelector.getSelectedItem();
            adjustingPiratePointTarget = true;
            if ((source == null) || source.manual()) {
                piratePointTargetSpinner.setValue(manualPiratePointTarget);
            } else {
                piratePointTargetSpinner.setValue(source.targetNumber());
            }
            adjustingPiratePointTarget = false;
            refreshPiratePointPresentation();
        });
        piratePointTargetSpinner.addChangeListener(event -> {
            if (!adjustingPiratePointTarget) {
                manualPiratePointTarget = ((Number) piratePointTargetSpinner.getValue()).intValue();
                refreshPiratePointPresentation();
            }
        });
        piratePointDistanceSpinner.addChangeListener(event -> refreshPiratePointPresentation());
        detectionRadiusSpinner.addChangeListener(event -> refreshPiratePointPresentation());
        refreshPiratePointPresentation();
        updatePiratePointDetailVisibility(planner);
        return planner;
    }

    private void updatePiratePointDetailVisibility(JPanel planner) {
        boolean detailsVisible = piratePointModeSelector.getSelectedIndex() == 1;
        for (int index = 2; index < planner.getComponentCount(); index++) {
            planner.getComponent(index).setVisible(detailsVisible);
        }
        planner.revalidate();
        planner.repaint();
    }

    private <T> void configureComboBox(JComboBox<T> comboBox, String labelKey, String tooltipKey, int width) {
        comboBox.setToolTipText(resourceMap.getString(tooltipKey));
        comboBox.getAccessibleContext().setAccessibleName(resourceMap.getString(labelKey));
        comboBox.getAccessibleContext().setAccessibleDescription(comboBox.getToolTipText());
        setPlannerControlWidth(comboBox, width);
    }

    private List<NavigationSource> navigationSources() {
        List<NavigationSource> sources = new ArrayList<>();
        for (var unit : campaign.getUnits()) {
            if ((unit.getEntity() == null) || !unit.getEntity().isJumpShip()) {
                continue;
            }
            Person navigator = unit.getNavigator();
            if (navigator == null) {
                continue;
            }
            Skill navigation = navigator.getSkill(SkillType.S_NAVIGATION);
            if (navigation == null) {
                continue;
            }
            int targetNumber = navigation.getFinalSkillValue(navigator.getSkillModifierData());
            sources.add(new NavigationSource(format(resourceMap.getString("pirate.skillSource.navigator.format"),
                  navigator.getFullName(), unit.getName(), targetNumber), targetNumber, false));
        }
        sources.sort(Comparator.comparing(NavigationSource::label));
        sources.addFirst(new NavigationSource(resourceMap.getString("pirate.skillSource.manual.text"),
              DEFAULT_MANUAL_TARGET_NUMBER, true));
        return List.copyOf(sources);
    }

    private void addPiratePointModifier(JPanel planner, int row, ModifierCategory category) {
        String label = resourceMap.getString(modifierLabelKey(category));
        String tooltip = resourceMap.getString("pirate.modifier.tooltip");
        JCheckBox enabled = new ImmersiveCheckBox(label);
        enabled.setToolTipText(tooltip);
        enabled.getAccessibleContext().setAccessibleName(label);
        enabled.getAccessibleContext().setAccessibleDescription(tooltip);

        JSpinner value = new ImmersiveSpinner(new SpinnerNumberModel(0, MINIMUM_ASSUMED_MODIFIER,
              MAXIMUM_ASSUMED_MODIFIER, 1));
        configureNumberSpinner(value, "+0;-0", label, tooltip);

        JPanel input = createBandPanel();
        input.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        input.add(enabled, constraints);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        input.add(value, constraints);
        constraints = createFullWidthConstraints(row);
        constraints.insets = new Insets(2, 0, 2, 0);
        planner.add(input, constraints);

        piratePointModifierControls.add(new ModifierControl(category, enabled, value));
        enabled.addActionListener(event -> refreshPiratePointPresentation());
        value.addChangeListener(event -> refreshPiratePointPresentation());
    }

    private String modifierLabelKey(ModifierCategory category) {
        return switch (category) {
            case POINT_GEOMETRY -> "pirate.modifier.geometry.text";
            case NAVIGATION_DATA -> "pirate.modifier.navigationData.text";
            case VESSEL_CREW_CONDITION -> "pirate.modifier.vesselCrew.text";
            case OTHER_SITUATION -> "pirate.modifier.other.text";
        };
    }

    private ApproachLabels createApproachColumn(JPanel comparison, int column, String headingKey) {
        JPanel approach = createBandPanel();
        approach.setLayout(new GridBagLayout());
        JLabel heading = new JLabel(resourceMap.getString(headingKey));
        heading.setForeground(column == 0 ? DOSSIER_TEXT : DOSSIER_ACTIVE);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D() * 0.8f));
        GridBagConstraints constraints = createFullWidthConstraints(0);
        constraints.insets = new Insets(0, 0, 3, 0);
        approach.add(heading, constraints);

        JLabel distance = addApproachFact(approach, 1, "pirate.distance.text");
        JLabel transit = addApproachFact(approach, 2, "pirate.transit.text");
        JLabel exposure = addApproachFact(approach, 3, "pirate.exposure.text");
        JLabel detection = addApproachFact(approach, 4, "pirate.detection.text");

        constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, column == 0 ? 0 : HORIZONTAL_PADDING / 2, 0,
              column == 0 ? HORIZONTAL_PADDING / 2 : 0);
        comparison.add(approach, constraints);
        return new ApproachLabels(approach, distance, transit, exposure, detection);
    }

    private JLabel addApproachFact(JPanel approach, int row, String labelKey) {
        JLabel label = createPlannerLabel(labelKey);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.45;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(1, 0, 1, 5);
        approach.add(label, constraints);

        JLabel value = new JLabel();
        value.setForeground(DOSSIER_TEXT);
        value.setFont(value.getFont().deriveFont(Font.BOLD, value.getFont().getSize2D() * 0.78f));
        value.setHorizontalAlignment(SwingConstants.TRAILING);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.weightx = 0.55;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(1, 0, 1, 0);
        approach.add(value, constraints);
        return value;
    }

    private void refreshPiratePointPresentation() {
        if (piratePointTargetValue == null) {
            return;
        }
        Planet destination = destinationPlanet(path);
        updatePiratePointControlState(destination != null);
        if (destination == null) {
            setApproachUnavailable(standardPointLabels);
            setApproachUnavailable(piratePointLabels);
            setPiratePointResultUnavailable();
            return;
        }

        double accelerationG = ((Number) accelerationSpinner.getValue()).doubleValue();
        double pirateDistanceKm = ((Number) piratePointDistanceSpinner.getValue()).doubleValue()
                                         * MILLION_KILOMETERS;
        double detectionRadiusKm = ((Number) detectionRadiusSpinner.getValue()).doubleValue()
                                         * MILLION_KILOMETERS;
        List<Modifier> modifiers = piratePointModifierControls.stream()
                                             .map(control -> new Modifier(control.category(),
                                                   ((Number) control.value().getValue()).intValue(),
                                                   control.enabled().isSelected()))
                                             .toList();
        Facts facts;
        try {
            facts = PiratePointAnalysis.analyze(new Input(destination.getDistanceToJumpPoint(), pirateDistanceKm,
                  detectionRadiusKm, accelerationG,
                  ((Number) piratePointTargetSpinner.getValue()).intValue(), modifiers));
        } catch (IllegalArgumentException exception) {
            setApproachUnavailable(standardPointLabels);
            setApproachUnavailable(piratePointLabels);
            setPiratePointResultUnavailable();
            return;
        }

        updateApproachLabels(standardPointLabels, facts.standardPoint());
        updateApproachLabels(piratePointLabels, facts.piratePoint());
        DifficultyFacts difficulty = facts.difficulty();
        piratePointTargetValue.setText(format(resourceMap.getString("pirate.target.format"),
              difficulty.targetNumber(), formatSignedNumber(difficulty.enabledModifierTotal())));
        piratePointOddsValue.setText(format2d6Odds(difficulty, locale, resourceMap));
        boolean transitAdded = facts.transitAddedDays() > 0.0;
        piratePointTransitDifferenceLabel.setText(resourceMap.getString(transitAdded
              ? "pirate.transitAdded.text"
              : "pirate.transitSaved.text"));
        piratePointTransitDifferenceValue.setText(formatDuration(transitAdded
              ? facts.transitAddedDays()
              : facts.transitSavingsDays()));
        piratePointAdjustedArrivalValue.setText(formatScheduleMoment(adjustedArrival(schedule, itineraryPlan,
              facts, isPiratePointAssumptionEnabled(piratePointModeSelector.getSelectedIndex(), true)), locale));
    }

    private void updatePiratePointControlState(boolean endpointAvailable) {
        boolean assumptionsEnabled = isPiratePointAssumptionEnabled(piratePointModeSelector.getSelectedIndex(),
              endpointAvailable);
        piratePointModeSelector.setEnabled(endpointAvailable);
        navigationSourceSelector.setEnabled(assumptionsEnabled);
        NavigationSource source = (NavigationSource) navigationSourceSelector.getSelectedItem();
        piratePointTargetSpinner.setEnabled(assumptionsEnabled && ((source == null) || source.manual()));
        piratePointDistanceSpinner.setEnabled(assumptionsEnabled);
        detectionRadiusSpinner.setEnabled(assumptionsEnabled);
        piratePointControlLabels.forEach(label -> label.setEnabled(assumptionsEnabled));
        for (ModifierControl control : piratePointModifierControls) {
            control.enabled().setEnabled(assumptionsEnabled);
            control.value().setEnabled(assumptionsEnabled && control.enabled().isSelected());
        }
        for (Component component : piratePointLabels.panel().getComponents()) {
            component.setEnabled(assumptionsEnabled);
        }
        piratePointResultLabels.forEach(label -> label.setEnabled(assumptionsEnabled));
        List.of(piratePointTargetValue, piratePointOddsValue, piratePointTransitDifferenceValue,
              piratePointAdjustedArrivalValue).forEach(value -> value.setEnabled(assumptionsEnabled));
    }

    private void updateApproachLabels(ApproachLabels labels, PiratePointAnalysis.ApproachFacts facts) {
        labels.distance().setText(format(resourceMap.getString("pirate.distance.format"),
              formatNumber(facts.distanceKm() / MILLION_KILOMETERS, 2)));
        labels.transit().setText(formatDuration(facts.transitDays()));
        labels.exposure().setText(formatDuration(facts.exposureDays()));
        labels.detection().setText(resourceMap.getString(facts.emergenceInsideDetectionEnvelope()
              ? "pirate.detection.inside.text"
              : "pirate.detection.outside.text"));
        labels.detection().setForeground(facts.emergenceInsideDetectionEnvelope() ? DOSSIER_ACTIVE : DOSSIER_ACCENT);
    }

    private void setApproachUnavailable(ApproachLabels labels) {
        String unavailable = resourceMap.getString("pirate.unavailable.text");
        labels.distance().setText(unavailable);
        labels.transit().setText(unavailable);
        labels.exposure().setText(unavailable);
        labels.detection().setText(unavailable);
        labels.detection().setForeground(DOSSIER_MUTED_TEXT);
    }

    private void setPiratePointResultUnavailable() {
        String unavailable = resourceMap.getString("pirate.unavailable.text");
        piratePointTargetValue.setText(unavailable);
        piratePointOddsValue.setText(unavailable);
        piratePointTransitDifferenceValue.setText(unavailable);
        piratePointAdjustedArrivalValue.setText(unavailable);
    }

    static Planet destinationPlanet(JumpPath path) {
        if (path.getTargetPlanet() != null) {
            return path.getTargetPlanet();
        }
        List<PlanetarySystem> systems = path.getSystems();
        return systems.isEmpty() ? null : systems.getLast().getPrimaryPlanet();
    }

    static boolean isPiratePointAssumptionEnabled(int selectedModeIndex, boolean endpointAvailable) {
        return endpointAvailable && (selectedModeIndex == 1);
    }

    static double initialPiratePointDistanceMillionsKm(double standardDistanceKm) {
        if (!Double.isFinite(standardDistanceKm) || (standardDistanceKm <= 0.0)) {
            return 0.0;
        }
        return Math.min(MAXIMUM_ASSUMED_DISTANCE_MILLIONS_KM, standardDistanceKm / MILLION_KILOMETERS);
    }

    static LocalDateTime adjustedArrival(Result schedule, Plan itinerary, Facts facts, boolean usePiratePoint) {
        if (!usePiratePoint) {
            return schedule.arrival();
        }
        double adjustedItineraryDays = itinerary.totalDays() - itinerary.endingTransitDays()
                                             + facts.piratePoint().transitDays();
        long adjustedHours = Math.round(adjustedItineraryDays * 24.0) + schedule.totalDwellHours();
        return schedule.departure().plusHours(adjustedHours);
    }

    static String format2d6Odds(DifficultyFacts difficulty, Locale locale, ResourceBundle resources) {
        NumberFormat percentage = NumberFormat.getNumberInstance(locale);
        percentage.setMaximumFractionDigits(1);
        percentage.setMinimumFractionDigits(0);
        return format(resources.getString("pirate.odds.format"), difficulty.successfulOutcomes(),
              percentage.format(difficulty.successProbability() * 100.0));
    }

    private String formatSignedNumber(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private JLabel addScheduleResult(JPanel planner, int row, String labelKey, JLabel value) {
        JPanel result = createBandPanel();
        result.setLayout(new GridBagLayout());
        JLabel label = createPlannerLabel(labelKey);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.42;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        result.add(label, constraints);

        value.setForeground(DOSSIER_TEXT);
        value.setFont(value.getFont().deriveFont(Font.BOLD, value.getFont().getSize2D() * 0.82f));
        value.setHorizontalAlignment(SwingConstants.TRAILING);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.58;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        result.add(value, constraints);

        constraints = createFullWidthConstraints(row);
        constraints.insets = new Insets(2, 0, 2, 0);
        planner.add(result, constraints);
        return label;
    }

    private void configureScheduleAnchorSpinner() {
        JSpinner.DateEditor editor = new JSpinner.DateEditor(scheduleAnchorSpinner,
              resourceMap.getString("schedule.datePattern.text"));
        editor.getFormat().setTimeZone(TimeZone.getTimeZone("UTC"));
        scheduleAnchorSpinner.setEditor(editor);
        String label = resourceMap.getString("schedule.anchor.text");
        String tooltip = resourceMap.getString("schedule.anchor.tooltip");
          styleSpinner(scheduleAnchorSpinner, label, tooltip);
                setPlannerControlWidth(scheduleAnchorSpinner, WIDE_PLANNER_CONTROL_WIDTH);
    }

    private void configureSpinner(JSpinner spinner, String pattern, String labelKey, String tooltipKey) {
        configureNumberSpinner(spinner, pattern, resourceMap.getString(labelKey), resourceMap.getString(tooltipKey));
    }

    private void configureNumberSpinner(JSpinner spinner, String pattern, String label, String tooltip) {
        spinner.setEditor(new JSpinner.NumberEditor(spinner, pattern));
        styleSpinner(spinner, label, tooltip);
    }

    private void styleSpinner(JSpinner spinner, String label, String tooltip) {
        setPlannerControlWidth(spinner, DENSE_SPINNER_WIDTH);
        spinner.setToolTipText(tooltip);
        spinner.getAccessibleContext().setAccessibleName(label);
        spinner.getAccessibleContext().setAccessibleDescription(tooltip);
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            editor.getTextField().setToolTipText(tooltip);
        }
    }

    private static void setPlannerControlWidth(JComponent control, int width) {
        Dimension size = new Dimension(width, control.getPreferredSize().height);
        control.setPreferredSize(size);
        control.setMinimumSize(size);
    }

    private JLabel addPlannerInput(JPanel planner, int row, String labelKey, String tooltipKey, JSpinner spinner) {
        return addPlannerControl(planner, row, resourceMap.getString(labelKey),
              resourceMap.getString(tooltipKey), spinner);
    }

    private JLabel addPlannerControl(JPanel planner, int row, String labelText, String tooltip,
          JComponent control) {
        JPanel input = createBandPanel();
        input.setLayout(new GridBagLayout());
        JLabel label = new JLabel(labelText);
        label.setForeground(DOSSIER_MUTED_TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() * 0.8f));
        label.setLabelFor(control);
        label.setToolTipText(tooltip);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        input.add(label, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        input.add(control, constraints);

        constraints = createFullWidthConstraints(row);
        constraints.insets = new Insets(3, 0, 3, 0);
        planner.add(input, constraints);
        return label;
    }

    private JLabel createPlannerLabel(String labelKey) {
        JLabel label = new JLabel(resourceMap.getString(labelKey));
        label.setForeground(DOSSIER_MUTED_TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() * 0.8f));
        return label;
    }

    private void updateRequiredAcceleration(double desiredTotalDays) {
        AbstractLocation currentLocation = getCurrentLocation();
        RequiredAcceleration result = JumpPathItinerary.solveRequiredAcceleration(path, campaign.getLocalDate(),
              desiredTotalDays, getFleetSystem(currentLocation), getCurrentTransit(currentLocation),
              circuitPlan);
        String resultText;
        if (result.isPossible()) {
            resultText = format(resourceMap.getString("planning.requiredAcceleration.format"),
                  formatNumber(result.accelerationG().orElseThrow(), 2));
            requiredAccelerationValue.setForeground(DOSSIER_ACCENT);
        } else {
            resultText = resourceMap.getString("planning.requiredAcceleration.impossible.text");
            requiredAccelerationValue.setForeground(DOSSIER_ACTIVE);
        }
        requiredAccelerationValue.setText(resultText);
        requiredAccelerationValue.getAccessibleContext().setAccessibleName(format(
              resourceMap.getString("planning.requiredAcceleration.accessible.format"), resultText));
    }

    private void refreshPlanPresentation() {
        schedule = calculateSchedule();
        startingTransitValue.setText(formatDays(itineraryPlan.startingTransitDays()));
        endingTransitValue.setText(formatDays(itineraryPlan.endingTransitDays()));
        rechargeValue.setText(formatDays(itineraryPlan.rechargeDays()));
        totalTimeValue.setText(formatDays(itineraryPlan.totalDays() + (schedule.totalDwellHours() / 24.0)));
        refreshSchedulePresentation();
        refreshPiratePointPresentation();
        populateItinerary(itinerarySection);
        itinerarySection.revalidate();
        itinerarySection.repaint();
    }

    private JPanel createItinerary() {
        JPanel itinerary = createSection("section.itinerary.text");
        populateItinerary(itinerary);
        return itinerary;
    }

    private void populateItinerary(JPanel itinerary) {
        while (itinerary.getComponentCount() > 1) {
            itinerary.remove(1);
        }

        LocalDate currentDate = schedule.departure().toLocalDate();
        Color routeColor = isActiveRoute() ? DOSSIER_ACTIVE : DOSSIER_ACCENT;

        for (JumpPathSchedule.Entry scheduledEntry : schedule.entries()) {
            TimelineEntry entry = scheduledEntry.itineraryEntry();
            JPanel waypoint = createBandPanel();
            waypoint.setLayout(new GridBagLayout());
            if (entry.sequence() > 1) {
                waypoint.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DOSSIER_DIVIDER));
            }

            JLabel sequence = new JLabel(String.format("%02d", entry.sequence()));
            sequence.setForeground(routeColor);
            sequence.setFont(sequence.getFont().deriveFont(Font.BOLD));
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridheight = 2;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(8, 0, 8, 12);
            waypoint.add(sequence, constraints);

            String systemNameText = entry.system().getPrintableName(currentDate);
            if (entry.destination() && (path.getTargetPlanet() != null)) {
                systemNameText = format(resourceMap.getString("timeline.targetPlanet.format"), systemNameText,
                      path.getTargetPlanet().getPrintableName(currentDate));
            }
            JLabel systemName = new JLabel(systemNameText);
            systemName.setForeground(DOSSIER_TEXT);
            systemName.setFont(systemName.getFont().deriveFont(Font.BOLD));
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(7, 0, 1, 0);
            waypoint.add(systemName, constraints);

            int eventRow = 1;
            if (!entry.origin()) {
                int legIndex = entry.sequence() - 2;
                if (legIndex < navigationAssessment.legs().size()) {
                    LegAssessment legAssessment = navigationAssessment.legs().get(legIndex);
                    JLabel legFacts = new JLabel(formatLegFacts(legAssessment, locale, resourceMap));
                    legFacts.setForeground(switch (legAssessment.severity()) {
                        case BLOCKED -> new Color(234, 86, 86);
                        case CAUTION -> DOSSIER_ACTIVE;
                        default -> DOSSIER_MUTED_TEXT;
                    });
                    legFacts.setFont(legFacts.getFont().deriveFont(Font.PLAIN,
                          legFacts.getFont().getSize2D() * 0.74f));
                    legFacts.getAccessibleContext().setAccessibleName(legFacts.getText());
                    constraints = new GridBagConstraints();
                    constraints.gridx = 1;
                    constraints.gridy = eventRow++;
                    constraints.gridwidth = 2;
                    constraints.weightx = 1.0;
                    constraints.fill = GridBagConstraints.HORIZONTAL;
                    constraints.anchor = GridBagConstraints.WEST;
                    constraints.insets = new Insets(0, 0, 3, 0);
                    waypoint.add(legFacts, constraints);
                }
            }
            if (entry.origin()) {
                addTimelineEvent(waypoint, eventRow++, resourceMap.getString("timeline.originStart.text"),
                    formatScheduleMoment(scheduledEntry.arrival(), locale));
                addTimelineEvent(waypoint, eventRow++,
                    format(resourceMap.getString("timeline.jumpDeparture.format"),
                        formatDuration(itineraryPlan.startingTransitDays())),
                    formatScheduleMoment(scheduledEntry.departure(), locale));
            } else {
                addTimelineEvent(waypoint, eventRow++, resourceMap.getString("timeline.jumpArrival.text"),
                    formatScheduleMoment(scheduledEntry.arrival(), locale));
            }
            if (!entry.origin() && !entry.destination()) {
                if (scheduledEntry.dwellHours() > 0) {
                    addTimelineEvent(waypoint, eventRow++,
                        format(resourceMap.getString("timeline.rechargeComplete.format"),
                            formatHours(entry.rechargeHours(), locale, resourceMap)),
                        formatScheduleMoment(scheduledEntry.readyForDeparture(), locale));
                    addTimelineEvent(waypoint, eventRow++,
                        format(resourceMap.getString("timeline.dwellDeparture.format"),
                            formatHours(scheduledEntry.dwellHours(), locale, resourceMap)),
                        formatScheduleMoment(scheduledEntry.departure(), locale));
                } else {
                    addTimelineEvent(waypoint, eventRow++,
                        format(resourceMap.getString("timeline.rechargeDeparture.format"),
                            formatHours(entry.rechargeHours(), locale, resourceMap)),
                        formatScheduleMoment(scheduledEntry.departure(), locale));
                }
            }
            if (entry.destination()) {
                addTimelineEvent(waypoint, eventRow,
                    format(resourceMap.getString("timeline.endpointArrival.format"),
                        formatDuration(entry.endpointTransitDays())),
                    formatScheduleMoment(scheduledEntry.endpointArrival(), locale));
            }

            constraints = createFullWidthConstraints(entry.sequence());
            itinerary.add(waypoint, constraints);
        }
        }

        static String formatLegFacts(LegAssessment assessment, Locale locale, ResourceBundle resources) {
          NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
          numberFormat.setMaximumFractionDigits(2);
          numberFormat.setMinimumFractionDigits(0);
          String status = navigationStatusText(assessment, resources);
          String recharge = Double.isFinite(assessment.facts().rechargeHours())
              ? format(resources.getString("timeline.rechargeHours.format"),
                  numberFormat.format(assessment.facts().rechargeHours()))
              : resources.getString("timeline.rechargeImpossible.text");
          String solar = Double.isFinite(assessment.facts().solarRechargeHours())
              ? format(resources.getString("timeline.solarRecharge.format"),
                  numberFormat.format(assessment.facts().solarRechargeHours()))
              : resources.getString("timeline.solarRechargeImpossible.text");
          String stations = assessment.facts().rechargeStationCount() == 0
              ? resources.getString("timeline.noRechargeStation.text")
              : format(resources.getString("timeline.rechargeStations.format"),
                  assessment.facts().rechargeStationCount());
          String rechargeSources = format(resources.getString("timeline.rechargeSources.format"), solar, stations);
          String circuit = assessment.facts().commandCircuitAssumed()
              ? resources.getString("timeline.circuitAssumed.text")
              : "";
          return format(resources.getString("timeline.legFacts.format"),
              numberFormat.format(assessment.facts().distanceLy()),
              assessment.facts().minimumStandardJumps(), status, recharge, rechargeSources, circuit);
        }

        private static String navigationStatusText(LegAssessment assessment, ResourceBundle resources) {
          NavigationRouteAnalysis.FindingKind primaryFinding = assessment.findings().stream()
              .filter(finding -> finding.severity() == Severity.BLOCKED)
              .map(NavigationRouteAnalysis.Finding::kind)
              .findFirst()
              .orElseGet(() -> assessment.findings().stream()
                  .filter(finding -> finding.severity() == Severity.CAUTION)
                  .map(NavigationRouteAnalysis.Finding::kind)
                  .findFirst()
                  .orElse(null));
          if (primaryFinding == null) {
            return resources.getString("timeline.status.clear.text");
          }
          return resources.getString(switch (primaryFinding) {
            case OUT_OF_STANDARD_JUMP_RANGE -> "timeline.status.rangeBlocked.text";
            case ACCESS_DENIED -> "timeline.status.accessBlocked.text";
            case ABANDONED_DESTINATION_AVOIDED -> "timeline.status.emptyBlocked.text";
            case ABANDONED_DESTINATION_ALLOWED -> "timeline.status.emptyCaution.text";
            case RECHARGE_IMPOSSIBLE -> "timeline.status.rechargeBlocked.text";
            default -> "timeline.status.blocked.text";
          });
        }

        private PathAssessment calculateNavigationAssessment() {
                    PathAssessment assessment = campaign.assessNavigationPath(path.getSystems(), requestedStops,
                            circuitPlan::usesCircuitAt);
          return assessment == null ? new PathAssessment(List.of(), Severity.CLEAR) : assessment;
        }

        private void addTimelineEvent(JPanel waypoint, int row, String eventText, String momentText) {
          JLabel event = new JLabel(eventText);
          event.setForeground(DOSSIER_MUTED_TEXT);
          event.setFont(event.getFont().deriveFont(Font.BOLD, event.getFont().getSize2D() * 0.78f));
          GridBagConstraints constraints = new GridBagConstraints();
          constraints.gridx = 1;
          constraints.gridy = row;
          constraints.weightx = 0.42;
          constraints.fill = GridBagConstraints.HORIZONTAL;
          constraints.anchor = GridBagConstraints.WEST;
          constraints.insets = new Insets(1, 0, 2, 8);
          waypoint.add(event, constraints);

          JLabel moment = new JLabel(momentText);
          moment.setForeground(DOSSIER_TEXT);
          moment.setFont(moment.getFont().deriveFont(Font.PLAIN, moment.getFont().getSize2D() * 0.78f));
          moment.setHorizontalAlignment(SwingConstants.TRAILING);
          constraints = new GridBagConstraints();
          constraints.gridx = 2;
          constraints.gridy = row;
          constraints.weightx = 0.58;
          constraints.fill = GridBagConstraints.HORIZONTAL;
          constraints.anchor = GridBagConstraints.EAST;
          constraints.insets = new Insets(1, 0, 2, 0);
          waypoint.add(moment, constraints);
    }

    private JPanel createSection(String headingKey) {
        JPanel section = createBandPanel();
        section.setLayout(new GridBagLayout());
        section.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(1, 0, 0, 0, DOSSIER_DIVIDER),
              BorderFactory.createEmptyBorder(9, HORIZONTAL_PADDING, 10, HORIZONTAL_PADDING)));

        JLabel heading = new JLabel(resourceMap.getString(headingKey));
        heading.setForeground(DOSSIER_ACCENT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D() * 0.85f));
        GridBagConstraints constraints = createFullWidthConstraints(0);
        constraints.insets = new Insets(0, 0, 5, 0);
        section.add(heading, constraints);
        return section;
    }

    private JLabel addMetric(JPanel summary, int metricIndex, String labelKey, String value) {
        JPanel metric = createBandPanel();
        metric.setLayout(new BoxLayout(metric, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(resourceMap.getString(labelKey));
        label.setForeground(DOSSIER_MUTED_TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() * 0.8f));
        label.setAlignmentX(LEFT_ALIGNMENT);
        metric.add(label);

        JLabel metricValue = new JLabel(value);
        metricValue.setForeground(DOSSIER_TEXT);
        metricValue.setFont(metricValue.getFont().deriveFont(Font.BOLD));
        metricValue.setAlignmentX(LEFT_ALIGNMENT);
        metric.add(metricValue);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = metricIndex % 2;
        constraints.gridy = 1 + (metricIndex / 2);
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(3, 0, 5, (metricIndex % 2 == 0) ? HORIZONTAL_PADDING : 0);
        summary.add(metric, constraints);
        return metricValue;
    }

    private JPanel createBandPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(DOSSIER_BACKGROUND);
        panel.setOpaque(true);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private GridBagConstraints createFullWidthConstraints(int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        return constraints;
    }

    private boolean isActiveRoute() {
        JumpPath activePath = campaign.getPlayerForce().getForceDetachment().getCurrentLocation().getJumpPath();
        return activePath == path;
    }

    private boolean isUseCommandCircuit() {
        return FactionStandingUtilities.isUseCommandCircuit(campaign.getPlayerForce()
                                                                  .isOverridingCommandCircuitRequirements(),
              campaign.isGM(), campaign.getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
              campaign.getPlayerForce().getFactionStandings(), campaign.getFutureContracts());
    }

    private Plan calculatePlan(double accelerationG) {
        AbstractLocation currentLocation = getCurrentLocation();
        return JumpPathItinerary.calculate(path, campaign.getLocalDate(), accelerationG,
              getFleetSystem(currentLocation), getCurrentTransit(currentLocation), circuitPlan);
    }

    private Result calculateSchedule() {
        LocalDateTime anchor = scheduleMode == Mode.DEPART_AT ? departureAnchor : arrivalDeadline;
        return JumpPathSchedule.calculate(itineraryPlan, requestedStops, dwellHoursByRequestedStop,
              scheduleMode, anchor, earliestFeasibleDeparture);
    }

    private void refreshSchedulePresentation() {
        if (scheduleAnchorValue == null) {
            return;
        }
        scheduleAnchorLabel.setText(resourceMap.getString(schedule.mode() == Mode.DEPART_AT
              ? "schedule.departureAnchor.text"
              : "schedule.arrivalDeadline.text"));
        scheduleAnchorValue.setText(formatScheduleMoment(schedule.anchor(), locale));
        scheduleDepartureValue.setText(formatScheduleMoment(schedule.departure(), locale));
        scheduleArrivalValue.setText(formatScheduleMoment(schedule.arrival(), locale));
        scheduleDwellValue.setText(formatHours(schedule.totalDwellHours(), locale, resourceMap));
        scheduleStatusValue.setText(scheduleStatusText(schedule, locale, resourceMap));
        scheduleStatusValue.setForeground(schedule.feasible() ? DOSSIER_ACCENT : DOSSIER_ACTIVE);
        scheduleStatusValue.getAccessibleContext().setAccessibleName(scheduleStatusValue.getText());
    }

    private void updateScheduleAnchorControl() {
        adjustingScheduleAnchor = true;
        LocalDateTime anchor = scheduleMode == Mode.DEPART_AT ? departureAnchor : arrivalDeadline;
        scheduleAnchorSpinner.setValue(toDate(anchor));
        adjustingScheduleAnchor = false;
    }

    private static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.toInstant(ZoneOffset.UTC));
    }

    private static LocalDateTime fromDate(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC).withSecond(0).withNano(0);
    }

    static boolean isCourseSelected(JumpPath path, Course course) {
        List<PlanetarySystem> pathSystems = path.getSystems();
        List<PlanetarySystem> courseSystems = course.systems();
        if (pathSystems.size() != courseSystems.size()) {
            return false;
        }
        for (int index = 0; index < pathSystems.size(); index++) {
            if (!pathSystems.get(index).getId().equals(courseSystems.get(index).getId())) {
                return false;
            }
        }
        return true;
    }

    static CircuitPlan initialCircuitPlan(JumpPath path, List<Course> courses, boolean campaignUsesCircuit) {
        for (Course course : courses) {
            if (isCourseSelected(path, course) && (course.circuitCoverage() == CircuitCoverage.WHOLE)) {
                return CircuitPlan.whole();
            }
        }
        return campaignUsesCircuit ? CircuitPlan.whole() : CircuitPlan.none();
    }

    private AbstractLocation getCurrentLocation() {
        return campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
    }

    private PlanetarySystem getFleetSystem(AbstractLocation currentLocation) {
        return (currentLocation == null) ? null : currentLocation.getCurrentSystem();
    }

    private double getCurrentTransit(AbstractLocation currentLocation) {
        return (currentLocation == null) ? 0.0 : currentLocation.getTransitTime();
    }

    private String formatDays(double days) {
        return format(resourceMap.getString("metric.days.format"), formatNumber(days, 2));
    }

    private String formatDuration(double days) {
        long roundedHours = Math.round(days * 24.0);
        String duration = formatHours(Math.abs(roundedHours), locale, resourceMap);
        return (roundedHours < 0)
              ? format(resourceMap.getString("timeline.negativeDuration.format"), duration)
              : duration;
    }

    private String formatNumber(double value, int maximumFractionDigits) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setMaximumFractionDigits(maximumFractionDigits);
        return numberFormat.format(value);
    }

    static String formatTimelineMoment(LocalDate startDate, double elapsedDays, Locale locale,
          ResourceBundle resources) {
        long roundedHours = Math.round(elapsedDays * 24.0);
        long elapsedWholeDays = Math.floorDiv(roundedHours, 24);
        String date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                            .withLocale(locale)
                            .format(startDate.plusDays(elapsedWholeDays));
        String sign = (roundedHours < 0) ? "-" : "+";
        return format(resources.getString("timeline.moment.format"), date, sign,
              formatHours(Math.abs(roundedHours), locale, resources));
    }

    static String formatScheduleMoment(LocalDateTime moment, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                     .withLocale(locale)
                     .format(moment);
    }

    static String scheduleStatusText(Result schedule, Locale locale, ResourceBundle resources) {
        if (schedule.feasible()) {
            return resources.getString(schedule.mode() == Mode.DEPART_AT
                  ? "schedule.status.forecast.text"
                  : "schedule.status.feasible.text");
        }
        long missedHours = Math.max(1,
              (Duration.between(schedule.departure(), schedule.earliestFeasibleDeparture()).toMinutes() + 59) / 60);
        return format(resources.getString(schedule.mode() == Mode.DEPART_AT
                    ? "schedule.status.departureUnavailable.format"
                    : "schedule.status.missed.format"),
              formatHours(missedHours, locale, resources));
    }

    private static String formatHours(long totalHours, Locale locale, ResourceBundle resources) {
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(locale);
        long days = totalHours / 24;
        long hours = totalHours % 24;
        if ((days > 0) && (hours > 0)) {
            return format(resources.getString("timeline.daysHours.format"), numberFormat.format(days),
                  numberFormat.format(hours));
        } else if (days > 0) {
            return format(resources.getString("timeline.days.format"), numberFormat.format(days));
        }
        return format(resources.getString("timeline.hours.format"), numberFormat.format(hours));
    }

    private String getSystemName(PlanetarySystem system, LocalDate currentDate) {
        return (system == null) ? resourceMap.getString("dossier.unknownSystem.text")
                     : system.getPrintableName(currentDate);
    }

    private record NavigationSource(String label, int targetNumber, boolean manual) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record ModifierControl(ModifierCategory category, JCheckBox enabled, JSpinner value) {
    }

    private record ApproachLabels(JPanel panel, JLabel distance, JLabel transit, JLabel exposure,
                                  JLabel detection) {
    }
}
