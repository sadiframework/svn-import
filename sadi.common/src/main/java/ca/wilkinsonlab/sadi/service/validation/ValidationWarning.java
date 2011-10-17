package ca.wilkinsonlab.sadi.service.validation;

public class ValidationWarning
{
	private String message;
	private String details;
	
	public ValidationWarning(String message)
	{
		this(message, null);
	}
	
	public ValidationWarning(String message, String details)
	{
		this.message = message;
		this.details = details;
	}

	/**
	 * @return the message
	 */
	public String getMessage()
	{
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message)
	{
		this.message = message;
	}

	/**
	 * @return the details
	 */
	public String getDetails()
	{
		return details;
	}

	/**
	 * @param details the details to set
	 */
	public void setDetails(String details)
	{
		this.details = details;
	}
}