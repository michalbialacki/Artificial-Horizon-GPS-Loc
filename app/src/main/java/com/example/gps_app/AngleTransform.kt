package com.example.gps_app

import android.content.ContentValues.TAG
import android.util.Log
import kotlin.math.sqrt

class AngleTransform (acclVals : MutableList<Double>, gyroVals : MutableList<Double>, magVals : MutableList<Double>){

    var prevAcclVals = acclVals
    var prevGyroVals = gyroVals
    var roll = 0.0
    var pitch = 0.0
    var calculatedPitch =
        Math.toDegrees(Math.atan(acclVals[1] / sqrt(acclVals[2] * acclVals[2] + acclVals[0] * acclVals[0])))
    var calculatedRoll =
        Math.toDegrees(Math.atan(-acclVals[2] / sqrt(acclVals[1] * acclVals[1] + acclVals[0] * acclVals[0])))
    var magneticYaw = Math.toDegrees(Math.atan2(magVals[1], magVals[0]))+90.0


    init {
        roll = magneticYaw
        Log.d(TAG, "${magneticYaw}: ")

        if(acclVals[0]<0){
            pitch = 90.0-calculatedPitch

        }
        else{
            pitch = (calculatedPitch)-90.0
        }


    }





}