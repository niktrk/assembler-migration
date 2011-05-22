title movs

.model small

.stack 100h

.data
nik db 32
let dw 49

.code

start:

cmp let, nik
jg letcar

letcar:
add al, let

end start