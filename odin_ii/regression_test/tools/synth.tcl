yosys -import

# the environment variable VTR_ROOT is set by Odin-II.
# Feel free to specify file paths using "$env(VTR_ROOT)/ ..."

# Read VTR baseline library first
read_verilog -nomem2reg $env(ODIN_TECHLIB)/../../vtr_flow/primitives.v
setattr -mod -set keep_hierarchy 1 single_port_ram
setattr -mod -set keep_hierarchy 1 dual_port_ram

# Read the HDL file with pre-defined parer in the "run_yosys.sh" script
if {$env(PARSER) == "surelog" } {
	puts "Using Yosys read_uhdm command"
    plugin -i systemverilog;
    yosys -import
	read_uhdm -debug $env(TCL_CIRCUIT);
} elseif {$env(PARSER) == "system-verilog" } {
	puts "Using Yosys read_systemverilog command"
    plugin -i systemverilog;
    yosys -import
	read_systemverilog -debug $env(TCL_CIRCUIT)
} elseif {$env(PARSER) == "default" } {
	puts "Using Yosys read_verilog command"
	read_verilog -sv -nomem2reg -nolatches $env(TCL_CIRCUIT);
} else {
    put "$env(PARSER)"
	error "Invalid PARSER"
}

# Check that cells match libraries and find top module
hierarchy -check -auto-top -purge_lib;

# Make name convention more readable
autoname;
# Translate processes to entlist components such as MUXs, FFs and latches
procs; opt;
# Extraction and optimization of finite state machines
fsm; opt;
# Collects memories, their port and create multiport memory cells
memory_collect; memory_dff; opt;

# Looking for combinatorial loops, wires with multiple drivers and used wires without any driver.
check;
# resolve asynchronous dffs
techmap -map $env(ODIN_TECHLIB)/adff2dff.v;
techmap -map $env(ODIN_TECHLIB)/adffe2dff.v;
# To resolve Yosys internal indexed part-select circuitry
techmap */t:\$shift */t:\$shiftx;
# convert mem block to bram/rom

# [NOTE]: Yosys complains about expression width more than 24 bits.
# E.g. [63:0] memory [18:0] ==>  ERROR: Expression width 33554432 exceeds implementation limit of 16777216!
# mem will be handled using Odin-II
# memory_bram -rules $env(ODIN_TECHLIB)/mem_rules.txt
# techmap -map $env(ODIN_TECHLIB)/mem_map.v; 

# Transform the design into a new one with single top module
flatten;
# Transforms pmux into trees of regular multiplexers
pmuxtree;
# To possibly reduce words size
wreduce;
# "undirven" to ensure there is no wire without drive
# "opt_muxtree" removes dead branches, "opt_expr" performs constant folding,
# removes "undef" inputs from mux cells, and replaces muxes with buffers and inverters.
# "-noff" a potential option to remove all sdff and etc. Only dff will remain
opt -undriven -full; opt_muxtree; opt_expr -mux_undef -mux_bool -fine;;;
# Make name convention more readable
autoname;
# Print statistics
stat;

write_blif -param -impltf $env(TCL_BLIF);
