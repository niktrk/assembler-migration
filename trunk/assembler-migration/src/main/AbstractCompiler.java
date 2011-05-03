package main;

public abstract class AbstractCompiler {

	protected static final int
	// terminals
	none = 0,
	ident = 1,
	number = 2,
	comma = 3,
	colon = 4,
	lbrack = 5,
	rbrack = 6,
	string = 7,

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
	
}
