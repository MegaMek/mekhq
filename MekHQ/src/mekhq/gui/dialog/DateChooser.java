package mekhq.gui.dialog;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Hovanes Gambaryan Henry Demirchian CSUN, CS 585 Professor Mike Barnes
 * December 06, 2000
 *
 * DateChooser class is a general GUI based date chooser. It allows the user to
 * select an instance of LocalDate defined in java.time package.
 *
 * Programming API is similar to JFC's JColorChooser or JFileChooser. This class
 * can be used in any application to enable the user to select a date from a
 * visually displayed calendar.
 *
 * There is a lot of improvements that can be done over this class in areas of
 * functionality, usability, and appearance. But as is, the class can be easily
 * used from within any Java program.
 *
 * Typical usage is like:
 *
 * // initial date LocalDate date = LocalDate.now()
 *
 * // The owner is the JFrame of the application ("AppClass.this")
 *
 * // show the date chooser DateChooser dc = new DateChooser(owner, date);
 *
 * // user can either choose a date or cancel by closing if (dc.showDateChooser()
 * == DateChooser.OK_OPTION) { date = dc.getDate(); }
 */
public class DateChooser extends JDialog implements ActionListener, FocusListener, KeyListener {
    public static final int OK_OPTION = 1;
    public static final int CANCEL_OPTION = 2;

