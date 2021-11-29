CC = riscv32-unknown-elf-gcc
OBJDUMP = riscv32-unknown-elf-objdump
OBJCOPY = riscv32-unknown-elf-objcopy
LD = riscv32-unknown-elf-ld
TLFLASH = /c/Users/user/projects/github.com/tl-flash/target/debug/tl-flash.exe

SRCS = main.c
OBJS = $(patsubst %.c, %.o, $(SRCS))


all: a.bin

entry.o: entry.s
	$(CC) -c $^ -o $@

%.o: %.c
	$(CC) -c $^ -o $@

clean:
	rm -rf *.o *.dump *.bin

a.elf: $(OBJS) entry.o linker.ld
	$(LD) -T linker.ld $(OBJS) entry.o -o $@

a.bin: a.elf
	$(OBJCOPY) -O binary $^ $@
	$(OBJDUMP) --disassemble $^ > a.dump
	$(OBJDUMP) --disassemble -M numeric $^ > a.numeric.dump

flash: a.bin
	$(TLFLASH) a.bin

.PHONY: clean all flash

