/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;

import javax.swing.*;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;

/**
 * Hovanes Gambaryan Henry Demirchian CSUN, CS 585 Professor Mike Barnes
 * December 06, 2000
 * <p>
 * DateChooser class is a general GUI based date chooser. It allows the user to
 * select an instance of LocalDate defined in java.time package.
 * <p>
 * Programming API is similar to JFC's JColorChooser or JFileChooser. This class
 * can be used in any application to enable the user to select a date from a
 * visually displayed calendar.
 * <p>
 * There is a lot of improvements that can be done over this class in areas of
 * functionality, usability, and appearance. But as is, the class can be easily
 * used from within any Java program.
 * <p>
 * Typical usage is like:
 * <p>
 * // initial date LocalDate date = LocalDate.now()
 * <p>
 * // The owner is the JFrame of the application ("AppClass.this")
 * <p>
 * // show the date chooser DateChooser dc = new DateChooser(owner, date);
 * <p>
 * // user can either choose a date or cancel by closing if
 * (dc.showDateChooser()
 * == DateChooser.OK_OPTION) { date = dc.getDate(); }
 */
public class DateChooser extends JDialog implements ActionListener, FocusListener, KeyListener {
    private static final MMLogger logger = MMLogger.create(DateChooser.class);

    private static final LocalDate MIN_DATE = LocalDate.of(3470, 1, 1);
    public static final int OK_OPTION = 1;
    public static final int CANCEL_OPTION = 2;
    private static final String RESOURCE_PACKAGE = "mekhq/resources/DateChooser";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE,
        MekHQ.getMHQOptions().getLocale());

    private static final List<String> monthNames;
    static {
        monthNames = new ArrayList<>(12);
        monthNames.add(resources.getString("monthNameJanuary.text"));
        monthNames.add(resources.getString("monthNameFebruary.text"));
        monthNames.add(resources.getString("monthNameMarch.text"));
        monthNames.add(resources.getString("monthNameApril.text"));
        monthNames.add(resources.getString("monthNameMay.text"));
        monthNames.add(resources.getString("monthNameJune.text"));
        monthNames.add(resources.getString("monthNameJuly.text"));
        monthNames.add(resources.getString("monthNameAugust.text"));
        monthNames.add(resources.getString("monthNameSeptember.text"));
        monthNames.add(resources.getString("monthNameOctober.text"));
        monthNames.add(resources.getString("monthNameNovember.text"));
        monthNames.add(resources.getString("monthNameDecember.text"));
    }

    private LocalDate date;
    private LocalDate workingDate;
    private JLabel monthLabel;
    private JLabel yearLabel;
    private JPanel dayGrid;
    private boolean ready;

    // Stores the user-input date.
    private JFormattedTextField dateField;

    /**
     * Constructor for DateChooser which has parent dialog
     *
     * @param parentDialog
     *                     JDialog istance. Dialog that owns this
     * @param date
     *                     LocalDate instance that will be the initial date for
     *                     this dialog
     */
    public DateChooser(JDialog parentDialog, LocalDate date) {
        super(parentDialog, resources.getString("DateChooser.title"), true);
        initialize(parentDialog, date);
    }

    /**
     * Constructor for DateChooser which does not have a parent dialog
     *
     * @param owner
     *              JFrame instance, owner of DateChooser dialog
     * @param date
     *              LocalDate instance that will be the initial date for
     *              this dialog
     */
    public DateChooser(JFrame owner, LocalDate date) {
        super(owner, resources.getString("DateChooser.title"), true);
        initialize(owner, date);
    }

    /**
     * Initialize the calendar dialog with the given owner and date.
     *
     * @param owner the component that owns this dialog
     * @param date  the initial date for the calendar
     */
    private void initialize(Component owner, LocalDate date) {
        this.date = date;
        workingDate = this.date;

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JPanel yearPane = new JPanel();
        JPanel monthPane = new JPanel();
        yearPane.setLayout(new BoxLayout(yearPane, BoxLayout.X_AXIS));
        monthPane.setLayout(new BoxLayout(monthPane, BoxLayout.X_AXIS));

        JButton[] navButton = new JButton[4];

        // build the panel with month name and navigation buttons with FlowLayout
        monthPane.setLayout(new FlowLayout(FlowLayout.CENTER));
        monthPane.add(navButton[0] = new JButton("<"));
        monthPane.add(monthLabel = new JLabel(String.valueOf(monthNames.get(this.date.getMonth().ordinal())), JLabel.CENTER));
        monthLabel.setMinimumSize(new Dimension(80, 17));
        monthLabel.setMaximumSize(new Dimension(80, 17));
        monthLabel.setPreferredSize(new Dimension(80, 17));
        monthPane.add(navButton[1] = new JButton(">"));

        // build the panel with year and navigation buttons with FlowLayout
        yearPane.setLayout(new FlowLayout(FlowLayout.CENTER));
        yearPane.add(navButton[2] = new JButton("<<"));
        yearPane.add(yearLabel = new JLabel(String.valueOf(this.date.getYear()), JLabel.CENTER));
        yearLabel.setMinimumSize(new Dimension(50, 17));
        yearLabel.setMaximumSize(new Dimension(50, 17));
        yearLabel.setPreferredSize(new Dimension(50, 17));
        yearPane.add(navButton[3] = new JButton(">>"));

        // register a listener on the navigation buttons
        for (int i = 0; i < 4; i++) {
            navButton[i].addActionListener(this);
        }

        // set the tool tip text on the navigation buttons
        navButton[0].setToolTipText(resources.getString("previousMonth.text"));
        navButton[1].setToolTipText(resources.getString("nextMonth.text"));
        navButton[2].setToolTipText(resources.getString("previousYear.text"));
        navButton[3].setToolTipText(resources.getString("nextYear.text"));

        // put the panel for months and years together and add some formatting
        JPanel topPane = new JPanel();
        topPane.setLayout(new BoxLayout(topPane, BoxLayout.X_AXIS));
        topPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        topPane.add(monthPane);
        topPane.add(Box.createRigidArea(new Dimension(20, 0)));
        topPane.add(yearPane);

        ImageIcon originalIcon = new ImageIcon("data/images/force/Pieces/Logos/Inner Sphere/Star League.png");
        Image originalImage = originalIcon.getImage();
        Image scaledImage = originalImage.getScaledInstance(120, 63, Image.SCALE_FAST);
        ImageIcon resizedIcon = new ImageIcon(scaledImage);

        JLabel imageLabel = new JLabel(resizedIcon);
        topPane.add(imageLabel, BorderLayout.BEFORE_FIRST_LINE);

        // create the panel that will hold the days of the months
        dayGrid = new JPanel(new GridLayout(7, 7));
        updateDayGrid(false);

        contentPane.add(topPane, BorderLayout.BEFORE_FIRST_LINE);
        contentPane.add(dayGrid, BorderLayout.CENTER);

        // Create the date label
        JLabel dateLabel = new JLabel(resources.getString("dateField.text"), JLabel.CENTER);

        // Set up the date input text field with the current campaign date.
        dateField = new JFormattedTextField(this.date);
        dateField.setName("dateField");
        dateField.addFocusListener(this);
        dateField.addKeyListener(this);
        dateField.setFormatterFactory(new DefaultFormatterFactory(new AbstractFormatter() {
            @Override
            public Object stringToValue(String text) {
                return parseDate(text);
            }

            @Override
            public String valueToString(Object value) {
                return MekHQ.getMHQOptions().getDisplayFormattedDate((LocalDate) value);
            }
        }));
        dateField.setHorizontalAlignment(SwingConstants.CENTER);
        contentPane.add(dateField, BorderLayout.SOUTH);
        dateField.setColumns(10);

        // Create a panel for the dateLabel
        JPanel dateLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        dateLabelPanel.add(dateLabel);

        // Create a panel for the dateField
        JPanel dateFieldPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        dateFieldPanel.add(dateField);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Create a panel for the buttons with a GridLayout
        JPanel eraPanel = new JPanel(new GridLayout(3, 4));
        for (int i = 0; i < 12; i++) {
            JButton eraButton = createEraButton(i);
            eraButton.setHorizontalAlignment(SwingConstants.CENTER);
            eraPanel.add(eraButton);
        }
        bottomPanel.add(eraPanel, BorderLayout.CENTER);

        // Create a separate panel for the confirmDate button
        JPanel confirmPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton confirmButton = new JButton(resources.getString("confirmDate.text"));
        confirmButton.addActionListener(e -> {
            if (updateDateFromDateField()) {
                dispose();
            }
        });
        confirmPanel.add(confirmButton);

        // Create an intermediate JPanel for dateLabelPanel, dateFieldPanel and eraPanel
        JPanel dateAndEraPanel = new JPanel(new BorderLayout());
        dateAndEraPanel.add(dateLabelPanel, BorderLayout.NORTH);
        dateAndEraPanel.add(dateFieldPanel, BorderLayout.CENTER);
        dateAndEraPanel.add(eraPanel, BorderLayout.SOUTH);

        // Create another JPanel for dateAndEraPanel and confirmPanel
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.add(dateAndEraPanel, BorderLayout.NORTH);
        bottomContainer.add(confirmPanel, BorderLayout.SOUTH);

        contentPane.add(dayGrid, BorderLayout.CENTER);
        contentPane.add(bottomContainer, BorderLayout.SOUTH);

        // setResizable(false);
        ready = false;
        pack();
        setMinimumSize(new Dimension(750, 550));

        // center this dialog over the owner
        setLocationRelativeTo(owner);
        setUserPreferences();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(DateChooser.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    /**
     * Return the last selected date for this instance of DateChooser
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Displays a DateChooser dialog on the screen. If a new date is selected
     * returnsor OK_OPTION. If the action is canceled returns CANCEL_OPTION.
     * Both of the returned values are defined as static constants.
     */
    public int showDateChooser() {
        ready = false;
        setVisible(true);
        if (ready) {
            return OK_OPTION;
        } else {
            return CANCEL_OPTION;
        }
    }

    /**
     * Action handler for this dialog, which handles all the button presses.
     *
     * @param event ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String label = ((JButton) event.getSource()).getText();

        switch (label) {
            case "<": {
                int month = monthNames.indexOf(monthLabel.getText());
                month = prevMonth(month);
                monthLabel.setText(monthNames.get(month));
                updateDayGrid(false);
                break;
            }
            case ">": {
                int month = monthNames.indexOf(monthLabel.getText());
                month = nextMonth(month);
                monthLabel.setText(monthNames.get(month));
                updateDayGrid(false);
                break;
            }
            case "<<": {
                int year = 0;
                try {
                    year = Integer.parseInt(yearLabel.getText());
                } catch (NumberFormatException e) {
                    logger.error("", e);
                }
                yearLabel.setText(String.valueOf(--year));
                updateDayGrid(false);
                break;
            }
            case ">>": {
                int year = 0;
                try {
                    year = Integer.parseInt(yearLabel.getText());
                } catch (NumberFormatException e) {
                    logger.error("", e);
                }
                yearLabel.setText(String.valueOf(++year));
                updateDayGrid(false);
                break;
            }
            default: {
                int month = monthNames.indexOf(monthLabel.getText()) + 1;
                int year = 0;
                int day = 0;
                try {
                    year = Integer.parseInt(yearLabel.getText());
                    day = Integer.parseInt(label);
                } catch (NumberFormatException e) {
                    logger.error("", e);
                }

                // Set the date field to the new date.
                setDate(LocalDate.of(year, month, day));
                ready = true;
                break;
            }
        }
    }

    /**
     * Updates the dialog's controls with the passed in date.
     *
     * @param date The date to be displayed.
     */
    private void setDate(LocalDate date) {
        this.date = date;
        ready = true;
        monthLabel.setText(monthNames.get(date.getMonth().ordinal()));
        yearLabel.setText(String.valueOf(date.getYear()));
        dateField.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        updateDayGrid(true);
    }

    /**
     * This method is used by DateChooser to calculate and display days of the
     * month in correct format for the month currently displayed. Days of the
     * months are displayed as JButtons that the user can select. DateChooser's
     * current day is highlighted in red color.
     */
    private void updateDayGrid(boolean fromDateField) {
        dayGrid.removeAll();

        // get the currently selected month and year
        int month = monthNames.indexOf(monthLabel.getText()) + 1;
        int year = 0;
        try {
            year = Integer.parseInt(yearLabel.getText());
        } catch (NumberFormatException e) {
            logger.error("", e);
        }

        // decide what day of the week is the first day of this month
        int offset = LocalDate.of(year, month, 1).getDayOfWeek().ordinal();

        // display 7 days of the week across the top
        dayGrid.add(new JLabel(resources.getString("monday.text"), JLabel.CENTER));
        dayGrid.add(new JLabel(resources.getString("tuesday.text"), JLabel.CENTER));
        dayGrid.add(new JLabel(resources.getString("wednesday.text"), JLabel.CENTER));
        dayGrid.add(new JLabel(resources.getString("thursday.text"), JLabel.CENTER));
        dayGrid.add(new JLabel(resources.getString("friday.text"), JLabel.CENTER));
        dayGrid.add(new JLabel(resources.getString("saturday.text"), JLabel.CENTER));
        dayGrid.add(new JLabel(resources.getString("sunday.text"), JLabel.CENTER));

        // skip to the correct first day of the week for this month
        for (int i = 1; i <= offset; i++) {
            dayGrid.add(new JLabel(""));
        }

        // display days of the month for this month
        JButton day;
        int workingDay = 1; // Start at the first day of the month.
        for (int i = 1; i <= getLastDay(); i++) {
            dayGrid.add(day = new JButton(String.valueOf(i)));
            day.setToolTipText(resources.getString("dayPicker.tooltip"));
            day.addActionListener(this);

            // show the current day in bright red.
            if ((i == date.getDayOfMonth()) && (month == date.getMonth().ordinal())
                    && (year == date.getYear())) {
                day.setForeground(Color.red);
                workingDay = i; // Store the correct day of the month.
            }
        }

        // display the remaining empty slots to preserve the structure
        for (int i = (offset + getLastDay() + 1); i <= 42; i++) {
            dayGrid.add(new JLabel(""));
        }

        // Update the date field with the newly selected date.
        if ((dateField != null) && !fromDateField) {
            workingDate = LocalDate.of(year, month, workingDay);
            setDate(workingDate);
        }

        repaint();
        validate();
    }

    /**
     * Return the month following the one passed in as an argument. If the
     * argument is the las month of the year, return the first month.
     *
     * @param month Current month expressed as an integer (0 to 11).
     */
    private int nextMonth(int month) {
        if (month == 11) {
            return (0);
        }
        return (++month);
    }

    /**
     * Return the month preceding the one passed in as an argument. If the
     * argument is the first month of the year, return the last month.
     *
     * @param month Current month expressed as an integer (0 to 11).
     */
    private int prevMonth(int month) {
        if (month == 0) {
            return (11);
        }
        return (--month);
    }

    /**
     * Return the value of the last day in the currently selected month
     */
    private int getLastDay() {
        int month = (monthNames.indexOf(monthLabel.getText()) + 1);
        int year = 0;
        try {
            year = Integer.parseInt(yearLabel.getText());
        } catch (NumberFormatException e) {
            logger.error("", e);
        }

        return LocalDate.of(year, month, 1).lengthOfMonth();
    }

    /**
     * Select all text in the date field when it gains the focus.
     *
     * @param event FocusEvent
     */
    @Override
    public void focusGained(FocusEvent event) {
        if (dateField.equals(event.getSource())) {
            SwingUtilities.invokeLater(() -> dateField.selectAll());
        }
    }

    @Override
    public void focusLost(FocusEvent event) {}

    /**
     * Parse the passed date string and return a Date object.
     * Currently recognized Date formats are:
     * LONG - January 12, 3025
     * FULL - Tuesday, April 12, 1952 AD
     * MEDIUM - Jan 12, 1952
     * MM/dd/yyyy
     * yyyy-MM-dd
     *
     * @param dateString The date to be parsed.
     * @return the parsed date
     */
    private LocalDate parseDate(String dateString) {
        DateTimeFormatter[] dateFormats = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern(MekHQ.getMHQOptions().getDisplayDateFormat())
                        .withLocale(MekHQ.getMHQOptions().getDateLocale()),
                DateTimeFormatter.ofPattern(MekHQ.getMHQOptions().getLongDisplayDateFormat())
                        .withLocale(MekHQ.getMHQOptions().getDateLocale()),
                DateTimeFormatter.ofPattern("MMMM d, yyyy")
                        .withLocale(MekHQ.getMHQOptions().getDateLocale()),
                DateTimeFormatter.ofPattern("E, MMMM d, yyyy G")
                        .withLocale(MekHQ.getMHQOptions().getDateLocale()),
                DateTimeFormatter.ofPattern("E, MMMM d, yyyy")
                        .withLocale(MekHQ.getMHQOptions().getDateLocale()),
                DateTimeFormatter.ofPattern("MMM d, yyyy")
                        .withLocale(MekHQ.getMHQOptions().getDateLocale()),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
                        .withLocale(MekHQ.getMHQOptions().getDateLocale()),
                DateTimeFormatter.ISO_LOCAL_DATE.withLocale(MekHQ.getMHQOptions().getDateLocale())
        };
        for (DateTimeFormatter format : dateFormats) {
            try {
                return LocalDate.parse(dateString, format);
            } catch (Exception ignored) {

            }
        }
        return null;
    }

    @Override
    public void keyTyped(KeyEvent event) {}

    @Override
    public void keyPressed(KeyEvent event) {}

    @Override
    public void keyReleased(KeyEvent event) {}

    /**
     * Updates the date of the {@link DateChooser} instance based on the value in the dateField.
     * If the new date is valid, the dialog's controls are updated with the new date.
     *
     * @return {@link true} if the update is successful, {@link false} otherwise.
     */
    private boolean updateDateFromDateField() {
        LocalDate newDate = parseDate(dateField.getText());

        if (newDate == null) {
            JOptionPane.showMessageDialog(null,
                "Invalid Date Format\nTry: yyyy-MM-dd", "Date Format",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (newDate.isBefore(MIN_DATE)) {
            int choice = JOptionPane.showOptionDialog(
                null,
                resources.getString("minDate.body"),
                resources.getString("minDate.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, // Use default icon
                new String[]{
                    resources.getString("minDate.confirm"),
                    resources.getString("minDate.cancel")},
                resources.getString("minDate.cancel")
            );

            if (choice != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        setDate(newDate);
        return true;
    }

    /**
     * Creates a JButton representing a specific era.
     *
     * @param era The era index.
     *            The possible values are:
     *            - 0: Age of War
     *            - 1: Star League
     *            - 2: Early Succession War
     *            - 3: Late Succession War (Lostech)
     *            - 4: Late Succession War (Renaissance)
     *            - 5: Clan Invasion
     *            - 6: Civil War
     *            - 7: Jihad
     *            - 8: Early Republic
     *            - 9: Late Republic
     *            - 10: Dark Age
     *            - 11: IlClan
     * @return The created JButton object representing the specified era.
     */
    private JButton createEraButton(int era) {
          String reference = switch (era) {
            case 0 -> "eraAgeOfWar";
            case 1 -> "eraStarLeague";
            case 2 -> "eraEarlySuccessionWar";
            case 3 -> "eraLateSuccessionWarLosTech";
            case 4 -> "eraLateSuccessionWarRenaissance";
            case 5 -> "eraClanInvasion";
            case 6 -> "eraCivilWar";
            case 7 -> "eraJihad";
            case 8 -> "eraEarlyRepublic";
            case 9 -> "eraLateRepublic";
            case 10 -> "eraDarkAge";
            case 11 -> "eraIlClan";
              default -> throw new IllegalStateException("Unexpected value mekhq/gui/dialog/DateChooser.java/createEraButton: "
                  + era);
          };

        String label = String.format("<html><center>%s%s</center></html>",
            resources.getString(reference + ".text"),
            resources.getString(reference + ".year"));

        JButton button = new JButton(label);
        button.setToolTipText(wordWrap(resources.getString(reference + ".tooltip")));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.addActionListener(e -> turningPointsDialog(era, reference));

        return button;
    }

    /**
     * Displays a dialog window with buttons representing turning points in history based on the era provided.
     *
     * @param era an integer value specifying the era of turning points to display
     */
    private void turningPointsDialog(int era, String reference) {
        TurningPoints turningPointData = getTurningPoints(era);

        JPanel panelDescriptionContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel panelDescription = new JLabel(String.format(
            "<html><div style='width:500px;'><center><font size='6'>%s</font></center><justify>%s</justify></div></html>",
            resources.getString(reference + ".text"),
            resources.getString(reference + ".tooltip")));
        panelDescriptionContainer.add(panelDescription);

        JDialog turningPointsDialog = new JDialog();
        turningPointsDialog.setTitle(resources.getString("turningPoints.title"));
        turningPointsDialog.setModalityType(ModalityType.APPLICATION_MODAL);

        JPanel buttonPanel = new JPanel();
        final List<LocalDate> finalTurningPointDates = turningPointData.turningPointDates();

        for (int index = 1; index <= turningPointData.turningPoints().size(); index++) {
            JButton eraButtonN = new JButton(String.format("<html><center><b>" + resources.getString(turningPointData.turningPoints().get(index - 1) + ".text")
                + "</b><br>(" + finalTurningPointDates.get(index - 1).toString() + ")</center></html>"));
            eraButtonN.setToolTipText(wordWrap(String.format(
                resources.getString(turningPointData.turningPoints().get(index - 1) + ".tooltip"))));

            final int finalIndex = index - 1;
            eraButtonN.addActionListener(e -> {
                setDate(finalTurningPointDates.get(finalIndex));
                turningPointsDialog.dispose();
            });

            buttonPanel.add(eraButtonN);
        }

        JLabel eraLogoLabel = new JLabel();
        eraLogoLabel.setIcon(turningPointData.eraLogo());
        eraLogoLabel.setHorizontalAlignment(JLabel.CENTER);

        // Create a JPanel to hold the description and buttons, then add to the center of the dialog
        JPanel descriptionAndButtonsPanel = new JPanel(new BorderLayout());
        descriptionAndButtonsPanel.add(panelDescriptionContainer, BorderLayout.NORTH);
        descriptionAndButtonsPanel.add(buttonPanel, BorderLayout.CENTER);

        turningPointsDialog.getContentPane().add(eraLogoLabel, BorderLayout.PAGE_START);
        turningPointsDialog.getContentPane().add(descriptionAndButtonsPanel, BorderLayout.CENTER);

        // set turningPointsDialog size and location, and make it visible
        setResizable(false);
        turningPointsDialog.pack();
        turningPointsDialog.setMinimumSize(turningPointsDialog.getSize());
        turningPointsDialog.setLocationRelativeTo(null);
        turningPointsDialog.setVisible(true);
    }

    /**
     * Retrieves turning points based on the given era.
     *
     * @param era the era for which turning points are requested
     * @return {@link TurningPoints} object containing the turning points, their dates, and era logo
     */
    private static TurningPoints getTurningPoints(int era) {
        final String LOGO_DIRECTORY = "data/images/universe/";
        final String LOGO_FILE_TYPE = ".png";

        List<String> turningPoints;
        List<LocalDate> turningPointDates;
        ImageIcon eraLogo;

        switch (era) {
            case 0 -> {
                turningPoints = List.of("TerranHegemonyFounded", "RiseOfTheBattleMek");
                turningPointDates = List.of(
                    LocalDate.of(2315, 6, 2),
                    LocalDate.of(2475, 1, 1));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_starleague" + LOGO_FILE_TYPE);
            }
            case 1 -> {
                turningPoints = List.of("ReunificationWar", "FirstHiddenWar", "SecondHiddenWar",
                    "ThirdHiddenWar", "AmarisCivilWar");
                turningPointDates = List.of(
                    LocalDate.of(2571, 1, 1),
                    LocalDate.of(2681, 1, 1),
                    LocalDate.of(2725, 1, 1),
                    LocalDate.of(2741, 1, 1),
                    LocalDate.of(2766, 11, 26));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_starleague" + LOGO_FILE_TYPE);
            }
            case 2 -> {
                turningPoints = List.of("OperationExodus", "OperationKlondike", "SecondSuccessionWar",
                    "ThirdSuccessionWar");
                turningPointDates = List.of(
                    LocalDate.of(2784, 2, 14),
                    LocalDate.of(2821, 7, 2),
                    LocalDate.of(2830, 1, 1),
                    LocalDate.of(2866, 1, 1));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_sw" + LOGO_FILE_TYPE);
            }
            case 3 -> {
                turningPoints = List.of("OperationFreedom");
                turningPointDates = List.of(LocalDate.of(2866, 1, 1));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_sw" + LOGO_FILE_TYPE);
            }
            case 4 -> {
                turningPoints = List.of("ThirdSuccessionWarEnds", "FourthSuccessionWar", "FRRFounded",
                    "WarOf3039");
                turningPointDates = List.of(
                    LocalDate.of(3025, 1, 1),
                    LocalDate.of(3028, 8, 20),
                    LocalDate.of(3034, 3, 13),
                    LocalDate.of(3039, 4, 16));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_sw" + LOGO_FILE_TYPE);
            }
            case 5 -> {
                turningPoints = List.of("FirstContact", "YearOfPeace", "Tukayyid", "RefusalWar",
                    "OperationBulldog");
                turningPointDates = List.of(
                    LocalDate.of(3049, 1, 1),
                    LocalDate.of(3050, 10, 31),
                    LocalDate.of(3052, 5, 1),
                    LocalDate.of(3057, 9, 1),
                    LocalDate.of(3059, 5, 1));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_claninvasion" + LOGO_FILE_TYPE);
            }
            case 6 -> {
                turningPoints = List.of("FCCWStarts", "JadeFalconOffensive");
                turningPointDates = List.of(
                    LocalDate.of(3062, 11, 16),
                    LocalDate.of(3064, 5, 10));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_civilwar" + LOGO_FILE_TYPE);
            }
            case 7 -> {
                turningPoints = List.of("FirstBattleOfHarlech", "WarsOfReaving", "OperationScour");
                turningPointDates = List.of(
                    LocalDate.of(3067, 10, 15),
                    LocalDate.of(3071, 12, 1),
                    LocalDate.of(3077, 1, 10));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_jihad" + LOGO_FILE_TYPE);
            }
            case 8 -> {
                turningPoints = List.of("RepublicFounded", "OperationGoldenDawn");
                turningPointDates = List.of(
                    LocalDate.of(3081, 3, 7),
                    LocalDate.of(3081, 4, 3));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_darkage" + LOGO_FILE_TYPE);
            }
            case 9 -> {
                turningPoints = List.of("SecondCombineDominionWar", "VictoriaWar", "CapellanCrusades");
                turningPointDates = List.of(
                    LocalDate.of(3098, 9, 14),
                    LocalDate.of(3103, 9, 7),
                    LocalDate.of(3111, 10, 11));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_darkage" + LOGO_FILE_TYPE);
            }
            case 10 -> {
                turningPoints = List.of("GreyMonday");
                turningPointDates = List.of(LocalDate.of(3132, 8, 7));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_darkage" + LOGO_FILE_TYPE);
            }
            case 11 -> {
                turningPoints = List.of("BattleOfTerra");
                turningPointDates = List.of(LocalDate.of(3151, 1, 1));
                eraLogo = new ImageIcon(LOGO_DIRECTORY + "era_ilclan" + LOGO_FILE_TYPE);
            }
            default -> throw new IllegalStateException(
                "Unexpected value mekhq/gui/turningPointsDialog/DateChooser.java/turningPointsDialog: "
                + era);
        }

        return new TurningPoints(turningPoints, turningPointDates, eraLogo);
    }

    /**
     * Represents a record for storing turning points in history.
     * Contains lists of turning point names, their corresponding dates, and an {@link ImageIcon} of the era logo.
     */
    private record TurningPoints(List<String> turningPoints, List<LocalDate> turningPointDates, ImageIcon eraLogo) {}
}
