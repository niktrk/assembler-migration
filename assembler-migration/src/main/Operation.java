package main;

/**
 * Enumeration class representing some of assembler instructions which are basically arithmetic
 * operations.
 * 
 * @author Nikola Trkulja
 * @author Igor Let
 */
public enum Operation {

	ADDITION("+"),

	SUBTRACTION("-"),

	COMPARE("-"),

	INCREMENTATION("+"),

	DECREMENTATION("-"),

	MULTIPLICATION("*"),

	DIVISION("/");

	private final String operator;

	private Operation(String operator) {
		this.operator = operator;
	}

	public String getOperator() {
		return operator;
	}

}
