cmake_minimum_required(VERSION 3.2)

if( NOT DEFINED RISCV_GCC_ROOT )
  message( FATAL_ERROR "RISCV_GCC_ROOT not defined" )
else()
  message( STATUS "RISCV_GCC_ROOT is ${RISCV_GCC_ROOT}")
endif()

if( NOT DEFINED TLFLASH_PATH )
  find_program(TLFLASH_PATH tl-flash)
  if (NOT TLFLASH_PATH)
    message( FATAL_ERROR "TLFLASH_PATH not defined" )
  endif()
endif()
message( STATUS "TLFLASH_PATH is ${TLFLASH_PATH}")



set(SYSTEM_PREFIX riscv32-unknown-elf)
set(CMAKE_SYSTEM_NAME Generic CACHE STRING sysName FORCE)
set(CMAKE_SYSTEM_PROCESSOR riscv)

project(rv32_software C ASM)


set(CMAKE_ASM_COMPILER ${RISCV_GCC_ROOT}/bin/${SYSTEM_PREFIX}-gcc)
set(CMAKE_C_COMPILER ${RISCV_GCC_ROOT}/bin/${SYSTEM_PREFIX}-gcc)
set(CMAKE_SYSROOT ${RISCV_GCC_ROOT}/${SYSTEM_PREFIX}/include)
set(CMAKE_EXECUTABLE_SUFFIX .elf)

set(CMAKE_EXE_LINKER_FLAGS "-nostartfiles -T ${CMAKE_SOURCE_DIR}/linker.ld")

set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)


include_directories(inc)

add_executable(
        system
        src_asm/entry.s
        src/main.c
        src/mii_net.c src/system.c inc/system.h)

add_custom_command(
        OUTPUT system.bin system.dump system.numeric.dump
        COMMAND ${RISCV_GCC_ROOT}/bin/${SYSTEM_PREFIX}-objcopy ARGS -O binary system.elf system.bin
        COMMAND ${RISCV_GCC_ROOT}/bin/${SYSTEM_PREFIX}-objdump ARGS --disassemble system.elf > system.dump
        COMMAND ${RISCV_GCC_ROOT}/bin/${SYSTEM_PREFIX}-objdump ARGS --disassemble -M numeric system.elf > system.numeric.dump
        DEPENDS system
)

add_custom_target(flash
        SOURCES system.bin
        COMMAND ${TLFLASH_PATH} system.bin
        USES_TERMINAL
        )
add_custom_target(system_bin SOURCES system.bin)



