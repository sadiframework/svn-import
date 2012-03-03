package ca.wilkinsonlab.sadi.utils.blast;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stringtree.util.StreamUtils;

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
		String query =
			">http://lsrn.org/KEGG:hsa:9#seq\n" + 
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
		String result = StreamUtils.readStream(
				new NCBIBLASTClient().doBLAST(program, database, query));
		assertFalse(result.isEmpty());
		log.debug(String.format("BLAST result:\n%s", result));
	}
}
