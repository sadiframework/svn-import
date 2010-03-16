package org.sadiframework.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;


import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.sadiframework.exceptions.SADIServiceException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationAssertionAxiomImpl;

public class Execute {

    private static String NEWLINE = System.getProperty("line.separator");

    public static String executeCgiService(String serviceEndpoint, String xml)
            throws SADIServiceException {
        try {
            // Construct data
            String postResult = post(serviceEndpoint, xml);
            return postResult;
        } catch (Exception e) {
            throw new SADIServiceException(e.getMessage());
        }
    }

    private static String USER_AGENT = "sadi-plugin/protege";

    private static String post(String serviceEndpoint, String xml) throws SADIServiceException {
        /*
         * SchemeRegistry schemeRegistry = new SchemeRegistry();
         * schemeRegistry.register( new Scheme("http",
         * PlainSocketFactory.getSocketFactory(), 80)); BasicHttpParams params =
         * new BasicHttpParams(); SingleClientConnManager connmgr = new
         * SingleClientConnManager(params, schemeRegistry);
         */

        // HttpClient client = new DefaultHttpClient(connmgr, params);
        HttpClient client = createHttpClient();
        HttpPost post = new HttpPost(serviceEndpoint);
        post.setEntity(new ByteArrayEntity(xml.getBytes()));

        try {
            HttpResponse response = client.execute(post);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() == 202) {
                return processIsDefinedBy(serviceEndpoint, response.getEntity().getContent());
            } else if (status.getStatusCode() == 200) {
                StringBuilder sb = new StringBuilder();
                String line;

                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response
                            .getEntity().getContent(), "UTF-8"));
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + NEWLINE);
                    }
                    reader.close();
                    return sb.toString();
                } catch (IOException e) {
                    throw new SADIServiceException(e.getMessage());
                }
            }
        } catch (ClientProtocolException e) {
            throw new SADIServiceException(e.getMessage());
        } catch (IOException e) {
            throw new SADIServiceException(e.getMessage());
        }

        return "";
    }

    private static String processIsDefinedBy(String endpoint, InputStream xml)
            throws SADIServiceException {

        HashMap<String, Boolean> definedByURLs = new HashMap<String, Boolean>();
        try {
            // extract all of the isDefinedBy URLs
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(xml);
            for (OWLAnnotationProperty oap : ontology.getAnnotationPropertiesInSignature()) {
                // System.out.println(oap.getIRI().toString());
                if (oap.getIRI().toString().equals(
                        "http://www.w3.org/2000/01/rdf-schema#isDefinedBy")) {
                    for (OWLAnnotationAxiom axiom : ontology.getAxioms(oap)) {
                        if (axiom instanceof OWLAnnotationAssertionAxiomImpl) {
                            OWLAnnotationAssertionAxiomImpl ax = (OWLAnnotationAssertionAxiomImpl) axiom;
                            definedByURLs.put(ax.getValue().toString(), new Boolean(false));
                        }
                    }
                }
            }
            manager.removeOntology(ontology);
        } catch (OWLOntologyCreationException e) {
            throw new SADIServiceException(e.getMessage());
        }

        // perform a get on each url now ...
        LinkedList<String> outputXML = new LinkedList<String>();

        for (String dUrl : definedByURLs.keySet()) {
            // our result from querying the isDefinedBy url
            String string = get(dUrl);
            // add our string
            if (string != null)
                outputXML.add(string);
        }

        // now we merge our output together
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        IRI mergedOntologyURI = IRI.create(endpoint);
        for (String s : outputXML) {
            try {
                // load the string into the ontology
                man.loadOntologyFromOntologyDocument(new ByteArrayInputStream(s.getBytes()));
            } catch (OWLOntologyCreationException e) {
                throw new SADIServiceException(e.getMessage());
            } catch (OWLOntologyChangeException e) {
                throw new SADIServiceException(e.getMessage());
            }
        }
        // Create our ontology merger
        OWLOntologyMerger merger = new OWLOntologyMerger(man);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            OWLOntology merged = merger.createMergedOntology(man, mergedOntologyURI);
            // create RDF/XML
            man.saveOntology(merged, new RDFXMLOntologyFormat(), stream);
        } catch (OWLOntologyStorageException e) {
            throw new SADIServiceException(e.getMessage());
        } catch (OWLOntologyCreationException e) {
            throw new SADIServiceException(e.getMessage());
        }
        // return merged data
        return stream.toString();
    }

    private static String get(String url) throws SADIServiceException {
        // process dUrl
        StatusLine status;
        int statCode = 0;
        do {
            HttpClient client = createHttpClient();
            HttpGet get = new HttpGet(url);
            if (statCode != 0) {
                // Pause for 15 seconds
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                }
            }
            try {
                HttpResponse response = client.execute(get);
                status = response.getStatusLine();
                statCode = status.getStatusCode();
                if (statCode == 202 || statCode == 200) {
                    StringBuilder sb = new StringBuilder();
                    String line;

                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response
                                .getEntity().getContent(), "UTF-8"));
                        while ((line = reader.readLine()) != null) {
                            sb.append(line + NEWLINE);
                        }
                        reader.close();
                        return sb.toString();
                    } catch (IOException e) {
                        return null;
                    }
                }
            } catch (ClientProtocolException e) {
                if (e.getCause() != null) {
                    if (e.getCause() instanceof RedirectException) {
                        statCode = 302;
                        continue;
                    }
                }
                e.printStackTrace();
                throw new SADIServiceException(e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                throw new SADIServiceException(e.getMessage());
            }

        } while (statCode == 302 || statCode == 307);
        // if 302, loop
        // if 200 | 202 return
        // otherwise return null
        return null;
    }

    private static HttpClient createHttpClient() {
        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter("http.protocol.allow-circular-redirects", true);
        client.getParams().setParameter("http.protocol.max-redirects", 5);
        client.getParams().setParameter("http.useragent", USER_AGENT);
        return client;
    }
}
