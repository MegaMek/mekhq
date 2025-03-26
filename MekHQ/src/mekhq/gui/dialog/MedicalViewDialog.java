/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.dialog;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Period;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.*;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.ExtraData.Key;
import mekhq.campaign.ExtraData.StringKey;
import mekhq.campaign.force.Force;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.LogEntryType;
import mekhq.campaign.personnel.BodyLocation;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.InjuryLevel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.view.Paperdoll;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Period;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MedicalViewDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(MedicalViewDialog.class);

    private static final String MENU_CMD_SEPARATOR = ",";

    private static final Key<String> DOCTOR_NOTES = new StringKey("doctor_notes");

    // TODO: Custom paper dolls
    // private static final Key<String> PAPERDOLL = new StringKey("paperdoll_xml_file");

    private final Campaign campaign;
    private final Person   person;

    private Paperdoll defaultMaleDoll;
    private Paperdoll defaultFemaleDoll;
    private JPanel    dollWrapper;
    private Paperdoll doll;
    private JPanel    injuryPanel;
    private JTextArea notesArea;

    private ActionListener dollActionListener;

    private transient       Font           labelFont;
    private transient       Font           handwritingFont;
    private transient       Color          labelColor;
    private transient       ImageIcon      healImageIcon;
    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MedicalViewDialog",
          MekHQ.getMHQOptions().getLocale());

    public MedicalViewDialog(Window parent, Campaign c, Person p) {
        super();
        this.campaign = Objects.requireNonNull(c);
        this.person   = Objects.requireNonNull(p);

        // Preload default paperdolls
        try (InputStream fis = new FileInputStream(c.getApp()
                                                         .getIconPackage()
                                                         .getGuiElement("default_male_paperdoll"))) { // TODO : Remove inline file
            // path
            defaultMaleDoll = new Paperdoll(fis);
        } catch (IOException e) {
            logger.error("", e);
        }

        try (InputStream fis = new FileInputStream(c.getApp()
                                                         .getIconPackage()
                                                         .getGuiElement("default_female_paperdoll"))) { // TODO : Remove inline file
            // path
            defaultFemaleDoll = new Paperdoll(fis);
        } catch (IOException e) {
            logger.error("", e);
        }

        setPreferredSize(new Dimension(1024, 840));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);

        labelFont       = UIManager.getDefaults().getFont("Menu.font").deriveFont(Font.PLAIN, 16);
        handwritingFont = Font.decode(MekHQ.getMHQOptions().getMedicalViewDialogHandwritingFont())
                                .deriveFont(Font.PLAIN, 22);
        labelColor      = new Color(170, 170, 170);
        healImageIcon   = new ImageIcon(new ImageIcon("data/images/misc/medical.png").getImage()
                                              .getScaledInstance(16,
                                                    16,
                                                    Image.SCALE_DEFAULT)); // TODO : Remove inline file path

        dollActionListener = ae -> {
            final BodyLocation loc            = BodyLocation.of(ae.getActionCommand());
            final boolean      locationPicked = !loc.locationName().isEmpty();
            Point              mousePos       = doll.getMousePosition();
            JPopupMenu         popup          = new JPopupMenu();
            if (locationPicked) {
                JLabel header = new JLabel(Utilities.capitalize(loc.locationName()));
                header.setFont(UIManager.getDefaults().getFont("Menu.font").deriveFont(Font.BOLD));
                popup.add(header);
                popup.addSeparator();
            }
            if (locationPicked) {
                ActionListener addActionListener = addEvent -> {
                    String[]   commands = addEvent.getActionCommand().split(MENU_CMD_SEPARATOR, 2);
                    InjuryType addIType = InjuryType.byKey(commands[0]);
                    int        severity = Integer.parseInt(commands[1]);
                    person.addInjury(addIType.newInjury(c, person, loc, severity));
                    revalidate();
                };
                JMenu addMenu = new JMenu(resourceMap.getString("menuAdd.text"));
                InjuryType.getAllTypes()
                      .stream()
                      .filter(it -> it.isValidInLocation(loc))
                      .sorted((it1, it2) -> it1.getSimpleName().compareToIgnoreCase(it2.getSimpleName()))
                      .forEach(it -> IntStream.range(1, it.getMaxSeverity() + 1).forEach(severity -> {
                          JMenuItem add = new JMenuItem(resourceMap.getString("menuMore.text") +
                                                        it.getSimpleName(severity));
                          add.setActionCommand(it.getKey() + MENU_CMD_SEPARATOR + severity);
                          add.addActionListener(addActionListener);
                          addMenu.add(add);
                      }));
                popup.add(addMenu);
            } else {
                JMenuItem edit = new JMenuItem(resourceMap.getString("menuNewInjury.text"),
                      UIManager.getIcon("FileView.fileIcon"));
                popup.add(edit);
            }
            JMenuItem remove = new JMenuItem(loc.locationName().isEmpty() ?
                                                   resourceMap.getString("menuHealAll.text") :
                                                   resourceMap.getString("menuHeal.text"), healImageIcon);
            if (locationPicked && p.getInjuriesByLocation(loc).isEmpty()) {
                remove.setEnabled(false);
            } else {
                remove.addActionListener(rae -> {
                    person.getInjuries()
                          .stream()
                          .filter(inj -> !locationPicked || (inj.getLocation() == loc))
                          .forEach(person::removeInjury);
                    revalidate();
                });
            }
            popup.add(remove);
            Dimension popupSize = popup.getPreferredSize();
            popup.show(doll, (int) (mousePos.getX() - popupSize.getWidth()) + 10, (int) mousePos.getY() - 10);
        };

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        Container scrollPanel = new JPanel();
        getContentPane().add(new JScrollPaneWithSpeed(scrollPanel,
              JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        initComponents(scrollPanel);

        JButton okayButton = new JButton(resourceMap.getString("buttonDone.text"));
        okayButton.addActionListener(ae -> {
            if (!notesArea.getText().isEmpty()) {
                p.getExtraData().set(DOCTOR_NOTES, notesArea.getText());
            }
            setVisible(false);
        });
        okayButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        getContentPane().add(okayButton);
        pack();
        setUserPreferences();
        setModal(true);
    }

    private void initComponents(Container cont) {
        cont.setLayout(new GridBagLayout());

        GridBagConstraints gbc;

        gbc            = new GridBagConstraints();
        gbc.gridx      = 0;
        gbc.gridy      = 0;
        gbc.gridheight = 6;
        gbc.weighty    = 1.0;
        gbc.fill       = GridBagConstraints.VERTICAL;

        dollWrapper = new JPanel(null);
        dollWrapper.setLayout(new BoxLayout(dollWrapper, BoxLayout.Y_AXIS));
        dollWrapper.setMinimumSize(new Dimension(256, 768));
        dollWrapper.setMaximumSize(new Dimension(256, Integer.MAX_VALUE));
        dollWrapper.setOpaque(false);
        dollWrapper.setAlignmentY(Component.TOP_ALIGNMENT);
        cont.add(dollWrapper, gbc);

        gbc.gridx      = 1;
        gbc.gridheight = 1;
        gbc.weightx    = 1.0;
        gbc.weighty    = 0.0;
        gbc.insets     = new Insets(10, 10, 10, 10);
        gbc.fill       = GridBagConstraints.BOTH;

        cont.add(genBaseData(campaign, person), gbc);

        gbc.gridy = 1;

        cont.add(genMedicalHistory(campaign, person), gbc);

        gbc.gridy = 2;

        cont.add(genAllergies(campaign, person), gbc);

        gbc.gridy = 3;

        cont.add(genIllnesses(campaign, person), gbc);

        gbc.gridy = 4;

        cont.add(injuryPanel = new JPanel(), gbc);

        gbc.gridy = 5;

        cont.add(genNotes(campaign, person), gbc);
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     *
     * @since 0.50.04
     * @deprecated Move to Suite Constants / Suite Options Setup
     */
    @Deprecated(since = "0.50.04")
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MedicalViewDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    @Override
    public void validate() {
        dollWrapper.setVisible(false);
        fillDoll(dollWrapper, campaign, person);
        dollWrapper.setVisible(true);
        injuryPanel.setVisible(false);
        fillInjuries(injuryPanel, campaign, person);
        injuryPanel.setVisible(true);
        super.validate();
    }

    private void fillDoll(JPanel panel, Campaign c, Person p) {
        panel.removeAll();

        if (null != doll) {
            doll.removeActionListener(dollActionListener);
        }
        doll = person.getGender().isMale() ? defaultMaleDoll : defaultFemaleDoll;
        doll.clearLocColors();
        doll.clearLocTags();
        doll.setHighlightColor(new Color(170, 170, 255));
        Arrays.stream(BodyLocation.values()).filter(p::hasInjury).forEach(bl -> {
            if (person.isLocationMissing(bl) && !person.isLocationMissing(bl.Parent())) {
                doll.setLocTag(bl, "lost");
            } else if (!person.isLocationMissing(bl)) {
                InjuryLevel level = getMaxInjuryLevel(person, bl);
                Color col = switch (level) {
                    case CHRONIC -> new Color(255, 204, 255);
                    case DEADLY -> Color.RED;
                    case MAJOR -> Color.ORANGE;
                    case MINOR -> Color.YELLOW;
                    default -> Color.WHITE;
                };
                doll.setLocColor(bl, col);
            }
        });

        if (c.isGM()) {
            doll.addActionListener(dollActionListener);
        }
        panel.add(doll);
        panel.add(Box.createVerticalGlue());

    }

    private JPanel genBaseData(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new GridLayout(10, 2));
        panel.setBorder(BorderFactory.createMatteBorder(3, 3, 0, 3, Color.BLACK));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        String givenName = p.getGivenName();
        String surname   = p.getSurname();

        if (p.isClanPersonnel()) {
            surname = p.getBloodname();
        }

        Period age = Period.between(p.getDateOfBirth(), c.getLocalDate());

        String phenotype = p.getPhenotype().isNone() ?
                                 resourceMap.getString("baselinePhenotype.text") :
                                 p.getPhenotype().toString();

        Force  f     = c.getForceFor(p);
        String force = (null != f) ? f.getFullName() : "-";

        Person doc    = c.getPerson(p.getDoctorId());
        String doctor = resourceMap.getString("none.text");
        if ((null != doc) && doc.getStatus().isActive()) {
            doctor = doc.getFullTitle();
        }
        panel.add(genLabel(resourceMap.getString("familyName.text")));
        panel.add(genLabel(resourceMap.getString("givenNames.text")));
        panel.add(genWrittenPanel(surname));
        panel.add(genWrittenPanel(givenName));
        panel.add(genLabel(resourceMap.getString("birthDate.text")));
        panel.add(genLabel(resourceMap.getString("age.text")));
        panel.add(genWrittenPanel(MekHQ.getMHQOptions().getDisplayFormattedDate(p.getDateOfBirth())));
        panel.add(genWrittenPanel(String.format(resourceMap.getString("age.format"), age.getYears(), age.getMonths())));
        panel.add(genLabel(resourceMap.getString("gender.text")));
        panel.add(genLabel(resourceMap.getString("phenotype.text")));
        panel.add(genWrittenPanel(p.getGender().isMale() ?
                                        resourceMap.getString("genderMale.text") :
                                        resourceMap.getString("genderFemale.text")));
        panel.add(genWrittenPanel(phenotype));
        panel.add(genLabel(resourceMap.getString("assignedTo.text")));
        panel.add(genLabel(""));
        panel.add(genWrittenPanel(force));
        panel.add(genWrittenPanel(""));
        panel.add(genLabel(resourceMap.getString("assignedDoctor.text")));
        panel.add(genLabel(resourceMap.getString("lastCheckup.text")));
        panel.add(genWrittenPanel(doctor));
        panel.add(genWrittenPanel(""));
        return panel;
    }

    private JPanel genMedicalHistory(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(genLabel(resourceMap.getString("medicalHistory.text")));
        Map<String, List<LogEntry>> groupedEntries = p.getPersonnelLog()
                                                           .stream()
                                                           .filter(entry -> entry.getType() == LogEntryType.MEDICAL)
                                                           .sorted(Comparator.comparing(LogEntry::getDate))
                                                           .collect(Collectors.groupingBy(entry -> MekHQ.getMHQOptions()
                                                                                                         .getDisplayFormattedDate(
                                                                                                               entry.getDate())));
        groupedEntries.entrySet()
              .stream()
              .filter(e -> !e.getValue().isEmpty())
              .sorted(Entry.comparingByKey())
              .forEachOrdered(e -> {
                  if (e.getValue().size() > 1) {
                      panel.add(genWrittenText(e.getKey()));
                      e.getValue().forEach(entry -> {
                          JPanel wrapper = new JPanel();
                          wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
                          wrapper.setOpaque(false);
                          wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                          wrapper.add(Box.createHorizontalStrut(60));
                          wrapper.add(genWrittenText(String.format(resourceMap.getString("historyText.format"),
                                entry.getDesc())));
                          panel.add(wrapper);
                      });
                  } else {
                      panel.add(genWrittenText(String.format(resourceMap.getString("historyDateAndText.format"),
                            e.getKey(),
                            e.getValue().get(0).getDesc())));
                  }
              });
        return panel;
    }

    private JPanel genAllergies(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(genLabel(resourceMap.getString("allergies.text")));
        panel.add(genWrittenText(""));

        return panel;
    }

    private JPanel genIllnesses(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(genLabel(resourceMap.getString("illnesses.text")));
        panel.add(genWrittenText(""));

        return panel;
    }

    /** Get the maximum injury level in the specified location */
    private InjuryLevel getMaxInjuryLevel(Person p, BodyLocation loc) {
        return p.getInjuries()
                     .stream()
                     .filter(inj -> !inj.isHidden() && (inj.getLocation() == loc))
                     .min((inj1, inj2) -> Integer.compare(inj2.getLevel().ordinal(), inj1.getLevel().ordinal()))
                     .map(Injury::getLevel)
                     .orElse(InjuryLevel.NONE);
    }

    /**
     * Compiles a list of body locations stream ordered by the maximum injury level in that location
     */
    private Stream<BodyLocation> maxInjuryLevelLocationStream(Person p) {
        Map<BodyLocation, InjuryLevel> levelMap = new HashMap<>();
        Arrays.stream(BodyLocation.values())
              .filter(p::hasInjury)
              .forEach(bl -> levelMap.put(bl, getMaxInjuryLevel(p, bl)));
        return levelMap.entrySet()
                     .stream()
                     .sorted((entry1, entry2) -> Integer.compare(entry2.getValue().ordinal(),
                           entry1.getValue().ordinal()))
                     .map(Entry::getKey);
    }

    private void fillInjuries(JPanel panel, Campaign c, Person p) {
        panel.removeAll();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(genLabel(resourceMap.getString("injuries.text")));

        maxInjuryLevelLocationStream(p).forEachOrdered(bl -> {
            JPanel blWrapper = new JPanel();
            blWrapper.setLayout(new BoxLayout(blWrapper, BoxLayout.X_AXIS));
            blWrapper.setOpaque(false);
            blWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            blWrapper.add(Box.createHorizontalStrut(30));
            blWrapper.add(genWrittenText(Utilities.capitalize(bl.locationName())));
            panel.add(blWrapper);

            p.getInjuriesByLocation(bl)
                  .stream()
                  .sorted((inj1, inj2) -> Integer.compare(inj2.getLevel().ordinal(), inj1.getLevel().ordinal()))
                  .forEachOrdered(inj -> {
                      JLabel injLabel;
                      if (inj.getType().isPermanent()) {
                          injLabel = genWrittenText(String.format(resourceMap.getString("injuriesText.format"),
                                inj.getType().getSimpleName(),
                                MekHQ.getMHQOptions().getDisplayFormattedDate(inj.getStart())));
                      } else if (inj.isPermanent() || (inj.getTime() <= 0)) {
                          injLabel = genWrittenText(String.format(resourceMap.getString("injuriesPermanent.format"),
                                inj.getType().getSimpleName(),
                                MekHQ.getMHQOptions().getDisplayFormattedDate(inj.getStart())));
                      } else {
                          injLabel = genWrittenText(String.format(resourceMap.getString("injuriesTextAndDuration.format"),
                                inj.getType().getSimpleName(),
                                MekHQ.getMHQOptions().getDisplayFormattedDate(inj.getStart()),
                                genTimePeriod(inj.getTime())));
                      }

                      if (c.isGM()) {
                          injLabel.addMouseListener(new InjuryLabelMouseAdapter(injLabel, p, inj));
                      }

                      JPanel wrapper = new JPanel();
                      wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
                      wrapper.setOpaque(false);
                      wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                      wrapper.add(Box.createHorizontalStrut(60));
                      wrapper.add(injLabel);

                      panel.add(wrapper);
                  });
        });

    }

    private JPanel genNotes(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(genLabel(resourceMap.getString("doctorsNotes.text")));

        String notes = p.getExtraData().get(DOCTOR_NOTES, "");
        notesArea = new JTextArea(notes);
        notesArea.setEditable(true);
        notesArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesArea.setFont(handwritingFont);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        JPanel notesPanel = new JPanel();
        notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.X_AXIS));
        notesPanel.setOpaque(false);
        notesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesPanel.add(Box.createHorizontalStrut(30));
        notesPanel.add(notesArea);

        panel.add(notesPanel);

        return panel;
    }

    private JLabel genLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(labelFont);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setForeground(labelColor);
        return label;
    }

    private JPanel genWrittenPanel(String text) {
        JLabel label = genWrittenText(text);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(Box.createHorizontalStrut(30));
        wrapper.add(label);
        wrapper.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, Color.BLACK));

        return wrapper;
    }

    private JLabel genWrittenText(String text) {
        JLabel label = new JLabel(text);
        label.setFont(handwritingFont);
        return label;
    }

    private String genTimePeriod(int days) {
        if (days <= 1) {
            return resourceMap.getString("durationOneDay.text");
        } else if (days < 21) {
            return String.format(resourceMap.getString("durationDays.format"), days);
        } else if (days <= 12 * 7) {
            return String.format(resourceMap.getString("durationWeeks.format"), days * 1.0 / 7.0);
        } else if (days <= 2 * 365) {
            return String.format(resourceMap.getString("durationMonths.format"), days * 12.0 / 365.0);
        } else {
            return String.format(resourceMap.getString("durationYears.format"), days * 1.0 / 365.0);
        }
    }

    private static class InjuryLabelMouseAdapter extends MouseAdapter {
        private final JLabel         label;
        private final Person         person;
        private final Injury         injury;
        private       ImageIcon      healImageIcon;
        private       ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MedicalViewDialog",
              MekHQ.getMHQOptions().getLocale());

        public InjuryLabelMouseAdapter(JLabel label, Person person, Injury injury) {
            this.label         = label;
            this.person        = person;
            this.injury        = injury;
            this.healImageIcon = new ImageIcon(new ImageIcon("data/images/misc/medical.png").getImage()
                                                     .getScaledInstance(16,
                                                           16,
                                                           Image.SCALE_DEFAULT)); // TODO : Remove inline file path
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            label.setBackground(Color.LIGHT_GRAY);
            label.setOpaque(true);
            label.invalidate();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            label.setBackground(Color.WHITE);
            label.setOpaque(false);
            label.invalidate();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                JPopupMenu popup  = new JPopupMenu();
                JLabel     header = new JLabel(injury.getFluff());
                header.setFont(UIManager.getDefaults().getFont("Menu.font").deriveFont(Font.BOLD));
                popup.add(header);
                popup.addSeparator();
                JMenuItem edit = new JMenuItem(resourceMap.getString("menuEdit.text"),
                      UIManager.getIcon("FileView.fileIcon"));
                popup.add(edit);
                JMenuItem remove = new JMenuItem(resourceMap.getString("menuRemove.text"), healImageIcon);
                remove.addActionListener(ae -> {
                    person.removeInjury(injury);
                    label.getRootPane().getParent().revalidate();
                });
                popup.add(remove);
                Dimension popupSize = popup.getPreferredSize();
                popup.show(e.getComponent(), e.getX() - (int) popupSize.getWidth() + 10, e.getY() - 10);
            }
        }
    }
}
