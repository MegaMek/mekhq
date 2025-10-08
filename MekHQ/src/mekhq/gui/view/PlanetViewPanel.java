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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.DefaultCaret;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.universe.LandMass;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Satellite;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.enums.PlanetaryType;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.SourceableValueLabel;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.utilities.MarkdownRenderer;
import org.apache.commons.lang3.StringUtils;

/**
 * A custom panel that gets filled in with goodies from a Planet record
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class PlanetViewPanel extends JScrollablePanel {
    private final PlanetarySystem system;
    private final Campaign campaign;
    private final int planetPos;

    private final Image planetIcon = null;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PlanetViewPanel",
          MekHQ.getMHQOptions().getLocale());

    public PlanetViewPanel(PlanetarySystem s, Campaign c) {
        this(s, c, 0);
    }

    public PlanetViewPanel(PlanetarySystem s, Campaign c, int p) {
        super();
        this.system = s;
        this.campaign = c;
        this.planetPos = p;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel pnlSystem = getSystemPanel();
        pnlSystem.setBorder(RoundedLineBorder.createRoundedLineBorder((system.getPrintableName(campaign.getLocalDate()) +
                                                                             ' ' +
                                                                             resourceMap.getString("system.text"))));
        add(pnlSystem);

        Planet planet = system.getPlanet(planetPos);
        if (null == planet) {
            //try the primary - but still could be null
            planet = system.getPrimaryPlanet();
        }
        if (null != planet) {
            JPanel pnlPlanet = getPlanetPanel(planet);
            pnlPlanet.setBorder(RoundedLineBorder.createRoundedLineBorder((planet.getPrintableName(campaign.getLocalDate()))));
            add(pnlPlanet);
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);

        if (null != planetIcon) {
            Graphics2D gfx = (Graphics2D) g;
            final int width = getWidth();
            final int offset = 6;
            gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Arc2D.Double arc = new Arc2D.Double();
            gfx.setPaint(Color.BLACK);
            arc.setArcByCenter(width - 32 - offset, 32 + offset, 35, 0, 360, Arc2D.OPEN);
            gfx.fill(arc);
            gfx.setPaint(Color.WHITE);
            arc.setArcByCenter(width - 32 - offset, 32 + offset, 34, 0, 360, Arc2D.OPEN);
            gfx.fill(arc);
            gfx.drawImage(planetIcon, width - 64 - offset, offset, 64, 64, null);
        }
    }

    private JPanel getPlanetPanel(Planet planet) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        LocalDate currentDate = campaign.getLocalDate();

        JLabel lblOwner = new JLabel("<html><nobr><i>" +
                                           planet.getFactionDesc(campaign.getLocalDate()) +
                                           "</i></nobr></html>");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panel.add(lblOwner, gridBagConstraints);

        //Set up grid bag constraints
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.gridx = 0;
        gbcLabel.fill = GridBagConstraints.HORIZONTAL;
        gbcLabel.anchor = GridBagConstraints.NORTHWEST;
        GridBagConstraints gbcText = new GridBagConstraints();
        gbcText.gridx = 1;
        gbcText.weightx = 1.0;
        gbcText.insets = new Insets(0, 10, 0, 0);
        gbcText.fill = GridBagConstraints.BOTH;
        gbcText.anchor = GridBagConstraints.NORTHWEST;
        int infoRow = 1;

        //Planet type
        JLabel lblPlanetType = new JLabel(resourceMap.getString("lblPlanetaryType1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblPlanetType, gbcLabel);
        SourceableValueLabel txtPlanetType = new SourceableValueLabel(planet.getSourcedPlanetType());
        gbcText.gridy = infoRow;
        panel.add(txtPlanetType, gbcText);
        ++infoRow;

        //System Position
        if (planet.getPlanetType() != PlanetaryType.ASTEROID_BELT) {
            JLabel lblDiameter = new JLabel(resourceMap.getString("lblDiameter.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblDiameter, gbcLabel);
            SourceableValueLabel txtDiameter = new SourceableValueLabel(planet.getSourcedDiameter(), "%.1f km");
            gbcText.gridy = infoRow;
            panel.add(txtDiameter, gbcText);
            ++infoRow;
        }

        //System Position
        if ((null != planet.getSystemPosition()) || (null != planet.getOrbitRadius())) {
            JLabel lblPosition = new JLabel(resourceMap.getString("lblPosition.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPosition, gbcLabel);
            JLabel txtPosition = getTxtPosition(planet);
            gbcText.gridy = infoRow;
            panel.add(txtPosition, gbcText);
            ++infoRow;
        }

        //Time to Jump point
        JLabel lblJumpPoint = new JLabel(resourceMap.getString("lblJumpPoint1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblJumpPoint, gbcLabel);
        JLabel txtJumpPoint = new JLabel(Math.round(100 * planet.getTimeToJumpPoint(1)) / 100.0 + " days");
        gbcText.gridy = infoRow;
        panel.add(txtJumpPoint, gbcText);
        ++infoRow;

        //Year length
        if (null != planet.getSourcedYearLength()) {
            JLabel lblYear = new JLabel(resourceMap.getString("lblYear1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblYear, gbcLabel);
            SourceableValueLabel txtYear = new SourceableValueLabel(planet.getSourcedYearLength(), "%s Terran years");
            gbcText.gridy = infoRow;
            panel.add(txtYear, gbcText);
            ++infoRow;
        }

        //day length
        if (null != planet.getSourcedDayLength(currentDate)) {
            JLabel lblDay = new JLabel(resourceMap.getString("lblDay1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblDay, gbcLabel);
            SourceableValueLabel txtDay = new SourceableValueLabel(planet.getSourcedDayLength(currentDate), "%s hours");
            gbcText.gridy = infoRow;
            panel.add(txtDay, gbcText);
            ++infoRow;
        }

        //Gravity
        if (null != planet.getSourcedGravity()) {
            JLabel lblGravity = new JLabel(resourceMap.getString("lblGravity1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblGravity, gbcLabel);
            SourceableValueLabel txtGravity = new SourceableValueLabel(planet.getSourcedGravity(), "%sg");
            gbcText.gridy = infoRow;
            panel.add(txtGravity, gbcText);
            ++infoRow;
        }

        //Atmosphere
        if (null != planet.getSourcedAtmosphere(currentDate)) {
            JLabel lblAtmosphere = new JLabel(resourceMap.getString("lblAtmosphere.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblAtmosphere, gbcLabel);
            SourceableValueLabel txtAtmosphere = new SourceableValueLabel(planet.getSourcedAtmosphere(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtAtmosphere, gbcText);
            ++infoRow;
        }

        //Atmospheric Pressure
        if (null != planet.getSourcedPressure(currentDate)) {
            JLabel lblPressure = new JLabel(resourceMap.getString("lblPressure1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPressure, gbcLabel);
            SourceableValueLabel txtPressure = new SourceableValueLabel(planet.getSourcedPressure(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtPressure, gbcText);
            ++infoRow;
        }

        //Atmospheric composition
        if (null != planet.getSourcedComposition(currentDate)) {
            JLabel lblComposition = new JLabel(resourceMap.getString("lblComposition.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblComposition, gbcLabel);
            SourceableValueLabel txtComposition = new SourceableValueLabel(planet.getSourcedComposition(currentDate),
                  "<html>%s</html>");
            gbcText.gridy = infoRow;
            panel.add(txtComposition, gbcText);
            ++infoRow;
        }

        //Temperature
        if ((null != planet.getSourcedTemperature(currentDate))) {
            JLabel lblTemp = new JLabel(resourceMap.getString("lblTemp1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblTemp, gbcLabel);
            //Using Unicode for the degree symbol as it is required for proper display on certain systems
            SourceableValueLabel txtTemp = new SourceableValueLabel(planet.getSourcedTemperature(currentDate), "%s°C");
            gbcText.gridy = infoRow;
            panel.add(txtTemp, gbcText);
            ++infoRow;
        }

        //Water
        if (null != planet.getSourcedPercentWater(currentDate)) {
            JLabel lblWater = new JLabel(resourceMap.getString("lblWater1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblWater, gbcLabel);
            SourceableValueLabel txtWater = new SourceableValueLabel(planet.getSourcedPercentWater(currentDate),
                  "%s percent");
            gbcText.gridy = infoRow;
            panel.add(txtWater, gbcText);
            ++infoRow;
        }

        //native life forms
        if (null != planet.getSourcedLifeForm(currentDate)) {
            JLabel lblAnimal = new JLabel(resourceMap.getString("lblAnimal1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblAnimal, gbcLabel);
            SourceableValueLabel txtAnimal = new SourceableValueLabel(planet.getSourcedLifeForm(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtAnimal, gbcText);
            ++infoRow;
        }

        //satellites
        if ((null != planet.getSatellites()) || (planet.getSmallMoons() > 0) || (planet.hasRing())) {
            JLabel lblSatellite = new JLabel(resourceMap.getString("lblSatellite1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblSatellite, gbcLabel);
            SourceableValueLabel txtSatellite;
            if ((null != planet.getSatellites())) {
                for (Satellite satellite : planet.getSatellites()) {
                    txtSatellite = new SourceableValueLabel(satellite.getSourcedName(),
                          "%s (" + satellite.getSize() + ")");
                    gbcText.gridy = infoRow;
                    panel.add(txtSatellite, gbcText);
                    ++infoRow;
                }
            }
            if (planet.getSmallMoons() > 0) {
                txtSatellite = new SourceableValueLabel(planet.getSourcedSmallMoons(), "%s small moons");
                gbcText.gridy = infoRow;
                panel.add(txtSatellite, gbcText);
                ++infoRow;
            }
            if (planet.hasRing()) {
                txtSatellite = new SourceableValueLabel(planet.getSourcedRing(), "dust ring");
                gbcText.gridy = infoRow;
                panel.add(txtSatellite, gbcText);
                ++infoRow;
            }
        }

        //landmasses
        if (null != planet.getLandMasses()) {
            JLabel lblLandMass = new JLabel(resourceMap.getString("lblLandMass1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblLandMass, gbcLabel);
            SourceableValueLabel txtLandMass;
            String capitalIndent;
            for (LandMass landmass : planet.getLandMasses()) {
                capitalIndent = "";
                if ((null != landmass.getSourcedName())) {
                    txtLandMass = new SourceableValueLabel(landmass.getSourcedName(), "<html>%s</html>");
                    gbcText.gridy = infoRow;
                    panel.add(txtLandMass, gbcText);
                    capitalIndent = "&nbsp;&nbsp;&nbsp;";
                    ++infoRow;
                }
                if ((null != landmass.getSourcedCapital())) {
                    txtLandMass = new SourceableValueLabel(landmass.getSourcedCapital(),
                          "<html>" + capitalIndent + "<i>Capital:</i> %s</html>");
                    gbcText.gridy = infoRow;
                    panel.add(txtLandMass, gbcText);
                    ++infoRow;
                }
            }
        }

        //Population
        if (null != planet.getSourcedPopulation(currentDate)) {
            JLabel lblPopulation = new JLabel(resourceMap.getString("lblPopulation.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPopulation, gbcLabel);
            SourceableValueLabel txtPopulation = new SourceableValueLabel(planet.getSourcedPopulation(currentDate),
                  "%,d");
            gbcText.gridy = infoRow;
            panel.add(txtPopulation, gbcText);
            ++infoRow;
        }

        //SIC codes
        if (null != planet.getSourcedSocioIndustrial(currentDate)) {
            JLabel lblSocioIndustrial = new JLabel(resourceMap.getString("lblSocioIndustrial1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblSocioIndustrial, gbcLabel);
            SocioIndustrialData sid = planet.getSocioIndustrial(currentDate);
            String sidText = (null == sid) ? "" : sid.getHTMLDescription();
            SourceableValueLabel txtSocioIndustrial = new SourceableValueLabel(planet.getSourcedSocioIndustrial(
                  currentDate));
            // replace with greater detail
            txtSocioIndustrial.setText(sidText);
            gbcText.gridy = infoRow;
            panel.add(txtSocioIndustrial, gbcText);
            ++infoRow;
        }

        //HPG status
        if (null != planet.getSourcedHPG(currentDate)) {
            JLabel lblHPG = new JLabel(resourceMap.getString("lblHPG1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblHPG, gbcLabel);
            SourceableValueLabel txtHPG = new SourceableValueLabel(planet.getSourcedHPG(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtHPG, gbcText);
            ++infoRow;
        }

        //Hiring Hall Level
        JLabel lblHiringHall = new JLabel(resourceMap.getString("lblHiringHall.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblHiringHall, gbcLabel);
        JLabel textHiringHall = new JLabel(StringUtils.capitalize(
              planet.getHiringHallLevel(currentDate)
                    .name()
                    .toLowerCase()));
        gbcText.gridy = infoRow;
        panel.add(textHiringHall, gbcText);
        ++infoRow;

        // Academies
        List<Academy> filteredAcademies = system.getFilteredAcademies(campaign);

        if (!filteredAcademies.isEmpty()) {
            ++infoRow;
            JLabel lblAcademies = new JLabel(resourceMap.getString("lblAcademies.text"));
            gbcLabel.gridx = 0;
            gbcLabel.gridy = infoRow;
            panel.add(lblAcademies, gbcLabel);

            JTextPane txtAcademies = new JTextPane();
            txtAcademies.setEditable(false);
            txtAcademies.setContentType("text/html");
            txtAcademies.setText(MarkdownRenderer.getRenderedHtml(system.getAcademiesForSystem(filteredAcademies)));
            ((DefaultCaret) txtAcademies.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 5, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            panel.add(txtAcademies, gridBagConstraints);
            ++infoRow;
        }

        if (null != planet.getDescription()) {
            JTextPane txtDesc = new JTextPane();
            txtDesc.setEditable(false);
            txtDesc.setContentType("text/html");
            txtDesc.setText(MarkdownRenderer.getRenderedHtml(planet.getDescription()));
            ((DefaultCaret) txtDesc.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 5, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            panel.add(txtDesc, gridBagConstraints);
        }

        return panel;
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

    private JPanel getSystemPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        LocalDate currentDate = campaign.getLocalDate();

        //Set up grid bag constraints
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.gridx = 0;
        gbcLabel.fill = GridBagConstraints.NONE;
        gbcLabel.anchor = GridBagConstraints.NORTHWEST;
        GridBagConstraints gbcText = new GridBagConstraints();
        gbcText.gridx = 1;
        gbcText.weightx = 0.5;
        gbcText.insets = new Insets(0, 10, 0, 0);
        gbcText.fill = GridBagConstraints.HORIZONTAL;
        gbcText.anchor = GridBagConstraints.WEST;
        int infoRow = 0;

        //Star Type
        JLabel lblStarType = new JLabel(resourceMap.getString("lblStarType1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblStarType, gbcLabel);
        SourceableValueLabel txtStarType = new SourceableValueLabel(system.getSourcedStar(),
              "%s (" + system.getRechargeTimeText(currentDate, false) + ')');
        gbcText.gridy = infoRow;
        panel.add(txtStarType, gbcText);
        ++infoRow;

        //Recharge Stations
        JLabel lblRecharge = new JLabel(resourceMap.getString("lblRecharge1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblRecharge, gbcLabel);
        JLabel txtRecharge = new JLabel(system.getRechargeStationsText(currentDate));
        gbcText.gridy = infoRow;
        panel.add(txtRecharge, gbcText);

        //TODO: maybe some other summary information, like best HPG and number of planetary systems

        return panel;
    }
}
