package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.Design;

public class ReadWriteRW {
    public static void main(String[] args) {
        /*
        java.util.Set<Integer> cols = new java.util.HashSet<>();
        for (int i = 0; i < FloorplanUtils.tiles.length; i++) {
            for (int j = 0; j < FloorplanUtils.tiles[i].length; j++) {
                if (FloorplanUtils.tiles[i][j].getTileTypeEnum() == com.xilinx.rapidwright.device.TileTypeEnum.BRAM_R) {
                    System.out.println(FloorplanUtils.tiles[i][j].getName());
                    cols.add(j);
                }
            }
        }
        System.out.println(cols);
        */
        String shellPath = "/home/jamestho/2_hole_cl.dcp";
        Design shell = Design.readCheckpoint(shellPath);
        shell.getNetlist().consolidateAllToWorkLibrary();
        shell.setAutoIOBuffers(false);
        shell.setDesignOutOfContext(true);
        shell.writeCheckpoint("/home/jamestho/merged_shell_kernel.dcp");
    }
}
