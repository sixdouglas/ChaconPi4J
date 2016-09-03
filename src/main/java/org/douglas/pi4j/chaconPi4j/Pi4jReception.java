/*
 * #%L
* **********************************************************************
 * PROJECT       :  Shutters
 * FILENAME      :  Pi4jReception.java
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

import java.lang.StringBuilder;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.Duration;

public final class Pi4jReception {
    private short timeHigh = (short) 220;
    private short timeLow  = (short) 1220;
    private short pin = (short) -1;

    /**
    * Simple constructor to set the pin to write to
    *  @param pin the pin ID in the BCM GPIO numbering for more information
    *    see : https://projects.drogon.net/raspberry-pi/wiringpi/pins/
    */
    public Pi4jReception(short pin){
        this.pin = pin;
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

    private void printValues(int sender, boolean group, boolean on, int recipient) {
        System.out.print(String.valueOf(sender));
        if (group) {
            System.out.print(" group   ");
        } else {
            System.out.print(" no group");
        }
        if (on) {
            System.out.print(" on ");
        } else {
            System.out.print(" off");
        }
        System.out.print(" ");
        System.out.println(String.valueOf(recipient));
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

        while (true) {
            short i = 0;
            long t = 0;
            short prevBit = 0;
            short bit = 0;
            int sender = 0;
            boolean group = false;
            boolean on = false;
            int recipient = 0;

            t = pulseIn(Gpio.LOW, 1000000);

            while (t < 2900 || t > 3230) {
                t = pulseIn(Gpio.LOW, 1000000);
            }

            while (i < 64) {
                t = pulseIn(Gpio.LOW, 1000000);
                System.out.println("t: " + t);
                if (t > 400 && t < 600) {
                    bit = 0;
                } else if (t > 1400 && t < 1800) {
                    bit = 1;
                }  else {
                    printValues(sender, group, on, recipient);
                    i = 0;
                    // just leave the current sequence
                    // and wish the next one won't have any noise
                    break;
                }

                if (i % 2 == 1) {
                    if ((prevBit ^ bit) == 0) {
                        i = 0;
                        break;
                    }

                    if (i < 53) {
                        sender <<= 1;
                        sender |= prevBit;
                    } else if (i == 53) {
                        group = prevBit == 1;
                    } else if (i == 55) {
                        on = prevBit == 1;
                    } else {
                        recipient <<= 1;
                        recipient |= prevBit;
                    }
                }

                prevBit = bit;
                i++;
            }

            if (i > 0){
                printValues(sender, group, on, recipient);
                System.exit(0);
                return;
            } else {
                System.out.println("No data!");
            }

            System.out.println("Wait before restarting");
            Thread.currentThread().sleep(0, 3000);
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

        Pi4jReception gpio = new Pi4jReception(pin);
        gpio.listen();

        System.out.println("Finish");
    }
}