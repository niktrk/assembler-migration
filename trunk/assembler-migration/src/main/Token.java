package main;

public class Token {
	int code;
	int val;
	boolean hex;
	String str;
	
	public Token(){
		
	};

	public Token(int code, int val, boolean hex, String str){
		this.code = code;
		this.val = val;
		this.hex = hex;
		this.str = str;
	}

}
