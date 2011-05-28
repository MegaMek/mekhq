package mekhq;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Provides a text-field that makes suggestions using a provided data-vector.
 * You might have seen this on Google (tm), this is the Java implementation.
 *
 * @author David von Ah
 */
public class JSuggestField extends JTextField {

	/** unique ID for serialization */
	private static final long serialVersionUID = 1756202080423312153L;

	/** Dialog used as the drop-down list. */
	private JDialog d;

	/** Location of said drop-down list. */
	private Point location;

	/** List contained in the drop-down dialog. */
	private JList list;

	/**
	 * Vectors containing the original data and the filtered data for the
	 * suggestions.
	 */
	private Vector<String> data, suggestions;

	/**
	 * Separate matcher-thread, prevents the text-field from hanging while the
	 * suggestions are beeing prepared.
	 */
	private InterruptableMatcher matcher;

	/**
	 * Fonts used to indicate that the text-field is processing the request,
	 * i.e. looking for matches
	 */
	private Font busy, regular;

	/** Needed for the new narrowing search, so we know when to reset the list */
	private String lastWord = "";
	
	/**
	 * The last chosen variable which exists. Needed if user
	 * continued to type but didn't press the enter key
	 * */
	private String lastChosenExistingVariable;

	/** Listeners, fire event when a selection as occured */
	private LinkedList<ActionListener> listeners;

	/**
	 * Create a new JSuggestField.
	 *
	 * @param owner
	 *            Frame containing this JSuggestField
	 */
	public JSuggestField(Window owner) {
		super();
		data = new Vector<String>();
		suggestions = new Vector<String>();
		listeners = new LinkedList<ActionListener>();
		owner.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) {
				relocate();
			}

