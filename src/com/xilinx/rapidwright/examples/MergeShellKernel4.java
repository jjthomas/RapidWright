package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.device.PIP;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.edif.EDIFCellInst;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFPortInst;

import java.util.*;

public class MergeShellKernel4 {

    private static void printSiteInst(SiteInst si) {
        System.out.println(Arrays.toString(si.getRawSitePIPs()));
        for (Map.Entry e : new TreeMap<>(si.getCTagMap()).entrySet()) {
            System.out.println(e);
        }
        for (Map.Entry e : new TreeMap<>(si.getCellMap()).entrySet()) {
            System.out.println(e);
        }
    }
    public static void main(String[] args) {
        String shellPath = "/home/jamestho/2_hole_cl.dcp";
        String kernelPath = "/home/jamestho/if_kernel_routed.dcp";
        Design shell = Design.readCheckpoint(shellPath);
        shell.getNetlist().consolidateAllToWorkLibrary();
        Design kernel = Design.readCheckpoint(kernelPath);
        Map<NetType, List<PIP>> kernelStaticNets = new HashMap<>();
        kernelStaticNets.put(NetType.GND, new ArrayList<>(kernel.getStaticNet(NetType.GND).getPIPs()));
        kernelStaticNets.put(NetType.VCC, new ArrayList<>(kernel.getStaticNet(NetType.VCC).getPIPs()));
        kernel.getNetlist().consolidateAllToWorkLibrary();
        kernel.getNetlist().renameNetlistAndTopCell("ktop");
        Module kernelM = new Module(kernel);

        shell.getNetlist().migrateCellAndSubCells(kernelM.getNetlist().getTopCell());

        for (int i = 0; i < 2; i++) {
            List<Net> netsToRemove = new ArrayList<>();
            for (Net n : shell.getNets()) {
                if (n.getName().startsWith("streaming_wrapper/if" + i) && !n.getName().endsWith("_sif")) { // was previously just adding ifn nets
                    netsToRemove.add(n);
                }
            }
            for (Net n : netsToRemove) {
                shell.removeNet(n);
            }

            List<Cell> cellsToRemove = new ArrayList<>();
            for (Cell c : shell.getCells()) {
                if (c.getName().startsWith("streaming_wrapper/if" + i + "/klut")) {
                    cellsToRemove.add(c);
                    SitePinInst inputPin = c.getSitePinFromLogicalPin("I0", null);
                    if (inputPin.getNet() != null && inputPin.getNet().isStaticNet()) {
                        inputPin.getNet().removePin(inputPin, true);
                    }
                } else if (c.getName().startsWith("streaming_wrapper/if" + i + "/slut")) {
                    cellsToRemove.add(c);
                }
            }
            for (Cell c : cellsToRemove) {
                shell.removeCell(c);
            }

            ModuleInst mi = shell.createModuleInst("streaming_wrapper/kernel" + i, kernelM);
            mi.place(kernelM.getAnchor().getSite().getNeighborSite(0, (1 - i) * 60));

            EDIFCellInst oldIf = shell.getTopEDIFCell().getCellInst("streaming_wrapper").getCellType()
                    .removeCellInst("if" + i);
            EDIFCellInst newKernel = shell.getTopEDIFCell().getCellInst("streaming_wrapper").getCellType()
                    .getCellInst("kernel" + i);
            for (EDIFPortInst pi : oldIf.getPortInsts()) {
                EDIFNet portNet = pi.getNet();
                portNet.removePortInst(pi);
                portNet.createPortInst(newKernel.getPort(pi.getPort().getBusName()), pi.getIndex(), newKernel);
            }
            EDIFNet portNet = shell.getTopEDIFCell().getCellInst("streaming_wrapper").getCellType()
                    .getNet("clock0_pn");
            portNet.createPortInst(newKernel.getCellType().getPort("clock"), newKernel);

            List<Net> netsToRename = new ArrayList<>();
            for (Net n : shell.getNets()) {
                if (n.getName().startsWith("streaming_wrapper/if" + i)) {
                    netsToRename.add(n);
                }
            }
            for (Net n : netsToRename) {
                n.rename(n.getName().replace("streaming_wrapper/if" + i, "streaming_wrapper/kernel" + i + "/if"));
            }

            /*
            // Causes conflicts, I think because the Module code moves some of the static net pins as well
            for (NetType nt : new NetType[]{NetType.VCC, NetType.GND}) {
                Net staticNet = shell.getStaticNet(nt);
                Tile origAnchor = kernelM.getAnchor().getTile();
                Tile newAnchor = mi.getAnchor().getTile();
                for (PIP p : kernelStaticNets.get(nt)) {
                    Tile translatedTile = Module.getCorrespondingTile(p.getTile(), newAnchor, origAnchor);
                    PIP translatedPip = new PIP(translatedTile, p.getStartWireIndex(), p.getEndWireIndex());
                    if (!staticNet.getPIPs().contains(translatedPip)) {
                        staticNet.addPIP(translatedPip);
                    } else {
                        System.out.println("Repeated!");
                    }
                }
            }
            */

        }

        // System.out.println("Is placed: " + shell.getCell("streaming_wrapper/kernel1/if/klut161").isPlaced());
        // printSiteInst(shell.getCell("streaming_wrapper/kernel0/if/klut161").getSiteInst());
        // System.out.println();
        // printSiteInst(shell.getCell("streaming_wrapper/kernel1/if/klut161").getSiteInst());

        shell.setAutoIOBuffers(false);
        shell.setDesignOutOfContext(true);
        shell.writeCheckpoint("/home/jamestho/merged_shell_kernel.dcp");
    }
}
