import java.util.HashMap;
import java.util.Vector;

public class RecursiveParsing {
	
	private static Vector<Pair<TokenNames,String>> inputTokens; // Stores the set of input tokens 
	private static TokenNames currentToken;  // shows what the current token removed from the stack was for debug purposes 
	private static HashMap<String, Integer> numberOfLocalVars;
	private static HashMap<String, HashMap<String, Integer>> symbolTable;
	String currentFunction;
	int currentCount;
	/**
	 * Constructor initializes the fields and get the list of input tokens
	 * @param inputTokens1
	 */
	public HashMap<String, Integer> getNumberVars() {
		return numberOfLocalVars;
	}
	
	public HashMap<String, HashMap<String, Integer>> getSymbolTable() {
		return symbolTable;
	}
	
	
	public RecursiveParsing(Vector<Pair<TokenNames,String>> inputTokens1) {
		//removeUnnecessaryTokens(inputTokens1);
		inputTokens = inputTokens1;
		currentToken = TokenNames.None;
		numberOfLocalVars = new HashMap<String, Integer>();
		symbolTable = new HashMap<String, HashMap<String, Integer>>();
	}
	
	
	/**
	 * initialized the parsing and prints out the results when finished
	 */
	public boolean parse() {
		return program();
	}
	
	/**
	 * <program> --> <type name> ID <data decls> <func list> | empty
	 * @return A boolean indicating pass or error 
	 */
	private boolean program() {
		// check if we are at the eof
		if(inputTokens.firstElement().getKey() == TokenNames.eof) {
			return true;
		}
		else if(type_name()) {
			currentFunction = "global";
			if(symbolTable.get("global")==null){
				symbolTable.put(currentFunction, new HashMap<String,Integer>());
				currentCount=0;
			}
			if(inputTokens.firstElement().getKey() == TokenNames.ID) {
				Pair<TokenNames,String> lastElement = inputTokens.remove(0); // get the ID token
				currentToken = lastElement.getKey();
				if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
					if(currentFunction!=null){
						numberOfLocalVars.put(currentFunction, currentCount);
					}
					currentFunction = lastElement.getValue();
					symbolTable.put(currentFunction, new HashMap<String,Integer>());
					currentCount = 0;
				}
				else {
					if(symbolTable.get(currentFunction).get(lastElement.getValue())==null) {
						symbolTable.get(currentFunction).put(lastElement.getValue(), currentCount);
						currentCount++;
					}
				}
				if(data_decls() && func_list()) {
					//check to see if the remaining token is eof is so this is a legal syntax
					if(inputTokens.firstElement().getKey() == TokenNames.eof) {
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
	private boolean func_list()
	
	{
		if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
			currentToken = inputTokens.remove(0).getKey();
			if(parameter_list()) {
				if(inputTokens.firstElement().getKey() == TokenNames.right_parenthesis) {
					currentToken = inputTokens.remove(0).getKey();
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
		if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
			currentToken = inputTokens.remove(0).getKey(); // remove the token from the stack
			return true;
		}
		
		if(inputTokens.firstElement().getKey() == TokenNames.left_brace) {
			currentToken = inputTokens.remove(0).getKey();
			if(data_decls_Z()) {
				if(statements()) {
					if(inputTokens.firstElement().getKey() == TokenNames.right_brace) {
						currentToken = inputTokens.remove(0).getKey();
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
			if(inputTokens.firstElement().getKey() == TokenNames.ID) {
				Pair<TokenNames,String> lastElement = inputTokens.remove(0);
				currentToken = lastElement.getKey();
				if(currentFunction!=null){
					numberOfLocalVars.put(currentFunction, currentCount);
				}
				currentFunction = lastElement.getValue();
				symbolTable.put(currentFunction, new HashMap<String,Integer>());
				currentCount=0;
				if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
					currentToken = inputTokens.remove(0).getKey();
					if(parameter_list()) {
						if(inputTokens.firstElement().getKey() == TokenNames.right_parenthesis) {
							currentToken = inputTokens.remove(0).getKey();
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
		numberOfLocalVars.put(currentFunction, currentCount);
		return true;		
	}
	
	/**
	 * <type name> --> int | void | binary | decimal 
	 * @return A boolean indicating if the rule passed or failed
	 */
	private boolean type_name() {
		if(inputTokens.firstElement().getKey() == TokenNames.Int || inputTokens.firstElement().getKey() == TokenNames.Void 
				|| inputTokens.firstElement().getKey() == TokenNames.binary || inputTokens.firstElement().getKey() == TokenNames.decimal) {
			currentToken = inputTokens.remove(0).getKey();
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
		if(inputTokens.firstElement().getKey() == TokenNames.Void) {
			currentToken = inputTokens.remove(0).getKey();
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
		if(inputTokens.firstElement().getKey() == TokenNames.ID) {
			Pair<TokenNames,String> lastElement = inputTokens.remove(0);
			currentToken = lastElement.getKey();
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
		if(inputTokens.firstElement().getKey() == TokenNames.Int || inputTokens.firstElement().getKey() == TokenNames.binary || 
				inputTokens.firstElement().getKey() == TokenNames.decimal) {
			currentToken = inputTokens.remove(0).getKey();
			if(inputTokens.firstElement().getKey() == TokenNames.ID) {
				Pair<TokenNames,String> lastElement = inputTokens.remove(0);
				currentToken = lastElement.getKey();
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
		if(inputTokens.firstElement().getKey() == TokenNames.comma) {
			currentToken = inputTokens.remove(0).getKey();
			if(type_name()) {
				if(inputTokens.firstElement().getKey() == TokenNames.ID) {
					Pair<TokenNames,String> lastElement = inputTokens.remove(0);
					currentToken = lastElement.getKey();
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
	private boolean data_decls() {
		if(id_list_Z()) {
			if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
				currentToken = inputTokens.remove(0).getKey();
				// count variable 
				return program(); //data_decls_Z();
			}
			return false;
		}
		if(id_list_prime()) {
			if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
				currentToken = inputTokens.remove(0).getKey();
				// since we consume the first id before we get here count this as a variable
				return program(); //data_decls_Z();
			}
			//return false;
		}
		return true;
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
				if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
					currentToken = inputTokens.remove(0).getKey();
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
		if(inputTokens.firstElement().getKey() == TokenNames.left_bracket) {
			Pair<TokenNames, String> el = inputTokens.remove(0);
			if(inputTokens.firstElement().getKey() == TokenNames.NUMBER) {
				el = inputTokens.remove(0);
				int num = Integer.parseInt(el.getValue());
				currentCount+=num-1;
				if(inputTokens.firstElement().getKey() == TokenNames.right_bracket) {
					currentToken = inputTokens.remove(0).getKey();
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
		if(inputTokens.firstElement().getKey() == TokenNames.comma) {
			currentToken = inputTokens.remove(0).getKey();
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
		if(inputTokens.firstElement().getKey() == TokenNames.ID) {
			Pair<TokenNames,String> lastElement = inputTokens.remove(0);
			currentToken = lastElement.getKey();
			if(symbolTable.get(currentFunction).get(lastElement.getValue())==null) {
				symbolTable.get(currentFunction).put(lastElement.getValue(), currentCount);
				currentCount++;
			}
			return id_Z();
		}
		return false;
	}
	
	/**
	 * <id Z> --> left_bracket <expression> right_bracket | empty
	 * @return a boolean
	 */
	private boolean id_Z() {
		if(inputTokens.firstElement().getKey() == TokenNames.left_bracket) {
			Pair<TokenNames, String> el = inputTokens.remove(0);
			if(inputTokens.firstElement().getKey()==TokenNames.NUMBER) {
				el = inputTokens.remove(0);
				int num = Integer.parseInt(el.getValue());
				if(inputTokens.firstElement().getKey() == TokenNames.right_bracket) {
					currentToken = inputTokens.remove(0).getKey();
					currentCount+=num-1;
					// count the number of variables 
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
	private boolean block_statements() {
		if(inputTokens.firstElement().getKey() == TokenNames.left_brace) {
			currentToken = inputTokens.remove(0).getKey();
			if(statements()) {
				if(inputTokens.firstElement().getKey() == TokenNames.right_brace) {
					currentToken = inputTokens.remove(0).getKey();
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
		if(inputTokens.firstElement().getKey() == TokenNames.ID) {
			String x = inputTokens.remove(0).getValue();
			if(symbolTable.get(currentFunction).get(x)==null && symbolTable.get("global").get(x)==null ) {
				currentCount++;
			}
			return statement_Z();
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
		if(inputTokens.firstElement().getKey() == TokenNames.read) {
			currentToken = inputTokens.remove(0).getKey();
			if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
				currentToken = inputTokens.remove(0).getKey();
				if(inputTokens.firstElement().getKey() == TokenNames.ID) {
					currentToken = inputTokens.remove(0).getKey();
					if(inputTokens.firstElement().getKey() == TokenNames.right_parenthesis) {
						currentToken = inputTokens.remove(0).getKey();
						if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
							currentToken = inputTokens.remove(0).getKey();
							return true;
						}
					}
				}
			}
			return false;
		}
		
		// write left_parenthesis <expression> right_parenthesis semicolon
		if(inputTokens.firstElement().getKey() == TokenNames.write) {
			currentToken = inputTokens.remove(0).getKey();
			if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
				currentToken = inputTokens.remove(0).getKey();
				if(expression()) {
					if(inputTokens.firstElement().getKey() == TokenNames.right_parenthesis) {
						currentToken = inputTokens.remove(0).getKey();
						if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
							currentToken = inputTokens.remove(0).getKey();
							return true;
						}
					}
				}
			}
			return false;
		}
		
		// print left_parenthesis  STRING right_parenthesis semicolon
		if(inputTokens.firstElement().getKey() == TokenNames.print) {
			currentToken = inputTokens.remove(0).getKey();
			if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
				currentToken = inputTokens.remove(0).getKey();
				if(inputTokens.firstElement().getKey() == TokenNames.STRING) {
					currentToken = inputTokens.remove(0).getKey();
					if(inputTokens.firstElement().getKey() == TokenNames.right_parenthesis) {
						currentToken = inputTokens.remove(0).getKey();
						if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
							currentToken = inputTokens.remove(0).getKey();
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
	private boolean statement_Z() {
		if(assignment_Z()) {
			return true;
		}
		else if(func_call()) {
			return true;
		}
		return false;
	}
	
	/**
	 * <assignment Z> --> equal_sign <expression> semicolon | 
	 * left_bracket <expression> right_bracket equal_sign <expression> semicolon
	 * @return a boolean
	 */
	private boolean assignment_Z() {
		if(inputTokens.firstElement().getKey() == TokenNames.equal_sign) {
			currentToken = inputTokens.remove(0).getKey();
			if(expression()) {
				if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
					currentToken = inputTokens.remove(0).getKey();
					return true;
				}
			}
			return false;
		}
		if(inputTokens.firstElement().getKey() == TokenNames.left_bracket) {
			currentToken = inputTokens.remove(0).getKey();
			if(expression()) {
				if(inputTokens.firstElement().getKey() == TokenNames.right_bracket) {
					currentToken = inputTokens.remove(0).getKey();
					if(inputTokens.firstElement().getKey() == TokenNames.equal_sign) {
						currentToken = inputTokens.remove(0).getKey();
						if(expression()) {
							if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
								currentToken = inputTokens.remove(0).getKey();
								currentCount++;
								return true;
							}
						}
					}
				}
			}
			return false;
		}
		return false;
	}
	
	/**
	 * <func call> --> left_parenthesis <expr list> right_parenthesis semicolon 
	 * @return a boolean
	 */
	private boolean func_call() {
		if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
			currentToken = inputTokens.remove(0).getKey();
			if(expr_list()) {
				if(inputTokens.firstElement().getKey() == TokenNames.right_parenthesis) {
					currentToken = inputTokens.remove(0).getKey();
					if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
						currentToken = inputTokens.remove(0).getKey();
						return true;
					}
				}
			}
		}		
		return false;
	}
	
	/**
	 * <expr list> --> empty | <non-empty expr list> 
	 * @return a boolean
	 */
	private boolean expr_list() {
		if(non_empty_expr_list()) {
			return true;
		}
		return true;
	}
	
	/**
	 * <non-empty expr list> --> <expression> <non-empty expr list prime>
	 * @return a boolean
	 */
	private boolean non_empty_expr_list() {
		if(expression()) {
			return non_empty_expr_list_prime();
		}
		return false;
	}
	
	/**
	 * <non-empty expr list prime> --> comma <expression> <non-empty expr list prime> | empty
	 * @return a boolean
	 */
	private boolean non_empty_expr_list_prime() {
		if(inputTokens.firstElement().getKey() == TokenNames.comma) {
			currentToken = inputTokens.remove(0).getKey();
			if(expression()) {
				return non_empty_expr_list_prime();
			}
			return false;
		}
		return true;
	}
	
	/**
	 * <if statement> --> if left_parenthesis <condition expression> right_parenthesis <block statements> 
	 * @return a boolean
	 */
	private boolean if_statement() {
		if(inputTokens.firstElement().getKey() == TokenNames.If) {
			currentToken = inputTokens.remove(0).getKey();
			if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
				currentToken = inputTokens.remove(0).getKey();
				if(condition_expression()) {
					if(inputTokens.firstElement().getKey() == TokenNames.right_parenthesis) {
						currentToken = inputTokens.remove(0).getKey();
						return block_statements();
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
	private boolean condition_expression() {
		if(condition()) {
			currentCount++;
			return condition_expression_Z();
		}
		return false;
	}
	
	/**
	 * <condition expression Z> --> <condition op> <condition> | empty
	 * @return a boolean
	 */
	private boolean condition_expression_Z() {
		if(condition_op()) {
			return condition();
		}
		return true;
	}
	
	/**
	 * <condition op> --> double_end_sign | double_or_sign 
	 * @return a boolean
	 */
	private boolean condition_op() {
		if(inputTokens.firstElement().getKey() == TokenNames.double_and_sign || inputTokens.firstElement().getKey() == TokenNames.double_or_sign) {
			currentToken = inputTokens.remove(0).getKey();
			return true;
		}
		return false;
	}
	
	/**
	 * <condition> --> <expression> <comparison op> <expression> 
	 * @return a boolean
	 */
	private boolean condition() {
		if(expression()) {
			if(comparison_op()) {
				currentCount++;
				return expression();
			}
		}
		return false;
	}
	
	/**
	 * <comparison op> --> == | != | > | >= | < | <=
	 * @return a boolean
	 */
	private boolean comparison_op() {
		if(inputTokens.firstElement().getKey() == TokenNames.doubleEqualSign || inputTokens.firstElement().getKey() == TokenNames.notEqualSign ||
				inputTokens.firstElement().getKey() == TokenNames.greaterThenSign || inputTokens.firstElement().getKey() == TokenNames.greaterThenOrEqualSign ||
				inputTokens.firstElement().getKey() == TokenNames.lessThenSign || inputTokens.firstElement().getKey() == TokenNames.lessThenOrEqualSign) {
			currentToken = inputTokens.remove(0).getKey();
			return true;
		}
		return false;
	}
	
	/**
	 * <while statement> --> while left_parenthesis <condition expression> right_parenthesis <block statements> 
	 * @return
	 */
	private boolean while_statement() {
		if(inputTokens.firstElement().getKey() == TokenNames.While) {
			currentToken = inputTokens.remove(0).getKey();
			if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
				currentToken = inputTokens.remove(0).getKey();
				if(condition_expression()){
					if(inputTokens.firstElement().getKey() == TokenNames.right_parenthesis) {
						currentToken = inputTokens.remove(0).getKey();
						return block_statements();
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
		if(inputTokens.firstElement().getKey() == TokenNames.Return) {
			currentToken = inputTokens.remove(0).getKey();
			return return_statement_Z();
		}
		return false;
	}
	
	/**
	 * <return statement Z> --> <expression> semicolon | semicolon 
	 * @return a boolean
	 */
	private boolean return_statement_Z() {
		if(expression()) {
			if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
				currentToken = inputTokens.remove(0).getKey();
				return true;
			}
			return false;
		}
		if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
			currentToken = inputTokens.remove(0).getKey();
			return true;
		}
		return false;
	}
	
	/**
	 * <break statement> ---> break semicolon
	 * @return a boolean
	 */
	private boolean break_statement() {
		if(inputTokens.firstElement().getKey() == TokenNames.Break) {
			currentToken = inputTokens.remove(0).getKey();
			if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
				currentToken = inputTokens.remove(0).getKey();
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
		if(inputTokens.firstElement().getKey() == TokenNames.Continue) {
			currentToken = inputTokens.remove(0).getKey();
			if(inputTokens.firstElement().getKey() == TokenNames.semicolon) {
				currentToken = inputTokens.remove(0).getKey();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * <expression> --> <term> <expression prime>
	 * @return a boolean
	 */
	private boolean expression() {
		if(term()) {
			int ret = expression_prime();
			if( ret==0)
				return false;
			else if(ret==1) {
				currentCount++;
				return true;
			}
			else {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * <expression prime> --> <addop> <term> <expression prime> | empty
	 * @return
	 */
	private int expression_prime() {
		if(addop()) {
			if(term()) {
				int ret = expression_prime();
				if( ret==0)
					return 0;
				else if( ret==1){
					currentCount++;
					return 1;
				}
				else
					return 1;
			}
			return 0;
		}
		return 2;
	}
	
	/**
	 * <addop> --> plus_sign | minus_sign 
	 * @return a boolean
	 */
	private boolean addop() {
		if(inputTokens.firstElement().getKey() == TokenNames.plus_sign || inputTokens.firstElement().getKey() == TokenNames.minus_sign) {
			currentToken = inputTokens.remove(0).getKey();
			return true;
		}
		return false;
	}
	
	/**
	 * <term> --> <factor> <term prime>
	 * @return a boolean
	 */
	private boolean term() {
		if(factor()) {
			int ret = term_prime();
			if(ret==0)
				return false;
			else if(ret==1){
				currentCount++;
				return true;
			}
			else {
				return true;
			}
			//return term_prime();
		}
		return false;
	}
	
	/**
	 * <term prime> --> <mulop> <factor> <term prime> | empty
	 * @return
	 */
	private int term_prime() {
		if(mulop()) {
			if(factor()) {
				int ret = term_prime();
				if(ret==0){
					return 0;
				}
				else if(ret==1){
					currentCount++;
					return 1;
				}
				else
					return 1;
			}
			return 0;
		}
		return 2;
	}
	
	/**
	 * <mulop> --> star_sign | forward_slash 
	 * @return a boolean
	 */
	private boolean mulop() {
		if(inputTokens.firstElement().getKey() == TokenNames.star_sign || inputTokens.firstElement().getKey() == TokenNames.forward_slash) {
			currentToken = inputTokens.remove(0).getKey();
			return true;
		}
		return false;
	}
	
	/**
	 * <factor> --> ID <factor Z> | NUMBER | minus_sign NUMBER | left_parenthesis <expression>right_parenthesis 
	 * @return
	 */
	private boolean factor() {
		if(inputTokens.firstElement().getKey() == TokenNames.ID) {
			Pair<TokenNames,String> lastElement = inputTokens.remove(0);
			currentToken = lastElement.getKey();
			if(symbolTable.get(currentFunction).get(lastElement.getValue())==null && inputTokens.firstElement().getKey()!=TokenNames.left_parenthesis) {
				currentCount++;
			}
			return factor_Z();
		}
		// NUMBER
		if(inputTokens.firstElement().getKey() == TokenNames.NUMBER) {
			currentToken = inputTokens.remove(0).getKey();
			return true;
		}
		
		// minus_sign NUMBER
		if(inputTokens.firstElement().getKey() == TokenNames.minus_sign) {
			currentToken = inputTokens.remove(0).getKey();
			if(inputTokens.firstElement().getKey() == TokenNames.NUMBER) {
				currentToken = inputTokens.remove(0).getKey();
				return true;
			}
			return false;
		}
		
		// left_parenthesis <expression>right_parenthesis
		if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
			currentCount++;
			currentToken = inputTokens.remove(0).getKey();
			if(expression()) {
				if(inputTokens.firstElement().getKey() == TokenNames.right_parenthesis) {
					currentToken = inputTokens.remove(0).getKey();
					currentCount++;
					return true;
				}
			}
			return false;
		}
		return false;
	}
	
	/**
	 * <factor Z> --> left_bracket <expression> right_bracket | left_parenthesis <expr list> right_parenthesis | empty
	 * @return
	 */
	private boolean factor_Z() {
		// left_bracket <expression> right_bracket
		if(inputTokens.firstElement().getKey() == TokenNames.left_bracket) {
			currentToken = inputTokens.remove(0).getKey();
			if(expression()) {
				if(inputTokens.firstElement().getKey() == TokenNames.right_bracket) {
					currentToken = inputTokens.remove(0).getKey();
					currentCount++;
					return true;
				}
			}
			return false;
		}
		// left_parenthesis <expr list> right_parenthesis
		if(inputTokens.firstElement().getKey() == TokenNames.left_parenthesis) {
			currentToken = inputTokens.remove(0).getKey();
			if(expr_list()) {
				if(inputTokens.firstElement().getKey() == TokenNames.right_parenthesis) {
					currentToken = inputTokens.remove(0).getKey();
					currentCount++;
					return true;
				}
			}
			return false;
		}
		// empty
		return true;
	}
	

}
