module SinglePortRam (
    clock,
    addr,
    we,
    data_in,
    data_out
);

parameter AW = 14;

input clock;
input [AW-1: 0] addr;
input we;
output reg [7:0] data_out;
input [7:0] data_in;

reg [7:0] mem [0:(1 << AW)-1];

always @(posedge clock) begin
    if (we)
        mem[addr] <= data_in;

    data_out <= mem[addr];
end

    
endmodule