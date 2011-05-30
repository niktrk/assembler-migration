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
	private static final String BEGIN = "_begin_";

	private String buffer;
	private boolean inProc;
	private boolean closed;

	public CodeBuffer() {
		buffer = new String();
		inProc = false;
		closed = false;
		init();
	}

	/**
	 * Initialize buffer. Create starting template and insert flags and registers to declarations.
	 */
	private void init() {
		String template = "VAR < " + DECLARATION + " >: " + NEW_LINE + BEGIN + NEW_LINE + BODY + PROCEDURE + "ENDVAR";

		String declaration = "flag_o := 0, flag_s := 0, flag_z := 0, flag_c := 0, ax := 0, " + NEW_LINE
				+ " bx:= 0, cx:= 0, dx:= 0, temp := 0, si:= 0, di:= 0, bp:= 0, sp:= 0, " + NEW_LINE
				+ " cs:= 0, ds:= 0, ss:= 0, es:= 0 " + NEW_LINE + " " + DECLARATION;

		buffer = template.replace(DECLARATION, declaration);
	}

	/**
	 * Close buffer. Remove declaration, body and procedure markers and remove every semicolon after which there is word
	 * "END" so generated WSL code could compile without errors.
	 */
	public void close() {
		buffer = buffer.replace(DECLARATION, "");
		buffer = buffer.replace(BODY, "");
		buffer = buffer.replace(BEGIN, "");
		buffer = buffer.replace(PROCEDURE, "");
		// temporary fix, I hope :)
		buffer = buffer.replace(";" + NEW_LINE + "END", NEW_LINE + "END");
		closed = true;
	}

	/**
	 * Insert to declaration.
	 * 
	 * @param s
	 */
	public void insertIntoDeclaration(String... s) {
		checkClosed();
		for (int i = 0; i < s.length; i++) {
			buffer = buffer.replace(DECLARATION, s[i] + DECLARATION);
		}
	}

	/**
	 * Insert to body.
	 * 
	 * @param s
	 */
	public void insertIntoBody(String... s) {
		checkClosed();
		for (int i = 0; i < s.length; i++) {
			buffer = buffer.replace(BODY, s[i] + BODY);
		}
		buffer = buffer.replace(BODY, NEW_LINE + BODY);
	}

	/**
	 * Insert to procedure.
	 * 
	 * @param s
	 */
	public void insertIntoProcedure(String... s) {
		checkClosed();
		for (int i = 0; i < s.length; i++) {
			buffer = buffer.replace(PROCEDURE, s[i] + PROCEDURE);
		}
		buffer = buffer.replace(PROCEDURE, NEW_LINE + PROCEDURE);
	}

	/**
	 * Insert to body or procedure regarding if we are currently generating code for procedure or not.
	 * 
	 * @param s
	 */
	public void insert(String... s) {
		if (inProc) {
			insertIntoProcedure(s);
		} else {
			insertIntoBody(s);
		}
	}
	
	public void addBegin(){
		buffer = buffer.replace(BEGIN, "BEGIN" + BEGIN);
	}

	/**
	 * Check if buffer is closed.
	 */
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
