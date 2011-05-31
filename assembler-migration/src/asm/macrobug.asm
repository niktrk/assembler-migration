.Model Small

.Data
niz1 db 2,3,6,3,6,5,2

.Code

setuj Macro
MOV AH,4CH
EndM

kraj Macro
setuj
INT 21h
EndM

MOV AX,@data
MOV DS,AX

kraj

end
