package ca.wilkinsonlab.sadi.service.tester;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * A goal that describes the goals and parameters of the plugin.
 * @author Luke McCarthy
 * @goal help
 */
public class Help extends AbstractMojo
{
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		try {
			InputStream is = getClass().getResourceAsStream("help/help.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
			reader.close();
		} catch (Exception e) {
			getLog().error("error reading help file", e);
			throw new MojoFailureException("error reading help file");
		}
	}
}
