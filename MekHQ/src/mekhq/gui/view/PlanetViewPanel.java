/*
 * Copyright (C) 2009-2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.DefaultCaret;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.utilities.MarkdownRenderer;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * A custom panel that gets filled in with goodies from a Planet record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PlanetViewPanel extends JScrollablePanel {
    private static final long serialVersionUID = 7004741688464105277L;

    private PlanetarySystem system;
    private Campaign campaign;
    private int planetPos;

    private JPanel pnlSystem;
    private JPanel pnlPlanet;

    private Image planetIcon = null;

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
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PlanetViewPanel", new EncodeControl()); //$NON-NLS-1
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        pnlSystem = getSystemPanel();
        pnlSystem.setBorder(BorderFactory.createTitledBorder(system.getPrintableName(campaign.getLocalDate()) + " " + resourceMap.getString("system.text")));
        add(pnlSystem);

        Planet planet = system.getPlanet(planetPos);
        if (null == planet) {
            //try the primary - but still could be null
            planet = system.getPrimaryPlanet();
        }
        if (null != planet) {
            pnlPlanet = getPlanetPanel(planet);
            pnlPlanet.setBorder(BorderFactory.createTitledBorder(planet.getPrintableName(campaign.getLocalDate())));
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
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PlanetViewPanel", new EncodeControl()); //$NON-NLS-1
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        LocalDate currentDate = campaign.getLocalDate();

        JLabel lblOwner = new JLabel("<html><nobr><i>" + planet.getFactionDesc(campaign.getLocalDate()) + "</i></nobr></html>");
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
        JLabel txtPlanetType = new JLabel(planet.getPlanetType());
        gbcText.gridy = infoRow;
        panel.add(txtPlanetType, gbcText);
        ++ infoRow;

        //System Position
        if (!"Asteroid Belt".equals(planet.getPlanetType())) {
            JLabel lblDiameter = new JLabel(resourceMap.getString("lblDiameter.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblDiameter, gbcLabel);
            JLabel txtDiameter = new JLabel( String.format("%.1f km", //$NON-NLS-1$
                    planet.getDiameter()));
            gbcText.gridy = infoRow;
            panel.add(txtDiameter, gbcText);
            ++ infoRow;
        }

        //System Position
        if ((null != planet.getSystemPosition()) || (null != planet.getOrbitRadius())) {
            JLabel lblPosition = new JLabel(resourceMap.getString("lblPosition.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPosition, gbcLabel);
            String text = "?";
            if (null != planet.getOrbitRadius()) {
            	if (planet.getPlanetType().equals("Asteroid Belt")) {
            		text = String.format("%.3f AU", //$NON-NLS-1$
            				planet.getOrbitRadius());
            	} else {
            		text = String.format("%s (%.3f AU)", //$NON-NLS-1$
            				planet.getDisplayableSystemPosition(), planet.getOrbitRadius());
            	}
            } else {
                text = planet.getDisplayableSystemPosition();
            }
            JLabel txtPosition = new JLabel(text);
            gbcText.gridy = infoRow;
            panel.add(txtPosition, gbcText);
            ++ infoRow;
        }

        //Time to Jump point
        JLabel lblJumpPoint = new JLabel(resourceMap.getString("lblJumpPoint1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblJumpPoint, gbcLabel);
        JLabel txtJumpPoint = new JLabel(Double.toString(Math.round(100 * planet.getTimeToJumpPoint(1))/100.0) + " days");
        gbcText.gridy = infoRow;
        panel.add(txtJumpPoint, gbcText);
        ++ infoRow;

        //Year length
        if (null != planet.getYearLength()) {
            JLabel lblYear = new JLabel(resourceMap.getString("lblYear1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblYear, gbcLabel);
            JLabel txtYear = new JLabel(Double.toString(planet.getYearLength()) + " Terran years");
            gbcText.gridy = infoRow;
            panel.add(txtYear, gbcText);
            ++ infoRow;
        }

        //day length
        if (null != planet.getDayLength(currentDate)) {
            JLabel lblDay = new JLabel(resourceMap.getString("lblDay1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblDay, gbcLabel);
            JLabel txtDay = new JLabel(Double.toString(planet.getDayLength(currentDate)) + " hours");
            gbcText.gridy = infoRow;
            panel.add(txtDay, gbcText);
            ++ infoRow;
        }

        //Gravity
        if (null != planet.getGravity()) {
            JLabel lblGravity = new JLabel(resourceMap.getString("lblGravity1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblGravity, gbcLabel);
            JLabel txtGravity = new JLabel(planet.getGravityText());
            gbcText.gridy = infoRow;
            panel.add(txtGravity, gbcText);
            ++ infoRow;
        }

        //Atmosphere
        if (null != planet.getAtmosphere(currentDate)) {
            JLabel lblAtmosphere = new JLabel(resourceMap.getString("lblAtmosphere.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblAtmosphere, gbcLabel);
            JLabel txtAtmosphere = new JLabel(planet.getAtmosphereName(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtAtmosphere, gbcText);
            ++ infoRow;
        }

        //Atmospheric Pressure
        if (null != planet.getPressure(currentDate)) {
            JLabel lblPressure = new JLabel(resourceMap.getString("lblPressure1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPressure, gbcLabel);
            JLabel txtPressure = new JLabel(planet.getPressureName(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtPressure, gbcText);
            ++ infoRow;
        }

        //Atmospheric composition
        if (null != planet.getComposition(currentDate)) {
            JLabel lblComposition = new JLabel(resourceMap.getString("lblComposition.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblComposition, gbcLabel);
            JLabel txtComposition = new JLabel("<html>" + planet.getComposition(currentDate) + "</html>");
            gbcText.gridy = infoRow;
            panel.add(txtComposition, gbcText);
            ++ infoRow;
        }

        //Temperature
        if ((null != planet.getTemperature(currentDate))) {
            JLabel lblTemp = new JLabel(resourceMap.getString("lblTemp1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblTemp, gbcLabel);
            //Using Unicode for the degree symbol as it is required for proper display on certain systems
            JLabel txtTemp = new JLabel(planet.getTemperature(currentDate) + "\u00B0" + "C");
            gbcText.gridy = infoRow;
            panel.add(txtTemp, gbcText);
            ++ infoRow;
        }

        //Water
        if (null != planet.getPercentWater(currentDate)) {
            JLabel lblWater = new JLabel(resourceMap.getString("lblWater1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblWater, gbcLabel);
            JLabel txtWater = new JLabel(planet.getPercentWater(currentDate) + " percent");
            gbcText.gridy = infoRow;
            panel.add(txtWater, gbcText);
            ++ infoRow;
        }

        //native life forms
        if (null != planet.getLifeForm(currentDate)) {
            JLabel lblAnimal = new JLabel(resourceMap.getString("lblAnimal1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblAnimal, gbcLabel);
            JLabel txtAnimal = new JLabel(planet.getLifeFormName(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtAnimal, gbcText);
            ++ infoRow;
        }

        //satellites
        if (null != planet.getSatellites() || planet.getSmallMoons()>0) {
        	JLabel lblSatellite = new JLabel(resourceMap.getString("lblSatellite1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblSatellite, gbcLabel);
            JLabel txtSatellite = new JLabel("<html>" + planet.getSatelliteDescription() + "</html>");
            gbcText.gridy = infoRow;
            panel.add(txtSatellite, gbcText);
            ++ infoRow;
        }

        //landmasses
        if (null != planet.getLandMasses()) {
            JLabel lblLandMass = new JLabel(resourceMap.getString("lblLandMass1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblLandMass, gbcLabel);
            JLabel txtLandMass = new JLabel("<html>" + planet.getLandMassDescription() + "</html>");
            gbcText.gridy = infoRow;
            panel.add(txtLandMass, gbcText);
            ++ infoRow;
        }

        //Population
        if (null != planet.getPopulation(currentDate)) {
            JLabel lblPopulation = new JLabel(resourceMap.getString("lblPopulation.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPopulation, gbcLabel);
            JLabel txtPopulation = new JLabel(NumberFormat.getNumberInstance(Locale.getDefault()).format(planet.getPopulation(currentDate)));
            gbcText.gridy = infoRow;
            panel.add(txtPopulation, gbcText);
            ++ infoRow;
        }

        //SIC codes
        if (null != planet.getSocioIndustrial(currentDate)) {
            JLabel lblSocioIndustrial = new JLabel(resourceMap.getString("lblSocioIndustrial1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblSocioIndustrial, gbcLabel);
            SocioIndustrialData sid = planet.getSocioIndustrial(currentDate);
            String sidText = (null == sid) ? "" : sid.getHTMLDescription();
            JLabel txtSocioIndustrial = new JLabel(sidText);
            gbcText.gridy = infoRow;
            panel.add(txtSocioIndustrial, gbcText);
            ++ infoRow;
        }

        //HPG status
        if (null != planet.getHPGClass(currentDate)) {
            JLabel lblHPG = new JLabel(resourceMap.getString("lblHPG1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblHPG, gbcLabel);
            JLabel txtHPG = new JLabel(planet.getHPGClass(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtHPG, gbcText);
            ++ infoRow;
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

    private JPanel getSystemPanel() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PlanetViewPanel", new EncodeControl()); //$NON-NLS-1$
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
        JLabel txtStarType = new JLabel(system.getSpectralTypeText() + " (" + system.getRechargeTimeText(currentDate) + ")");
        gbcText.gridy = infoRow;
        panel.add(txtStarType, gbcText);
        ++ infoRow;

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
