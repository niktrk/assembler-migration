package main;

/**
 * Class used as buffer for code generated during parsing.
 * 
 * @author Nikola Trkulja
 * @author Igor Let
 */
public class CodeBuffer {

	private static final String NEW_LINE = System.getProperty("line.separator");
	private static final String DECLARATION = "_decl_";
	private static final String BODY = "_body_";
	private static final String PROCEDURE = "_proc_";

	private String buffer;
	private boolean inProc;
	private boolean closed;

	public CodeBuffer() {
		buffer = new String();
		inProc = false;
		closed = false;
		init();
	}

	private void init() {
		String template = "VAR < " + DECLARATION + " >: " + NEW_LINE + BODY + PROCEDURE + "ENDVAR";

		String declaration = "flag_o := 0, flag_s := 0, flag_z := 0, flag_c := 0, ax := 0, " + NEW_LINE
				+ " bx:= 0, cx:= 0, dx:= 0, temp := 0, si:= 0, di:= 0, bp:= 0, sp:= 0, " + NEW_LINE
				+ " cs:= 0, ds:= 0, ss:= 0, es:= 0 " + NEW_LINE + " " + DECLARATION;

		buffer = template.replace(DECLARATION, declaration);
	}

	public void close() {
		buffer = buffer.replace(DECLARATION, "");
		buffer = buffer.replace(BODY, "");
		buffer = buffer.replace(PROCEDURE, "");
		// temporary fix, I hope :)
		buffer = buffer.replace(";" + NEW_LINE + "END", NEW_LINE + "END");
		closed = true;
	}

	public void insertIntoDeclaration(String... s) {
		checkClosed();
		for (int i = 0; i < s.length; i++) {
			buffer = buffer.replace(DECLARATION, s[i] + DECLARATION);
		}
	}

	public void insertIntoBody(String... s) {
		checkClosed();
		for (int i = 0; i < s.length; i++) {
			buffer = buffer.replace(BODY, s[i] + BODY);
		}
		buffer = buffer.replace(BODY, NEW_LINE + BODY);
	}

	public void insertIntoProcedure(String... s) {
		checkClosed();
		for (int i = 0; i < s.length; i++) {
			buffer = buffer.replace(PROCEDURE, s[i] + PROCEDURE);
		}
		buffer = buffer.replace(PROCEDURE, NEW_LINE + PROCEDURE);
	}

	public void insert(String... s) {
		if (inProc) {
			insertIntoProcedure(s);
		} else {
			insertIntoBody(s);
		}
	}

	private void checkClosed() {
		if (closed) {
			throw new IllegalStateException("Code buffer is closed, can not insert.");
		}
	}

	public void setInProc(boolean inProc) {
		this.inProc = inProc;
	}

	public boolean isInProc() {
		return inProc;
	}

	@Override
	public String toString() {
		return buffer;
	}

}
