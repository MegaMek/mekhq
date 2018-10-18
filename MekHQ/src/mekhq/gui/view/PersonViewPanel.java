/*
 * PersonViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq.gui.view;

import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import megamek.common.Crew;
import megamek.common.options.PilotOptions;
import megamek.common.util.DirectoryItems;
import megamek.common.util.EncodeControl;
import mekhq.IconPackage;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.LogEntry;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.dialog.MedicalViewDialog;
import mekhq.gui.model.PersonnelEventLogModel;
import mekhq.gui.model.PersonnelKillLogModel;
import mekhq.gui.utilities.ImageHelpers;
import mekhq.gui.utilities.WrapLayout;

/**
 * A custom panel that gets filled in with goodies from a Person record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PersonViewPanel extends JPanel {
    private static final long serialVersionUID = 7004741688464105277L;

    private static final int MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW = 4;

    private Person person;
    private Campaign campaign;

    private DirectoryItems portraits;
    private DirectoryItems awardIcons;
    private IconPackage ip;

    private JPanel pnlPortrait;
    private JLabel lblPortrait;
    private JPanel pnlStats;
    private JTextArea txtDesc;
    private JPanel pnlKills;
    private JPanel pnlLog;
    private JPanel pnlInjuries;

    private JLabel lblType;
    private JLabel lblCall1;
    private JLabel lblCall2;
    private JLabel lblAge1;
    private JLabel lblAge2;
    private JLabel lblGender1;
    private JLabel lblGender2;
    private JLabel lblStatus1;
    private JLabel lblStatus2;
    private JLabel lblRecruited1;
    private JLabel lblRecruited2;
    private JLabel lblTimeServed1;
    private JLabel lblTimeServed2;
    private JLabel lblDuedate1;
    private JLabel lblDuedate2;
    private JLabel lblTough1;
    private JLabel lblTough2;
    private JLabel lblEdge1;
    private JLabel lblEdge2;
    private JLabel lblAbility1;
    private JLabel lblAbility2;
    private JLabel lblImplants1;
    private JLabel lblImplants2;
    private JLabel lblAdvancedMedical1;
    private JLabel lblAdvancedMedical2;
    private JLabel lblSpouse1;
    private JLabel lblSpouse2;
    private JLabel lblChildren1;
    private JLabel lblChildren2;
    private JPanel pnlMedals;
    private JPanel pnlMiscAwards;
    private Box boxRibbons;

    ResourceBundle resourceMap = null;


    public PersonViewPanel(Person p, Campaign c, IconPackage ip) {
        this.person = p;
        this.campaign = c;
        this.ip = ip;
        this.portraits = ip.getPortraits();
        this.awardIcons = ip.getAwardIcons();
        resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonViewPanel", new EncodeControl()); //$NON-NLS-1$
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        lblPortrait = new JLabel();
        pnlStats = new JPanel();
        pnlPortrait = new JPanel();
        txtDesc = new JTextArea();
        pnlKills = new JPanel();
        pnlLog = new JPanel();
        pnlInjuries = new JPanel();
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);

        // Panel portrait will include the person picture and the ribbons
        pnlPortrait.setName("pnlPortrait");
        pnlPortrait.setBackground(Color.WHITE);
        pnlPortrait.setLayout(new GridBagLayout());

        GridBagConstraints gbc_pnlPortrait = new GridBagConstraints();
        gbc_pnlPortrait = new GridBagConstraints();
        gbc_pnlPortrait.gridx = 0;
        gbc_pnlPortrait.gridy = 0;
        gbc_pnlPortrait.fill = GridBagConstraints.NONE;
        gbc_pnlPortrait.anchor = GridBagConstraints.NORTHWEST;
        gbc_pnlPortrait.insets = new Insets(10,10,0,0);
        add(pnlPortrait, gbc_pnlPortrait);

        lblPortrait.setName("lblPortait"); // NOI18N
        lblPortrait.setBackground(Color.WHITE);
        setPortrait();
        
        GridBagConstraints gbc_lblPortrait = new GridBagConstraints();
        gbc_lblPortrait.gridx = 0;
        gbc_lblPortrait.gridy = 0;
        gbc_lblPortrait.fill = GridBagConstraints.NONE;
        gbc_lblPortrait.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblPortrait.insets = new Insets(0,0,0,0);
        pnlPortrait.add(lblPortrait, gbc_lblPortrait);
        
        pnlStats.setName("pnlStats");
        pnlStats.setBorder(BorderFactory.createTitledBorder(person.getFullTitle()));
        pnlStats.setBackground(Color.WHITE);
        fillStats();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 20);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        int gridy = 1;
        
        if(person.awardController.hasAwards()) {
            if(person.awardController.hasAwardsWithRibbons()){
                boxRibbons = Box.createVerticalBox();
                boxRibbons.add(Box.createRigidArea(new Dimension(100,0)));
                drawRibbons();

                GridBagConstraints gbc_pnlAllRibbons = new GridBagConstraints();
                gbc_pnlAllRibbons.gridx = 0;
                gbc_pnlAllRibbons.gridy = 1;
                gbc_pnlAllRibbons.fill = GridBagConstraints.NONE;
                gbc_pnlAllRibbons.anchor = GridBagConstraints.NORTHWEST;
                gbc_pnlAllRibbons.insets = new Insets(0,0,0,0);
                pnlPortrait.add(boxRibbons, gbc_pnlAllRibbons);
            }

            if(person.awardController.hasAwardsWithMedals()){
                pnlMedals = new JPanel();
                pnlMedals.setName("pnlMedals");
                pnlMedals.setBackground(Color.WHITE);
                drawMedals();

                GridBagConstraints gbc_pnlMedals = new GridBagConstraints();
                gbc_pnlMedals.fill = GridBagConstraints.BOTH;
                gbc_pnlMedals.gridwidth = 2;
                gbc_pnlMedals.insets = new Insets(5, 5, 5, 20);
                gbc_pnlMedals.gridx = 0;
                gbc_pnlMedals.gridy = gridy;
                gbc_pnlMedals.anchor = GridBagConstraints.NORTHWEST;
                add(pnlMedals, gbc_pnlMedals);
                pnlMedals.setLayout(new WrapLayout(FlowLayout.LEFT));
                gridy++;
            }

            if(person.awardController.hasAwardsWithMiscs()){
                pnlMiscAwards = new JPanel();
                pnlMiscAwards.setName("pnlMiscAwards");
                pnlMiscAwards.setBackground(Color.WHITE);
                drawMiscAwards();

                GridBagConstraints gbc_pnlMiscAwards = new GridBagConstraints();
                gbc_pnlMiscAwards.fill = GridBagConstraints.BOTH;
                gbc_pnlMiscAwards.gridwidth = 2;
                gbc_pnlMiscAwards.insets = new Insets(5, 5, 5, 20);
                gbc_pnlMiscAwards.gridx = 0;
                gbc_pnlMiscAwards.gridy = gridy;
                gbc_pnlMiscAwards.anchor = GridBagConstraints.NORTHWEST;
                add(pnlMiscAwards, gbc_pnlMiscAwards);
                pnlMiscAwards.setLayout(new WrapLayout(FlowLayout.LEFT));
                gridy++;
            }
        }

        if(campaign.getCampaignOptions().useAdvancedMedical() && person.hasInjuries(false)) {
            pnlInjuries.setName("pnlInjuries"); //$NON-NLS-1$
            pnlInjuries.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlInjuries.title"))); //$NON-NLS-1$
            pnlInjuries.setBackground(Color.WHITE);
            fillInjuries();
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

        if(person.getBiography().length() > 0) {
            txtDesc.setName("txtDesc"); //$NON-NLS-1$
            txtDesc.setText(person.getBiography());
            txtDesc.setEditable(false);
            txtDesc.setLineWrap(true);
            txtDesc.setWrapStyleWord(true);
            txtDesc.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(resourceMap.getString("pnlDescription.title")), //$NON-NLS-1$
                    BorderFactory.createEmptyBorder(5,5,5,5)));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(5, 5, 5, 20);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(txtDesc, gridBagConstraints);
            gridy++;
        }

        if(person.getPersonnelLog().size() >0) {
            pnlLog.setName("pnlLog"); //$NON-NLS-1$
            pnlLog.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlLog.title"))); //$NON-NLS-1$
            pnlLog.setBackground(Color.WHITE);
            fillLog();
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(5, 5, 5, 20);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlLog, gridBagConstraints);
            gridy++;
        }

        if(!campaign.getKillsFor(person.getId()).isEmpty()) {
            fillKillRecord();

            pnlKills.setName("txtKills"); //$NON-NLS-1$
            pnlKills.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(resourceMap.getString("pnlKills.title")), //$NON-NLS-1$
                    BorderFactory.createEmptyBorder(5,5,5,5)));
            gridBagConstraints = new GridBagConstraints();
            pnlKills.setBackground(Color.WHITE);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.insets = new Insets(5, 5, 5, 20);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlKills, gridBagConstraints);
            gridy++;
        }

        //just to flush something to the bottom of the page
        JTextArea txtFiller = new JTextArea(""); //$NON-NLS-1$
        txtFiller.setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 20);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(txtFiller, gridBagConstraints);
    }

    /**
     * Draws the ribbons below the person portrait.
     */
    private void drawRibbons() {
        List<Award> awards = person.awardController.getAwards().stream().filter(a -> a.getNumberOfRibbonFiles() > 0).sorted()
                .collect(Collectors.toList());
        Collections.reverse(awards);

        int i = 0;
        Box rowRibbonsBox = null;
        ArrayList<Box> rowRibbonsBoxes = new ArrayList<>();

        for(Award award : awards){
            JLabel ribbonLabel = new JLabel();
            Image ribbon;

            if(i%MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW == 0){
                rowRibbonsBox = Box.createHorizontalBox();
                rowRibbonsBox.setBackground(Color.RED);
            }
            try{
                int numberOfAwards = person.awardController.getNumberOfAwards(award);
                String ribbonFileName = award.getRibbonFileName(numberOfAwards);
                ribbon = (Image) awardIcons.getItem(award.getSet() + "/ribbons/", ribbonFileName);
                if(ribbon == null) continue;
                ribbon = ribbon.getScaledInstance(25,8, Image.SCALE_DEFAULT);
                ribbonLabel.setIcon(new ImageIcon(ribbon));
                ribbonLabel.setToolTipText(award.getTooltip());
                rowRibbonsBox.add(ribbonLabel, 0);
            }
            catch (Exception err) {
                err.printStackTrace();
            }

            i++;
            if(i%MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW == 0){
                rowRibbonsBoxes.add(rowRibbonsBox);
            }
        }
        if(i%MAX_NUMBER_OF_RIBBON_AWARDS_PER_ROW!=0){
            rowRibbonsBoxes.add(rowRibbonsBox);
        }

        Collections.reverse(rowRibbonsBoxes);
        for(Box box : rowRibbonsBoxes){
            boxRibbons.add(box);
        }
    }

    /**
     * Draws the medals above the personal log.
     */
    private void drawMedals(){
        List<Award> awards = person.awardController.getAwards().stream().filter(a -> a.getNumberOfMedalFiles() > 0).sorted()
                .collect(Collectors.toList());

        for(Award award : awards){
            JLabel medalLabel = new JLabel();

            Image medal = null;
            try{
                int numberOfAwards = person.awardController.getNumberOfAwards(award);
                String medalFileName = award.getMedalFileName(numberOfAwards);
                medal = (Image) awardIcons.getItem(award.getSet() + "/medals/", medalFileName);
                if(medal == null) continue;
                medal = ImageHelpers.getScaledForBoundaries(medal, new Dimension(30,60), Image.SCALE_DEFAULT);
                medalLabel.setIcon(new ImageIcon(medal));
                medalLabel.setToolTipText(award.getTooltip());
                pnlMedals.add(medalLabel);
            }
            catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    /**
     * Draws the misc awards below the medals.
     */
    private void drawMiscAwards() {
        ArrayList<Award> awards = person.awardController.getAwards().stream().filter(a -> a.getNumberOfMiscFiles() > 0)
                .collect(Collectors.toCollection(ArrayList::new));

        for (Award award : awards) {
            JLabel miscLabel = new JLabel();

            Image miscAward = null;
            try {
                int numberOfAwards = person.awardController.getNumberOfAwards(award);
                String miscFileName = award.getMiscFileName(numberOfAwards);
                Image miscAwardBufferedImage = (Image) awardIcons.getItem(award.getSet() + "/misc/", miscFileName);
                if (miscAwardBufferedImage == null) continue;
                miscAward = ImageHelpers.getScaledForBoundaries(miscAwardBufferedImage, new Dimension(100,100), Image.SCALE_DEFAULT);
                miscLabel.setIcon(new ImageIcon(miscAward));
                miscLabel.setToolTipText(award.getTooltip());
                pnlMiscAwards.add(miscLabel);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    /**
     * set the portrait for the given person.
     *
     * @return The <code>Image</code> of the pilot's portrait. This value
     *         will be <code>null</code> if no portrait was selected
     *          or if there was an error loading it.
     */
    public void setPortrait() {

        String category = person.getPortraitCategory();
        String filename = person.getPortraitFileName();

        if(Crew.ROOT_PORTRAIT.equals(category)) {
            category = ""; //$NON-NLS-1$
        }

        // Return a null if the player has selected no portrait file.
        if ((null == category) || (null == filename) || Crew.PORTRAIT_NONE.equals(filename)) {
            filename = "default.gif"; //$NON-NLS-1$
        }

        // Try to get the player's portrait file.
        Image portrait = null;
        try {
            portrait = (Image) portraits.getItem(category, filename);
            if(null != portrait) {
                portrait = portrait.getScaledInstance(100, -1, Image.SCALE_DEFAULT);
            } else {
                portrait = (Image) portraits.getItem("", "default.gif");  //$NON-NLS-1$ //$NON-NLS-2$
                if(null != portrait) {
                    portrait = portrait.getScaledInstance(100, -1, Image.SCALE_DEFAULT);
                }
            }
            lblPortrait.setIcon(new ImageIcon(portrait));
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private void fillStats() {
        lblType = new JLabel();
        lblCall1 = new JLabel();
        lblCall2 = new JLabel();
        lblAge1 = new JLabel();
        lblAge2 = new JLabel();
        lblGender1 = new JLabel();
        lblGender2 = new JLabel();
        lblStatus1 = new JLabel();
        lblStatus2 = new JLabel();
        lblRecruited1 = new JLabel();
        lblRecruited2 = new JLabel();
        lblTimeServed1 = new JLabel();
        lblTimeServed2 = new JLabel();
        lblDuedate1 = new JLabel();
        lblDuedate2 = new JLabel();
        lblTough1 = new JLabel();
        lblTough2 = new JLabel();
        lblEdge1 = new JLabel();
        lblEdge2 = new JLabel();
        lblAbility1 = new JLabel();
        lblAbility2 = new JLabel();
        lblImplants1 = new JLabel();
        lblImplants2 = new JLabel();
        lblAdvancedMedical1 = new JLabel();
        lblAdvancedMedical2 = new JLabel();
        lblSpouse1 = new JLabel();
        lblSpouse2 = new JLabel();
        lblChildren1 = new JLabel();
        lblChildren2 = new JLabel();

        GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new GridBagLayout());

        lblType.setName("lblType"); // NOI18N
        lblType.setText(String.format(resourceMap.getString("format.italic"), person.getRoleDesc())); //$NON-NLS-1$
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblType, gridBagConstraints);

        int firsty = 0;

        if(!person.getCallsign().equals("-") && person.getCallsign().length() > 0) { //$NON-NLS-1$
            firsty++;
            lblCall1.setName("lblCall1"); // NOI18N
            lblCall1.setText(resourceMap.getString("lblCall1.text")); //$NON-NLS-1$
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblCall1, gridBagConstraints);

            lblCall2.setName("lblCall2"); // NOI18N
            lblCall2.setText(person.getCallsign());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblCall2, gridBagConstraints);
        }

        firsty++;
        lblAge1.setName("lblAge1"); // NOI18N
        lblAge1.setText(resourceMap.getString("lblAge1.text")); //$NON-NLS-1$
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblAge1, gridBagConstraints);

        lblAge2.setName("lblAge2"); // NOI18N
        lblAge2.setText(Integer.toString(person.getAge(campaign.getCalendar())));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblAge2, gridBagConstraints);

        firsty++;
        lblGender1.setName("lblGender1"); // NOI18N
        lblGender1.setText(resourceMap.getString("lblGender1.text")); //$NON-NLS-1$
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblGender1, gridBagConstraints);

        lblGender2.setName("lblGender2"); // NOI18N
        lblGender2.setText(person.getGenderName());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblGender2, gridBagConstraints);

        firsty++;
        lblStatus1.setName("lblStatus1"); // NOI18N
        lblStatus1.setText(resourceMap.getString("lblStatus1.text")); //$NON-NLS-1$
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStatus1, gridBagConstraints);

        lblStatus2.setName("lblStatus2"); // NOI18N
        lblStatus2.setText(person.getStatusName() + person.pregnancyStatus());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = firsty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStatus2, gridBagConstraints);

        if (campaign.getCampaignOptions().getUseTimeInService()) {
            if ((null != person.getRecruitmentAsString()) && !person.isDependent() && !person.isPrisoner() && !person.isBondsman()) {
                firsty++;
                lblRecruited1.setName("lblRecruited1"); // NOI18N
                lblRecruited1.setText(resourceMap.getString("lblRecruited1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlStats.add(lblRecruited1, gridBagConstraints);

                lblRecruited2.setName("lblRecruited2"); // NOI18N
                lblRecruited2.setText(person.getRecruitmentAsString());
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlStats.add(lblRecruited2, gridBagConstraints);

                firsty++;
                lblTimeServed1.setName("lblTimeServed1"); // NOI18N
                lblTimeServed1.setText(resourceMap.getString("lblTimeServed1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlStats.add(lblTimeServed1, gridBagConstraints);

                lblTimeServed2.setName("lblTimeServed2"); // NOI18N
                lblTimeServed2.setText(Integer.toString(person.getTimeInService(campaign.getCalendar())) + "yrs");
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = firsty;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlStats.add(lblTimeServed2, gridBagConstraints);
            }
        }

        if (person.getDueDate() != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String DueDate = df.format(person.getDueDate().getTime());

            firsty++;
            lblDuedate1.setName("lblDuedate1");
            lblDuedate1.setText(resourceMap.getString("lblDuedate1.text")); //$NON-NLS-1$
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblDuedate1, gridBagConstraints);

            lblDuedate2.setName("lblDuedate2"); // NOI18N
            lblDuedate2.setText(DueDate);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = firsty;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblDuedate2, gridBagConstraints);
        }

        int secondy = 0;
        JLabel lblName;
        JLabel lblValue;

        for(int i = 0; i < SkillType.getSkillList().length; i++) {
            if(person.hasSkill(SkillType.getSkillList()[i])) {
                secondy++;
                lblName = new JLabel(
                    String.format(resourceMap.getString("format.itemHeader"), SkillType.getSkillList()[i])); //$NON-NLS-1$
                lblValue = new JLabel(person.getSkill(SkillType.getSkillList()[i]).toString());
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = secondy;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlStats.add(lblName, gridBagConstraints);
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 3;
                gridBagConstraints.gridy = secondy;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlStats.add(lblValue, gridBagConstraints);
            }
        }

        if(campaign.getCampaignOptions().useToughness()) {
            secondy++;
            lblTough1.setName("lblTough1"); // NOI18N
            lblTough1.setText(resourceMap.getString("lblTough1.text")); //$NON-NLS-1$
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblTough1, gridBagConstraints);

            lblTough2.setName("lblTough2"); // NOI18N //$NON-NLS-1$
            lblTough2.setText("+" + Integer.toString(person.getToughness())); //$NON-NLS-1$
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblTough2, gridBagConstraints);
        }
        if(campaign.getCampaignOptions().useEdge()) {
            secondy++;
            lblEdge1.setName("lblEdge1"); // NOI18N //$NON-NLS-1$
            lblEdge1.setText(resourceMap.getString("lblEdge1.text")); //$NON-NLS-1$
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblEdge1, gridBagConstraints);

            lblEdge2.setName("lblEdge2"); // NOI18N //$NON-NLS-1$
            lblEdge2.setText(Integer.toString(person.getEdge()));
            lblEdge2.setToolTipText(person.getEdgeTooltip());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblEdge2, gridBagConstraints);
        }

        //special abilities and implants need to be three columns wide to handle their large width
        if(firsty > secondy) {
            secondy = firsty;
        }

        if (person.hasSpouse()) {
            secondy++;
            lblSpouse1.setName("lblSpouse1"); // NOI18N //$NON-NLS-1$
            lblSpouse1.setText(resourceMap.getString("lblSpouse1.text")); //$NON-NLS-1$
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSpouse1, gridBagConstraints);

            lblSpouse2.setName("lblSpouse2"); // NOI18N //$NON-NLS-1$
            lblSpouse2.setText(person.getSpouse().getFullName());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSpouse2, gridBagConstraints);
        }

        if (campaign.getCampaignOptions().useParentage() && person.hasChildren()) {
            secondy++;
            lblChildren1.setName("lblChildren1"); // NOI18N //$NON-NLS-1$
            lblChildren1.setText(resourceMap.getString("lblChildren1.text")); //$NON-NLS-1$
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblChildren1, gridBagConstraints);

            lblChildren2.setName("lblChildren2"); // NOI18N //$NON-NLS-1$
            lblChildren2.setText(person.getChildList());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblChildren2, gridBagConstraints);
        }

        if(campaign.getCampaignOptions().useAbilities() && person.countOptions(PilotOptions.LVL3_ADVANTAGES) > 0) {
            secondy++;
            lblAbility1.setName("lblAbility1"); // NOI18N //$NON-NLS-1$
            lblAbility1.setText(resourceMap.getString("lblAbility1.text")); //$NON-NLS-1$
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblAbility1, gridBagConstraints);

            lblAbility2.setName("lblAbility2"); // NOI18N //$NON-NLS-1$
            lblAbility2.setText(person.getAbilityList(PilotOptions.LVL3_ADVANTAGES));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblAbility2, gridBagConstraints);
        }

        if(campaign.getCampaignOptions().useImplants() && person.countOptions(PilotOptions.MD_ADVANTAGES) > 0) {
            secondy++;
            lblImplants1.setName("lblImplants1"); // NOI18N
            lblImplants1.setText(resourceMap.getString("lblImplants1.text")); //$NON-NLS-1$
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblImplants1, gridBagConstraints);

            lblImplants2.setName("lblImplants2"); // NOI18N
            lblImplants2.setText(person.getAbilityList(PilotOptions.MD_ADVANTAGES));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = secondy;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblImplants2, gridBagConstraints);
        }

        secondy++;
        lblAdvancedMedical1.setName("lblAdvancedMedical1"); // NOI18N
        lblAdvancedMedical1.setText(resourceMap.getString("lblAdvancedMedical1.text")); //$NON-NLS-1$
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = secondy;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblAdvancedMedical1, gridBagConstraints);

        lblAdvancedMedical2.setName("lblAdvancedMedical2"); // NOI18N
        lblAdvancedMedical2.setText(person.getEffectString());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = secondy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblAdvancedMedical2, gridBagConstraints);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = secondy;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
        JButton medicalButton = new JButton(new ImageIcon("data/images/misc/medical.png")); //$NON-NLS-1$
        medicalButton.addActionListener(event -> {
            MedicalViewDialog medDialog = new MedicalViewDialog(SwingUtilities.getWindowAncestor(this), campaign, person, ip);
            medDialog.setGMMode(campaign.isGM());
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
        medicalButton.setToolTipText(resourceMap.getString("btnMedical.tooltip")); //$NON-NLS-1$
        pnlStats.add(medicalButton, gridBagConstraints);
    }

    private void fillLog() {
        ArrayList<LogEntry> logs = person.getPersonnelLog();
        pnlLog.setLayout(new GridBagLayout());
        PersonnelEventLogModel eventModel = new PersonnelEventLogModel();
        eventModel.setData(logs);
        JTable eventTable = new JTable(eventModel);
        eventTable.setRowSelectionAllowed(false);
        eventTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn column = null;
        for(int i = 0; i < eventModel.getColumnCount(); ++ i) {
            column = eventTable.getColumnModel().getColumn(i);
            column.setCellRenderer(eventModel.getRenderer());
            column.setPreferredWidth(eventModel.getPreferredWidth(i));
            if(eventModel.hasConstantWidth(i)) {
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
    }

    private void fillInjuries() {
        GridBagConstraints gridBagConstraints;
        pnlInjuries.setLayout(new GridBagLayout());
        JLabel lblInjury;
        JLabel txtInjury;
        int row = 0;
        ArrayList<Injury> injuries = person.getInjuries();
        for(Injury injury : injuries) {
            lblInjury = new JLabel(injury.getFluff());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = row;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInjuries.add(lblInjury, gridBagConstraints);

            String text = (injury.isPermanent() && injury.getTime() < 1) ?
                resourceMap.getString("lblPermanentInjury.text") //$NON-NLS-1$
                : String.format(resourceMap.getString("format.injuryTime"), injury.getTime()); //$NON-NLS-1$
            txtInjury = new JLabel("<html>" + text + "</html>");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = row;
            gridBagConstraints.weightx = 1.0;
            if(row == (injuries.size() - 1)) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new Insets(0, 20, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlInjuries.add(txtInjury, gridBagConstraints);
            row++;
        }
    }

    private void fillKillRecord() {
        ArrayList<Kill> kills = campaign.getKillsFor(person.getId());
        pnlKills.setLayout(new GridBagLayout());

        JLabel lblRecord = new JLabel(String.format(resourceMap.getString("format.kills"), kills.size())); //$NON-NLS-1$
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
        TableColumn column = null;
        for(int i = 0; i < killModel.getColumnCount(); ++ i) {
            column = killTable.getColumnModel().getColumn(i);
            column.setCellRenderer(killModel.getRenderer());
            column.setPreferredWidth(killModel.getPreferredWidth(i));
            if(killModel.hasConstantWidth(i)) {
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
    }
}