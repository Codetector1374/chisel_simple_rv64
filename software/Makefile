CC = riscv32-unknown-elf-gcc
AS = riscv32-unknown-elf-as
OBJDUMP = riscv32-unknown-elf-objdump
OBJCOPY = riscv32-unknown-elf-objcopy
LD = riscv32-unknown-elf-ld
TLFLASH = /c/Users/user/projects/github.com/tl-flash/target/debug/tl-flash.exe
LDPATH = /c/Users/user/projects/build/riscv32-gcc/lib/gcc/riscv32-unknown-elf/11.1.0

SRCS = main.c
OBJS = $(patsubst %.c, %.o, $(SRCS))


all: a.bin

entry.o: src_asm/entry.s
	$(CC) -c $^ -o $@

%.o: %.c
	$(CC) -O3 -c $^ -o $@

clean:
	rm -rf *.o *.dump *.bin

a.elf: $(OBJS) entry.o linker.ld
	$(LD) -T linker.ld entry.o $(OBJS) -L$(LDPATH) -lgcc -o $@

a.bin: a.elf
	$(OBJCOPY) -O binary $^ $@
	$(OBJDUMP) --disassemble $^ > a.dump
	$(OBJDUMP) --disassemble -M numeric $^ > a.numeric.dump

flash: a.bin
	$(TLFLASH) a.bin

.PHONY: clean all flash

