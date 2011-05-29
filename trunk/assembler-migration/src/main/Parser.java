package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Parser and code generation component of the translator.
 * 
 * @author Igor Let
 * @author Nikola Trkulja
 * 
 * @date 2. 5. 2011.
 */
public class Parser extends AbstractCompiler {

	private static final String NEW_LINE = System.getProperty("line.separator");

	private Scanner sc;
	private Token curr, la;
	private ListIterator<Token> tokenListIterator;
	private String buffer;
	private boolean inProc;

	private BitSet oneArgComm, twoArgComm, registers, lowByte, highByte, doubleByte, singleByte;
	private Map<String, Size> variableSize;
	private Map<String, List<Token>> macroParams;
	private Map<String, List<Token>> macroTokens;

	public Parser(Scanner sc) {
		this.sc = sc;

		oneArgComm = new BitSet();
		oneArgComm.set(interr);
		oneArgComm.set(loop);
		oneArgComm.set(push);
		oneArgComm.set(pop);
		oneArgComm.set(inc);
		oneArgComm.set(dec);
		oneArgComm.set(call);
		oneArgComm.set(jmp);
		oneArgComm.set(ja);
		oneArgComm.set(jae);
		oneArgComm.set(jb);
		oneArgComm.set(jbe);
		oneArgComm.set(jg);
		oneArgComm.set(jge);
		oneArgComm.set(jl);
		oneArgComm.set(jle);
		oneArgComm.set(je);
		oneArgComm.set(neg);
		oneArgComm.set(mul);
		oneArgComm.set(div);

		twoArgComm = new BitSet();
		twoArgComm.set(mov);
		twoArgComm.set(xchg);
		twoArgComm.set(cmp);
		twoArgComm.set(add);
		twoArgComm.set(sub);
		twoArgComm.set(shl);
		twoArgComm.set(shr);

		registers = new BitSet();
		registers.set(ax);
		registers.set(al);
		registers.set(ah);
		registers.set(bx);
		registers.set(bl);
		registers.set(bh);
		registers.set(cx);
		registers.set(cl);
		registers.set(ch);
		registers.set(dx);
		registers.set(dl);
		registers.set(dh);
		registers.set(si);
		registers.set(di);
		registers.set(bp);
		registers.set(sp);
		registers.set(cs);
		registers.set(ds);
		registers.set(ss);
		registers.set(es);

		lowByte = new BitSet();
		lowByte.set(al);
		lowByte.set(bl);
		lowByte.set(cl);
		lowByte.set(dl);

		highByte = new BitSet();
		highByte.set(ah);
		highByte.set(bh);
		highByte.set(ch);
		highByte.set(dh);

		singleByte = new BitSet();
		singleByte.or(lowByte);
		singleByte.or(highByte);

		doubleByte = (BitSet) registers.clone();
		doubleByte.xor(lowByte);
		doubleByte.xor(highByte);

		buffer = new String();

		variableSize = new HashMap<String, Size>();
		macroParams = new HashMap<String, List<Token>>();
		macroTokens = new HashMap<String, List<Token>>();

		inProc = false;

		curr = nextToken();
		la = nextToken();
	}

	/**
	 * Get code of double byte register for given code of its low or high part. For example, return value for given code
	 * of al or ah is code of ax.
	 * 
	 * @param code
	 * @return
	 */
	private String getXRegister(int code) {
		if (lowByte.get(code) || highByte.get(code)) {
			return str[code].charAt(0) + "x";
		} else {
			throw new IllegalArgumentException("Invalid token code number: " + code);
		}
	}

	/**
	 * Get generated string for obtaining value of low or high part of given 16bit register.
	 * 
	 * @param code
	 * @return
	 */
	private String getByteRegister(int code) {
		if (lowByte.get(code)) {
			return str[code].charAt(0) + "x MOD 256";
		} else if (highByte.get(code)) {
			return str[code].charAt(0) + "x DIV 256";
		} else {
			throw new IllegalArgumentException("Invalid token code number: " + code);
		}
	}

	/**
	 * Checks if current token is one of expected tokens which are provided as arguments. If that is the case moves to
	 * next token, and if it's not throws exception.
	 * 
	 * @param code
	 */
	private void check(int... code) {
		for (int i = 0; i < code.length; i++) {
			if (curr.code == code[i]) {
				// read next token
				curr = la;
				la = nextToken();
				return;
			}
		}
		// error
		throw new IllegalStateException("Expected to get some of: " + Arrays.toString(code) + " but current token is: "
				+ curr.code);
	}

