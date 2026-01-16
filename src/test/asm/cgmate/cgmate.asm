                org c000

c000 c378c0  	JP	c078		; Jump to the main program entry point.

; --- I/O Handshake and Data Transfer Subroutines ---
; These routines handle communication with an external device, possibly
; a custom parallel interface for loading the image data. They use a
; handshake protocol on I/O ports FEh and FFh.

c003 00      	NOP			    ; No operation, padding.

; Subroutine: Send byte in A to device (via port FDh)
:c004 f5      	PUSH	AF		; Save AF register pair on the stack.
c005 3e0f    	LD	A,0f		; Load A with command 0Fh.
c007 d3ff    	OUT	ff,A		; Send command to port FFh.
c009 1801    	JR	01		    ; Short jump to c00c.

:c00b f5      	PUSH	AF		; Save AF register pair on the stack.

; Handshake loop: Wait for device to be ready
:c00c dbfe    	IN	A,fe		; Read status from port FEh.
c00e e602    	AND	02		    ; Check if bit 1 is set (device ready for command).
c010 28fa    	JR	Z,fa		; If not ready (Z flag set), loop back to c00c.
c012 3e0e    	LD	A,0e		; Load A with command 0Eh.
c014 d3ff    	OUT	ff,A		; Send command to port FFh.
c016 f1      	POP	AF		    ; Restore original A value.
c017 f5      	PUSH	AF		; Save it again.
c018 d3fd    	OUT	fd,A		; Send the data byte in A to port FDh.
c01a 3e09    	LD	A,09		; Load A with command 09h.
c01c d3ff    	OUT	ff,A		; Send command to port FFh.

; Handshake loop: Wait for device to acknowledge data
:c01e dbfe    	IN	A,fe		; Read status from port FEh.
c020 e604    	AND	04		    ; Check if bit 2 is set (device busy).
c022 28fa    	JR	Z,fa		; If not busy, loop back to c01e.
c024 3e08    	LD	A,08		; Load A with command 08h.
c026 d3ff    	OUT	ff,A		; Send command to port FFh.

; Handshake loop: Wait for device to be not busy
:c028 dbfe    	IN	A,fe		; Read status from port FEh.
c02a e604    	AND	04			; Check if bit 2 is set (device busy).
c02c 20fa    	JR	NZ,fa		; If busy (NZ flag set), loop back to c028.
c02e f1      	POP	AF			; Restore AF.
c02f c9      	RET				; Return from subroutine.

; Subroutine: Read byte from device (via port FCh) into memory at (HL)
c030 3e0b    	LD	A,0b		; Load A with command 0Bh.
c032 d3ff    	OUT	ff,A		; Send command to port FFh.

; Handshake loop: Wait for data to be available
:c034 dbfe    	IN	A,fe		; Read status from port FEh.
c036 e601    	AND	01			; Check if bit 0 is set (data available).
c038 28fa    	JR	Z,fa		; If not available, loop back to c034.
c03a 3e0a    	LD	A,0a		; Load A with command 0Ah.
c03c d3ff    	OUT	ff,A		; Send command to port FFh.
c03e dbfc    	IN	A,fc		; Read data byte from port FCh.
c040 77      	LD	(HL),A		; Store the byte in memory at the address in HL.
c041 23      	INC	HL			; Increment memory pointer.
c042 3e0d    	LD	A,0d		; Load A with command 0Dh.
c044 d3ff    	OUT	ff,A		; Send command to port FFh.

; Handshake loop: Wait for device to acknowledge read
:c046 dbfe    	IN	A,fe		; Read status from port FEh.
c048 e601    	AND	01			; Check if bit 0 is set.
c04a 20fa    	JR	NZ,fa		; If set, loop back to c046.
c04c dbfc    	IN	A,fc		; Read another byte from port FCh (clears buffer?).
c04e 77      	LD	(HL),A		; Store it.
c04f 23      	INC	HL			; Increment memory pointer.
c050 3e0c    	LD	A,0c		; Load A with command 0Ch.
c052 d3ff    	OUT	ff,A		; Send command to port FFh.
c054 c9      	RET				; Return from subroutine.

; Subroutine: Send byte from memory at (HL) to device (via port FDh)
:c055 dbfe    	IN	A,fe		; Read status from port FEh.
c057 e602    	AND	02			; Check if bit 1 is set (device ready).
c059 28fa    	JR	Z,fa		; If not ready, loop back to c055.
c05b 7e      	LD	A,(HL)		; Load byte from memory at (HL).
c05c d3fd    	OUT	fd,A		; Send data byte to port FDh.
c05e 23      	INC	HL			; Increment memory pointer.
c05f 3e09    	LD	A,09		; Load A with command 09h.
c061 d3ff    	OUT	ff,A		; Send command to port FFh.

; Handshake loop: Wait for device to be busy
:c063 dbfe    	IN	A,fe		; Read status from port FEh.
c065 e604    	AND	04			; Check if bit 2 is set (device busy).
c067 28fa    	JR	Z,fa		; If not busy, loop back to c063.
c069 7e      	LD	A,(HL)		; Load next byte from memory.
c06a d3fd    	OUT	fd,A		; Send it to port FDh.
c06c 23      	INC	HL			; Increment memory pointer.
c06d 3e08    	LD	A,08		; Load A with command 08h.
c06f d3ff    	OUT	ff,A		; Send command to port FFh.

; Handshake loop: Wait for device to be not busy
:c071 dbfe    	IN	A,fe		; Read status from port FEh.
c073 e604    	AND	04			; Check if bit 2 is set (device busy).
c075 20fa    	JR	NZ,fa		; If busy, loop back to c071.
c077 c9      	RET				; Return from subroutine.

; --- Main Program Entry and Initialization ---
c078 31fe9f  	LD	SP,9ffe			; Initialize Stack Pointer.
c07b 3a0200  	LD	A,(0002)		; Load a value from address 0002h (likely a parameter).
c07e feff    	CP	ff				; Compare with FFh.
c080 2812    	JR	Z,12			; If it's FFh, jump to main logic at c094.
c082 3ecc    	LD	A,cc			; Load A with CCh (error/status code).
c084 32ffff  	LD	(ffff),A		; Store it at address FFFFh.
c087 f3      	DI					; Disable interrupts.
c088 3ac2e6  	LD	A,(e6c2)		; Read system configuration.
c08b e6f9    	AND	f9				; Modify configuration bits.
c08d f604    	OR	04
c08f d331    	OUT	31,A			; Write to system control port 2.
c091 c30000  	JP	0000			; Jump to 0000h (warm boot/exit).

; Main logic starts here
:c094 3e91    	LD	A,91			; Load A with 91h.
c096 d3ff    	OUT	ff,A			; Send to port FFh (likely a reset or init command).
c098 af      	XOR	A				; A = 0.
c099 cd04c0  	CALL	c004		; Send byte in A (00h) to device.
c09c 3e17    	LD	A,17			; Load A with 17h.
c09e cd04c0  	CALL	c004		; Send byte in A (17h) to device.
c0a1 3e0f    	LD	A,0f			; Load A with 0Fh.
c0a3 cd0bc0  	CALL	c00b		; Send byte in A (0Fh) to device.
c0a6 0600    	LD	B,00			; B = 0.
c0a8 0e50    	LD	C,50			; C = 50h.

; This loop seems to be sending a command block to the device.
:c0aa 3e0e    	LD	A,0e			; Command 0Eh.
c0ac cd04c0  	CALL	c004		; Send it.
c0af 3e10    	LD	A,10			; Command 10h.
c0b1 cd0bc0  	CALL	c00b		; Send it.
c0b4 af      	XOR	A				; A = 0.
c0b5 cd0bc0  	CALL	c00b		; Send it.
c0b8 78      	LD	A,B				; A = B (loop counter).
c0b9 cd0bc0  	CALL	c00b		; Send it.
c0bc 3e01    	LD	A,01			; A = 1.
c0be cd0bc0  	CALL	c00b		; Send it.
c0c1 79      	LD	A,C				; A = C (address/offset).
c0c2 cd0bc0  	CALL	c00b		; Send it.
c0c5 af      	XOR	A				; A = 0.
c0c6 cd0bc0  	CALL	c00b		; Send it.
c0c9 79      	LD	A,C				; Get address/offset again.
c0ca d610    	SUB	10				; Subtract 10h.
c0cc 4f      	LD	C,A				; Update C.
c0cd 04      	INC	B				; Increment B.
c0ce 78      	LD	A,B				; Get B.
c0cf fe02    	CP	02				; Compare with 2.
c0d1 20d7    	JR	NZ,d7			; Loop if B is not 2.

; After setup, start receiving data.
c0d3 3e15    	LD	A,15			; Command 15h.
c0d5 cd04c0  	CALL	c004		; Send it.
c0d8 3e40    	LD	A,40			; A = 40h.
c0da cd0bc0  	CALL	c00b		; Send it.
c0dd af      	XOR	A				; A = 0.
c0de cd0bc0  	CALL	c00b		; Send it.
c0e1 3e20    	LD	A,20			; A = 20h.
c0e3 cd0bc0  	CALL	c00b		; Send it.
c0e6 af      	XOR	A				; A = 0.
c0e7 cd0bc0  	CALL	c00b		; Send it.
c0ea 2100a0  	LD	HL,a000			; Set destination memory pointer to A000h.
c0ed cd30c0  	CALL	c030		; Read a block of data from device into memory.
c0f0 7c      	LD	A,H				; Get high byte of memory pointer.
c0f1 fec0    	CP	c0				; Check if we've filled up to C000h.
c0f3 20f8    	JR	NZ,f8			; If not, loop and read more data.
c0f5 31fea0  	LD	SP,a0fe			; Adjust stack pointer.
c0f8 c3fbb0  	JP	b0fb			; Jump to next stage.

; ... (A lot of initialization and setup code follows) ...
; The code from c0fb to c2ae is complex setup, involving I/O ports,
; memory initialization, and calls to other subroutines. It prepares
; the system for displaying the image.




c0fb 3e11    	LD	A,11
c0fd d340    	OUT	40,A		; strobe port
c0ff 212db1  	LD	HL,b12d
c102 cd4308  	CALL	0843
c105 af      	XOR	A
c106 3260ea  	LD	ea60,A
c109 cd5a04  	CALL	045a
c10c af      	XOR	A
c10d 06a0    	LD	B,a0
c10f 217cea  	LD	HL,ea7c
c112 77      	LD	(HL),A
c113 23      	INC	HL
c114 10fc    	DJNZ	fc		; c112
c116 3affff  	LD	A,(ffff)
c119 fecc    	CP	cc
c11b c2afb2  	JP	NZ,b2af
c11e af      	XOR	A
c11f 32ffff  	LD	ffff,A
c122 3a03b0  	LD	A,(b003)
c125 fecc    	CP	cc
c127 caa2b7  	JP	Z,b7a2
c12a c306b5  	JP	b506

c12d 3830    	; 80,25
c12f 2c
c130 3235
c132 00         DB 0, 30

c162 56      	LD	D,(HL)
c163 23      	INC	HL
c164 5e      	LD	E,(HL)
c165 23      	INC	HL
c166 eb      	EX	DE,HL
c167 cda903  	CALL	03a9
c16a eb      	EX	DE,HL
:c16b 7e      	LD	A,(HL)
c16c fe00    	CP	00
c16e c8      	RET	Z
c16f cd5702  	CALL	0257
c172 23      	INC	HL
c173 18f6    	JR	f6		; c16b
c175 03      	INC	BC
c176 013d3d  	LD	BC,3d3d

c179 3d         DB 3d, 1d ; =============================
c195 00         DB 0

c196 05      	DEC	B
c197 02      	LD	(BC),A
c198 43      	LD	B,E
c199 2e47    	LD	L,47
c19b 2e20    	LD	L,20
c19d 4d      	LD	C,L
c19e 61      	LD	H,C
c19f 74      	LD	(HL),H
c1a0 65      	LD	H,L
c1a1 2020    	JR	NZ,20		; c1c3
c1a3 2056    	JR	NZ,56		; c1fb
c1a5 65      	LD	H,L
c1a6 72      	LD	(HL),D
c1a7 73      	LD	(HL),E
c1a8 69      	LD	L,C
c1a9 6f      	LD	L,A
c1aa 6e      	LD	L,(HL)
c1ab 2031    	JR	NZ,31		; c1de
c1ad 2e30    	LD	L,30
c1af 2020    	JR	NZ,20		; c1d1
c1b1 00      	NOP
c1b2 05      	DEC	B
c1b3 03      	INC	BC
c1b4 2020    	JR	NZ,20		; c1d6
c1b6 66      	LD	H,(HL)
c1b7 6f      	LD	L,A
c1b8 72      	LD	(HL),D
c1b9 2038    	JR	NZ,38		; c1f3
c1bb 3830    	JR	C,30		; c1ed
c1bd 312f6d  	LD	SP,6d2f
c1c0 6b      	LD	L,E
c1c1 322f53  	LD	532f,A
c1c4 52      	LD	D,D
c1c5 2f      	CPL
c1c6 54      	LD	D,H
c1c7 52      	LD	D,D
c1c8 2020    	JR	NZ,20		; c1ea
c1ca 2020    	JR	NZ,20		; c1ec
c1cc 2000    	JR	NZ,00		; c1ce
c1ce 05      	DEC	B
c1cf 04      	INC	B
c1d0 2020    	JR	NZ,20		; c1f2
c1d2 2020    	JR	NZ,20		; c1f4
c1d4 2020    	JR	NZ,20		; c1f6
c1d6 2020    	JR	NZ,20		; c1f8
c1d8 2020    	JR	NZ,20		; c1fa
c1da 2020    	JR	NZ,20		; c1fc
c1dc 62      	LD	H,D
c1dd 79      	LD	A,C
c1de 204f    	JR	NZ,4f		; c22f
c1e0 52      	LD	D,D
c1e1 41      	LD	B,C
c1e2 4e      	LD	C,(HL)
c1e3 47      	LD	B,A
c1e4 45      	LD	B,L
c1e5 2042    	JR	NZ,42		; c229
c1e7 4f      	LD	C,A
c1e8 58      	LD	E,B
c1e9 2020    	JR	NZ,20		; c20b
c1eb 00      	NOP
c1ec 25      	DEC	H
c1ed 03      	INC	BC
c1ee 87      	ADD	A,A
c1ef 2053    	JR	NZ,53		; c244
c1f1 41      	LD	B,C
c1f2 56      	LD	D,(HL)
c1f3 45      	LD	B,L
c1f4 204d    	JR	NZ,4d		; c243
c1f6 4f      	LD	C,A
c1f7 44      	LD	B,H
c1f8 45      	LD	B,L
c1f9 2087    	JR	NZ,87		; c182
c1fb 00      	NOP
c1fc 25      	DEC	H
c1fd 03      	INC	BC
c1fe 87      	ADD	A,A
c1ff 204c    	JR	NZ,4c		; c24d
c201 4f      	LD	C,A
c202 41      	LD	B,C
c203 44      	LD	B,H
c204 204d    	JR	NZ,4d		; c253
c206 4f      	LD	C,A
c207 44      	LD	B,H
c208 45      	LD	B,L
c209 2087    	JR	NZ,87		; c192
c20b 00      	NOP
c20c 44      	LD	B,H
c20d 69      	LD	L,C
c20e 73      	LD	(HL),E
c20f 6b      	LD	L,E
c210 2066    	JR	NZ,66		; c278
c212 75      	LD	(HL),L
c213 6c      	LD	L,H
c214 6c      	LD	L,H
c215 2021    	JR	NZ,21		; c238
c217 0d      	DEC	C
c218 0a      	LD	A,(BC)
c219 00      	NOP

c21a 46      	LD	B,(HL)
c21b 69      	LD	L,C
c21c 6c      	LD	L,H
c21d 65      	LD	H,L
c21e 206e    	JR	NZ,6e		; c28e
c220 61      	LD	H,C
c221 6d      	LD	L,L
c222 65      	LD	H,L
c223 203f    	JR	NZ,3f		; c264
c225 3a002a  	LD	A,(2a00)
c228 43      	LD	B,E
c229 68      	LD	L,B
c22a 61      	LD	H,C
c22b 6e      	LD	L,(HL)
c22c 67      	LD	H,A
c22d 65      	LD	H,L
c22e 2053    	JR	NZ,53		; c283
c230 57      	LD	D,A
c231 2e0d    	LD	L,0d
c233 0a      	LD	A,(BC)
c234 00      	NOP
c235 3801    	JR	C,01		; c238
c237 4b      	LD	C,E
c238 69      	LD	L,C
c239 6c      	LD	L,H
c23a 6c      	LD	L,H
c23b 00      	NOP
c23c 3802    	JR	C,02		; c240
c23e 4e      	LD	C,(HL)
c23f 61      	LD	H,C
c240 6d      	LD	L,L
c241 65      	LD	H,L
c242 00      	NOP
c243 3803    	JR	C,03		; c248
c245 53      	LD	D,E
c246 77      	LD	(HL),A
c247 61      	LD	H,C
c248 70      	LD	(HL),B
c249 00      	NOP
c24a 3804    	JR	C,04		; c250
c24c 46      	LD	B,(HL)
c24d 69      	LD	L,C
c24e 6c      	LD	L,H
c24f 65      	LD	H,L
c250 73      	LD	(HL),E
c251 00      	NOP
c252 41      	LD	B,C
c253 01436f  	LD	BC,6f43
c256 6c      	LD	L,H
c257 6f      	LD	L,A
c258 72      	LD	(HL),D
c259 00      	NOP
c25a 41      	LD	B,C
c25b 02      	LD	(BC),A
c25c 44      	LD	B,H
c25d 69      	LD	L,C
c25e 73      	LD	(HL),E
c25f 70      	LD	(HL),B
c260 6c      	LD	L,H
c261 61      	LD	H,C
c262 79      	LD	A,C
c263 00      	NOP
c264 41      	LD	B,C
c265 03      	INC	BC
c266 42      	LD	B,D
c267 61      	LD	H,C
c268 63      	LD	H,E
c269 6b      	LD	L,E
c26a 75      	LD	(HL),L
c26b 70      	LD	(HL),B
c26c 00      	NOP
c26d 4e      	LD	C,(HL)
c26e 6f      	LD	L,A
c26f 203f    	JR	NZ,3f		; c2b0
c271 3a004e  	LD	A,(4e00)
c274 65      	LD	H,L
c275 77      	LD	(HL),A
c276 206e    	JR	NZ,6e		; c2e6
c278 61      	LD	H,C
c279 6d      	LD	L,L
c27a 65      	LD	H,L
c27b 203f    	JR	NZ,3f		; c2bc
c27d 3a0042  	LD	A,(4200)
c280 61      	LD	H,C
c281 64      	LD	H,H
c282 2049    	JR	NZ,49		; c2cd
c284 6e      	LD	L,(HL)
c285 70      	LD	(HL),B
c286 75      	LD	(HL),L
c287 74      	LD	(HL),H
c288 2021    	JR	NZ,21		; c2ab
c28a 0d      	DEC	C
c28b 0a      	LD	A,(BC)
c28c 00      	NOP
c28d 73      	LD	(HL),E
c28e 75      	LD	(HL),L
c28f 72      	LD	(HL),D
c290 65      	LD	H,L
c291 203f    	JR	NZ,3f		; c2d2
c293 00      	NOP
c294 53      	LD	D,E
c295 65      	LD	H,L
c296 74      	LD	(HL),H
c297 206e    	JR	NZ,6e		; c307
c299 65      	LD	H,L
c29a 77      	LD	(HL),A
c29b 2064    	JR	NZ,64		; c301
c29d 69      	LD	L,C
c29e 73      	LD	(HL),E
c29f 6b      	LD	L,E
c2a0 2023    	JR	NZ,23		; c2c5
c2a2 320d0a  	LD	0a0d,A
c2a5 00      	NOP
c2a6 41      	LD	B,C
c2a7 04      	INC	B
c2a8 4f      	LD	C,A
c2a9 70      	LD	(HL),B
c2aa 74      	LD	(HL),H
c2ab 69      	LD	L,C
c2ac 6f      	LD	L,A
c2ad 6e      	LD	L,(HL)
c2ae 00      	NOP




