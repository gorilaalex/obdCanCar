package com.med101.obdobdobd;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by alexgaluska on 27/05/15.
 */

//references https://blogs.oracle.com/speakjava/entry/java_and_the_raspberry_pi

    //
public class ObdCommunication {

    private static final String TAG = "Elm327";
    private static final String ipAddress = "192.168.0.10";
    private static final int port = 35000;
    private static final byte obdResponse = (byte) 0x40;
    private static final String CR = "\n";
    private static final String LF = "\r";
    private static final String CR_LF = "\n\r";
    private static final String prompt = ">";

    private Socket mSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private byte[] response = new byte[1024];
    protected byte[] responseData = new byte[1024];
    public String stringResponse="";

    public ObdCommunication() {

    }

    //establish a socket and create the input and output streams to it
    public void init() throws IOException{
        try{
            mSocket = new Socket(ipAddress,port);
            outputStream = mSocket.getOutputStream();
            inputStream = mSocket.getInputStream();

        }
        catch(UnknownHostException ex){
            Log.d(TAG, "Unknown host " + ipAddress + " " + port);
        }
        catch(IOException ex){
            Log.d(TAG, "IOException " + ex.getMessage());
        }

        //ensure that we have input and output stream
        if(outputStream == null || inputStream==null){
            Log.d(TAG,"Input or output to device is null.");
        }

        resetInterface();

    }

    public void resetInterface() throws IOException {
        sendAtCommand(Constants.SEND_ALL_TO_DEFAULTS);//send all to defaults
        sendAtCommand(Constants.RESET_OBD);//reset obd
        sendAtCommand(Constants.ECHO_OFF); //echo off
        sendAtCommand(Constants.LINE_OFF); //line feed off
        sendAtCommand(Constants.SPACES_OFF); // spaces off
        sendAtCommand(Constants.HEADERS_OFF); // headers off
        sendAtCommand(Constants.AUTO_SEARCH_MODE);//auto mode
    }

    //send at command and return the response string
    public String sendAtCommand(String command) throws IOException{
        //construct the full command string to send.
        //we must remember to include a carriage return (ASCII 0x0D)
        String atCommand = "AT "+ command + CR_LF;
        Log.d(TAG, "Sendind at command " + atCommand);

        //send it to the interface
        outputStream.write(atCommand.getBytes());
        Log.d(TAG, "Data send");

        String responseString = getResponse();

        //delete the command, which may be echoed back
        responseString = responseString.replace("AT " + command, "");
        return responseString;
    }

    public int sendOBDCommand(String command) throws IOException{
        byte[] commandBytes = bytesStringToArray(command);

        //a valid obd command must be at least two bytes to indicate the mode and the information request

        if(commandBytes.length <2){
            Log.e(TAG,"Obd command must be at least 2 bytes ");
        }

        byte obdMode = commandBytes[0];

        //send the command to the interface
        outputStream.write((command + CR_LF).getBytes());

        Log.d(TAG,"Command sent");

        //read the response
        String responseString = getResponse();

        //remove the original command in case that gets echoed back
        responseString = responseString.replace(command,"");
        Log.d(TAG,"Response is "+ responseString);

        //if there is no data, there is no data
        if(responseString.compareTo("NO DATA")==0)
            return 0;

        //trap error message from can bus
        if(responseString.compareTo("CAN ERROR")==0)
            Log.e(TAG,"Can error detected");

        response = bytesStringToArray(responseString);
        int responseDataLength = response.length;

        //the first byte indicates a response for the request mode and the second byte is a repeat of the pid
        //we test these to ensure that the response is of the correct format

        if(responseDataLength<2)
            Log.e(TAG,"Response was too short");

        if (response[0] != (byte)(obdMode + obdResponse))
           Log.e(TAG,"ELM327: Incorrect response [" +
                    String.format("%02X", responseData[0]) + " != " +
                    String.format("%02X", (byte)(obdMode + obdResponse)) + "]");

        if (response[1] != commandBytes[1])
            Log.e(TAG,"ELM327: Incorrect command response [" +
                    String.format("%02X", responseData[1]) + " != " +
                    String.format("%02X", commandBytes[1]));

        for (int i = 0; i < responseDataLength; i++)
            Log.d(TAG,String.format("ELM327: byte %d = %02X", i, response[i]));

        responseData = Arrays.copyOfRange(response, 2, responseDataLength);

        stringResponse = responseString;
        return responseDataLength - 2;
    }

    public String sendOBDCommand2(String command) throws IOException{
        sendOBDCommand(command);
        if(stringResponse!="")
            return stringResponse;
        else return "";
    }

