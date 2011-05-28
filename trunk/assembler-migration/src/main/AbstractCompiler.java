package main;

/**
 * Scanner and parser base class.
 * 
 * @author Nikola Trkulja
 * @author Igor Let
 * 
 */
public abstract class AbstractCompiler {

	protected static final int
	// terminals
			none = 0,
			ident = 1, number = 2, comma = 3, colon = 4, lbrack = 5, rbrack = 6, string = 7,

			// segments
			title = 8, model = 9, stack = 10, data = 11, code = 12, end = 13,

			// models
			small = 14, compact = 15, medium = 16, large = 17,

			// size
			db = 18, dw = 19,

			// procedures
			proc = 20, far = 21, ret = 22, endp = 23,

			// macros
			macro = 24, endm = 25,

			// OneArgStat
			interr = 26, loop = 27, push = 28, pop = 29, inc = 30, dec = 31, call = 32, neg = 33,
			// jumps
			jmp = 34, ja = 35, jae = 36, jb = 37, jbe = 38, jg = 39, jge = 40, jl = 41, jle = 42, je = 43,

			// TwoArgStat
			mov = 44, xchg = 45, cmp = 46, add = 47, sub = 48, mul = 49, div = 50, shl = 51, shr = 52,

			// Registers
			ax = 53, ah = 54, al = 55, bx = 56, bh = 57, bl = 58, cx = 59, ch = 60, cl = 61, dx = 62, dh = 63, dl = 64,
			si = 65,
			di = 66, bp = 67, sp = 68, cs = 69, ds = 70, ss = 71, es = 72,

			// other
			atdata = 73, // @data
			offset = 74,

			// indexing
			minus = 75, plus = 76;

	protected static final String[] str = { "none", "ident", "number", "comma", "colon", "lbrack", "rbrack", "string", "title",
			"model", "stack", "data", "code", "end", "small", "compact", "medium", "large", "db", "dw", "proc", "far", "ret",
			"endp", "macro", "endm", "interr", "loop", "push", "pop", "inc", "dec", "call", "neg", "jmp", "ja", "jae", "jb",
			"jbe", "jg", "jge", "jl", "jle", "je", "mov", "xchg", "cmp", "add", "sub", "mul", "div", "shl", "shr", "ax", "ah",
			"al", "bx", "bh", "bl", "cx", "ch", "cl", "dx", "dh", "dl", "si", "di", "bp", "sp", "cs", "ds", "ss", "es", "atdata",
			"offset", "-", "+" };

}
