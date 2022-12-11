package Raft;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.MarshalledObject;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.DoubleToIntFunction;
import java.util.concurrent.ThreadLocalRandom;

public class Client implements ClientLibrary, Serializable {
    Registry registry;
    RegisterHandler stub;
    private boolean boundToLeader = false;
    private int randomServer = -1;
    private int leader = -1;
    private int serialNumber;
    private int id;
    public Client(){
        id = 1;
        randomServer = ThreadLocalRandom.current().nextInt(0, 4 + 1);
        serialNumber = 0;
        serverLookup(randomServer);

        new Thread(new Client.SimpleProgram()).start();
    }

    //sends the request to the server through the execute with the byte array to be added in the log, the client serial number and the client instance
    public byte[] request(String requestLabel, byte[] reqeustArray, Client client, int serialNumber) throws RemoteException, NotBoundException {
        try{
            byte[] brita = stub.execute(requestLabel, reqeustArray, client, serialNumber);
            return brita;
        }catch (Exception e){
            System.out.println("!!!!!!!!!!!!!!!!!" + e);
            boundToLeader = false;
            serverLookup(leader);
            return stub.execute(requestLabel, reqeustArray, client, serialNumber);
        }
    }

    //First tries to bind to a random server, if it is not the leader, that server will tell the client who is the leader, client connects to it
    public void serverLookup(int serverID){
        if(!boundToLeader){
            try{
                registry = LocateRegistry.getRegistry(getPort(serverID));
                stub = (RegisterHandler) registry.lookup(Integer.toString(serverID));
                leader = stub.getReplicaLeader();
                if(stub.isLeader()){
                    boundToLeader = true;
                    System.out.println("Connected to leader " + stub.getId() + " in first try");
                }else {
                    registry = LocateRegistry.getRegistry(getPort(leader));
                    stub = (RegisterHandler) registry.lookup(Integer.toString(leader));
                    boundToLeader = true;
                    System.out.println("Connected to leader " + stub.getId() + " in second try");
                }
            }catch (Exception e){
                System.out.println("Retrying connection...");
                serverLookup(ThreadLocalRandom.current().nextInt(0, 4 + 1));
                //System.out.println(e);
            }
        }
    }

    public int getPort(int line) {
        try {
            List<String> file = Files.readAllLines(Paths.get("replicas.txt"));
            //TODO: line format is: IP space PORT
            String[] split = file.get(line).split("\s+");
            return Integer.parseInt(split[1]);

        } catch (Exception e){
            throw new RuntimeException();
        }
    }

    public static void main(String[] args){
        Client client = new Client();

        while (true){
            try{
                byte[] array = null;
                Scanner in = new Scanner(System.in);
                System.out.println("Input > ");
                String line = in.nextLine();
                String[] split = line.split("\s+");

                if (split.length == 1 && split[0].equals("STATE")) {
                    String requestLabel = split[0];
                    while (true){
                        array = client.request(requestLabel, null, client, client.serialNumber);
                        if(array != null){
                            if(array[0] == 4){
                                System.out.println("EOT");
                            }
                            System.out.println(Arrays.toString(array));
                            break;
                        }
                    }
                } else if (split.length == 2 && split[0].equals("ADD")) {
                    String requestLabel = split[0];
                    String requestData = split[1];
                    while (true){
                        array = client.request(requestLabel, requestData.getBytes(), client, client.serialNumber);
                        if(array != null){
                            if(array[0] == 4){
                                System.out.println("EOT");
                            }
                            System.out.println(Arrays.toString(array));
                            break;
                        }
                    }
                } else if (split.length == 2 && split[0].equals("GET")) {
                    String requestLabel = split[0];
                    String requestData = split[1];
                    while (true){
                        array = client.request(requestLabel, requestData.getBytes(), client, client.serialNumber);
                        if(array != null){
                            if(array[0] == 4){
                                System.out.println("EOT");
                            }
                            System.out.println(Arrays.toString(array));
                            break;
                        }
                    }
                }
                client.serialNumber++;
                System.out.println(array);

            }catch (Exception e){
                client.serverLookup(ThreadLocalRandom.current().nextInt(0, 4 + 1));
                System.out.println(e);
            }
        }

    }

    class SimpleProgram implements Runnable {
        public long startTime = System.currentTimeMillis();
        private long time = 0;
        @Override
        public void run() {
            while (true){

                time = System.currentTimeMillis() - startTime;
                if(time > 5000){
                    startTime = System.currentTimeMillis();
                    try {
                        stub.increaseBy(ThreadLocalRandom.current().nextInt(1, 4 + 1));
                    } catch (RemoteException e) {
                        serverLookup(ThreadLocalRandom.current().nextInt(0, 4 + 1));
                        // throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}