title Pera ;blah blah b34 23d
.model small
.stack 100h
.data
.code

write macro char
	mov ah,02
	mov dl,char
	int 21h
endm

start:
mov ax, @data
mov ds, ax

write 65

theend:
MOV AX,4C00H
INT 21h

end start