    //send an obs command to the car via interface. test the length of the response to see
    /* if it matches an expected value
    public int sendOBDCommand(String command, int expectedLength) throws IOException {
        int responseLength = sendOBDCommand(command);

        if(responseLength!=expectedLength)
            Log.e(TAG,"ELM327: sendOBDCommand: bad reply length ["
                    + responseLength + " != " + expectedLength + "]");
        return responseLength;
    }*/

    //get the response to a command
    //clean the response of command and other stuffs
    public String getResponse() throws IOException{
        boolean readComplete = false;
        StringBuilder responseBuilder = new StringBuilder();

        while(!readComplete){
            int readLength = inputStream.read(response);
            Log.d(TAG, "Response received , length " + readLength);

            String data = new String(Arrays.copyOfRange(response,0,readLength));
            responseBuilder.append(data);

            if(data.contains(prompt)) {
                Log.d(TAG, "Got a prompt");
                break;
            }
        }

        //strip out newline, carriage return and the prompt
        String responseString = responseBuilder.toString();
        responseString = responseString.replace(CR,"");
        responseString = responseString.replace(LF,"");
        responseString = responseString.replace(prompt,"");
        return responseString;

    }

    public boolean[] getPIDSupport(byte pid) throws IOException {
        int dataLength = sendOBDCommand("01 " + String.format("%02X", pid));

    /* If we get zero bytes back then we assume that there are no
     * supported PIDs for the requested range
     */
        if (dataLength == 0)
            return null;

        int pidCount = dataLength * 8;
        boolean[] pidList = new boolean[pidCount];
        int p = 0;

    /* Now decode the bit map of supported PIDs */
        for (int i = 2; i < dataLength; i++)
            for (int j = 0; j < 8; j++) {
                if ((responseData[i] & (1 << j)) != 0)
                    pidList[p++] = true;
                else
                    pidList[p++] = false;
            }

        return pidList;
    }

    //convert a string to a byte array
    public static byte[] bytesStringToArray(String str){
        byte[] bt = str.getBytes();
        return bt;
    }

    //region of getting informations

    //get the version number of the elm327 connected
    public String getInterfaceVersionNumber() throws IOException{
        return sendAtCommand(Constants.ELM_VERSION);
    }



    //get fuel percent
    public int getIntankeTemp() throws IOException{
        return sendOBDCommand(Constants.INTANK_TEMP);
    }

    //get vehicle speed in kph
    public int getVehicleSpeed() throws IOException{
        return sendOBDCommand(Constants.VEHICLE_SPEED_KPH);
    }

    static short[] toBytes(String data, int bytesSize) {

        if (bytesSize <= 0) {
            throw new IllegalArgumentException("illegal bytesSize");
        }

        short[] bytes = new short[bytesSize];
        int b = bytesSize;
        int index = data.length() -2;

        for (; b > 0 && index > 0; index -= 2, b -= 1) {

            while (data.charAt(index + 1) == ' ') {
                index -= 1;
            }

            String hex = data.substring(index, index + 2);
            bytes[b - 1] = Short.parseShort(hex, 16);
        }

        if (b > 0) {
            throw new NumberFormatException();
        }

        return bytes;
    };

    //get rpm
    public double getEngineRPM() throws IOException{

        sendOBDCommand(Constants.ENGINE_SPEED_RPM);
        String x = stringResponse;

        short[] bytes = toBytes(x, 2);
        double y = ((bytes[0] * 256) + bytes[1]) / 4.0;


        return y;
    }

    public int getSpeed() throws IOException{
        sendOBDCommand(Constants.VEHICLE_SPEED_KPH);
        String data = stringResponse;
        short[] bytes = toBytes(data, 1);
        return (int) bytes[0];
    }

    public int getCoolantTemperature() throws IOException{
        sendOBDCommand(Constants.AMBIENT_AIR_TEMP);

        String data = stringResponse;

        short[] bytes = toBytes(data, 1);
        return bytes[0] - 40;

    }

    public int getInTankTemp() throws IOException{
        sendOBDCommand(Constants.INTANK_TEMP);

        String data = stringResponse;

        short[] bytes = toBytes(data, 1);
        return bytes[0] - 40;

    }

    //get vin of vehicle
    public int getVehicleIdentifier() throws IOException{
        return sendOBDCommand(Constants.VEHICLE_IDENTIFIER);
    }

    //get vehicle ecu name
    public int getVehicleEcuName() throws IOException{
        return sendOBDCommand(Constants.VEHICLE_ECU_NAME);
    }

    //get fuel status
    public int getFuelSystemStatus() throws IOException{
        return sendOBDCommand(Constants.FUEL_SYSTEM_STATUS);
    }

    //get ambiental air temperature
    public int getAmbientalAirTemperature() throws IOException{
        return sendOBDCommand(Constants.AMBIENT_AIR_TEMP);
    }

}
