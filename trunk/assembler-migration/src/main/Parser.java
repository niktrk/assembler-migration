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

	private BitSet oneArgComm, twoArgComm, registers;

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

		buffer = new String();

		curr = sc.next();
		la = sc.next();
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
				+ " flag_p := 0, flag_c := 0, \n"
				+ " ax := 0, ah := 0, al:= 0," + " bx:= 0,  bh:= 0, bl:= 0, \n"
				+ " cx:= 0, ch:= 0, cl:= 0," + " dx:= 0, dh:= 0, dl:= 0, \n"
				+ " si:= 0, di:= 0, bp:= 0, sp:= 0,\n"
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
			while (curr.code == ident && la.code == colon
					|| oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {

				if (curr.code == ident && la.code == colon)
					Label();
				else if (oneArgComm.get(curr.code) || twoArgComm.get(curr.code))
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
		if (curr.code == ident) {
			insIntoProc(curr.str);
			check(ident);
			while (curr.code == ident) {
				insIntoProc(",",curr.str);
				check(ident);
			}
		}
		insIntoProc(") ==\n");
		while (oneArgComm.get(curr.code) || twoArgComm.get(curr.code)) {
			Statement();
		}
		check(endm);
		insIntoProc("END \n");

	}

	private void Procedure() throws Exception {
		insIntoProc("PROC ", curr.str, "()==\n");

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
		insIntoProc("END \n");

	}

	private void Label() throws Exception {
		insIntoBody("CALL ", curr.str, "\nEND\n");
		insIntoBody(curr.str, "==\n");
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

	private void TwoArgStatement() throws Exception {
		switch (curr.code) {
		case mov:
			check(mov);
			break;
		case xchg:
			check(xchg);
			break;
		case cmp:
			check(cmp);
			break;
		case add:
			check(add);
			break;
		case sub:
			check(sub);
			break;
		case mul:
			check(mul);
			break;
		case div:
			check(div);
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
		Argument();
		check(comma);
		Argument();
	}

	private void OneArgStatement() throws Exception {
		switch (curr.code) {
		case interr:
			check(interr);
			break;
		case loop:
			check(loop);
			break;
		case push:
			check(push);
			break;
		case pop:
			check(pop);
			break;
		case inc:
			check(inc);
			break;
		case dec:
			check(dec);
			break;
		case call:
			check(call);
			break;
		case not:
			check(not);
			break;
		case jmp:
			check(jmp);
			break;
		case ja:
			check(ja);
			break;
		case jae:
			check(jae);
			break;
		case jb:
			check(jb);
			break;
		case jbe:
			check(jbe);
			break;
		case jg:
			check(jg);
			break;
		case jge:
			check(jge);
			break;
		case jl:
			check(jl);
			break;
		case jle:
			check(jle);
			break;
		case je:
			check(je);
			break;
		default:
			throw new IllegalArgumentException();
		}
		Argument();
	}

	private void Argument() throws Exception {
		if (curr.code == ident) {
			check(ident);
		} else if (curr.code == number) {
			check(number);
		} else if (registers.get(curr.code)) {
			Register();
		} else if (curr.code == lbrack) {
			check(lbrack);
			Register();
			check(rbrack);
		} else if (curr.code == atdata) {
			check(atdata);
		} else if (curr.code == offset) {
			check(offset);
			check(ident);
		}
	}

	private void Register() throws Exception {
		switch (curr.code) {
		case ax:
			check(ax);
			break;
		case ah:
			check(ah);
			break;
		case al:
			check(al);
			break;
		case bx:
			check(bx);
			break;
		case bh:
			check(bh);
			break;
		case bl:
			check(bl);
			break;
		case cx:
			check(cx);
			break;
		case ch:
			check(ch);
			break;
		case cl:
			check(cl);
			break;
		case dx:
			check(dx);
			break;
		case dh:
			check(dh);
			break;
		case dl:
			check(dl);
			break;
		case si:
			check(si);
			break;
		case di:
			check(di);
			break;
		case bp:
			check(bp);
			break;
		case sp:
			check(sp);
			break;
		case cs:
			check(cs);
			break;
		case ds:
			check(ds);
			break;
		case ss:
			check(ss);
			break;
		case es:
			check(es);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

}
