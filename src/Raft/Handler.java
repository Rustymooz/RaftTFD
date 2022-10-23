package Raft;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Handler extends Remote {

    String Invoke(String id, String requestLabel, String requestData) throws RemoteException, NotBoundException;
}
