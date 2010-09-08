package ca.wilkinsonlab.sadi.registry.utils;

import java.io.IOException;
import java.util.List;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.log4j.Logger;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.http.AccessToken;
import twitter4j.http.Authorization;
import twitter4j.http.OAuthAuthorization;
import twitter4j.http.RequestToken;
import ca.wilkinsonlab.sadi.registry.Registry;
import ca.wilkinsonlab.sadi.registry.ServiceBean;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Utility class to tweet when new services are registered.
 * @author Luke McCarthy
 */
public class Twitter
{
	private static final Logger log = Logger.getLogger(Twitter.class);
	
	private static String consumerKey = "ETUlln0cRAlhtRuxLVeABw";
	private static String consumerSecret = "4XDeRf2p1RysqoL8DKKi9gjqGve6lmlBTh6Zef4Pms";
	private static Resource subject = ResourceFactory.createResource("info@sadiframework.org");
	private static Property predicate = RDF.value;
	
	public static void tweetService(ServiceBean service) throws IOException
	{
		try {
			Status status = getTwitter().updateStatus(getUpdateMessage(service));
			log.info(String.format("successfully updated status to %s", status.getText()));
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	public static String getUpdateMessage(ServiceBean service)
	{
		String shortURL = null;
		try {
			shortURL = BitLy.getShortURL(String.format("http://sadiframework.org/registry/service.jsp?serviceURI=%s", service.getServiceURI()));
		} catch (IOException e) {
			log.error("error shortening URL", e);
		}
		StringBuilder buf = new StringBuilder();
		buf.append("new SADI service \"");
		buf.append(service.getName());
		buf.append("\" registered!");
		if (shortURL != null) {
			buf.append(" ");
			buf.append(shortURL);
		}
		return buf.toString();
	}
	
	public static twitter4j.Twitter getTwitter() throws Exception
	{
		AccessToken accessToken = retrieveAccessToken();
		ConfigurationBuilder config = new ConfigurationBuilder();
		config.setOAuthConsumerKey(consumerKey);
		config.setOAuthConsumerSecret(consumerSecret);
		config.setOAuthAccessToken(accessToken.getToken());
		config.setOAuthAccessTokenSecret(accessToken.getTokenSecret());
		Authorization authorization = new OAuthAuthorization(config.build(), consumerKey, consumerSecret, accessToken);
		return new TwitterFactory().getInstance(authorization);
	}
	
	public static AccessToken retrieveAccessToken() throws Exception
	{
		Model model = Registry.getRegistry().getModel();
		List<Statement> statements = model.listStatements(subject, predicate, (RDFNode)null).toList();
		if (statements.isEmpty())
			return null;
		String encoded = statements.get(0).getString();
		byte[] serialized = new BASE64Decoder().decodeBuffer(encoded);
		return (AccessToken)SerializationUtils.deserialize(serialized);
	}
	
	public static void storeAccessToken(AccessToken accessToken) throws Exception
	{
		Model model = Registry.getRegistry().getModel();
		if (model.contains(subject, predicate)) {
			model.removeAll(subject, predicate, null);
		}
		byte[] serialized = SerializationUtils.serialize(accessToken);
		String encoded = new BASE64Encoder().encodeBuffer(serialized);
		model.add(subject, predicate, encoded);
	}
	
	public static RequestToken getRequestToken() throws Exception
	{
		twitter4j.Twitter twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(consumerKey, consumerSecret);
		return twitter.getOAuthRequestToken();
	}
	
	public static AccessToken getAccessToken(RequestToken requestToken, String pin) throws IOException
	{
		twitter4j.Twitter twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(consumerKey, consumerSecret);
		try {
			if (!StringUtils.isEmpty(pin)) {
				return twitter.getOAuthAccessToken(requestToken, pin);
			} else {
				return twitter.getOAuthAccessToken();
			}
		} catch (TwitterException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
}
