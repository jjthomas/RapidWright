package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFNetlist;
import com.xilinx.rapidwright.edif.EDIFPort;
import com.xilinx.rapidwright.router.Router;

import java.util.ArrayList;
import java.util.List;

public class ConnectIFShell {
    public static void main(String[] args) {
        boolean USE_ROUTED_SHELL = true;
        String ifPath = "/home/jamestho/shell_kernel_if_routed.dcp";
        String shellPath = "/home/jamestho/blockCache/2018.2/4a0bd7e9e9fb90b3/passthrough_2_StreamingMemoryContr_0_opt.dcp";
        if (USE_ROUTED_SHELL) {
            shellPath = shellPath.replace("opt.dcp", "0_routed.dcp");
        }
        Design ifD = Design.readCheckpoint(ifPath);
        Design shellD = Design.readCheckpoint(shellPath);
        Design d = new Design("top", Device.AWS_F1);
        Module ifM = new Module(ifD);
        Module shellM = new Module(shellD);
        EDIFNetlist netlist = d.getNetlist();
        netlist.migrateCellAndSubCells(shellD.getTopEDIFCell());
        netlist.migrateCellAndSubCells(ifD.getTopEDIFCell());
        if (USE_ROUTED_SHELL) {
            ModuleInst shellInst = d.createModuleInst("shell", shellM);
            shellInst.place(shellM.getAnchor().getSite());
        } else {
            netlist.getTopCell().createChildCellInst("shell", shellD.getTopEDIFCell());
        }
        for (int i = -1; i < 1; i++) {
            ModuleInst ifInst = d.createModuleInst("if" + (i + 1), ifM);
            ifInst.place(ifM.getAnchor().getSite().getNeighborSite(0, -i * 60));
        }
        int i = 0;
        for (EDIFPort sPort : d.getTopEDIFCell().getCellInst("shell").getCellType().getPorts()) {
            if (!sPort.getBusName().contains("streamingCores_")) {
                EDIFPort topPort = d.getTopEDIFCell().createPort(sPort.getName().replace("axi_", ""),
                        sPort.getDirection(), sPort.getWidth());
                for (int j = 0; j < sPort.getWidth(); j++) {
                    EDIFNet portNet = d.getTopEDIFCell().createNet(topPort.getName() + j + "_pn");
                    portNet.createPortInst(sPort, j, d.getTopEDIFCell().getCellInst("shell"));
                    portNet.createPortInst(topPort, j);
                }
                continue;
            }
            int ifIdx = Integer.parseInt(sPort.getBusName().split("_")[2]);
            String iPortName = sPort.getBusName().endsWith("_reset") ? "shell_reset" :
                    "shell_" + sPort.getBusName().replace("streamingCores_" + ifIdx + "_", "");
            EDIFPort iPort = d.getTopEDIFCell().getCellInst("if" + ifIdx).getCellType()
                    .getPort(iPortName);
            for (int j = 0; j < sPort.getWidth(); j++) {
                EDIFNet sifNet = d.getTopEDIFCell().createNet("sifn" + i);
                sifNet.createPortInst(sPort, j, d.getTopEDIFCell().getCellInst("shell"));
                sifNet.createPortInst(iPort, j, d.getTopEDIFCell().getCellInst("if" + ifIdx));
                i++;
            }
        }
        /*
        List<Net> toRemove = new ArrayList<>();
        for (Net n : d.getNets()) {
            if (n.getName().endsWith("_kif")) {
                toRemove.add(n);
            }
        }
        for (Net n : toRemove) {
            d.removeNet(n);
        }
        */
        /*
        for (int ifIdx = 0; ifIdx < 4; ifIdx++) {
            for (EDIFPort iPort : d.getTopEDIFCell().getCellInst("if" + ifIdx).getCellType().getPorts()) {
                if (!iPort.getBusName().startsWith("shell")) {
                    EDIFPort topPort = d.getTopEDIFCell().createPort((iPort.getName().equals("reset") ? "kernel_reset" :
                            iPort.getName()).replace("[", "_" + ifIdx + "[") +
                            (iPort.getWidth() > 1 ? "" : "_" + ifIdx), iPort.getDirection(), iPort.getWidth());
                    for (int j = 0; j < iPort.getWidth(); j++) {
                        EDIFNet portNet = d.getTopEDIFCell().createNet(topPort.getName() + "_" + j + "_pn");
                        portNet.createPortInst(iPort, j, d.getTopEDIFCell().getCellInst("if" + ifIdx));
                        portNet.createPortInst(topPort, j);
                    }
                }
            }
        }
        */
        // d.routeSites();
        // new Router(d).routeDesign();
        d.setAutoIOBuffers(false);
        d.setDesignOutOfContext(true);
        d.writeCheckpoint("/home/jamestho/if_shell.dcp");
    }
}
