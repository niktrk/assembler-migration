package main;

public enum Operation {

	ADDITION("+"),
	
	SUBTRACTION("-"),
	
	COMPARE("-"),
	
	INCREMENTATION("+"),
	
	DECREMENTATION("-"),
	
	MULTIPLICATION("*"),
	
	DIVISION("/");
	
	private String operator;
	
	private Operation(String operator) {
		this.operator = operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getOperator() {
		return operator;
	}
	
}
