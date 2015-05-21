package com.med101.obdobdobd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;

/**
 * Created by alexgaluska on 04/05/15.
 */
public class MyMultiCastSocket {
    SocketAddress mSocketAddress;
    MulticastSocket multicastSocket;
    InetAddress broadcastAddress;

    public MyMultiCastSocket(InetAddress addr) throws IOException {
        multicastSocket = new MulticastSocket(55325);
        broadcastAddress = addr;
        multicastSocket.joinGroup(broadcastAddress);
    }

    public void send(String data) throws IOException {
        DatagramPacket dp = new DatagramPacket(data.getBytes(),data.length(),broadcastAddress,MainActivity.Port);
        multicastSocket.setTimeToLive(2);
        multicastSocket.send(dp);
    }

    public void receive(){

    }
}

