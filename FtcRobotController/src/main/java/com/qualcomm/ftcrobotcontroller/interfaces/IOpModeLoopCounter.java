package com.qualcomm.ftcrobotcontroller.interfaces;

import com.qualcomm.ftcrobotcontroller.ClassFactory;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

/**
 * Public interface to the little OpModeLoopCounter utility
 * @see ClassFactory#createLoopCounter(OpMode)
 */
public interface IOpModeLoopCounter
    {
    /**
     * Returns the number of times that loop() has been called in the associated OpMode
     * @return the number of times loop() has been called
     */
    int getLoopCount();

    /** Shuts down the loop counter */
    void close();
    }