package ca.wilkinsonlab.sadi.beans;

import java.io.Serializable;

public class TestCaseBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private String input;
	private String expectedOutput;
	
	public TestCaseBean()
	{
		input = null;
		expectedOutput = null;
	}

	public String getInput()
	{
		return input;
	}

	public void setInput(String input)
	{
		this.input = input;
	}

	public String getExpectedOutput()
	{
		return expectedOutput;
	}

	public void setExpectedOutput(String expectedOutput)
	{
		this.expectedOutput = expectedOutput;
	}
}
