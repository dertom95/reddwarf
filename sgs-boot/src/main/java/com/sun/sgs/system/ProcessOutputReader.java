/*
 * Copyright 2007-2008 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.system;

import java.io.PrintStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Reads output stream from a process and writes it to the
 * given stream.
 */
public class ProcessOutputReader implements Runnable {
    
    private static final Logger logger = 
            Logger.getLogger(ProcessOutputReader.class.getName());
    
    private Process p;
    private PrintStream output;
    
    /**
     * Constructs a new {@code ProcessOutputReader}.
     * 
     * @param p the process to read from
     * @param output the output stream to write to
     */
    public ProcessOutputReader(Process p, PrintStream output) {
        this.p = p;
        this.output = output;
    }
    
    /**
     * Reads the output from the {@code Process} associated with this object
     * and outputs the results to the {@code PrintStream} associated with
     * this object.
     */
    @Override
    public void run() {
        InputStream processOutput = p.getInputStream();
        BufferedReader processReader = new BufferedReader(
                new InputStreamReader(processOutput));
        String line = null;
        try {
            while ((line = processReader.readLine()) != null) {
                output.println(line);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to read process output", e);
            p.destroy();
        } finally {
            try {
                if (processReader != null) {
                    processReader.close();
                }
            } catch (IOException ignore) {
                
            }
        }
    }

}