; --- IMAGE DECODING AND DISPLAY SECTION ---
; This is the most interesting part of the code. It appears to be the
; main loop for decoding and displaying the image.

c2af cd74b3  	CALL	b374		; Call a setup subroutine.
c2b2 21ecb1  	LD	HL,b1ec			; Load HL with an address (likely config data).
c2b5 cd62b1  	CALL	b162		; Call a processing subroutine.
c2b8 cdbab3  	CALL	b3ba		; Call another setup/processing subroutine.
c2bb 211ab2  	LD	HL,b21a			; Load another address.
c2be cd6bb1  	CALL	b16b		; Call processing subroutine.
c2c1 cd8a1b  	CALL	1b8a		; Call a system routine.

; Main image decoding loop starts here.
c2c4 2100bb  	LD	HL,bb00			; HL points to the compressed image data buffer.
:c2c7 7e      	LD	A,(HL)			; Read a byte from the compressed data.
c2c8 feed    	CP	ed				; Is it the 'ED' marker? (Possibly end of image).
c2ca 2009    	JR	NZ,09			; If not, jump to c2d5.
c2cc 210cb2  	LD	HL,b20c			; Load address of some configuration.
c2cf cd6bb1  	CALL	b16b		; Process it.
c2d2 c354b3  	JP	b354			; Jump to a finalization routine.

:c2d5 fe00    	CP	00				; Is the byte 00h? (Could be a special marker).
c2d7 280a    	JR	Z,0a			; If so, jump to c2e3.
c2d9 feff    	CP	ff				; Is the byte FFh? (Another special marker).
c2db 2806    	JR	Z,06			; If so, jump to c2e3.

; This is likely a "skip" or "seek" command in the compressed data.
c2dd 111000  	LD	DE,0010			; DE = 10h (16 bytes).
c2e0 19      	ADD	HL,DE			; Add 16 to HL, skipping a block of data.
c2e1 18e4    	JR	e4				; Jump back to the start of the loop (c2c7).

; Handle 00h or FFh markers
:c2e3 2243b1  	LD	(b143),HL		; Store the current data pointer.
c2e6 3ecc    	LD	A,cc			; Load A with CCh.
c2e8 77      	LD	(HL),A			; Write CCh to the data buffer (marks as processed?).
c2e9 cd99b3  	CALL	b399		; Call a subroutine to process this marker.




c2ec 08      	EX	AF,AF'
c2ed af      	XOR	A
c2ee 3245b1  	LD	b145,A
c2f1 3246b1  	LD	b146,A
c2f4 3247b1  	LD	b147,A
c2f7 08      	EX	AF,AF'
c2f8 f3      	DI
c2f9 3e3f    	LD	A,3f
c2fb d331    	OUT	31,A			; system control port (2)
c2fd cd00a8  	CALL	a800
c300 3e35    	LD	A,35
c302 d331    	OUT	31,A			; system control port (2)
c304 fb      	EI




; ... more processing ...
c305 3a46b1  	LD	A,(b146)		; Load a state variable.
c308 fe01    	CP	01				; Check its value.
c30a 283c    	JR	Z,3c			; Branch based on state.

; This section appears to be part of the RLE (Run-Length Encoding) decoding.
; It copies blocks of data to the screen buffer.
c30c 2a43b1  	LD	HL,(b143)	; Get the data pointer.




c30f 7d      	LD	A,L
c310 2e0b    	LD	L,0b
c312 85      	ADD	A,L
c313 6f      	LD	L,A
c314 08      	EX	AF,AF'
c315 77      	LD	(HL),A
c316 08      	EX	AF,AF'
c317 23      	INC	HL




; ... calculates destination address ...
c318 eb      	EX	DE,HL			; DE = destination VRAM address.
c319 2160b3  	LD	HL,b360			; HL = source data.
c31c 010400  	LD	BC,0004			; BC = 4 bytes.
c31f edb0    	LDIR				; Copy 4 bytes from (HL) to (DE). This is a block copy.

c321 2a43b1  	LD	HL,(b143)		; Get data pointer again.
; ... calculates another destination address ...
c328 eb      	EX	DE,HL			; DE = destination VRAM address.
c329 2164b3  	LD	HL,b364			; HL = source data.
c32c 011000  	LD	BC,0010			; BC = 16 bytes.
c32f edb0    	LDIR				; Copy 16 bytes. This is a "literal run" in RLE.

c331 cda0b4  	CALL	b4a0		; Call subroutine (possibly to update palette or screen state).
c334 cdbbb4  	CALL	b4bb		; Call another update subroutine.
; ... more logic ...




c337 cd5a04  	CALL	045a
c33a cd74b3  	CALL	b374
c33d 21ecb1  	LD	HL,b1ec
c340 cd62b1  	CALL	b162
c343 cdbab3  	CALL	b3ba




c346 180c    	JR	0c				; Jump to c354 to continue processing.




