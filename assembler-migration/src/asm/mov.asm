title movs

.model small

.stack 100h

.data
nik db 32
let dw 49

.code

start:

mov ax,let
mov bl,nik
mov let,dx
mov ds,ax
mov ah,9
mov cl,nik
xchg let, nik

add cx, ax
add cx, let

end start