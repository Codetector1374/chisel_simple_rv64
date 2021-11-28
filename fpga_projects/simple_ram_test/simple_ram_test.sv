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

    stall,
    idle
);

  input wire    clock;
  input wire    reset;
  inout wire [15:0] io_sdram_data;

  output wire [12:0] io_sdram_addr;
  output wire [1:0]  io_sdram_bank;
  output wire [1:0]  io_sdram_dqm;
  output wire        io_sdram_clk;
  output wire        io_sdram_cke;
  output wire        io_sdram_cs_n;
  output wire        io_sdram_ras_n;
  output wire        io_sdram_cas_n;
  output wire        io_sdram_we_n;

  input  [9:0]  io_switches;
  input         io_write;

  output wire [6:0] hex0;
  output wire [6:0] hex1;

  output wire stall;
  output wire idle;

wire io_sdram_output_en;
wire [15:0] io_sdram_dataOut;
assign io_sdram_data = io_sdram_output_en ? io_sdram_dataOut : 16'bz;

wire [7:0] byteOut;

sseg seg1(hex0, byteOut[3:0]);
sseg seg2(hex1, byteOut[7:4]);

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
    .io_idle(idle)
);

endmodule