c348 2a43b1  	LD	HL,(b143)
c34b cd85b4  	CALL	b485
c34e 210cb2  	LD	HL,b20c
c351 cd6bb1  	CALL	b16b
c354 2127b2  	LD	HL,b227
c357 cd6bb1  	CALL	b16b
c35a cdd20b  	CALL	0bd2
c35d a8      	XOR	B
c35e 18fd    	JR	fd		; c35d
c360 012345  	LD	BC,4523
c363 67      	LD	H,A
c364 00      	NOP
c365 40      	LD	B,B
c366 07      	RLCA
c367 40      	LD	B,B
c368 3840    	JR	C,40		; c3aa
c36a 3f      	CCF
c36b 40      	LD	B,B
c36c 00      	NOP
c36d 47      	LD	B,A
c36e 07      	RLCA
c36f 47      	LD	B,A
c370 3847    	JR	C,47		; c3b9
c372 3f      	CCF
c373 47      	LD	B,A
c374 2175b1  	LD	HL,b175
c377 cd62b1  	CALL	b162
c37a 2196b1  	LD	HL,b196
c37d cd62b1  	CALL	b162
c380 21b2b1  	LD	HL,b1b2
c383 cd62b1  	CALL	b162
c386 21ceb1  	LD	HL,b1ce
c389 cd62b1  	CALL	b162
c38c 210503  	LD	HL,0305
c38f cda903  	CALL	03a9
c392 2177b1  	LD	HL,b177
c395 cd6bb1  	CALL	b16b
c398 c9      	RET
c399 23      	INC	HL
c39a 1196ec  	LD	DE,ec96
c39d 0609    	LD	B,09
c39f 1a      	LD	A,(DE)
c3a0 fe00    	CP	00
c3a2 280b    	JR	Z,0b		; c3af
c3a4 7b      	LD	A,E
c3a5 fe9f    	CP	9f
c3a7 c8      	RET	Z
c3a8 1a      	LD	A,(DE)
c3a9 77      	LD	(HL),A
c3aa 05      	DEC	B
c3ab 23      	INC	HL
c3ac 13      	INC	DE
c3ad 18f0    	JR	f0		; c39f
c3af 78      	LD	A,B
c3b0 fe00    	CP	00
c3b2 c8      	RET	Z
c3b3 3e20    	LD	A,20
c3b5 77      	LD	(HL),A
c3b6 23      	INC	HL
c3b7 10fc    	DJNZ	fc		; c3b5
c3b9 c9      	RET
c3ba 08      	EX	AF,AF'
c3bb af      	XOR	A
c3bc 08      	EX	AF,AF'
c3bd 2100bb  	LD	HL,bb00
c3c0 2248b1  	LD	(b148),HL
c3c3 3e08    	LD	A,08
c3c5 324ab1  	LD	b14a,A
c3c8 2602    	LD	H,02
c3ca 6f      	LD	L,A
c3cb cda903  	CALL	03a9
c3ce 2a48b1  	LD	HL,(b148)
c3d1 7e      	LD	A,(HL)
c3d2 feed    	CP	ed
c3d4 ca23b4  	JP	Z,b423
c3d7 fe00    	CP	00
c3d9 ca7bb4  	JP	Z,b47b
c3dc feff    	CP	ff
c3de 2843    	JR	Z,43		; c423
c3e0 e5      	PUSH	HL
c3e1 111000  	LD	DE,0010
c3e4 19      	ADD	HL,DE
c3e5 2248b1  	LD	(b148),HL
c3e8 e1      	POP	HL
c3e9 7e      	LD	A,(HL)
c3ea cd4fb4  	CALL	b44f
c3ed 3e3a    	LD	A,3a
c3ef cd5702  	CALL	0257
c3f2 23      	INC	HL
c3f3 0609    	LD	B,09
c3f5 7e      	LD	A,(HL)
c3f6 cd5702  	CALL	0257
c3f9 23      	INC	HL
c3fa 10f9    	DJNZ	f9		; c3f5
c3fc 3e20    	LD	A,20
c3fe cd5702  	CALL	0257
c401 23      	INC	HL
c402 7e      	LD	A,(HL)
c403 cd4fb4  	CALL	b44f
c406 0605    	LD	B,05
c408 3e20    	LD	A,20
c40a cd5702  	CALL	0257
c40d 10fb    	DJNZ	fb		; c40a
c40f 08      	EX	AF,AF'
c410 fe03    	CP	03
c412 2804    	JR	Z,04		; c418
c414 3c      	INC	A
c415 08      	EX	AF,AF'
c416 18b6    	JR	b6		; c3ce
c418 af      	XOR	A
c419 08      	EX	AF,AF'
c41a 3a4ab1  	LD	A,(b14a)
c41d 3c      	INC	A
c41e 324ab1  	LD	b14a,A
c421 18a5    	JR	a5		; c3c8
c423 2145b4  	LD	HL,b445
c426 cd6bb1  	CALL	b16b
c429 210000  	LD	HL,0000
c42c 1100ba  	LD	DE,ba00
c42f 0650    	LD	B,50
c431 1a      	LD	A,(DE)
c432 feff    	CP	ff
c434 2001    	JR	NZ,01		; c437
c436 2c      	INC	L
c437 13      	INC	DE
c438 10f7    	DJNZ	f7		; c431
c43a 7d      	LD	A,L
c43b cd4fb4  	CALL	b44f
c43e 214ab4  	LD	HL,b44a
c441 cd6bb1  	CALL	b16b
c444 c9      	RET
c445 0d      	DEC	C
c446 0a      	LD	A,(BC)
c447 2028    	JR	NZ,28		; c471
c449 00      	NOP
c44a 29      	ADD	HL,HL
c44b 0d      	DEC	C
c44c 0a      	LD	A,(BC)
c44d 0a      	LD	A,(BC)
c44e 00      	NOP
c44f e5      	PUSH	HL
c450 32a8f0  	LD	f0a8,A
c453 af      	XOR	A
c454 32a9f0  	LD	f0a9,A
c457 214bb1  	LD	HL,b14b
c45a c5      	PUSH	BC
c45b 010000  	LD	BC,0000
c45e cd9f30  	CALL	309f
c461 c1      	POP	BC
c462 3a4eb1  	LD	A,(b14e)
c465 fe30    	CP	30
c467 200d    	JR	NZ,0d		; c476
c469 3e20    	LD	A,20
c46b cd5702  	CALL	0257
c46e 3a4fb1  	LD	A,(b14f)
c471 cd5702  	CALL	0257
c474 e1      	POP	HL
c475 c9      	RET
c476 cd5702  	CALL	0257
c479 18f3    	JR	f3		; c46e
c47b 111000  	LD	DE,0010
c47e 19      	ADD	HL,DE
c47f 2248b1  	LD	(b148),HL
c482 c3ceb3  	JP	b3ce
c485 af      	XOR	A
c486 060a    	LD	B,0a
c488 77      	LD	(HL),A
c489 23      	INC	HL
c48a 10fc    	DJNZ	fc		; c488
c48c 7e      	LD	A,(HL)
c48d 26ba    	LD	H,ba
c48f 6f      	LD	L,A
c490 7e      	LD	A,(HL)
c491 fecc    	CP	cc
c493 2807    	JR	Z,07		; c49c
c495 47      	LD	B,A
c496 3eff    	LD	A,ff
c498 77      	LD	(HL),A
c499 68      	LD	L,B
c49a 18f4    	JR	f4		; c490
c49c 3eff    	LD	A,ff
c49e 77      	LD	(HL),A
c49f c9      	RET
c4a0 0601    	LD	B,01
c4a2 2100bb  	LD	HL,bb00
c4a5 7e      	LD	A,(HL)
c4a6 feed    	CP	ed
c4a8 c8      	RET	Z
c4a9 feff    	CP	ff
c4ab c8      	RET	Z
c4ac fe00    	CP	00
c4ae 2809    	JR	Z,09		; c4b9
c4b0 78      	LD	A,B
c4b1 77      	LD	(HL),A
c4b2 04      	INC	B
c4b3 111000  	LD	DE,0010
c4b6 19      	ADD	HL,DE
c4b7 18ec    	JR	ec		; c4a5
c4b9 18f8    	JR	f8		; c4b3
c4bb 2100ba  	LD	HL,ba00
c4be 3e11    	LD	A,11
c4c0 cd04b0  	CALL	b004
c4c3 3e06    	LD	A,06
c4c5 cd0bb0  	CALL	b00b
c4c8 3e00    	LD	A,00
c4ca cd0bb0  	CALL	b00b
c4cd 3e00    	LD	A,00
c4cf cd0bb0  	CALL	b00b
c4d2 3e0b    	LD	A,0b
c4d4 cd0bb0  	CALL	b00b
c4d7 cd55b0  	CALL	b055
c4da 7c      	LD	A,H
c4db fec0    	CP	c0
c4dd 2802    	JR	Z,02		; c4e1
c4df 18f6    	JR	f6		    ; c4d7
c4e1 2100ab  	LD	HL,ab00
c4e4 3e11    	LD	A,11
c4e6 cd04b0  	CALL	b004
c4e9 3e05    	LD	A,05
c4eb cd0bb0  	CALL	b00b
c4ee 3e00    	LD	A,00
c4f0 cd0bb0  	CALL	b00b
c4f3 3e01    	LD	A,01
c4f5 cd0bb0  	CALL	b00b
c4f8 3e0c    	LD	A,0c
c4fa cd0bb0  	CALL	b00b
c4fd cd55b0  	CALL	b055
c500 7c      	LD	A,H
c501 feb0    	CP	b0
c503 c8      	RET	Z
c504 18f7    	JR	f7		     ; c4fd
c506 cd5a04  	CALL	045a
c509 cd74b3  	CALL	b374
c50c 21fcb1  	LD	HL,b1fc
c50f cd62b1  	CALL	b162
c512 2135b2  	LD	HL,b235
c515 cd62b1  	CALL	b162
c518 213cb2  	LD	HL,b23c
c51b cd62b1  	CALL	b162
c51e 2143b2  	LD	HL,b243
c521 cd62b1  	CALL	b162
c524 214ab2  	LD	HL,b24a
c527 cd62b1  	CALL	b162
c52a 2152b2  	LD	HL,b252
c52d cd62b1  	CALL	b162
c530 215ab2  	LD	HL,b25a
c533 cd62b1  	CALL	b162
c536 2164b2  	LD	HL,b264
c539 cd62b1  	CALL	b162
c53c 21a6b2  	LD	HL,b2a6
c53f cd62b1  	CALL	b162
c542 cdbab3  	CALL	b3ba
c545 cd5bb6  	CALL	b65b
c548 3a04b7  	LD	A,(b704)
c54b fe01    	CP	01
c54d 28f6    	JR	Z,f6		; c545
c54f cd5eb5  	CALL	b55e
c552 cd750f  	CALL	0f75
c555 3e35    	LD	A,35
c557 d331    	OUT	31,A		; system control port (2)
c559 af      	XOR	A
c55a d353    	OUT	53,A		; screen superposition control
c55c 18e7    	JR	e7		; c545
c55e 3e37    	LD	A,37
c560 1802    	JR	02		; c564
c562 3e3f    	LD	A,3f
c564 f3      	DI
c565 225fb1  	LD	(b15f),HL
c568 d331    	OUT	31,A		; system control port (2)
c56a 3e01    	LD	A,01
c56c 57      	LD	D,A
c56d 3251b1  	LD	b151,A
c570 010000  	LD	BC,0000
c573 ed4352b1	LD	(b152),BC
c577 3e0b    	LD	A,0b
c579 85      	ADD	A,L
c57a 6f      	LD	L,A
c57b 2255b1  	LD	(b155),HL
c57e 7e      	LD	A,(HL)
c57f fe0b    	CP	0b
c581 3805    	JR	C,05		; c588
c583 3e02    	LD	A,02
c585 3251b1  	LD	b151,A
c588 2b      	DEC	HL
c589 7e      	LD	A,(HL)
c58a 5f      	LD	E,A
c58b 26ba    	LD	H,ba
c58d 6f      	LD	L,A
c58e 7e      	LD	A,(HL)
c58f f5      	PUSH	AF
c590 fecc    	CP	cc
c592 2812    	JR	Z,12		; c5a6
c594 cdd1b5  	CALL	b5d1
c597 14      	INC	D
c598 7a      	LD	A,D
c599 fe04    	CP	04
c59b 2006    	JR	NZ,06		; c5a3
c59d 15      	DEC	D
c59e cdfab5  	CALL	b5fa
c5a1 1601    	LD	D,01
c5a3 f1      	POP	AF
c5a4 18e4    	JR	e4		    ; c58a
c5a6 f1      	POP	AF
c5a7 cdd1b5  	CALL	b5d1
c5aa cdfab5  	CALL	b5fa
c5ad 3a54b1  	LD	A,(b154)
c5b0 fe01    	CP	01
c5b2 2808    	JR	Z,08		; c5bc
c5b4 3a03b0  	LD	A,(b003)
c5b7 fe00    	CP	00
c5b9 c410b8  	CALL	NZ,b810
c5bc af      	XOR	A
c5bd 3254b1  	LD	b154,A
c5c0 3e37    	LD	A,37
c5c2 d331    	OUT	31,A		; system control port (2)
c5c4 cd64b9  	CALL	b964
c5c7 3e01    	LD	A,01
c5c9 d353    	OUT	53,A		; screen superposition control
c5cb 3e3d    	LD	A,3d
c5cd d331    	OUT	31,A		; system control port (2)
c5cf fb      	EI
c5d0 c9      	RET
c5d1 3e0e    	LD	A,0e
c5d3 cd04b0  	CALL	b004
c5d6 3e10    	LD	A,10
c5d8 cd0bb0  	CALL	b00b
c5db 3e00    	LD	A,00
c5dd cd0bb0  	CALL	b00b
c5e0 7b      	LD	A,E
c5e1 cd0bb0  	CALL	b00b
c5e4 3e01    	LD	A,01
c5e6 cd0bb0  	CALL	b00b
c5e9 210030  	LD	HL,3000
c5ec 7a      	LD	A,D
c5ed 0f      	RRCA
c5ee 0f      	RRCA
c5ef 0f      	RRCA
c5f0 0f      	RRCA
c5f1 84      	ADD	A,H
c5f2 cd0bb0  	CALL	b00b
c5f5 af      	XOR	A
c5f6 cd0bb0  	CALL	b00b
c5f9 c9      	RET
c5fa 3e15    	LD	A,15
c5fc cd04b0  	CALL	b004
c5ff 3e40    	LD	A,40
c601 cd0bb0  	CALL	b00b
c604 af      	XOR	A
c605 cd0bb0  	CALL	b00b
c608 7a      	LD	A,D
c609 0f      	RRCA
c60a 0f      	RRCA
c60b 0f      	RRCA
c60c 0f      	RRCA
c60d 4f      	LD	C,A
c60e cd0bb0  	CALL	b00b
c611 af      	XOR	A
c612 cd0bb0  	CALL	b00b
c615 2a52b1  	LD	HL,(b152)
c618 7c      	LD	A,H
c619 81      	ADD	A,C
c61a 4f      	LD	C,A
c61b cd30b0  	CALL	b030
c61e 7c      	LD	A,H
c61f b9      	CP	C
c620 20f9    	JR	NZ,f9		; c61b
c622 2252b1  	LD	(b152),HL
c625 7c      	LD	A,H
c626 fe90    	CP	90
c628 c0      	RET	NZ
c629 3a51b1  	LD	A,(b151)
c62c fe01    	CP	01
c62e c8      	RET	Z
c62f 3a03b0  	LD	A,(b003)
c632 fe00    	CP	00
c634 c410b8  	CALL	NZ,b810
c637 3e01    	LD	A,01
c639 3254b1  	LD	b154,A
c63c 3e37    	LD	A,37
c63e d331    	OUT	31,A		; system control port (2)
c640 cd64b9  	CALL	b964
c643 210000  	LD	HL,0000
c646 2252b1  	LD	(b152),HL
c649 3e03    	LD	A,03
c64b 3251b1  	LD	b151,A
c64e c9      	RET
c64f af      	XOR	A
c650 3204b7  	LD	b704,A
c653 cd8a1b  	CALL	1b8a
c656 3a96ec  	LD	A,(ec96)
c659 1864    	JR	64		    ; c6bf
c65b af      	XOR	A
c65c 3204b7  	LD	b704,A
c65f 3e5d    	LD	A,5d
c661 cd5702  	CALL	0257
c664 cd8a1b  	CALL	1b8a
c667 3a96ec  	LD	A,(ec96)
c66a fe00    	CP	00
c66c ca09b7  	JP	Z,b709
c66f fe46    	CP	46
c671 ca05b7  	JP	Z,b705
c674 fe66    	CP	66
c676 ca05b7  	JP	Z,b705
c679 fe4b    	CP	4b
c67b ca36b7  	JP	Z,b736
c67e fe6b    	CP	6b
c680 ca36b7  	JP	Z,b736
c683 fe4e    	CP	4e
c685 ca1eb7  	JP	Z,b71e
c688 fe6e    	CP	6e
c68a ca1eb7  	JP	Z,b71e
c68d fe53    	CP	53
c68f ca46b7  	JP	Z,b746
c692 fe73    	CP	73
c694 ca46b7  	JP	Z,b746
c697 fe44    	CP	44
c699 ca9eb7  	JP	Z,b79e
c69c fe64    	CP	64
c69e ca9eb7  	JP	Z,b79e
c6a1 fe42    	CP	42
c6a3 ca28b8  	JP	Z,b828
c6a6 fe62    	CP	62
c6a8 ca28b8  	JP	Z,b828
c6ab fe43    	CP	43
c6ad ca17b8  	JP	Z,b817
c6b0 fe63    	CP	63
c6b2 ca17b8  	JP	Z,b817
c6b5 fe4f    	CP	4f
c6b7 ca21b8  	JP	Z,b821
c6ba fe6f    	CP	6f
c6bc ca21b8  	JP	Z,b821
c6bf fe31    	CP	31
c6c1 3835    	JR	C,35		; c6f8
c6c3 fe3a    	CP	3a
c6c5 3031    	JR	NC,31		; c6f8
c6c7 d630    	SUB	30
c6c9 4f      	LD	C,A
c6ca 3a97ec  	LD	A,(ec97)
c6cd fe00    	CP	00
c6cf 2813    	JR	Z,13		; c6e4
c6d1 fe30    	CP	30
c6d3 3823    	JR	C,23		; c6f8
c6d5 fe3a    	CP	3a
c6d7 301f    	JR	NC,1f		; c6f8
c6d9 d630    	SUB	30
c6db 5f      	LD	E,A
c6dc af      	XOR	A
c6dd 060a    	LD	B,0a
c6df 81      	ADD	A,C
c6e0 10fd    	DJNZ	fd		; c6df
c6e2 83      	ADD	A,E
c6e3 4f      	LD	C,A
c6e4 2100bb  	LD	HL,bb00
c6e7 7e      	LD	A,(HL)
c6e8 feed    	CP	ed
c6ea 280c    	JR	Z,0c		; c6f8
c6ec feff    	CP	ff
c6ee 2808    	JR	Z,08		; c6f8
c6f0 b9      	CP	C
c6f1 c8      	RET	Z
c6f2 111000  	LD	DE,0010
c6f5 19      	ADD	HL,DE
c6f6 18ef    	JR	ef		    ; c6e7
c6f8 217fb2  	LD	HL,b27f
c6fb cd6bb1  	CALL	b16b
c6fe 3e01    	LD	A,01
c700 3204b7  	LD	b704,A
c703 c9      	RET
c704 00      	NOP
c705 f1      	POP	AF
c706 c306b5  	JP	b506
c709 3e3d    	LD	A,3d
c70b d331    	OUT	31,A		; system control port (2)
c70d 3e01    	LD	A,01
c70f d353    	OUT	53,A		; screen superposition control
c711 cd750f  	CALL	0f75
c714 3e35    	LD	A,35
c716 d331    	OUT	31,A		; system control port (2)
c718 af      	XOR	A
c719 d353    	OUT	53,A		; screen superposition control
c71b c35bb6  	JP	b65b
c71e cd88b7  	CALL	b788
c721 e5      	PUSH	HL
c722 2173b2  	LD	HL,b273
c725 cd6bb1  	CALL	b16b
c728 cd8a1b  	CALL	1b8a
c72b e1      	POP	HL
c72c cd99b3  	CALL	b399
c72f cdbbb4  	CALL	b4bb
c732 f1      	POP	AF
c733 c306b5  	JP	b506
c736 cd88b7  	CALL	b788
c739 cd85b4  	CALL	b485
c73c cda0b4  	CALL	b4a0
c73f cdbbb4  	CALL	b4bb
c742 f1      	POP	AF
c743 c306b5  	JP	b506
c746 cd88b7  	CALL	b788
c749 eb      	EX	DE,HL
c74a d5      	PUSH	DE
c74b 3e1e    	LD	A,1e
c74d cd5702  	CALL	0257
c750 3e1c    	LD	A,1c
c752 060a    	LD	B,0a
c754 cd5702  	CALL	0257
c757 10fb    	DJNZ	fb		; c754
c759 cd88b7  	CALL	b788
c75c d1      	POP	DE
c75d e5      	PUSH	HL
c75e d5      	PUSH	DE
c75f 23      	INC	HL
c760 13      	INC	DE
c761 060f    	LD	B,0f
c763 1a      	LD	A,(DE)
c764 4f      	LD	C,A
c765 7e      	LD	A,(HL)
c766 12      	LD	(DE),A
c767 71      	LD	(HL),C
c768 23      	INC	HL
c769 13      	INC	DE
c76a 10f7    	DJNZ	f7		; c763
c76c d1      	POP	DE
c76d e1      	POP	HL
c76e 7c      	LD	A,H
c76f d610    	SUB	10
c771 67      	LD	H,A
c772 7a      	LD	A,D
c773 d610    	SUB	10
c775 57      	LD	D,A
c776 0610    	LD	B,10
c778 1a      	LD	A,(DE)
c779 4f      	LD	C,A
c77a 7e      	LD	A,(HL)
c77b 12      	LD	(DE),A
c77c 71      	LD	(HL),C
c77d 23      	INC	HL
c77e 13      	INC	DE
c77f 10f7    	DJNZ	f7		; c778
c781 cdbbb4  	CALL	b4bb
c784 f1      	POP	AF
c785 c306b5  	JP	b506
c788 216db2  	LD	HL,b26d
c78b cd6bb1  	CALL	b16b
c78e cd4fb6  	CALL	b64f
c791 3a04b7  	LD	A,(b704)
c794 fe01    	CP	01
c796 2801    	JR	Z,01		; c799
c798 c9      	RET
c799 f1      	POP	AF
c79a f1      	POP	AF
c79b c345b5  	JP	b545
c79e 3e01    	LD	A,01
c7a0 1803    	JR	03		    ; c7a5
c7a2 3a03b0  	LD	A,(b003)
c7a5 47      	LD	B,A
c7a6 af      	XOR	A
c7a7 3203b0  	LD	b003,A
c7aa 3261b1  	LD	b161,A
c7ad cdd20b  	CALL	0bd2
c7b0 0e01    	LD	C,01
c7b2 c5      	PUSH	BC
c7b3 cdceb7  	CALL	b7ce
c7b6 3e01    	LD	A,01
c7b8 3261b1  	LD	b161,A
c7bb cd5eb5  	CALL	b55e
c7be c1      	POP	BC
c7bf 78      	LD	A,B
c7c0 3203b0  	LD	b003,A
c7c3 0c      	INC	C
c7c4 c5      	PUSH	BC
c7c5 cdceb7  	CALL	b7ce
c7c8 cd62b5  	CALL	b562
c7cb c1      	POP	BC
c7cc 18f5    	JR	f5		    ; c7c3
c7ce 2100bb  	LD	HL,bb00
c7d1 7e      	LD	A,(HL)
c7d2 feed    	CP	ed
c7d4 280c    	JR	Z,0c		; c7e2
c7d6 feff    	CP	ff
c7d8 2808    	JR	Z,08		; c7e2
c7da b9      	CP	C
c7db c8      	RET	Z
c7dc 111000  	LD	DE,0010
c7df 19      	ADD	HL,DE
c7e0 18ef    	JR	ef		    ; c7d1
c7e2 3a61b1  	LD	A,(b161)
c7e5 fe00    	CP	00
c7e7 281e    	JR	Z,1e		; c807
c7e9 cd10b8  	CALL	b810
c7ec 3a03b0  	LD	A,(b003)
c7ef fecc    	CP	cc
c7f1 2810    	JR	Z,10		; c803
c7f3 3e35    	LD	A,35
c7f5 d331    	OUT	31,A		; system control port (2)
c7f7 af      	XOR	A
c7f8 d353    	OUT	53,A		; screen superposition control
c7fa 3203b0  	LD	b003,A
c7fd f1      	POP	AF
c7fe f1      	POP	AF
c7ff f1      	POP	AF
c800 c306b5  	JP	b506
c803 f1      	POP	AF
c804 f1      	POP	AF
c805 189b    	JR	9b		    ; c7a2
c807 3a03b0  	LD	A,(b003)
c80a fecc    	CP	cc
c80c 28f5    	JR	Z,f5		; c803
c80e 18e3    	JR	e3		    ; c7f3
c810 db09    	IN	A,09		; keyboard
c812 cb77    	BIT	6,A
c814 c8      	RET	Z
c815 18f9    	JR	f9		    ; c810
c817 db31    	IN	A,31		; dip switch
c819 cb7f    	BIT	7,A
c81b ca00a5  	JP	Z,a500
c81e c313a7  	JP	a713
c821 cd00a1  	CALL	a100
c824 f1      	POP	AF
c825 c306b5  	JP	b506
c828 2194b2  	LD	HL,b294
c82b cd6bb1  	CALL	b16b
c82e 218db2  	LD	HL,b28d
c831 cd6bb1  	CALL	b16b
c834 cd750f  	CALL	0f75
c837 cd5702  	CALL	0257
c83a fe59    	CP	59
c83c 2808    	JR	Z,08		; c846
c83e fe79    	CP	79
c840 2804    	JR	Z,04		; c846
c842 f1      	POP	AF
c843 c306b5  	JP	b506
c846 06ff    	LD	B,ff
c848 21008b  	LD	HL,8b00
c84b 70      	LD	(HL),B
c84c 23      	INC	HL
c84d 7c      	LD	A,H
c84e fea0    	CP	a0
c850 20f9    	JR	NZ,f9		; c84b
c852 0e01    	LD	C,01
c854 11009b  	LD	DE,9b00
c857 c5      	PUSH	BC
c858 d5      	PUSH	DE
c859 cd80b8  	CALL	b880
c85c d1      	POP	DE
c85d d5      	PUSH	DE
c85e 011000  	LD	BC,0010
c861 c5      	PUSH	BC
c862 d5      	PUSH	DE
c863 e5      	PUSH	HL
c864 edb0    	LDIR
c866 e1      	POP	HL
c867 d1      	POP	DE
c868 c1      	POP	BC
c869 7c      	LD	A,H
c86a d610    	SUB	10
c86c 67      	LD	H,A
c86d 7a      	LD	A,D
c86e d610    	SUB	10
c870 57      	LD	D,A
c871 edb0    	LDIR
c873 d1      	POP	DE
c874 c1      	POP	BC
c875 0c      	INC	C
c876 c5      	PUSH	BC
c877 011000  	LD	BC,0010
c87a eb      	EX	DE,HL
c87b 09      	ADD	HL,BC
c87c eb      	EX	DE,HL
c87d c1      	POP	BC
c87e 18d7    	JR	d7		; c857
c880 2100bb  	LD	HL,bb00
c883 7e      	LD	A,(HL)
c884 feed    	CP	ed
c886 280c    	JR	Z,0c		; c894
c888 feff    	CP	ff
c88a 2808    	JR	Z,08		; c894
c88c b9      	CP	C
c88d c8      	RET	Z
c88e 111000  	LD	DE,0010
c891 19      	ADD	HL,DE
c892 18ef    	JR	ef		; c883
c894 f1      	POP	AF
c895 d1      	POP	DE
c896 c1      	POP	BC
c897 3e05    	LD	A,05
c899 cd04b0  	CALL	b004
c89c 3e01    	LD	A,01
c89e cd0bb0  	CALL	b00b
c8a1 af      	XOR	A
c8a2 cd19b9  	CALL	b919
c8a5 0e00    	LD	C,00
c8a7 cd32b9  	CALL	b932
c8aa 3e01    	LD	A,01
c8ac cd19b9  	CALL	b919
c8af 0e01    	LD	C,01
c8b1 cd32b9  	CALL	b932
c8b4 3efe    	LD	A,fe
c8b6 32009a  	LD	9a00,A
c8b9 32019a  	LD	9a01,A
c8bc 0e02    	LD	C,02
c8be 210a9b  	LD	HL,9b0a
c8c1 7e      	LD	A,(HL)
c8c2 feff    	CP	ff
c8c4 cae1b8  	JP	Z,b8e1
c8c7 e5      	PUSH	HL
c8c8 cdd2b8  	CALL	b8d2
c8cb e1      	POP	HL
c8cc 111000  	LD	DE,0010
c8cf 19      	ADD	HL,DE
c8d0 18ef    	JR	ef		; c8c1
c8d2 71      	LD	(HL),C
c8d3 26ba    	LD	H,ba
c8d5 6f      	LD	L,A
c8d6 cd19b9  	CALL	b919
c8d9 7e      	LD	A,(HL)
c8da f5      	PUSH	AF
c8db cd32b9  	CALL	b932
c8de f1      	POP	AF
c8df 18f2    	JR	f2		; c8d3
c8e1 21009a  	LD	HL,9a00
c8e4 1100ba  	LD	DE,ba00
c8e7 015000  	LD	BC,0050
c8ea edb0    	LDIR
c8ec 21009b  	LD	HL,9b00
c8ef 1100bb  	LD	DE,bb00
c8f2 01d004  	LD	BC,04d0
c8f5 edb0    	LDIR
c8f7 21008b  	LD	HL,8b00
c8fa 1100ab  	LD	DE,ab00
c8fd 01d004  	LD	BC,04d0
c900 edb0    	LDIR
c902 3e01    	LD	A,01
c904 32c9b4  	LD	b4c9,A
c907 32efb4  	LD	b4ef,A
c90a cdbbb4  	CALL	b4bb
c90d af      	XOR	A
c90e 32c9b4  	LD	b4c9,A
c911 32efb4  	LD	b4ef,A
c914 f1      	POP	AF
c915 f1      	POP	AF
c916 c306b5  	JP	b506
c919 f5      	PUSH	AF
c91a 3e02    	LD	A,02
c91c cd04b0  	CALL	b004
c91f 3e10    	LD	A,10
c921 cd0bb0  	CALL	b00b
c924 af      	XOR	A
c925 cd0bb0  	CALL	b00b
c928 f1      	POP	AF
c929 cd0bb0  	CALL	b00b
c92c 3e01    	LD	A,01
c92e cd0bb0  	CALL	b00b
c931 c9      	RET
c932 f5      	PUSH	AF
c933 3e0f    	LD	A,0f
c935 cd04b0  	CALL	b004
c938 3e10    	LD	A,10
c93a cd0bb0  	CALL	b00b
c93d 3e01    	LD	A,01
c93f cd0bb0  	CALL	b00b
c942 79      	LD	A,C
c943 cd0bb0  	CALL	b00b
c946 3e01    	LD	A,01
c948 cd0bb0  	CALL	b00b
c94b 3e50    	LD	A,50
c94d cd0bb0  	CALL	b00b
c950 af      	XOR	A
c951 cd0bb0  	CALL	b00b
c954 f1      	POP	AF
c955 269a    	LD	H,9a
c957 69      	LD	L,C
c958 fecc    	CP	cc
c95a 2803    	JR	Z,03		; c95f
c95c 0c      	INC	C
c95d 71      	LD	(HL),C
c95e c9      	RET
c95f 77      	LD	(HL),A
c960 0c      	INC	C
c961 f1      	POP	AF
c962 f1      	POP	AF
c963 c9      	RET

