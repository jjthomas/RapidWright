package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFNetlist;
import com.xilinx.rapidwright.edif.EDIFPort;

public class ConnectIFKernel {
    public static void main(String[] args) {
        String ifPath = "/home/jamestho/shell_kernel_if_routed.dcp";
        String kernelPath = "/home/jamestho/blockCache/2018.2/d74dbc374ad21a9e/passthrough_4_StreamingCore_0_0_opt.dcp";
        Design ifD = Design.readCheckpoint(ifPath);
        Design kernelD = Design.readCheckpoint(kernelPath);
        Design d = new Design("top", Device.AWS_F1);
        Module ifM = new Module(ifD);
        EDIFNetlist netlist = d.getNetlist();
        netlist.migrateCellAndSubCells(kernelD.getTopEDIFCell());
        netlist.migrateCellAndSubCells(ifD.getTopEDIFCell());
        netlist.getTopCell().createChildCellInst("kernel", kernelD.getTopEDIFCell());
        ModuleInst ifInst = d.createModuleInst("if", ifM);
        ifInst.place(ifM.getAnchor().getSite());
        int i = 0;
        for (EDIFPort kPort : d.getTopEDIFCell().getCellInst("kernel").getCellType().getPorts()) {
            if (kPort.getName().equals("clock")) {
                EDIFPort topPort = d.getTopEDIFCell().createPort(kPort.getName(), kPort.getDirection(), kPort.getWidth());
                EDIFNet portNet = d.getTopEDIFCell().createNet(topPort.getName() + "0_pn");
                portNet.createPortInst(kPort, d.getTopEDIFCell().getCellInst("kernel"));
                portNet.createPortInst(topPort);
                continue;
            }
            EDIFPort iPort = d.getTopEDIFCell().getCellInst("if").getCellType().getPort(kPort.getBusName());
            for (int j = 0; j < kPort.getWidth(); j++) {
                EDIFNet kifNet = d.getTopEDIFCell().createNet("kifn" + i);
                kifNet.createPortInst(kPort, j, d.getTopEDIFCell().getCellInst("kernel"));
                kifNet.createPortInst(iPort, j, d.getTopEDIFCell().getCellInst("if"));
                i++;
            }
        }
        for (EDIFPort iPort : d.getTopEDIFCell().getCellInst("if").getCellType().getPorts()) {
            if (d.getTopEDIFCell().getCellInst("kernel").getCellType().getPort(iPort.getBusName()) == null) {
                EDIFPort topPort = d.getTopEDIFCell().createPort(iPort.getName(), iPort.getDirection(), iPort.getWidth());
                for (int j = 0; j < iPort.getWidth(); j++) {
                    EDIFNet portNet = d.getTopEDIFCell().createNet(topPort.getName() + j + "_pn");
                    portNet.createPortInst(iPort, j, d.getTopEDIFCell().getCellInst("if"));
                    portNet.createPortInst(topPort, j);
                }
            }
        }
        d.setAutoIOBuffers(false);
        d.setDesignOutOfContext(true);
        d.writeCheckpoint("/home/jamestho/if_kernel.dcp");
    }
}
