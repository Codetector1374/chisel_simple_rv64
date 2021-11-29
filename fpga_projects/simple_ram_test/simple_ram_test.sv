module simple_ram_test(
    clock,
    reset,
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
    io_write,

    hex0,
    hex1,
	 hex2,
    hex3,
	 hex4,
    hex5,
	 hex6,
    hex7,
	 
	 tx, 
	 rx,


    stall,
    idle
);

  input wire    clock;
  input wire    reset;
  inout wire [31:0] io_sdram_data;

  output wire [12:0] io_sdram_addr;
  output wire [1:0]  io_sdram_bank;
  output wire [3:0]  io_sdram_dqm;
  output wire        io_sdram_clk;
  output wire        io_sdram_cke;
  output wire        io_sdram_cs_n;
  output wire        io_sdram_ras_n;
  output wire        io_sdram_cas_n;
  output wire        io_sdram_we_n;

  input  [17:0]  io_switches;
  input         io_write;

  output wire [6:0] hex0;
  output wire [6:0] hex1;
  output wire [6:0] hex2;
  output wire [6:0] hex3;
  output wire [6:0] hex4;
  output wire [6:0] hex5;
  output wire [6:0] hex6;
  output wire [6:0] hex7;
  
  output wire tx;
  input wire rx;

  output wire stall;
  output wire idle;

wire io_sdram_output_en;
wire [31:0] io_sdram_dataOut;
assign io_sdram_data = io_sdram_output_en ? io_sdram_dataOut : 16'bz;

wire [31:0] byteOut;

sseg seg1(hex0, byteOut[3:0]);
sseg seg2(hex1, byteOut[7:4]);
sseg seg3(hex2, byteOut[11:8]);
sseg seg4(hex3, byteOut[15:12]);
sseg seg5(hex4, byteOut[19:16]);
sseg seg6(hex5, byteOut[23:20]);
sseg seg7(hex6, byteOut[27:24]);
sseg seg8(hex7, byteOut[31:28]);

reg breset, int_reset;
reg bwrite, int_write;

always @(posedge clock) begin
{breset , int_reset} <= {int_reset, ~reset};
{bwrite , int_write} <= {int_write, ~io_write};
end

TestWBMaster a(
    .clock,
    .reset(breset),
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
    .io_write(bwrite),
    .io_output(byteOut),
    .io_stall(stall),
    .io_idle(idle),
	 .io_rx(rx),
	 .io_tx(tx)
);

endmodule