title Pera ;blah blah b34 23d
.model small 200h
.stack 100h
.data
message db 'Ja volim vatat muve$'
message2 db 'Trula kobila $'

.code

novi proc
mov dx, 10
mov ah, 02
int 21h
ret
novi endp


start:
mov ax,@data
mov ds,ax

mov dx, offset message
mov ah,9
int 21h

call novi

mov dx, offset message2
mov ah,9
int 21h

quit: jmp quit

end start