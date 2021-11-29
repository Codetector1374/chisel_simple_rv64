.extern cmain

.section .text.init
_start:
    li sp, 0x10000
    # li sp, 0x1000
    j cmain

    nop
    nop
    nop
_here:
    j _here