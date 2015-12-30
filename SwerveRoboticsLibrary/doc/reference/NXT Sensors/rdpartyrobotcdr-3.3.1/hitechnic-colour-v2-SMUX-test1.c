#pragma config(Sensor, S1,     HTSMUX,              sensorI2CCustom)
//*!!Code automatically generated by 'ROBOTC' configuration wizard               !!*//

/*
 * $Id: hitechnic-colour-v2-SMUX-test1.c 133 2013-03-10 15:15:38Z xander $
 */

/**
 * hitechnic-colour-v2.h provides an API for the HiTechnic Color V2 Sensor.  This program
 * demonstrates how to use that API.

 * Changelog:
 * - 0.1: Initial release
 * - 0.2: More comments
 * - 0.3: Now uses HTSMUX driver which has been split from common.h\n
 *        HTSMUX initialisation and scanning functions have been removed\n
 *
 * Credits:
 * - Big thanks to HiTechnic for providing me with the hardware necessary to write and test this.
 *
 * License: You may use this code as you wish, provided you give credit where it's due.
 *
 * THIS CODE WILL ONLY WORK WITH ROBOTC VERSION 3.59 AND HIGHER. 

 * Xander Soldaat (xander_at_botbench.com)
 * 20 February 2011
 * version 0.3
 */

#include "drivers/hitechnic-sensormux.h"
#include "drivers/hitechnic-colour-v2.h"

// The sensor is connected to the first port
// of the SMUX which is connected to the NXT port S1.
// To access that sensor, we must use msensor_S1_1.  If the sensor
// were connected to 3rd port of the SMUX connected to the NXT port S4,
// we would use msensor_S4_3

// Give the sensor a nice easy to use name
const tMUXSensor HTCOLOR = msensor_S1_1;

task main () {
  int _color = 0;
  string _tmp;
  int red = 0;
  int green = 0;
  int blue = 0;

  nxtDisplayCenteredTextLine(0, "HiTechnic");
  nxtDisplayCenteredBigTextLine(1, "COLOR V2");
  nxtDisplayCenteredTextLine(3, "SMUX Test");
  nxtDisplayCenteredTextLine(5, "Connect SMUX to");
  nxtDisplayCenteredTextLine(6, "S1 and CS to");
  nxtDisplayCenteredTextLine(7, "SMUX Port 1");
  wait1Msec(2000);

  eraseDisplay();
  while (true) {
    // Read the currently detected colour from the sensor
    _color = HTCS2readColor(HTCOLOR);

    // If colour == -1, it implies an error has occurred
    if (_color < 0) {
      nxtDisplayTextLine(4, "ERROR!!");
      nxtDisplayTextLine(5, "HTCS2readColor()");
      wait1Msec(2000);
      StopAllTasks();
    }

    // Read the RGB values of the currently colour from the sensor
    // A return value of false implies an error has occurred
    if (!HTCS2readRGB(HTCOLOR, red, green, blue)) {
      nxtDisplayTextLine(4, "ERROR!!");
      nxtDisplayTextLine(5, "HTCS2readRGB()");
      wait1Msec(2000);
      StopAllTasks();
    }

    nxtDisplayCenteredTextLine(0, "Color: %d", _color);
    nxtDisplayCenteredBigTextLine(1, "R  G  B");

    nxtEraseRect(0,10, 99, 41);
    nxtFillRect( 0, 10, 30, 10 + (red+1)/8);
    nxtFillRect(35, 10, 65, 10 + (green+1)/8);
    nxtFillRect(70, 10, 99, 10 + (blue+1)/8);
    StringFormat(_tmp, " %3d   %3d", red, green);
    nxtDisplayTextLine(7, "%s   %3d", _tmp, blue);

    wait1Msec(100);
  }
}

/*
 * $Id: hitechnic-colour-v2-SMUX-test1.c 133 2013-03-10 15:15:38Z xander $
 */
