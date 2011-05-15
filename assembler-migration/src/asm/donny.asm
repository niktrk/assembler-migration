.Model Small

.Code
MOV AX,12
MOV BX,8

compare:
CMP AX,BX
JE theend
JA greater
SUB BX,AX
JMP compare

greater:
SUB AX,BX
JMP compare

theend:
MOV AX,4C00H
INT 21h

End