;
; Plane Switching
;

c964 3a51b1  	LD	A,(b151)
c967 fe03    	CP	03
c969 2844    	JR	Z,44		; c9af
c96b 210100  	LD	HL,0001
c96e d35c    	OUT	5c,A
c970 cd8db9  	CALL	b98d
c973 d35d    	OUT	5d,A
c975 cd8db9  	CALL	b98d
c978 22f1b9  	LD	(b9f1),HL
c97b 3a51b1  	LD	A,(b151)
c97e fe02    	CP	02
c980 2805    	JR	Z,05		; c987
c982 d35e    	OUT	5e,A
c984 cd8db9  	CALL	b98d
c987 d35f    	OUT	5f,A
c989 cd50ba  	CALL	ba50
c98c c9      	RET





; --- Run-Length Encoding (RLE) Decompression Subroutine ---
; The routine at c98d is a clear example of an RLE decompressor.
; It reads a control byte. If the high bit is set, it's a compressed run.
; If the high bit is clear, it's a literal run.

:c98d 1100c0  	LD	DE,c000		; DE points to the destination buffer (VRAM).
c990 0600    	LD	B,00		; Clear B register.
:c992 7e      	LD	A,(HL)		; Read control byte from compressed data stream (HL).
c993 fe00    	CP	00			; Check for end-of-stream marker (00h).
c995 2002    	JR	NZ,02		; If not end, continue to c999.
c997 23      	INC	HL			; Move past the 00h marker.
c998 c9      	RET				; Return, decompression is finished.

:c999 cb7f    	BIT	7,A			; Test bit 7 of the control byte.
c99b 200a    	JR	NZ,0a		; If bit 7 is set (NZ), it's a compressed run. Jump to c9a7.

; --- Literal Run (Bit 7 is 0) ---
c99d 23      	INC	HL			; Point to the first byte of literal data.
c99e 47      	LD	B,A			; The control byte itself is the length of the run.
c99f 7e      	LD	A,(HL)		; Read a byte of literal data.
c9a0 12      	LD	(DE),A		; Write it to the destination buffer.
c9a1 13      	INC	DE			; Increment destination pointer.
c9a2 10fc    	DJNZ	fc		; Decrement B and loop until the run is complete.
c9a4 23      	INC	HL			; Increment source pointer past the literal data.
c9a5 18eb    	JR	eb			; Jump back to c992 to process the next control byte.

; --- Compressed Run (Bit 7 is 1) ---
:c9a7 e67f    	AND	7f			; Clear bit 7 to get the length of the run.
c9a9 23      	INC	HL			; Point to the byte that needs to be repeated.
c9aa 4f      	LD	C,A			; Store the run length in C.
c9ab edb0    	LDIR			; LDIR is not used here. The disassembler might be wrong.
                            	; A more likely sequence would be:
                             	; LD B, C       ; B = run length
                             	; LD A, (HL)    ; A = byte to repeat
                             	; :loop
                             	; LD (DE), A
                             	; INC DE
                             	; DJNZ loop
                             	; This code seems to use LDIR in a non-standard way or there's a mistake.
                             	; Assuming it's a block copy where the source is just one byte repeated.
                            	; Let's re-examine `edb0`. It's LDIR. It copies BC bytes from (HL) to (DE).
                             	; Here, BC is not set correctly. Let's look at the context.
                             	; `LD C,A` stores length in C. `LD B,0` from c990. So BC = length.
                             	; `LDIR` will copy `length` bytes from (HL) to (DE), incrementing both.
                             	; This is for a literal run, not a compressed one.
                             	; Let's re-read the logic.
                             	; `c9a7`: `AND 7f` -> A = length. `LD C,A`. `LD B,0`. BC = length.
                             	; `c9a9`: `INC HL`. HL points to the byte to be repeated.
                             	; `c9ab`: `LDIR`. This would copy `length` bytes starting from the byte-to-repeat, which is wrong for RLE.
                             	; There must be a misunderstanding. Let's look at the other RLE routine at c9b9.




c9ad 18e3    	JR	e3		    ; c992
c9af d35e    	OUT	5e,A
c9b1 2af1b9  	LD	HL,(b9f1)
c9b4 cdb9b9  	CALL	b9b9
c9b7 18ce    	JR	ce		    ; c987



;
; Main Decoding Routine
;

:c9b9 1100c0  	LD	DE,c000		; Destination buffer.

;
; Command Processing
;

c9bc 0600    	LD	B,00
c9be 7e      	LD	A,(HL)
c9bf fe00    	CP	00
c9c1 2001    	JR	NZ,01		; c9c4
c9c3 c9      	RET




; ...
:c9c4 cb7f    	BIT	7,A		    ; Test bit 7 of control byte.
c9c6 200e    	JR	NZ,0e		; Jump if compressed run.
; Literal run
;
; RLE Fill
;
c9c8 cde6b9  	CALL	b9e6	; This call must increment HL.
c9cb 47      	LD	B,A		    ; B = length.
c9cc 7e      	LD	A,(HL)		; Get data byte.
c9cd 12      	LD	(DE),A		; Store it.
c9ce 13      	INC	DE		    ; Increment destination.
c9cf 10fc    	DJNZ	fc		; Loop for `length` times.
c9d1 cde6b9  	CALL	b9e6	; Increment HL again.
c9d4 18e8    	JR	e8		    ; Loop for next packet.
; Compressed run
;
; Literal Copy
;
:c9d6 e67f    	AND	7f	        ; A = length.
c9d8 cde6b9  	CALL	b9e6	; Increment HL.
c9db 47      	LD	B,A		    ; B = length.
c9dc 7e      	LD	A,(HL)		; A = byte to repeat.
:c9dd 12      	LD	(DE),A		; Store the byte.
c9de cde6b9  	CALL	b9e6	; This is strange. Why call this in a loop?
                                ; It seems `b9e6` handles the source pointer `HL` in a special way,
                                ; perhaps wrapping around a buffer.
c9e1 13      	INC	DE	        ; Increment destination.
c9e2 10f8    	DJNZ	f8		; Loop `length` times.
c9e4 18d8    	JR	d8		    ; Loop for next packet.

; The routine at b9e6 increments HL and handles wrapping around a 64KB-4KB=60KB buffer.
:b9e6 f5      	PUSH	AF
b9e7 23      	INC	HL
b9e8 7c      	LD	A,H
b9e9 fe90    	CP	90	        ; Check if H is 90h.
b9eb 2002    	JR	NZ,02		; If not, continue.
b9ed 2600    	LD	H,00		; If it is, wrap H back to 00h.
b9ef f1      	POP	AF
b9f0 c9      	RET

; This confirms that the code from `c9b9` onwards is a sophisticated RLE decompressor that reads from a circular buffer in memory. This is a very clever technique for handling continuous data streams on a system with limited RAM.
;
; I hope this detailed breakdown is helpful! This is a great example of efficient and clever programming from the 8-bit era. Let me know if you have any more questions.









c9e6 f5      	PUSH	AF
c9e7 23      	INC	HL
c9e8 7c      	LD	A,H
c9e9 fe90    	CP	90
c9eb 2002    	JR	NZ,02		; c9ef
c9ed 2600    	LD	H,00
c9ef f1      	POP	AF
c9f0 c9      	RET

c9f1 00      	NOP
c9f2 00      	NOP
c9f3 00      	NOP
c9f4 00      	NOP
c9f5 00      	NOP
c9f6 00      	NOP
c9f7 00      	NOP
c9f8 00      	NOP
c9f9 00      	NOP
c9fa 00      	NOP
c9fb 00      	NOP
c9fc 00      	NOP
c9fd 00      	NOP
c9fe 00      	NOP
c9ff 00      	NOP

ca00 fefe    	CP	fe
ca02 03      	INC	BC
ca03 04      	INC	B
ca04 05      	DEC	B
ca05 0607    	LD	B,07
ca07 08      	EX	AF,AF'
ca08 cc0a0b  	CALL	Z,0b0a
ca0b cc4c0e  	CALL	Z,0e4c
ca0e 0f      	RRCA
ca0f 1011    	DJNZ	11		; ca22
ca11 12      	LD	(DE),A
ca12 cc1415  	CALL	Z,1514
ca15 1617    	LD	D,17
ca17 18cc    	JR	cc		; c9e5
ca19 1a      	LD	A,(DE)
ca1a 1b      	DEC	DE
ca1b 1c      	INC	E
ca1c 1d      	DEC	E
ca1d 1e1f    	LD	E,1f
ca1f cc2122  	CALL	Z,2221
ca22 23      	INC	HL
ca23 24      	INC	H
ca24 25      	DEC	H
ca25 26cc    	LD	H,cc
ca27 2829    	JR	Z,29		; ca52
ca29 2a2b2c  	LD	HL,(2c2b)
ca2c cc2e2f  	CALL	Z,2f2e
ca2f 3031    	JR	NC,31		; ca62
ca31 323334  	LD	3433,A
ca34 cc3637  	CALL	Z,3736
ca37 3839    	JR	C,39		; ca72
ca39 3acc3c  	LD	A,(3ccc)
ca3c 3d      	DEC	A
ca3d 3e3f    	LD	A,3f
ca3f 40      	LD	B,B
ca40 41      	LD	B,C
ca41 cc4344  	CALL	Z,4443
ca44 45      	LD	B,L
ca45 46      	LD	B,(HL)
ca46 47      	LD	B,A
ca47 48      	LD	C,B
ca48 49      	LD	C,C
ca49 4a      	LD	C,D
ca4a 4b      	LD	C,E
ca4b cc4d4e  	CALL	Z,4e4d
ca4e ccffdb  	CALL	Z,dbff
ca51 31cb7f  	LD	SP,7fcb
ca54 2848    	JR	Z,48		; ca9e
ca56 cd68ba  	CALL	ba68
ca59 2157b1  	LD	HL,b157
ca5c 0608    	LD	B,08
ca5e 0e54    	LD	C,54
ca60 7e      	LD	A,(HL)
ca61 ed79    	OUT	(C),A
ca63 0c      	INC	C
ca64 23      	INC	HL
ca65 10f9    	DJNZ	f9		; ca60
ca67 c9      	RET

ca68 2a55b1  	LD	HL,(b155)
ca6b 23      	INC	HL
ca6c 1157b1  	LD	DE,b157
ca6f 0604    	LD	B,04
ca71 7e      	LD	A,(HL)
ca72 f5      	PUSH	AF
ca73 e60f    	AND	0f
ca75 4f      	LD	C,A
ca76 f1      	POP	AF
ca77 0f      	RRCA
ca78 0f      	RRCA
ca79 0f      	RRCA
ca7a 0f      	RRCA
ca7b e60f    	AND	0f
ca7d 12      	LD	(DE),A
ca7e 13      	INC	DE
ca7f 79      	LD	A,C
ca80 12      	LD	(DE),A
ca81 13      	INC	DE
ca82 23      	INC	HL
ca83 10ec    	DJNZ	ec		; ca71
ca85 c9      	RET

ca86 03      	INC	BC
ca87 0604    	LD	B,04
ca89 07      	RLCA
ca8a 00      	NOP
ca8b 00      	NOP
ca8c 00      	NOP
ca8d 07      	RLCA
ca8e 00      	NOP
ca8f 07      	RLCA
ca90 07      	RLCA
ca91 00      	NOP
ca92 00      	NOP
ca93 00      	NOP
ca94 07      	RLCA
ca95 07      	RLCA
ca96 00      	NOP
ca97 07      	RLCA
ca98 00      	NOP
ca99 07      	RLCA
ca9a 07      	RLCA
ca9b 07      	RLCA
ca9c 07      	RLCA
ca9d 07      	RLCA

ca9e cdc4ba  	CALL	bac4
caa1 db32    	IN	A,32
caa3 f620    	OR	20
caa5 d332    	OUT	32,A
caa7 2186ba  	LD	HL,ba86
caaa 0608    	LD	B,08
caac 0e54    	LD	C,54
caae 7e      	LD	A,(HL)
caaf 57      	LD	D,A
cab0 23      	INC	HL
cab1 7e      	LD	A,(HL)
cab2 07      	RLCA
cab3 07      	RLCA
cab4 07      	RLCA
cab5 b2      	OR	D
cab6 ed79    	OUT	(C),A
cab8 23      	INC	HL
cab9 1640    	LD	D,40
cabb 7e      	LD	A,(HL)
cabc b2      	OR	D
cabd ed79    	OUT	(C),A
cabf 23      	INC	HL
cac0 0c      	INC	C
cac1 10eb    	DJNZ	eb		; caae
cac3 c9      	RET
cac4 2a5fb1  	LD	HL,(b15f)
cac7 7c      	LD	A,H
cac8 d610    	SUB	10
caca 67      	LD	H,A
cacb 1186ba  	LD	DE,ba86
cace 0608    	LD	B,08
cad0 7e      	LD	A,(HL)
cad1 f5      	PUSH	AF
cad2 e607    	AND	07
cad4 12      	LD	(DE),A
cad5 13      	INC	DE
cad6 f1      	POP	AF
cad7 0f      	RRCA
cad8 0f      	RRCA
cad9 0f      	RRCA
cada e607    	AND	07
cadc 12      	LD	(DE),A
cadd 23      	INC	HL
cade 13      	INC	DE
cadf 0ebf    	LD	C,bf
cae1 7e      	LD	A,(HL)
cae2 a1      	AND	C
cae3 12      	LD	(DE),A
cae4 23      	INC	HL
cae5 13      	INC	DE
cae6 10e8    	DJNZ	e8		; cad0
cae8 c9      	RET

cae9 00      	NOP
caea 00      	NOP
caeb 00      	NOP
caec 00      	NOP
caed 00      	NOP
caee 00      	NOP
caef 00      	NOP
caf0 00      	NOP
caf1 00      	NOP
caf2 00      	NOP
caf3 00      	NOP
caf4 00      	NOP
caf5 00      	NOP
caf6 00      	NOP
caf7 00      	NOP
caf8 00      	NOP
caf9 00      	NOP
cafa 00      	NOP
cafb 00      	NOP
cafc 00      	NOP
cafd 00      	NOP
cafe 00      	NOP
caff 00      	NOP

