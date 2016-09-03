/*
#%L
  **********************************************************************
 * PROJECT       :  Shutters
 * FILENAME      :  Pi4jEmission.java
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

public final class Pi4jEmission {
    private short timeHigh = (short) 220;
    private short timeLow  = (short) 1220;
    private short pin = (short) -1;

    /**
    * Simple constructor to set the pin to write to
    *  @param pin the pin ID in the BCM GPIO numbering for more information
    *    see : https://projects.drogon.net/raspberry-pi/wiringpi/pins/
    */
    public Pi4jEmission(short pin){
        this.pin = pin;
    }

    /**
    * Send the real bit. Known that it is frequence modulation
    * a 1 is some microseconds High and some microseconds Low
    */
    private void sendBit(boolean bit){
        if (bit) {
            Gpio.digitalWrite(this.pin, Gpio.HIGH);
            Gpio.delayMicroseconds(this.timeHigh);
            Gpio.digitalWrite(this.pin, Gpio.LOW);
            Gpio.delayMicroseconds(this.timeLow);
        } else {
            Gpio.digitalWrite(this.pin, Gpio.HIGH);
            Gpio.delayMicroseconds(this.timeHigh);
            Gpio.digitalWrite(this.pin, Gpio.LOW);
            Gpio.delayMicroseconds(this.timeHigh);
        }
    }

    /**
    * Manchester Encoding bits
    *
    * In this encodig, to send the value 1 we really send 10
    * and to send 0 we really send 01
    *
    * @param bit the original bit to send
    **/
    private void sendPair(boolean bit){
        if (bit){
            sendBit(true);
            sendBit(false);
        } else {
            sendBit(false);
            sendBit(true);
        }
    }

    /**
    * In order to prepare the GPIO and the WiringPi system
    * I add some noises before sending real stuff
    **/
    private void sendNoise() {
        // Send noise
        sendBit(true);
        sendBit(false);
        sendBit(true);
        sendBit(false);
        sendBit(false);
        sendBit(true);
        sendBit(true);
        sendBit(false);
    }

    /**
    * Send the locks. A very long Low (10560 us), a short High then a long Low (2750 us)
    */
    private void sendLocks(){
        // First Lock
        Gpio.digitalWrite(this.pin, Gpio.LOW);
        Gpio.delayMicroseconds(10720);

        Gpio.digitalWrite(this.pin, Gpio.HIGH);
        Gpio.delayMicroseconds(this.timeHigh);

        // Second lock
        Gpio.digitalWrite(this.pin, Gpio.LOW);
        Gpio.delayMicroseconds(3030);
    }

    private String getCodeAsBitString(int code){
        // convert code to binary code
        final String str = Integer.toBinaryString(code);
        // must be 26 bits
        if (str.length() < 26){
            final String padStr = "0";
            final int padLen = padStr.length();
            final int strLen = str.length();
            final int pads = 26 - strLen;

            if (pads == padLen) {
                return padStr.concat(str);
            } else if (pads < padLen) {
                return padStr.substring(0, pads).concat(str);
            } else {
                final char[] padding = new char[pads];
                final char[] padChars = padStr.toCharArray();
                for (int i = 0; i < pads; i++) {
                    padding[i] = padChars[i % padLen];
                }
                return new String(padding).concat(str);
            }
        }

        return str;
    }

    public void send(int code, boolean open) throws InterruptedException {
        // check if pin numberhas been set
        if (this.pin == -1) {
            System.out.println(" ==>> OUTPUT PIN SETUP FAILED");
            System.exit(-1);
            return;
        }

        // setup wiringPi
        if (Gpio.wiringPiSetupSys() == -1) {
            System.out.println(" ==>> GPIO SETUP FAILED");
            System.exit(-2);
            return;
        }

        System.out.println("Set GPIO " + pin + " as OUT");
        Gpio.delayMicroseconds(500000);

        final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        final String binary = getCodeAsBitString(code);
        System.out.println("                            Remote control code : " + binary);
        final char[] chars = binary.toCharArray();

        sendNoise();

        for (int cptSend = 0; cptSend < 6; cptSend++) {

            sendLocks();

            Gpio.digitalWrite(this.pin, Gpio.HIGH);

            // send the code
            for (int cpt = 0; cpt < chars.length; cpt++) {
                if (chars[cpt] == '0'){
                    sendPair(false);
                } else if (chars[cpt] == '1') {
                    sendPair(true);
                }
            }

            // Envoie du bit définissant si c'est une commande de groupe ou non (26em bit)
            sendPair(false);

            // Envoie du bit définissant si c'est allumé ou eteint 27em bit)
            sendPair(open);

            for(int i = 0; i < 4; i++) {
                sendPair(false);
            }

            Gpio.digitalWrite(this.pin, Gpio.HIGH);
            Gpio.delayMicroseconds(this.timeHigh);
            Gpio.digitalWrite(this.pin, Gpio.LOW);

        }
    }

    public static void main(String args[]) throws InterruptedException {
        if (args.length != 3){
            System.out.println("<--Pi4J-->");
            System.out.println("  arg 0: pin to connect to");
            System.out.println("  arg 1: code to send");
            System.out.println("  arg 2: on / off");
            System.exit(1);
        }

        final short   pin   = (short)Integer.parseInt(args[0]);
        final int     code  = Integer.parseInt(args[1]);
        final boolean open  = (args[2].equals("on"));

        System.out.println("<--Pi4J--> GPIO test program");

        Pi4jEmission gpio = new Pi4jEmission(pin);
        gpio.send(code, open);

        System.out.println("Finish");
    }
}