package main;

import java.util.BitSet;

/**
 * @author Igor i ostali
 * @date 2. 5. 2011.
 */
public class Parser extends AbstractCompiler {

	private Scanner sc;
	private Token curr, la;
	private String buffer;
	private boolean inProc = false;

	private BitSet oneArgComm, twoArgComm, registers, lowByte, highByte,
			doubleByte, singleByte;

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
		oneArgComm.set(not);
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

		twoArgComm = new BitSet();
		twoArgComm.set(mov);
		twoArgComm.set(xchg);
		twoArgComm.set(cmp);
		twoArgComm.set(add);
		twoArgComm.set(sub);
		twoArgComm.set(mul);
		twoArgComm.set(div);
		twoArgComm.set(and);
		twoArgComm.set(or);
		twoArgComm.set(xor);

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

		curr = sc.next();
		la = sc.next();
	}

	private String getXRegister(int code) {
		return str[code].charAt(0) + "x";
	}

	private String getByteRegister(int code) {
		if (lowByte.get(code)) {
			return str[code].charAt(0) + "x MOD 256";
		}
		if (highByte.get(code)) {
			return str[code].charAt(0) + "x DIV 256";
		}
		return "";
	}

	private void check(int... code) throws Exception {
		boolean in = false;
		for (int i = 0; i < code.length; i++) {
			if (curr.code == code[i])
				in = true;
		}
		if (in) {
			curr = la;
			la = sc.next();
		} else {
			System.out.println("Kurac!");
			System.exit(0);
		}
	}

	public String parse() throws Exception {
		String template = "VAR < _decl_ >:\n_body__proc_ENDVAR";
		String declaration = "flag_o := 0, flag_s := 0, flag_z := 0, \n"
				+ " flag_c := 0, \n" + " ax := 0, "
				+ " bx:= 0, overflow:= 0, \n" + " cx:= 0,"
				+ " dx:= 0, temp := 0, \n"
				+ " si:= 0, di:= 0, bp:= 0, sp:= 0, \n"
				+ " cs:= 0, ds:= 0, ss:= 0," + " es:= 0 \n _decl_";

		buffer = template.replace("_decl_", declaration);
		Program();
		buffer = buffer.replace("_decl_", "");
		buffer = buffer.replace("_body_", "");
		buffer = buffer.replace("_proc_", "");

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
		} else
			insIntoBody(s);
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
		boolean array = false;
		check(data);
		while (curr.code == ident) {
			insIntoDecl(", ", curr.str, " := ");
			check(ident);
			check(db, dw);
			if (curr.code == number || curr.code == string) {
				if (la.code == comma) {
					insIntoDecl("< ");
					array = true;
				}
				Value();
				while (curr.code == comma) {
					check(comma);
					insIntoDecl(", ");
					Value();
				}
				if (array)
					insIntoDecl(" >");

			}
		}

	}

	private void Value() throws Exception {
		if (curr.code == number) {
			insIntoDecl(Integer.toString(curr.val));
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
				if (la.code == proc)
					Procedure();
				else
					Macro();
			}
			insIntoProc("END \n");
		}

		if (curr.code == ident && la.code == colon || oneArgComm.get(curr.code)
				|| twoArgComm.get(curr.code)) {
			insIntoBody("ACTIONS beg: \n");
			insIntoBody("beg== \n");
			inProc = false;
			while (curr.code == ident && la.code == colon
					|| oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {

				if (curr.code == ident && la.code == colon) {
					Label();
				} else if (oneArgComm.get(curr.code)
						|| twoArgComm.get(curr.code))
					Statement();

			}
			insIntoBody("END\n");
			insIntoBody("ENDACTIONS \n");
		}
	}

	private void Macro() throws Exception {
		insIntoProc("PROC ", curr.str, "(");
		check(ident);
		check(macro);
		if (curr.code == ident && la.code != colon) {
			insIntoProc(curr.str);
			check(ident);
			while (curr.code == ident && la.code != colon) {
				insIntoProc(",", curr.str);
				check(ident);
			}
		}
		insIntoProc(") ==\n");
		insIntoProc("ACTIONS beg: \n");
		insIntoProc("beg== \n");

		while (curr.code == ident && la.code == colon
				|| oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {

			if (curr.code == ident && la.code == colon) {
				Label();
			} else if (oneArgComm.get(curr.code) || twoArgComm.get(curr.code))
				Statement();

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
		while (curr.code == ident || oneArgComm.get(curr.code)
				|| twoArgComm.get(curr.code)) {
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
		} else {
			TwoArgStatement();
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
		case mul:
			check(mul);
			arguments = getArguments();
			mul(arguments[0], arguments[1]);
			break;
		case div:
			check(div);
			arguments = getArguments();
			div(arguments[0], arguments[1]);
			break;
		case and:
			check(and);
			break;
		case or:
			check(or);
			break;
		case xor:
			check(xor);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	private void cmp(Token arg1, Token arg2) {
		arithmInstruct(arg1, arg2, "-");
	}

	private void add(Token arg1, Token arg2) {
		arithmInstruct(arg1, arg2, "+");
		setResult(arg1);
	}

	private void sub(Token arg1, Token arg2) {
		arithmInstruct(arg1, arg2, "-");
		setResult(arg1);
	}

	private void mul(Token arg1, Token arg2) {
		arithmInstruct(arg1, arg2, "*");
		setResult(arg1);
	}

	private void div(Token arg1, Token arg2) {
		arithmInstruct(arg1, arg2, "/");
		setResult(arg1);
	}

	private void arithmInstruct(Token arg1, Token arg2, String op) {
		op = " " + op + " ";
		if (doubleByte.get(arg1.code)) {
			insert("temp := ", arg1.str, ";\n");
			insert("temp := temp", op);
			if (doubleByte.get(arg2.code)) {
				insert(arg2.str, ";\n");
			} else { // dw variable or const
				insert(arg2.str, ";\n");
			}
			setFlags(arg1);

		} else if (highByte.get(arg1.code)) {
			insert("temp := ", getXRegister(arg1.code), " DIV 256;\n");
			insert("temp := temp", op);
			if (singleByte.get(arg2.code)) {
				insert("(", getByteRegister(arg2.code), ");\n");
			} else { // db variable or const
				insert(arg2.str, ";\n");
			}
			setFlags(arg1);

		} else if (lowByte.get(arg1.code)) {
			insert("temp := ", getXRegister(arg1.code), " MOD 256;\n");
			insert("temp := temp", op);
			if (singleByte.get(arg2.code)) {
				insert("(", getByteRegister(arg2.code), ");\n");
			} else { // db variable or const
				insert(arg2.str, ";\n");
			}
			setFlags(arg1);

		} else { // db or dw variable
			insert(arg1.str, " := ");
			insert(arg1.str, op);
			if (doubleByte.get(arg2.code)) {
				insert(arg2.str, ";\n");
			} else if (singleByte.get(arg2.code)) {
				insert(getByteRegister(arg2.code), ";\n");
			} else { // db or dw variable or const
				insert(arg2.str, ";\n");
			}
			setFlags(arg1);
		}
	}
	
	private void setResult(Token arg){
		if (doubleByte.get(arg.code)) {
			insert(getXRegister(arg.code), " := temp \n");
		} else if (lowByte.get(arg.code)) {
			insert(getXRegister(arg.code), " := (", getXRegister(arg.code),
					" DIV 256)*256 + temp \n");
		} else if (highByte.get(arg.code)) {
			insert(getXRegister(arg.code), ":= (", getXRegister(arg.code),
					" MOD 256) + temp*256 \n");
		}
	}

	private void generateOverflowCheck(Token arg) {
		String overflow;
		if(doubleByte.get(arg.code)){
			 overflow = "65536";
		}
		else{
			overflow = "256";
		}
		insert("overflow := ", overflow, ";\n");
		insert("IF  temp  >= ", overflow, " THEN\n temp :=  temp", " MOD ",
				overflow, ";\n ");
		insert("flag_o :=1;\n flag_c := 1\nELSE\n flag_o :=0;\n flag_c := 0;\nFI;\n");

	}

	private void generateZeroCheck() {
		insert("IF temp = 0 THEN\n flag_z :=1\nELSE\n flag_z :=0;\nFI;\n");
	}

	private void generateSignCheck() {
		insert("IF temp < 0 THEN\n flag_s :=1\nELSE\n flag_s :=0;\nFI;\n");
	}

	private void setFlags(Token arg) {
		generateOverflowCheck(arg);
		generateZeroCheck();
		generateSignCheck();
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
		insert("< ", arg1Name, " := ", arg2Name, ", ", arg2Name, " := ",
				arg1Name, " >;\n");
	}

	private void mov(Token arg1, Token arg2) throws Exception {
		if (arg2.code == atdata && arg2.code == offset) {
			return;
		}

		if (doubleByte.get(arg1.code)) {

			insert(arg1.str, " := ");
			if (doubleByte.get(arg2.code)) {
				insert(str[arg2.code], ";\n");
			} else { // dw variable or const
				insert(arg2.str, ";\n");
			}

		} else if (highByte.get(arg1.code)) {

			insert(getXRegister(arg1.code), " := ", "(",
					getXRegister(arg1.code), " MOD 256) + ");
			if (highByte.get(arg2.code)) {
				insert("(", getXRegister(arg2.code), " DIV 256) * 256;\n");
			} else if (lowByte.get(arg2.code)) {
				insert("(", getXRegister(arg2.code), " MOD 256) * 256;\n");
			} else { // db variable or const
				insert(arg2.str, " * 256;\n");
			}

		} else if (lowByte.get(arg1.code)) {

			insert(getXRegister(arg1.code), " := ", "(",
					getXRegister(arg1.code), " DIV 256) * 256 + ");
			if (lowByte.get(arg2.code)) {
				insert("(", getXRegister(arg2.code), " MOD 256);\n");
			} else if (highByte.get(arg2.code)) {
				insert("(", getXRegister(arg2.code), " DIV 256);\n");
			} else { // db variable or const
				insert(arg2.str, ";\n");
			}

		} else { // db or dw variable

			insert(arg1.str, " := ");
			if (doubleByte.get(arg2.code)) {
				insert(arg2.str, ";\n");
			} else if (lowByte.get(arg2.code)) {
				insert(getXRegister(arg2.code), " MOD 256;\n");
			} else if (highByte.get(arg2.code)) {
				insert(getXRegister(arg2.code), " DIV 256;\n");
			} else { // db or dw variable or const
				insert(arg2.str, ";\n");
			}

		}
	}

	private void OneArgStatement() throws Exception {
		Token arg = new Token();
		switch (curr.code) {
		case interr:
			check(interr);
			arg = Argument();
			if(arg.code == number && arg.val == 21 && arg.hex){
				insert("temp := ax DIV 256; \n");
				insert("IF temp = 1 THEN PRINT ");
				//NIJE GOTOVO NI BLIZU!
			}
			break;
		case loop:
			check(loop);
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
			insert("temp := ", arg.str, "; \n");
			insert("temp := temp + 1; \n");
			setFlags(arg);
			setResult(arg);
			break;
		case dec:
			check(dec);
			arg = Argument();
			insert("temp := ", arg.str, "; \n");
			insert("temp := temp - 1; \n");
			setFlags(arg);
			setResult(arg);
			break;
		case call:
			check(call);
			arg = Argument();
			insert(arg.str, "(); \n");
			break;
		case not:
			check(not);
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
		} else if (curr.code == number) {
			ret = curr;
			check(number);
		} else if (registers.get(curr.code)) {
			ret = Register();
		} else if (curr.code == lbrack) {
			check(lbrack);
			ret = Register();
			check(rbrack);
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