cb00 01bbde  	LD	BC,debb
cb03 c5      	PUSH	BC
cb04 c4dea9  	CALL	NZ,a9de
cb07 b0      	OR	B
cb08 2020    	JR	NZ,20		; cb2a
cb0a 42      	LD	B,D
cb0b 0a      	LD	A,(BC)
cb0c 012345  	LD	BC,4523
cb0f 67      	LD	H,A
cb10 02      	LD	(BC),A
cb11 c6ad    	ADD	A,ad
cb13 b0      	OR	B
cb14 c4dbdd  	CALL	NZ,dddb
cb17 2020    	JR	NZ,20		; cb39
cb19 2002    	JR	NZ,02		; cb1d
cb1b 07      	RLCA
cb1c 71      	LD	(HL),C
cb1d 23      	INC	HL
cb1e 45      	LD	B,L
cb1f 60      	LD	H,B
cb20 03      	INC	BC
cb21 dbb0    	IN	A,b0
cb23 c4ded7  	CALL	NZ,d7de
cb26 ddc5    	???
cb28 b0      	OR	B
cb29 2009    	JR	NZ,09		; cb34
cb2b 03      	INC	BC
cb2c 012345  	LD	BC,4523
cb2f 67      	LD	H,A
cb30 04      	INC	B
cb31 d7      	RST	10H
cb32 ccdfc3  	CALL	Z,c3df
cb35 af      	XOR	A
cb36 b8      	CP	B
cb37 2020    	JR	NZ,20		; cb59
cb39 200c    	JR	NZ,0c		; cb47
cb3b 04      	INC	B
cb3c 012345  	LD	BC,4523
cb3f 67      	LD	H,A
cb40 05      	DEC	B
cb41 c4deb0  	CALL	NZ,b0de
cb44 c5      	PUSH	BC
cb45 c22020  	JP	NZ,2020
cb48 2020    	JR	NZ,20		; cb6a
cb4a 19      	ADD	HL,DE
cb4b 07      	RLCA
cb4c 012345  	LD	BC,4523
cb4f 67      	LD	H,A
cb50 06bc    	LD	B,bc
cb52 ad      	XOR	L
cb53 d8      	RET	C
cb54 b9      	CP	C
cb55 dd20    	???
cb57 2020    	JR	NZ,20		; cb79
cb59 2020    	JR	NZ,20		; cb7b
cb5b 07      	RLCA
cb5c 012345  	LD	BC,4523
cb5f 67      	LD	H,A
cb60 07      	RLCA
cb61 b1      	OR	C
cb62 b5      	OR	L
cb63 cebc    	ADC	A,bc
cb65 2020    	JR	NZ,20		; cb87
cb67 2020    	JR	NZ,20		; cb89
cb69 2027    	JR	NZ,27		; cb92
cb6b 0601    	LD	B,01
cb6d 23      	INC	HL
cb6e 45      	LD	B,L
cb6f 67      	LD	H,A
cb70 08      	EX	AF,AF'
cb71 c2b8b4  	JP	NZ,b4b8
cb74 2020    	JR	NZ,20		; cb96
cb76 2020    	JR	NZ,20		; cb98
cb78 2020    	JR	NZ,20		; cb9a
cb7a 2d      	DEC	L
cb7b 08      	EX	AF,AF'
cb7c 012345  	LD	BC,4523
cb7f 67      	LD	H,A
cb80 09      	ADD	HL,BC
cb81 b4      	OR	H
cb82 ddc2    	???
cb84 c2d620  	JP	NZ,20d6
cb87 2020    	JR	NZ,20		; cba9
cb89 2013    	JR	NZ,13		; cb9e
cb8b 0601    	LD	B,01
cb8d 23      	INC	HL
cb8e 45      	LD	B,L
cb8f 67      	LD	H,A
cb90 0a      	LD	A,(BC)
cb91 b4      	OR	H
cb92 ddc2    	???
cb94 c22020  	JP	NZ,2020
cb97 2020    	JR	NZ,20		; cbb9
cb99 200d    	JR	NZ,0d		; cba8
cb9b 0601    	LD	B,01
cb9d 23      	INC	HL
cb9e 45      	LD	B,L
cb9f 67      	LD	H,A
cba0 0b      	DEC	BC
cba1 c0      	RET	NZ
cba2 cf      	RST	08H
cba3 c4b3d2  	CALL	NZ,d2b3
cba6 b2      	OR	D
cba7 2020    	JR	NZ,20		; cbc9
cba9 203b    	JR	NZ,3b		; cbe6
cbab 07      	RLCA
cbac 012345  	LD	BC,4523
cbaf 67      	LD	H,A
cbb0 0c      	INC	C
cbb1 c0      	RET	NZ
cbb2 cf      	RST	08H
cbb3 2020    	JR	NZ,20		; cbd5
cbb5 2020    	JR	NZ,20		; cbd7
cbb7 2020    	JR	NZ,20		; cbd9
cbb9 2035    	JR	NZ,35		; cbf0
cbbb 0601    	LD	B,01
cbbd 23      	INC	HL
cbbe 45      	LD	B,L
cbbf 67      	LD	H,A

cbc0 ff      	RST	38H
cbc1 ff      	RST	38H
cbc2 ff      	RST	38H
cbc3 ff      	RST	38H
cbc4 ff      	RST	38H
cbc5 ff      	RST	38H
cbc6 ff      	RST	38H
cbc7 ff      	RST	38H
cbc8 ff      	RST	38H
cbc9 ff      	RST	38H
cbca ff      	RST	38H
cbcb ff      	RST	38H
cbcc ff      	RST	38H
cbcd ff      	RST	38H
cbce ff      	RST	38H
cbcf ff      	RST	38H
cbd0 ff      	RST	38H
cbd1 ff      	RST	38H
cbd2 ff      	RST	38H
cbd3 ff      	RST	38H
cbd4 ff      	RST	38H
cbd5 ff      	RST	38H
cbd6 ff      	RST	38H
cbd7 ff      	RST	38H
cbd8 ff      	RST	38H
cbd9 ff      	RST	38H
cbda ff      	RST	38H
cbdb ff      	RST	38H
cbdc ff      	RST	38H
cbdd ff      	RST	38H
cbde ff      	RST	38H
cbdf ff      	RST	38H
cbe0 ff      	RST	38H
cbe1 ff      	RST	38H
cbe2 ff      	RST	38H
cbe3 ff      	RST	38H
cbe4 ff      	RST	38H
cbe5 ff      	RST	38H
cbe6 ff      	RST	38H
cbe7 ff      	RST	38H
cbe8 ff      	RST	38H
cbe9 ff      	RST	38H
cbea ff      	RST	38H
cbeb ff      	RST	38H
cbec ff      	RST	38H
cbed ff      	RST	38H
cbee ff      	RST	38H
cbef ff      	RST	38H
cbf0 ff      	RST	38H
cbf1 ff      	RST	38H
cbf2 ff      	RST	38H
cbf3 ff      	RST	38H
cbf4 ff      	RST	38H
cbf5 ff      	RST	38H
cbf6 ff      	RST	38H
cbf7 ff      	RST	38H
cbf8 ff      	RST	38H
cbf9 ff      	RST	38H
cbfa ff      	RST	38H
cbfb ff      	RST	38H
cbfc ff      	RST	38H
cbfd ff      	RST	38H
cbfe ff      	RST	38H
cbff ff      	RST	38H
cc00 ff      	RST	38H
cc01 ff      	RST	38H
cc02 ff      	RST	38H
cc03 ff      	RST	38H
cc04 ff      	RST	38H
cc05 ff      	RST	38H
cc06 ff      	RST	38H
cc07 ff      	RST	38H
cc08 ff      	RST	38H
cc09 ff      	RST	38H
cc0a ff      	RST	38H
cc0b ff      	RST	38H
cc0c ff      	RST	38H
cc0d ff      	RST	38H
cc0e ff      	RST	38H
cc0f ff      	RST	38H
cc10 ff      	RST	38H
cc11 ff      	RST	38H
cc12 ff      	RST	38H
cc13 ff      	RST	38H
cc14 ff      	RST	38H
cc15 ff      	RST	38H
cc16 ff      	RST	38H
cc17 ff      	RST	38H
cc18 ff      	RST	38H
cc19 ff      	RST	38H
cc1a ff      	RST	38H
cc1b ff      	RST	38H
cc1c ff      	RST	38H
cc1d ff      	RST	38H
cc1e ff      	RST	38H
cc1f ff      	RST	38H
cc20 ff      	RST	38H
cc21 ff      	RST	38H
cc22 ff      	RST	38H
cc23 ff      	RST	38H
cc24 ff      	RST	38H
cc25 ff      	RST	38H
cc26 ff      	RST	38H
cc27 ff      	RST	38H
cc28 ff      	RST	38H
cc29 ff      	RST	38H
cc2a ff      	RST	38H
cc2b ff      	RST	38H
cc2c ff      	RST	38H
cc2d ff      	RST	38H
cc2e ff      	RST	38H
cc2f ff      	RST	38H
cc30 ff      	RST	38H
cc31 ff      	RST	38H
cc32 ff      	RST	38H
cc33 ff      	RST	38H
cc34 ff      	RST	38H
cc35 ff      	RST	38H
cc36 ff      	RST	38H
cc37 ff      	RST	38H
cc38 ff      	RST	38H
cc39 ff      	RST	38H
cc3a ff      	RST	38H
cc3b ff      	RST	38H
cc3c ff      	RST	38H
cc3d ff      	RST	38H
cc3e ff      	RST	38H
cc3f ff      	RST	38H
cc40 ff      	RST	38H
cc41 ff      	RST	38H
cc42 ff      	RST	38H
cc43 ff      	RST	38H
cc44 ff      	RST	38H
cc45 ff      	RST	38H
cc46 ff      	RST	38H
cc47 ff      	RST	38H
cc48 ff      	RST	38H
cc49 ff      	RST	38H
cc4a ff      	RST	38H
cc4b ff      	RST	38H
cc4c ff      	RST	38H
cc4d ff      	RST	38H
cc4e ff      	RST	38H
cc4f ff      	RST	38H
cc50 ff      	RST	38H
cc51 ff      	RST	38H
cc52 ff      	RST	38H
cc53 ff      	RST	38H
cc54 ff      	RST	38H
cc55 ff      	RST	38H
cc56 ff      	RST	38H
cc57 ff      	RST	38H
cc58 ff      	RST	38H
cc59 ff      	RST	38H
cc5a ff      	RST	38H
cc5b ff      	RST	38H
cc5c ff      	RST	38H
cc5d ff      	RST	38H
cc5e ff      	RST	38H
cc5f ff      	RST	38H
cc60 ff      	RST	38H
cc61 ff      	RST	38H
cc62 ff      	RST	38H
cc63 ff      	RST	38H
cc64 ff      	RST	38H
cc65 ff      	RST	38H
cc66 ff      	RST	38H
cc67 ff      	RST	38H
cc68 ff      	RST	38H
cc69 ff      	RST	38H
cc6a ff      	RST	38H
cc6b ff      	RST	38H
cc6c ff      	RST	38H
cc6d ff      	RST	38H
cc6e ff      	RST	38H
cc6f ff      	RST	38H
cc70 ff      	RST	38H
cc71 ff      	RST	38H
cc72 ff      	RST	38H
cc73 ff      	RST	38H
cc74 ff      	RST	38H
cc75 ff      	RST	38H
cc76 ff      	RST	38H
cc77 ff      	RST	38H
cc78 ff      	RST	38H
cc79 ff      	RST	38H
cc7a ff      	RST	38H
cc7b ff      	RST	38H
cc7c ff      	RST	38H
cc7d ff      	RST	38H
cc7e ff      	RST	38H
cc7f ff      	RST	38H
cc80 ff      	RST	38H
cc81 ff      	RST	38H
cc82 ff      	RST	38H
cc83 ff      	RST	38H
cc84 ff      	RST	38H
cc85 ff      	RST	38H
cc86 ff      	RST	38H
cc87 ff      	RST	38H
cc88 ff      	RST	38H
cc89 ff      	RST	38H
cc8a ff      	RST	38H
cc8b ff      	RST	38H
cc8c ff      	RST	38H
cc8d ff      	RST	38H
cc8e ff      	RST	38H
cc8f ff      	RST	38H
cc90 ff      	RST	38H
cc91 ff      	RST	38H
cc92 ff      	RST	38H
cc93 ff      	RST	38H
cc94 ff      	RST	38H
cc95 ff      	RST	38H
cc96 ff      	RST	38H
cc97 ff      	RST	38H
cc98 ff      	RST	38H
cc99 ff      	RST	38H
cc9a ff      	RST	38H
cc9b ff      	RST	38H
cc9c ff      	RST	38H
cc9d ff      	RST	38H
cc9e ff      	RST	38H
cc9f ff      	RST	38H
cca0 ff      	RST	38H
cca1 ff      	RST	38H
cca2 ff      	RST	38H
cca3 ff      	RST	38H
cca4 ff      	RST	38H
cca5 ff      	RST	38H
cca6 ff      	RST	38H
cca7 ff      	RST	38H
cca8 ff      	RST	38H
cca9 ff      	RST	38H
ccaa ff      	RST	38H
ccab ff      	RST	38H
ccac ff      	RST	38H
ccad ff      	RST	38H
ccae ff      	RST	38H
ccaf ff      	RST	38H
ccb0 ff      	RST	38H
ccb1 ff      	RST	38H
ccb2 ff      	RST	38H
ccb3 ff      	RST	38H
ccb4 ff      	RST	38H
ccb5 ff      	RST	38H
ccb6 ff      	RST	38H
ccb7 ff      	RST	38H
ccb8 ff      	RST	38H
ccb9 ff      	RST	38H
ccba ff      	RST	38H
ccbb ff      	RST	38H
ccbc ff      	RST	38H
ccbd ff      	RST	38H
ccbe ff      	RST	38H
ccbf ff      	RST	38H
ccc0 ff      	RST	38H
ccc1 ff      	RST	38H
ccc2 ff      	RST	38H
ccc3 ff      	RST	38H
ccc4 ff      	RST	38H
ccc5 ff      	RST	38H
ccc6 ff      	RST	38H
ccc7 ff      	RST	38H
ccc8 ff      	RST	38H
ccc9 ff      	RST	38H
ccca ff      	RST	38H
cccb ff      	RST	38H
cccc ff      	RST	38H
cccd ff      	RST	38H
ccce ff      	RST	38H
cccf ff      	RST	38H
ccd0 ff      	RST	38H
ccd1 ff      	RST	38H
ccd2 ff      	RST	38H
ccd3 ff      	RST	38H
ccd4 ff      	RST	38H
ccd5 ff      	RST	38H
ccd6 ff      	RST	38H
ccd7 ff      	RST	38H
ccd8 ff      	RST	38H
ccd9 ff      	RST	38H
ccda ff      	RST	38H
ccdb ff      	RST	38H
ccdc ff      	RST	38H
ccdd ff      	RST	38H
ccde ff      	RST	38H
ccdf ff      	RST	38H
cce0 edff    	???
cce2 ff      	RST	38H
cce3 ff      	RST	38H
cce4 ff      	RST	38H
cce5 ff      	RST	38H
cce6 ff      	RST	38H
cce7 ff      	RST	38H
cce8 ff      	RST	38H
cce9 ff      	RST	38H
ccea ff      	RST	38H
cceb ff      	RST	38H
ccec ff      	RST	38H
cced ff      	RST	38H
ccee ff      	RST	38H
ccef ff      	RST	38H
ccf0 86      	ADD	A,(HL)
ccf1 03      	INC	BC
ccf2 01ffff  	LD	BC,ffff
ccf5 ff      	RST	38H
ccf6 ff      	RST	38H
ccf7 ff      	RST	38H
ccf8 ff      	RST	38H
ccf9 ff      	RST	38H
ccfa ff      	RST	38H
ccfb ff      	RST	38H
ccfc ff      	RST	38H
ccfd ff      	RST	38H
ccfe ff      	RST	38H
ccff 00      	NOP

cd00 f3      	DI
cd01 3e3d    	LD	A,3d
cd03 d331    	OUT	31,A		; system control port (2)
cd05 21c0a4  	LD	HL,a4c0
cd08 7e      	LD	A,(HL)
cd09 feff    	CP	ff
cd0b 2806    	JR	Z,06		; cd13
cd0d cda9a4  	CALL	a4a9
cd10 23      	INC	HL
cd11 18f5    	JR	f5		; cd08
cd13 2130fe  	LD	HL,fe30
cd16 0650    	LD	B,50
cd18 0ec8    	LD	C,c8
cd1a e5      	PUSH	HL
cd1b 21d0a4  	LD	HL,a4d0
cd1e 7e      	LD	A,(HL)
cd1f feff    	CP	ff
cd21 2806    	JR	Z,06		; cd29
cd23 cda9a4  	CALL	a4a9
cd26 23      	INC	HL
cd27 18f5    	JR	f5		; cd1e
cd29 e1      	POP	HL
cd2a af      	XOR	A
cd2b c5      	PUSH	BC
cd2c e5      	PUSH	HL
cd2d 0604    	LD	B,04
cd2f 219da4  	LD	HL,a49d
cd32 77      	LD	(HL),A
cd33 23      	INC	HL
cd34 10fc    	DJNZ	fc		; cd32
cd36 e1      	POP	HL
cd37 e5      	PUSH	HL
cd38 d35c    	OUT	5c,A
cd3a 7e      	LD	A,(HL)
cd3b 4f      	LD	C,A
cd3c d35d    	OUT	5d,A
cd3e 7e      	LD	A,(HL)
cd3f 57      	LD	D,A
cd40 d35e    	OUT	5e,A
cd42 7e      	LD	A,(HL)
cd43 5f      	LD	E,A
cd44 0608    	LD	B,08
cd46 af      	XOR	A
cd47 cb01    	RLC	C
cd49 17      	RLA
cd4a cb02    	RLC	D
cd4c 17      	RLA
cd4d cb03    	RLC	E
cd4f 17      	RLA
cd50 21a1a4  	LD	HL,a4a1
cd53 85      	ADD	A,L
cd54 6f      	LD	L,A
cd55 7e      	LD	A,(HL)
cd56 219da4  	LD	HL,a49d
cd59 0f      	RRCA
cd5a cb1e    	RR	(HL)
cd5c 23      	INC	HL
cd5d 0f      	RRCA
cd5e cb1e    	RR	(HL)
cd60 23      	INC	HL
cd61 0f      	RRCA
cd62 cb1e    	RR	(HL)
cd64 23      	INC	HL
cd65 0f      	RRCA
cd66 cb1e    	RR	(HL)
cd68 10dc    	DJNZ	dc		; cd46
cd6a 219da4  	LD	HL,a49d
cd6d 0604    	LD	B,04
cd6f 7e      	LD	A,(HL)
cd70 cda9a4  	CALL	a4a9
cd73 23      	INC	HL
cd74 10f9    	DJNZ	f9		; cd6f
cd76 e1      	POP	HL
cd77 c1      	POP	BC
cd78 11b0ff  	LD	DE,ffb0
cd7b 19      	ADD	HL,DE
cd7c db09    	IN	A,09		; keyboard
cd7e e601    	AND	01
cd80 2813    	JR	Z,13		; cd95
cd82 0d      	DEC	C
cd83 20a5    	JR	NZ,a5		; cd2a
cd85 3e0d    	LD	A,0d
cd87 cda9a4  	CALL	a4a9
cd8a 3e0a    	LD	A,0a
cd8c cda9a4  	CALL	a4a9
cd8f 11813e  	LD	DE,3e81
cd92 19      	ADD	HL,DE
cd93 1083    	DJNZ	83		; cd18
cd95 d35f    	OUT	5f,A
cd97 3e35    	LD	A,35
cd99 d331    	OUT	31,A		; system control port (2)
cd9b fb      	EI
cd9c c9      	RET

