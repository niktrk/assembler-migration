package main;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Igor i ostali
 * @date 2. 5. 2011.
 */
public class Parser extends AbstractCompiler {

	private Scanner sc;
	private Token curr, la;
	private String buffer;
	private boolean inProc = false;

	private BitSet oneArgComm, twoArgComm, registers, lowByte, highByte, doubleByte, singleByte;
	Map<String, Size> variableSize;
	Map<String, List<String>> macroParams;

	public Parser(Scanner sc) throws Exception {
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
		macroParams = new HashMap<String, List<String>>();

		curr = sc.next();
		la = sc.next();
	}

	private String getXRegister(int code) {
		return str[code].charAt(0) + "x";
	}

	private String getByteRegister(int code) {
		if (lowByte.get(code)) {
			return str[code].charAt(0) + "x MOD 256";
		} else if (highByte.get(code)) {
			return str[code].charAt(0) + "x DIV 256";
		}
		return "";
	}

	private void check(int... code) throws Exception {
		for (int i = 0; i < code.length; i++) {
			if (curr.code == code[i]) {
				curr = la;
				la = sc.next();
				return;
			}
		}
		// error
		System.out.println("Kurac!");
		System.exit(0);
	}

	public String parse() throws Exception {
		String template = "VAR < _decl_ >:\n_body__proc_ENDVAR";
		String declaration = "flag_o := 0, flag_s := 0, flag_z := 0, flag_c := 0, ax := 0, \n"
				+ " bx:= 0, overflow:= 0, cx:= 0, dx:= 0, temp := 0, \n" + " si:= 0, di:= 0, bp:= 0, sp:= 0, \n"
				+ " cs:= 0, ds:= 0, ss:= 0, es:= 0 \n _decl_";

		buffer = template.replace("_decl_", declaration);
		Program();
		buffer = buffer.replace("_decl_", "");
		buffer = buffer.replace("_body_", "");
		buffer = buffer.replace("_proc_", "");
		// temporary fix, I hope :)
		buffer = buffer.replace("; \nEND", "\nEND");
		buffer = buffer.replace(";\n END", "\nEND");
		buffer = buffer.replace("; \n END", "\nEND");
		buffer = buffer.replace(";\nEND", "\nEND");

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
	}

	private void insIntoProc(String... s) {
		for (int i = 0; i < s.length; i++) {
			buffer = buffer.replace("_proc_", s[i] + "_proc_");
		}
	}

	private void insert(String... s) {
		if (inProc) {
			insIntoProc(s);
		} else {
			insIntoBody(s);
		}
	}

	private void Program() throws Exception {
		if (curr.code == title) {
			check(title);
			check(ident);
		}
		check(model);
		check(small, compact, medium, large);

		if (curr.code == stack) {
			check(stack);
			check(number);
			insIntoDecl(", stack :=< >");
		}
		if (curr.code == data) {
			Data();
		}

		Code();

	}