	/**
	 * Get next token. First checks if there are some injected tokens, if that is the case returns next token using
	 * iterator, and if it's not return next token from scanner.
	 * 
	 * @return
	 */
	private Token nextToken() {
		if (tokenListIterator != null && tokenListIterator.hasNext()) {
			return tokenListIterator.next();
		}
		return sc.next();
	}

	public String parse() {
		String template = "VAR <_decl_>: " + NEW_LINE + "_body__proc_ENDVAR";
		String declaration = "flag_o := 0, flag_s := 0, flag_z := 0, flag_c := 0, ax := 0, " + NEW_LINE
				+ " bx:= 0, cx:= 0, dx:= 0, temp := 0, si:= 0, di:= 0, bp:= 0, sp:= 0, " + NEW_LINE
				+ " cs:= 0, ds:= 0, ss:= 0, es:= 0 " + NEW_LINE + " _decl_";

		buffer = template.replace("_decl_", declaration);
		Program();
		buffer = buffer.replace("_decl_", "");
		buffer = buffer.replace("_body_", "");
		buffer = buffer.replace("_proc_", "");
		// temporary fix, I hope :)
		buffer = buffer.replace(";" + NEW_LINE + "END", NEW_LINE + "END");

		return buffer;
	}

	private void insIntoDecl(String... s) {
		for (int i = 0; i < s.length; i++) {
			buffer = buffer.replace("_decl_", s[i] + "_decl_");
		}
	}

	private void insIntoBody(String... s) {
		for (int i = 0; i < s.length; i++) {
			buffer = buffer.replace("_body_", s[i] + "_body_");
		}
		buffer = buffer.replace("_body_", NEW_LINE + "_body_");
	}

	private void insIntoProc(String... s) {
		for (int i = 0; i < s.length; i++) {
			buffer = buffer.replace("_proc_", s[i] + "_proc_");
		}
		buffer = buffer.replace("_proc_", NEW_LINE + "_proc_");
	}

	private void insert(String... s) {
		if (inProc) {
			insIntoProc(s);
		} else {
			insIntoBody(s);
		}
	}

	private void Program() {
		if (curr.code == title) {
			check(title);
			check(ident);
		}
		check(model);
		check(small, compact, medium, large);
		if (curr.code == stack) {
			check(stack);
			check(number);
			insIntoDecl(", stack := < >");
		}
		if (curr.code == data) {
			Data();
		}
		Code();
	}

	private void Data() {
		boolean array;
		check(data);
		String varName;
		while (curr.code == ident) {
			array = false;
			insIntoDecl(", ", curr.str, " := ");
			if (la.code == db) {
				variableSize.put(curr.str, Size.BYTE);
			} else if (la.code == dw) {
				variableSize.put(curr.str, Size.DOUBLE_BYTE);
			}
			varName = curr.str;
			check(ident);
			check(db, dw);
			// TODO mozemo mozda dodati onaj upitnik shit
			if (curr.code == number || curr.code == string) {
				if (la.code == comma) {
					insIntoDecl("< ");
					array = true;
				}
				Value(varName);
				while (curr.code == comma) {
					check(comma);
					insIntoDecl(", ");
					Value(varName);
				}
				if (array) {
					insIntoDecl(" >");
				}
			} else {
				// error
			}
		}
	}

	private void Value(String varName) {
		if (curr.code == number) {
			insIntoDecl(Integer.toString(toUnsigned(curr.val, variableSize.get(getVariableName(varName)))));
			check(number);
		} else if (curr.code == string) {
			insIntoDecl("\"");
			insIntoDecl(curr.str);
			insIntoDecl("\"");
			check(string);
		} else {
			// error
		}
	}

	private void Code() {
		check(code);

		if (curr.code == ident && (la.code == proc || la.code == macro)) {
			while (curr.code == ident && (la.code == proc || la.code == macro)) {
				if (la.code == proc) {
					insIntoBody("BEGIN");
					insIntoProc("WHERE");
					inProc = true;
					Procedure();
					insIntoProc("END");
				} else {
					Macro();
				}
			}
		}

		if (curr.code == ident && la.code == colon || oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
			insIntoBody("ACTIONS beg:");
			insIntoBody("beg == ");
			inProc = false;
			while (curr.code == ident || oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
				// if we have macro call, inject macro tokens
				if (curr.code == ident && macroParams.get(curr.str) != null) {
					injectMacro();
				} else if (curr.code == ident && la.code == colon) {
					Label();
				} else if (oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
					Statement();
				}

			}
			insIntoBody("END");
			insIntoBody("ENDACTIONS");
		}
	}

