package main;

/**
 * Class defines token entity used during scanning, parsing and code generation.
 * 
 * @author Igor Let
 * @author Nikola Trkulja
 */
public class Token {

	int code;
	int val;
	String str;

	public Token() {
		super();
	}

	public Token(int code, int val, String str) {
		super();
		this.code = code;
		this.val = val;
		this.str = str;
	}

	public Token(int code, String str) {
		this(code, 0, str);
	}

	public boolean sameAs(Token token) {
		return this.code == token.code && this.val == token.val && this.str.equals(token.str);
	}

	@Override
	public String toString() {
		String out = "Token: \ncode: " + code;
		out += "\n val: " + val;
		out += "\n str: " + str;
		return out;
	}

}
