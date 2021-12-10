ENTRY(_start)
SECTIONS
{
  . = 0x0;
  .text : { 
      KEEP(*(.text.init))
      *(.text)
  }
  . = ALIGN(0x100);
  .data : {
    *(.rodata)
    *(.sdata .data)
  }
  .bss : {
    *(.bss)
  }
}