package org.sadiframework.utils.blast;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sadiframework.utils.blast.NCBIBLASTClient;

public class NCBIBLASTClientTest
{
	private static final Logger log = Logger.getLogger(NCBIBLASTClientTest.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}

	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testDoBLAST() throws Exception
	{
		String program = "blastn";
		String database = "GPIPE/9606/current/ref_contig";
		String escapedURI = URLEncoder.encode("http://lsrn.org/KEGG:hsa:9#seq", "UTF-8");
		String query =
			">" + escapedURI + "\n" + 
			"atggacattgaagcatatcttgaaagaattggctataagaagtctaggaacaaattggac\n" +
			"ttggaaacattaactgacattcttcaacaccagatccgagctgttccctttgagaacctt\n" +
			"aacatccattgtggggatgccatggacttaggcttagaggccatttttgatcaagttgtg\n" +
			"agaagaaatcggggtggatggtgtctccaggtcaatcatcttctgtactgggctctgacc\n" +
			"actattggttttgagaccacgatgttgggagggtatgtttacagcactccagccaaaaaa\n" +
			"tacagcactggcatgattcaccttctcctgcaggtgaccattgatggcaggaactacatt\n" +
			"gtcgatgctgggtttggacgctcataccagatgtggcagcctctggagttaatttctggg\n" +
			"aaggatcagcctcaggtgccttgtgtcttccgtttgacggaagagaatggattctggtat\n" +
			"ctagaccaaatcagaagggaacagtacattccaaatgaagaatttcttcattctgatctc\n" +
			"ctagaagacagcaaataccgaaaaatctactcctttactcttaagcctcgaacaattgaa\n" +
			"gattttgagtctatgaatacatacctgcagacatctccatcatctgtgtttactagtaaa\n" +
			"tcattttgttccttgcagaccccagatggggttcactgtttggtgggcttcaccctcacc\n" +
			"cataggagattcaattataaggacaatacagatctaatagagttcaagactctgagtgag\n" +
			"gaagaaatagaaaaagtgctgaaaaatatatttaatatttccttgcagagaaagcttgtg\n" +
			"cccaaacatggtgatagattttttactatttag";
		String result = IOUtils.toString(
				new NCBIBLASTClient().doBLAST(program, database, query));
		log.debug(String.format("BLAST result:\n%s", result));
		assertFalse("no results", result.isEmpty());
		assertTrue("result lost sequence ID", result.contains(escapedURI));
		assertFalse(result.matches("No definition line"));
	}
}
