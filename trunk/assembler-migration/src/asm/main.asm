title Pera ;blah blah b34 23d
.model small
.stack 100h
.data
message db 'Ja volim vatat muve$'
message2 db 'Trula kobila $'


.code

novi proc
start:
mov dx, 10
mov ah, 02
ret
novi endp

stari macro z o v
start: 
mov ax, z
endm


start:
mov ax,@data
mov ds,ax

mov dx, offset message
mov ah,9

call novi

mov dx, offset message2
mov ah,9

quit: jmp quit

end start