title movs

.model small

.stack 100h

.data
nik dw 32
let dw 49
.code

start:

mov bx, nik
cmp let, bx
jg letcar

letcar:
add ax, let

end start
