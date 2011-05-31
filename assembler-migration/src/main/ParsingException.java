package main;

import java.io.PrintStream;

/**
 * {@link RuntimeException} subclass for logging exceptions encountered during parsing and code
 * generation.
 * 
 * @author Nikola Trkulja
 * @author Igor Let
 */
public class ParsingException extends RuntimeException {

	private static final long serialVersionUID = 7114029755696702828L;

	public ParsingException(int line, String message) {
		super("[line " + line + "]" + message);
	}

	public ParsingException(int line, String actual, String expected) {
		super("[line " + line + "] expected: " + expected + ", but got: " + actual + ".");
	}

	public ParsingException(int line, int actualCode, int... expectedCodes) {
		super("[line " + line + "] expected: " + formatTokens(expectedCodes) + ", but got: "
				+ AbstractCompiler.str[actualCode] + ".");
	}

	@Override
	public void printStackTrace(PrintStream s) {
		System.out.println();
		System.out.println(getMessage());
	}

	private static String formatTokens(int... codes) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < codes.length; i++) {
			sb.append(AbstractCompiler.str[codes[i]]);
			if (i < codes.length - 1) {
				sb.append(" or ");
			}
		}
		sb.append("]");
		return sb.toString();
	}

}