    private static final List<String> monthNames;
    static {
        monthNames = new ArrayList<>(12);
        monthNames.add("January");
        monthNames.add("February");
        monthNames.add("March");
        monthNames.add("April");
        monthNames.add("May");
        monthNames.add("June");
        monthNames.add("July");
        monthNames.add("August");
        monthNames.add("September");
        monthNames.add("October ");
        monthNames.add("November");
        monthNames.add("December");
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
     *            JDialog istance. Dialog that owns this
     * @param d
     *            LocalDate instance that will be the initial date for
     *            this dialog
     */
    public DateChooser(JDialog parentDialog, LocalDate d) {
        super(parentDialog, "Date Chooser", true);
        init(parentDialog, d);
    }

    /**
     * Constructor for DateChooser which does not have a parent dialog
     *
     * @param owner
     *            JFrame instance, owner of DateChooser dialog
     * @param d
     *            LocalDate instance that will be the initial date for
     *            this dialog
     */
    public DateChooser(JFrame owner, LocalDate d) {
        super(owner, "Date Chooser", true);
        init(owner, d);
    }

    private void init(Component owner, LocalDate d) {


        date = d;
        workingDate = date;

        // Ensure the dialog isn't hidden
        setAlwaysOnTop(true);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JPanel yearPane = new JPanel();
        JPanel monthPane = new JPanel();
        yearPane.setLayout(new BoxLayout(yearPane, BoxLayout.X_AXIS));
        monthPane.setLayout(new BoxLayout(monthPane, BoxLayout.X_AXIS));

        JButton[] navButton = new JButton[4];

        // build the panel with month name and navigation buttons
        monthPane.add(navButton[0] = new JButton("<"));
        monthPane.add(monthLabel = new JLabel(String.valueOf(
                monthNames.get(date.getMonth().ordinal())), JLabel.CENTER));
        monthLabel.setMinimumSize(new Dimension(80, 17));
        monthLabel.setMaximumSize(new Dimension(80, 17));
        monthLabel.setPreferredSize(new Dimension(80, 17));
        monthPane.add(navButton[1] = new JButton(">"));

        // build the panel with year and navigation buttons
        yearPane.add(navButton[2] = new JButton("<<"));
        yearPane.add(
                yearLabel = new JLabel(String.valueOf(date.getYear()), JLabel.CENTER),
                BorderLayout.CENTER);
        yearLabel.setMinimumSize(new Dimension(50, 17));
        yearLabel.setMaximumSize(new Dimension(50, 17));
        yearLabel.setPreferredSize(new Dimension(50, 17));
        yearPane.add(navButton[3] = new JButton(">>"));

        // register a listener on the navigation buttons
        for (int i = 0; i < 4; i++) {
            navButton[i].addActionListener(this);
        }

        // set the tool tip text on the navigation buttons
        navButton[0].setToolTipText("Go to the previous month");
        navButton[1].setToolTipText("Go to tne next month");
        navButton[2].setToolTipText("Go to the previous year");
        navButton[3].setToolTipText("Go to the next year");

        // put the panel for months and years together and add some formatting
        JPanel topPane = new JPanel();
        topPane.setLayout(new BoxLayout(topPane, BoxLayout.X_AXIS));
        topPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        topPane.add(monthPane);
        topPane.add(Box.createRigidArea(new Dimension(20, 0)));
        topPane.add(yearPane);

        // create the panel that will hold the days of the months
        dayGrid = new JPanel(new GridLayout(7, 7));
        updateDayGrid(false);

        contentPane.add(topPane, BorderLayout.NORTH);
        contentPane.add(dayGrid, BorderLayout.CENTER);

        //Set up the date input text field with the current campaign date.
        dateField = new JFormattedTextField(date);
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

        //setResizable(false);
        ready = false;
        pack();

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
            LogManager.getLogger().error("Failed to set user preferences", ex);
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
     * @param evt ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        String label = ((JButton) evt.getSource()).getText();

        switch (label) {
            case "<": {
                int m = monthNames.indexOf(monthLabel.getText());
                m = prevMonth(m);
                monthLabel.setText(monthNames.get(m));
                updateDayGrid(false);
                break;
            }
            case ">": {
                int m = monthNames.indexOf(monthLabel.getText());
                m = nextMonth(m);
                monthLabel.setText(monthNames.get(m));
                updateDayGrid(false);
                break;
            }
            case "<<": {
                int y = 0;
                try {
                    y = Integer.parseInt(yearLabel.getText());
                } catch (NumberFormatException e) {
                    LogManager.getLogger().error("", e);
                }
                yearLabel.setText(String.valueOf(--y));
                updateDayGrid(false);
                break;
            }
            case ">>": {
                int y = 0;
                try {
                    y = Integer.parseInt(yearLabel.getText());
                } catch (NumberFormatException e) {
                    LogManager.getLogger().error("", e);
                }
                yearLabel.setText(String.valueOf(++y));
                updateDayGrid(false);
                break;
            }
            default: {
                int m = monthNames.indexOf(monthLabel.getText()) + 1;
                int y = 0;
                int d = 0;
                try {
                    y = Integer.parseInt(yearLabel.getText());
                    d = Integer.parseInt(label);
                } catch (NumberFormatException e) {
                    LogManager.getLogger().error("", e);
                }
                date = LocalDate.of(y, m, d);
                ready = true;

                //Set the date field to the new date.
                dateField.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
                setVisible(false);
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
        int m = monthNames.indexOf(monthLabel.getText()) + 1;
        int y = 0;
        try {
            y = Integer.parseInt(yearLabel.getText());
        } catch (NumberFormatException e) {
            LogManager.getLogger().error("", e);
        }

        // decide what day of the week is the first day of this month
        int offset = LocalDate.of(y, m, 1).getDayOfWeek().ordinal();


        // display 7 days of the week across the top
        dayGrid.add(new JLabel("Mon", JLabel.CENTER));
        dayGrid.add(new JLabel("Tue", JLabel.CENTER));
        dayGrid.add(new JLabel("Wed", JLabel.CENTER));
        dayGrid.add(new JLabel("Thu", JLabel.CENTER));
        dayGrid.add(new JLabel("Fri", JLabel.CENTER));
        dayGrid.add(new JLabel("Sat", JLabel.CENTER));
        dayGrid.add(new JLabel("Sun", JLabel.CENTER));

        // skip to the correct first day of the week for this month
        for (int i = 1; i <= offset; i++) {
            dayGrid.add(new JLabel(""));
        }

        // display days of the month for this month
        JButton day;
        int workingDay = 1; //Start at the first day of the month.
        for (int i = 1; i <= getLastDay(); i++) {
            dayGrid.add(day = new JButton(String.valueOf(i)));
            day.setToolTipText("Click on a day to choose it");
            day.addActionListener(this);

            // show the current day in bright red.
            if ((i == date.getDayOfMonth()) && (m == date.getMonth().ordinal())
                    && (y == date.getYear())) {
                day.setForeground(Color.red);
                workingDay = i; //Store the correct day of the month.
            }
        }

        // display the remaining empty slots to preserve the structure
        for (int i = (offset + getLastDay() + 1); i <= 42; i++) {
            dayGrid.add(new JLabel(""));
        }

        //Update the date field with the newly selected date.
        if ((dateField != null) && !fromDateField) {
            workingDate = LocalDate.of(y, m, workingDay);
            dateField.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(workingDate));
        }

        repaint();
        validate();
    }

    /**
     * Return the month following the one passed in as an argument. If the
     * argument is the las month of the year, return the first month.
     *
     * @param month
     *            Current month expressed as an integer (0 to 11).
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
     * @param month
     *            Current month expressed as an integer (0 to 11).
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
        int m = (monthNames.indexOf(monthLabel.getText()) + 1);
        int y = 0;
        try {
            y = Integer.parseInt(yearLabel.getText());
        } catch (NumberFormatException e) {
            LogManager.getLogger().error("", e);
        }

        return LocalDate.of(y, m, 1).lengthOfMonth();
    }

    /**
     * Select all text in the date field when it gains the focus.
     * @param e FocusEvent
     */
    @Override
    public void focusGained(FocusEvent e) {
        if (dateField.equals(e.getSource())) {
            SwingUtilities.invokeLater(() -> dateField.selectAll());
        }
    }

    /**
     * Update the date picker controls when the date field looses focus.
     * @param e FocusEvent
     */
    @Override
    public void focusLost(FocusEvent e) {
        if (dateField.equals(e.getSource())) {
            updateDateFromDateField();
        }
    }

    /**
     * Parse the passed date string and return a Date object.
     * Currently recognized Date formats are:
     *   LONG - January 12, 3025
     *   FULL - Tuesday, April 12, 1952 AD
     *   MEDIUM - Jan 12, 1952
     *   MM/dd/yyyy
     *   yyyy-MM-dd
     *
     * @param dateString The date to be parsed.
     * @return the parsed date
     */
    private LocalDate parseDate(String dateString) {
        DateTimeFormatter[] dateFormats = new DateTimeFormatter[]
        {
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
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    /**
     * Update the date chooser controls when the Enter key is pressed while the date field has the focus.  Then close
     * the dialog.
     *
     * @param e KeyEvent
     */
    @Override
    public void keyReleased(KeyEvent e) {
        if (dateField.equals(e.getSource())) {
            if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                updateDateFromDateField();
                setVisible(false);
            }
        }
    }

    /**
     * Sets the dialog's date based on the value in the date field.
     */
    private void updateDateFromDateField() {
        LocalDate newDate = parseDate(dateField.getText());
        if (newDate == null) {
            JOptionPane.showMessageDialog(this, "Invalid Date Format\nTry: yyyy-MM-dd", "Date Format", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setDate(newDate);
    }
}
