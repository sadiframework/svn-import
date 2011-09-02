package ca.wilkinsonlab.sadi.client;

public enum ServiceStatus
{
	OK("ok"),
	SLOW("slow"),
	INCORRECT("test case returning incorrect result"),
	DEAD("not responding") ;
	
	private final String message;
	
	private ServiceStatus(String message)
	{
		this.message = message;
	}
	
	public String getMessage()
	{
		return message;
	}
}