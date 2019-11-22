package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.PIP;
import java.util.*;

public class CompareDCPs {
    public static void main(String[] args) {
        String preRoute = "/home/jamestho/2_hole_merged_pre_route_cl.dcp";
        String postRoute = "/home/jamestho/2_hole_merged_post_route_cl.dcp";
        Design preRouteD = Design.readCheckpoint(preRoute);
        Design postRouteD = Design.readCheckpoint(postRoute);
        System.out.println(preRouteD.getNets().size() + " " + postRouteD.getNets().size());
        for (Net preRouteNet : preRouteD.getNets()) {
            Net postRouteNet = postRouteD.getNet(preRouteNet.getName());
            if (postRouteNet == null) {
                System.out.println(preRouteNet.getName() + " null in post-route design");
            }
            Set<PIP> preRouteSet = new HashSet<>(preRouteNet.getPIPs());
            Set<PIP> postRouteSet = new HashSet<>(postRouteNet.getPIPs());
            if (!preRouteSet.equals(postRouteSet)) {
                System.out.println("Difference in net " + preRouteNet.getName());
                Set<PIP> preRouteCopy = new HashSet<>(preRouteSet);
                preRouteSet.removeAll(postRouteSet);
                System.out.println("\tpre-route uniques: " + preRouteSet);
                postRouteSet.removeAll(preRouteCopy);
                System.out.println("\tpost-route uniques: " + postRouteSet);

            }
        }
    }
}