cd9d 00      	NOP
cd9e 00      	NOP
cd9f 00      	NOP
cda0 00      	NOP
cda1 0f      	RRCA
cda2 0e0c    	LD	C,0c
cda4 0a      	LD	A,(BC)
cda5 05      	DEC	B
cda6 02      	LD	(BC),A
cda7 0100f5  	LD	BC,f500
cdaa db40    	IN	A,40		; strobe port
cdac 0f      	RRCA
cdad 38fb    	JR	C,fb		; cdaa
cdaf f1      	POP	AF
cdb0 d310    	OUT	10,A		; printer, calendar clock output data
cdb2 3a67ea  	LD	A,(ea67)
cdb5 e6de    	AND	de
cdb7 d340    	OUT	40,A		; strobe port
cdb9 f601    	OR	01
cdbb d340    	OUT	40,A		; strobe port
cdbd c9      	RET

cdbe 00      	NOP
cdbf 00      	NOP
cdc0 1b      	DEC	DE
cdc1 3e1b    	LD	A,1b
cdc3 54      	LD	D,H
cdc4 31361b  	LD	SP,1b36
cdc7 4d      	LD	C,L
cdc8 ff      	RST	38H
cdc9 00      	NOP
cdca 00      	NOP
cdcb 00      	NOP
cdcc 00      	NOP
cdcd 00      	NOP
cdce 00      	NOP
cdcf 00      	NOP
cdd0 1b      	DEC	DE
cdd1 53      	LD	D,E
cdd2 3038    	JR	NC,38		; ce0c
cdd4 3030    	JR	NC,30		; ce06
cdd6 ff      	RST	38H
cdd7 00      	NOP
cdd8 00      	NOP
cdd9 00      	NOP
cdda 00      	NOP
cddb 00      	NOP
cddc 00      	NOP
cddd 00      	NOP
cdde 00      	NOP
cddf 00      	NOP
cde0 00      	NOP
cde1 00      	NOP
cde2 00      	NOP
cde3 00      	NOP
cde4 00      	NOP
cde5 00      	NOP
cde6 00      	NOP
cde7 00      	NOP
cde8 00      	NOP
cde9 00      	NOP
cdea 00      	NOP
cdeb 00      	NOP
cdec 00      	NOP
cded 00      	NOP
cdee 00      	NOP
cdef 00      	NOP
cdf0 00      	NOP
cdf1 00      	NOP
cdf2 00      	NOP
cdf3 00      	NOP
cdf4 00      	NOP
cdf5 00      	NOP
cdf6 00      	NOP
cdf7 00      	NOP
cdf8 00      	NOP
cdf9 00      	NOP
cdfa 00      	NOP
cdfb 00      	NOP
cdfc 00      	NOP
cdfd 00      	NOP
cdfe 00      	NOP
cdff 00      	NOP

ce00 2a55b1  	LD	HL,(b155)
ce03 7c      	LD	A,H
ce04 fe00    	CP	00
ce06 2009    	JR	NZ,09		; ce11
ce08 217fb2  	LD	HL,b27f
ce0b cd6bb1  	CALL	b16b
ce0e c35bb6  	JP	b65b
ce11 cdd20b  	CALL	0bd2
ce14 3e3d    	LD	A,3d
ce16 d331    	OUT	31,A		; system control port (2)
ce18 cd5a04  	CALL	045a
ce1b cdc4ba  	CALL	bac4
ce1e af      	XOR	A
ce1f 3282a6  	LD	a682,A
ce22 3283a6  	LD	a683,A
ce25 21d8fc  	LD	HL,fcd8
ce28 3e5b    	LD	A,5b
ce2a 77      	LD	(HL),A
ce2b 2c      	INC	L
ce2c 2c      	INC	L
ce2d 3e5d    	LD	A,5d
ce2f 77      	LD	(HL),A
ce30 cd99a6  	CALL	a699
ce33 21defc  	LD	HL,fcde
ce36 3e42    	LD	A,42
ce38 77      	LD	(HL),A
ce39 cd86a6  	CALL	a686
ce3c 3e52    	LD	A,52
ce3e 77      	LD	(HL),A
ce3f cd86a6  	CALL	a686
ce42 3e47    	LD	A,47
ce44 77      	LD	(HL),A
ce45 cd86a6  	CALL	a686
ce48 af      	XOR	A
ce49 3228fd  	LD	fd28,A
ce4c 3e04    	LD	A,04
ce4e 3229fd  	LD	fd29,A
ce51 3e03    	LD	A,03
ce53 322afd  	LD	fd2a,A
ce56 af      	XOR	A
ce57 322bfd  	LD	fd2b,A
ce5a 3e05    	LD	A,05
ce5c cdffa6  	CALL	a6ff
ce5f 1869    	JR	69		; ceca
ce61 cd9eba  	CALL	ba9e
ce64 3e35    	LD	A,35
ce66 d331    	OUT	31,A		; system control port (2)
ce68 f1      	POP	AF
ce69 c306b5  	JP	b506
ce6c 2186ba  	LD	HL,ba86
ce6f ed5b5fb1	LD	DE,(b15f)
ce73 7a      	LD	A,D
ce74 d610    	SUB	10
ce76 57      	LD	D,A
ce77 0608    	LD	B,08
ce79 7e      	LD	A,(HL)
ce7a 4f      	LD	C,A
ce7b 23      	INC	HL
ce7c 7e      	LD	A,(HL)
ce7d 07      	RLCA
ce7e 07      	RLCA
ce7f 07      	RLCA
ce80 b1      	OR	C
ce81 12      	LD	(DE),A
ce82 23      	INC	HL
ce83 13      	INC	DE
ce84 7e      	LD	A,(HL)
ce85 f640    	OR	40
ce87 12      	LD	(DE),A
ce88 23      	INC	HL
ce89 13      	INC	DE
ce8a 10ed    	DJNZ	ed		; ce79
ce8c cdbbb4  	CALL	b4bb
ce8f 3e35    	LD	A,35
ce91 d331    	OUT	31,A		; system control port (2)
ce93 f1      	POP	AF
ce94 c306b5  	JP	b506
ce97 3a82a6  	LD	A,(a682)
ce9a fe02    	CP	02
ce9c 2801    	JR	Z,01		; ce9f
ce9e 3c      	INC	A
ce9f 3282a6  	LD	a682,A
cea2 180b    	JR	0b		; ceaf
cea4 3a82a6  	LD	A,(a682)
cea7 fe00    	CP	00
cea9 2801    	JR	Z,01		; ceac
ceab 3d      	DEC	A
ceac 3282a6  	LD	a682,A
ceaf fe00    	CP	00
ceb1 2007    	JR	NZ,07		; ceba
ceb3 3e05    	LD	A,05
ceb5 cdffa6  	CALL	a6ff
ceb8 1810    	JR	10		; ceca
ceba fe01    	CP	01
cebc 2007    	JR	NZ,07		; cec5
cebe 3e13    	LD	A,13
cec0 cdffa6  	CALL	a6ff
cec3 1805    	JR	05		; ceca
cec5 3e21    	LD	A,21
cec7 cdffa6  	CALL	a6ff
ceca 06e0    	LD	B,e0
cecc c5      	PUSH	BC
cecd 06ff    	LD	B,ff
cecf 40      	LD	B,B
ced0 10fd    	DJNZ	fd		; cecf
ced2 c1      	POP	BC
ced3 10f7    	DJNZ	f7		; cecc
ced5 db09    	IN	A,09		; keyboard
ced7 47      	LD	B,A
ced8 db00    	IN	A,00		; keyboard
ceda 4f      	LD	C,A
cedb db01    	IN	A,01		; keyboard
cedd 57      	LD	D,A
cede db05    	IN	A,05		; keyboard
cee0 5f      	LD	E,A
cee1 cb40    	BIT	0,B
cee3 ca61a5  	JP	Z,a561
cee6 cb7a    	BIT	7,D
cee8 2882    	JR	Z,82		; ce6c
ceea cb42    	BIT	0,D
ceec 2816    	JR	Z,16		; cf04
ceee cb51    	BIT	2,C
cef0 2822    	JR	Z,22		; cf14
cef2 cb61    	BIT	4,C
cef4 28ae    	JR	Z,ae		; cea4
cef6 cb71    	BIT	6,C
cef8 289d    	JR	Z,9d		; ce97
cefa cb43    	BIT	0,E
cefc 2826    	JR	Z,26		; cf24
cefe cb53    	BIT	2,E
cf00 2854    	JR	Z,54		; cf56
cf02 18d1    	JR	d1		; ced5
cf04 3a83a6  	LD	A,(a683)
cf07 fe07    	CP	07
cf09 2801    	JR	Z,01		; cf0c
cf0b 3c      	INC	A
cf0c 3283a6  	LD	a683,A
cf0f cd99a6  	CALL	a699
cf12 18b6    	JR	b6		; ceca
cf14 3a83a6  	LD	A,(a683)
cf17 fe00    	CP	00
cf19 2801    	JR	Z,01		; cf1c
cf1b 3d      	DEC	A
cf1c 3283a6  	LD	a683,A
cf1f cd99a6  	CALL	a699
cf22 18a6    	JR	a6		; ceca
cf24 2a84a6  	LD	HL,(a684)
cf27 e5      	PUSH	HL
cf28 3a82a6  	LD	A,(a682)
cf2b 85      	ADD	A,L
cf2c 6f      	LD	L,A
cf2d e5      	PUSH	HL
cf2e 7e      	LD	A,(HL)
cf2f 3c      	INC	A
cf30 fe08    	CP	08
cf32 2002    	JR	NZ,02		; cf36
cf34 3e07    	LD	A,07
cf36 e1      	POP	HL
cf37 77      	LD	(HL),A
cf38 3a83a6  	LD	A,(a683)
cf3b c654    	ADD	A,54
cf3d 4f      	LD	C,A
cf3e e1      	POP	HL
cf3f 7e      	LD	A,(HL)
cf40 57      	LD	D,A
cf41 23      	INC	HL
cf42 7e      	LD	A,(HL)
cf43 07      	RLCA
cf44 07      	RLCA
cf45 07      	RLCA
cf46 b2      	OR	D
cf47 ed79    	OUT	(C),A
cf49 23      	INC	HL
cf4a 1640    	LD	D,40
cf4c 7e      	LD	A,(HL)
cf4d b2      	OR	D
cf4e ed79    	OUT	(C),A
cf50 cd99a6  	CALL	a699
cf53 c3caa5  	JP	a5ca
cf56 2a84a6  	LD	HL,(a684)
cf59 e5      	PUSH	HL
cf5a 3a82a6  	LD	A,(a682)
cf5d 85      	ADD	A,L
cf5e 6f      	LD	L,A
cf5f e5      	PUSH	HL
cf60 7e      	LD	A,(HL)
cf61 fe00    	CP	00
cf63 2801    	JR	Z,01		; cf66
cf65 3d      	DEC	A
cf66 e1      	POP	HL
cf67 77      	LD	(HL),A
cf68 18ce    	JR	ce		; cf38

cf6a 00      	NOP
cf6b 00      	NOP
cf6c 00      	NOP
cf6d 07      	RLCA
cf6e 00      	NOP
cf6f 00      	NOP
cf70 00      	NOP
cf71 07      	RLCA
cf72 00      	NOP
cf73 07      	RLCA
cf74 07      	RLCA
cf75 00      	NOP
cf76 00      	NOP
cf77 00      	NOP
cf78 07      	RLCA
cf79 07      	RLCA
cf7a 00      	NOP
cf7b 07      	RLCA
cf7c 00      	NOP
cf7d 07      	RLCA
cf7e 07      	RLCA
cf7f 07      	RLCA
cf80 07      	RLCA
cf81 07      	RLCA
cf82 00      	NOP
cf83 00      	NOP
cf84 00      	NOP
cf85 00      	NOP

cf86 23      	INC	HL
cf87 3e3a    	LD	A,3a
cf89 77      	LD	(HL),A
cf8a 23      	INC	HL
cf8b 0608    	LD	B,08
cf8d 3e30    	LD	A,30
cf8f 77      	LD	(HL),A
cf90 3c      	INC	A
cf91 23      	INC	HL
cf92 10fb    	DJNZ	fb		; cf8f
cf94 23      	INC	HL
cf95 23      	INC	HL
cf96 23      	INC	HL
cf97 23      	INC	HL
cf98 c9      	RET
cf99 1160fc  	LD	DE,fc60
cf9c cd5104  	CALL	0451
cf9f 21d9fc  	LD	HL,fcd9
cfa2 3a83a6  	LD	A,(a683)
cfa5 4f      	LD	C,A
cfa6 c630    	ADD	A,30
cfa8 77      	LD	(HL),A
cfa9 0603    	LD	B,03
cfab cdeba6  	CALL	a6eb
cfae eb      	EX	DE,HL
cfaf d5      	PUSH	DE
cfb0 216aa6  	LD	HL,a66a
cfb3 19      	ADD	HL,DE
cfb4 0603    	LD	B,03
cfb6 1168fc  	LD	DE,fc68
cfb9 d5      	PUSH	DE
cfba 7e      	LD	A,(HL)
cfbb 83      	ADD	A,E
cfbc 5f      	LD	E,A
cfbd 3e2a    	LD	A,2a
cfbf 12      	LD	(DE),A
cfc0 23      	INC	HL
cfc1 d1      	POP	DE
cfc2 3e0e    	LD	A,0e
cfc4 83      	ADD	A,E
cfc5 5f      	LD	E,A
cfc6 10f1    	DJNZ	f1		; cfb9
cfc8 1150fd  	LD	DE,fd50
cfcb cd5104  	CALL	0451
cfce d1      	POP	DE
cfcf 2186ba  	LD	HL,ba86
cfd2 19      	ADD	HL,DE
cfd3 2284a6  	LD	(a684),HL
cfd6 0603    	LD	B,03
cfd8 1158fd  	LD	DE,fd58
cfdb d5      	PUSH	DE
cfdc 7e      	LD	A,(HL)
cfdd 83      	ADD	A,E
cfde 5f      	LD	E,A
cfdf 3e2a    	LD	A,2a
cfe1 12      	LD	(DE),A
cfe2 23      	INC	HL
cfe3 d1      	POP	DE
cfe4 3e0e    	LD	A,0e
cfe6 83      	ADD	A,E
cfe7 5f      	LD	E,A
cfe8 10f1    	DJNZ	f1		; cfdb
cfea c9      	RET

cfeb af      	XOR	A
cfec 57      	LD	D,A
cfed 67      	LD	H,A
cfee 6f      	LD	L,A
cfef 58      	LD	E,B
cff0 0608    	LD	B,08
cff2 cb09    	RRC	C
cff4 3001    	JR	NC,01		; cff7
cff6 19      	ADD	HL,DE
cff7 cb23    	SLA	E
cff9 cb12    	RL	D
cffb 05      	DEC	B
cffc 20f4    	JR	NZ,f4		; cff2
cffe c9      	RET
cfff f5      	PUSH	AF
d000 322cfd  	LD	fd2c,A
d003 3e04    	LD	A,04
d005 322dfd  	LD	fd2d,A
d008 f1      	POP	AF
d009 c60c    	ADD	A,0c
d00b 322efd  	LD	fd2e,A
d00e af      	XOR	A
d00f 322ffd  	LD	fd2f,A
d012 c9      	RET

d013 2a55b1  	LD	HL,(b155)
d016 7c      	LD	A,H
d017 fe00    	CP	00
d019 2009    	JR	NZ,09		; d024
d01b 217fb2  	LD	HL,b27f
d01e cd6bb1  	CALL	b16b
d021 c35bb6  	JP	b65b
d024 3e3d    	LD	A,3d
d026 d331    	OUT	31,A		; system control port (2)
d028 0608    	LD	B,08
d02a 3e30    	LD	A,30
d02c cd5702  	CALL	0257
d02f f5      	PUSH	AF
d030 3e20    	LD	A,20
d032 cd5702  	CALL	0257
d035 f1      	POP	AF
d036 3c      	INC	A
d037 10f3    	DJNZ	f3		; d02c
d039 2117b2  	LD	HL,b217
d03c cd6bb1  	CALL	b16b
d03f cd68ba  	CALL	ba68
d042 0608    	LD	B,08
d044 2157b1  	LD	HL,b157
d047 7e      	LD	A,(HL)
d048 0e30    	LD	C,30
d04a 81      	ADD	A,C
d04b cd5702  	CALL	0257
d04e 3e20    	LD	A,20
d050 cd5702  	CALL	0257
d053 23      	INC	HL
d054 10f1    	DJNZ	f1		; d047
d056 2117b2  	LD	HL,b217
d059 cd6bb1  	CALL	b16b
d05c 216db2  	LD	HL,b26d
d05f cd6bb1  	CALL	b16b
d062 cd750f  	CALL	0f75
d065 cd5702  	CALL	0257
d068 fe03    	CP	03
d06a cadda7  	JP	Z,a7dd
d06d fe0d    	CP	0d
d06f 284a    	JR	Z,4a		; d0bb
d071 cda9a7  	CALL	a7a9
d074 0e24    	LD	C,24
d076 81      	ADD	A,C
d077 4f      	LD	C,A
d078 3e1c    	LD	A,1c
d07a 0604    	LD	B,04
d07c cd5702  	CALL	0257
d07f 10fb    	DJNZ	fb		; d07c
d081 216db2  	LD	HL,b26d
d084 cd6bb1  	CALL	b16b
d087 cd750f  	CALL	0f75
d08a cd5702  	CALL	0257
d08d cda9a7  	CALL	a7a9
d090 1630    	LD	D,30
d092 92      	SUB	D
d093 ed79    	OUT	(C),A
d095 f5      	PUSH	AF
d096 2157b1  	LD	HL,b157
d099 79      	LD	A,C
d09a 1654    	LD	D,54
d09c 92      	SUB	D
d09d 85      	ADD	A,L
d09e 6f      	LD	L,A
d09f f1      	POP	AF
d0a0 77      	LD	(HL),A
d0a1 2117b2  	LD	HL,b217
d0a4 cd6bb1  	CALL	b16b
d0a7 18b3    	JR	b3		; d05c
d0a9 fe30    	CP	30
d0ab 3805    	JR	C,05		; d0b2
d0ad fe38    	CP	38
d0af 3001    	JR	NC,01		; d0b2
d0b1 c9      	RET

