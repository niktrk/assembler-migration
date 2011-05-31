package main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Scanner component of the translator.
 * 
 * @author Igor Let
 * @author Nikola Trkulja
 */
public class Scanner extends AbstractCompiler {

	private BufferedReader sc;
	private char chr;
	private boolean eof;
	private int line;

	public Scanner(String file) {
		eof = false;
		line = 1;
		try {
			sc = new BufferedReader(new FileReader(file));
			read();
		} catch (FileNotFoundException e) {
			System.out.println("File " + file + " doesn't exist.");
			System.exit(0);
		}
	}

	/**
	 * Read next character.
	 */
	private void read() {
		try {
			int i = sc.read();
			if (i == -1) {
				eof = true;
			} else {
				chr = (char) i;
				if (chr == '\n') {
					line++;
				}
			}
		} catch (IOException ioe) {
			System.out.println("Error occurred while trying to read input file.");
			System.exit(0);
		}
	}

	/**
	 * Read string.
	 * 
	 * @return
	 */
	private Token readString() {
		String str = "";
		read();
		while (chr != '\'') {
			str += chr;
			read();
		}
		read();
		return new Token(string, 0, str, line);
	}

	/**
	 * Read name;
	 * 
	 * @return
	 */
	private String readName() {
		String ret = "";
		while ((chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z' || chr >= '0' && chr <= '9')
				&& !eof) {
			ret += chr;
			read();
		}
		return ret.toLowerCase();
	}

	/**
	 * Read decimal or hexadecimal number.
	 * 
	 * @return
	 */
	private Token readNumber() {
		String num = "";
		int radix = 10;
		while ((chr >= '0' && chr <= '9') || (chr >= 'a' && chr <= 'f')
				|| (chr >= 'A' && chr <= 'F') && !eof) {
			num += chr;
			read();
		}
		if (chr == 'h' || chr == 'H') {
			radix = 16;
			read();
		}
		Integer value = Integer.parseInt(num, radix);
		return new Token(number, value.intValue(), value.toString(), line);
	}

	/**
	 * Read (skip) comment.
	 */
	private void readComment() {
		while (chr != '\n' && !eof) {
			read();
		}
		if (!eof) {
			read();
		}
	}

	/**
	 * Return next token.
	 * 
	 * @return
	 */
	public Token next() {
		while (chr <= ' ' && !eof) {
			read();
		}
		if (chr == ';') {
			readComment();
		}
		Token ret = new Token(none, -1, "", line);
		if (!eof) {
			String str = "";
			if (chr >= '0' && chr <= '9') {
				return readNumber();
			} else if (chr == ',') {
				ret.code = comma;
				read();
			} else if (chr == ':') {
				ret.code = colon;
				read();
			} else if (chr == '[') {
				ret.code = lbrack;
				read();
			} else if (chr == ']') {
				ret.code = rbrack;
				read();
			} else if (chr == '\'') {
				return readString();
			} else if (chr == '+') {
				ret.code = plus;
				read();
			} else if (chr == '-') {
				ret.code = minus;
				read();
			} else if (chr == '?') {
				ret.code = quest;
				read();
			} else if (chr == '.') {
				read();
				str = readName();
				if (str.equals("model")) {
					ret.code = model;
				} else if (str.equals("stack")) {
					ret.code = stack;
				}
				if (str.equals("data")) {
					ret.code = data;
				}
				if (str.equals("code")) {
					ret.code = code;
				}
			} else if (chr == '@') {
				read();
				if (readName().equals("data")) {
					ret.code = atdata;
				}
			} else if (chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z') {
				str = readName();
				if (str.equals("title")) {
					ret.code = title;
				} else if (str.equals("title")) {
					ret.code = title;
				} else if (str.equals("small")) {
					ret.code = small;
				} else if (str.equals("compact")) {
					ret.code = compact;
				} else if (str.equals("medium")) {
					ret.code = medium;
				} else if (str.equals("large")) {
					ret.code = large;
				} else if (str.equals("db")) {
					ret.code = db;
				} else if (str.equals("dw")) {
					ret.code = dw;
				} else if (str.equals("end")) {
					ret.code = end;
				} else if (str.equals("proc")) {
					ret.code = proc;
				} else if (str.equals("far")) {
					ret.code = far;
				} else if (str.equals("ret")) {
					ret.code = Scanner.ret;
				} else if (str.equals("endp")) {
					ret.code = endp;
				} else if (str.equals("macro")) {
					ret.code = macro;
				} else if (str.equals("endm")) {
					ret.code = endm;
				} else if (str.equals("int")) {
					ret.code = interr;
				} else if (str.equals("loop")) {
					ret.code = loop;
				} else if (str.equals("push")) {
					ret.code = push;
				} else if (str.equals("pop")) {
					ret.code = pop;
				} else if (str.equals("inc")) {
					ret.code = inc;
				} else if (str.equals("dec")) {
					ret.code = dec;
				} else if (str.equals("call")) {
					ret.code = call;
				} else if (str.equals("jmp")) {
					ret.code = jmp;
				} else if (str.equals("ja")) {
					ret.code = ja;
				} else if (str.equals("jae")) {
					ret.code = jae;
				} else if (str.equals("jb")) {
					ret.code = jb;
				} else if (str.equals("jbe")) {
					ret.code = jbe;
				} else if (str.equals("jg")) {
					ret.code = jg;
				} else if (str.equals("jge")) {
					ret.code = jge;
				} else if (str.equals("jl")) {
					ret.code = jl;
				} else if (str.equals("jle")) {
					ret.code = jle;
				} else if (str.equals("je")) {
					ret.code = je;
				} else if (str.equals("mov")) {
					ret.code = mov;
				} else if (str.equals("xchg")) {
					ret.code = xchg;
				} else if (str.equals("cmp")) {
					ret.code = cmp;
				} else if (str.equals("add")) {
					ret.code = add;
				} else if (str.equals("sub")) {
					ret.code = sub;
				} else if (str.equals("mul")) {
					ret.code = mul;
				} else if (str.equals("div")) {
					ret.code = div;
				} else if (str.equals("neg")) {
					ret.code = neg;
				} else if (str.equals("ax")) {
					ret.code = ax;
				} else if (str.equals("ah")) {
					ret.code = ah;
				} else if (str.equals("al")) {
					ret.code = al;
				} else if (str.equals("bx")) {
					ret.code = bx;
				} else if (str.equals("bh")) {
					ret.code = bh;
				} else if (str.equals("bl")) {
					ret.code = bl;
				} else if (str.equals("cx")) {
					ret.code = cx;
				} else if (str.equals("ch")) {
					ret.code = ch;
				} else if (str.equals("cl")) {
					ret.code = cl;
				} else if (str.equals("dx")) {
					ret.code = dx;
				} else if (str.equals("dh")) {
					ret.code = dh;
				} else if (str.equals("dl")) {
					ret.code = dl;
				} else if (str.equals("si")) {
					ret.code = si;
				} else if (str.equals("di")) {
					ret.code = di;
				} else if (str.equals("bp")) {
					ret.code = bp;
				} else if (str.equals("sp")) {
					ret.code = sp;
				} else if (str.equals("cs")) {
					ret.code = cs;
				} else if (str.equals("ds")) {
					ret.code = ds;
				} else if (str.equals("ss")) {
					ret.code = ss;
				} else if (str.equals("es")) {
					ret.code = es;
				} else {
					ret.code = ident;
				}
			}
			if (ret.code == ident) {
				ret.str = str;
			} else {
				ret.str = AbstractCompiler.str[ret.code];
			}
		}
		return ret;
	}

}