			@Override
			public void componentResized(ComponentEvent e) {
				relocate();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				relocate();
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				relocate();
			}
		});
		owner.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
				d.setVisible(false);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}

			@Override
			public void windowClosing(WindowEvent e) {
				d.dispose();
			}

			@Override
			public void windowClosed(WindowEvent e) {
				d.dispose();
			}

			@Override
			public void windowActivated(WindowEvent e) {
			}
		});
		addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				d.setVisible(false);
				
				if (getText().equals("") && e.getOppositeComponent() != null && e.getOppositeComponent().getName() != null) {
					if (!e.getOppositeComponent().getName().equals("suggestFieldDropdownButton")) {
						setText("Type a variable name here...");
					}
				} else if (getText().equals("")) {
					setText("Type a variable name here...");
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				if (getText().equals("Type a variable name here...")) {
					setText("");
				}
				
				showSuggest();
			}
		});
		d = new JDialog(owner);
		d.setUndecorated(true);
		d.setFocusableWindowState(false);
		d.setFocusable(false);
		list = new JList();
		list.addMouseListener(new MouseListener() {
			private int selected;

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (selected == list.getSelectedIndex()) {
					// provide double-click for selecting a suggestion
					setText((String) list.getSelectedValue());
					lastChosenExistingVariable = list.getSelectedValue().toString();
					fireActionEvent();
					d.setVisible(false);
				}
				selected = list.getSelectedIndex();
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
			}
		});
		d.add(new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		d.pack();
		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				relocate();
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					d.setVisible(false);
					return;
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					if (d.isVisible()) {
						list.setSelectedIndex(list.getSelectedIndex() + 1);
						list.ensureIndexIsVisible(list.getSelectedIndex() + 1);
						return;
					} else {
						showSuggest();
					}
				} else if (e.getKeyCode() == KeyEvent.VK_UP) {
					list.setSelectedIndex(list.getSelectedIndex() - 1);
					list.ensureIndexIsVisible(list.getSelectedIndex() - 1);
					return;
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER
						& list.getSelectedIndex() != -1 & suggestions.size() > 0) {
					setText((String) list.getSelectedValue());
					lastChosenExistingVariable = list.getSelectedValue().toString();
					fireActionEvent();
					d.setVisible(false);
					return;
				}
				showSuggest();
			}
		});
		regular = getFont();
		busy = new Font(getFont().getName(), Font.ITALIC, getFont().getSize());
	}

	/**
	 * Create a new JSuggestField.
	 *
	 * @param owner
	 *            Frame containing this JSuggestField
	 * @param data
	 *            Available suggestions
	 */
	public JSuggestField(Window owner, Vector<String> data) {
		this(owner);
		setSuggestData(data);
	}

	/**
	 * Sets new data used to suggest similar words.
	 *
	 * @param data
	 *            Vector containing available words
	 * @return success, true unless the data-vector was null
	 */
	public boolean setSuggestData(Vector<String> data) {
		if (data == null) {
			return false;
		}
		Collections.sort(data);
		this.data = data;
		list.setListData(data);
		return true;
	}

	/**
	 * Get all words that are available for suggestion.
	 *
	 * @return Vector containing Strings
	 */
	@SuppressWarnings("unchecked")
	public Vector<String> getSuggestData() {
		return (Vector<String>) data.clone();
	}

	/**
	 * Set preferred size for the drop-down that will appear.
	 *
	 * @param size
	 *            Preferred size of the drop-down list
	 */
	public void setPreferredSuggestSize(Dimension size) {
		d.setPreferredSize(size);
	}

	/**
	 * Set minimum size for the drop-down that will appear.
	 *
	 * @param size
	 *            Minimum size of the drop-down list
	 */
	public void setMinimumSuggestSize(Dimension size) {
		d.setMinimumSize(size);
	}

	/**
	 * Set maximum size for the drop-down that will appear.
	 *
	 * @param size
	 *            Maximum size of the drop-down list
	 */
	public void setMaximumSuggestSize(Dimension size) {
		d.setMaximumSize(size);
	}

	/**
	 * Force the suggestions to be displayed (Useful for buttons
	 * e.g. for using JSuggestionField like a ComboBox)
	 */
	public void showSuggest() {
		if (!getText().toLowerCase().contains(lastWord.toLowerCase())) {
			suggestions.clear();
		}
		if (suggestions.isEmpty()) {
			suggestions.addAll(data);
		}
		if (matcher != null) {
			matcher.stop = true;
		}
		matcher = new InterruptableMatcher();
		//matcher.start();
		SwingUtilities.invokeLater(matcher);
		lastWord = getText();
		relocate();
	}

	/**
	 * Force the suggestions to be hidden (Useful for buttons, e.g. to use
	 * JSuggestionField like a ComboBox)
	 */
	public void hideSuggest() {
		d.setVisible(false);
	}

	/**
	 * @return boolean Visibility of the suggestion window
	 */
	public boolean isSuggestVisible() {
		return d.isVisible();
	}

	/**
	 * Place the suggestion window under the JTextField.
	 */
	private void relocate() {
		try {
			location = getLocationOnScreen();
			location.y += getHeight();
			d.setLocation(location);
		} catch (IllegalComponentStateException e) {
			return; // might happen on window creation
		}
	}

	/**
	 * Inner class providing the independent matcher-thread. This thread can be
	 * interrupted, so it won't process older requests while there's already a
	 * new one.
	 */
	private class InterruptableMatcher extends Thread {
		/** flag used to stop the thread */
		private volatile boolean stop;

		/**
		 * Standard run method used in threads
		 * responsible for the actual search
		 */
		@Override
		public void run() {
			try {
				setFont(busy);
				Iterator<String> it = suggestions.iterator();
				String word = getText();
				while (it.hasNext()) {
					if (stop) {
						return;
					}
					// rather than using the entire list, let's rather remove
					// the words that don't match, thus narrowing
					// the search and making it faster
					if (!it.next().toLowerCase().startsWith(word.toLowerCase())) {
						it.remove();
					}
				}
				setFont(regular);
				if (suggestions.size() > 0) {
					list.setListData(suggestions);
					list.setSelectedIndex(0);
					list.ensureIndexIsVisible(0);
					d.setVisible(true);
				} else {
					d.setVisible(false);
				}
			} catch (Exception e) {
				// Despite all precautions, external changes have occurred.
				// Let the new thread handle it...
				return;
			}
		}
	}

	/**
	 * Adds a listener that notifies when a selection has occured
	 * @param listener
	 * 			ActionListener to use
	 */
	public void addSelectionListener(ActionListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes the Listener
	 * @param listener
	 * 			ActionListener to remove
	 */
	public void removeSelectionListener(ActionListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Use ActionListener to notify on changes
	 * so we don't have to create an extra event
	 */
	private void fireActionEvent() {
		ActionEvent event = new ActionEvent(this, 0, getText());
		for (ActionListener listener : listeners) {
			listener.actionPerformed(event);
		}
	}
	
	
	/**
	 * Returns the selected value in the drop down list
	 * 
	 * @return selected value from the user or null if the entered value does not exist
	 */
	public String getLastChosenExistingVariable() {
		return lastChosenExistingVariable;
	}
}

