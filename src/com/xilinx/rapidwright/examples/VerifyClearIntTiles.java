package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.device.*;
import com.xilinx.rapidwright.design.blocks.PBlock;

public class VerifyClearIntTiles {
    public static void main(String[] args) {
        String shellPath = "/home/jamestho/if_shell_routed.dcp";
        Design shell = Design.readCheckpoint(shellPath);
        PBlock pblock = new PBlock(Device.getDevice(Device.AWS_F1), "SLICE_X13Y570:SLICE_X17Y599 SLICE_X13Y510:SLICE_X17Y539");
        for (Net n : shell.getNets()) {
            if (n.getName().contains("/ifn") || n.isStaticNet()) {
                continue;
            }
            for (PIP p : n.getPIPs()) {
                if (pblock.containsTile(p.getTile())) {
                    System.out.println(n.getName() + " " + p.getTile().getName());
                }
            }
        }
    }
}
