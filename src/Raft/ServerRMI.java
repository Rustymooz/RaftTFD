package Raft;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;

public class ServerRMI implements Handler{

    private final LinkedList<String> operations;

    public ServerRMI(){
        operations = new LinkedList<>();
    };

    public ServerRMI(int port, String id) {
        operations = new LinkedList<>();
        try {
            // think of this like the server
            Handler obj = new ServerRMI();
            Handler  stub = (Handler) UnicastRemoteObject.exportObject(obj, port);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind(id, stub);

            System.out.println("Sou a replica: " + id);
        } catch (Exception e) {
            System.err.println("Server exception");
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public String Invoke(String id, String requestLabel, String requestData) throws RemoteException, NotBoundException {
        String operation;
        switch (requestLabel) {
            case "GET" -> {
                try {
                    operation = operations.get(operations.indexOf(requestData));
                    return operation;
                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            }
            case "ADD" -> {
                operations.add(requestData);
                return "Data added to list";
            }
            default -> {
                return "Insert valid label";
            }
        }
        return "Insert valid label";
    }
}