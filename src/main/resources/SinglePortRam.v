module SinglePortRam (
    clock,
    addr,
    we,
    data_in,
    data_out
);

parameter AW = 14;
parameter DW = 8;

input clock;
input [AW-1: 0] addr;
input we;
output reg [DW-1:0] data_out;
input [DW-1:0] data_in;

reg [DW-1:0] mem [0:(1 << AW)-1];

always @(posedge clock) begin
    if (we)
        mem[addr] <= data_in;

    data_out <= mem[addr];
end

    
endmodule