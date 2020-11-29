/*
 * Copyright (C) 2013-2020 - The MegaMek Team. All Rights Reserved.
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

import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Image;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import megamek.common.options.IOption;
import mekhq.MHQStaticDirectoryManager;
import mekhq.Utilities;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.FormerSpouse;
import mekhq.campaign.personnel.enums.GenderDescriptors;

import megamek.common.options.PilotOptions;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.CampaignGUI;
import mekhq.gui.GuiTabType;
import mekhq.gui.dialog.MedicalViewDialog;
import mekhq.gui.model.PersonnelEventLogModel;
import mekhq.gui.model.PersonnelKillLogModel;
import mekhq.gui.utilities.ImageHelpers;
import mekhq.gui.utilities.MarkdownRenderer;
import mekhq.gui.utilities.WrapLayout;

/**
 * A custom panel that gets filled in with goodies from a Person record
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PersonViewPanel extends ScrollablePanel {
    private static final long serialVersionUID = 7004741688464105277L;

    private static final int MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW = 4;

    private final CampaignGUI gui;

    private final Person person;
    private final Campaign campaign;

    ResourceBundle resourceMap;

    public PersonViewPanel(Person p, Campaign c, CampaignGUI gui) {
        this.person = p;
        this.campaign = c;
        this.gui = gui;
        resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonViewPanel", new EncodeControl());
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        setLayout(new GridBagLayout());

        JPanel pnlPortrait = setPortrait();
        GridBagConstraints gbc_pnlPortrait = new GridBagConstraints();
        gbc_pnlPortrait.gridx = 0;
        gbc_pnlPortrait.gridy = 0;
        gbc_pnlPortrait.fill = GridBagConstraints.NONE;
        gbc_pnlPortrait.anchor = GridBagConstraints.NORTHWEST;
        gbc_pnlPortrait.insets = new Insets(10, 10, 0, 0);
        add(pnlPortrait, gbc_pnlPortrait);

        JPanel pnlInfo = fillInfo();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlInfo, gridBagConstraints);

        int gridy = 1;

        JPanel pnlSkills = fillSkills();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlSkills, gridBagConstraints);
        gridy++;

        if (campaign.getCampaignOptions().useAdvancedMedical()) {
            JPanel pnlInjuries = fillInjuries();
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlInjuries, gridBagConstraints);
            gridy++;
        }

        if (person.getGenealogy().hasAnyFamily() || person.getGenealogy().hasFormerSpouse()) {
            JPanel pnlFamily = fillFamily();
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlFamily, gridBagConstraints);
            gridy++;
        }

        if (person.getAwardController().hasAwards()) {
            if (person.getAwardController().hasAwardsWithRibbons()) {
                Box boxRibbons = drawRibbons();

                GridBagConstraints gbc_pnlAllRibbons = new GridBagConstraints();
                gbc_pnlAllRibbons.gridx = 0;
                gbc_pnlAllRibbons.gridy = 1;
                gbc_pnlAllRibbons.fill = GridBagConstraints.NONE;
                gbc_pnlAllRibbons.anchor = GridBagConstraints.NORTHWEST;
                gbc_pnlAllRibbons.insets = new Insets(0, 0, 0, 0);
                pnlPortrait.add(boxRibbons, gbc_pnlAllRibbons);
            }

            JPanel pnlAllAwards = new JPanel();
            pnlAllAwards.setLayout(new BoxLayout(pnlAllAwards, BoxLayout.PAGE_AXIS));
            pnlAllAwards.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlAwards.title")));

            if (person.getAwardController().hasAwardsWithMedals()) {
                JPanel pnlMedals = drawMedals();
                pnlMedals.setName("pnlMedals");
                pnlMedals.setLayout(new WrapLayout(FlowLayout.LEFT));
                pnlAllAwards.add(pnlMedals);
            }

            if (person.getAwardController().hasAwardsWithMiscs()) {
                JPanel pnlMiscAwards = drawMiscAwards();
                pnlMiscAwards.setName("pnlMiscAwards");
                pnlMiscAwards.setLayout(new WrapLayout(FlowLayout.LEFT));
                pnlAllAwards.add(pnlMiscAwards);
            }

            if (person.getAwardController().hasAwardsWithMedals() || person.getAwardController().hasAwardsWithMiscs()) {
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.fill = GridBagConstraints.BOTH;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.insets = new Insets(5, 5, 5, 5);
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = gridy;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                add(pnlAllAwards, gridBagConstraints);
                gridy++;
            }
        }

        if (person.getBiography().length() > 0) {
            JTextPane txtDesc = new JTextPane();
            txtDesc.setName("txtDesc"); //$NON-NLS-1$
            txtDesc.setEditable(false);
            txtDesc.setContentType("text/html");
            txtDesc.setText(MarkdownRenderer.getRenderedHtml(person.getBiography()));
            txtDesc.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(resourceMap.getString("pnlDescription.title")), //$NON-NLS-1$
                    BorderFactory.createEmptyBorder(0, 2, 2, 2)));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(txtDesc, gridBagConstraints);
            gridy++;
        }

        if (person.getPersonnelLog().size() > 0) {
            JPanel pnlLog = fillLog();
            pnlLog.setName("pnlLog"); //$NON-NLS-1$
            pnlLog.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlLog.title"))); //$NON-NLS-1$

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlLog, gridBagConstraints);
            gridy++;
        }

        if (person.getMissionLog().size() > 0) {
            JPanel pnlMissionsLog = fillMissionLog();

            pnlMissionsLog.setName("missionLog"); //$NON-NLS-1$
            pnlMissionsLog.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(resourceMap.getString("missionLog.title")), //$NON-NLS-1$
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlMissionsLog, gridBagConstraints);
            gridy++;
        }

        if (!campaign.getKillsFor(person.getId()).isEmpty()) {
            JPanel pnlKills = fillKillRecord();

            pnlKills.setName("txtKills"); //$NON-NLS-1$
            pnlKills.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(resourceMap.getString("pnlKills.title")), //$NON-NLS-1$
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlKills, gridBagConstraints);
            gridy++;
        }

        // use glue to fill up the remaining space so everything is aligned to the top
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(javax.swing.Box.createGlue(), gridBagConstraints);
    }

    /**
     * Draws the ribbons below the person portrait.
     */
    private Box drawRibbons() {
        Box boxRibbons = Box.createVerticalBox();
        boxRibbons.add(Box.createRigidArea(new Dimension(100, 0)));

        List<Award> awards = person.getAwardController().getAwards().stream().filter(a -> a.getNumberOfRibbonFiles() > 0)
                .sorted().collect(Collectors.toList());
        Collections.reverse(awards);

        int i = 0;
        Box rowRibbonsBox = null;
        ArrayList<Box> rowRibbonsBoxes = new ArrayList<>();

        for (Award award : awards) {
            JLabel ribbonLabel = new JLabel();
            Image ribbon;

            if (i % MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW == 0) {
                rowRibbonsBox = Box.createHorizontalBox();
                rowRibbonsBox.setBackground(Color.RED);
            }
            try {
                int numberOfAwards = person.getAwardController().getNumberOfAwards(award);
                String ribbonFileName = award.getRibbonFileName(numberOfAwards);
                ribbon = (Image) MHQStaticDirectoryManager.getAwardIcons()
                        .getItem(award.getSet() + "/ribbons/", ribbonFileName);
                if (ribbon == null)
                    continue;
                ribbon = ribbon.getScaledInstance(25, 8, Image.SCALE_DEFAULT);
                ribbonLabel.setIcon(new ImageIcon(ribbon));
                ribbonLabel.setToolTipText(award.getTooltip());
                rowRibbonsBox.add(ribbonLabel, 0);
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }

            i++;
            if (i % MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW == 0) {
                rowRibbonsBoxes.add(rowRibbonsBox);
            }
        }
        if (i % MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW != 0) {
            rowRibbonsBoxes.add(rowRibbonsBox);
        }

        Collections.reverse(rowRibbonsBoxes);
        for (Box box : rowRibbonsBoxes) {
            boxRibbons.add(box);
        }

        return boxRibbons;
    }

    /**
     * Draws the medals above the personal log.
     */
    private JPanel drawMedals() {
        JPanel pnlMedals = new JPanel();

        List<Award> awards = person.getAwardController().getAwards().stream().filter(a -> a.getNumberOfMedalFiles() > 0)
                .sorted().collect(Collectors.toList());

        for (Award award : awards) {
            JLabel medalLabel = new JLabel();

            Image medal;
            try {
                int numberOfAwards = person.getAwardController().getNumberOfAwards(award);
                String medalFileName = award.getMedalFileName(numberOfAwards);
                medal = (Image) MHQStaticDirectoryManager.getAwardIcons()
                        .getItem(award.getSet() + "/medals/", medalFileName);
                if (medal == null)
                    continue;
                medal = ImageHelpers.getScaledForBoundaries(medal, new Dimension(30, 60), Image.SCALE_DEFAULT);
                medalLabel.setIcon(new ImageIcon(medal));
                medalLabel.setToolTipText(award.getTooltip());
                pnlMedals.add(medalLabel);
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }

        return pnlMedals;
    }

    /**
     * Draws the misc awards below the medals.
     */
    private JPanel drawMiscAwards() {
        JPanel pnlMiscAwards = new JPanel();
        ArrayList<Award> awards = person.getAwardController().getAwards().stream().filter(a -> a.getNumberOfMiscFiles() > 0)
                .collect(Collectors.toCollection(ArrayList::new));

        for (Award award : awards) {
            JLabel miscLabel = new JLabel();

            Image miscAward;
            try {
                int numberOfAwards = person.getAwardController().getNumberOfAwards(award);
                String miscFileName = award.getMiscFileName(numberOfAwards);
                Image miscAwardBufferedImage = (Image) MHQStaticDirectoryManager.getAwardIcons()
                        .getItem(award.getSet() + "/misc/", miscFileName);
                if (miscAwardBufferedImage == null)
                    continue;
                miscAward = ImageHelpers.getScaledForBoundaries(miscAwardBufferedImage, new Dimension(100, 100),
                        Image.SCALE_DEFAULT);
                miscLabel.setIcon(new ImageIcon(miscAward));
                miscLabel.setToolTipText(award.getTooltip());
                pnlMiscAwards.add(miscLabel);
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
        return pnlMiscAwards;
    }

    /**
     * set the portrait for the given person.
     *
     * @return The <code>Image</code> of the pilot's portrait. This value will be
     *         <code>null</code> if no portrait was selected or if there was an
     *         error loading it.
     */
    public JPanel setPortrait() {
        JPanel pnlPortrait = new JPanel();

        // Panel portrait will include the person picture and the ribbons
        pnlPortrait.setName("pnlPortrait");
        pnlPortrait.setLayout(new GridBagLayout());

        JLabel lblPortrait = new JLabel();
        lblPortrait.setName("lblPortrait");

        lblPortrait.setIcon(person.getPortrait().getImageIcon(100));

        GridBagConstraints gbc_lblPortrait = new GridBagConstraints();
        gbc_lblPortrait.gridx = 0;
        gbc_lblPortrait.gridy = 0;
        gbc_lblPortrait.fill = GridBagConstraints.NONE;
        gbc_lblPortrait.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblPortrait.insets = new Insets(0, 0, 0, 0);
        pnlPortrait.add(lblPortrait, gbc_lblPortrait);

        return pnlPortrait;
    }

    private JPanel fillInfo() {
        JPanel pnlInfo = new JPanel(new GridBagLayout());
        pnlInfo.setBorder(BorderFactory.createTitledBorder(person.getFullTitle()));
        JLabel lblType = new JLabel();
        JLabel lblStatus1 = new JLabel();
        JLabel lblStatus2 = new JLabel();
        JLabel lblOrigin1 = new JLabel();
        JLabel lblOrigin2 = new JLabel();
        JLabel lblCall1 = new JLabel();
        JLabel lblCall2 = new JLabel();
        JLabel lblAge1 = new JLabel();
        JLabel lblAge2 = new JLabel();
        JLabel lblGender1 = new JLabel();
        JLabel lblGender2 = new JLabel();
        JLabel lblDueDate1 = new JLabel();
        JLabel lblDueDate2 = new JLabel();
        JLabel lblRecruited1 = new JLabel();
        JLabel lblRecruited2 = new JLabel();
        JLabel lblTimeServed1 = new JLabel();
        JLabel lblTimeServed2 = new JLabel();

        GridBagConstraints gridBagConstraints;

        int firsty = 0, secondy = 0;

        lblType.setName("lblType");
        lblType.setText(String.format(resourceMap.getString("format.italic"), person.getRoleDesc()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblType, gridBagConstraints);
        firsty++;

        lblStatus1.setName("lblStatus1");
        lblStatus1.setText(resourceMap.getString("lblStatus1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblStatus1, gridBagConstraints);

        lblStatus2.setName("lblStatus2");
        lblStatus2.setText(person.getStatus().toString() + person.pregnancyStatus());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblStatus2, gridBagConstraints);
        firsty++;

        if (campaign.getCampaignOptions().showOriginFaction()) {
            lblOrigin1.setName("lblOrigin1");
            lblOrigin1.setText(resourceMap.getString("lblOrigin1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblOrigin1, gridBagConstraints);

            lblOrigin2.setName("lblOrigin2");
            String factionName = person.getOriginFaction().getFullName(campaign.getGameYear());
            if (person.getOriginPlanet() != null) {
                String planetName = person.getOriginPlanet().getName(campaign.getLocalDate());
                lblOrigin2.setText(String.format("<html><a href='#'>%s</a> (%s)</html>", planetName, factionName));
                lblOrigin2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                lblOrigin2.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        PlanetarySystem system = person.getOriginPlanet().getParentSystem();
                        // Stay on the interstellar map if their origin planet is the primary planet...
                        if (system.getPrimaryPlanet().equals(person.getOriginPlanet())) {
                            gui.getMapTab().switchSystemsMap(system);
                        } else {
                            // ...otherwise, dive on in to the system view!
                            gui.getMapTab().switchPlanetaryMap(person.getOriginPlanet());
                        }
                        gui.setSelectedTab(GuiTabType.MAP);
                    }
                });
            } else {
                lblOrigin2.setText(factionName);
            }
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblOrigin2, gridBagConstraints);
            firsty++;
        }

        if (!person.getCallsign().equals("-") && (person.getCallsign().length() > 0)) {
            lblCall1.setName("lblCall1");
            lblCall1.setText(resourceMap.getString("lblCall1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblCall1, gridBagConstraints);

            lblCall2.setName("lblCall2");
            lblCall2.setText(person.getCallsign());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblCall2, gridBagConstraints);
            firsty++;
        }

        lblAge1.setName("lblAge1");
        lblAge1.setText(resourceMap.getString("lblAge1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblAge1, gridBagConstraints);

        lblAge2.setName("lblAge2");
        lblAge2.setText(Integer.toString(person.getAge(campaign.getLocalDate())));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblAge2, gridBagConstraints);
        firsty++;
        secondy = firsty;

        lblGender1.setName("lblGender1");
        lblGender1.setText(resourceMap.getString("lblGender1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblGender1, gridBagConstraints);

        lblGender2.setName("lblGender2");
        lblGender2.setText(GenderDescriptors.MALE_FEMALE.getDescriptorCapitalized(person.getGender()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInfo.add(lblGender2, gridBagConstraints);
        firsty++;

        if (person.isPregnant()) {
            lblDueDate1.setName("lblDueDate1");
            lblDueDate1.setText(resourceMap.getString("lblDueDate1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblDueDate1, gridBagConstraints);

            String dueDate = MekHQ.getMekHQOptions().getDisplayFormattedDate(
                    campaign.getCampaignOptions().getDisplayTrueDueDate()
                            ? person.getDueDate()
                            : person.getExpectedDueDate());

            lblDueDate2.setName("lblDueDate2");
            lblDueDate2.setText(dueDate);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblDueDate2, gridBagConstraints);
            firsty++;
        }

        if (person.getRetirement() != null) {
            JLabel lblRetirement1 = new JLabel(resourceMap.getString("lblRetirement1.text"));
            lblRetirement1.setName("lblRetirement1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblRetirement1, gridBagConstraints);

            JLabel lblRetirement2 = new JLabel(person.getRetirementAsString());
            lblRetirement2.setName("lblRetirement2");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblRetirement2, gridBagConstraints);
            firsty++;
        }

        // We show the following if track total earnings is on for a free non-dependent, or if the
        // person has tracked total earnings
        if (campaign.getCampaignOptions().trackTotalEarnings()
                && ((person.getPrisonerStatus().isFree() && !person.isDependent())
                || person.getTotalEarnings().isGreaterThan(Money.zero()))) {
            JLabel lblTotalEarnings1 = new JLabel(resourceMap.getString("lblTotalEarnings1.text"));
            lblTotalEarnings1.setName("lblTotalEarnings1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTotalEarnings1, gridBagConstraints);

            JLabel lblTotalEarnings2 = new JLabel(person.getTotalEarnings().toAmountAndSymbolString());
            lblTotalEarnings2.setName("lblTotalEarnings2");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTotalEarnings2, gridBagConstraints);
            firsty++;
        }

        if (person.getRecruitment() != null) {
            lblRecruited1.setName("lblRecruited1");
            lblRecruited1.setText(resourceMap.getString("lblRecruited1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblRecruited1, gridBagConstraints);

            lblRecruited2.setName("lblRecruited2");
            lblRecruited2.setText(person.getRecruitmentAsString());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblRecruited2, gridBagConstraints);
            secondy++;

            lblTimeServed1.setName("lblTimeServed1");
            lblTimeServed1.setText(resourceMap.getString("lblTimeServed1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTimeServed1, gridBagConstraints);

            lblTimeServed2.setName("lblTimeServed2");
            lblTimeServed2.setText(person.getTimeInService(campaign));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTimeServed2, gridBagConstraints);
            secondy++;
        }

        if (person.getLastRankChangeDate() != null) {
            JLabel lblLastRankChangeDate1 = new JLabel(resourceMap.getString("lblLastRankChangeDate1.text"));
            lblLastRankChangeDate1.setName("lblLastRankChangeDate1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblLastRankChangeDate1, gridBagConstraints);

            JLabel lblLastRankChangeDate2 = new JLabel(person.getLastRankChangeDateAsString());
            lblLastRankChangeDate2.setName("lblLastRankChangeDate2");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblLastRankChangeDate2, gridBagConstraints);
            secondy++;

            JLabel lblTimeInRank1 = new JLabel(resourceMap.getString("lblTimeInRank1.text"));
            lblTimeInRank1.setName("lblTimeInRank1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTimeInRank1, gridBagConstraints);

            JLabel lblTimeInRank2 = new JLabel(person.getTimeInRank(campaign));
            lblTimeInRank2.setName("lblTimeInRank2");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInfo.add(lblTimeInRank2, gridBagConstraints);
            secondy++;
        }

        return pnlInfo;
    }

    private JPanel fillFamily() {
        JPanel pnlFamily = new JPanel(new GridBagLayout());
        pnlFamily.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlFamily.title")));

        //family panel
        JLabel lblSpouse2 = new JLabel();
        JLabel lblFormerSpouses1 = new JLabel();
        JLabel lblFormerSpouses2;
        JLabel lblChildren1 = new JLabel();
        JLabel lblChildren2;
        JLabel lblGrandchildren1 = new JLabel();
        JLabel lblGrandchildren2;
        JLabel lblSiblings1 = new JLabel();
        JLabel lblSiblings2;
        JLabel lblGrandparents1 = new JLabel();
        JLabel lblGrandparents2;
        JLabel lblAuntsOrUncles1 = new JLabel();
        JLabel lblAuntsOrUncles2;
        JLabel lblCousins1 = new JLabel();
        JLabel lblCousins2;

        GridBagConstraints gridBagConstraints;

        int firsty = 0;

        Person spouse = person.getGenealogy().getSpouse(campaign);
        if (spouse != null) {
            JLabel lblSpouse1 = new JLabel(resourceMap.getString("lblSpouse1.text"));
            lblSpouse1.setName("lblSpouse1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlFamily.add(lblSpouse1, gridBagConstraints);

            lblSpouse2.setName("lblSpouse2");
            lblSpouse2.setText(String.format("<html>%s</html>", spouse.getHyperlinkedName()));
            lblSpouse2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblSpouse2.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    gui.getPersonnelTab().focusOnPerson(spouse.getId());
                }
            });
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            pnlFamily.add(lblSpouse2, gridBagConstraints);
            firsty++;
        }

        if (person.getGenealogy().hasFormerSpouse()) {
            lblFormerSpouses1.setName("lblFormerSpouses1");
            lblFormerSpouses1.setText(resourceMap.getString("lblFormerSpouses1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlFamily.add(lblFormerSpouses1, gridBagConstraints);

            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);

            for (FormerSpouse formerSpouse : person.getGenealogy().getFormerSpouses()) {
                Person ex = campaign.getPerson(formerSpouse.getFormerSpouseId());
                gridBagConstraints.gridy = firsty;
                lblFormerSpouses2 = new JLabel();
                lblFormerSpouses2.setName("lblFormerSpouses2");
                lblFormerSpouses2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                lblFormerSpouses2.setText(String.format("<html><a href='#'>%s</a>, %s, %s</html>",
                        ex.getFullName(), formerSpouse.getReasonString(),
                        MekHQ.getMekHQOptions().getDisplayFormattedDate(formerSpouse.getDate())));
                lblFormerSpouses2.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        gui.getPersonnelTab().focusOnPerson(ex.getId());
                    }
                });
                pnlFamily.add(lblFormerSpouses2, gridBagConstraints);
                firsty++;
            }
        }

        if (campaign.getCampaignOptions().getDisplayFamilyLevel().displayExtendedFamily()) {
            List<UUID> children = person.getGenealogy().getChildren();
            if (!children.isEmpty() && campaign.getCampaignOptions().getDisplayFamilyLevel().displayParentsChildrenSiblings()) {
                lblChildren1.setName("lblChildren1");
                lblChildren1.setText(resourceMap.getString("lblChildren1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblChildren1, gridBagConstraints);

                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);

                for (UUID childId : children) {
                    Person child = campaign.getPerson(childId);

                    gridBagConstraints.gridy = firsty;
                    lblChildren2 = new JLabel();
                    lblChildren2.setName("lblChildren2");
                    lblChildren2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblChildren2.setText(String.format("<html><a href='#'>%s</a></html>", child.getFullName()));
                    lblChildren2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.getPersonnelTab().focusOnPerson(childId);
                        }
                    });
                    pnlFamily.add(lblChildren2, gridBagConstraints);
                    firsty++;
                }
            }

            List<UUID> grandchildren = person.getGenealogy().getGrandchildren(campaign);
            if (!grandchildren.isEmpty() && campaign.getCampaignOptions().getDisplayFamilyLevel().displayGrandparentsGrandchildren()) {
                lblGrandchildren1.setName("lblGrandchildren1");
                lblGrandchildren1.setText(resourceMap.getString("lblGrandchildren1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblGrandchildren1, gridBagConstraints);

                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

                for (UUID grandchildId : grandchildren) {
                    Person grandchild = campaign.getPerson(grandchildId);

                    gridBagConstraints.gridy = firsty;
                    lblGrandchildren2 = new JLabel();
                    lblGrandchildren2.setName("lblGrandchildren2");
                    lblGrandchildren2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblGrandchildren2.setText(String.format("<html><a href='#'>%s</a></html>", grandchild.getFullName()));
                    lblGrandchildren2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.getPersonnelTab().focusOnPerson(grandchildId);
                        }
                    });
                    pnlFamily.add(lblGrandchildren2, gridBagConstraints);
                    firsty++;
                }
            }

            if (campaign.getCampaignOptions().getDisplayFamilyLevel().displayParentsChildrenSiblings()) {
                List<UUID> parents = person.getGenealogy().getParents();
                for (UUID parentId : parents) {
                    Person parent = campaign.getPerson(parentId);

                    JLabel labelParent = new JLabel(resourceMap.getString(parent.getGender().isMale()
                            ? "lblFather1.text" : "lblMother1.text"));
                    labelParent.setName("lblParent");
                    gridBagConstraints = new GridBagConstraints();
                    gridBagConstraints.gridx = 0;
                    gridBagConstraints.gridy = firsty;
                    gridBagConstraints.fill = GridBagConstraints.NONE;
                    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                    pnlFamily.add(labelParent, gridBagConstraints);

                    JLabel labelParentName = new JLabel(String.format("<html>%s</html>", parent.getHyperlinkedName()));
                    labelParentName.setName("lblParentName");
                    labelParentName.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    labelParentName.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.getPersonnelTab().focusOnPerson(parentId);
                        }
                    });
                    gridBagConstraints.gridx = 1;
                    gridBagConstraints.weightx = 1.0;
                    gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                    pnlFamily.add(labelParentName, gridBagConstraints);
                    firsty++;
                }
            }

            List<UUID> siblings = person.getGenealogy().getSiblings(campaign);
            if (!siblings.isEmpty() && campaign.getCampaignOptions().getDisplayFamilyLevel().displayParentsChildrenSiblings()) {
                lblSiblings1.setName("lblSiblings1");
                lblSiblings1.setText(resourceMap.getString("lblSiblings1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblSiblings1, gridBagConstraints);

                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

                for (UUID siblingId : siblings) {
                    Person sibling = campaign.getPerson(siblingId);

                    gridBagConstraints.gridy = firsty;
                    lblSiblings2 = new JLabel(String.format("<html>%s</html>", sibling.getHyperlinkedName()));
                    lblSiblings2.setName("lblSiblings2");
                    lblSiblings2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblSiblings2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.getPersonnelTab().focusOnPerson(siblingId);
                        }
                    });
                    pnlFamily.add(lblSiblings2, gridBagConstraints);
                    firsty++;
                }
            }

            List<UUID> grandparents = person.getGenealogy().getGrandparents(campaign);
            if (!grandparents.isEmpty() && campaign.getCampaignOptions().getDisplayFamilyLevel().displayGrandparentsGrandchildren()) {
                lblGrandparents1.setName("lblGrandparents1");
                lblGrandparents1.setText(resourceMap.getString("lblGrandparents1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblGrandparents1, gridBagConstraints);

                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

                for (UUID grandparentId : grandparents) {
                    Person grandparent = campaign.getPerson(grandparentId);

                    gridBagConstraints.gridy = firsty;
                    lblGrandparents2 = new JLabel(String.format("<html>%s</html>",
                            grandparent.getHyperlinkedName()));
                    lblGrandparents2.setName("lblGrandparents2");
                    lblGrandparents2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblGrandparents2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.getPersonnelTab().focusOnPerson(grandparentId);
                        }
                    });
                    pnlFamily.add(lblGrandparents2, gridBagConstraints);
                    firsty++;
                }
            }

            List<UUID> auntsAndUncles = person.getGenealogy().getsAuntsAndUncles(campaign);
            if (!auntsAndUncles.isEmpty() && campaign.getCampaignOptions().getDisplayFamilyLevel().displayAuntsUnclesCousins()) {
                lblAuntsOrUncles1.setName("lblAuntsOrUncles1");
                lblAuntsOrUncles1.setText(resourceMap.getString("lblAuntsOrUncles1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblAuntsOrUncles1, gridBagConstraints);

                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

                for (UUID auntOrUncleId : auntsAndUncles) {
                    Person auntOrUncle = campaign.getPerson(auntOrUncleId);

                    gridBagConstraints.gridy = firsty;
                    lblAuntsOrUncles2 = new JLabel(String.format("<html>%s</html>",
                            auntOrUncle.getHyperlinkedName()));
                    lblAuntsOrUncles2.setName("lblAuntsOrUncles2");
                    lblAuntsOrUncles2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblAuntsOrUncles2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.getPersonnelTab().focusOnPerson(auntOrUncleId);
                        }
                    });
                    pnlFamily.add(lblAuntsOrUncles2, gridBagConstraints);
                    firsty++;
                }
            }

            List<UUID> cousins = person.getGenealogy().getCousins(campaign);
            if (!cousins.isEmpty() && campaign.getCampaignOptions().getDisplayFamilyLevel().displayAuntsUnclesCousins()) {
                lblCousins1.setName("lblCousins1");
                lblCousins1.setText(resourceMap.getString("lblCousins1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlFamily.add(lblCousins1, gridBagConstraints);

                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

                for (UUID cousinId : cousins) {
                    Person cousin = campaign.getPerson(cousinId);

                    gridBagConstraints.gridy = firsty;
                    lblCousins2 = new JLabel();
                    lblCousins2.setName("lblCousins2");
                    lblCousins2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblCousins2.setText(String.format("<html>%s</html>", cousin.getHyperlinkedName()));
                    lblCousins2.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            gui.getPersonnelTab().focusOnPerson(cousinId);
                        }
                    });
                    pnlFamily.add(lblCousins2, gridBagConstraints);
                    firsty++;
                }
            }
        }

        return pnlFamily;
    }

    private JPanel fillSkills() {

        //skill panel
        JPanel pnlSkills = new JPanel(new GridBagLayout());
        pnlSkills.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlSkills.title")));

        //abilities and implants
        JLabel lblTough1 = new JLabel();
        JLabel lblTough2 = new JLabel();
        JLabel lblEdge1 = new JLabel();
        JLabel lblEdge2 = new JLabel();
        JLabel lblEdgeAvail1 = new JLabel();
        JLabel lblEdgeAvail2 = new JLabel();

        GridBagConstraints gridBagConstraints;

        JLabel lblName;
        JLabel lblValue;

        int firsty=0;
        int colBreak = Math.max((int) Math.ceil(person.getSkillNumber() / 2.0)+1, 3);
        int addition = 0;
        double weight = 0.5;

        int j = 0;
        for (int i = 0; i < SkillType.getSkillList().length; i++) {
            if (person.hasSkill(SkillType.getSkillList()[i])) {
                j++;
                if (j == colBreak) {
                    addition = 2;
                    firsty = 0;
                    weight = 1.0;
                }
                lblName = new JLabel(String.format(resourceMap.getString("format.itemHeader"), SkillType.getSkillList()[i]));
                lblValue = new JLabel(person.getSkill(SkillType.getSkillList()[i]).toString());
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = addition;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlSkills.add(lblName, gridBagConstraints);
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1 + addition;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.weightx = weight;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlSkills.add(lblValue, gridBagConstraints);
                firsty++;
            }
        }

        //reset firsty
        firsty = colBreak;

        if (campaign.getCampaignOptions().useAbilities() && (person.countOptions(PilotOptions.LVL3_ADVANTAGES) > 0)) {
            JLabel lblAbility1 = new JLabel(resourceMap.getString("lblAbility1.text"));
            lblAbility1.setName("lblAbility1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlSkills.add(lblAbility1, gridBagConstraints);

            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);

            for (Enumeration<IOption> i = person.getOptions(PilotOptions.LVL3_ADVANTAGES); i.hasMoreElements();) {
                IOption option = i.nextElement();
                if (option.booleanValue()) {
                    JLabel lblAbility2 = new JLabel(Utilities.getOptionDisplayName(option));
                    lblAbility2.setToolTipText(option.getDescription());
                    lblAbility2.setName("lblAbility2");
                    gridBagConstraints.gridy = firsty++;
                    pnlSkills.add(lblAbility2, gridBagConstraints);
                }
            }
        }

        if (campaign.getCampaignOptions().useImplants() && (person.countOptions(PilotOptions.MD_ADVANTAGES) > 0)) {
            JLabel lblImplants1 = new JLabel(resourceMap.getString("lblImplants1.text"));
            lblImplants1.setName("lblImplants1");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlSkills.add(lblImplants1, gridBagConstraints);

            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);

            for (Enumeration<IOption> i = person.getOptions(PilotOptions.MD_ADVANTAGES); i.hasMoreElements();) {
                IOption option = i.nextElement();

                if (option.booleanValue()) {
                    JLabel lblImplants2 = new JLabel(Utilities.getOptionDisplayName(option));
                    lblImplants2.setToolTipText(option.getDescription());
                    lblImplants2.setName("lblImplants2");
                    gridBagConstraints.gridy = firsty++;
                    pnlSkills.add(lblImplants2, gridBagConstraints);
                }
            }
        }

        if (campaign.getCampaignOptions().useEdge() && (person.getEdge() > 0)) {
            lblEdge1.setName("lblEdge1");
            lblEdge1.setText(resourceMap.getString("lblEdge1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlSkills.add(lblEdge1, gridBagConstraints);

            lblEdge2.setName("lblEdge2");
            lblEdge2.setText(Integer.toString(person.getEdge()));
            lblEdge2.setToolTipText(person.getEdgeTooltip());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlSkills.add(lblEdge2, gridBagConstraints);

            if (campaign.getCampaignOptions().useSupportEdge() && person.hasSupportRole(false)) {
                //Add the Edge Available field for support personnel only
                lblEdgeAvail1.setName("lblEdgeAvail1");
                lblEdgeAvail1.setText(resourceMap.getString("lblEdgeAvail1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlSkills.add(lblEdgeAvail1, gridBagConstraints);

                lblEdgeAvail2.setName("lblEdgeAvail2");
                lblEdgeAvail2.setText(Integer.toString(person.getCurrentEdge()));
                gridBagConstraints.gridx = 3;
                gridBagConstraints.gridwidth = 1;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                pnlSkills.add(lblEdgeAvail2, gridBagConstraints);
            }
            firsty++;
        }

        if (campaign.getCampaignOptions().useToughness()) {
            lblTough1.setName("lblTough1");
            lblTough1.setText(resourceMap.getString("lblTough1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlSkills.add(lblTough1, gridBagConstraints);

            lblTough2.setName("lblTough2");
            lblTough2.setText("+" + person.getToughness());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlSkills.add(lblTough2, gridBagConstraints);
        }

        return pnlSkills;
    }

    private JPanel fillLog() {
        List<LogEntry> logs = person.getPersonnelLog();

        JPanel pnlLog = new JPanel(new GridBagLayout());

        PersonnelEventLogModel eventModel = new PersonnelEventLogModel();
        eventModel.setData(logs);
        JTable eventTable = new JTable(eventModel);
        eventTable.setRowSelectionAllowed(false);
        eventTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn column;
        for (int i = 0; i < eventModel.getColumnCount(); ++ i) {
            column = eventTable.getColumnModel().getColumn(i);
            column.setCellRenderer(eventModel.getRenderer());
            column.setPreferredWidth(eventModel.getPreferredWidth(i));
            if (eventModel.hasConstantWidth(i)) {
                column.setMinWidth(eventModel.getPreferredWidth(i));
                column.setMaxWidth(eventModel.getPreferredWidth(i));
            }
        }
        eventTable.setIntercellSpacing(new Dimension(0, 0));
        eventTable.setShowGrid(false);
        eventTable.setTableHeader(null);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

        pnlLog.add(eventTable, gridBagConstraints);

        return pnlLog;
    }

    private JPanel fillMissionLog() {
        List<LogEntry> missionLog = person.getMissionLog();

        JPanel pnlMissionsLog = new JPanel(new GridBagLayout());

        JLabel lblMissions = new JLabel(String.format(resourceMap.getString("format.missions"), missionLog.size()));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlMissionsLog.add(lblMissions, gridBagConstraints);

        PersonnelEventLogModel eventModel = new PersonnelEventLogModel();
        eventModel.setData(missionLog);
        JTable missionsTable = new JTable(eventModel);
        missionsTable.setRowSelectionAllowed(false);
        missionsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn column;
        for (int i = 0; i < eventModel.getColumnCount(); ++i) {
            column = missionsTable.getColumnModel().getColumn(i);
            column.setCellRenderer(eventModel.getRenderer());
            column.setPreferredWidth(eventModel.getPreferredWidth(i));
            if (eventModel.hasConstantWidth(i)) {
                column.setMinWidth(eventModel.getPreferredWidth(i));
                column.setMaxWidth(eventModel.getPreferredWidth(i));
            }
        }
        missionsTable.setIntercellSpacing(new Dimension(0, 0));
        missionsTable.setShowGrid(false);
        missionsTable.setTableHeader(null);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlMissionsLog.add(missionsTable, gridBagConstraints);

        return pnlMissionsLog;
    }

    private JPanel fillInjuries() {
        JPanel pnlInjuries = new JPanel(new BorderLayout());
        pnlInjuries.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlInjuries.title")));

        JButton medicalButton = new JButton(new ImageIcon("data/images/misc/medical.png"));
        medicalButton.addActionListener(event -> {
            MedicalViewDialog medDialog = new MedicalViewDialog(SwingUtilities.getWindowAncestor(this), campaign, person);
            medDialog.setModalityType(ModalityType.APPLICATION_MODAL);
            medDialog.setVisible(true);
            removeAll();
            repaint();
            revalidate();
            initComponents();
            revalidate();
            MekHQ.triggerEvent(new PersonChangedEvent(person));
        });
        medicalButton.setMaximumSize(new Dimension(32, 32));
        medicalButton.setMargin(new Insets(0, 0, 0, 0));
        medicalButton.setToolTipText(resourceMap.getString("btnMedical.tooltip"));
        medicalButton.setAlignmentY(Component.TOP_ALIGNMENT);
        pnlInjuries.add(medicalButton, BorderLayout.LINE_START);

        JPanel pnlInjuryDetails = new JPanel(new GridBagLayout());
        pnlInjuryDetails.setAlignmentY(Component.TOP_ALIGNMENT);


        JLabel lblAdvancedMedical1 = new JLabel();
        JLabel lblAdvancedMedical2 = new JLabel();

        GridBagConstraints gridBagConstraints;

        lblAdvancedMedical1.setName("lblAdvancedMedical1");
        lblAdvancedMedical1.setText(resourceMap.getString("lblAdvancedMedical1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInjuryDetails.add(lblAdvancedMedical1, gridBagConstraints);

        double vweight = 1.0;
        if (person.hasInjuries(false)) {
        	vweight = 0.0;
        }

        lblAdvancedMedical2.setName("lblAdvancedMedical2");
        lblAdvancedMedical2.setText(getAdvancedMedalEffectString(person));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = vweight;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlInjuryDetails.add(lblAdvancedMedical2, gridBagConstraints);

        JLabel lblInjury;
        JLabel txtInjury;
        int row = 1;
        List<Injury> injuries = person.getInjuries();
        for (Injury injury : injuries) {
            lblInjury = new JLabel(injury.getFluff());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = row;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInjuryDetails.add(lblInjury, gridBagConstraints);

            String text = (injury.isPermanent() && injury.getTime() < 1)
                    ? resourceMap.getString("lblPermanentInjury.text")
                    : String.format(resourceMap.getString("format.injuryTime"), injury.getTime());
            txtInjury = new JLabel("<html>" + text + "</html>");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = row;
            gridBagConstraints.weightx = 1.0;
            if (row == (injuries.size() - 1)) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new Insets(0, 20, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInjuryDetails.add(txtInjury, gridBagConstraints);
            row++;
        }

        pnlInjuries.add(pnlInjuryDetails, BorderLayout.CENTER);

        return pnlInjuries;
    }

    /**
     * Gets the advanced medical effects active for the person.
     * @return an HTML encoded string of effects
     */
    private String getAdvancedMedalEffectString(Person p) {
        StringBuilder sb = new StringBuilder("<html>");
        final int pilotingMod = p.getPilotingInjuryMod();
        final int gunneryMod = p.getGunneryInjuryMod();
        if ((pilotingMod != 0) && (pilotingMod < Integer.MAX_VALUE)) {
            sb.append(String.format("  Piloting %+d <br>", pilotingMod));
        } else if (pilotingMod == Integer.MAX_VALUE) {
            sb.append("  Piloting: <i>Impossible</i>  <br>");
        }

        if ((gunneryMod != 0) && (gunneryMod < Integer.MAX_VALUE)) {
            sb.append(String.format("  Gunnery: %+d <br>", gunneryMod));
        } else if (gunneryMod == Integer.MAX_VALUE) {
            sb.append("  Gunnery: <i>Impossible</i>  <br>");
        }

        if ((gunneryMod == 0) && (pilotingMod == 0)) {
            sb.append("None");
        }
        return sb.append("</html>").toString();
    }

    private JPanel fillKillRecord() {
        List<Kill> kills = campaign.getKillsFor(person.getId());

        JPanel pnlKills = new JPanel(new GridBagLayout());

        JLabel lblRecord = new JLabel(String.format(resourceMap.getString("format.kills"), kills.size()));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlKills.add(lblRecord, gridBagConstraints);

        PersonnelKillLogModel killModel = new PersonnelKillLogModel();
        killModel.setData(kills);
        JTable killTable = new JTable(killModel);
        killTable.setRowSelectionAllowed(false);
        killTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn column;
        for (int i = 0; i < killModel.getColumnCount(); ++ i) {
            column = killTable.getColumnModel().getColumn(i);
            column.setCellRenderer(killModel.getRenderer());
            column.setPreferredWidth(killModel.getPreferredWidth(i));
            if (killModel.hasConstantWidth(i)) {
                column.setMinWidth(killModel.getPreferredWidth(i));
                column.setMaxWidth(killModel.getPreferredWidth(i));
            }
        }
        killTable.setIntercellSpacing(new Dimension(0, 0));
        killTable.setShowGrid(false);
        killTable.setTableHeader(null);
        gridBagConstraints = new GridBagConstraints();

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

        pnlKills.add(killTable, gridBagConstraints);

        return pnlKills;
    }
}
