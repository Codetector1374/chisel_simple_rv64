for i in rv32ui-*; do
    [[ $i == *.dump || $i == *.bin ]] && continue
    riscv32-unknown-elf-objcopy -O binary $i bin/$i.bin
done