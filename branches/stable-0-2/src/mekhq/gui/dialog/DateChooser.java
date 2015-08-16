/*********************  DateChooser.java **************************/

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;

import mekhq.MekHQ;

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

    private final DateFormat MMDDYYYY = new SimpleDateFormat("MM/dd/yyyy");

	private static final ArrayList<String> monthNames;
	static {
		monthNames = new ArrayList<String>(12);
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
	};

	private GregorianCalendar date;
    private GregorianCalendar workingDate;
	private JLabel monthLabel;
	private JLabel yearLabel;
	private JPanel dayGrid;
	private boolean ready;

    // Stores the user-input date.
    private JFormattedTextField dateField = null;

	/**
	 * Constructor for DateChooser
	 * 
	 * @param owner
	 *            JFrame instance, owner of DateChooser dialog
	 * @param d
	 *            GregorianCalendar instance that will be the initial date for
	 *            this dialog
	 */
	public DateChooser(java.awt.Frame owner, GregorianCalendar d) {
		super(owner, "Date Chooser", true);
		date = d;
        workingDate = date;

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		JPanel yearPane = new JPanel();
		JPanel monthPane = new JPanel();
		yearPane.setLayout(new BoxLayout(yearPane, BoxLayout.X_AXIS));
		monthPane.setLayout(new BoxLayout(monthPane, BoxLayout.X_AXIS));

		JButton[] navButton = new JButton[4];

		// build the panel with month name and navigation buttons
		monthPane.add(navButton[0] = new JButton("<"));
		monthPane.add(monthLabel = new JLabel(String.valueOf(monthNames
				.get(date.get(GregorianCalendar.MONTH))), JLabel.CENTER));
		monthLabel.setMinimumSize(new Dimension(80, 17));
		monthLabel.setMaximumSize(new Dimension(80, 17));
		monthLabel.setPreferredSize(new Dimension(80, 17));
		monthPane.add(navButton[1] = new JButton(">"));

		// build the panel with year and navigation buttons
		yearPane.add(navButton[2] = new JButton("<<"));
		yearPane.add(
				yearLabel = new JLabel(String.valueOf(date
						.get(GregorianCalendar.YEAR)), JLabel.CENTER),
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
        try {
            dateField = new JFormattedTextField();
            dateField.addFocusListener(this);
            dateField.addKeyListener(this);
            dateField.setFormatterFactory(new DefaultFormatterFactory(new DateFormatter(MMDDYYYY)));
            dateField.setText(dateField.getFormatter().valueToString(date.getTime()));
            dateField.setToolTipText("Date of the transaction.");
            dateField.setName("dateField");
            dateField.setHorizontalAlignment(SwingConstants.CENTER);
            contentPane.add(dateField, BorderLayout.SOUTH);
            dateField.setColumns(10);
        } catch (ParseException e) {
            MekHQ.logError(e);
        }

        setResizable(false);
		ready = false;
		pack();

		// center this dialog over the owner
		int xPos = (int) (owner.getLocation().getX() + (owner.getWidth() / 2) - (getWidth() / 2));
		int yPos = (int) (owner.getLocation().getY() + (owner.getHeight() / 2) - (getHeight() / 2));
		setLocation(xPos, yPos);
	}

	/**
	 * Return the last selected date for this instance of DateChooser
	 */
	public GregorianCalendar getDate() {
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
	public void actionPerformed(ActionEvent evt) {
		String label = ((JButton) evt.getSource()).getText();

		if (label.equals("<")) {
			int m = monthNames.indexOf(monthLabel.getText());
			m = prevMonth(m);
			monthLabel.setText((String) monthNames.get(m));
			updateDayGrid(false);
		} else if (label.equals(">")) {
			int m = monthNames.indexOf(monthLabel.getText());
			m = nextMonth(m);
			monthLabel.setText((String) monthNames.get(m));
			updateDayGrid(false);
		} else if (label.equals("<<")) {
			int y = 0;
			try {
				y = Integer.parseInt(yearLabel.getText());
			} catch (NumberFormatException e) {
				System.err.println(e.toString());
			}
			yearLabel.setText(String.valueOf(--y));
			updateDayGrid(false);
		} else if (label.equals(">>")) {
			int y = 0;
			try {
				y = Integer.parseInt(yearLabel.getText());
			} catch (NumberFormatException e) {
				System.err.println(e.toString());
			}
			yearLabel.setText(String.valueOf(++y));
			updateDayGrid(false);
		} else {
			int m = monthNames.indexOf(monthLabel.getText());
			int y = 0;
			int d = 0;
			try {
				y = Integer.parseInt(yearLabel.getText());
				d = Integer.parseInt(label);
			} catch (NumberFormatException e) {
				System.err.println(e.toString());
			}
			date = new GregorianCalendar(y, m, d);
			date.setLenient(false);
			ready = true;

            //Set the date field to the new date.
            dateField.setText(MMDDYYYY.format(date.getTime()));
			setVisible(false);
		}
	}

    /**
     * Updates the dialog's controls with the passed in date.
     *
     * @param cal The date to be displayed.
     */
    private void setDate(GregorianCalendar cal, boolean fromDateField) {
        date = cal;
        date.setLenient(false);
        ready = true;
        monthLabel.setText(monthNames.get(cal.get(Calendar.MONTH)));
        yearLabel.setText("" + cal.get(Calendar.YEAR));
        updateDayGrid(fromDateField);

    }

    /**
     * Updates the dialog's controls with the passed in date.
     *
     * @param date The date to be displayed.
     */
    private void setDate(Date date, boolean fromDateField) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        setDate(cal, fromDateField);
    }

	/**
	 * This method is used by DateChooser to calculate and display days of the
	 * month in correct format for the month currently displayed. Days of the
	 * months are displayed as JButtons that the user can select. DateChooser's
	 * current day is higlighted in red color.
	 */
	private void updateDayGrid(boolean fromDateField) {
		dayGrid.removeAll();

		// get the currently selected month and year
		int m = monthNames.indexOf(monthLabel.getText());
		int y = 0;
		try {
			y = Integer.parseInt(yearLabel.getText());
		} catch (NumberFormatException e) {
			System.err.println(e.toString());
		}

		// look at the first day of the month for this month
		GregorianCalendar temp = new GregorianCalendar(y, m, 1);
		temp.setLenient(false);
		int offset = 0;

		// decide what day of the week is the first day of this month
		switch (temp.get(GregorianCalendar.DAY_OF_WEEK)) {
		case GregorianCalendar.MONDAY:
			offset = 0;
			break;
		case GregorianCalendar.TUESDAY:
			offset = 1;
			break;
		case GregorianCalendar.WEDNESDAY:
			offset = 2;
			break;
		case GregorianCalendar.THURSDAY:
			offset = 3;
			break;
		case GregorianCalendar.FRIDAY:
			offset = 4;
			break;
		case GregorianCalendar.SATURDAY:
			offset = 5;
			break;
		case GregorianCalendar.SUNDAY:
			offset = 6;
			break;
		}

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
			if (i == date.get(Calendar.DATE) && m == date.get(Calendar.MONTH)
					&& y == date.get(Calendar.YEAR)) {
				day.setForeground(Color.red);
                workingDay = i; //Store the correct day of the month.
			}
		}

		// display the remaining empty slots to preserve the structure
		for (int i = (offset + getLastDay() + 1); i <= 42; i++) {
			dayGrid.add(new JLabel(""));
		}

        //Update the date field with the newly selected date.
        if (dateField != null && !fromDateField) {
            workingDate = new GregorianCalendar(y, m, workingDay);
            String textDate = MMDDYYYY.format(workingDate.getTime());
            dateField.setText(textDate);
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
			System.err.println(e.toString());
		}

		if ((m == 4) || (m == 6) || (m == 9) || (m == 11)) {
			return (30);
		} else if (m == 2) {
			if (date.isLeapYear(y)) {
				return (29);
			}
			return (28);
		}
		return (31);
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
     *
     * @param dateString The date to be parsed.
     * @return
     */
    private Date parseDate(String dateString) {
        DateFormat[] dateFormats = new DateFormat[]
        {
                DateFormat.getDateInstance(DateFormat.LONG),
                DateFormat.getDateInstance(DateFormat.FULL),
                DateFormat.getDateInstance(DateFormat.DEFAULT),
                DateFormat.getDateInstance(DateFormat.MEDIUM),
                MMDDYYYY
        };
        for (DateFormat format : dateFormats) {
            try {
                Date date = format.parse(dateString);
                return date;
            } catch (ParseException e1) {
                //continue
            }
        }
        return null;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void keyPressed(KeyEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
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
                updateDateFromDateField();
                setVisible(false);
            }
        }
    }

    /**
     * Sets the dialog's date based on the value in the date field.
     */
    private void updateDateFromDateField() {
        Date newDate = parseDate(dateField.getText());
        if (newDate == null) {
            JOptionPane.showMessageDialog(this, "Invalid Date Format\nTry: " + MMDDYYYY, "Date Format", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setDate(newDate, true);
    }
}