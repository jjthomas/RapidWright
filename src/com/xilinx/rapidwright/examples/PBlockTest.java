package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.design.blocks.PBlock;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.PIP;
import com.xilinx.rapidwright.device.Tile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PBlockTest {
    public static void main(String[] args) throws IOException {
        /*
        Set<Tile> tiles = new PBlock(Device.getDevice(Device.AWS_F1), "SLICE_X12Y569:SLICE_X12Y570").getAllTiles();
        System.out.println(tiles);
        */
        String ifPath = "/home/jamestho/shell_kernel_if_routed.dcp";
        Design ifD = Design.readCheckpoint(ifPath);
        Set<PIP> ifnPips = new HashSet<>();
        for (Net n : ifD.getNets()) {
            if (n.getName().startsWith("ifn")) {
                for (PIP p : n.getPIPs()) {
                    ifnPips.add(p);
                }
            }
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter("/home/jamestho/pips_to_disable.txt"));
        /*
        Device f1 = Device.getDevice(Device.AWS_F1);
        for (int offset : new int[]{588, 528}) {
            for (int i = 0; i < 12; i++) {
                for (PIP p : f1.getTile("INT_X8Y" + (offset + i)).getPIPs()){
                    if (!ifnPips.contains(p)) {
                        bw.write(p.toString());
                        bw.newLine();
                    }
                }
            }
        }
        */
        for (PIP p : ifnPips) {
            bw.write(p.toString());
            bw.newLine();
        }
        bw.close();

    }
}