	private void Data() throws Exception {
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

			}
		}

	}

	private void Value(String varName) throws Exception {
		if (curr.code == number) {
			insIntoDecl(Integer.toString(toUnsigned(curr.val, variableSize.get(getVariableName(varName)))));
			check(number);
		} else if (curr.code == string) {
			insIntoDecl("\"");
			insIntoDecl(curr.str);
			insIntoDecl("\"");
			check(string);
		}
	}

	private void Code() throws Exception {
		check(code);

		if (curr.code == ident && (la.code == proc || la.code == macro)) {
			insIntoBody("BEGIN \n");
			insIntoProc("WHERE \n");
			inProc = true;
			while (curr.code == ident && (la.code == proc || la.code == macro)) {
				if (la.code == proc) {
					Procedure();
				} else {
					Macro();
				}
			}
			insIntoProc("END \n");
		}

		if (curr.code == ident && la.code == colon || oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
			insIntoBody("ACTIONS beg: \n");
			insIntoBody("beg== \n");
			inProc = false;
			while (curr.code == ident && la.code == colon || oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {

				if (curr.code == ident && la.code == colon) {
					Label();
				} else if (oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
					Statement();
				}

			}
			insIntoBody("END\n");
			insIntoBody("ENDACTIONS \n");
		}
	}

	private void Macro() throws Exception {
		insIntoProc("PROC ", curr.str, "(");
		String macroName = curr.str;
		List<String> params = new ArrayList<String>();
		check(ident);
		check(macro);
		if (curr.code == ident && la.code != colon) {
			insIntoProc(curr.str);
			params.add(macroName + "." + curr.str);
			check(ident);
			while (curr.code == ident && la.code == comma) {
				check(comma);
				insIntoProc(",", curr.str);
				params.add(macroName + "." + curr.str);
				check(ident);
			}
		}
		macroParams.put(macroName, params);
		insIntoProc(") ==\n");
		insIntoProc("ACTIONS beg: \n");
		insIntoProc("beg== \n");

		while (curr.code == ident && la.code == colon || oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {

			if (curr.code == ident && la.code == colon) {
				Label();
			} else if (oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
				Statement();
			}

		}
		check(endm);
		insIntoProc("END\n");
		insIntoProc("ENDACTIONS \n");
		insIntoProc("END \n");

	}

	private void Procedure() throws Exception {
		insIntoProc("PROC ", curr.str, "()==\n");
		insIntoProc("ACTIONS beg: \n");
		insIntoProc("beg== \n");

		check(ident);
		check(proc);
		if (curr.code == far) {
			check(far);
		}
		while (curr.code == ident || oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
			if (curr.code == ident) {
				Label();
			} else {
				Statement();
			}

		}
		check(ret);
		check(ident);
		check(endp);
		insIntoProc("END\n");
		insIntoProc("ENDACTIONS \n");
		insIntoProc("END \n");

	}

	private void Label() throws Exception {
		insert("CALL ", curr.str, "\nEND\n");
		insert(curr.str, "==\n");
		check(ident);
		check(colon);
	}

	private void Statement() throws Exception {
		if (oneArgComm.get(curr.code)) {
			OneArgStatement();
		} else if (twoArgComm.get(curr.code)) {
			TwoArgStatement();
		} else if (macroParams.get(curr.str) != null) { // macro
			String macroName = curr.str;
			List<String> params = macroParams.get(macroName);
			check(ident);
			insert(macroName, "(");
			for (String param : params) {
				if (singleByte.get(curr.code)) {
					insert(getByteRegister(curr.code));
				} else {
					insert(curr.str);
				}
				check(ident);
				if (curr.code == comma) {
					check(comma);
					insert(", ");
				}
			}
			insert("); \n");
		}
	}

	private Token[] getArguments() throws Exception {
		Token[] ret = new Token[2];
		ret[0] = Argument();
		check(comma);
		ret[1] = Argument();
		return ret;
	}

	private void TwoArgStatement() throws Exception {
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
			arithmeticInstruction(arguments[0], arguments[1], Operation.COMPARE);
			break;
		case add:
			check(add);
			arguments = getArguments();
			arithmeticInstruction(arguments[0], arguments[1], Operation.ADDITION);
			break;
		case sub:
			check(sub);
			arguments = getArguments();
			arithmeticInstruction(arguments[0], arguments[1], Operation.SUBTRACTION);
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
			throw new IllegalArgumentException();
		}
	}

	private int toUnsigned(int num, Size size) {
		if (num >= 0) {
			return num;
		}
		return (1 << size.getSize()) + num;
	}

	private void arithmeticInstruction(Token arg1, Token arg2, Operation operation) {
		String val1, val2;
		Size size;
		if (doubleByte.get(arg1.code)) {
			size = Size.DOUBLE_BYTE;
			val1 = arg1.str;
			insert("temp := ", val1, ";\n");
			insert("temp := temp ", operation.getOperator());
			if (doubleByte.get(arg2.code)) {
				val2 = arg2.str;
				insert(val2, ";\n");
			} else if (isVariable(arg2.str)) {// dw
				val2 = arg2.str;
				insert(val2, ";\n");
			} else { // const
				int val = toUnsigned(arg2.val, Size.DOUBLE_BYTE);
				val2 = Integer.toString(val);
				insert(val2, ";\n");
			}

		} else if (highByte.get(arg1.code)) {
			size = Size.BYTE;
			val1 = getXRegister(arg1.code) + " DIV 256";
			insert("temp := ", val1, ";\n");
			insert("temp := temp", operation.getOperator());
			if (singleByte.get(arg2.code)) {
				val2 = getByteRegister(arg2.code);
				insert("(", val2, ");\n");
			} else if (isVariable(arg2.str)) {// db
				val2 = arg2.str;
				insert(val2, ";\n");
			} else { // const
				int val = toUnsigned(arg2.val, Size.BYTE);
				val2 = Integer.toString(val);
				insert(val2, ";\n");
			}

		} else if (lowByte.get(arg1.code)) {
			size = Size.BYTE;
			val1 = getXRegister(arg1.code) + " MOD 256";
			insert("temp := ", val1, ";\n");
			insert("temp := temp", operation.getOperator());
			if (singleByte.get(arg2.code)) {
				val2 = getByteRegister(arg2.code);
				insert("(", val2, ");\n");
			} else if (isVariable(arg2.str)) {// db
				val2 = arg2.str;
				insert(val2, ";\n");
			} else { // const
				int val = toUnsigned(arg2.val, Size.BYTE);
				val2 = Integer.toString(val);
				insert(val2, ";\n");
			}

		} else { // db or dw variable
			size = variableSize.get(getVariableName(arg1.str));
			val1 = arg1.str;
			insert(arg1.str, " := ");
			insert(val1, operation.getOperator());
			if (doubleByte.get(arg2.code)) {
				val2 = arg2.str;
				insert(val2, ";\n");
			} else if (singleByte.get(arg2.code)) {
				val2 = getByteRegister(arg2.code);
				insert(val2, ";\n");
			} else if (isVariable(arg2.str)) {
				val2 = arg2.str;
				insert(val2, "\n");
			} else {
				int val = toUnsigned(arg2.val, size);
				val2 = Integer.toString(val);
				insert(val2, ";\n");
			}
		}
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
			insert("temp :=  (dx * 65536 + ax) DIV ", arg.str, "; \n");
			insert("IF ", arg.str, " = 0 OR temp >= 65536 THEN \n");
			insert("CALL Z \n ELSE \n");
			insert("dx := (dx * 65536 + ax) MOD ", arg.str, "; \n");
			insert("ax := temp \n FI; \n");
		} else { // arg is byte
			insert("temp := ax DIV ", arg.str, ";\n");
			insert("IF ", arg.str, " = 0 OR temp >= 256 THEN \n");
			insert("CALL Z \n ELSE \n");
			insert("ax := (ax MOD ", arg.str, ") * 256 + temp FI; \n");
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
			insert("ax := temp; \n");
		} else { // double byte
			insert("ax := temp MOD 65536; \n");
			insert("dx := temp DIV 65536; \n");
		}
	}

	private void setMulFlags(Size size) {
		insert("IF temp >= 2**", Integer.toString(size.getSize()), " THEN \n");
		insert("flag_o = 1; \n flag_c = 1 \n");
		insert("ELSE \n");
		insert("flag_o = 0; \n flag_c = 0 \n FI; \n");
	}

	private void setResult(Token arg) {
		if (doubleByte.get(arg.code)) {
			insert(getXRegister(arg.code), " := temp; \n");
		} else if (lowByte.get(arg.code)) {
			insert(getXRegister(arg.code), " := (", getXRegister(arg.code), " DIV 256)*256 + temp; \n");
		} else if (highByte.get(arg.code)) {
			insert(getXRegister(arg.code), ":= (", getXRegister(arg.code), " MOD 256) + temp*256; \n");
		} else {
			insert(arg.str, " := temp; \n");
		}
	}

	private void setSubFlags(String val1, String val2, Size size) {
		generateSubCarryCheck(size);
		generateZeroCheck();
		generateSignCheck(size);
		generateSubOverflowCheck(val1, val2, size);
	}

	private void setDecFlags(String val1, String val2, Size size) {
		insert("temp := temp + (2**", Integer.toString(size.getSize()), "); \n");
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
		insert("temp := temp MOD 2**", Integer.toString(size.getSize()), "; \n");
		generateZeroCheck();
		generateSignCheck(size);
		generateAddOverflowCheck(val1, val2, size);
	}

	private void generateAddOverflowCheck(String val1, String val2, Size size) {
		insert("IF (((", val1, ") DIV 2**", Integer.toString(size.getSize() - 1), ") MOD 2) = ");
		insert("(((", val2, ") DIV 2**", Integer.toString(size.getSize() - 1), ") MOD 2) AND ");
		insert("((temp DIV 2**", Integer.toString(size.getSize() - 1), ") MOD 2) <> ");
		insert("(((", val2, ") DIV 2**", Integer.toString(size.getSize() - 1), ") MOD 2) THEN \n");
		insert("flag_o := 1 \n ELSE \n flag_o := 0 \n FI; \n");
	}

	private void generateSubOverflowCheck(String val1, String val2, Size size) {
		insert("IF (((", val1, ") DIV 2**", Integer.toString(size.getSize() - 1), ") MOD 2) <> ");
		insert("(((", val2, ") DIV 2**", Integer.toString(size.getSize() - 1), ") MOD 2) AND ");
		insert("((temp DIV 2**", Integer.toString(size.getSize() - 1), ") MOD 2) = ");
		insert("(((", val2, ") DIV 2**", Integer.toString(size.getSize() - 1), ") MOD 2) THEN \n");
		insert("flag_o := 1 \n ELSE \n flag_o := 0 \n FI; \n");
	}

	private void generateZeroCheck() {
		insert("IF temp = 0 THEN\n flag_z :=1 \n ELSE \n flag_z :=0; \n FI; \n");
	}

	private void generateSignCheck(Size size) {
		insert("IF (temp DIV 2**", Integer.toString(size.getSize() - 1), ") MOD 2 = 1 \n THEN \n");
		insert("flag_s :=1 \n ELSE \n flag_s :=0 \n FI; \n ");
	}

	private void generateAddCarryCheck(Size size) {
		insert("IF temp >= 2**", Integer.toString(size.getSize()), " THEN \n");
		insert("temp := temp MOD 2**", Integer.toString(size.getSize()), "; \n");
		insert("flag_c := 1 \n");
		insert("ELSE \n flag_c := 0 \n");
		insert("FI; \n");
	}

	private void generateSubCarryCheck(Size size) {
		insert("IF temp < 0 THEN \n");
		insert("temp := temp + (2**", Integer.toString(size.getSize()), "); \n");
		insert("flag_c := 1 \n");
		insert("ELSE \n flag_c := 0 \n");
		insert("FI; \n");
	}

	private void xchg(Token arg1, Token arg2) {
		String arg1Name;
		String arg2Name;
		if (registers.get(arg1.code)) {
			arg1Name = getXRegister(arg1.code);
		} else {
			arg1Name = arg1.str;
		}
		if (registers.get(arg2.code)) {
			arg2Name = getXRegister(arg2.code);
		} else {
			arg2Name = arg2.str;
		}
		insert("< ", arg1Name, " := ", arg2Name, ", ", arg2Name, " := ", arg1Name, " >;\n");
	}

	private void mov(Token arg1, Token arg2) throws Exception {
		if (arg2.code == atdata || arg2.code == offset) {
			return;
		}

		if (doubleByte.get(arg1.code)) {

			insert(arg1.str, " := ");
			if (doubleByte.get(arg2.code)) {
				insert(str[arg2.code], ";\n");
			} else if (isVariable(arg2.str)) {
				// db or dw
				insert(arg2.str, ";\n");
			} else { // dw variable or const
				int val = toUnsigned(arg2.val, Size.DOUBLE_BYTE);
				insert(Integer.toString(val), ";\n");
			}

		} else if (highByte.get(arg1.code)) {

			insert(getXRegister(arg1.code), " := ", "(", getXRegister(arg1.code), " MOD 256) + ");
			if (highByte.get(arg2.code)) {
				insert("(", getXRegister(arg2.code), " DIV 256) * 256;\n");
			} else if (lowByte.get(arg2.code)) {
				insert("(", getXRegister(arg2.code), " MOD 256) * 256;\n");

			} else if (isVariable(arg2.str)) { // db variable
				insert(arg2.str, " * 256; \n ");
			} else { // const
				int val = toUnsigned(arg2.val, Size.BYTE);
				insert(Integer.toString(val), " * 256;\n");
			}

		} else if (lowByte.get(arg1.code)) {

			insert(getXRegister(arg1.code), " := ", "(", getXRegister(arg1.code), " DIV 256) * 256 + ");
			if (lowByte.get(arg2.code)) {
				insert("(", getXRegister(arg2.code), " MOD 256);\n");
			} else if (highByte.get(arg2.code)) {
				insert("(", getXRegister(arg2.code), " DIV 256);\n");
			} else if (isVariable(arg2.str)) {
				insert(arg2.str, ";\n");

			} else { // db variable or const
				int val = toUnsigned(arg2.val, Size.BYTE);
				insert(Integer.toString(val), ";\n");
			}

		} else { // db or dw variable

			insert(arg1.str, " := ");
			if (doubleByte.get(arg2.code)) {
				insert(arg2.str, ";\n");
			} else if (lowByte.get(arg2.code)) {
				insert(getXRegister(arg2.code), " MOD 256;\n");
			} else if (highByte.get(arg2.code)) {
				insert(getXRegister(arg2.code), " DIV 256;\n");
			} else if (isVariable(arg2.str)) { // db or dw
				insert(arg2.str, ";\n");
			} else {
				// const
				int val = toUnsigned(arg2.val, variableSize.get(getVariableName(arg1.str)));
				insert(Integer.toString(val), ";\n");
			}

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

	private void OneArgStatement() throws Exception {
		Token arg = new Token();
		switch (curr.code) {
		case interr:
			check(interr);
			arg = Argument();
			if (arg.code == number && arg.val == 33) { // 33 == 21h
				insert("temp := ax DIV 256; \n");
				insert("IF temp = 2 THEN \n PRINT(@ASCII_To_String(dx MOD 256)) \n");
				insert("ELSIF temp = 76 THEN \n CALL Z \n FI; \n"); // 4c == 76
			}
			break;
		case loop:
			check(loop);
			arg = Argument();
			insert("cx := cx - 1; \n");
			insert("IF cx <> 0 THEN\n CALL ", arg.str, " \n FI; \n");
			break;
		case push:
			check(push);
			arg = Argument();
			insert("PUSH (stack, temp); \n");
			setResult(arg);
			break;
		case pop:
			check(pop);
			arg = Argument();
			insert("POP (temp, stack); \n");
			setResult(arg);
			break;
		case inc:
			check(inc);
			arg = Argument();
			arithmeticInstruction(arg, new Token(number, 1, "1"), Operation.INCREMENTATION);
			break;
		case dec:
			check(dec);
			arg = Argument();
			arithmeticInstruction(arg, new Token(number, 1, "1"), Operation.DECREMENTATION);
			break;
		case call:
			check(call);
			arg = Argument();
			insert(arg.str, "(); \n");
			break;
		case neg:
			check(neg);
			arg = Argument();
			arithmeticInstruction(new Token(number, 0, "0"), arg, Operation.SUBTRACTION);
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
			insert("CALL ", arg.str, "; \n");
			break;
		case ja:
			check(ja);
			arg = Argument();
			insert("IF flag_c = 0 AND flag_z = 0 THEN CALL ", arg.str, " FI; \n");
			break;
		case jae:
			check(jae);
			arg = Argument();
			insert("IF flag_c = 0 THEN CALL ", arg.str, " FI; \n");
			break;
		case jb:
			check(jb);
			arg = Argument();
			insert("IF flag_c = 1 THEN CALL ", arg.str, " FI; \n");
			break;
		case jbe:
			check(jbe);
			arg = Argument();
			insert("IF flag_c = 1 AND flag_z = 1 THEN CALL ", arg.str, " FI; \n");
			break;
		case jg:
			check(jg);
			arg = Argument();
			insert("IF flag_z = 0 AND flag_s = flag_o THEN CALL ", arg.str, " FI; \n");
			break;
		case jge:
			check(jge);
			arg = Argument();
			insert("IF flag_s = flag_o THEN CALL ", arg.str, " FI; \n");
			break;
		case jl:
			check(jl);
			arg = Argument();
			insert("IF flag_s <> flag_o THEN CALL ", arg.str, " FI; \n");
			break;
		case jle:
			check(jle);
			arg = Argument();
			insert("IF flag_z = 1 OR flag_s <> flag_o THEN CALL ", arg.str, " FI; \n");
			break;
		case je:
			check(je);
			arg = Argument();
			insert("IF flag_z = 1 THEN CALL ", arg.str, " FI; \n");
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	private Token Argument() throws Exception {
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
		}
		return ret;
	}

	private Token Number() throws Exception {
		Token ret = curr;
		check(number);
		return ret;
	}

	private Token Register() throws Exception {
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
			throw new IllegalArgumentException();
		}
		return ret;
	}

}
