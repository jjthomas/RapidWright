package com.xilinx.rapidwright.examples;

import com.xilinx.rapidwright.design.ConstraintGroup;
import com.xilinx.rapidwright.design.Design;

import java.util.ArrayList;
import java.util.List;

public class RemoveClockConstraints {
    public static void main(String[] args) {
        Design d = Design.readCheckpoint(args[0]);
        List<String> newConstraints = new ArrayList<>();
        for (String constraint : d.getXDCConstraints(ConstraintGroup.IN_CONTEXT)) {
            if (!constraint.contains("clock")) {
                newConstraints.add(constraint);
            } else {
                System.out.println("Removing constraint: " + constraint);
            }
        }
        d.setXDCConstraints(newConstraints, ConstraintGroup.IN_CONTEXT);
        // doesn't change edf, so shouldn't need to rewrite that
        d.writeCheckpoint(args[0]);
    }
}
