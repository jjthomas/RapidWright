package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.PIP;
import com.xilinx.rapidwright.edif.*;

import java.util.*;

public class MergeShellKernel3 {
    public static void main(String[] args) {
        String shellPath = "/home/jamestho/2_hole_cl.dcp";
        String kernelPath = "/home/jamestho/if_kernel_routed.dcp";
        Design shell = Design.readCheckpoint(shellPath);
        shell.getNetlist().consolidateAllToWorkLibrary();
        Design kernelSource = Design.readCheckpoint(kernelPath);
        kernelSource.getNetlist().consolidateAllToWorkLibrary();
        Module kernelSourceM = new Module(kernelSource);

        EDIFCell kernelCell = kernelSource.getTopEDIFCell().getCellInst("kernel").getCellType();
        shell.getNetlist().migrateCellAndSubCells(kernelCell);

        for (int i = 0; i < 2; i++) {
            String prefix = "kernel" + i;
            Design kernel = new Design(prefix, Device.AWS_F1);
            kernel.getTopEDIFCell().createChildCellInst("streaming_wrapper",
                    new EDIFCell(kernel.getNetlist().getWorkLibrary(), "streaming_wrapper"));
            ModuleInst modInst = kernel.createModuleInst("streaming_wrapper/" + prefix, kernelSourceM);
            modInst.place(kernelSourceM.getAnchor().getSite().getNeighborSite(0, (1 - i) * 60));

            shell.getTopEDIFCell().getCellInst("streaming_wrapper").getCellType().createChildCellInst(prefix,
                    new EDIFCell(shell.getNetlist().getWorkLibrary(), prefix));
            shell.getTopEDIFCell().getCellInst("streaming_wrapper").getCellType().getCellInst(prefix).getCellType()
                    .addNewCellInstUniqueName("kernel", kernelCell);

            /*
            List<Net> toRemove = new ArrayList<>();
            for (Net n : shell.getNets()) {
                if (n.getName().startsWith("if/") && n.getName().endsWith("_kif")) {
                    toRemove.add(n);
                }
            }
            for (Net n : toRemove) {
                shell.removeNet(n);
            }
            */

            List<Net> netsToAdd = new ArrayList<>();
            for (Net n : kernel.getNets()) {
                if (n.getName().startsWith("streaming_wrapper/" + prefix + "/if/") && n.getName().endsWith("_kif")) {
                    netsToAdd.add(n);
                } else if (n.getName().startsWith("streaming_wrapper/" + prefix + "/kernel/")) {
                    netsToAdd.add(n);
                }
            }

            for (Net n : netsToAdd) {
                if (n.getName().startsWith("streaming_wrapper/" + prefix + "/if/") ) {
                    n.rename(n.getName().replace("streaming_wrapper/" + prefix + "/if", "streaming_wrapper/if" + i));
                    shell.addNet(n);
                } else {
                    shell.addNet(n);
                }
            }

            Net shell0 = shell.getNet("GLOBAL_LOGIC0");
            Net kernel0 = kernel.getNet("GLOBAL_LOGIC0");
            for (PIP p : kernel0.getPIPs()) {
                shell0.addPIP(p);
            }
            for (SitePinInst p : kernel0.getSinkPins()) {
                shell0.addPin(p);
            }

            Net shell1 = shell.getNet("GLOBAL_LOGIC1");
            Net kernel1 = kernel.getNet("GLOBAL_LOGIC1");
            Set<String> shell1Pips = new HashSet<>();
            for (PIP p : shell1.getPIPs()) {
                shell1Pips.add(p.toString());
            }
            for (PIP p : kernel1.getPIPs()) {
                if (!shell1Pips.contains(p.toString())) {
                    shell1.addPIP(p);
                }
            }
            for (SitePinInst p : kernel1.getSinkPins()) {
                if (!shell1.getSinkPins().contains(p)) {
                    shell1.addPin(p);
                }
            }

            Set<SiteInst> sites = new HashSet<>();
            for (EDIFHierCellInst edifCell : kernel.getNetlist().getAllLeafDescendants("streaming_wrapper/" + prefix + "/kernel")) {
                Cell c = kernel.getCell(edifCell.getFullHierarchicalInstName());
                if (c != null) {
                    shell.addCell(c);
                    sites.add(c.getSiteInst());
                }
            }
            for (SiteInst site : sites) {
                shell.addSiteInst(site);
            }

            EDIFNet portNet = shell.getTopEDIFCell().getCellInst("streaming_wrapper").getCellType()
                    .getNet("clock0_pn");
            portNet.createPortInst(shell.getTopEDIFCell().getCellInst("streaming_wrapper").getCellType()
                    .getCellInst("kernel" + i).getCellType().getPort("clock"),
                    shell.getTopEDIFCell().getCellInst("streaming_wrapper").getCellType()
                            .getCellInst("kernel" + i));

            for (EDIFPortInst port : shell.getTopEDIFCell().getCellInst("streaming_wrapper").getCellType()
                    .getCellInst("if" + i).getPortInsts()) {
                if (!port.getName().contains("shell_")) {
                    /*
                    EDIFPortInst otherPort = null;
                    for (EDIFPortInst p : port.getNet().getPortInsts()) {
                        if (p != port) {
                            otherPort = p;
                        }
                    }
                    port.getNet().removePortInst(otherPort);
                    port.getNet().addPortInst(shell.getTopEDIFCell().getCellInst("streaming_wrapper")
                            .getCellType().getCellInst("kernel" + i).getPortInst(port.getName()));
                    */
                    EDIFNet pn = shell.getTopEDIFCell().getCellInst("streaming_wrapper").getCellType()
                            .createNet(port.getName() + "_connector");
                    pn.addPortInst(port);
                    pn.createPortInst(shell.getTopEDIFCell().getCellInst("streaming_wrapper")
                            .getCellType().getCellInst("kernel" + i).getCellType().getPort(port.getPort().getBusName()),
                            port.getIndex(), shell.getTopEDIFCell().getCellInst("streaming_wrapper")
                                    .getCellType().getCellInst("kernel" + i));

                }
            }
        }

        shell.setAutoIOBuffers(false);
        shell.setDesignOutOfContext(true);
        shell.writeCheckpoint("/home/jamestho/merged_shell_kernel.dcp");
    }
}