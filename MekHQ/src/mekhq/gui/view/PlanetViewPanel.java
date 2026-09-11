/*
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllActiveBioweapons;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllActiveDiseases;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JToolTip;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import megamek.client.ui.util.UIUtil;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.universe.LandMass;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Satellite;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.enums.CapitalType;
import mekhq.campaign.universe.enums.PlanetaryType;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.SourceableValueLabel;
import mekhq.gui.utilities.MarkdownRenderer;
import mekhq.utilities.MHQInternationalization;
import org.apache.commons.lang3.StringUtils;

/**
 * A custom panel that gets filled in with goodies from a Planet record
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class PlanetViewPanel extends JScrollablePanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PlanetViewPanel";
    private static final Color DOSSIER_BACKGROUND = new Color(7, 16, 27);
    private static final Color DOSSIER_TEXT = new Color(218, 231, 235);
    private static final Color DOSSIER_MUTED_TEXT = new Color(132, 153, 161);
    private static final Color DOSSIER_ACCENT = new Color(65, 210, 224);
    private static final Color DOSSIER_WARNING = new Color(235, 166, 66);
    private static final Color DOSSIER_DIVIDER = new Color(35, 66, 82);
    private static final int HORIZONTAL_PADDING = UIUtil.scaleForGUI(12);
    private static final int LABEL_COLUMN_WIDTH = UIUtil.scaleForGUI(150);
    private static final int REVEAL_FRAME_DELAY_MS = 16;
    private static final long HEADER_REVEAL_DURATION_NS = 200_000_000L;
    private static final long SUMMARY_REVEAL_DELAY_NS = 120_000_000L;
    private static final long SUMMARY_REVEAL_DURATION_NS = 220_000_000L;
    private static final long SECTION_REVEAL_INITIAL_DELAY_NS = 250_000_000L;
    private static final long SECTION_REVEAL_STAGGER_NS = 110_000_000L;
    private static final long SECTION_REVEAL_DURATION_NS = 250_000_000L;

    private final PlanetarySystem system;
    private final Campaign campaign;
    private final int planetPos;
    private final boolean animateReveal;
    private final List<RevealBandPanel> revealBands = new ArrayList<>();

    private Timer revealTimer;
    private JViewport revealViewport;
    private int previousViewportScrollMode;
    private long revealStartTime;
    private int detailRevealIndex;
    private boolean revealComplete;

    public PlanetViewPanel(PlanetarySystem s, Campaign c) {
        this(s, c, 0, false);
    }

    public PlanetViewPanel(PlanetarySystem s, Campaign c, int p) {
        this(s, c, p, false);
    }

    public PlanetViewPanel(PlanetarySystem s, Campaign c, int p, boolean animateReveal) {
        super();
        this.system = s;
        this.campaign = c;
        this.planetPos = p;
        this.animateReveal = animateReveal;
        revealComplete = !animateReveal;
        initComponents();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (animateReveal && !revealComplete) {
            revealViewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
            if (revealViewport != null) {
                revealViewport.setViewPosition(new java.awt.Point(0, 0));
                previousViewportScrollMode = revealViewport.getScrollMode();
                revealViewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (animateReveal && !revealComplete && (revealTimer == null) && isDisplayable()) {
            startRevealAnimation();
        }
        super.paintComponent(graphics);
    }

    @Override
    public void removeNotify() {
        if (!revealComplete) {
            finishRevealAnimation();
        } else {
            stopRevealTimer();
        }
        super.removeNotify();
    }

    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(DOSSIER_BACKGROUND);
        setOpaque(true);

        Planet planet = getSelectedPlanet();
        Set<InjuryType> activeDiseases = getActiveDiseases();
        add(createHeader(planet));
        add(createOperationalSummary(planet, activeDiseases));

        if (planet != null) {
            add(getWorldProfilePanel(planet));
            DossierSection environmentPanel = getEnvironmentPanel(planet);
            if (environmentPanel.hasContent()) {
                add(environmentPanel);
            }
            add(getInfrastructurePanel(planet, activeDiseases));
            if (planet.getDescription() != null) {
                add(getReferencePanel(planet));
            }
        }
    }

    private Planet getSelectedPlanet() {
        Planet planet = system.getPlanet(planetPos);
        if (planet == null) {
            planet = system.getPrimaryPlanet();
        }
        return planet;
    }

    private Set<InjuryType> getActiveDiseases() {
        LocalDate currentDate = campaign.getLocalDate();
        Set<InjuryType> activeDiseases = new HashSet<>(getAllActiveBioweapons(system.getId(), currentDate, true));
        activeDiseases.addAll(getAllActiveDiseases(system.getId(), currentDate, true));
        return activeDiseases;
    }

    private JPanel createHeader(Planet planet) {
        JPanel header = createRevealBand(0, HEADER_REVEAL_DURATION_NS);
        header.setLayout(new GridBagLayout());
          header.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(12), HORIZONTAL_PADDING,
              UIUtil.scaleForGUI(10), HORIZONTAL_PADDING));
        LocalDate currentDate = campaign.getLocalDate();
        String printableSystemName = system.getPrintableName(currentDate);

        JLabel eyebrow = new JLabel(text("dossier.eyebrow.text"));
        eyebrow.setForeground(DOSSIER_ACCENT);
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, eyebrow.getFont().getSize2D() * 0.85f));
        GridBagConstraints constraints = createFullWidthConstraints(0);
        constraints.insets = new Insets(0, 0, UIUtil.scaleForGUI(3), 0);
        header.add(eyebrow, constraints);

        JLabel systemName = new JLabel(printableSystemName);
        systemName.setForeground(DOSSIER_TEXT);
        systemName.setFont(systemName.getFont().deriveFont(Font.BOLD, systemName.getFont().getSize2D() * 1.35f));
        constraints = createFullWidthConstraints(1);
        constraints.insets = new Insets(0, 0, UIUtil.scaleForGUI(4), 0);
        header.add(systemName, constraints);

        String context;
        if (planet == null) {
            context = text("dossier.systemContext.text");
        } else {
            String printablePlanetName = planet.getPrintableName(currentDate);
            String factionDescription = planet.getFactionDesc(currentDate);
            context = printableSystemName.equals(printablePlanetName)
                  ? factionDescription
                  : format("dossier.planetContext.format", printablePlanetName, factionDescription);
        }
        JLabel planetContext = new JLabel(context);
        planetContext.setForeground(DOSSIER_MUTED_TEXT);
        constraints = createFullWidthConstraints(2);
        header.add(planetContext, constraints);
        return header;
    }

    private JPanel createOperationalSummary(Planet planet, Set<InjuryType> activeDiseases) {
        JPanel summary = createRevealBand(SUMMARY_REVEAL_DELAY_NS, SUMMARY_REVEAL_DURATION_NS);
        summary.setLayout(new GridBagLayout());
        summary.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(UIUtil.scaleForGUI(1), 0, 0, 0, DOSSIER_DIVIDER),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(9), HORIZONTAL_PADDING,
                  UIUtil.scaleForGUI(10), HORIZONTAL_PADDING)));

        JLabel heading = createSectionHeading("section.operationalSummary.text");
        GridBagConstraints constraints = createFullWidthConstraints(0);
        constraints.insets = new Insets(0, 0, UIUtil.scaleForGUI(5), 0);
        summary.add(heading, constraints);

        int metricIndex = 0;
        addMetric(summary, metricIndex++, "summary.starType.text",
              String.valueOf(system.getSourcedStar().getValue()), false);
        addMetric(summary, metricIndex++, "summary.solarRecharge.text",
              system.getRechargeTimeText(campaign.getLocalDate(), false), false);
        addMetric(summary, metricIndex++, "summary.rechargeStations.text",
              system.getRechargeStationsText(campaign.getLocalDate()), false);

        if (planet != null) {
            LocalDate currentDate = campaign.getLocalDate();
            addMetric(summary, metricIndex++, "summary.jumpPoint.text",
                  format("summary.jumpPoint.value", planet.getTimeToJumpPoint(1)), false);
            if (planet.getSourcedHPG(currentDate) != null) {
                addMetric(summary, metricIndex++, "summary.hpg.text",
                    String.valueOf(planet.getSourcedHPG(currentDate).getValue()), false);
            }
            if (planet.getPopulation(currentDate) != null) {
                String population = NumberFormat.getIntegerInstance(MekHQ.getMHQOptions().getLocale())
                                .format(planet.getPopulation(currentDate));
                addMetric(summary, metricIndex++, "summary.population.text", population, false);
            }
            addMetric(summary, metricIndex++, "summary.hiringHall.text", getHiringHallText(planet), false);
        }

        if (!activeDiseases.isEmpty()) {
            addMetric(summary, metricIndex, "summary.healthWarning.text",
                  format("summary.healthWarning.value", activeDiseases.size()), true);
        }
        return summary;
    }

    private void addMetric(JPanel summary, int metricIndex, String labelKey, String value, boolean warning) {
        JPanel metric = createBandPanel();
        metric.setLayout(new BoxLayout(metric, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(text(labelKey));
        label.setForeground(warning ? DOSSIER_WARNING : DOSSIER_MUTED_TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() * 0.8f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        metric.add(label);

        JLabel metricValue = new JLabel(value);
        metricValue.setForeground(warning ? DOSSIER_WARNING : DOSSIER_TEXT);
        metricValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        metric.add(metricValue);

        if (metricIndex % 2 == 0) {
            Dimension preferredSize = metric.getPreferredSize();
            Dimension columnSize = new Dimension(LABEL_COLUMN_WIDTH, preferredSize.height);
            metric.setMinimumSize(columnSize);
            metric.setPreferredSize(columnSize);
        }

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = metricIndex % 2;
        constraints.gridy = 1 + (metricIndex / 2);
        constraints.weightx = (metricIndex % 2 == 0) ? 0.0 : 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
          constraints.insets = new Insets(UIUtil.scaleForGUI(3), 0, UIUtil.scaleForGUI(4),
              (metricIndex % 2 == 0) ? HORIZONTAL_PADDING : 0);
        summary.add(metric, constraints);
    }

    private DossierSection getWorldProfilePanel(Planet planet) {
        DossierSection section = new DossierSection("section.worldProfile.text");
        LocalDate currentDate = campaign.getLocalDate();

        JLabel lblOwner = new JLabel("<html><nobr><i>" +
                                           planet.getFactionDesc(currentDate) +
                                           "</i></nobr></html>");
        section.addRow("lblOwner.text", lblOwner);

        List<String> administration = planet.getAdministration(currentDate);
        if (!administration.isEmpty()) {
            section.addRow("lblAdministration.text",
                  createClippedDossierValue(formatAdministrationPath(administration)));
        }

        CapitalType capitalType = planet.getCapitalType(currentDate);
        if (!capitalType.isNone()) {
            section.addRow("lblCapitalStatus.text", new JLabel(capitalType.getLabel()));
        }

        //Planet type
        SourceableValueLabel txtPlanetType = new SourceableValueLabel(planet.getSourcedPlanetType());
        section.addRow("lblPlanetaryType1.text", txtPlanetType);

        // Diameter
        if (planet.getPlanetType() != PlanetaryType.ASTEROID_BELT) {
            SourceableValueLabel txtDiameter = new SourceableValueLabel(planet.getSourcedDiameter(), "%.1f km");
            section.addRow("lblDiameter.text", txtDiameter);
        }

        // System Position
        if ((null != planet.getSystemPosition()) || (null != planet.getOrbitRadius())) {
            section.addRow("lblPosition.text", getTxtPosition(planet));
        }

        // Time to Jump point
        JLabel txtJumpPoint = new JLabel(Math.round(100 * planet.getTimeToJumpPoint(1)) / 100.0 + " days");
        section.addRow("lblJumpPoint1.text", txtJumpPoint);

        // Year length
        if (null != planet.getSourcedYearLength()) {
            SourceableValueLabel txtYear = new SourceableValueLabel(planet.getSourcedYearLength(), "%s Terran years");
            section.addRow("lblYear1.text", txtYear);
        }

        // Day length
        if (null != planet.getSourcedDayLength(currentDate)) {
            SourceableValueLabel txtDay = new SourceableValueLabel(planet.getSourcedDayLength(currentDate), "%s hours");
            section.addRow("lblDay1.text", txtDay);
        }

        // Satellites
        if ((null != planet.getSatellites()) || (planet.getSmallMoons() > 0) || (planet.hasRing())) {
            SourceableValueLabel txtSatellite;
            String labelKey = "lblSatellite1.text";
            if ((null != planet.getSatellites())) {
                for (Satellite satellite : planet.getSatellites()) {
                    txtSatellite = new SourceableValueLabel(satellite.getSourcedName(),
                          "%s (" + satellite.getSize() + ")");
                    section.addRow(labelKey, txtSatellite);
                    labelKey = null;
                }
            }
            if (planet.getSmallMoons() > 0) {
                txtSatellite = new SourceableValueLabel(planet.getSourcedSmallMoons(), "%s small moons");
                section.addRow(labelKey, txtSatellite);
                labelKey = null;
            }
            if (planet.hasRing()) {
                txtSatellite = new SourceableValueLabel(planet.getSourcedRing(), "dust ring");
                section.addRow(labelKey, txtSatellite);
            }
        }

        return section;
    }

    static String formatAdministrationPath(List<String> administration) {
        return String.join(" > ", administration);
    }

    static JLabel createClippedDossierValue(String text) {
        return new DossierTooltipLabel(text);
    }

    private DossierSection getEnvironmentPanel(Planet planet) {
        DossierSection section = new DossierSection("section.environment.text");
        LocalDate currentDate = campaign.getLocalDate();

        // Gravity
        if (null != planet.getSourcedGravity()) {
            SourceableValueLabel txtGravity = new SourceableValueLabel(planet.getSourcedGravity(), "%sg");
            section.addRow("lblGravity1.text", txtGravity);
        }

        // Atmosphere
        if (null != planet.getSourcedAtmosphere(currentDate)) {
            SourceableValueLabel txtAtmosphere = new SourceableValueLabel(planet.getSourcedAtmosphere(currentDate));
            section.addRow("lblAtmosphere.text", txtAtmosphere);
        }

        // Atmospheric Pressure
        if (null != planet.getSourcedPressure(currentDate)) {
            SourceableValueLabel txtPressure = new SourceableValueLabel(planet.getSourcedPressure(currentDate));
            section.addRow("lblPressure1.text", txtPressure);
        }

        // Atmospheric composition
        if (null != planet.getSourcedComposition(currentDate)) {
            SourceableValueLabel txtComposition = new SourceableValueLabel(planet.getSourcedComposition(currentDate),
                  "<html>%s</html>");
            section.addRow("lblComposition.text", txtComposition);
        }

        // Temperature
        if ((null != planet.getSourcedTemperature(currentDate))) {
            // Using Unicode for the degree symbol as it is required for proper display on certain systems
            SourceableValueLabel txtTemp = new SourceableValueLabel(planet.getSourcedTemperature(currentDate), "%s°C");
            section.addRow("lblTemp1.text", txtTemp);
        }

        // Water
        if (null != planet.getSourcedPercentWater(currentDate)) {
            SourceableValueLabel txtWater = new SourceableValueLabel(planet.getSourcedPercentWater(currentDate),
                  "%s percent");
            section.addRow("lblWater1.text", txtWater);
        }

        // Native life forms
        if (null != planet.getSourcedLifeForm(currentDate)) {
            SourceableValueLabel txtAnimal = new SourceableValueLabel(planet.getSourcedLifeForm(currentDate));
            section.addRow("lblAnimal1.text", txtAnimal);
        }

        return section;
    }

    private DossierSection getInfrastructurePanel(Planet planet, Set<InjuryType> activeDiseases) {
        DossierSection section = new DossierSection("section.infrastructure.text");
        LocalDate currentDate = campaign.getLocalDate();

        // HPG status
        if (null != planet.getSourcedHPG(currentDate)) {
            SourceableValueLabel txtHPG = new SourceableValueLabel(planet.getSourcedHPG(currentDate));
            section.addRow("lblHPG1.text", txtHPG);
        }

        // Hiring Hall Level
        section.addRow("lblHiringHall.text", new JLabel(getHiringHallText(planet)));

        // Landmasses
        if (null != planet.getLandMasses()) {
            SourceableValueLabel txtLandMass;
            String capitalIndent;
            String labelKey = "lblLandMass1.text";
            for (LandMass landmass : planet.getLandMasses()) {
                capitalIndent = "";
                if ((null != landmass.getSourcedName())) {
                    txtLandMass = new SourceableValueLabel(landmass.getSourcedName(), "<html>%s</html>");
                    section.addRow(labelKey, txtLandMass);
                    labelKey = null;
                    capitalIndent = "&nbsp;&nbsp;&nbsp;";
                }
                if ((null != landmass.getSourcedCapital())) {
                    txtLandMass = new SourceableValueLabel(landmass.getSourcedCapital(),
                          "<html>" + capitalIndent + "<i>Capital:</i> %s</html>");
                    section.addRow(labelKey, txtLandMass);
                    labelKey = null;
                }
            }
        }

        // Population
        if (null != planet.getSourcedPopulation(currentDate)) {
            SourceableValueLabel txtPopulation = new SourceableValueLabel(planet.getSourcedPopulation(currentDate),
                  "%,d");
            section.addRow("lblPopulation.text", txtPopulation);
        }

        // Academies
        List<Academy> filteredAcademies = system.getFilteredAcademies(campaign);
        if (!filteredAcademies.isEmpty()) {
            JTextPane txtAcademies = createHtmlTextPane(
                  MarkdownRenderer.getRenderedHtml(system.getAcademiesForSystem(filteredAcademies)));
            section.addStackedRow("lblAcademies.text", txtAcademies);
        }

        // Noteworthy Diseases
        if (!activeDiseases.isEmpty()) {
            JTextPane txtDiseases = createHtmlTextPane(getDiseaseText(activeDiseases));
            section.addStackedRow("lblDiseases.text", txtDiseases);
        }

        // SIC codes
        if (null != planet.getSourcedSocioIndustrial(currentDate)) {
            SocioIndustrialData sid = planet.getSocioIndustrial(currentDate);
            String sidText = (null == sid) ? "" : sid.getHTMLDescription();
            SourceableValueLabel txtSocioIndustrial = new SourceableValueLabel(planet.getSourcedSocioIndustrial(
                  currentDate));
            txtSocioIndustrial.setText(sidText);
            section.addStackedRow("lblSocioIndustrial1.text", txtSocioIndustrial);
        }

        return section;
    }

    private DossierSection getReferencePanel(Planet planet) {
        DossierSection section = new DossierSection("section.reference.text");
        JTextPane txtDesc = createHtmlTextPane(MarkdownRenderer.getRenderedHtml(planet.getDescription()));
        section.addFullWidth(txtDesc);
        return section;
    }

    private static JLabel getTxtPosition(Planet planet) {
        String text;
        if (null != planet.getOrbitRadius()) {
            if (planet.getPlanetType() == PlanetaryType.ASTEROID_BELT) {
                text = String.format("%.3f AU",
                      planet.getOrbitRadius());
            } else {
                text = String.format("%s (%.3f AU)",
                      planet.getDisplayableSystemPosition(), planet.getOrbitRadius());
            }
        } else {
            text = planet.getDisplayableSystemPosition();
        }
        SourceableValueLabel txtPosition = new SourceableValueLabel(planet.getSourcedSystemPosition());
        // replace with our text
        txtPosition.setText(text);
        return txtPosition;
    }

    private String getHiringHallText(Planet planet) {
        return StringUtils.capitalize(planet.getHiringHallLevel(campaign.getLocalDate()).name().toLowerCase());
    }

    private static String getDiseaseText(Set<InjuryType> activeDiseases) {
        StringJoiner diseaseJoiner = new StringJoiner(", ");
        for (InjuryType disease : activeDiseases) {
            diseaseJoiner.add(disease.getSimpleName());
        }
        return diseaseJoiner.toString();
    }

    private JTextPane createHtmlTextPane(String text) {
        JTextPane textPane = new JTextPane();
        HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
        StyleSheet dossierStyleSheet = new StyleSheet();
        dossierStyleSheet.addRule("body { color: #DAE7EB; margin: 0; padding: 0; }");
        dossierStyleSheet.addRule("p { margin: 0 0 0.5em 0; }");
        dossierStyleSheet.addRule("a { color: #41D2E0; }");
        htmlEditorKit.setStyleSheet(dossierStyleSheet);
        textPane.setEditorKit(htmlEditorKit);
        textPane.setDocument(new HTMLDocument(dossierStyleSheet));
        textPane.setEditable(false);
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        textPane.setOpaque(false);
        textPane.setBackground(DOSSIER_BACKGROUND);
        textPane.setForeground(DOSSIER_TEXT);
        textPane.setBorder(BorderFactory.createEmptyBorder());
        textPane.setText(text);
        ((DefaultCaret) textPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        return textPane;
    }

    private JPanel createBandPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private RevealBandPanel createRevealBand(long revealDelay, long revealDuration) {
        RevealBandPanel panel = new RevealBandPanel();
        registerRevealBand(panel, revealDelay, revealDuration);
        return panel;
    }

    private void registerRevealBand(RevealBandPanel panel, long revealDelay, long revealDuration) {
        panel.setRevealTiming(revealDelay, revealDuration);
        panel.setRevealAlpha(animateReveal ? 0.0f : 1.0f);
        revealBands.add(panel);
    }

    private void startRevealAnimation() {
        stopRevealTimer();
        revealStartTime = System.nanoTime();
        revealTimer = new Timer(REVEAL_FRAME_DELAY_MS, event -> updateRevealAnimation());
        revealTimer.setCoalesce(true);
        revealTimer.start();
    }

    private void updateRevealAnimation() {
        long elapsed = System.nanoTime() - revealStartTime;
        boolean allBandsComplete = true;
        for (RevealBandPanel panel : revealBands) {
            float progress = panel.getRevealProgress(elapsed);
            panel.setRevealAlpha(easeOut(progress));
            allBandsComplete &= progress >= 1.0f;
        }
        if (allBandsComplete) {
            finishRevealAnimation();
        }
    }

    private static float easeOut(float progress) {
        float remaining = 1.0f - progress;
        return 1.0f - (remaining * remaining * remaining);
    }

    private void finishRevealAnimation() {
        stopRevealTimer();
        if (revealViewport != null) {
            revealViewport.setScrollMode(previousViewportScrollMode);
            revealViewport = null;
        }
        revealBands.forEach(panel -> panel.setRevealAlpha(1.0f));
        revealComplete = true;
    }

    private void stopRevealTimer() {
        if (revealTimer != null) {
            revealTimer.stop();
            revealTimer = null;
        }
    }

    boolean isRevealAnimationRunning() {
        return revealTimer != null;
    }

    private JLabel createSectionHeading(String headingKey) {
        JLabel heading = new JLabel(text(headingKey));
        heading.setForeground(DOSSIER_ACCENT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D() * 0.85f));
        return heading;
    }

    private static GridBagConstraints createFullWidthConstraints(int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        return constraints;
    }

    private String format(String key, Object... arguments) {
        return MHQInternationalization.getFormattedTextAt(RESOURCE_BUNDLE, key, arguments);
    }

    private static class RevealBandPanel extends JPanel {
        private long revealDelay;
        private long revealDuration;
        private float revealAlpha = 1.0f;

        private RevealBandPanel() {
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        private void setRevealTiming(long revealDelay, long revealDuration) {
            this.revealDelay = revealDelay;
            this.revealDuration = revealDuration;
        }

        private float getRevealProgress(long elapsed) {
            if (elapsed <= revealDelay) {
                return 0.0f;
            }
            if (elapsed >= revealDelay + revealDuration) {
                return 1.0f;
            }
            return (float) (elapsed - revealDelay) / revealDuration;
        }

        private void setRevealAlpha(float revealAlpha) {
            float boundedAlpha = Math.max(0.0f, Math.min(1.0f, revealAlpha));
            if (this.revealAlpha == boundedAlpha) {
                return;
            }
            this.revealAlpha = boundedAlpha;
            repaint();
        }

        @Override
        public void paint(Graphics graphics) {
            if (revealAlpha <= 0.0f) {
                return;
            }
            if (revealAlpha >= 1.0f) {
                super.paint(graphics);
                return;
            }

            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, revealAlpha));
                super.paint(graphics2D);
            } finally {
                graphics2D.dispose();
            }
        }
    }

    private final class DossierSection extends RevealBandPanel {
        private int row = 1;

        private DossierSection(String headingKey) {
            setLayout(new GridBagLayout());
            long revealDelay = SECTION_REVEAL_INITIAL_DELAY_NS +
                                     (detailRevealIndex++ * SECTION_REVEAL_STAGGER_NS);
            registerRevealBand(this, revealDelay, SECTION_REVEAL_DURATION_NS);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(UIUtil.scaleForGUI(1), 0, 0, 0, DOSSIER_DIVIDER),
                BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(9), HORIZONTAL_PADDING,
                    UIUtil.scaleForGUI(10), HORIZONTAL_PADDING)));

            GridBagConstraints constraints = createFullWidthConstraints(0);
            constraints.insets = new Insets(0, 0, UIUtil.scaleForGUI(6), 0);
            add(createSectionHeading(headingKey), constraints);
        }

        private boolean hasContent() {
            return row > 1;
        }

        private void addRow(String labelKey, JComponent value) {
            JLabel label = new JLabel(labelKey == null ? "" : text(labelKey));
            label.setForeground(DOSSIER_MUTED_TEXT);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            Dimension preferredSize = label.getPreferredSize();
            Dimension columnSize = new Dimension(LABEL_COLUMN_WIDTH, preferredSize.height);
            label.setMinimumSize(columnSize);
            label.setPreferredSize(columnSize);

            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = row;
            labelConstraints.anchor = GridBagConstraints.NORTHWEST;
            labelConstraints.insets = new Insets(UIUtil.scaleForGUI(2), 0, UIUtil.scaleForGUI(3),
                UIUtil.scaleForGUI(10));
            add(label, labelConstraints);

            value.setForeground(DOSSIER_TEXT);
            if (value instanceof JTextPane textPane) {
                textPane.setOpaque(false);
            }
            GridBagConstraints valueConstraints = new GridBagConstraints();
            valueConstraints.gridx = 1;
            valueConstraints.gridy = row;
            valueConstraints.weightx = 1.0;
            valueConstraints.fill = GridBagConstraints.HORIZONTAL;
            valueConstraints.anchor = GridBagConstraints.NORTHWEST;
            valueConstraints.insets = new Insets(UIUtil.scaleForGUI(2), 0, UIUtil.scaleForGUI(3), 0);
            add(value, valueConstraints);
            row++;
        }

        private void addStackedRow(String labelKey, JComponent value) {
            JLabel label = new JLabel(text(labelKey));
            label.setForeground(DOSSIER_MUTED_TEXT);
            label.setFont(label.getFont().deriveFont(Font.BOLD));

            GridBagConstraints labelConstraints = createFullWidthConstraints(row++);
            labelConstraints.insets = new Insets(UIUtil.scaleForGUI(5), 0, UIUtil.scaleForGUI(2), 0);
            add(label, labelConstraints);

            value.setForeground(DOSSIER_TEXT);
            GridBagConstraints valueConstraints = createFullWidthConstraints(row++);
            valueConstraints.insets = new Insets(0, 0, UIUtil.scaleForGUI(7), 0);
            add(value, valueConstraints);
        }

        private void addFullWidth(JComponent value) {
            value.setForeground(DOSSIER_TEXT);
            GridBagConstraints constraints = createFullWidthConstraints(row++);
            constraints.insets = new Insets(UIUtil.scaleForGUI(2), 0, UIUtil.scaleForGUI(3), 0);
            add(value, constraints);
        }
    }

    private static final class DossierTooltipLabel extends JLabel {
        private final String fullText;

        private DossierTooltipLabel(String text) {
            super(text);
            fullText = text;
            setToolTipText(text);
            getAccessibleContext().setAccessibleDescription(text);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            return (getWidth() > 0) && (getPreferredSize().width > getWidth()) ? fullText : null;
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tooltip = super.createToolTip();
            tooltip.setBackground(DOSSIER_BACKGROUND);
            tooltip.setForeground(DOSSIER_TEXT);
            tooltip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DOSSIER_ACCENT, UIUtil.scaleForGUI(1)),
                BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(5), UIUtil.scaleForGUI(8),
                    UIUtil.scaleForGUI(5), UIUtil.scaleForGUI(8))));
            return tooltip;
        }
    }

    private static String text(String key) {
        return MHQInternationalization.getTextAt(RESOURCE_BUNDLE, key);
    }
}