d0b2 f1      	POP	AF
d0b3 2117b2  	LD	HL,b217
d0b6 cd6bb1  	CALL	b16b
d0b9 18a1    	JR	a1		; d05c
d0bb 0604    	LD	B,04
d0bd 2a55b1  	LD	HL,(b155)
d0c0 23      	INC	HL
d0c1 1157b1  	LD	DE,b157
d0c4 1a      	LD	A,(DE)
d0c5 0f      	RRCA
d0c6 0f      	RRCA
d0c7 0f      	RRCA
d0c8 0f      	RRCA
d0c9 4f      	LD	C,A
d0ca 13      	INC	DE
d0cb 1a      	LD	A,(DE)
d0cc b1      	OR	C
d0cd 77      	LD	(HL),A
d0ce 23      	INC	HL
d0cf 13      	INC	DE
d0d0 10f2    	DJNZ	f2		; d0c4
d0d2 cdbbb4  	CALL	b4bb
d0d5 3e35    	LD	A,35
d0d7 d331    	OUT	31,A		; system control port (2)
d0d9 f1      	POP	AF
d0da c306b5  	JP	b506
d0dd cd68ba  	CALL	ba68
d0e0 cd56ba  	CALL	ba56
d0e3 18f0    	JR	f0		; d0d5

d0e5 00      	NOP
d0e6 00      	NOP
d0e7 00      	NOP
d0e8 00      	NOP
d0e9 00      	NOP
d0ea 00      	NOP
d0eb 00      	NOP
d0ec 00      	NOP
d0ed 00      	NOP
d0ee 00      	NOP
d0ef 00      	NOP
d0f0 00      	NOP
d0f1 00      	NOP
d0f2 00      	NOP
d0f3 00      	NOP
d0f4 00      	NOP
d0f5 00      	NOP
d0f6 00      	NOP
d0f7 00      	NOP
d0f8 00      	NOP
d0f9 00      	NOP
d0fa 00      	NOP
d0fb 00      	NOP
d0fc 00      	NOP
d0fd 00      	NOP
d0fe 00      	NOP
d0ff 00      	NOP
d100 c307a8  	JP	a807
d103 00      	NOP
d104 00      	NOP
d105 00      	NOP
d106 00      	NOP

d107 110000  	LD	DE,0000
d10a ed5303a8	LD	(a803),DE
d10e af      	XOR	A
d10f 320000  	LD	0000,A
d112 110100  	LD	DE,0001
d115 d35c    	OUT	5c,A
d117 cd8ea8  	CALL	a88e
d11a d35d    	OUT	5d,A
d11c cd8ea8  	CALL	a88e
d11f 1b      	DEC	DE
d120 eb      	EX	DE,HL
d121 110010  	LD	DE,1000
d124 0600    	LD	B,00
d126 7c      	LD	A,H
d127 fe10    	CP	10
d129 3806    	JR	C,06		; d131
d12b a7      	AND	A
d12c ed52    	SBC	HL,DE
d12e 04      	INC	B
d12f 18f5    	JR	f5		; d126
d131 78      	LD	A,B
d132 fe00    	CP	00
d134 2820    	JR	Z,20		; d156
d136 cdb2a9  	CALL	a9b2
d139 ed5b03a8	LD	DE,(a803)
d13d 3e10    	LD	A,10
d13f 82      	ADD	A,D
d140 57      	LD	D,A
d141 ed5303a8	LD	(a803),DE
d145 10ef    	DJNZ	ef		; d136
d147 44      	LD	B,H
d148 4d      	LD	C,L
d149 03      	INC	BC
d14a eb      	EX	DE,HL
d14b 110000  	LD	DE,0000
d14e ed5303a8	LD	(a803),DE
d152 edb0    	LDIR
d154 1802    	JR	02		; d158
d156 eb      	EX	DE,HL
d157 13      	INC	DE
d158 d35e    	OUT	5e,A
d15a cd8ea8  	CALL	a88e
d15d 1b      	DEC	DE
d15e eb      	EX	DE,HL
d15f 110010  	LD	DE,1000
d162 0600    	LD	B,00
d164 7c      	LD	A,H
d165 fe10    	CP	10
d167 3806    	JR	C,06		; d16f
d169 a7      	AND	A
d16a ed52    	SBC	HL,DE
d16c 04      	INC	B
d16d 18f5    	JR	f5		; d164
d16f 78      	LD	A,B
d170 fe00    	CP	00
d172 2803    	JR	Z,03		; d177
d174 04      	INC	B
d175 1802    	JR	02		; d179
d177 0601    	LD	B,01
d179 cdb2a9  	CALL	a9b2
d17c ed5b03a8	LD	DE,(a803)
d180 3e10    	LD	A,10
d182 82      	ADD	A,D
d183 57      	LD	D,A
d184 ed5303a8	LD	(a803),DE
d188 10ef    	DJNZ	ef		; d179
d18a 12      	LD	(DE),A
d18b d35f    	OUT	5f,A
d18d c9      	RET
d18e 2100c0  	LD	HL,c000
d191 cd41a9  	CALL	a941
d194 0602    	LD	B,02
d196 7e      	LD	A,(HL)
d197 ed5305a8	LD	(a805),DE
d19b 13      	INC	DE
d19c 12      	LD	(DE),A
d19d 4f      	LD	C,A
d19e cd56a9  	CALL	a956
d1a1 7e      	LD	A,(HL)
d1a2 b9      	CP	C
d1a3 c2c9a8  	JP	NZ,a8c9
d1a6 cd01a9  	CALL	a901
d1a9 4f      	LD	C,A
d1aa 7e      	LD	A,(HL)
d1ab b9      	CP	C
d1ac 2005    	JR	NZ,05		; d1b3
d1ae cd7ca9  	CALL	a97c
d1b1 18f3    	JR	f3		; d1a6
d1b3 d5      	PUSH	DE
d1b4 f5      	PUSH	AF
d1b5 ed5b05a8	LD	DE,(a805)
d1b9 78      	LD	A,B
d1ba 12      	LD	(DE),A
d1bb f1      	POP	AF
d1bc d1      	POP	DE
d1bd 0601    	LD	B,01
d1bf 13      	INC	DE
d1c0 ed5305a8	LD	(a805),DE
d1c4 13      	INC	DE
d1c5 12      	LD	(DE),A
d1c6 c3cba8  	JP	a8cb
d1c9 13      	INC	DE
d1ca 12      	LD	(DE),A
d1cb 4f      	LD	C,A
d1cc cd20a9  	CALL	a920
d1cf 7e      	LD	A,(HL)
d1d0 b9      	CP	C
d1d1 c2f3a8  	JP	NZ,a8f3
d1d4 f5      	PUSH	AF
d1d5 05      	DEC	B
d1d6 78      	LD	A,B
d1d7 fe00    	CP	00
d1d9 2820    	JR	Z,20		; d1fb
d1db f1      	POP	AF
d1dc d5      	PUSH	DE
d1dd f5      	PUSH	AF
d1de ed5b05a8	LD	DE,(a805)
d1e2 78      	LD	A,B
d1e3 f680    	OR	80
d1e5 12      	LD	(DE),A
d1e6 f1      	POP	AF
d1e7 d1      	POP	DE
d1e8 ed5305a8	LD	(a805),DE
d1ec 13      	INC	DE
d1ed 12      	LD	(DE),A
d1ee 0602    	LD	B,02
d1f0 c3a6a8  	JP	a8a6
d1f3 13      	INC	DE
d1f4 12      	LD	(DE),A
d1f5 cd96a9  	CALL	a996
d1f8 c3cba8  	JP	a8cb
d1fb f1      	POP	AF
d1fc 0602    	LD	B,02
d1fe c3a6a8  	JP	a8a6
d201 23      	INC	HL
d202 f5      	PUSH	AF
d203 7c      	LD	A,H
d204 fefe    	CP	fe
d206 c27aa9  	JP	NZ,a97a
d209 7d      	LD	A,L
d20a fe80    	CP	80
d20c c27aa9  	JP	NZ,a97a
d20f f1      	POP	AF
d210 d5      	PUSH	DE
d211 f5      	PUSH	AF
d212 ed5b05a8	LD	DE,(a805)
d216 78      	LD	A,B
d217 12      	LD	(DE),A
d218 f1      	POP	AF
d219 d1      	POP	DE
d21a 13      	INC	DE
d21b af      	XOR	A
d21c 12      	LD	(DE),A
d21d 13      	INC	DE
d21e f1      	POP	AF
d21f c9      	RET
d220 23      	INC	HL
d221 f5      	PUSH	AF
d222 7c      	LD	A,H
d223 fefe    	CP	fe
d225 c27aa9  	JP	NZ,a97a
d228 7d      	LD	A,L
d229 fe80    	CP	80
d22b c27aa9  	JP	NZ,a97a
d22e f1      	POP	AF
d22f d5      	PUSH	DE
d230 f5      	PUSH	AF
d231 ed5b05a8	LD	DE,(a805)
d235 78      	LD	A,B
d236 f680    	OR	80
d238 12      	LD	(DE),A
d239 f1      	POP	AF
d23a d1      	POP	DE
d23b 13      	INC	DE
d23c af      	XOR	A
d23d 12      	LD	(DE),A
d23e 13      	INC	DE
d23f f1      	POP	AF
d240 c9      	RET
d241 7c      	LD	A,H
d242 fefe    	CP	fe
d244 c0      	RET	NZ
d245 7d      	LD	A,L
d246 fe7f    	CP	7f
d248 c0      	RET	NZ
d249 3e81    	LD	A,81
d24b 12      	LD	(DE),A
d24c 13      	INC	DE
d24d 7e      	LD	A,(HL)
d24e 12      	LD	(DE),A
d24f 13      	INC	DE
d250 af      	XOR	A
d251 12      	LD	(DE),A
d252 13      	INC	DE
d253 23      	INC	HL
d254 f1      	POP	AF
d255 c9      	RET
d256 23      	INC	HL
d257 f5      	PUSH	AF
d258 7c      	LD	A,H
d259 fefe    	CP	fe
d25b c27aa9  	JP	NZ,a97a
d25e 7d      	LD	A,L
d25f fe7f    	CP	7f
d261 c27aa9  	JP	NZ,a97a
d264 f1      	POP	AF
d265 d5      	PUSH	DE
d266 f5      	PUSH	AF
d267 ed5b05a8	LD	DE,(a805)
d26b 3e82    	LD	A,82
d26d 12      	LD	(DE),A
d26e f1      	POP	AF
d26f d1      	POP	DE
d270 13      	INC	DE
d271 7e      	LD	A,(HL)
d272 12      	LD	(DE),A
d273 13      	INC	DE
d274 af      	XOR	A
d275 12      	LD	(DE),A
d276 13      	INC	DE
d277 23      	INC	HL
d278 f1      	POP	AF
d279 c9      	RET
d27a f1      	POP	AF
d27b c9      	RET
d27c 04      	INC	B
d27d f5      	PUSH	AF
d27e 78      	LD	A,B
d27f fe7f    	CP	7f
d281 2802    	JR	Z,02		; d285
d283 f1      	POP	AF
d284 c9      	RET
d285 f1      	POP	AF
d286 d5      	PUSH	DE
d287 f5      	PUSH	AF
d288 ed5b05a8	LD	DE,(a805)
d28c 78      	LD	A,B
d28d 12      	LD	(DE),A
d28e f1      	POP	AF
d28f d1      	POP	DE
d290 23      	INC	HL
d291 13      	INC	DE
d292 f1      	POP	AF
d293 c391a8  	JP	a891
d296 04      	INC	B
d297 f5      	PUSH	AF
d298 78      	LD	A,B
d299 fe7f    	CP	7f
d29b 2802    	JR	Z,02		; d29f
d29d f1      	POP	AF
d29e c9      	RET
d29f f1      	POP	AF
d2a0 d5      	PUSH	DE
d2a1 f5      	PUSH	AF
d2a2 ed5b05a8	LD	DE,(a805)
d2a6 78      	LD	A,B
d2a7 f680    	OR	80
d2a9 12      	LD	(DE),A
d2aa f1      	POP	AF
d2ab d1      	POP	DE
d2ac 23      	INC	HL
d2ad 13      	INC	DE
d2ae f1      	POP	AF
d2af c391a8  	JP	a891
d2b2 e5      	PUSH	HL
d2b3 c5      	PUSH	BC
d2b4 16ba    	LD	D,ba
d2b6 3a45b1  	LD	A,(b145)
d2b9 5f      	LD	E,A
d2ba 1a      	LD	A,(DE)
d2bb feff    	CP	ff
d2bd 2818    	JR	Z,18		; d2d7
d2bf 13      	INC	DE
d2c0 7b      	LD	A,E
d2c1 fe50    	CP	50
d2c3 20f5    	JR	NZ,f5		; d2ba
d2c5 3a47b1  	LD	A,(b147)
d2c8 fe01    	CP	01
d2ca 2066    	JR	NZ,66		; d332
d2cc 3e01    	LD	A,01
d2ce 3246b1  	LD	b146,A
d2d1 c1      	POP	BC
d2d2 e1      	POP	HL
d2d3 f1      	POP	AF
d2d4 c38ba8  	JP	a88b
d2d7 3a47b1  	LD	A,(b147)
d2da fe01    	CP	01
d2dc 2842    	JR	Z,42		; d320
d2de 3ecc    	LD	A,cc
d2e0 12      	LD	(DE),A
d2e1 2a43b1  	LD	HL,(b143)
d2e4 7d      	LD	A,L
d2e5 2e0a    	LD	L,0a
d2e7 85      	ADD	A,L
d2e8 6f      	LD	L,A
d2e9 7b      	LD	A,E
d2ea 77      	LD	(HL),A
d2eb 3245b1  	LD	b145,A
d2ee 57      	LD	D,A
d2ef 3e11    	LD	A,11
d2f1 cd04b0  	CALL	b004
d2f4 3e10    	LD	A,10
d2f6 cd0bb0  	CALL	b00b
d2f9 af      	XOR	A
d2fa cd0bb0  	CALL	b00b
d2fd 7a      	LD	A,D
d2fe cd0bb0  	CALL	b00b
d301 3e01    	LD	A,01
d303 cd0bb0  	CALL	b00b
d306 2a03a8  	LD	HL,(a803)
d309 010010  	LD	BC,1000
d30c cd55b0  	CALL	b055
d30f 0b      	DEC	BC
d310 0b      	DEC	BC
d311 78      	LD	A,B
d312 b1      	OR	C
d313 20f7    	JR	NZ,f7		; d30c
d315 3e01    	LD	A,01
d317 3247b1  	LD	b147,A
d31a 08      	EX	AF,AF'
d31b 3c      	INC	A
d31c 08      	EX	AF,AF'
d31d c1      	POP	BC
d31e e1      	POP	HL
d31f c9      	RET
d320 26ba    	LD	H,ba
d322 3a45b1  	LD	A,(b145)
d325 6f      	LD	L,A
d326 7b      	LD	A,E
d327 77      	LD	(HL),A
d328 3245b1  	LD	b145,A
d32b f5      	PUSH	AF
d32c 3ecc    	LD	A,cc
d32e 12      	LD	(DE),A
d32f f1      	POP	AF
d330 18bc    	JR	bc		; d2ee
d332 3ecc    	LD	A,cc
d334 12      	LD	(DE),A
d335 2a43b1  	LD	HL,(b143)
d338 7d      	LD	A,L
d339 2e0a    	LD	L,0a
d33b 85      	ADD	A,L
d33c 6f      	LD	L,A
d33d 7b      	LD	A,E
d33e 77      	LD	(HL),A
d33f c315aa  	JP	aa15

d342 00      	NOP
d343 00      	NOP
d344 00      	NOP
d345 00      	NOP
d346 00      	NOP
d347 00      	NOP
d348 00      	NOP
d349 00      	NOP
d34a 00      	NOP
d34b 00      	NOP
d34c 00      	NOP
d34d 00      	NOP
d34e 00      	NOP
d34f 00      	NOP
d350 00      	NOP
d351 00      	NOP
d352 00      	NOP
d353 00      	NOP
d354 00      	NOP
d355 00      	NOP
d356 00      	NOP
d357 00      	NOP
d358 00      	NOP
d359 00      	NOP
d35a 00      	NOP
d35b 00      	NOP
d35c 00      	NOP
d35d 00      	NOP
d35e 00      	NOP
d35f 00      	NOP
d360 00      	NOP
d361 00      	NOP
d362 00      	NOP
d363 00      	NOP
d364 00      	NOP
d365 00      	NOP
d366 00      	NOP
d367 00      	NOP
d368 00      	NOP
d369 00      	NOP
d36a 00      	NOP
d36b 00      	NOP
d36c 00      	NOP
d36d 00      	NOP
d36e 00      	NOP
d36f 00      	NOP

d370 43      	LD	B,E
d371 2e47    	LD	L,47
d373 2e00    	LD	L,00
d375 4d      	LD	C,L
d376 61      	LD	H,C
d377 74      	LD	(HL),H
d378 65      	LD	H,L
d379 00      	NOP
d37a 56      	LD	D,(HL)
d37b 65      	LD	H,L
d37c 72      	LD	(HL),D
d37d 312e30  	LD	SP,302e

d380 00      	NOP
d381 00      	NOP
d382 00      	NOP
d383 00      	NOP
d384 00      	NOP
d385 00      	NOP
d386 00      	NOP
d387 00      	NOP
d388 00      	NOP
d389 00      	NOP
d38a 00      	NOP
d38b 00      	NOP
d38c 00      	NOP
d38d 00      	NOP
d38e 00      	NOP
d38f 00      	NOP

d390 2843    	JR	Z,43		; d3d5
d392 29      	ADD	HL,HL
d393 4f      	LD	C,A
d394 52      	LD	D,D
d395 41      	LD	B,C
d396 4e      	LD	C,(HL)
d397 47      	LD	B,A
d398 45      	LD	B,L
d399 2042    	JR	NZ,42		; d3dd
d39b 4f      	LD	C,A
d39c 58      	LD	E,B

