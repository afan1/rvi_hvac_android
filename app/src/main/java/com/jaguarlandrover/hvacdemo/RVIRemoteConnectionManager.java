package com.jaguarlandrover.hvacdemo;
/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *
 * Copyright (c) 2015 Jaguar Land Rover.
 *
 * This program is licensed under the terms and conditions of the
 * Mozilla Public License, version 2.0. The full text of the
 * Mozilla Public License is at https://www.mozilla.org/MPL/2.0/
 *
 * File:    RVIRemoteConnectionManager.java
 * Project: HVACDemo
 *
 * Created by Lilli Szafranski on 5/19/15.
 *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

import android.util.Log;

public class RVIRemoteConnectionManager implements RVIRemoteConnectionInterface.RemoteConnectionListener
{
    private final static String TAG = "HVACDemo:RVIRemoteCo...";

    private static RVIRemoteConnectionManager ourInstance = new RVIRemoteConnectionManager();

    private boolean mUsingProxyServer;

    private RVIServerConnection    mProxyServerConnection;
    private RVIBluetoothConnection mBluetoothConnection;
    private RVIServerConnection    mDirectServerConnection;

    private RVIRemoteConnectionListener mListener;

    private RVIRemoteConnectionManager() {
        mProxyServerConnection = new RVIServerConnection();
        mBluetoothConnection = new RVIBluetoothConnection();
        mDirectServerConnection = new RVIServerConnection();
    }

    public static void connect() {
        ourInstance.closeConnections();

        RVIRemoteConnectionInterface remoteConnection = ourInstance.selectEnabledRemoteConnection();

        if (remoteConnection == null) return;

        remoteConnection
                .setRemoteConnectionListener(ourInstance); // TODO: Doing it this way, dynamically selecting a connection at the beginning and later when sending messages,
        // TODO, cont': but only setting the listener here, will lead to funny async race conditions later; fix.
        remoteConnection.connect();
    }

    public static void disconnect() {
        ourInstance.closeConnections();
        ourInstance.mListener.onRVIDidDisconnect();
    }

    public static void sendPacket(RVIDlinkPacket dlinkPacket) {
        Log.d(TAG, Util.getMethodName());

        RVIRemoteConnectionInterface remoteConnection = ourInstance.selectConnectedRemoteConnection();

        if (remoteConnection == null) return; // TODO: Implement a cache to send out stuff after a connection has been established

        remoteConnection.sendRviRequest(dlinkPacket);
    }

    private RVIRemoteConnectionInterface selectConnectedRemoteConnection() {
        if (mDirectServerConnection.isEnabled() && mDirectServerConnection.isConnected() && !mUsingProxyServer)
            return mDirectServerConnection;
        if (mProxyServerConnection.isEnabled() && mProxyServerConnection.isConnected() && mUsingProxyServer)
            return mProxyServerConnection;
        if (mBluetoothConnection.isEnabled() && mBluetoothConnection.isConnected()) {
            return mBluetoothConnection;
        }

        return null;
    }

    private RVIRemoteConnectionInterface selectEnabledRemoteConnection() { // TODO: This is going to be buggy if a connection is enabled but not connected; the other connections won't have connected
        if (mDirectServerConnection.isEnabled() && !mUsingProxyServer)     // TODO: Rewrite better 'chosing' code
            return mDirectServerConnection;
        if (mProxyServerConnection.isEnabled() && mUsingProxyServer)
            return mProxyServerConnection;
        if (mBluetoothConnection.isEnabled()) {
            return mBluetoothConnection;
        }

        return null;
    }

    private void closeConnections() {
        mDirectServerConnection.disconnect();
        mProxyServerConnection.disconnect();
        mBluetoothConnection.disconnect();
    }

    @Override
    public void onRemoteConnectionDidConnect() {
        mListener.onRVIDidConnect();
    }

    @Override
    public void onRemoteConnectionDidFailToConnect(Error error) {
        mListener.onRVIDidFailToConnect(error);
    }

    @Override
    public void onRemoteConnectionDidReceiveData(String data) {
        mListener.onRVIDidReceiveData(data);
    }

    @Override
    public void onDidSendDataToRemoteConnection() {
        mListener.onRVIDidSendData();
    }

    @Override
    public void onDidFailToSendDataToRemoteConnection(Error error) {
        mListener.onRVIDidFailToSendData(error);
    }

    public static void setServerUrl(String serverUrl) {
        RVIRemoteConnectionManager.ourInstance.mDirectServerConnection.setServerUrl(serverUrl);
    }

    public static void setServerPort(Integer serverPort) {
        RVIRemoteConnectionManager.ourInstance.mDirectServerConnection.setServerPort(serverPort);
    }

    public static void setProxyServerUrl(String proxyServerUrl) {
        RVIRemoteConnectionManager.ourInstance.mProxyServerConnection.setServerUrl(proxyServerUrl);
    }

    public static void setProxyServerPort(Integer proxyServerPort) {
        RVIRemoteConnectionManager.ourInstance.mProxyServerConnection.setServerPort(proxyServerPort);
    }

    public static void setUsingProxyServer(boolean usingProxyServer) {
        RVIRemoteConnectionManager.ourInstance.mUsingProxyServer = usingProxyServer;
    }

    public static RVIRemoteConnectionListener getListener() {
        return RVIRemoteConnectionManager.ourInstance.mListener;
    }

    public static void setListener(RVIRemoteConnectionListener listener) {
        RVIRemoteConnectionManager.ourInstance.mListener = listener;
    }
}
