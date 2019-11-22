/*
 * Copyright (c) 2017 Xilinx, Inc.
 * All rights reserved.
 *
 * Author: Zac Blair, Xilinx Research Labs.
 *
 * This file is part of RapidWright.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.xilinx.rapidwright.examples;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Created on: Feb 22, 2017
 */
public class CompareRouteDB {

    private static final boolean HEADER_MISMATCH_EXIT = false;

    public static void main(String[] args) {
        args = new String[]{"/home/jamestho/2_hole_merged_route_working_cl_routes.txt", "/home/jamestho/2_hole_merged_post_route_cl_routes.txt"};

        //MessageGenerator.waitOnAnyKey();

/*
        if(args.length != 2){
            System.out.println("Usage is: CompareRouteDB <file1> <file2>\n\tFiles are the textual output of reportRouteStatus in Vivado.");
            return;
        }
        */

        List<String> f1Lines = null;
        List<String> f2Lines = null;

        try {
            f1Lines = Files.readAllLines(Paths.get(args[0]), Charset.forName("US-ASCII"));
            f2Lines = Files.readAllLines(Paths.get(args[1]), Charset.forName("US-ASCII"));
        } catch (IOException e) {
            System.out.println("Error reading input files.\n" + e);
        }

        //Compare headers
        int lineIdx1 = 0, lineIdx2 = 0;
        String header1, header2;
        while(true) {
            header1 = f1Lines.get(lineIdx1);
            header2 = f2Lines.get(lineIdx1++);
            if(header1.contains("Logical Net Detailed Routing")) {
                lineIdx2 = lineIdx1;
                if(header2.contains("Logical Net Detailed Routing")) {
                    break;
                }
                System.out.println("Mismatched line count in header. Errors may be reported below header.");
                if(HEADER_MISMATCH_EXIT)
                    return;
                else {
                    while(!f2Lines.get(lineIdx2++).contains("Logical Net Detailed Routing"));
                    break;
                }
            }
            if(!header1.equals(header2)) {
                System.out.println("\nMismatched line in header:");
                System.out.println(String.format("%-80s", args[0]) + header1);
                System.out.println(String.format("%-80s", args[1]) + header2);
            }
        }

        //Index nets of file 2 string list.
        HashMap<String,Integer> f2NetMap = new HashMap<String,Integer>();
        f2NetMap.put(f2Lines.get(lineIdx2), lineIdx2);
        for(int lineNum = lineIdx2; lineNum < f2Lines.size()-2; lineNum++) {
            if(f2Lines.get(lineNum).equals("")) {
                lineNum++;
                if(f2Lines.get(lineNum).equals("Special nets:")){
                    lineNum++;
                }
                f2NetMap.put(f2Lines.get(lineNum), lineNum);
            }
        }

        Pattern pat = Pattern.compile("[\\[\\]{}\\(\\)<>-]");

        //Loop through all nets.
        while(lineIdx1 < f1Lines.size()) {
            //Get all net data from file 1.
            List<String> net1Lines = new ArrayList<String>();
            String line = null;
            while(!((line = f1Lines.get(lineIdx1++)).equals(""))) {
                net1Lines.add(line);
            }
            if(net1Lines.get(0).equals("Special nets:")) net1Lines.remove(0);
            String netName = net1Lines.get(0);
            String status1 = net1Lines.get(1);

            //Get data for same net from file 2.
            List<String> net2Lines = new ArrayList<String>();
            Integer net2LineNum = f2NetMap.get(netName);
            if(net2LineNum == null) {
                System.out.println("\nNet " + netName + " is missing from file 2!");
                continue;
            }
            while(!((line = f2Lines.get(net2LineNum++)).equals(""))) {
                net2Lines.add(line);
            }

            //Compare status of nets
            String status2 = net2Lines.get(1);
            if(!status1.equals(status2)) {
                System.out.println("\nMismatched status for Net: " + netName);
                System.out.println(String.format("%-80s", args[0]) + status1);
                System.out.println(String.format("%-80s", args[1]) + status2);
                continue;
            }
            if(status1.equals("  Routing status: INTRASITE")
                    || status1.equals("  Routing status: NOLOADS")
                    || status1.equals("  Routing status: INTRASITE")) {
                continue; //Nothing to compare.
            }

            //Build subtrees of each net
            ArrayList<HashSet<PIPTriple>> subTrees1 = new ArrayList<HashSet<PIPTriple>>();
            ArrayList<HashSet<PIPTriple>> subTrees2 = new ArrayList<HashSet<PIPTriple>>();
            HashSet<PIPTriple> currTree = null;
            for(int lineNum = 4; lineNum < net1Lines.size()-1; lineNum++) {
                if(net1Lines.get(lineNum).contains("Subtree:")){
                    currTree = new HashSet<PIPTriple>();
                    subTrees1.add(currTree);
                    continue;
                }
                String pipline = net1Lines.get(lineNum);
                //pipline = pipline.replaceAll("[\\[\\]{}\\(\\)<>-]"," ").trim();
                pipline = pat.matcher(pipline).replaceAll(" ").trim();
                String[] pipvals = pipline.split("\\s+");
                currTree.add(new PIPTriple(pipvals[0], pipvals[1], pipvals.length==4 ? pipvals[2] : null));
            }
            for(int lineNum = 4; lineNum < net2Lines.size()-1; lineNum++) {
                if(net2Lines.get(lineNum).contains("Subtree:")){
                    currTree = new HashSet<PIPTriple>();
                    subTrees2.add(currTree);
                    continue;
                }
                String pipline = net2Lines.get(lineNum);
                //pipline = pipline.replaceAll("[\\[\\]{}\\(\\)<>-]"," ").trim();
                pipline = pat.matcher(pipline).replaceAll(" ").trim();
                String[] pipvals = pipline.split("\\s+");
                currTree.add(new PIPTriple(pipvals[0], pipvals[1], pipvals.length==4 ? pipvals[2] : null));
            }

            //Compare contents of nets
            for(int tree1Num=0; tree1Num<subTrees1.size(); tree1Num++) {
                boolean matchFound = false;
                HashSet<PIPTriple> set1 = subTrees1.get(tree1Num);
                for(int tree2Num=0; tree2Num<subTrees2.size(); tree2Num++) {
                    HashSet<PIPTriple> set2 = subTrees2.get(tree2Num);
                    //if(set2.containsAll(set1) && set1.containsAll(set2)) {
                    if(set2.containsAll(set1) && (set1.size() == set2.size())) {
                        matchFound = true;
                        break;
                    }
                }
                if(!matchFound) {
                    System.out.println("\nNo match found for Subtree " + tree1Num + " of net " + netName + " from file " + args[0]);
                }
            }
        }
    }


    private static class PIPTriple {

        public String load;
        public String index;
        public String driver;
        private int hash;

        public PIPTriple(String load, String index, String driver) {
            this.load = load;
            this.index = index;
            this.driver = driver;
            this.hash = load.hashCode() ^ index.hashCode() ^ (driver == null ? 0 : driver.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            PIPTriple p = (PIPTriple) o;
            boolean b = true;
            b &= load.equals(p.load);
            b &= index.equals(p.index);
            b &= (driver==null ? p.driver == null : driver.equals(p.driver));
            return b;
        }

        @Override
        public String toString() {
            return load + "," + index + "," + driver;
        }

        @Override
        public int hashCode() {
            return hash;
        }

    }

}