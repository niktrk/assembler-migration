.Model Small

.Data
n dw 3

.Code

;init data segment
MOV AX,@data
MOV DS,AX
MOV CX,n

llabel:

;init value
MOV BX,CX
ADD BX,48

;write
MOV AH,2
MOV DL,BL
INT 21h

LOOP llabel

end
