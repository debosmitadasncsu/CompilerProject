import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import java.io.IOException;

/**
 * This is the main class for the Scanner
 */


public class Scanner {
	
	private String fileName;
	Vector<Pair<TokenNames,String>> tokenPairs;
	Vector<Pair<TokenNames,String>> outputElements;
	
	public Scanner(String fileName) {
		this.fileName = fileName;
		tokenPairs = new Vector<Pair<TokenNames,String>>();
		outputElements = new Vector<Pair<TokenNames,String>>();
	}

	public Vector<Pair<TokenNames,String>> getTokenPairs() {
		return tokenPairs;
	}
	
	public Vector<Pair<TokenNames,String>> getOutputElements() {
		return outputElements;
	}
	
	public Vector<TokenNames> runScanner() throws IOException {
		// checks to see if we are given any arguments
//		if(args.length < 1) {
//			System.out.println("Please provide an input file to process");
//			System.exit(0);
//		}
		
		//String fileName = args[0];
		Scan scan = new Scan(fileName);
		Vector<TokenNames> outputTokens = new Vector<TokenNames>();
		Pair<TokenNames,String> tokenPair;
		
		
		// get the name of the file minus the dot 
//			int pos = fileName.lastIndexOf(".");
//			String newFileName = fileName.substring(0, pos) + "_gen.c";
//			PrintWriter writer = new PrintWriter(newFileName,"UTF-8");
		
		// keep getting the next token until we get a null
		while((tokenPair = scan.getNextToken()) != null) {
			if(tokenPair.getKey() != TokenNames.Space && tokenPair.getKey() != TokenNames.MetaStatements) {
				outputElements.addElement(tokenPair);
			}
			tokenPairs.addElement(tokenPair);
		}
		outputElements.addElement(new Pair<TokenNames,String>(TokenNames.eof,""));
		tokenPairs.addElement(new Pair<TokenNames,String>(TokenNames.eof,""));
		return outputTokens;
	}

}