	private void Macro() {
		String macroName = curr.str;
		List<Token> currentMacroParams = new ArrayList<Token>();
		check(ident);
		check(macro);
		if (curr.code == ident && la.code != colon) {
			currentMacroParams.add(curr);
			check(ident);
			while (curr.code == comma && la.code == ident) {
				check(comma);
				currentMacroParams.add(curr);
				check(ident);
			}
		}
		macroParams.put(macroName, currentMacroParams);

		List<Token> currentMacroTokens = new ArrayList<Token>();
		while (curr.code != endm) {
			if (curr.code == ident && macroParams.get(curr.str) != null) {
				injectMacro();
			} else {
				currentMacroTokens.add(curr);
				curr = la;
				la = nextToken();
			}
		}
		macroTokens.put(macroName, currentMacroTokens);
		check(endm);
	}

	private void Procedure() {
		insIntoProc("PROC ", curr.str, "() == ");
		insIntoProc("ACTIONS beg: ");
		insIntoProc("beg == ");

		check(ident);
		check(proc);
		if (curr.code == far) {
			check(far);
		}
		while (curr.code == ident || oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
			// if we have macro call, inject macro tokens
			if (curr.code == ident && macroParams.get(curr.str) != null) {
				injectMacro();
			} else if (curr.code == ident && la.code == colon) {
				Label();
			} else if (oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
				Statement();
			}

		}
		check(ret);
		check(ident);
		check(endp);
		insIntoProc("END");
		insIntoProc("ENDACTIONS");
		insIntoProc("END");

	}

	private void Label() {
		insert("CALL ", curr.str);
		insert("END");
		insert(curr.str, " == ");
		check(ident);
		check(colon);
	}

	private void injectMacro() {
		if (macroParams.get(curr.str) != null) {

			String macroName = curr.str;
			List<Token> formalParams = macroParams.get(macroName);
			List<Token> actualParams = new ArrayList<Token>();
			List<Token> tokens = macroTokens.get(macroName);
			List<Token> generatedTokens = new ArrayList<Token>();
			check(ident);

			for (int i = 0; i < formalParams.size(); i++) {
				actualParams.add(curr);
				check(ident, number, string, ax, ah, al, bx, bh, bl, cx, ch, cl, dx, dh, dl, si, di, bp, sp, cs, ds, ss, es);
				if (curr.code == comma) {
					check(comma);
				}
			}

			// iterate through all macro tokens and replace all occurrences of formal parameters with actual parameters
			for (Token token : tokens) {
				// iterate through all formal parameters
				boolean found = false;
				for (int i = 0; i < formalParams.size() && !found; i++) {
					// if current token is formal parameter put actual parameter to list
					if (token.sameAs(formalParams.get(i))) {
						generatedTokens.add(actualParams.get(i));
						found = true;
					}
				}
				if (!found) {
					generatedTokens.add(token);
				}
			}

			generatedTokens.add(curr);
			generatedTokens.add(la);
			tokenListIterator = generatedTokens.listIterator();
			curr = nextToken();
			la = nextToken();

		} else {
			// error
		}
	}

	private void Statement() {
		if (oneArgComm.get(curr.code)) {
			OneArgStatement();
		} else if (twoArgComm.get(curr.code)) {
			TwoArgStatement();
		} else {
			// error
		}
	}

	private Token[] getArguments() {
		Token[] ret = new Token[2];
		ret[0] = Argument();
		check(comma);
		ret[1] = Argument();
		return ret;
	}

	private int toUnsigned(int num, Size size) {
		if (num >= 0) {
			return num;
		}
		return (1 << size.getSize()) + num;
	}

	private void TwoArgStatement() {
		Token[] arguments;
		switch (curr.code) {
		case mov:
			check(mov);
			arguments = getArguments();
			mov(arguments[0], arguments[1]);
			break;
		case xchg:
			check(xchg);
			arguments = getArguments();
			xchg(arguments[0], arguments[1]);
			break;
		case cmp:
			check(cmp);
			arguments = getArguments();
			cmp(arguments[0], arguments[1]);
			break;
		case add:
			check(add);
			arguments = getArguments();
			add(arguments[0], arguments[1]);
			break;
		case sub:
			check(sub);
			arguments = getArguments();
			sub(arguments[0], arguments[1]);
			break;
		case shl:
			check(shl);
			arguments = getArguments();
			int arg2Val = (1 << arguments[1].val);
			// mul(arguments[0], new Token(number, arg2Val,
			// String.valueOf(arg2Val)));
			break;
		case shr:
			check(shr);
			arguments = getArguments();
			arg2Val = (1 << arguments[1].val);
			// div(arguments[0], new Token(number, arg2Val,
			// String.valueOf(arg2Val)));
			break;
		default:
			throw new IllegalArgumentException("Unsupported two argument instruction: " + curr.str);
		}
	}

