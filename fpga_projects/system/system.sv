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
	 io_neopixel_data,

	 io_ssegs_0,
	io_ssegs_1,
	io_ssegs_2,
	io_ssegs_3,
	io_ssegs_4,
	io_ssegs_5,
	io_ssegs_6,
	io_ssegs_7,
    io_miiEther0_gtx_clk,
    io_miiEther0_reset_n,
    io_miiEther0_tx_clk,
    io_miiEther0_tx_data,
    io_miiEther0_tx_en,
    io_miiEther0_tx_err,
    io_miiEther0_rx_clk,
    io_miiEther0_rx_data,
    io_miiEther0_rx_valid,
    io_miiEther0_rx_err,
    io_miiEther0_rx_carrierSense,
    io_miiEther0_rx_collision,
    io_miiEther0_mgmt_clk,
    io_miiEther0_mgmt_data,
	 
	 o_mii_tx_clk_out
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

output        io_neopixel_data;


input         io_miiEther0_tx_clk;
output [3:0]  io_miiEther0_tx_data;
output        io_miiEther0_tx_en;
output        io_miiEther0_tx_err;
input         io_miiEther0_rx_clk;
input  [3:0]  io_miiEther0_rx_data;
input         io_miiEther0_rx_valid;
input         io_miiEther0_rx_err;
input         io_miiEther0_rx_carrierSense;
input         io_miiEther0_rx_collision;
output        io_miiEther0_mgmt_clk;
inout         io_miiEther0_mgmt_data;

output io_miiEther0_gtx_clk;
output io_miiEther0_reset_n;
assign io_miiEther0_gtx_clk = 1'b0;
assign io_miiEther0_reset_n = ~reset;

output o_mii_tx_clk_out;
assign o_mii_tx_clk_out = io_miiEther0_tx_clk;

// Ether0 TriState
wire io_miiEther0_mgmt_dataOut, io_miiEther0_mgmt_outEn;
assign io_miiEther0_mgmt_data = io_miiEther0_mgmt_outEn ? io_miiEther0_mgmt_dataOut : 1'bz;

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
	 
	.io_neopixel_data,
	 
	.io_switches,
	.io_ssegs_0,
	.io_ssegs_1,
	.io_ssegs_2,
	.io_ssegs_3,
	.io_ssegs_4,
	.io_ssegs_5,
	.io_ssegs_6,
	.io_ssegs_7,

    .io_miiEther0_tx_clk,
    .io_miiEther0_tx_data,
    .io_miiEther0_tx_en,
    .io_miiEther0_tx_err,
    .io_miiEther0_rx_clk,
    .io_miiEther0_rx_data,
    .io_miiEther0_rx_valid,
    .io_miiEther0_rx_err,
    .io_miiEther0_rx_carrierSense,
    .io_miiEther0_rx_collision,
    .io_miiEther0_mgmt_clk,
    .io_miiEther0_mgmt_dataOut,
    .io_miiEther0_mgmt_outEn,
    .io_miiEther0_mgmt_dataIn(io_miiEther0_mgmt_data)
);

endmodule