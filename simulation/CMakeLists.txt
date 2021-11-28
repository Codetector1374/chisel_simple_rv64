cmake_minimum_required(VERSION 3.1)
project(rv_sim)

set (CMAKE_CXX_STANDARD 17)

set(CMAKE_MODULE_PATH ${CMAKE_CURRENT_SOURCE_DIR}/cmake ${CMAKE_MODULE_PATH})
#set(CMAKE_CXX_FLAGS -fsanitize=address)

find_package(verilator REQUIRED)

include_directories(src)

add_executable(rv_sim src/rv_sim.cpp)
verilate(rv_sim SOURCES ../generated/Top.v ../generated/memdev.v
        TOP_MODULE Top TRACE
        VERILATOR_ARGS -Wno-UNOPTFLAT
        )


add_executable(sdram_sim src/sdramsim.cpp src/wbsdram_sim.cpp)
verilate(sdram_sim SOURCES ../generated/fpga_test/TestWBMaster.v TRACE
        VERILATOR_ARGS)