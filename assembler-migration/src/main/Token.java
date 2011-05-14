package main;

public class Token {

	int code;
	int val;
	String str;

	public Token() {
	}

	public Token(int code, int val, String str) {
		this.code = code;
		this.val = val;
		this.str = str;
	}

	public Token(int code, String str) {
		this(code, 0, str);
	}

	public void write() {
		System.out.println(code);
		System.out.println(val);
		System.out.println(str);
	}

}
