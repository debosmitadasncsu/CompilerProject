/*
 * Class to create a tree. This node is mainly used by various child
 * productions to inform the parent about the values parsed by them. 
 * This class is most heavily used to build the expression tree.
 */
public class Node {
	private String value; // The value (eg: local[5]) in which the combined result of all children are stored.
	String special; //  this field can be used if the child has any additional data to the tell the parent.
	int retValue; // This field specifies if the child's status: success, failure, empty.
	
	public Node() {
		value = new String();
	}
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
}
