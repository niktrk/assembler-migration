.Model Small

.Data
niz1 db 2,3,6,3,6,5,2

.Code

swap Macro a,b
MOV AL,a
MOV a,b
MOV b,AL
EndM

write Macro char
MOV AH,02
swap dl,char
INT 21h
EndM

MOV AX,@data
MOV DS,AX

MOV CL,niz1[1]
MOV BL,niz1[2]

ADD CL,48
ADD BL,48
write CL
write BL

MOV AH,4CH
INT 21h

end
