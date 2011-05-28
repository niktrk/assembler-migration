title Pera ;blah blah b34 23d
.model small
.stack 100h
.data

i db 3
j db 4
k dw 9
l dw 10

.code

start:
mov ax, @data
mov ds, ax

mov ax, 5
mov bx, 6
mov cl, 7
mov dl, 8

;xchg
xchg ax,bx
xchg cl,dl
xchg ax,k
xchg cl,i
xchg ch,al

xchg bp,bx

theend:
MOV AX,4C00H
INT 21h

end start
