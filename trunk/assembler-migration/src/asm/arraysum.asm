.Model Small

.data
array db 1,2,3,4,5,6,7,0
n dw 7

.code
mov dx, @data
mov ds, dx

mov bx, 0
mov ax, 0
mov dx, 0

mainloop:

mov al, array[bx]     		; read array member
cmp bx,n 					; is it the n-th?
je progend 					; if yes, go to end
add dx, ax 					; the sum is in dx
inc bx
jmp mainloop

progend:
MOV AX,4C00H
INT 21h

end
