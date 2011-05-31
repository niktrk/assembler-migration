.Model Small

.Data
niz db "hello", ' ', "world"
pom dw 3
decl db ?

.Code

;init data segment
MOV AX,@data
MOV DS,AX

write Macro param
MOV AH,2
MOV DL,param
INT 21h
EndM

;init value
MOV CX,11
MOV SI,0

ispis:
write niz[si]
inc si
LOOP ispis

;end program
MOV AH,4CH
INT 21h

End
