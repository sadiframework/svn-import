package ca.wilkinsonlab.sadi.client;

import ca.wilkinsonlab.sadi.vocab.SADI;

public enum ServiceStatus
{
	OK("ok", SADI.ok.getURI()),
	SLOW("slow", SADI.slow.getURI()),
	INCORRECT("test case returning incorrect result", SADI.incorrect.getURI()),
	DEAD("not responding", SADI.dead.getURI()) ;
	
	private final String message;
	private final String uri;
	
	private ServiceStatus(String message, String uri)
	{
		this.message = message;
		this.uri = uri;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public String getURI()
	{
		return uri;
	}
	
	public static ServiceStatus uriToStatus(String statusURI)
	{
		for(ServiceStatus status : ServiceStatus.values()) {
			if (status.getURI().equals(statusURI)) {
				return status;
			}
		}
		throw new IllegalArgumentException(String.format("unrecogized URI for service status: %s", statusURI)); 
	}	
}