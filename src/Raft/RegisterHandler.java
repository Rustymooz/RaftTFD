package Raft;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegisterHandler extends Remote {

    byte[] processRequest(String requestLabel, byte[] requestData, int serial_number) throws RemoteException, NotBoundException;
    boolean answerVoteRequest(int term) throws RemoteException, NotBoundException;
    void receiveHeartBeat(int term, int replica) throws RemoteException, NotBoundException;
    int getTerm() throws RemoteException;
    byte[] execute(String requestLabel, byte[] byteArray, Client client, int serialNumber) throws RemoteException, NotBoundException;
    boolean isLeader() throws RemoteException;
    int getId() throws RemoteException;
    int getReplicaLeader() throws RemoteException;
    int increaseBy(int x) throws RemoteException;

}