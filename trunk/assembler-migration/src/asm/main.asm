.model small

.data
	let db 30
	nik db 41
.code

sabiraj proc
mov al, let
mov bl, nik
add al, bl
mov dl, al
mov ah, 02
int 21h
ret
sabiraj endp

start:
mov ax, @data
mov ds, ax
mov ax, 0
call sabiraj
mov ah, 4ch
int 21h
end start