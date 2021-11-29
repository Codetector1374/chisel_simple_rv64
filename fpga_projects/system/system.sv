module system (
    clock,
    i_reset,
    io_leds,
    io_rx,
    io_tx,
    io_sdram_addr,
    io_sdram_bank,
    io_sdram_data,
    io_sdram_dqm,
    io_sdram_clk,
    io_sdram_cke,
    io_sdram_cs_n,
    io_sdram_ras_n,
    io_sdram_cas_n,
    io_sdram_we_n,
	 io_switches,
	 io_ssegs_0,
	io_ssegs_1,
	io_ssegs_2,
	io_ssegs_3,
	io_ssegs_4,
	io_ssegs_5,
	io_ssegs_6,
	io_ssegs_7
);

input         clock;
input         i_reset;
output [15:0] io_leds;
input         io_rx;
output        io_tx;
output [12:0] io_sdram_addr;
output [1:0]  io_sdram_bank;
inout  [31:0] io_sdram_data;
output [3:0]  io_sdram_dqm;
output        io_sdram_clk;
output        io_sdram_cke;
output        io_sdram_cs_n;
output        io_sdram_ras_n;
output        io_sdram_cas_n;
output        io_sdram_we_n;
input [17:0] io_switches;
  output [6:0]  io_ssegs_0;
  output [6:0]  io_ssegs_1;
  output [6:0]  io_ssegs_2;
  output [6:0]  io_ssegs_3;
  output [6:0]  io_ssegs_4;
  output [6:0]  io_ssegs_5;
  output [6:0]  io_ssegs_6;
  output [6:0]  io_ssegs_7;

reg reset, reset_int;
always @(posedge clock) begin
    {reset, reset_int} <= {reset_int, ~i_reset};
end

wire io_sdram_output_en;
wire [31:0] io_sdram_dataOut;
assign io_sdram_data = io_sdram_output_en ? io_sdram_dataOut : 16'bz;

FullSystemTop top(
    .clock,
    .reset,
    .io_leds,
    .io_rx,
    .io_tx,

    .io_sdram_addr,
    .io_sdram_bank,
    .io_sdram_dataOut,
    .io_sdram_dqm,
    .io_sdram_clk,
    .io_sdram_cke,
    .io_sdram_cs_n,
    .io_sdram_ras_n,
    .io_sdram_cas_n,
    .io_sdram_we_n,
    .io_sdram_output_en,
    .io_dqIn(io_sdram_data),
	 
	 .io_switches,
	 .io_ssegs_0,
	.io_ssegs_1,
	.io_ssegs_2,
	.io_ssegs_3,
	.io_ssegs_4,
	.io_ssegs_5,
	.io_ssegs_6,
	.io_ssegs_7

);

endmodule