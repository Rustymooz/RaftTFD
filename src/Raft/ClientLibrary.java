package Raft;

import java.rmi.MarshalledObject;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public interface ClientLibrary {
    byte[] request(String requestLabel, byte[] reqeustArray, Client client, int serialNumber) throws RemoteException, NotBoundException;
}