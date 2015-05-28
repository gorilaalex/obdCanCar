package com.med101.obdobdobd;

/**
 * Created by alexgaluska on 28/05/15.
 */
public class Constants {

   //pid's taken from here http://www.outilsobdfacile.com/obd-mode-pid.php

    public static final String VEHICLE_IDENTIFIER = "09 02"; // vin of the vehicle
    public static final String VEHICLE_ECU_NAME = "01 0A"; //
    public static final String FUEL_SYSTEM_STATUS = "01 03";
    public static final String ENGINE_SPEED_RPM = "01 0C";
    public static final String VEHICLE_SPEED_KPH = "01 0D";
    public static final String INTANK_TEMP = "01 0F";
    public static final String AMBIENT_AIR_TEMP = "01 05";

    //at command

    public static final String ELM_VERSION = "I";
    public static final String SEND_ALL_TO_DEFAULTS = "ATD";
    public static final String RESET_OBD = "ATZ";
    public static final String ECHO_OFF= "E0";
    public static final String LINE_OFF= "L0";
    public static final String SPACES_OFF = "S0";
    public static final String HEADERS_OFF= "H0";
    public static final String AUTO_SEARCH_MODE= "SP 0";
}
