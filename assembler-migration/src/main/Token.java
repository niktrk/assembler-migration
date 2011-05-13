package main;

public class Token {
	int code;
	int val;
	boolean hex;
	String str;
	
	public Token(){
		
	}

	public Token(int code, int val, boolean hex, String str){
		this.code = code;
		this.val = val;
		this.hex = hex;
		this.str = str;
	}
	
	public Token(int code, String str) {
		this(code, 0, false, str);
	}
	
	public void write(){
		System.out.println(code);
		System.out.println(val);
		System.out.println(str);
	}

}
