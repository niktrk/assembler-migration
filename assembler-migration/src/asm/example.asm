.model small

.data
n dw 3

.code

;init data segment
mov ax,@data
mov ds,ax

;init value
mov bx,n
add bx,48

;write
mov ah,02
mov dl,bl
int 21h

;end program
mov ah,4ch
int 21h

end