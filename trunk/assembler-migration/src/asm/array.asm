.Model Small

.Data
niz db 48,49,50,51
test dw 1

.Code

;init data segment
MOV AX,@data
MOV DS,AX

;init value
MOV BX,1

;write
MOV AH,2
MOV DL,niz[BX + 1 - SI + 1]
INT 21h


;end program
MOV AH,4Ch
INT 21h

end
