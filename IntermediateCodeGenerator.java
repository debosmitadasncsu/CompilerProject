import java.util.Vector;
import java.io.IOException;

public class IntermediateCodeGenerator {

	/**
	 * starting point for the program
	 * @param args The file name to read in and parse
	 */
	public static void main(String[] args) throws IOException {
		// checks to see if we are given any arguments
		if(args.length < 1) {
			System.err.println("Please provide an input file to process");
			System.exit(1);
		}
		Vector<TokenNames> scannedTokens = new Vector<TokenNames>();
		// run initialize and run the scanner
		
		Scanner scanner = new Scanner(args[0]);
		scannedTokens = scanner.runScanner();
		// initialize and run the parser

		RecursiveParsing RP = new RecursiveParsing(scanner.getOutputElements());
		boolean success = RP.parse();
		if( !success ) {
			System.err.println("Parsing failed");
			System.exit(1);
		}
		else {
			/*
			 * Reference: http://stackoverflow.com/questions/21417606/concatenating-to-a-file-name-before-the-filename-extension-in-java
			 */
			int lastDotIndex=args[0].lastIndexOf(".");
			String outputFileName=args[0].substring(0,lastDotIndex) + "_gen" + args[0].substring(lastDotIndex);
			
			CodeGenerator codegen = new CodeGenerator(scanner.getTokenPairs(), RP.getNumberVars(), RP.getSymbolTable(),outputFileName);
			codegen.generateCode();
		}

	}
	
	
	
	

}
