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
	int line;

	public Token(int line) {
		this.line = line;
	}

	public Token(int code, int val, String str, int line) {
		this.code = code;
		this.val = val;
		this.str = str;
		this.line = line;
	}

	public boolean sameAs(Token token) {
		return this.code == token.code && this.val == token.val && this.str.equals(token.str);
	}

	@Override
	public String toString() {
		String out = "Token: \ncode: " + code;
		out += "\n val: " + val;
		out += "\n str: " + str;
		out += "\n line: " + line;
		return out;
	}

}
