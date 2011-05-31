.model small

.data
	let db 30
	nik db 45
	prc db ?
.code

start:
mov ax, @data
mov ds, ax
mov ax, 0

sabiraj proc
mov al, let
mov bl, nik
add al, bl
mov dl, al
mov ah, 02
int 21h
ret
sabiraj endp

oduzimaj proc
mov bl, prc
sub al, bl
mov dl, al
mov ah, 02
int 21h
ret
oduzimaj endp

quit:
call sabiraj
call oduzimaj
mov ah, 4ch
int 21h
end quit