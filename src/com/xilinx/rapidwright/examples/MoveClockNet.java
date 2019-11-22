package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.Design;

public class MoveClockNet {
    public static void main(String[] args) {
        String preRoute = "/home/jamestho/2_hole_merged_pre_route_cl.dcp";
        String postRoute = "/home/jamestho/2_hole_merged_post_route_cl.dcp";
        Design preRouteD = Design.readCheckpoint(preRoute);
        Design postRouteD = Design.readCheckpoint(postRoute);
        preRouteD.getNet("clk_main_a0").setPIPs(postRouteD.getNet("clk_main_a0").getPIPs());
        preRouteD.setAutoIOBuffers(false);
        preRouteD.setDesignOutOfContext(true);
        preRouteD.writeCheckpoint("/home/jamestho/2_hole_merged_clock_moved_cl.dcp");
    }
}
