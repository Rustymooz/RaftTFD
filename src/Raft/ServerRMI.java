package Raft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Executors;

public class ServerRMI implements RegisterHandler {

    private final LinkedList<Operation> operations;
    public int request_number;
    private ClientRMI clientRMI;
    private HashMap<Client, List<Integer>> clientSerialNumbers;
    public static volatile boolean flag = true;
    public static volatile long startTime = 0;
    public static volatile double random = 0;
    public int term = 0;
    public int already_voted = 0;
    public int replicaLeader = -1;

    public int increase = 0;

    public ServerRMI(int port, String id, ClientRMI clientRMI) {
        this.request_number = 0;
        this.clientRMI = clientRMI;
        operations = new LinkedList<>();
        clientSerialNumbers = new HashMap<>();
        int max = 6000;
        int min = 4000;

        random = Math.random()*(max-min+1)+min;
        try {
            // think of this like the server
            //RegisterHandler obj = new ServerRMI();
            RegisterHandler stub = (RegisterHandler) UnicastRemoteObject.exportObject(this, port);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind(id, stub);

            System.out.println("Sou a replica: " + id);

            //timer for each replia
            //this timer is refreshed when an entry or an heartbeat is received
            //if timeout, the replica becomes candidate and sends a vote request
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        startTime = System.currentTimeMillis();
                        while(true){
                            if(flag && !clientRMI.marosca){
                                long tempo_que_passou = System.currentTimeMillis() - startTime;
                                if(random < tempo_que_passou){
                                    random = Math.random()*(max-min+1)+min;
                                    term++;
                                    System.out.println(term);
                                    startTime = System.currentTimeMillis();
                                    System.out.println("A enviar vote request");
                                    clientRMI.sendVoteRequest();
                                }
                            }else{
                                long tempo_que_passou = System.currentTimeMillis() - startTime;
                                if(random < tempo_que_passou){
                                    random = Math.random()*(max-min+1)+min;
                                    flag = true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            });

        } catch (Exception e) {
            System.err.println("Server exception");
            throw new RuntimeException(e.getCause());
        }
    }

    public LinkedList<Operation> getOperations() {
        return operations;
    }

    // processes the request from the client, adds in the log
    @Override
    public byte[] processRequest(String requestLabel, byte[] requestData, int request_number) throws RemoteException, NotBoundException {
        byte[] operation;
        System.out.println("processRequest");
        //System.out.println(this.clientRMI.id);
        switch (requestLabel) {
            case "GET" -> {
                try {
                    for (Operation operation1 : operations) {
                        if(operation1.compareRequest(requestData)){
                            return operation1.getOperations();
                        }
                    }
                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            }
            case "ADD" -> {
                if(this.request_number == 0){
                    operations.add(new Operation(requestData,request_number));
                    System.out.println("Data added to list");
                    return requestData;
                }
                if(operations.getLast().getRequest_number() == request_number - 1){
                    operations.add(new Operation(requestData,request_number));
                    System.out.println("Data added to list");
                    return requestData;
                }
                System.out.println("log nao ta atualizado");
            }
            case "STATE" -> {
                if(operations.size() == 0){
                    return null;
                }

                ByteArrayOutputStream bytesToReturn = new ByteArrayOutputStream();
                for (Operation operation1 : operations) {
                    for (byte b : operation1.getOperations()) {
                        bytesToReturn.write(b);
                    }
                }
                return bytesToReturn.toByteArray();
            }
            default -> {
                return null;
            }
        }
        return null;
    }

    // answers to vote request, if that request has an higher term, it will vote for it
    @Override
    public boolean answerVoteRequest(int term){

        if(term <= already_voted){
            return false;
        }else {
            this.clientRMI.marosca = false;
            this.already_voted = term;
            this.term = term;

            startTime = System.currentTimeMillis();

            return true;
        }
    }

    // resets the timer
    public void receiveHeartBeat(int term, int replica) throws RemoteException{
        this.term = term;
        this.already_voted = term;
        flag = false;
        this.replicaLeader = replica;
        this.clientRMI.marosca = false;

        startTime = System.currentTimeMillis();

        System.out.println("Heartbeat received from replica: " + replica);
    }

    // update own log with the client requests
    public byte[] processOwnOperation(String requestLabel, byte[] requestData, int request_number){
        byte[] operation;
        switch (requestLabel) {
            case "GET" -> {
                try {
                    for (Operation operation1 : operations) {
                        if(operation1.compareRequest(requestData)){
                            return operation1.getOperations();
                        }
                    }
                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            }
            case "ADD" -> {
                if(this.request_number == 0){
                    operations.add(new Operation(requestData,request_number));
                    System.out.println("Data added to list");
                    return requestData;
                }
                if(operations.getLast().getRequest_number() == request_number - 1){
                    operations.add(new Operation(requestData,request_number));
                    System.out.println("Data added to list");
                    return requestData;
                }
                System.out.println("log nao ta atualizado");
            }
            case "STATE" -> {
                if(operations.size() == 0){
                    return null;
                }

                ByteArrayOutputStream bytesToReturn = new ByteArrayOutputStream();
                for (Operation operation1 : operations) {
                    for (byte b : operation1.getOperations()) {
                        bytesToReturn.write(b);
                    }
                }
                return bytesToReturn.toByteArray();
            }
            default -> {
                return null;
            }
        }
        return null;
    }

    // retrieves the current term of the server
    public int getTerm(){
        return term;
    }

    // receives the request from the client
    // clientSerialNumbers is a hash table containing all the serial number requests already executed for each client
    @Override
    public byte[] execute(String requestLabel, byte[] byteArray, Client client, int serialNumber) throws NotBoundException, RemoteException {

        System.out.println("entrou execute");

        if(!this.clientRMI.isLeader()) return null;
        // se a tabela já tem o cliente e o respetivo pedido, não faz nada
        if(clientSerialNumbers.containsKey(client)) {
            if(clientSerialNumbers.get(client).contains(serialNumber)){
                System.out.println("Operation already executed, returning byte array");
                return byteArray;
            }
        }
        // se tem o cliente mas nao tem o pedido, adiciona o pedido e executa
        if(clientSerialNumbers.containsKey(client)){
            clientSerialNumbers.get(client).add(serialNumber);
            return this.clientRMI.appendEntry(requestLabel, byteArray, request_number+1);
        }
        // se não tem nada, adiciona o cliente, o pedido e executa
        clientSerialNumbers.put(client, new ArrayList<Integer>());
        clientSerialNumbers.get(client).add(serialNumber);
        return this.clientRMI.appendEntry(requestLabel, byteArray, request_number+1);
    }

    public boolean isLeader(){
        return this.clientRMI.isLeader();
    }

    public int getId(){
        return Integer.parseInt(this.clientRMI.getId());
    }

    public int getReplicaLeader() {
        return replicaLeader;
    }
    public int increaseBy(int x) throws RemoteException{
        increase += x;
        System.out.println("Simple program: " + increase);
        return increase;
    }
}