import java.util.HashMap;
import java.util.Vector;
import java.io.*;

public class CodeGenerator {
	
	private static Vector<Pair<TokenNames,String>> inputTokens; // Stores the set of input tokens 
	private static TokenNames currentToken;  // shows what the current token removed from the stack was for debug purposes 
	FileWriter fileWriter;
	PrintWriter printer;
	Vector<Pair<TokenNames,String>> toPrint;
	int currentLabel;
	int currentCount;
	String currentFunction;
	private static HashMap<String, Integer> numberOfLocalVars;
	private static HashMap<String, HashMap<String, Integer>> symbolTable;
	private int while_start_label;
	private int while_end_label;
	
	/**
	 * Constructor initializes the fields and get the list of input tokens
	 * @param inputTokens1
	 */
	
	public CodeGenerator(Vector<Pair<TokenNames, String>> inputTokens1, HashMap<String, Integer> numberOfLocalVars1, HashMap<String, HashMap<String, Integer>> symbolTable1,String outputFileName) throws IOException{
		inputTokens = inputTokens1;
		toPrint = new Vector<Pair<TokenNames,String>>();
		currentToken = TokenNames.None;
		numberOfLocalVars = numberOfLocalVars1;
		symbolTable = symbolTable1;
		currentLabel = 0;
		currentCount = 0;
		try {
			fileWriter = new FileWriter(outputFileName);
			printer = new PrintWriter(fileWriter);
		}catch(IOException e) {
			System.err.println("Invalid output file");
			throw e;
		}
	}
	
	// Helper function to pass through space and meta statement tokens
	public Pair<TokenNames,String> firstElement(Vector<Pair<TokenNames, String>> inputTokens1) {
		for(Pair<TokenNames,String> tokenPair : inputTokens1) {
			if(tokenPair.getKey() != TokenNames.Space && tokenPair.getKey() != TokenNames.MetaStatements) {
				return tokenPair;
			}
		}
		return null;
	}
	
	// Removes tokens from the stream and adds to a temporary vector
	public Pair<TokenNames,String> removeToken(Vector<Pair<TokenNames, String>> inputTokens1) {
		Pair<TokenNames,String> tokenPair = null;
		while(true) {
			tokenPair = inputTokens1.remove(0);
			if(tokenPair.getKey() == TokenNames.Space || tokenPair.getKey() == TokenNames.MetaStatements) {
				toPrint.addElement(tokenPair);
			}
			else
				break;
		}
		toPrint.addElement(tokenPair);
		return tokenPair;
	}
	
	// Removes tokens but does not add to temporary vector
	public Pair<TokenNames,String> removeTokenComplete(Vector<Pair<TokenNames, String>> inputTokens1) {
		Pair<TokenNames,String> tokenPair = null;
		while(true) {
			tokenPair = inputTokens1.remove(0);
			if(tokenPair.getKey() != TokenNames.Space && tokenPair.getKey() != TokenNames.MetaStatements) {
				return tokenPair;
			}
		}
	}
	
	/**
	 * initialized the parsing and prints out the results when finished
	 */
	public void generateCode() {
		program();
		printer.flush();
		printer.close();
		try {
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// To print out the results of the temporary vector
	void printArray() {
		Pair<TokenNames, String> tokenpair = null;
		while(toPrint.size()>0) {
			tokenpair = toPrint.remove(0);
			printer.print(tokenpair.getValue());
		}
	}
	
	void printArraySpaces() {
		Pair<TokenNames, String> tokenpair = null;
		while(toPrint.size()>0 && toPrint.firstElement().getKey()==TokenNames.Space) {
			tokenpair = toPrint.remove(0);
			printer.print(tokenpair.getValue());
		}
	}
	
	/**
	 * <program> --> <type name> ID <data decls> <func list> | empty
	 * @return A boolean indicating pass or error 
	 */
	private boolean program() {
		// check if we are at the eof
		if(firstElement(inputTokens).getKey() == TokenNames.eof) {
			return true;
		}
		
		while(inputTokens.firstElement().getKey() == TokenNames.MetaStatements) {
			printer.println(inputTokens.remove(0).getValue());
		}
		
		currentFunction = "global";
		if(type_name()) {
			if(firstElement(inputTokens).getKey() == TokenNames.ID) {
				Pair<TokenNames,String> lastElement = removeToken(inputTokens);
				currentToken = lastElement.getKey(); // get the ID token
				// Check whether the id belongs to function or data declaration
				if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
					printer.println("int global["+numberOfLocalVars.get(currentFunction)+"];");
					currentFunction = lastElement.getValue();
					currentCount = symbolTable.get(currentFunction).size();
				}
				else {
					// print only when data_decl returns empty. That denotes the start of function declarations.
					data_decls();						
				}
				
				if(func_list()) {
					//check to see if the remaining token is eof is so this is a legal syntax
						if(firstElement(inputTokens).getKey() == TokenNames.eof) {
							return true;
						}
					}
				}
			}
		return false;
	}
	
	/**
	 * <func list> --> empty | left_parenthesis <parameter list> right_parenthesis <func Z> <func list Z> 
	 * @return A boolean indicating if the rule passed or failed 
	 */
	private boolean func_list(){
		if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
			currentToken = removeToken(inputTokens).getKey();
			if(parameter_list()) {
				if(firstElement(inputTokens).getKey() == TokenNames.right_parenthesis) {
					currentToken = removeToken(inputTokens).getKey();
					printArray();
					if(func_Z()) {
						return func_list_Z();
					}
					return false;
				}
				return false;
			}
			return false;
		}
		return true;
	}
	
