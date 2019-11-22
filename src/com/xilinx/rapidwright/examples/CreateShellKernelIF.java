package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.Cell;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.design.Unisim;
import com.xilinx.rapidwright.device.BELPin;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.edif.*;
import com.xilinx.rapidwright.router.Router;

public class CreateShellKernelIF {
    public static void main(String[] args) {
        // String path = "/home/jamestho/blockCache/2018.2/d74dbc374ad21a9e/passthrough_4_StreamingCore_0_0_opt.dcp";
        String path = "/home/jamestho/kernel_opt.dcp";
        Design kernel = Design.readCheckpoint(path);
        Design d = new Design("IF", Device.AWS_F1);
        EDIFCell top = d.getNetlist().getTopCell();
        int startSliceY = 539;
        int startSliceX = 11;
        int i = 0;
        // EDIFCellInst curTopCellInst = kernel.getNetlist().getCellInstFromHierName("inst");
        EDIFCellInst curTopCellInst = kernel.getNetlist().getTopCellInst();
        for (EDIFPort port : curTopCellInst.getCellType().getPorts()) {
            if (port.getName().equals("clock")) {
                continue;
            }
            EDIFPort kernelPort = top.createPort(port.getName(),
                    port.isInput() ? EDIFDirection.OUTPUT : EDIFDirection.INPUT, port.getWidth());
            EDIFPort shellPort = top.createPort("shell_" + port.getName(),
                    port.isInput() ? EDIFDirection.INPUT : EDIFDirection.OUTPUT, port.getWidth());
            for (int j = 0; j < port.getWidth(); j++) {
                Net newPhysNet = d.createNet("ifn" + i);
                EDIFNet newNet = newPhysNet.getLogicalNet();
                int yOffset = i / 16;
                String lutLetter = Character.toString((char) ('H' - i / 2 % 8));
                String lutKind = i % 2 == 0 ? "5" : "6";
                String belPin = i % 2 == 0 ? "4" : "5";
                Cell slut = d.createAndPlaceCell("slut" + i, Unisim.LUT1,
                        "SLICE_X" + startSliceX + "Y" + (startSliceY - yOffset) + "/" + lutLetter + lutKind + "LUT");
                slut.addProperty("INIT", "2'h2", EDIFValueType.STRING);
                Cell klut = d.createAndPlaceCell("klut" + i, Unisim.LUT1,
                        "SLICE_X" + (startSliceX + 2) + "Y" + (startSliceY - yOffset) + "/" + lutLetter + lutKind + "LUT");
                klut.addProperty("INIT", "2'h2", EDIFValueType.STRING);
                System.out.println(slut.getSite() + " " + klut.getSite());
                // System.out.printf("set_property LOCK_PINS {I0:A%s} [get_cell if/%s]\n", belPin, "slut" + i);
                // System.out.printf("set_property LOCK_PINS {I0:A%s} [get_cell if/%s]\n", belPin, "klut" + i);
                slut.removePinMapping("A" + lutKind);
                slut.addPinMapping("A" + belPin, "I0");
                klut.removePinMapping("A" + lutKind);
                klut.addPinMapping("A" + belPin, "I0");
                newNet.createPortInst("O", port.isOutput() ? klut : slut);
                newNet.createPortInst("I0", port.isOutput() ? slut : klut);
                Cell outputLut = port.isOutput() ? klut : slut;
                BELPin src = outputLut.getBEL().getPin("O" + lutKind);
                BELPin snk = outputLut.getSite().getBELPin(lutLetter + (i % 2 == 0 ? "MUX/" + lutLetter + "MUX" : "_O"));
                outputLut.getSiteInst().routeIntraSiteNet(newPhysNet, src, snk);
                Cell inputLut = port.isOutput() ? slut : klut;
                BELPin bp = inputLut.getBEL().getPin(inputLut.getPhysicalPinMapping("I0"));
                inputLut.getSiteInst().routeIntraSiteNet(newPhysNet, bp, bp);
                // if (i % 2 == 0) {
                    d.getVccNet().createPin(false, lutLetter + "6", klut.getSiteInst());
                    d.getVccNet().createPin(false, lutLetter + "6", slut.getSiteInst());
                // }
                EDIFNet kPortNet = top.createNet(port.getName() + j + "_kif");
                kPortNet.createPortInst(port.isOutput() ? "I0" : "O", klut);
                kPortNet.createPortInst(kernelPort, j);
                Net kPortPhysNet = d.createNet(kPortNet);
                if (port.isOutput()) {
                    bp = klut.getBEL().getPin(klut.getPhysicalPinMapping("I0"));
                    klut.getSiteInst().routeIntraSiteNet(kPortPhysNet, bp, bp);
                } else {
                    src = klut.getBEL().getPin("O" + lutKind);
                    snk = klut.getSite().getBELPin(lutLetter + (i % 2 == 0 ? "MUX/" + lutLetter + "MUX" : "_O"));
                    klut.getSiteInst().routeIntraSiteNet(kPortPhysNet, src, snk);
                }
                EDIFNet sPortNet = top.createNet(port.getName() + j + "_sif");
                sPortNet.createPortInst(port.isOutput() ? "O" : "I0", slut);
                sPortNet.createPortInst(shellPort, j);
                Net sPortPhysNet = d.createNet(sPortNet);
                if (!port.isOutput()) {
                    bp = slut.getBEL().getPin(slut.getPhysicalPinMapping("I0"));
                    slut.getSiteInst().routeIntraSiteNet(sPortPhysNet, bp, bp);
                } else {
                    src = slut.getBEL().getPin("O" + lutKind);
                    snk = slut.getSite().getBELPin(lutLetter + (i % 2 == 0 ? "MUX/" + lutLetter + "MUX" : "_O"));
                    slut.getSiteInst().routeIntraSiteNet(sPortPhysNet, src, snk);
                }
                i++;
            }
        }
        // d.routeSites();
        d.setAutoIOBuffers(false);
        d.setDesignOutOfContext(true);
        // Router r = new Router(d);
        // r.routeDesign();
        d.writeCheckpoint("/home/jamestho/shell_kernel_if.dcp");
    }
}