	private void cmp(Token arg1, Token arg2) {
		arithmeticInstruction(arg1, arg2, Operation.COMPARE);
	}

	/**
	 * Generates code for assembler <strong>add</strong> instruction which performs addition of given arguments.
	 * Example: <code>add dest,src</code> is doing following dest := dest + src. Possible combinations of arguments:
	 * <ul>
	 * <li>reg - reg</li>
	 * <li>mem - reg</li>
	 * <li>reg - mem</li>
	 * <li>reg - immediate data</li>
	 * <li>mem - immediate data</li>
	 * </ul>
	 * Both arguments must be the same size. Add instruction affects zero, carry, overflow and sign (and some other not
	 * important for us) flags.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void add(Token arg1, Token arg2) {
		arithmeticInstruction(arg1, arg2, Operation.ADDITION);
	}

	/**
	 * Generates code for assembler <strong>sub</strong> instruction which performs subtraction of given arguments.
	 * Example: <code>sub dest,src</code> is doing following dest := dest - src. Possible combinations of arguments:
	 * <ul>
	 * <li>reg - reg</li>
	 * <li>mem - reg</li>
	 * <li>reg - mem</li>
	 * <li>reg - immediate data</li>
	 * <li>mem - immediate data</li>
	 * </ul>
	 * Both arguments must be the same size. Sub instruction affects zero, carry, overflow and sign (and some other not
	 * important for us) flags.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void sub(Token arg1, Token arg2) {
		arithmeticInstruction(arg1, arg2, Operation.SUBTRACTION);
	}

	private void arithmeticInstruction(Token arg1, Token arg2, Operation operation) {
		String val1, val2;
		Size size;
		if (doubleByte.get(arg1.code)) {// reg
			size = Size.DOUBLE_BYTE;
			val1 = arg1.str;

			if (doubleByte.get(arg2.code)) {// reg
				val2 = arg2.str;
			} else if (isVariable(arg2.str)) {// mem (dw)
				val2 = arg2.str;
			} else if (arg2.code == number) { // immediate (const)
				int val = toUnsigned(arg2.val, Size.DOUBLE_BYTE);
				val2 = Integer.toString(val);
			} else {
				throw new IllegalArgumentException("First argument of instruction is 16bit register, "
						+ "expeted to get 16bit register, dw variable or constant value for second argument, but got: "
						+ arg2.str);
			}

		} else if (highByte.get(arg1.code)) {// reg
			size = Size.BYTE;
			val1 = getXRegister(arg1.code) + " DIV 256";

			if (singleByte.get(arg2.code)) {// reg
				val2 = getByteRegister(arg2.code);
			} else if (isVariable(arg2.str)) {// mem (db)
				val2 = arg2.str;
			} else if (arg2.code == number) { // immediate (const)
				int val = toUnsigned(arg2.val, Size.BYTE);
				val2 = Integer.toString(val);
			} else {
				throw new IllegalArgumentException("First argument of instruction is 8bit (high part of) register, "
						+ "expeted to get 8bit (high or low part of) register, "
						+ "db variable or constant value for second argument, but got: " + arg2.str);
			}

		} else if (lowByte.get(arg1.code)) {// reg
			size = Size.BYTE;
			val1 = getXRegister(arg1.code) + " MOD 256";

			if (singleByte.get(arg2.code)) {// reg
				val2 = getByteRegister(arg2.code);
			} else if (isVariable(arg2.str)) {// mem (db)
				val2 = arg2.str;
			} else if (arg2.code == number) { // immediate (const)
				int val = toUnsigned(arg2.val, Size.BYTE);
				val2 = Integer.toString(val);
			} else {
				throw new IllegalArgumentException("First argument of instruction is 8bit (low part of) register, "
						+ "expeted to get 8bit (high or low part of) register, "
						+ "db variable or constant value for second argument, but got: " + arg2.str);
			}

		} else if (isVariable(arg1.str)) { // mem (db or dw)
			size = variableSize.get(getVariableName(arg1.str));
			val1 = arg1.str;

			if (doubleByte.get(arg2.code)) {// reg
				val2 = arg2.str;
			} else if (singleByte.get(arg2.code)) {// reg
				val2 = getByteRegister(arg2.code);
			} else if (arg2.code == number) {// immediate (const)
				int val = toUnsigned(arg2.val, size);
				val2 = Integer.toString(val);
			} else {
				throw new IllegalArgumentException("First argument of instruction is db or dw variable, "
						+ "expeted to get register (16bit or 8bit) or constant value for second argument, but got: " + arg2.str);
			}

		} else {
			throw new IllegalArgumentException("Expeted to get 8bit (high or low part of) register, 16bit register, "
					+ "db or dw variable or constant value for first argument, but got: " + arg1.str);
		}

		insert("temp := ", val1, ";");
		insert("temp := temp ", operation.getOperator(), " ", val2, ";");

		switch (operation) {
		case ADDITION:
			setAddFlags(val1, val2, size);
			setResult(arg1);
			break;
		case SUBTRACTION:
			setSubFlags(val1, val2, size);
			setResult(arg1);
			break;
		case COMPARE:
			setSubFlags(val1, val2, size);
			break;
		case INCREMENTATION:
			setIncFlags(val1, val2, size);
			setResult(arg1);
			break;
		case DECREMENTATION:
			setDecFlags(val1, val2, size);
			setResult(arg1);
			break;
		case MULTIPLICATION:
			setMulFlags(size);
			setMulResult(size);
			break;
		default:
			throw new IllegalArgumentException("Operator not supported " + operation.getOperator());
		}
	}

	private void div(Token arg) {
		if (doubleByte.get(arg.code) || variableSize.get(getVariableName(arg.str)).equals(Size.DOUBLE_BYTE)) {
			insert("temp :=  (dx * 65536 + ax) DIV ", arg.str, ";");
			insert("IF ", arg.str, " = 0 OR temp >= 65536 THEN");
			insert("CALL Z");
			insert("ELSE ");
			insert("dx := (dx * 65536 + ax) MOD ", arg.str, ";");
			insert("ax := temp");
			insert("FI;");
		} else { // arg is byte
			insert("temp := ax DIV ", arg.str, ";");
			insert("IF ", arg.str, " = 0 OR temp >= 256 THEN");
			insert("CALL Z");
			insert("ELSE");
			insert("ax := (ax MOD ", arg.str, ") * 256 + temp");
			insert("FI;");
		}
	}

	private void mul(Token arg) {
		if (doubleByte.get(arg.code) || variableSize.get(getVariableName(arg.str)).equals(Size.DOUBLE_BYTE)) {
			arithmeticInstruction(new Token(ax, 0, str[ax]), arg, Operation.MULTIPLICATION);
		} else { // arg is byte
			arithmeticInstruction(new Token(al, 0, str[al]), arg, Operation.MULTIPLICATION);
		}
	}

	private void setMulResult(Size size) {
		if (size == Size.BYTE) {
			insert("ax := temp;");
		} else { // double byte
			insert("ax := temp MOD 65536;");
			insert("dx := temp DIV 65536;");
		}
	}

	private void setMulFlags(Size size) {
		insert("IF temp >= 2**", Integer.toString(size.getSize()), " THEN");
		insert("flag_o = 1;");
		insert("flag_c = 1");
		insert("ELSE");
		insert("flag_o = 0;");
		insert("flag_c = 0");
		insert("FI;");
	}

	private void setResult(Token arg) {
		if (doubleByte.get(arg.code)) {
			insert(arg.str, " := temp;");
		} else if (lowByte.get(arg.code)) {
			insert(getXRegister(arg.code), " := (", getXRegister(arg.code), " DIV 256) * 256 + temp;");
		} else if (highByte.get(arg.code)) {
			insert(getXRegister(arg.code), " := (", getXRegister(arg.code), " MOD 256) + temp * 256;");
		} else {
			insert(arg.str, " := temp;");
		}
	}

	private void setSubFlags(String val1, String val2, Size size) {
		generateSubCarryCheck(size);
		generateZeroCheck();
		generateSignCheck(size);
		generateSubOverflowCheck(val1, val2, size);
	}

	private void setDecFlags(String val1, String val2, Size size) {
		insert("temp := temp + (2**", Integer.toString(size.getSize()), ");");
		generateZeroCheck();
		generateSignCheck(size);
		generateSubOverflowCheck(val1, val2, size);
	}

	private void setAddFlags(String val1, String val2, Size size) {
		generateAddCarryCheck(size);
		generateZeroCheck();
		generateSignCheck(size);
		generateAddOverflowCheck(val1, val2, size);
	}

	private void setIncFlags(String val1, String val2, Size size) {
		insert("temp := temp MOD 2**", Integer.toString(size.getSize()), ";");
		generateZeroCheck();
		generateSignCheck(size);
		generateAddOverflowCheck(val1, val2, size);
	}

	private void generateAddOverflowCheck(String val1, String val2, Size size) {
		insert("IF ", generateGetSignBit(val1, size), " = ", generateGetSignBit(val2, size), " AND ", generateGetSignBit("temp",
				size), " <> ", generateGetSignBit(val2, size), " THEN");
		insert("flag_o := 1");
		insert("ELSE");
		insert("flag_o := 0");
		insert("FI;");
	}

	private void generateSubOverflowCheck(String val1, String val2, Size size) {
		insert("IF ", generateGetSignBit(val1, size), " <> ", generateGetSignBit(val2, size), " AND ", generateGetSignBit("temp",
				size), " = ", generateGetSignBit(val2, size), " THEN");
		insert("flag_o := 1");
		insert("ELSE");
		insert("flag_o := 0");
		insert("FI;");
	}

	private String generateGetSignBit(String val, Size size) {
		return "(((" + val + ") DIV 2**" + Integer.toString(size.getSize() - 1) + ") MOD 2)";
	}

	private void generateZeroCheck() {
		insert("IF temp = 0 THEN");
		insert("flag_z := 1");
		insert("ELSE");
		insert("flag_z := 0");
		insert("FI;");
	}

	private void generateSignCheck(Size size) {
		insert("IF ", generateGetSignBit("temp", size), " = 1 THEN");
		insert("flag_s := 1");
		insert("ELSE");
		insert("flag_s := 0");
		insert("FI;");
	}

	private void generateAddCarryCheck(Size size) {
		insert("IF temp >= 2**", Integer.toString(size.getSize()), " THEN");
		insert("temp := temp MOD 2**", Integer.toString(size.getSize()), ";");
		insert("flag_c := 1");
		insert("ELSE");
		insert("flag_c := 0");
		insert("FI;");
	}

	private void generateSubCarryCheck(Size size) {
		insert("IF temp < 0 THEN");
		insert("temp := temp + (2**", Integer.toString(size.getSize()), ");");
		insert("flag_c := 1");
		insert("ELSE");
		insert("flag_c := 0");
		insert("FI;");
	}

	/**
	 * Generates code for <strong>xchg</strong> assembler instruction which exchanges values of given arguments.
	 * Possible combinations of arguments:
	 * <ul>
	 * <li>reg - reg</li>
	 * <li>mem - reg</li>
	 * <li>reg - mem</li>
	 * </ul>
	 * Both locations must be of the same size.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void xchg(Token arg1, Token arg2) {
		insert("< ", generateMov(arg1, arg2), ", ", generateMov(arg2, arg1), " >;");
	}

	private void mov(Token arg1, Token arg2) {
		// FIXME create 16bit variable @data or smth like that and initialize to random value, to
		// simulate @data pointer
		if (arg2.code == atdata || arg2.code == offset) {
			return;
		}
		insert(generateMov(arg1, arg2), ";");
	}

	private String generateMov(Token arg1, Token arg2) {
		String val2;
		if (doubleByte.get(arg1.code)) {

			if (doubleByte.get(arg2.code)) {
				val2 = str[arg2.code];
			} else if (isVariable(arg2.str)) { // db or dw variable
				val2 = arg2.str;
			} else { // const
				int val = toUnsigned(arg2.val, Size.DOUBLE_BYTE);
				val2 = Integer.toString(val);
			}

			return arg1.str + " := " + val2;

		} else if (highByte.get(arg1.code)) {

			if (highByte.get(arg2.code)) {
				val2 = "(" + getXRegister(arg2.code) + " DIV 256) * 256";
			} else if (lowByte.get(arg2.code)) {
				val2 = "(" + getXRegister(arg2.code) + " MOD 256) * 256";
			} else if (isVariable(arg2.str)) { // db variable
				val2 = arg2.str + " * 256";
			} else { // const
				int val = toUnsigned(arg2.val, Size.BYTE);
				val2 = Integer.toString(val) + " * 256";
			}

			return getXRegister(arg1.code) + " := (" + getXRegister(arg1.code) + " MOD 256) + " + val2;

		} else if (lowByte.get(arg1.code)) {

			if (lowByte.get(arg2.code)) {
				val2 = "(" + getXRegister(arg2.code) + " MOD 256)";
			} else if (highByte.get(arg2.code)) {
				val2 = "(" + getXRegister(arg2.code) + " DIV 256)";
			} else if (isVariable(arg2.str)) { // db variable
				val2 = arg2.str;
			} else { // const
				int val = toUnsigned(arg2.val, Size.BYTE);
				val2 = Integer.toString(val);
			}

			return getXRegister(arg1.code) + " := (" + getXRegister(arg1.code) + " DIV 256) * 256 + " + val2;

		} else { // db or dw variable

			if (doubleByte.get(arg2.code)) {
				val2 = arg2.str;
			} else if (lowByte.get(arg2.code)) {
				val2 = getXRegister(arg2.code) + " MOD 256";
			} else if (highByte.get(arg2.code)) {
				val2 = getXRegister(arg2.code) + " DIV 256";
			} else if (isVariable(arg2.str)) { // db or dw variable
				val2 = arg2.str;
			} else { // const
				int val = toUnsigned(arg2.val, variableSize.get(getVariableName(arg1.str)));
				val2 = Integer.toString(val);
			}

			return arg1.str + " := " + val2;

		}
	}

	private boolean isVariable(String var) {
		return variableSize.get(getVariableName(var)) != null;
	}

	private String getVariableName(String var) {
		int index = var.indexOf('[');
		if (index != -1) {
			return var.substring(0, index);
		}
		return var;
	}

	/**
	 * Generates code for assembler <strong>inc</strong> instruction which performs incrementation of given argument by
	 * one. Example: <code>inc dest</code> is doing following dest := dest + 1. Possible arguments:
	 * <ul>
	 * <li>reg</li>
	 * <li>mem</li>
	 * </ul>
	 * Inc instruction affects zero, overflow and sign (and some other not important for us) flags. Flags are set in the
	 * same way as for <code>add dest,1</code> except value of carry flag is not changed.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void inc(Token arg) {
		arithmeticInstruction(arg, new Token(number, 1, "1"), Operation.INCREMENTATION);
	}

	/**
	 * Generates code for assembler <strong>dec</strong> instruction which performs decrementation of given argument by
	 * one. Example: <code>dec dest</code> is doing following dest := dest - 1. Possible arguments:
	 * <ul>
	 * <li>reg</li>
	 * <li>mem</li>
	 * </ul>
	 * Dec instruction affects zero, overflow and sign (and some other not important for us) flags. Flags are set in the
	 * same way as for <code>dec dest,1</code> except value of carry flag is not changed.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void dec(Token arg) {
		arithmeticInstruction(arg, new Token(number, 1, "1"), Operation.DECREMENTATION);
	}

	private void neg(Token arg) {
		arithmeticInstruction(new Token(number, 0, "0"), arg, Operation.SUBTRACTION);
	}

	private void OneArgStatement() {
		Token arg = new Token();
		switch (curr.code) {
		case interr:
			check(interr);
			arg = Argument();
			if (arg.code == number && arg.val == 33) { // 33 == 21h
				insert("temp := ax DIV 256;");
				insert("IF temp = 2 THEN");
				insert("PRINT(@ASCII_To_String(dx MOD 256))");
				insert("ELSIF temp = 76 THEN"); // 4c == 76
				insert("CALL Z");
				insert("FI;");
			} else {
				// error
			}
			break;
		case loop:
			check(loop);
			arg = Argument();
			insert("cx := cx - 1;");
			insert("IF cx <> 0 THEN");
			insert("CALL ", arg.str);
			insert("FI;");
			break;
		case push:
			check(push);
			arg = Argument();
			insert("PUSH (stack, temp);");
			setResult(arg);
			break;
		case pop:
			check(pop);
			arg = Argument();
			insert("POP (temp, stack);");
			setResult(arg);
			break;
		case inc:
			check(inc);
			arg = Argument();
			inc(arg);
			break;
		case dec:
			check(dec);
			arg = Argument();
			dec(arg);
			break;
		case call:
			check(call);
			arg = Argument();
			insert(arg.str, "();");
			break;
		case neg:
			check(neg);
			arg = Argument();
			neg(arg);
			break;
		case mul:
			check(mul);
			arg = Argument();
			mul(arg);
			break;
		case div:
			check(div);
			arg = Argument();
			div(arg);
			break;
		case jmp:
			check(jmp);
			arg = Argument();
			insert("CALL ", arg.str, ";");
			break;
		case ja:
			check(ja);
			arg = Argument();
			insert("IF flag_c = 0 AND flag_z = 0 THEN");
			insert("CALL ", arg.str);
			insert("FI;");
			break;
		case jae:
			check(jae);
			arg = Argument();
			insert("IF flag_c = 0 THEN");
			insert("CALL ", arg.str);
			insert("FI;");
			break;
		case jb:
			check(jb);
			arg = Argument();
			insert("IF flag_c = 1 THEN");
			insert("CALL ", arg.str);
			insert("FI;");
			break;
		case jbe:
			check(jbe);
			arg = Argument();
			insert("IF flag_c = 1 AND flag_z = 1 THEN");
			insert("CALL ", arg.str);
			insert("FI;");
			break;
		case jg:
			check(jg);
			arg = Argument();
			insert("IF flag_z = 0 AND flag_s = flag_o THEN");
			insert("CALL ", arg.str);
			insert("FI;");
			break;
		case jge:
			check(jge);
			arg = Argument();
			insert("IF flag_s = flag_o THEN");
			insert("CALL ", arg.str);
			insert("FI;");
			break;
		case jl:
			check(jl);
			arg = Argument();
			insert("IF flag_s <> flag_o THEN");
			insert("CALL ", arg.str);
			insert("FI;");
			break;
		case jle:
			check(jle);
			arg = Argument();
			insert("IF flag_z = 1 OR flag_s <> flag_o THEN");
			insert("CALL ", arg.str);
			insert("FI;");
			break;
		case je:
			check(je);
			arg = Argument();
			insert("IF flag_z = 1 THEN");
			insert("CALL ", arg.str);
			insert("FI;");
			break;
		default:
			throw new IllegalArgumentException("Unsupporeted one argument instruction: " + curr.str);
		}
	}

	private Token Argument() {
		Token ret = new Token();
		if (curr.code == ident) {
			ret = curr;
			check(ident);
			if (curr.code == lbrack) {
				check(lbrack);
				ret.str += "[";
				Token arg;
				while (true) {
					if (curr.code == number) {
						arg = Number();
					} else {
						arg = Register();
					}
					ret.str += arg.str;
					if (curr.code == plus || curr.code == minus) {
						ret.str += curr.str;
						check(plus, minus);
					} else {
						break;
					}
				}
				check(rbrack);
				ret.str += " + 1]";
			}
		} else if (curr.code == number) {
			ret = Number();
		} else if (registers.get(curr.code)) {
			ret = Register();
		} else if (curr.code == atdata) {
			ret = curr;
			check(atdata);
		} else if (curr.code == offset) {
			ret = curr;
			check(offset);
			check(ident);
		} else {
			// error
		}
		return ret;
	}

	private Token Number() {
		Token ret = curr;
		check(number);
		return ret;
	}

	private Token Register() {
		Token ret = new Token();
		switch (curr.code) {
		case ax:
			ret = curr;
			check(ax);
			break;
		case ah:
			ret = curr;
			check(ah);
			break;
		case al:
			ret = curr;
			check(al);
			break;
		case bx:
			ret = curr;
			check(bx);
			break;
		case bh:
			ret = curr;
			check(bh);
			break;
		case bl:
			ret = curr;
			check(bl);
			break;
		case cx:
			ret = curr;
			check(cx);
			break;
		case ch:
			ret = curr;
			check(ch);
			break;
		case cl:
			ret = curr;
			check(cl);
			break;
		case dx:
			ret = curr;
			check(dx);
			break;
		case dh:
			ret = curr;
			check(dh);
			break;
		case dl:
			ret = curr;
			check(dl);
			break;
		case si:
			ret = curr;
			check(si);
			break;
		case di:
			ret = curr;
			check(di);
			break;
		case bp:
			ret = curr;
			check(bp);
			break;
		case sp:
			ret = curr;
			check(sp);
			break;
		case cs:
			ret = curr;
			check(cs);
			break;
		case ds:
			ret = curr;
			check(ds);
			break;
		case ss:
			ret = curr;
			check(ss);
			break;
		case es:
			ret = curr;
			check(es);
			break;
		default:
			throw new IllegalArgumentException("Register expected, but got: " + curr.str);
		}
		return ret;
	}

}
