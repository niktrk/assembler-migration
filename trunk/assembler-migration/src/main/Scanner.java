package main;

import java.io.BufferedReader;
import java.io.FileReader;

public class Scanner {

	public BufferedReader sc;
	private char chr;
	private boolean eof = false;

	public static final int
			// terminals
			none = 0,
			ident = 1,
			number = 2,
			comma = 3,
			colon = 4,
			lbrack = 5,
			rbrack = 6,
			apostr = 7,

			// segments
			title = 8,
			model = 9,
			stack = 10,
			data = 11,
			code = 12,
			end = 13,

			// models
			small = 14,
			compact = 15,
			medium = 16,
			large = 17,

			// size
			db = 18,
			dw = 19,

			// procedures
			proc = 20,
			far = 21,
			ret = 22,
			endp = 23,

			// macros
			macro = 24,
			endm = 25,

			// OneArgStat
			interr = 26, 
			loop = 27,
			push = 28,
			pop = 29,
			inc = 30,
			dec = 31,
			call = 32,
			not = 33,
				// jumps
			jmp = 34, 
			ja = 35, 
			jae = 36,
			jb = 37, 
			jbe = 38, 
			jg = 39,
			jge = 40,
			jl = 41,
			jle = 42,
			je = 43,

			// TwoArgStat
			mov = 44, 
			xchg = 45, 
			cmp = 46,
			add = 47, 
			sub = 48,
			mul = 49,
			div = 50, 
			and = 51, 
			or = 52,
			xor = 53,

			// Registers
			ax = 54,
			ah = 55, 
			al = 56, 
			bx = 57, 
			bh = 58, 
			bl = 59,
			cx = 60,
			ch = 61,
			cl = 62, 
			dx = 63, 
			dh = 64, 
			dl = 65, 
			si = 66,
			di = 67,
			bp = 68, 
			sp = 69, 
			cs = 70, 
			ds = 71, 
			ss = 72, 
			es = 73,

			// other
			atdata = 74, // @data
			offset = 75;

	public Scanner(String file) {
			try {
				sc = new BufferedReader(new FileReader(file));
				chr = (char) sc.read();
			} catch (Exception e) {
				System.out.println("File " + file + " doesn't exist." );
				System.exit(0);
			}


	}

	private void read() throws Exception {
		int i = sc.read();
		if (i == -1)
			eof = true;
		else
			chr = (char) i;
	}

	private String readName() throws Exception {
		String ret = "";
		while ((chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z' ||chr >= '0' && chr <= '9'|| chr =='$')&& !eof) {
			ret += chr;
			read();
		}
		return ret.toLowerCase();
	}

	private Token readNumber() throws Exception {
		String num = "";
		boolean hex = false;
		while (chr >= '0' && chr <= '9' && !eof) {
			num += chr;
			read();
		}
		if (chr == 'h' || chr == 'H'){
			hex = true;
			read();
		}
		Token ret = new Token(number, Integer.parseInt(num), hex, "");
		return ret;
	}

	private void readComment() throws Exception {
		while (chr != '\n' && !eof) {
			read();
		}
		if(!eof)
			read();
	}

	public Token next() throws Exception {
		while(chr <= ' ' && !eof){
			read();
		}
		if (chr == ';') {
			readComment();
		}
		Token ret = new Token(none, -1, false, "");
		if (!eof) {
			String str;
			if (chr >= '0' && chr <= '9') {
				ret = readNumber();
			}
			else if (chr == ',') {
				ret.code = comma;
				read();
			} else if (chr == ':') {
				ret.code = colon;
				read();
			}

			else if (chr == '[') {
				ret.code = lbrack;
				read();
			} else if (chr == ']') {
				ret.code = rbrack;
				read();
			} else if (chr == '\'') {
				ret.code = apostr;
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
				if (readName().equals("data"))
					ret.code = atdata;
			}

			else if (chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z' || chr == '$') {
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
				} else if (str.equals("not")) {
					ret.code = not;
				} else if (str.equals("jmp")) {
					ret.code = jmp;
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
				} else if (str.equals("and")) {
					ret.code = and;
				} else if (str.equals("or")) {
					ret.code = or;
				} else if (str.equals("xor")) {
					ret.code = xor;
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
				} else if (str.equals("offset")) {
					ret.code = offset;
				} else {
					ret.code = ident;
					ret.str = str;
				}

			}
		}
		return ret;
	}
	
}