	/**
	 * <func Z> --> semicolon | left_brace <data decls Z> <statements> right_brace 
	 * @return A boolean indicating if the rule passed or failed 
	 */
	private boolean func_Z() {
		// checks if the next token is a semicolon
		if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
			currentToken = removeToken(inputTokens).getKey(); // remove the token from the stack
			printArray();
			return true;
		}
		
		if(firstElement(inputTokens).getKey() == TokenNames.left_brace) {
			currentToken = removeToken(inputTokens).getKey();
			printArray();
			if(data_decls_Z()) {
				printArraySpaces();
				toPrint.removeAllElements();
				printer.print("int local["+numberOfLocalVars.get(currentFunction)+"];");
				if(statements()) {
					if(firstElement(inputTokens).getKey() == TokenNames.right_brace) {
						currentToken = removeToken(inputTokens).getKey();
						printArray();
						// Count the number of function definitions
						return true;
					}
					return false;
				}
				return false;
			}
			return false;
		}
		return false;
	}
	
	/**
	 * <func list Z> --> empty | <type name> ID left_parenthesis <parameter list> right_parenthesis <func Z> <func list Z>
	 * @return a boolean 
	 */
	private boolean func_list_Z() {
		if(type_name()) {
			if(firstElement(inputTokens).getKey() == TokenNames.ID) {
				Pair<TokenNames,String> lastElement = removeToken(inputTokens);
				currentToken = lastElement.getKey();
				currentFunction = lastElement.getValue();
				currentCount = symbolTable.get(currentFunction).size();
				if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
					currentToken = removeToken(inputTokens).getKey();
					if(parameter_list()) {
						if(firstElement(inputTokens).getKey() == TokenNames.right_parenthesis) {
							currentToken = removeToken(inputTokens).getKey();
							printArray();
							if(func_Z()) {
								return func_list_Z();
							}
						}						
					}					
				}				
			}
			return false;
		}
		// return true for the empty rule
		return true;		
	}
	
	/**
	 * <type name> --> int | void | binary | decimal 
	 * @return A boolean indicating if the rule passed or failed
	 */
	private boolean type_name() {
		if(firstElement(inputTokens).getKey() == TokenNames.Int || firstElement(inputTokens).getKey() == TokenNames.Void 
				|| firstElement(inputTokens).getKey() == TokenNames.binary || firstElement(inputTokens).getKey() == TokenNames.decimal) {
			currentToken = removeToken(inputTokens).getKey();
			return true;
		}
		return false;
	}
	
	/**
	 * <parameter list> --> empty | void <parameter list Z> | <non-empty list> 
	 * @return a boolean
	 */
	private boolean parameter_list() {
		// void <parameter list Z>
		if(firstElement(inputTokens).getKey() == TokenNames.Void) {
			currentToken = removeToken(inputTokens).getKey();
			return parameter_list_Z();
		}
		// <non-empty list>
		else if(non_empty_list()) {
			return true;
		}
		// empty
		return true;
	}
	
	/**
	 * <parameter list Z> --> empty | ID <non-empty list prime>
	 * @return a boolean
	 */
	private boolean parameter_list_Z() {
		if(firstElement(inputTokens).getKey() == TokenNames.ID) {
			currentToken = removeToken(inputTokens).getKey();
			return non_empty_list_prime();
		}
		return true;
	}
	
	/**
	 * <non-empty list> --> int ID <non-empty list prime> | binary ID <non-empty list prime> | 
	 * decimal ID <non-empty list prime>
	 * @return a boolean
	 */
	private boolean non_empty_list() {
		// check for int, binary, decimal
		if(firstElement(inputTokens).getKey() == TokenNames.Int || firstElement(inputTokens).getKey() == TokenNames.binary || 
				firstElement(inputTokens).getKey() == TokenNames.decimal) {
			currentToken = removeToken(inputTokens).getKey();
			if(firstElement(inputTokens).getKey() == TokenNames.ID) {
				currentToken = removeToken(inputTokens).getKey();
				return non_empty_list_prime();
			}
		}
		return false;
	}
	
	/**
	 * <non-empty list prime> --> comma <type name> ID <non-empty list prime> | empty
	 * @return a boolean
	 */
	private boolean non_empty_list_prime() {
		if(firstElement(inputTokens).getKey() == TokenNames.comma) {
			currentToken = removeToken(inputTokens).getKey();
			if(type_name()) {
				if(firstElement(inputTokens).getKey() == TokenNames.ID) {
					currentToken = removeToken(inputTokens).getKey();
					return non_empty_list_prime();
				}
				return false;
			}
			return false;
		}
		return true;
	}
	
	/**
	 * <data decls> --> empty | <id list Z> semicolon <program> | <id list prime> semicolon <program>
	 * @return a boolean
	 */
	private Node data_decls() {
		Node result = new Node();
		result.retValue=1;
		if(id_list_Z()) {
			if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
				currentToken = removeToken(inputTokens).getKey();
				if(currentFunction.equals("global")) {
					toPrint.removeAllElements();
				}
				// count variable
				program(); //data_decls_Z();
				return result;
			}
		}
		if(id_list_prime()) {
			if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
				currentToken = removeToken(inputTokens).getKey();
				if(currentFunction.equals("global")) {
					toPrint.removeAllElements();
				}
				// since we consume the first id before we get here count this as a variable
				program(); //data_decls_Z();
				return result;
			}
			//return false;
		}
		
		result.retValue=2;
		return result;
	}
	
	/**
	 * <data decls Z> --> empty | int <id list> semicolon <data decls Z> | 
	 * 				     void <id list> semicolon <data decls Z> | 
	 * 			         binary <id list> semicolon <data decls Z> | decimal <id list> semicolon <data decls Z> 
	 * @return A boolean indicating if the rule passed or failed
	 */
	private boolean data_decls_Z() {
		if(type_name()) {
			if(id_list()) {
				if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
					currentToken = removeToken(inputTokens).getKey();
					return data_decls_Z();
				}
				return false;
			}
			return false;
		}
		return true;
	}
	
	/**
	 * <id list> --> <id> <id list prime>
	 * @return a boolean
	 */
	private boolean id_list() {
		if(id()) {
			return id_list_prime();
		}
		return false;
	}
	
	/**
	 * <id list Z> --> left_bracket <expression> right_bracket <id list prime>
	 * @return a boolean indicating if the rule passed or failed
	 */
	private boolean id_list_Z() {
		if(firstElement(inputTokens).getKey() == TokenNames.left_bracket) {
			currentToken = removeToken(inputTokens).getKey();
			Node expr = expression();
			if(expr.retValue!=0) {
				toPrint.addElement(new Pair<TokenNames,String>(TokenNames.ID,expr.getValue()));
				if(firstElement(inputTokens).getKey() == TokenNames.right_bracket) {
					currentToken = removeToken(inputTokens).getKey();
					return id_list_prime();
				}
			}
		}
		return false;
	}
	
	/**
	 * <id list prime> --> comma <id> <id list prime> | empty
	 * @return a boolean indicating if the rule passed or failed
	 */
	private boolean id_list_prime() {
		if(firstElement(inputTokens).getKey() == TokenNames.comma) {
			currentToken = removeToken(inputTokens).getKey();
			if(id()) {
				return id_list_prime();
			}
			return false;
		}
		return true;
	}
	
	/**
	 * <id> --> ID <id Z>
	 * @return a boolean
	 */
	private boolean id() {
		if(firstElement(inputTokens).getKey() == TokenNames.ID) {
			currentToken = removeToken(inputTokens).getKey();
			return id_Z();
		}
		return false;
	}
	
	/**
	 * <id Z> --> left_bracket <expression> right_bracket | empty
	 * @return a boolean
	 */
	private boolean id_Z() {
		if(firstElement(inputTokens).getKey() == TokenNames.left_bracket) {
			currentToken = removeToken(inputTokens).getKey();
			if(expression().retValue!=0) {
				if(firstElement(inputTokens).getKey() == TokenNames.right_bracket) {
					currentToken = removeToken(inputTokens).getKey();
					//printArray(); 
					return true;
				}
				return false;
			}
			return false;
		}
		// count the number of variables 
		return true;
	}
	
	/**
	 * <block statements> --> left_brace <statements> right_brace 
	 * @return a boolean
	 */
	private boolean block_statements(boolean dontPrintBraces) {
		if(firstElement(inputTokens).getKey() == TokenNames.left_brace) {
			if(dontPrintBraces==false)
				currentToken = removeToken(inputTokens).getKey();
			else
				currentToken= removeTokenComplete(inputTokens).getKey();
			printArray();
			if(statements()) {
				if(firstElement(inputTokens).getKey() == TokenNames.right_brace) {
					if(dontPrintBraces==false)
						currentToken = removeToken(inputTokens).getKey();
					else
						currentToken= removeTokenComplete(inputTokens).getKey();
					printArray();
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * <statements> --> empty | <statement> <statements> 
	 * @return a boolean
	 */
	private boolean statements() {
		if(statement()) {
			return statements();
		}
		return true;
	}
	
	/**
	 * <statement> --> ID <statement Z> | <if statement> | <while statement> | 
	 *	<return statement> | <break statement> | <continue statement> | 
	 *	read left_parenthesis  ID right_parenthesis semicolon | 
	 *  write left_parenthesis <expression> right_parenthesis semicolon | 
	 *  print left_parenthesis  STRING right_parenthesis semicolon 
	 * @return a boolean indicating if the rule passed or failed 
	 */
	private boolean statement() {
		if(firstElement(inputTokens).getKey() == TokenNames.ID) {
			String id = removeTokenComplete(inputTokens).getValue();
			Node st = statement_Z();
			// The ID can be part of assignment or function call
			//function call
			if(st.retValue==1) {
				printer.println(id+st.getValue()+";");
				return true;
			}
			else {
				// assignment
				// id can be an array
				if(st.retValue==2) {
					// array can be global
					if(symbolTable.get("global").get(id)!=null) {
						printer.println("local["+currentCount+"]="+symbolTable.get("global").get(id)+"+"+st.special+";");
						printer.println("global[local["+currentCount+"]]"+st.getValue()+";");
						currentCount++;
					}
					// array can be local
					else {
						printer.println("local["+currentCount+"]="+symbolTable.get(currentFunction).get(id)+"+"+st.special+";");
						printer.println("local[local["+currentCount+"]]"+st.getValue()+";");
						currentCount++;
					}
				}
				// id can be variable
				else {
					// id can be global
					if(symbolTable.get("global").get(id)!=null) {
						printer.println("global["+symbolTable.get("global").get(id)+"]"+st.getValue()+";");
					}
					// array can be local
					else {
						printer.println("local["+symbolTable.get(currentFunction).get(id)+"]"+st.getValue()+";");
					}
				}
				return true;
			}
		}
		if(if_statement()) {
			return true;
		}
		if(while_statement()) {
			return true;
		}
		if(return_statement()) {
			return true;
		}
		if(break_statement()) {
			return true;
		}
		if(continue_statement()) {
			return true;
		}
		if(firstElement(inputTokens).getKey() == TokenNames.read) {
			currentToken = removeToken(inputTokens).getKey();
			if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
				currentToken = removeToken(inputTokens).getKey();
				if(firstElement(inputTokens).getKey() == TokenNames.ID) {
					Pair<TokenNames, String> l = removeToken(inputTokens);
					Pair<TokenNames, String> l1;
					if(symbolTable.get(currentFunction).get(l.getValue())!=null) {         
							l1 = new Pair<TokenNames,String>(TokenNames.ID,"local["+symbolTable.get(currentFunction).get(l.getValue())+"]" );
					}
					else{
						l1 = new Pair<TokenNames,String>(TokenNames.ID,"global["+symbolTable.get("global").get(l.getValue())+"]" );
					}
						
					toPrint.set(toPrint.size()-1, l1 );
					if(firstElement(inputTokens).getKey() == TokenNames.right_parenthesis) {
						currentToken = removeToken(inputTokens).getKey();
						if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
							currentToken = removeToken(inputTokens).getKey();
							printArray();
							return true;
						}
					}
				}
			}
			return false;
		}
		
		// write left_parenthesis <expression> right_parenthesis semicolon
		if(firstElement(inputTokens).getKey() == TokenNames.write) {
			currentToken = removeToken(inputTokens).getKey();
			if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
				currentToken = removeToken(inputTokens).getKey();
				Node ret = expression();
				if(ret.retValue!=0) {
					toPrint.addElement(new Pair<TokenNames, String>(TokenNames.ID,ret.getValue()));
					if(firstElement(inputTokens).getKey() == TokenNames.right_parenthesis) {
						currentToken = removeToken(inputTokens).getKey();
						if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
							currentToken = removeToken(inputTokens).getKey();
							printArray();
							return true;
						}
					}
				}
			}
			return false;
		}
		
		// print left_parenthesis  STRING right_parenthesis semicolon
		if(firstElement(inputTokens).getKey() == TokenNames.print) {
			currentToken = removeToken(inputTokens).getKey();
			if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
				currentToken = removeToken(inputTokens).getKey();
				if(firstElement(inputTokens).getKey() == TokenNames.STRING) {
					currentToken = removeToken(inputTokens).getKey();
					if(firstElement(inputTokens).getKey() == TokenNames.right_parenthesis) {
						currentToken = removeToken(inputTokens).getKey();
						if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
							currentToken = removeToken(inputTokens).getKey();
							printArray();
							return true;
						}
					}
				}
			}
			return false;
		}
		return false;
	}
	
	/**
	 * <statement Z> --> <assignment Z> | <func call>
	 * @return a boolean indicating if the rule passed or failed
	 */
	private Node statement_Z() {
		Node assign = assignment_Z();
		Node func;
		
		if(assign.retValue!=0) {
			return assign;
		}
		else if((func=func_call()).retValue!=0) {
			func.retValue=1;
			return func;
		}
		return new Node();
	}
	
	/**
	 * <assignment Z> --> equal_sign <expression> semicolon | 
	 * left_bracket <expression> right_bracket equal_sign <expression> semicolon
	 * @return a Node
	 */
	private Node assignment_Z() {
		Node result = new Node();
		if(firstElement(inputTokens).getKey() == TokenNames.equal_sign) {
			currentToken = removeTokenComplete(inputTokens).getKey();
			Node expr = expression();
			if(expr.retValue!=0) {
				if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
					currentToken = removeTokenComplete(inputTokens).getKey();
					result.setValue(" = "+expr.getValue());
					result.retValue = 3;
					return result;
				}
			}
			result.retValue=0;
			return result;
		}
		if(firstElement(inputTokens).getKey() == TokenNames.left_bracket) {
			currentToken = removeTokenComplete(inputTokens).getKey();
			Node expr2 = expression();
			if(expr2.retValue!=0) {
				result.retValue = 2;
				if(firstElement(inputTokens).getKey() == TokenNames.right_bracket) {
					currentToken = removeTokenComplete(inputTokens).getKey();
					if(firstElement(inputTokens).getKey() == TokenNames.equal_sign) {
						currentToken = removeTokenComplete(inputTokens).getKey();
						Node expr3 = expression();
						if(expr3.retValue!=0) {
							if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
								currentToken = removeTokenComplete(inputTokens).getKey();
								result.setValue(" = "+expr3.getValue());
								result.special = expr2.getValue();
								return result;
							}
						}
					}
				}
			}
			result.retValue=0;
			return result;
		}
		result.retValue=0;
		return result;
	}
	
	/**
	 * <func call> --> left_parenthesis <expr list> right_parenthesis semicolon 
	 * @return a Node
	 */
	private Node func_call() {
		Node result = new Node();
		if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
			currentToken = removeTokenComplete(inputTokens).getKey();
			Node expr = expr_list();
			if(expr.retValue!=0) {
				if(firstElement(inputTokens).getKey() == TokenNames.right_parenthesis) {
					currentToken = removeTokenComplete(inputTokens).getKey();
					if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
						currentToken = removeTokenComplete(inputTokens).getKey();
						result.setValue("( "+expr.getValue()+" )");
						result.retValue=1;
						return result;
					}
				}
			}
		}		
		result.retValue=0;
		return result;
	}
	
	/**
	 * <expr list> --> empty | <non-empty expr list> 
	 * @return a Node
	 */
	private Node expr_list() {
		Node result = new Node();
		Node list = non_empty_expr_list(); 
		if(list.retValue!=0) {
			return list;
		}
		result.retValue=2;
		return result;
	}
	
	/**
	 * <non-empty expr list> --> <expression> <non-empty expr list prime>
	 * @return a Node
	 */
	private Node non_empty_expr_list() {
		Node result = new Node();
		Node expr = expression();
		if(expr.retValue!=0) {
			Node list = non_empty_expr_list_prime();
			if(list.retValue!=0) {
				result.setValue(expr.getValue()+list.getValue());
				result.retValue=1;
				return result;
			}
		}
		result.retValue=0;
		return result;
	}
	
	/**
	 * <non-empty expr list prime> --> comma <expression> <non-empty expr list prime> | empty
	 * @return a Node
	 */
	private Node non_empty_expr_list_prime() {
		Node result = new Node();
		if(firstElement(inputTokens).getKey() == TokenNames.comma) {
			Pair<TokenNames, String> last = removeToken(inputTokens);
			toPrint.remove(toPrint.size()-1);
			Node exp = expression();
			if(exp.retValue!=0) {
				Node list = non_empty_expr_list_prime();
				if(list.retValue!=2) {
					result.setValue(","+exp.getValue()+list.getValue());
					result.retValue=1;
					return result;
				}
				else {
					result.setValue(","+exp.getValue());
					result.retValue=1;
					return result;
				}
				
			}
			result.retValue = 2;
			return result;
		}
		result.retValue=2;
		return result;
	}
	
	/**
	 * <if statement> --> if left_parenthesis <condition expression> right_parenthesis <block statements> 
	 * @return a boolean
	 */
	private boolean if_statement() {
		if(firstElement(inputTokens).getKey() == TokenNames.If) {
			currentToken = removeTokenComplete(inputTokens).getKey();
			if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
				currentToken = removeTokenComplete(inputTokens).getKey();
				Node cond = condition_expression();
				if(cond.retValue!=0) {
					if(firstElement(inputTokens).getKey() == TokenNames.right_parenthesis) {
						currentToken = removeTokenComplete(inputTokens).getKey();
						printArray();
						int label = currentLabel;
						printer.println("if ("+cond.getValue()+" ) goto c"+currentLabel+";");
						currentLabel++;
						printer.println("goto c"+currentLabel+";");
						currentLabel++;
						printer.println("c"+(currentLabel-2)+":;");
						block_statements(true);
						printer.println("c"+(label+1)+":;");
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * <condition expression> -->  <condition> <condition expression Z>
	 * @return a boolean
	 */
	private Node condition_expression() {
		Node result = new Node();
		Node cond = condition();
		if(cond.retValue!=0) {
			Node ce = condition_expression_Z();
			printer.println("local["+currentCount+"]="+cond.getValue()+ce.getValue()+";");
			result.setValue("local["+currentCount+"]");
			currentCount++;
			result.retValue=1;
			return result;
		}
		result.retValue=0;
		return result;
	}
	
	/**
	 * <condition expression Z> --> <condition op> <condition> | empty
	 * @return a boolean
	 */
	private Node condition_expression_Z() {
		String comp = condition_op();
		Node result = new Node();
		if(comp!=null) {
			Node cond = condition();
			result.setValue(comp+cond.getValue());
			result.retValue=1;
			return result;
		}
		result.retValue=2;
		return result;
	}
	
	/**
	 * <condition op> --> double_end_sign | double_or_sign 
	 * @return a String
	 */
	private String condition_op() {
		if(firstElement(inputTokens).getKey() == TokenNames.double_and_sign || firstElement(inputTokens).getKey() == TokenNames.double_or_sign) {
			String x = removeToken(inputTokens).getValue();
			toPrint.remove(toPrint.size()-1);
			return x;
		}
		return null;
	}
	
	/**
	 * <condition> --> <expression> <comparison op> <expression> 
	 * @return a Node
	 */
	private Node condition() {
		Node result = new Node();
		Node expr = expression();
		if(expr.retValue==1) {
			String op = comparison_op();
			if(op!=null) {
				Node expr1 = expression();
				printer.println("local["+currentCount+"]="+expr.getValue()+" "+op+" "+expr1.getValue()+";");
				result.setValue("local["+currentCount+"]");
				currentCount++;
				result.retValue=1;
				return result;
			}
		}
		result.retValue=0;
		return result;
	}
	
	/**
	 * <comparison op> --> == | != | > | >= | < | <=
	 * @return a String
	 */
	private String comparison_op() {
		if(firstElement(inputTokens).getKey() == TokenNames.doubleEqualSign || firstElement(inputTokens).getKey() == TokenNames.notEqualSign ||
				firstElement(inputTokens).getKey() == TokenNames.greaterThenSign || firstElement(inputTokens).getKey() == TokenNames.greaterThenOrEqualSign ||
				firstElement(inputTokens).getKey() == TokenNames.lessThenSign || firstElement(inputTokens).getKey() == TokenNames.lessThenOrEqualSign) {
			String result = removeToken(inputTokens).getValue();
			toPrint.remove(toPrint.size()-1);
			return result;
		}
		return null;
	}
	
	/**
	 * <while statement> --> while left_parenthesis <condition expression> right_parenthesis <block statements> 
	 * @return
	 */
	private boolean while_statement() {
		if(firstElement(inputTokens).getKey() == TokenNames.While) {
			currentToken = removeTokenComplete(inputTokens).getKey();
			if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
				currentToken = removeTokenComplete(inputTokens).getKey();
				printer.println("c"+currentLabel+":;");
				while_start_label = currentLabel; // will be used by break and continue productions.
				while_end_label = currentLabel+2;
				currentLabel++;
				Node cond = condition_expression();
				if(cond.retValue!=0){
					if(firstElement(inputTokens).getKey() == TokenNames.right_parenthesis) {
						currentToken = removeTokenComplete(inputTokens).getKey();
						int label = currentLabel-1;
						printer.println("if("+cond.getValue()+") "+ "goto c"+currentLabel+";");
						currentLabel++;
						printer.println("goto c"+currentLabel+";");
						currentLabel++;
						printer.println("c"+(currentLabel-2)+":;");
						block_statements(true);
						printer.println("goto c"+(label)+";");
						printer.println("c"+(label+2)+":;");
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * <return statement> --> return <return statement Z>
	 * @return a boolean
	 */
	private boolean return_statement() {
		if(firstElement(inputTokens).getKey() == TokenNames.Return) {
			currentToken = removeToken(inputTokens).getKey();
			return return_statement_Z();
		}
		return false;
	}
	
	/**
	 * <return statement Z> --> <expression> semicolon | semicolon 
	 * @return a boolean
	 */
	private boolean return_statement_Z() {
		Node expr = expression();
		if(expr.retValue==1) {
			if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
				toPrint.add(new Pair<TokenNames, String>(TokenNames.ID, " "+expr.getValue()));
				currentToken = removeToken(inputTokens).getKey();
				printArray();
				return true;
			}
			return false;
		}
		if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
			currentToken = removeToken(inputTokens).getKey();
			printArray();
			return true;
		}
		return false;
	}
	
	/**
	 * <break statement> ---> break semicolon
	 * @return a boolean
	 */
	private boolean break_statement() {
		if(firstElement(inputTokens).getKey() == TokenNames.Break) {
			currentToken = removeTokenComplete(inputTokens).getKey();
			if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
				currentToken = removeTokenComplete(inputTokens).getKey();
				// set by the last executed while statement productions.
				printer.println("goto c"+while_end_label +";");
				return true;
			}
		}
		return false;
	}
	
	/**
	 * <continue statement> ---> continue semicolon
	 * @return a boolean
	 */
	private boolean continue_statement() {
		if(firstElement(inputTokens).getKey() == TokenNames.Continue) {
			currentToken = removeTokenComplete(inputTokens).getKey();
			if(firstElement(inputTokens).getKey() == TokenNames.semicolon) {
				currentToken = removeTokenComplete(inputTokens).getKey();
				// set by the latest executed while statements productions.
				printer.println("goto c"+while_start_label+";");
				return true;
			}
		}
		return false;
	}
	
	/**
	 * <expression> --> <term> <expression prime>
	 * @return a Node
	 */
	private Node expression() {
		Node result = new Node();
		Node t = term();
		if(t.retValue==1) {
			result.retValue=1;
			Node epr =  expression_prime(t.getValue());
			if(epr.retValue==1) {
				return epr;
			}
			else {
				return t;
			}
		}
		result.retValue=0;
		return result;
	}
	
	/**
	 * <expression prime> --> <addop> <term> <expression prime> | empty
	 * @return
	 */
	
	private Node expression_prime(String tvalue) {
		Node result = new Node();
		String addop = addop();
		if(addop!=null) {
			Node t = term();
			if(t.retValue!=0) {
				printer.println("local["+currentCount+"]="+tvalue+addop+t.getValue()+";");
				currentCount++;
				Node tp = expression_prime("local["+(currentCount-1)+"]");
				if(tp.retValue==2) {
					result.setValue("local["+(currentCount-1)+"]");
					result.retValue=1;
					return result;
				}
				else if(tp.retValue==1) {
					return tp;
				}
			}
		}
		result.retValue=2;
		return result;
	}
	
	/**
	 * <addop> --> plus_sign | minus_sign 
	 * @return a String
	 */
	private String addop() {
		if(firstElement(inputTokens).getKey() == TokenNames.plus_sign || firstElement(inputTokens).getKey() == TokenNames.minus_sign) {
			Pair<TokenNames,String> tokenpair = removeToken(inputTokens); 
			currentToken = tokenpair.getKey();
			toPrint.remove(toPrint.size()-1);
			return tokenpair.getValue();
		}
		return null;
	}
	
	/**
	 * <term> --> <factor> <term prime>
	 * @returns a Node
	 */
	private Node term() {
		Node result = new Node();
		Node fac = factor();
		if(fac.retValue!=0) {
			Node x = term_prime(fac.getValue());
			result.retValue=1;
			if(x.retValue==2) {
				result.setValue(fac.getValue());
				result.retValue=1;
				return result;
			}
			else {
				return x;
			}
		}
		result.retValue=0;
		return result;
	}
	
	/**
	 * <term prime> --> <mulop> <factor> <term prime> | empty
	 * @return
	 */
		
	private Node term_prime(String fvalue) {
		Node result = new Node();
		String mulop = mulop();
		if(mulop!=null) {
			Node t = factor();
			if(t.retValue!=0) {
				printer.println("local["+currentCount+"]="+fvalue+mulop+t.getValue()+";");
				currentCount++;
				Node tp = term_prime("local["+(currentCount-1)+"]");
				if(tp.retValue==2) {
					result.setValue("local["+(currentCount-1)+"]");
					result.retValue=1;
					return result;
				}
				else if(tp.retValue==1) {
					return tp;
				}
			}
		}
		result.retValue=2;
		return result;
	}
	
	/**
	 * <mulop> --> star_sign | forward_slash 
	 * @return a string
	 */
	private String mulop() {
		if(firstElement(inputTokens).getKey() == TokenNames.star_sign || firstElement(inputTokens).getKey() == TokenNames.forward_slash) {
			Pair<TokenNames, String> lastElement = removeToken(inputTokens);
			toPrint.remove(toPrint.size()-1);
			currentToken = lastElement.getKey();
			return lastElement.getValue();
		}
		return null;
	}
	
	/**
	 * <factor> --> ID <factor Z> | NUMBER | minus_sign NUMBER | left_parenthesis <expression>right_parenthesis 
	 * @return
	 */
	private Node factor() {
		Node result = new Node();
		if(firstElement(inputTokens).getKey() == TokenNames.ID) {
			Pair<TokenNames, String> lastElement = removeTokenComplete(inputTokens);
			Node x = factor_Z();
			if(x.retValue==2) {
				if((symbolTable.get(currentFunction).get(lastElement.getValue())==null) && symbolTable.get("global").get(lastElement.getValue())==null) {
					printer.println("local["+currentCount+"]="+lastElement.getValue()+";");
					symbolTable.get(currentFunction).put(lastElement.getValue(),currentCount);
					currentCount++;
				}
				if(symbolTable.get("global").get(lastElement.getValue())!=null) { 
					result.setValue("global["+symbolTable.get("global").get(lastElement.getValue())+"]");
					
				}
				else
					result.setValue("local["+symbolTable.get(currentFunction).get(lastElement.getValue())+"]");
				result.retValue=1;
				return result;
			}
			// If factorZ returned an array
			else if(x.retValue==3){
				if(symbolTable.get(currentFunction).get(lastElement.getValue())!=null) {
					printer.println("local["+currentCount+"]="+symbolTable.get(currentFunction).get(lastElement.getValue())+"+"+ x.getValue()+";");
					result.setValue("local[local["+currentCount+"]]");
				}
				else {
					printer.println("local["+currentCount+"]="+symbolTable.get("global").get(lastElement.getValue())+"+"+ x.getValue()+";");
					result.setValue("global[local["+currentCount+"]]");
				}
				currentCount++;
				result.retValue=1;
				return result;
			}
			else {
				printer.println("local["+currentCount+"]="+lastElement.getValue()+x.getValue()+";");
				result.setValue("local["+currentCount+"]");
				currentCount++;
				result.retValue = 1;
				return result;
			}
		}
		// NUMBER
		if(firstElement(inputTokens).getKey() == TokenNames.NUMBER) {
			String num = removeTokenComplete(inputTokens).getValue();
			result.setValue(num);
			result.retValue=1;
			return result;
		}
		
		// minus_sign NUMBER
		if(firstElement(inputTokens).getKey() == TokenNames.minus_sign) {
			currentToken = removeTokenComplete(inputTokens).getKey();
			if(firstElement(inputTokens).getKey() == TokenNames.NUMBER) {
				String num = removeTokenComplete(inputTokens).getValue();
				result.setValue("-"+num);
				result.retValue=1;
				return result;
			}
			return result;
		}
		
		// left_parenthesis <expression>right_parenthesis
		if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
			removeTokenComplete(inputTokens).getValue();
			Node ex = expression();
			if(ex.retValue==1) {
				if(firstElement(inputTokens).getKey() == TokenNames.right_parenthesis) {
					currentToken = removeTokenComplete(inputTokens).getKey();
					result.setValue(ex.getValue());
					result.retValue=1;
					return result;
				}
			}
			return result;
		}
		return result;
	}
	
	/**
	 * <factor Z> --> left_bracket <expression> right_bracket | left_parenthesis <expr list> right_parenthesis | empty
	 * @return
	 */
	private Node factor_Z() {
		Node result = new Node();
		// left_bracket <expression> right_bracket
		if(firstElement(inputTokens).getKey() == TokenNames.left_bracket) {
			currentToken = removeTokenComplete(inputTokens).getKey();
			Node expr = expression();
			if(expr.retValue!=0) {
				if(firstElement(inputTokens).getKey() == TokenNames.right_bracket) {
					currentToken = removeTokenComplete(inputTokens).getKey();
					result.setValue(expr.getValue());
					// Denote that return value is an array.
					result.retValue=3;
					return result;
				}
			}
			result.retValue=0;
			return result;
		}
		// left_parenthesis <expr list> right_parenthesis
		if(firstElement(inputTokens).getKey() == TokenNames.left_parenthesis) {
			currentToken = removeToken(inputTokens).getKey();
			toPrint.remove(toPrint.size()-1);
			Node expr = expr_list();
			if(expr.retValue!=0) {
				if(firstElement(inputTokens).getKey() == TokenNames.right_parenthesis) {
					currentToken = removeTokenComplete(inputTokens).getKey();
					result.setValue("("+expr.getValue()+")");
					result.retValue=1;
					return result;
				}
			}
			result.retValue=0;
			return result;
		}
		// empty
		result.retValue=2;
		return result;
	}
	

}
