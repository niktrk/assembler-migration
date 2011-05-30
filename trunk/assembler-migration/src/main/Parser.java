package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

/**
 * Parser and code generation component of the translator.
 * 
 * @author Igor Let
 * @author Nikola Trkulja
 * 
 * @date 02. 05. 2011.
 */
public class Parser extends AbstractCompiler {

	private Scanner sc;
	private Token curr, la;
	private ListIterator<Token> tokenListIterator;
	private CodeBuffer buffer;
	private Token atData;

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

		buffer = new CodeBuffer();

		variableSize = new HashMap<String, Size>();
		macroParams = new HashMap<String, List<Token>>();
		macroTokens = new HashMap<String, List<Token>>();

		curr = nextToken();
		la = nextToken();
	}

	/**
	 * Get code of double byte register for given code of its low or high part. For example, return
	 * value for given code of al or ah is code of ax.
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
	 * Checks if current token is one of expected tokens which are provided as arguments. If that is
	 * the case moves to next token, and if it's not throws exception.
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
		throw new IllegalStateException("Expected to get some of: " + Arrays.toString(code)
				+ " but current token is: " + curr.code);
	}

	/**
	 * Get next token. First checks if there are some injected tokens, if that is the case returns
	 * next token using iterator, and if it's not return next token from scanner.
	 * 
	 * @return
	 */
	private Token nextToken() {
		if (tokenListIterator != null && tokenListIterator.hasNext()) {
			return tokenListIterator.next();
		}
		return sc.next();
	}

	/**
	 * Main parser method used for parsing of input file. Returns string representation of generated
	 * action system.
	 * 
	 * @return
	 */
	public String parse() {
		Program();
		buffer.close();
		return buffer.toString();
	}

	/**
	 * Parse Program. Generate stack if there is stack segment.
	 */
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
			buffer.insertIntoDeclaration(", stack := < >");
		}
		if (curr.code == data) {
			Data();
		}
		Code();
	}

	/**
	 * Parse data. Generate variable and array declarations.
	 */
	private void Data() {
		boolean array;
		check(data);
		String varName;
		while (curr.code == ident) {
			array = false;
			buffer.insertIntoDeclaration(", ", curr.str, " := ");
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
					buffer.insertIntoDeclaration("< ");
					array = true;
				}
				Value(varName);
				while (curr.code == comma) {
					check(comma);
					buffer.insertIntoDeclaration(", ");
					Value(varName);
				}
				if (array) {
					buffer.insertIntoDeclaration(" >");
				}
			} else {
				// error
			}
		}
	}

	/**
	 * Parse value. Insert obtained value to declaration of action system.
	 * 
	 * @param varName
	 */
	private void Value(String varName) {
		if (curr.code == number) {
			buffer.insertIntoDeclaration(Integer.toString(toUnsigned(curr.val, variableSize
					.get(getVariableName(varName)))));
			check(number);
		} else if (curr.code == string) {
			buffer.insertIntoDeclaration("\"");
			buffer.insertIntoDeclaration(curr.str);
			buffer.insertIntoDeclaration("\"");
			check(string);
		} else {
			throw new IllegalArgumentException("Expected to get number or string, but got: "
					+ curr.str);
		}
	}

	/**
	 * Parse code.
	 */
	private void Code() {
		boolean thereIsProc = false;
		check(code);

		if ((curr.code == ident && (la.code == colon || la.code == proc || la.code == macro))
				|| oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {

			buffer.insertIntoBody("ACTIONS beg:");
			buffer.insertIntoBody("beg == ");

			while ((curr.code == ident && (la.code == colon || la.code == proc || la.code == macro))
					|| oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {

				if (curr.code == ident) {
					// label
					if (la.code == colon) {
						Label();
					}
					// procedure
					else if (la.code == proc) {
						if (!thereIsProc) {
							thereIsProc = true;
							buffer.addBegin();
							buffer.insertIntoProcedure("WHERE");
						}
						buffer.setInProc(true);
						Procedure();
						buffer.setInProc(false);
					}
					// macro
					else if (la.code == macro) {
						Macro();
					}
					// if we have macro call, inject macro tokens
					else if (macroParams.get(curr.str) != null) {
						injectMacro();
					}

				} else if (oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
					Statement();
				}

			}
			buffer.insertIntoBody("END");
			buffer.insertIntoBody("ENDACTIONS");

			if (thereIsProc) {
				buffer.insertIntoProcedure("END");
			}
		}

		check(end);
		check(none, ident);
	}

	/**
	 * Parse macro. Read formal parameters of macro and map them with macro name as key. Read macro
	 * tokens and map them with macro name as key.
	 */
	private void Macro() {
		String macroName = curr.str;
		List<Token> currentMacroParams = new ArrayList<Token>();
		check(ident);
		check(macro);
		// FIXME treba proveriti, moguce da kada se na pocetku makroa bez
		// parametara nalazi poziv makroa bez parametara
		// ovo ne radi
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

	/**
	 * Parse procedure. Assembler procedure is translated to WSL procedure.
	 */
	private void Procedure() {
		buffer.insertIntoProcedure("PROC ", curr.str, "() == ");
		buffer.insertIntoProcedure("ACTIONS beg: ");
		buffer.insertIntoProcedure("beg == ");

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
		if (curr.code == ident) {
			check(ident);
		}
		check(endp);
		buffer.insertIntoProcedure("END");
		buffer.insertIntoProcedure("ENDACTIONS");
		buffer.insertIntoProcedure("END");

	}

	/**
	 * Parse label. Generate code for calling new action system.
	 */
	private void Label() {
		buffer.insert("CALL ", curr.str);
		buffer.insert("END");
		buffer.insert(curr.str, " == ");
		check(ident);
		check(colon);
	}

	/**
	 * Method called when we encounter macro call inside procedure, statement or another macro. This
	 * method uses list of formal parameters and macro tokens (retrieved from the maps using macro
	 * name) to generate actual tokens of a macro (where every formal parameter is replaced with
	 * actual parameter). After that tokens curr and la are appended to the end of macro tokens list
	 * and list iterator is set to the beginning of newly formed list.
	 */
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
				check(ident, number, string, ax, ah, al, bx, bh, bl, cx, ch, cl, dx, dh, dl, si,
						di, bp, sp, cs, ds, ss, es);
				if (curr.code == comma) {
					check(comma);
				}
			}

			// iterate through all macro tokens and replace all occurrences of
			// formal parameters with actual parameters
			for (Token token : tokens) {
				// iterate through all formal parameters
				boolean found = false;
				for (int i = 0; i < formalParams.size() && !found; i++) {
					// if current token is formal parameter put actual parameter
					// to list
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

	/**
	 * Parse statement. Only one argument or two argument instruction can follow. Macros can also be
	 * called from statement but their tokens are already injected in this moment.
	 */
	private void Statement() {
		if (oneArgComm.get(curr.code)) {
			OneArgStatement();
		} else if (twoArgComm.get(curr.code)) {
			TwoArgStatement();
		} else {
			throw new IllegalArgumentException(
					"Expected to encounter one or two argument instruction, but got: " + curr.str);
		}
	}

	/**
	 * Get arguments of instructions with two parameters. Returns two element {@link Token} array.
	 * 
	 * @return
	 */
	private Token[] getArguments() {
		Token[] ret = new Token[2];
		ret[0] = Argument();
		check(comma);
		ret[1] = Argument();
		return ret;
	}

	/**
	 * Return unsigned version of given number using size parameter. For example, if size is 8bits
	 * and number is -3 method returns 253 (=256-3). When considering bit representation of -3 and
	 * 253 those two numbers are the same.
	 * 
	 * @param num
	 * @param size
	 * @return
	 */
	private int toUnsigned(int num, Size size) {
		if (num >= 0) {
			return num;
		}
		return (1 << size.getSize()) + num;
	}

	/**
	 * Parse and generate code for two arguments instructions and that are <strong>mov</strong>,
	 * <strong>xchg</strong>, <strong>cmp</strong>, <strong>add</strong>, <strong>sub</strong>.
	 */
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
		default:
			throw new IllegalArgumentException("Unsupported two argument instruction: " + curr.str);
		}
	}

	/**
	 * Generates code for assembler <strong>cmp</strong> instruction which performs subtraction of
	 * given arguments but does not store the result back into the destination operand. Example:
	 * <code>cmp dest,src</code> is doing following dest - src. Possible combinations of arguments:
	 * <ul>
	 * <li>reg - reg</li>
	 * <li>mem - reg</li>
	 * <li>reg - mem</li>
	 * <li>reg - immediate data</li>
	 * <li>mem - immediate data</li>
	 * </ul>
	 * Both arguments must be the same size. Cmp instruction affects zero, carry, overflow and sign
	 * (and some other not important for us) flags.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void cmp(Token arg1, Token arg2) {
		arithmeticInstruction(arg1, arg2, Operation.COMPARE);
	}

	/**
	 * Generates code for assembler <strong>add</strong> instruction which performs addition of
	 * given arguments. Example: <code>add dest,src</code> is doing following dest := dest + src.
	 * Possible combinations of arguments:
	 * <ul>
	 * <li>reg - reg</li>
	 * <li>mem - reg</li>
	 * <li>reg - mem</li>
	 * <li>reg - immediate data</li>
	 * <li>mem - immediate data</li>
	 * </ul>
	 * Both arguments must be the same size. Add instruction affects zero, carry, overflow and sign
	 * (and some other not important for us) flags.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void add(Token arg1, Token arg2) {
		arithmeticInstruction(arg1, arg2, Operation.ADDITION);
	}

	/**
	 * Generates code for assembler <strong>sub</strong> instruction which performs subtraction of
	 * given arguments. Example: <code>sub dest,src</code> is doing following dest := dest - src.
	 * Possible combinations of arguments:
	 * <ul>
	 * <li>reg - reg</li>
	 * <li>mem - reg</li>
	 * <li>reg - mem</li>
	 * <li>reg - immediate data</li>
	 * <li>mem - immediate data</li>
	 * </ul>
	 * Both arguments must be the same size. Sub instruction affects zero, carry, overflow and sign
	 * (and some other not important for us) flags.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void sub(Token arg1, Token arg2) {
		arithmeticInstruction(arg1, arg2, Operation.SUBTRACTION);
	}

	/**
	 * Generic arithmetic instruction code generation method, used for generation of
	 * <strong>add</strong>, <strong>sub</strong>, <strong>inc</strong>, <strong>dec</strong>,
	 * <strong>cmp</strong>, <strong>neg</strong> and <strong>mul</strong>. instruction.
	 * 
	 * @param arg1
	 * @param arg2
	 * @param operation
	 */
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
				throw new IllegalArgumentException(
						"First argument of instruction is 16bit register, "
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
				throw new IllegalArgumentException(
						"First argument of instruction is 8bit (high part of) register, "
								+ "expeted to get 8bit (high or low part of) register, "
								+ "db variable or constant value for second argument, but got: "
								+ arg2.str);
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
				throw new IllegalArgumentException(
						"First argument of instruction is 8bit (low part of) register, "
								+ "expeted to get 8bit (high or low part of) register, "
								+ "db variable or constant value for second argument, but got: "
								+ arg2.str);
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
				throw new IllegalArgumentException(
						"First argument of instruction is db or dw variable, "
								+ "expeted to get register (16bit or 8bit) or constant value for second argument, but got: "
								+ arg2.str);
			}

		} else if (arg1.code == number) { // only if negation is the operation

			if (Operation.NEGATION.equals(operation)) {
				val1 = arg1.str;

				if (doubleByte.get(arg2.code)) {// reg
					val2 = arg2.str;
					size = Size.DOUBLE_BYTE;
				} else if (singleByte.get(arg2.code)) {// reg
					val2 = getByteRegister(arg2.code);
					size = Size.BYTE;
				} else if (isVariable(arg2.str)) {// mem (db or dw)
					val2 = arg2.str;
					size = variableSize.get(getVariableName(arg2.str));
				} else {
					throw new IllegalArgumentException(
							"First argument of instruction is number, "
									+ "expeted to get register (16bit or 8bit), db or dw variable for second argument, but got: "
									+ arg2.str);
				}

			} else {
				throw new IllegalArgumentException(
						"Expected to get negation for operation, but got: " + operation.toString());
			}

		} else {
			throw new IllegalArgumentException(
					"Expeted to get 8bit (high or low part of) register, 16bit register, "
							+ "db or dw variable or constant value for first argument, but got: "
							+ arg1.str);
		}

		buffer.insert("temp := ", val1, ";");
		buffer.insert("temp := temp ", operation.getOperator(), " ", val2, ";");

		switch (operation) {
		case ADDITION:
			setAddFlags(val1, val2, size);
			setResult(arg1);
			break;
		case NEGATION:
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

	/**
	 * Generates code for assembler <strong>div</strong> instruction which performs unsigned
	 * division. Behavior: <code>div arg</code> is doing following al := ax DIV arg, ah := ax MOD
	 * arg if arg is 8bit register or variable; ax := dx:ax DIV arg, dx := dx:ax MOD arg if arg is
	 * 16bit register or variable. Possible arguments:
	 * <ul>
	 * <li>reg</li>
	 * <li>mem</li>
	 * </ul>
	 * Div instruction affects zero, overflow, carry and sign (and some other not important for us)
	 * flags and they are all undefined after div operation.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void div(Token arg) {
		if (doubleByte.get(arg.code)
				|| variableSize.get(getVariableName(arg.str)).equals(Size.DOUBLE_BYTE)) {
			buffer.insert("temp :=  (dx * 65536 + ax) DIV ", arg.str, ";");
			buffer.insert("IF ", arg.str, " = 0 OR temp >= 65536 THEN");
			buffer.insert("CALL Z");
			buffer.insert("ELSE ");
			buffer.insert("dx := (dx * 65536 + ax) MOD ", arg.str, ";");
			buffer.insert("ax := temp");
			buffer.insert("FI;");
		} else if (singleByte.get(arg.code)
				|| variableSize.get(getVariableName(arg.str)).equals(Size.BYTE)) {
			buffer.insert("temp := ax DIV ", arg.str, ";");
			buffer.insert("IF ", arg.str, " = 0 OR temp >= 256 THEN");
			buffer.insert("CALL Z");
			buffer.insert("ELSE");
			buffer.insert("ax := (ax MOD ", arg.str, ") * 256 + temp");
			buffer.insert("FI;");
		} else {
			throw new IllegalArgumentException(
					"Expected to get register (16bit or 8bit) or variable (db or dw), but got: "
							+ arg.str);
		}
	}

	/**
	 * Generates code for assembler <strong>mul</strong> instruction which performs unsigned
	 * multiplication. Behavior: <code>mul arg</code> is doing following ax := al * arg if arg is
	 * 8bit register or variable; dx:ax := ax * arg if arg is 16bit register or variable. Possible
	 * arguments:
	 * <ul>
	 * <li>reg</li>
	 * <li>mem</li>
	 * </ul>
	 * Mul instruction affects zero, overflow, carry and sign (and some other not important for us)
	 * flags. Sign and zero flags do not contain meaningful values after the execution of mul
	 * instructions
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void mul(Token arg) {
		if (doubleByte.get(arg.code)
				|| variableSize.get(getVariableName(arg.str)).equals(Size.DOUBLE_BYTE)) {
			arithmeticInstruction(new Token(ax, 0, str[ax]), arg, Operation.MULTIPLICATION);
		} else if (singleByte.get(arg.code)
				|| variableSize.get(getVariableName(arg.str)).equals(Size.BYTE)) {
			arithmeticInstruction(new Token(al, 0, str[al]), arg, Operation.MULTIPLICATION);
		} else {
			throw new IllegalArgumentException(
					"Expected to get register (16bit or 8bit) or variable (db or dw), but got: "
							+ arg.str);
		}
	}

	/**
	 * Generate set result code after mul instruction.
	 * 
	 * @param size
	 */
	private void setMulResult(Size size) {
		if (size == Size.BYTE) {
			buffer.insert("ax := temp;");
		} else { // double byte
			buffer.insert("ax := temp MOD 65536;");
			buffer.insert("dx := temp DIV 65536;");
		}
	}

	/**
	 * Generate set flag code after mul instruction.
	 * 
	 * @param size
	 */
	private void setMulFlags(Size size) {
		buffer.insert("IF temp >= 2**", Integer.toString(size.getSize()), " THEN");
		buffer.insert("flag_o = 1;");
		buffer.insert("flag_c = 1");
		buffer.insert("ELSE");
		buffer.insert("flag_o = 0;");
		buffer.insert("flag_c = 0");
		buffer.insert("FI;");
	}

	/**
	 * Generate set result code after add,sub,dec or inc instruction.
	 * 
	 * @param arg
	 */
	private void setResult(Token arg) {
		if (doubleByte.get(arg.code)) {
			buffer.insert(arg.str, " := temp;");
		} else if (lowByte.get(arg.code)) {
			buffer.insert(getXRegister(arg.code), " := (", getXRegister(arg.code),
					" DIV 256) * 256 + temp;");
		} else if (highByte.get(arg.code)) {
			buffer.insert(getXRegister(arg.code), " := (", getXRegister(arg.code),
					" MOD 256) + temp * 256;");
		} else if (isVariable(arg.str)) {
			buffer.insert(arg.str, " := temp;");
		} else {
			// error
		}
	}

	/**
	 * Generate set flag code after sub instruction.
	 * 
	 * @param size
	 */
	private void setSubFlags(String val1, String val2, Size size) {
		generateSubCarryCheck(size);
		generateZeroCheck();
		generateSignCheck(size);
		generateSubOverflowCheck(val1, val2, size);
	}

	/**
	 * Generate set flag code after dec instruction.
	 * 
	 * @param size
	 */
	private void setDecFlags(String val1, String val2, Size size) {
		buffer.insert("temp := temp + (2**", Integer.toString(size.getSize()), ");");
		generateZeroCheck();
		generateSignCheck(size);
		generateSubOverflowCheck(val1, val2, size);
	}

	/**
	 * Generate set flag code after add instruction.
	 * 
	 * @param size
	 */
	private void setAddFlags(String val1, String val2, Size size) {
		generateAddCarryCheck(size);
		generateZeroCheck();
		generateSignCheck(size);
		generateAddOverflowCheck(val1, val2, size);
	}

	/**
	 * Generate set flag code after inc instruction.
	 * 
	 * @param size
	 */
	private void setIncFlags(String val1, String val2, Size size) {
		buffer.insert("temp := temp MOD 2**", Integer.toString(size.getSize()), ";");
		generateZeroCheck();
		generateSignCheck(size);
		generateAddOverflowCheck(val1, val2, size);
	}

	/**
	 * Generate set overflow check code after add instruction.
	 * 
	 * @param size
	 */
	private void generateAddOverflowCheck(String val1, String val2, Size size) {
		buffer.insert("IF ", generateGetSignBit(val1, size), " = ", generateGetSignBit(val2, size),
				" AND ", generateGetSignBit("temp", size), " <> ", generateGetSignBit(val2, size),
				" THEN");
		buffer.insert("flag_o := 1");
		buffer.insert("ELSE");
		buffer.insert("flag_o := 0");
		buffer.insert("FI;");
	}

	/**
	 * Generate set overflow check code after sub instruction.
	 * 
	 * @param size
	 */
	private void generateSubOverflowCheck(String val1, String val2, Size size) {
		buffer.insert("IF ", generateGetSignBit(val1, size), " <> ",
				generateGetSignBit(val2, size), " AND ", generateGetSignBit("temp", size), " = ",
				generateGetSignBit(val2, size), " THEN");
		buffer.insert("flag_o := 1");
		buffer.insert("ELSE");
		buffer.insert("flag_o := 0");
		buffer.insert("FI;");
	}

	/**
	 * Generate get sign bit code of given value with specified size.
	 * 
	 * @param size
	 */
	private String generateGetSignBit(String val, Size size) {
		return "(((" + val + ") DIV 2**" + Integer.toString(size.getSize() - 1) + ") MOD 2)";
	}

	/**
	 * Generate zero check code.
	 * 
	 * @param size
	 */
	private void generateZeroCheck() {
		buffer.insert("IF temp = 0 THEN");
		buffer.insert("flag_z := 1");
		buffer.insert("ELSE");
		buffer.insert("flag_z := 0");
		buffer.insert("FI;");
	}

	/**
	 * Generate sign check code.
	 * 
	 * @param size
	 */
	private void generateSignCheck(Size size) {
		buffer.insert("IF ", generateGetSignBit("temp", size), " = 1 THEN");
		buffer.insert("flag_s := 1");
		buffer.insert("ELSE");
		buffer.insert("flag_s := 0");
		buffer.insert("FI;");
	}

	/**
	 * Generate carry check after add instruction.
	 * 
	 * @param size
	 */
	private void generateAddCarryCheck(Size size) {
		buffer.insert("IF temp >= 2**", Integer.toString(size.getSize()), " THEN");
		buffer.insert("temp := temp MOD 2**", Integer.toString(size.getSize()), ";");
		buffer.insert("flag_c := 1");
		buffer.insert("ELSE");
		buffer.insert("flag_c := 0");
		buffer.insert("FI;");
	}

	/**
	 * Generate carry check after sub instruction.
	 * 
	 * @param size
	 */
	private void generateSubCarryCheck(Size size) {
		buffer.insert("IF temp < 0 THEN");
		buffer.insert("temp := temp + (2**", Integer.toString(size.getSize()), ");");
		buffer.insert("flag_c := 1");
		buffer.insert("ELSE");
		buffer.insert("flag_c := 0");
		buffer.insert("FI;");
	}

	/**
	 * Generates code for <strong>xchg</strong> assembler instruction which exchanges values of
	 * given arguments. Possible combinations of arguments:
	 * <ul>
	 * <li>reg - reg</li>
	 * <li>mem - reg</li>
	 * <li>reg - mem</li>
	 * </ul>
	 * Mov instruction does not affect any flag. Both locations must be of the same size.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void xchg(Token arg1, Token arg2) {
		buffer.insert("< ", generateMov(arg1, arg2), ", ", generateMov(arg2, arg1), " >;");
	}

	/**
	 * Generates code for assembler <strong>mov</strong> instruction which performs assignment.
	 * Example: <code>mov dest,src</code> is doing following dest := src. Possible arguments:
	 * <ul>
	 * <li>reg - reg</li>
	 * <li>mem - reg</li>
	 * <li>reg - mem</li>
	 * <li>reg - immediate data</li>
	 * <li>mem - immediate data</li>
	 * </ul>
	 * Mov instruction does not affect any flag. Both locations must be of the same size.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void mov(Token arg1, Token arg2) {
		if (arg2.code == offset) {// FIXME sta cemo sa ovim offsetom?
			return;
		} else if (arg2.code == atdata) { // simulate value returned by @data with random int
			// generator

			if (atData == null) {
				Size size;
				if (doubleByte.get(arg1.code)) {
					size = Size.DOUBLE_BYTE;
				} else if (singleByte.get(arg1.code)) {
					size = Size.BYTE;
				} else {
					size = variableSize.get(getVariableName(arg1.str));
				}
				Integer val = new Random().nextInt(1 << size.getSize());
				atData = new Token(number, val, val.toString());
			}

			arg2 = atData;
		}
		buffer.insert(generateMov(arg1, arg2), ";");
	}

	/**
	 * Generic assignment code generation method, used for generation of <strong>mov</strong> and
	 * <strong>xchg</strong> instruction.
	 * 
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	private String generateMov(Token arg1, Token arg2) {
		String val2;
		if (doubleByte.get(arg1.code)) { // reg

			if (doubleByte.get(arg2.code)) { // reg
				val2 = str[arg2.code];
			} else if (isVariable(arg2.str)) { // mem (dw)
				val2 = arg2.str;
			} else if (arg2.code == number) { // immediate (const)
				int val = toUnsigned(arg2.val, Size.DOUBLE_BYTE);
				val2 = Integer.toString(val);
			} else {
				throw new IllegalArgumentException(
						"First argument of instruction is 16bit register, "
								+ "expeted to get 16bit register, dw variable or constant value for second argument, but got: "
								+ arg2.str);
			}

			return arg1.str + " := " + val2;

		} else if (highByte.get(arg1.code)) { // reg

			if (highByte.get(arg2.code)) { // reg
				val2 = "(" + getXRegister(arg2.code) + " DIV 256) * 256";
			} else if (lowByte.get(arg2.code)) { // reg
				val2 = "(" + getXRegister(arg2.code) + " MOD 256) * 256";
			} else if (isVariable(arg2.str)) { // mem (db)
				val2 = arg2.str + " * 256";
			} else if (arg2.code == number) { // immediate (const)
				int val = toUnsigned(arg2.val, Size.BYTE);
				val2 = Integer.toString(val) + " * 256";
			} else {
				throw new IllegalArgumentException(
						"First argument of instruction is 8bit (high part of) register, "
								+ "expeted to get 8bit (high or low part of) register, "
								+ "db variable or constant value for second argument, but got: "
								+ arg2.str);
			}

			return getXRegister(arg1.code) + " := (" + getXRegister(arg1.code) + " MOD 256) + "
					+ val2;

		} else if (lowByte.get(arg1.code)) { // reg

			if (lowByte.get(arg2.code)) { // reg
				val2 = "(" + getXRegister(arg2.code) + " MOD 256)";
			} else if (highByte.get(arg2.code)) { // reg
				val2 = "(" + getXRegister(arg2.code) + " DIV 256)";
			} else if (isVariable(arg2.str)) { // mem (db)
				val2 = arg2.str;
			} else if (arg2.code == number) { // immediate (const)
				int val = toUnsigned(arg2.val, Size.BYTE);
				val2 = Integer.toString(val);
			} else {
				throw new IllegalArgumentException(
						"First argument of instruction is 8bit (low part of) register, "
								+ "expeted to get 8bit (high or low part of) register, "
								+ "db variable or constant value for second argument, but got: "
								+ arg2.str);
			}

			return getXRegister(arg1.code) + " := (" + getXRegister(arg1.code)
					+ " DIV 256) * 256 + " + val2;

		} else if (isVariable(arg1.str)) { // mem (db or dw)

			if (doubleByte.get(arg2.code)) { // reg
				val2 = arg2.str;
			} else if (lowByte.get(arg2.code)) { // reg
				val2 = getXRegister(arg2.code) + " MOD 256";
			} else if (highByte.get(arg2.code)) { // reg
				val2 = getXRegister(arg2.code) + " DIV 256";
			} else if (arg2.code == number) { // immediate (const)
				int val = toUnsigned(arg2.val, variableSize.get(getVariableName(arg1.str)));
				val2 = Integer.toString(val);
			} else {
				throw new IllegalArgumentException(
						"First argument of instruction is db or dw variable, "
								+ "expeted to get register (16bit or 8bit) or constant value for second argument, but got: "
								+ arg2.str);
			}

			return arg1.str + " := " + val2;

		} else {
			throw new IllegalArgumentException(
					"Expeted to get 8bit (high or low part of) register, 16bit register, "
							+ "db or dw variable for first argument, but got: " + arg1.str);
		}
	}

	/**
	 * Check if given string provided as argument is name of variable.
	 * 
	 * @param var
	 * @return
	 */
	private boolean isVariable(String var) {
		return variableSize.get(getVariableName(var)) != null;
	}

	/**
	 * Get variable name. Needed when we have string like this <code>array[bp+si+1]</code> two get
	 * only "array" which is name of variable.
	 * 
	 * @param var
	 * @return
	 */
	private String getVariableName(String var) {
		int index = var.indexOf('[');
		if (index != -1) {
			return var.substring(0, index);
		}
		return var;
	}

	/**
	 * Generates code for assembler <strong>inc</strong> instruction which performs incrementation
	 * of given argument by one. Example: <code>inc dest</code> is doing following dest := dest + 1.
	 * Possible arguments:
	 * <ul>
	 * <li>reg</li>
	 * <li>mem</li>
	 * </ul>
	 * Inc instruction affects zero, overflow and sign (and some other not important for us) flags.
	 * Flags are set in the same way as for <code>add dest,1</code> except value of carry flag is
	 * not changed.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void inc(Token arg) {
		arithmeticInstruction(arg, new Token(number, 1, "1"), Operation.INCREMENTATION);
	}

	/**
	 * Generates code for assembler <strong>dec</strong> instruction which performs decrementation
	 * of given argument by one. Example: <code>dec dest</code> is doing following dest := dest - 1.
	 * Possible arguments:
	 * <ul>
	 * <li>reg</li>
	 * <li>mem</li>
	 * </ul>
	 * Dec instruction affects zero, overflow and sign (and some other not important for us) flags.
	 * Flags are set in the same way as for <code>sub dest,1</code> except value of carry flag is
	 * not changed.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void dec(Token arg) {
		arithmeticInstruction(arg, new Token(number, 1, "1"), Operation.DECREMENTATION);
	}

	/**
	 * Generates code for assembler <strong>neg</strong> instruction which performs negation of
	 * given argument. Example: <code>neg dest</code> is doing following dest := 0 - dest. Possible
	 * arguments:
	 * <ul>
	 * <li>reg</li>
	 * <li>mem</li>
	 * </ul>
	 * Neg instruction affects zero, overflow, carry and sign (and some other not important for us)
	 * flags. Flags are set in the same way as for <code>sub 0,dest</code>.
	 * 
	 * @param arg1
	 * @param arg2
	 */
	private void neg(Token arg) {
		arithmeticInstruction(new Token(number, 0, "0"), arg, Operation.NEGATION);
	}

	/**
	 * Parse and generate code for one argument instructions and that are <strong>int</strong>,
	 * <strong>loop</strong>, <strong>push</strong>, <strong>pop</strong>, <strong>inc</strong>,
	 * <strong>dec</strong>, <strong>call</strong>, <strong>neg</strong>, <strong>mul</strong>,
	 * <strong>div</strong>, <strong>jmp</strong>, <strong>ja</strong>, <strong>jae</strong>,
	 * <strong>jb</strong>, <strong>jbe</strong>, <strong>jg</strong>, <strong>jge</strong>,
	 * <strong>jl</strong>, <strong>jle</strong>, <strong>je</strong>.
	 */
	private void OneArgStatement() {
		Token arg = new Token();
		switch (curr.code) {
		case interr:
			check(interr);
			arg = Argument();
			if (arg.code == number && arg.val == 33) { // 33 == 21h
				buffer.insert("temp := ax DIV 256;");
				buffer.insert("IF temp = 2 THEN");
				buffer.insert("PRINT(@ASCII_To_String(dx MOD 256))");
				buffer.insert("ELSIF temp = 76 THEN"); // 4c == 76
				buffer.insert("CALL Z");
				buffer.insert("FI;");
			} else {
				// error
			}
			break;
		case loop:
			check(loop);
			arg = Argument();
			buffer.insert("cx := cx - 1;");
			buffer.insert("IF cx <> 0 THEN");
			buffer.insert("CALL ", arg.str);
			buffer.insert("FI;");
			break;
		case push:
			check(push);
			arg = Argument();
			if (doubleByte.get(arg.code)) {
				buffer.insert("temp := ", arg.str, ";" );
			} else if (lowByte.get(arg.code)) {
				buffer.insert("temp := ", getXRegister(arg.code), " MOD 256;");
			} else if (highByte.get(arg.code)) {
				buffer.insert("temp := ", getXRegister(arg.code), " DIV 256;");
			} else if (isVariable(arg.str)) {
				buffer.insert("temp := ", arg.str, ";");
			} else {
				// error
			}
			buffer.insert("PUSH (stack, temp);");
			break;
		case pop:
			check(pop);
			arg = Argument();
			buffer.insert("POP (temp, stack);");
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
			buffer.insert(arg.str, "();");
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
			buffer.insert("CALL ", arg.str, ";");
			break;
		case ja:
			check(ja);
			arg = Argument();
			buffer.insert("IF flag_c = 0 AND flag_z = 0 THEN");
			buffer.insert("CALL ", arg.str);
			buffer.insert("FI;");
			break;
		case jae:
			check(jae);
			arg = Argument();
			buffer.insert("IF flag_c = 0 THEN");
			buffer.insert("CALL ", arg.str);
			buffer.insert("FI;");
			break;
		case jb:
			check(jb);
			arg = Argument();
			buffer.insert("IF flag_c = 1 THEN");
			buffer.insert("CALL ", arg.str);
			buffer.insert("FI;");
			break;
		case jbe:
			check(jbe);
			arg = Argument();
			buffer.insert("IF flag_c = 1 AND flag_z = 1 THEN");
			buffer.insert("CALL ", arg.str);
			buffer.insert("FI;");
			break;
		case jg:
			check(jg);
			arg = Argument();
			buffer.insert("IF flag_z = 0 AND flag_s = flag_o THEN");
			buffer.insert("CALL ", arg.str);
			buffer.insert("FI;");
			break;
		case jge:
			check(jge);
			arg = Argument();
			buffer.insert("IF flag_s = flag_o THEN");
			buffer.insert("CALL ", arg.str);
			buffer.insert("FI;");
			break;
		case jl:
			check(jl);
			arg = Argument();
			buffer.insert("IF flag_s <> flag_o THEN");
			buffer.insert("CALL ", arg.str);
			buffer.insert("FI;");
			break;
		case jle:
			check(jle);
			arg = Argument();
			buffer.insert("IF flag_z = 1 OR flag_s <> flag_o THEN");
			buffer.insert("CALL ", arg.str);
			buffer.insert("FI;");
			break;
		case je:
			check(je);
			arg = Argument();
			buffer.insert("IF flag_z = 1 THEN");
			buffer.insert("CALL ", arg.str);
			buffer.insert("FI;");
			break;
		default:
			throw new IllegalArgumentException("Unsupporeted one argument instruction: " + curr.str);
		}
	}

	/**
	 * Parse arguemnt and return its token.
	 * 
	 * @return
	 */
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

	/**
	 * Parse number and return number token.
	 * 
	 * @return
	 */
	private Token Number() {
		Token ret = curr;
		check(number);
		return ret;
	}

	/**
	 * Parse register and return register token.
	 * 
	 * @return
	 */
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
