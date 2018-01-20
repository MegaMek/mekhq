package mekhq;

public class NullEntityException extends Exception {
	/**
	 *
	 */
	private static final long serialVersionUID = -8953076304368184685L;

	String mistake;
	//----------------------------------------------
	// Default constructor - initializes instance variable to unknown
	public NullEntityException()
	{
		super();             // call superclass constructor
        mistake = "unknown";
	}

    //-----------------------------------------------
    // Constructor receives some kind of message that is saved in an instance variable.
	public NullEntityException(String err)
	{
		super(err);     // call super class constructor
        mistake = err;  // save message
	}

    //------------------------------------------------
    // public method, callable by exception catcher. It returns the error message.
	public String getError()
	{
		return mistake;
	}
}
