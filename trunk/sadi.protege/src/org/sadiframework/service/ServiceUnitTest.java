/**
 * 
 */
package org.sadiframework.service;

/**
 * @author Eddie Kawas
 * 
 */
public class ServiceUnitTest {

    private String inputFilePath, outputFilePath;

    /**
     * Default constructor: sets input/output file path to ""
     */
    public ServiceUnitTest() {
        this("", "");
    }

    /**
     * 
     * @param input
     *            the full input file path
     * @param output
     *            the full output file path
     */
    public ServiceUnitTest(String input, String output) {
        setInputFilePath(input);
        setOutputFilePath(output);
    }

    /**
     * 
     * @return the input file path
     */
    public String getInputFilePath() {
        return this.inputFilePath;
    }

    /**
     * 
     * @return the output file path
     */
    public String getOutputFilePath() {
        return this.outputFilePath;
    }

    /**
     * 
     * @param inputFilePath
     *            the full input file path
     */
    public void setInputFilePath(String inputFilePath) {
        if (inputFilePath == null)
            inputFilePath = "";

        this.inputFilePath = inputFilePath.trim();
    }

    /**
     * 
     * @param outputFilePath
     *            the full output file path
     */
    public void setOutputFilePath(String outputFilePath) {
        if (outputFilePath == null)
            outputFilePath = "";
        this.outputFilePath = outputFilePath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format(
                "# This is a SADI service unit test file\n\n[unitTest]\ninput=%s\noutput=%s\n",
                getInputFilePath(), getOutputFilePath());
    }

}
