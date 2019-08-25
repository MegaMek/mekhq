/*********************  DateChooser.java **************************/

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.text.DefaultFormatterFactory;

import mekhq.MekHQ;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 * Hovanes Gambaryan Henry Demirchian CSUN, CS 585 Professor Mike Barnes
 * December 06, 2000
 *
 * DateChooser class is a general GUI based date chooser. It allows the user to
 * select an instance of GregorianCalendar defined in java.util package.
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
 * // initial date GregorianCalendar date = new GregorianCalendar()
 *
 * // The owner is the JFrame of the application ("AppClass.this")
 *
 * // show the date chooser DateChooser dc = new DateChooser(owner, date);
 *
 * // user can eiter choose a date or cancel by closing if (dc.showDateChooser()
 * == DateChooser.OK_OPTION) { date = dc.getDate(); }
 */

public class DateChooser extends JDialog implements ActionListener, FocusListener, KeyListener {
    private static final long serialVersionUID = 4353945278962427075L;

    public static final int OK_OPTION = 1;
    public static final int CANCEL_OPTION = 2;

    private static final DateTimeFormatter STANDARD_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private static final DateTimeFormatter[] SUPPORTED_DATE_FORMATS = new DateTimeFormatter[]
    {
        STANDARD_DATE_FORMAT,
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.BASIC_ISO_DATE,
        DateTimeFormatter.ISO_DATE,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL),
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG),
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT),
    };

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM");

    private LocalDate date;
    private LocalDate workingDate;

    private JLabel monthLabel;
    private JLabel yearLabel;
    private JPanel dayGrid;

    private boolean ready;

    // Stores the user-input date.
    private JFormattedTextField dateField = null;

    public DateChooser(Frame owner, GregorianCalendar calendar) {
        this(owner, toLocalDate(calendar));
    }

    /**
     * Constructor for DateChooser
     *
     * @param owner JFrame instance, owner of DateChooser dialog
     * @param d LocalDate instance that will be the initial date for
     *          this dialog
     */
    public DateChooser(Frame owner, LocalDate d) {
        super(owner, "Date Chooser", true);
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
        navButton[0] = new JButton("<");
        navButton[0].setActionCommand("PREVIOUS_MONTH");
        navButton[1] = new JButton(">");
        navButton[1].setActionCommand("NEXT_MONTH");
        monthLabel = new JLabel(MONTH_FORMAT.format(date), JLabel.CENTER);
        monthPane.add(navButton[0]);
        monthPane.add(monthLabel);
        monthLabel.setMinimumSize(new Dimension(80, 17));
        monthLabel.setMaximumSize(new Dimension(80, 17));
        monthLabel.setPreferredSize(new Dimension(80, 17));
        monthPane.add(navButton[1]);

        // build the panel with year and navigation buttons
        navButton[2] = new JButton("<<");
        navButton[2].setActionCommand("PREVIOUS_YEAR");
        navButton[3] = new JButton(">>");
        navButton[3].setActionCommand("NEXT_YEAR");
        yearLabel = new JLabel(String.valueOf(date.getYear()), JLabel.CENTER);
        yearPane.add(navButton[2]);
        yearPane.add(yearLabel, BorderLayout.CENTER);
        yearLabel.setMinimumSize(new Dimension(50, 17));
        yearLabel.setMaximumSize(new Dimension(50, 17));
        yearLabel.setPreferredSize(new Dimension(50, 17));
        yearPane.add(navButton[3]);

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
        try {
            dateField = new JFormattedTextField();
            dateField.addFocusListener(this);
            dateField.addKeyListener(this);
            dateField.setFormatterFactory(new DefaultFormatterFactory(new LocalDateTimeFormatter(STANDARD_DATE_FORMAT)));
            dateField.setText(STANDARD_DATE_FORMAT.format(date));
            dateField.setToolTipText("Date of the transaction.");
            dateField.setName("dateField");
            dateField.setHorizontalAlignment(SwingConstants.CENTER);
            contentPane.add(dateField, BorderLayout.SOUTH);
            dateField.setColumns(10);
        } catch (DateTimeException e) {
            MekHQ.getLogger().error(getClass(), "<init>(Frame,GregorianCalendar)", e);
        }

        setResizable(false);
        ready = false;
        pack();

        // center this dialog over the owner
        setLocationRelativeTo(owner);
        setUserPreferences();
    }

    class LocalDateTimeFormatter extends AbstractFormatter {

        private static final long serialVersionUID = 9194596776652247593L;

        private final DateTimeFormatter formatter;

        public LocalDateTimeFormatter(DateTimeFormatter outputFormat) {
            formatter = outputFormat;
        }

        @Override
        public Object stringToValue(String text) throws ParseException {
            if (null == text) {
                return null;
            }
            return parseDate(text);
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (null == value) {
                return null;
            }
            return formatter.format((LocalDate)value);
        }

    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(DateChooser.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    /**
     * Get the last selected date.
     * @return The last selected date.
     *
     * @deprecated Use {@link getLocalDate}
     */
    @Deprecated
	public GregorianCalendar getDate() {
		return new GregorianCalendar(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
    }

    /**
     * Gets the last selected date.
     * @return The last selected date.
     */
    public LocalDate getLocalDate() {
        return date;
    }

    private static LocalDate toLocalDate(GregorianCalendar c) {
        return LocalDate.of(c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH) + 1,
                            c.get(Calendar.DAY_OF_MONTH));
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
            return (OK_OPTION);
        } else {
            return (CANCEL_OPTION);
        }
    }

    /**
     * Action handler for this dialog, which handles all the button presses.
     *
     * @param evt
     *            ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getActionCommand().equalsIgnoreCase("PREVIOUS_MONTH")) {
            workingDate = workingDate.minusMonths(1);
            updateDayGrid(false);
        } else if (evt.getActionCommand().equalsIgnoreCase("NEXT_MONTH")) {
            workingDate = workingDate.plusMonths(1);
            updateDayGrid(false);
        } else if (evt.getActionCommand().equalsIgnoreCase("PREVIOUS_YEAR")) {
            workingDate = workingDate.minusYears(1);
            updateDayGrid(false);
        } else if (evt.getActionCommand().equalsIgnoreCase("NEXT_YEAR")) {
            workingDate = workingDate.plusYears(1);
            updateDayGrid(false);
        } else if (evt.getActionCommand().equalsIgnoreCase("SET_DAY")) {
            JButton source = (JButton)evt.getSource();
            workingDate = workingDate.withDayOfMonth(Integer.parseInt(source.getText()));

            date = workingDate;
            ready = true;

            //Set the date field to the new date.
            dateField.setText(STANDARD_DATE_FORMAT.format(date));
            setVisible(false);
        }
    }

    /**
     * Updates the dialog's controls with the passed in date.
     *
     * @param cal The date to be displayed.
     */
    private void setDate(LocalDate d, boolean fromDateField) {
        date = workingDate = d;
        ready = true;
        monthLabel.setText(MONTH_FORMAT.format(d));
        yearLabel.setText(Integer.toString(d.getYear()));
        dateField.setText(STANDARD_DATE_FORMAT.format(date));
        updateDayGrid(fromDateField);
    }

    /**
     * This method is used by DateChooser to calculate and display days of the
     * month in correct format for the month currently displayed. Days of the
     * months are displayed as JButtons that the user can select. DateChooser's
     * current day is higlighted in red color.
     */
    private void updateDayGrid(boolean fromDateField) {
        dayGrid.removeAll();

        // look at the first day of the month for this month
        int offset = 0;

        // decide what day of the week is the first day of this month
        switch (workingDate.getDayOfWeek()) {
            case MONDAY:
                offset = 0;
                break;
            case TUESDAY:
                offset = 1;
                break;
            case WEDNESDAY:
                offset = 2;
                break;
            case THURSDAY:
                offset = 3;
                break;
            case FRIDAY:
                offset = 4;
                break;
            case SATURDAY:
                offset = 5;
                break;
            case SUNDAY:
                offset = 6;
                break;
        }

        monthLabel.setText(MONTH_FORMAT.format(workingDate));
        yearLabel.setText(Integer.toString(workingDate.getYear()));

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
        for (int i = 1; i <= getLastDay(); i++) {
            JButton day = new JButton(String.valueOf(i));

            dayGrid.add(day);
            day.setToolTipText("Click on a day to choose it");
            day.setActionCommand("SET_DAY");
            day.addActionListener(this);

            // show the current day in bright red.
            if (i == date.getDayOfMonth()
                    && workingDate.getMonthValue() == date.getMonthValue()
                    && workingDate.getYear() == date.getYear()) {
                day.setForeground(Color.red);
            }
        }

        // display the remaining empty slots to preserve the structure
        for (int i = (offset + getLastDay() + 1); i <= 42; i++) {
            dayGrid.add(new JLabel(""));
        }

        //Update the date field with the newly selected date.
        if (dateField != null && !fromDateField) {
            String textDate = STANDARD_DATE_FORMAT.format(workingDate);
            dateField.setText(textDate);
        }

        repaint();
        validate();
    }

    /**
     * Return the value of the last day in the currently selected month
     */
    private int getLastDay() {
        return YearMonth.of(workingDate.getYear(), workingDate.getMonthValue())
                        .lengthOfMonth();
    }

    /**
     * Select all text in the date field when it gains the focus.
     * @param e
     */
    @Override
    public void focusGained(FocusEvent e) {
        if (dateField.equals(e.getSource())) {
            SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                dateField.selectAll();
                            }
                        });
        }
    }

    /**
     * Update the date picker controls when the date field looses focus.
     * @param e
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
     *   MM/DD/YYYY
     *   YYYY-MM-DD
     *
     * @param dateString The date to be parsed.
     * @return
     */
    private static LocalDate parseDate(String dateString) {
        if (null == dateString) {
            return null;
        }
        for (DateTimeFormatter format : SUPPORTED_DATE_FORMATS) {
            try {
                TemporalAccessor ta = format.parseBest(dateString, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
                if (ta instanceof LocalDate) {
                    return (LocalDate)ta;
                } else if (ta instanceof LocalDateTime) {
                    return ((LocalDateTime)ta).toLocalDate();
                } else if (ta instanceof ZonedDateTime) {
                    return ((ZonedDateTime)ta).toLocalDate();
                }
            } catch (DateTimeParseException e1) {
                //continue
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
     * @param e
     */
    @Override
    public void keyReleased(KeyEvent e) {
        if (dateField.equals(e.getSource())) {
            if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                if (updateDateFromDateField()) {
                    setVisible(false);
                }
            }
        }
    }

    /**
     * Sets the dialog's date based on the value in the date field.
     * @return A value indicating whether or not the date was parsed
     *         successfully.
     */
    private boolean updateDateFromDateField() {
        LocalDate newDate = parseDate(dateField.getText());
        if (newDate == null) {
            JOptionPane.showMessageDialog(this, "Invalid Date Format\nTry: MM/DD/YYYY or YYYY-MM-DD", "Date Format", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        setDate(newDate, true);
        return true;
    }
}
