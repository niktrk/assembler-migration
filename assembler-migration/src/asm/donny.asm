.model small
.code
mov ax,12
mov bx,8
compare:
cmp ax,bx
je theend
ja greater
sub bx,ax
jmp compare
greater:
sub ax,bx
jmp compare
theend:
nop
end