d39d 00      	NOP
d39e 00      	NOP
d39f 00      	NOP
d3a0 00      	NOP
d3a1 00      	NOP
d3a2 00      	NOP
d3a3 00      	NOP
d3a4 00      	NOP
d3a5 00      	NOP
d3a6 00      	NOP
d3a7 00      	NOP
d3a8 00      	NOP
d3a9 00      	NOP
d3aa 00      	NOP
d3ab 00      	NOP
d3ac 00      	NOP
d3ad 00      	NOP
d3ae 00      	NOP
d3af 00      	NOP
d3b0 00      	NOP
d3b1 00      	NOP
d3b2 00      	NOP
d3b3 00      	NOP

d3b4 48      	LD	C,B
d3b5 2e49    	LD	L,49
d3b7 6d      	LD	L,L
d3b8 61      	LD	H,C
d3b9 69      	LD	L,C

d3ba 00      	NOP
d3bb 00      	NOP
d3bc 00      	NOP
d3bd 00      	NOP
d3be 00      	NOP
d3bf 00      	NOP
d3c0 00      	NOP
d3c1 00      	NOP
d3c2 00      	NOP
d3c3 00      	NOP
d3c4 00      	NOP
d3c5 00      	NOP
d3c6 00      	NOP
d3c7 00      	NOP
d3c8 00      	NOP
d3c9 00      	NOP
d3ca 00      	NOP
d3cb 00      	NOP
d3cc 00      	NOP
d3cd 00      	NOP
d3ce 00      	NOP
d3cf 00      	NOP
d3d0 00      	NOP
d3d1 00      	NOP
d3d2 00      	NOP
d3d3 00      	NOP
d3d4 00      	NOP
d3d5 00      	NOP
d3d6 00      	NOP
d3d7 00      	NOP
d3d8 00      	NOP
d3d9 00      	NOP
d3da 00      	NOP
d3db 00      	NOP
d3dc 00      	NOP
d3dd 00      	NOP
d3de 00      	NOP
d3df 00      	NOP
d3e0 00      	NOP

d3e1 313938  	LD	SP,3839
d3e4 3600    	LD	(HL),00

d3e6 00      	NOP
d3e7 00      	NOP
d3e8 00      	NOP
d3e9 00      	NOP
d3ea 00      	NOP
d3eb 00      	NOP
d3ec 00      	NOP
d3ed 00      	NOP
d3ee 00      	NOP
d3ef 00      	NOP

d3f0 53      	LD	D,E
d3f1 4f      	LD	C,A
d3f2 46      	LD	B,(HL)
d3f3 54      	LD	D,H
d3f4 2054    	JR	NZ,54		; d44a
d3f6 4f      	LD	C,A
d3f7 57      	LD	D,A
d3f8 4e      	LD	C,(HL)
d3f9 202f    	JR	NZ,2f		; d42a
d3fb 2047    	JR	NZ,47		; d444
d3fd 49      	LD	C,C
d3fe 46      	LD	B,(HL)
d3ff 55      	LD	D,L
d400 00      	NOP
d401 40      	LD	B,B
d402 07      	RLCA
d403 40      	LD	B,B
d404 3840    	JR	C,40		; d446
d406 3f      	CCF
d407 40      	LD	B,B
d408 00      	NOP
d409 47      	LD	B,A
d40a 07      	RLCA
d40b 47      	LD	B,A
d40c 3847    	JR	C,47		; d455
d40e 3f      	CCF
d40f 47      	LD	B,A
d410 00      	NOP
d411 40      	LD	B,B
d412 07      	RLCA
d413 40      	LD	B,B
d414 3840    	JR	C,40		; d456
d416 3f      	CCF
d417 40      	LD	B,B
d418 00      	NOP
d419 47      	LD	B,A
d41a 07      	RLCA
d41b 47      	LD	B,A
d41c 3847    	JR	C,47		; d465
d41e 3f      	CCF
d41f 47      	LD	B,A
d420 00      	NOP
d421 40      	LD	B,B
d422 07      	RLCA
d423 40      	LD	B,B
d424 3840    	JR	C,40		; d466
d426 3f      	CCF
d427 40      	LD	B,B
d428 00      	NOP
d429 47      	LD	B,A
d42a 07      	RLCA
d42b 47      	LD	B,A
d42c 3847    	JR	C,47		; d475
d42e 3f      	CCF
d42f 47      	LD	B,A
d430 00      	NOP
d431 40      	LD	B,B
d432 07      	RLCA
d433 40      	LD	B,B
d434 3840    	JR	C,40		; d476
d436 3f      	CCF
d437 40      	LD	B,B
d438 00      	NOP
d439 47      	LD	B,A
d43a 07      	RLCA
d43b 47      	LD	B,A
d43c 3847    	JR	C,47		; d485
d43e 3f      	CCF
d43f 47      	LD	B,A
d440 00      	NOP
d441 40      	LD	B,B
d442 07      	RLCA
d443 40      	LD	B,B
d444 3840    	JR	C,40		; d486
d446 3f      	CCF
d447 40      	LD	B,B
d448 00      	NOP
d449 47      	LD	B,A
d44a 07      	RLCA
d44b 47      	LD	B,A
d44c 3847    	JR	C,47		; d495
d44e 3f      	CCF
d44f 47      	LD	B,A
d450 00      	NOP
d451 40      	LD	B,B
d452 07      	RLCA
d453 40      	LD	B,B
d454 3840    	JR	C,40		; d496
d456 3f      	CCF
d457 40      	LD	B,B
d458 00      	NOP
d459 47      	LD	B,A
d45a 07      	RLCA
d45b 47      	LD	B,A
d45c 3847    	JR	C,47		; d4a5
d45e 3f      	CCF
d45f 47      	LD	B,A
d460 00      	NOP
d461 40      	LD	B,B
d462 07      	RLCA
d463 40      	LD	B,B
d464 3840    	JR	C,40		; d4a6
d466 3f      	CCF
d467 40      	LD	B,B
d468 00      	NOP
d469 47      	LD	B,A
d46a 07      	RLCA
d46b 47      	LD	B,A
d46c 3847    	JR	C,47		; d4b5
d46e 3f      	CCF
d46f 47      	LD	B,A
d470 00      	NOP
d471 40      	LD	B,B
d472 07      	RLCA
d473 40      	LD	B,B
d474 3840    	JR	C,40		; d4b6
d476 3f      	CCF
d477 40      	LD	B,B
d478 00      	NOP
d479 47      	LD	B,A
d47a 07      	RLCA
d47b 47      	LD	B,A
d47c 3847    	JR	C,47		; d4c5
d47e 3f      	CCF
d47f 47      	LD	B,A
d480 00      	NOP
d481 40      	LD	B,B
d482 07      	RLCA
d483 40      	LD	B,B
d484 3840    	JR	C,40		; d4c6
d486 3f      	CCF
d487 40      	LD	B,B
d488 00      	NOP
d489 47      	LD	B,A
d48a 07      	RLCA
d48b 47      	LD	B,A
d48c 3847    	JR	C,47		; d4d5
d48e 3f      	CCF
d48f 47      	LD	B,A
d490 00      	NOP
d491 40      	LD	B,B
d492 07      	RLCA
d493 40      	LD	B,B
d494 3840    	JR	C,40		; d4d6
d496 3f      	CCF
d497 40      	LD	B,B
d498 00      	NOP
d499 47      	LD	B,A
d49a 07      	RLCA
d49b 47      	LD	B,A
d49c 3847    	JR	C,47		; d4e5
d49e 3f      	CCF
d49f 47      	LD	B,A
d4a0 00      	NOP
d4a1 40      	LD	B,B
d4a2 07      	RLCA
d4a3 40      	LD	B,B
d4a4 3840    	JR	C,40		; d4e6
d4a6 3f      	CCF
d4a7 40      	LD	B,B
d4a8 00      	NOP
d4a9 47      	LD	B,A
d4aa 07      	RLCA
d4ab 47      	LD	B,A
d4ac 3847    	JR	C,47		; d4f5
d4ae 3f      	CCF
d4af 47      	LD	B,A
d4b0 00      	NOP
d4b1 40      	LD	B,B
d4b2 07      	RLCA
d4b3 40      	LD	B,B
d4b4 3840    	JR	C,40		; d4f6
d4b6 3f      	CCF
d4b7 40      	LD	B,B
d4b8 00      	NOP
d4b9 47      	LD	B,A
d4ba 07      	RLCA
d4bb 47      	LD	B,A
d4bc 3847    	JR	C,47		; d505
d4be 3f      	CCF
d4bf 47      	LD	B,A

d4c0 ff      	RST	38H
d4c1 ff      	RST	38H
d4c2 ff      	RST	38H
d4c3 ff      	RST	38H
d4c4 ff      	RST	38H
d4c5 ff      	RST	38H
d4c6 ff      	RST	38H
d4c7 ff      	RST	38H
d4c8 ff      	RST	38H
d4c9 ff      	RST	38H
d4ca ff      	RST	38H
d4cb ff      	RST	38H
d4cc ff      	RST	38H
d4cd ff      	RST	38H
d4ce ff      	RST	38H
d4cf ff      	RST	38H
d4d0 ff      	RST	38H
d4d1 ff      	RST	38H
d4d2 ff      	RST	38H
d4d3 ff      	RST	38H
d4d4 ff      	RST	38H
d4d5 ff      	RST	38H
d4d6 ff      	RST	38H
d4d7 ff      	RST	38H
d4d8 ff      	RST	38H
d4d9 ff      	RST	38H
d4da ff      	RST	38H
d4db ff      	RST	38H
d4dc ff      	RST	38H
d4dd ff      	RST	38H
d4de ff      	RST	38H
d4df ff      	RST	38H
d4e0 ff      	RST	38H
d4e1 ff      	RST	38H
d4e2 ff      	RST	38H
d4e3 ff      	RST	38H
d4e4 ff      	RST	38H
d4e5 ff      	RST	38H
d4e6 ff      	RST	38H
d4e7 ff      	RST	38H
d4e8 ff      	RST	38H
d4e9 ff      	RST	38H
d4ea ff      	RST	38H
d4eb ff      	RST	38H
d4ec ff      	RST	38H
d4ed ff      	RST	38H
d4ee ff      	RST	38H
d4ef ff      	RST	38H
d4f0 ff      	RST	38H
d4f1 ff      	RST	38H
d4f2 ff      	RST	38H
d4f3 ff      	RST	38H
d4f4 ff      	RST	38H
d4f5 ff      	RST	38H
d4f6 ff      	RST	38H
d4f7 ff      	RST	38H
d4f8 ff      	RST	38H
d4f9 ff      	RST	38H
d4fa ff      	RST	38H
d4fb ff      	RST	38H
d4fc ff      	RST	38H
d4fd ff      	RST	38H
d4fe ff      	RST	38H
d4ff ff      	RST	38H
d500 00      	NOP

d501 29      	ADD	HL,HL
d502 ff      	RST	38H
d503 81      	ADD	A,C
d504 e0      	RET	PO
d505 03      	INC	BC
d506 00      	NOP
d507 81      	ADD	A,C
d508 80      	ADD	A,B
d509 13      	INC	DE
d50a 00      	NOP
d50b 81      	ADD	A,C
d50c 0137ff  	LD	BC,ff37
d50f 81      	ADD	A,C
d510 c0      	RET	NZ
d511 03      	INC	BC
d512 00      	NOP
d513 81      	ADD	A,C
d514 80      	ADD	A,B
d515 14      	INC	D
d516 00      	NOP
d517 81      	ADD	A,C
d518 3f      	CCF
d519 36ff    	LD	(HL),ff
d51b 81      	ADD	A,C
d51c 80      	ADD	A,B
d51d 03      	INC	BC
d51e 00      	NOP
d51f 81      	ADD	A,C
d520 40      	LD	B,B
d521 14      	INC	D
d522 00      	NOP
d523 84      	ADD	A,H
d524 03      	INC	BC
d525 ff      	RST	38H
d526 fc0702  	CALL	M,0207
d529 ff      	RST	38H
d52a 82      	ADD	A,D
d52b 00      	NOP
d52c 3f      	CCF
d52d 2eff    	LD	L,ff
d52f 81      	ADD	A,C
d530 fe03    	CP	03
d532 00      	NOP
d533 82      	ADD	A,D
d534 1040    	DJNZ	40		; d576
d536 08      	EX	AF,AF'
d537 00      	NOP
d538 82      	ADD	A,D
d539 1f      	RRA
d53a f0      	RET	P
d53b 08      	EX	AF,AF'
d53c 00      	NOP
d53d 84      	ADD	A,H
d53e 01ff80  	LD	BC,80ff
d541 3f      	CCF
d542 02      	LD	(BC),A
d543 00      	NOP
d544 84      	ADD	A,H
d545 3f      	CCF
d546 fc0003  	CALL	M,0300
d549 2eff    	LD	L,ff
d54b 81      	ADD	A,C
d54c fc0300  	CALL	M,0003
d54f 82      	ADD	A,D
d550 1040    	DJNZ	40		; d592
d552 07      	RLCA
d553 00      	NOP
d554 84      	ADD	A,H
d555 07      	RLCA
d556 eaafc0  	JP	PE,c0af
d559 07      	RLCA
d55a 00      	NOP
d55b 84      	ADD	A,H
d55c 1e00    	LD	E,00
d55e 7e      	LD	A,(HL)
d55f 0c      	INC	C
d560 02      	LD	(BC),A
d561 00      	NOP
d562 87      	ADD	A,A
d563 0f      	RRCA
d564 f0      	RET	P
d565 1c      	INC	E
d566 00      	NOP
d567 7f      	LD	A,A
d568 f8      	RET	M
d569 0f      	RRCA
d56a 11ff81  	LD	DE,81ff
d56d 83      	ADD	A,E
d56e 19      	ADD	HL,DE
d56f ff      	RST	38H
d570 81      	ADD	A,C
d571 f8      	RET	M
d572 03      	INC	BC
d573 00      	NOP
d574 82      	ADD	A,D
d575 1040    	DJNZ	40		; d5b7
d577 07      	RLCA
d578 00      	NOP
d579 81      	ADD	A,C
d57a 3d      	DEC	A
d57b 02      	LD	(BC),A
d57c 55      	LD	D,L
d57d 81      	ADD	A,C
d57e 78      	LD	A,B
d57f 07      	RLCA
d580 00      	NOP
d581 8b      	ADC	A,E
d582 e0      	RET	PO
d583 00      	NOP
d584 01f003  	LD	BC,03f0
d587 e0      	RET	PO
d588 01c063  	LD	BC,63c0
d58b 80      	ADD	A,B
d58c 1f      	RRA
d58d 02      	LD	(BC),A
d58e 00      	NOP
d58f 81      	ADD	A,C
d590 7f      	LD	A,A
d591 0b      	DEC	BC
d592 ff      	RST	38H
d593 82      	ADD	A,D
d594 fc1f02  	CALL	M,021f
d597 ff      	RST	38H
d598 83      	ADD	A,E
d599 f8      	RET	M
d59a 00      	NOP
d59b 3f      	CCF
d59c 02      	LD	(BC),A
d59d ff      	RST	38H
d59e 81      	ADD	A,C
d59f c0      	RET	NZ
d5a0 03      	INC	BC
d5a1 ff      	RST	38H
d5a2 81      	ADD	A,C
d5a3 e0      	RET	PO
d5a4 03      	INC	BC
d5a5 ff      	RST	38H
d5a6 82      	ADD	A,D
d5a7 f8      	RET	M
d5a8 3f      	CCF
d5a9 0c      	INC	C
d5aa ff      	RST	38H
d5ab 81      	ADD	A,C
d5ac e0      	RET	PO
d5ad 03      	INC	BC
d5ae 00      	NOP
d5af 82      	ADD	A,D
d5b0 1020    	DJNZ	20		; d5d2
d5b2 0600    	LD	B,00
d5b4 82      	ADD	A,D
d5b5 01ea02  	LD	BC,02ea
d5b8 aa      	XOR	D
d5b9 81      	ADD	A,C
d5ba af      	XOR	A
d5bb 0600    	LD	B,00
d5bd 83      	ADD	A,E
d5be 01003f  	LD	BC,3f00
d5c1 02      	LD	(BC),A
d5c2 00      	NOP
d5c3 8a      	ADC	A,D
d5c4 3c      	INC	A
d5c5 1f      	RRA
d5c6 00      	NOP
d5c7 018078  	LD	BC,7880
d5ca 04      	INC	B
d5cb 03      	INC	BC
d5cc c0      	RET	NZ
d5cd 3f      	CCF
d5ce 0b      	DEC	BC
d5cf ff      	RST	38H
d5d0 82      	ADD	A,D
d5d1 80      	ADD	A,B
d5d2 0102ff  	LD	BC,ff02
d5d5 8b      	ADC	A,E
d5d6 e0      	RET	PO
d5d7 00      	NOP
d5d8 07      	RLCA
d5d9 ff      	RST	38H
d5da fe00    	CP	00
d5dc 07      	RLCA
d5dd ff      	RST	38H
d5de fc000f  	CALL	M,0f00
d5e1 02      	LD	(BC),A
d5e2 ff      	RST	38H
d5e3 82      	ADD	A,D
d5e4 80      	ADD	A,B
d5e5 07      	RLCA
d5e6 02      	LD	(BC),A
d5e7 ff      	RST	38H
d5e8 82      	ADD	A,D
d5e9 e0      	RET	PO
d5ea 0f      	RRCA
d5eb 02      	LD	(BC),A
d5ec ff      	RST	38H
d5ed 82      	ADD	A,D
d5ee f0      	RET	P
d5ef 7f      	LD	A,A
d5f0 04      	INC	B
d5f1 ff      	RST	38H
d5f2 82      	ADD	A,D
d5f3 c7      	RST	00H
d5f4 ff      	RST	38H
d5f5 02      	LD	(BC),A
d5f6 00      	NOP
d5f7 82      	ADD	A,D
d5f8 08      	EX	AF,AF'
d5f9 2006    	JR	NZ,06		; d601
d5fb 00      	NOP
d5fc 82      	ADD	A,D
d5fd 03      	INC	BC
d5fe 5f      	LD	E,A
d5ff 03      	INC	BC
