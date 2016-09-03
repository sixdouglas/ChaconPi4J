/*
 * #%L
 * **********************************************************************
 * PROJECT       :  Shutters
 * FILENAME      :  Pi4jTraceReception.java
 *
 * This file is part of the Shutters project. More information about
 * this project can be found here:  https://github.com/sixdouglas/shutters
 * **********************************************************************
 * %%
 * Copyright (C) 2016 - 2016 Shutters
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

package org.douglas.pi4j.chaconPi4j;

import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

public final class Pi4jTraceReception {
    private short pin;
    private long[] pulses;

    /**
    * Simple constructor to set the pin to write to
    *  @param pin the pin ID in the BCM GPIO numbering for more information
    *    see : https://projects.drogon.net/raspberry-pi/wiringpi/pins/
    */
    public Pi4jTraceReception(short pin){
        this.pin = pin;
        this.pulses = new long[10000];
    }

    /**
     * Gets the time in microseconds between the current time and the end of
     * level given as parameter
     *
     * @param level the level to wait the end of
     * @param timeout time out in microseconds
     * @return the amount of microseconds needed to go from the current 
     *      level to the end of the given level
     */
    private long pulseIn(int level, long timeout){
        long startTime = System.nanoTime();

        while (Gpio.digitalRead(this.pin) != level){
            if ((System.nanoTime() - startTime) / 1000 > timeout){
                return 0;
            }
        }

        while (Gpio.digitalRead(this.pin) == level){
            if ((System.nanoTime() - startTime) / 1000 > timeout){
                return 0;
            }
        }

        return (System.nanoTime() - startTime) / 1000;
    }

    private void listen() throws InterruptedException {
        if (this.pin == -1) {
            System.out.println(" ==>> OUTPUT PIN SETUP FAILED");
            System.exit(-1);
            return;
        }

        if (Gpio.wiringPiSetupSys() == -1) {
            System.out.println(" ==>> GPIO SETUP FAILED");
            System.exit(-2);
            return;
        }

        for (int cpt = 0; cpt < this.pulses.length; cpt++) {
            this.pulses[cpt] = pulseIn(Gpio.LOW, 1000000);
        }
    }

    private void print() {
        for (int cpt = 0; cpt < this.pulses.length; cpt++) {
            System.out.println(this.pulses[cpt]);
        }
    }

    public static void main(String args[]) throws InterruptedException {
        if (args.length != 1){
            System.out.println("<--Pi4J-->");
            System.out.println("  arg 0: pin to connect to");
            System.exit(1);
        }

        final short pin = (short)Integer.parseInt(args[0]);

        System.out.println("<--Pi4J--> GPIO reception program");

        Pi4jTraceReception gpio = new Pi4jTraceReception(pin);
        gpio.listen();
        gpio.print();

        System.out.println("Finish");
    }
}