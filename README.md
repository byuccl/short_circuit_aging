# Short Circuit Aging

This repository contains a RapidWright-based library that can be used to create short circuits for Xilinx series 7 FPGAs.
These short circuits can be programmed onto the FPGA to achieve a unique non-uniform aging effect. For more information on short circuit aging, see [our paper](https://ccl.byu.edu/assets/cook_trets22.pdf).

## Setup
To build the library, simply run `make`. This should create a file called short_circuit_aging.jar, that can be used as a dependency in your own Java project.

If the build process fails, try installing JDK-8 and setting the JAVA_HOME variable to point to JDK-8 (e.g. `export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64/`).

If you are using IntelliJ IDEA, you could also directly import this repository--along with RapidWright--as a module with existing sources. You would then have to add RapidWright as a dependency for this module and add both RapidWright and this module as a dependency for your own project. This would give you access to the source code in this project during debugging.

## Creating a bitstream containing short circuits

The API provided can be used alongside RapidWright to create a Vivado checkpoint file that contains short circuits (see [this example](src/edu/byu/shortCircuits/examples/SimpleShortedDesign.java) to see how). Once the checkpoint has been generated, it can be opened in Vivado. 

To generate a bitstream from the short circuit checkpoint, run the following TCL commands: 

```
set_property SEVERITY Warning [get_drc_checks MDRV-1]
set_property SEVERITY Warning [get_drc_checks RTSTAT-13]
```

A couple of things to note:
1. Short circuit generation works on Vivado 2018.3. It may not work on newer versions of Vivado.
2. In order to bypass the DRCs, you must create at least 1000 nets with multiple drivers (i.e. short circuits).

## Documentation

For further information on the short circuit API, refer to the source code.
