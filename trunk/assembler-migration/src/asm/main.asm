title Pera ;blah blah b34 23d
.model small
.stack 100h
.data
let db 8
nik db 10


.code

write macro char
	mov ah,02
	mov dl,char
	int 21h
endm	

start:
mov ax, @data
mov ds, ax
mov ax,0

mov al, 65
mov bh, 12
add al, bh

div nik					    ;podeli index(ax) sa 10
mov bl,al					;prebaci rezultat deljenja u bl
add bl,48					;napravi broj od ASCII coda
mov bh,ah					;prebaci ostatak pri deljenju u bh
add bh,48					;napravi broj od ASCII coda
write bl					;ispisi prvu cifru
write bh					;ispisi drugi cifru

theend:
MOV AX,4C00H
INT 21h
	
end start