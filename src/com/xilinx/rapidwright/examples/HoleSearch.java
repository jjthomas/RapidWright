package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.blocks.PBlock;
import com.xilinx.rapidwright.device.*;

public class HoleSearch {
    static int leftMargin, interfaceWidth, holeHeight, upperMargin, holeWidth;
    static PBlock toAvoid;

    enum HorizontalState {
        MARGIN,
        INTERFACE,
        HOLE
    }
    enum VerticalState {
        MARGIN,
        HOLE
    }
    public static void main(String[] args) {
        // these two constants are denominated in interconnect columns
        leftMargin = 1;
        interfaceWidth = 1;
        // these three constants are denominated in slices
        holeHeight = 15; // must be >= interface height
        upperMargin = 0;
        holeWidth = 3;
        toAvoid = new PBlock(Device.getDevice(Device.AWS_F1), "CLOCKREGION_X2Y0:CLOCKREGION_X5Y9");

        int c = 0;
        int skippedInts = 0;
        int firstInterfaceCol = -1;
        HorizontalState state = HorizontalState.MARGIN;
        while (c < FloorplanUtils.tiles[0].length) {
            TileTypeEnum t = FloorplanUtils.getColumnType(c);
            if (state == HorizontalState.MARGIN) {
                if (t == TileTypeEnum.INT) {
                    skippedInts++;
                    if (FloorplanUtils.getColumnType(c + 1) == TileTypeEnum.CLEL_R) {
                        c++; // skip attached logic column
                    }
                }
                if (skippedInts >= leftMargin) {
                    state = HorizontalState.INTERFACE;
                    skippedInts = 0;
                }
            } else if (state == HorizontalState.INTERFACE) {
                if (firstInterfaceCol == -1) {
                    if (t == TileTypeEnum.CLEM || t == TileTypeEnum.CLEL_R) {
                        firstInterfaceCol = c;
                    }
                } else if (skippedInts < interfaceWidth) {
                    if (t == TileTypeEnum.INT) {
                        skippedInts++;
                        if (FloorplanUtils.getColumnType(c + 1) == TileTypeEnum.CLEL_R) {
                            c++; // skip attached logic column
                        }
                    }
                } else {
                    if (t == TileTypeEnum.CLEM || t == TileTypeEnum.CLEL_R) {

                        firstInterfaceCol = -1;
                        skippedInts = 0;
                    }
                }
            } else { // HOLE

            }
            c++;
        }
    }
}
