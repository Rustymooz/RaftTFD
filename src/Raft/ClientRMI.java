package Raft;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.DoubleToIntFunction;

public class ClientRMI{

    private String id;
    private RegisterHandler[] stubs;
    private ServerRMI MyServerRMI;
    private boolean[] nVotes;
    private int line_cnt;
    public static volatile boolean isLeader;
    public boolean marosca;

    public ClientRMI(int port, String id) throws IOException, NotBoundException {
        MyServerRMI = new ServerRMI(port, id, this);
        this.id = id;
        line_cnt = Files.readAllLines(Paths.get("replicas.txt")).size();
        stubs = new RegisterHandler[line_cnt];
        nVotes = new boolean[line_cnt];
        isLeader = false;



        //this thread bind will keep trying to bind all stubs, after all stubs bound thread is destroyed
        //even if a replica is destroyed after this happening no problem because registerHandler will launch an exception for this stub we're trying to access.
        new Thread(new Thread_to_bind_stubs()).start();

        //thread to send heartbeats if leader -- thread is always running sending heartbeats if leader
        new Thread(new Thread_to_send_HeartBeats()).start();
    }

    public void invoke(String id, String requestLabel, byte[] requestData, int request_numbers) throws RemoteException, NotBoundException {

        try {
            if(this.id.equals(id)){ // if same id no need stub just need to change on my server
                byte[] response = this.MyServerRMI.processOwnOperation(requestLabel, requestData, request_numbers);
                System.out.println(response);
            }else{
                byte[] response = stubs[Integer.parseInt(id)].processRequest(requestLabel, requestData, request_numbers);
                System.out.println(response);
            }
        } catch (Exception e) {
            System.out.println("replica " + id + " is dead");
        }
    }


    public byte[] quorumInvoke(String requestLabel, byte[] requestData, int request_number) throws RemoteException, NotBoundException{
        System.out.println("quorumInvoke");
        try {
            byte[][] responses;
            responses = new byte[this.line_cnt][];

            for (int i = 0; i < this.line_cnt; i++) {

                final int final_i = i;
                final String final_i_string = String.valueOf(i);

                if(this.id.equals(final_i_string)){ // if same id no need stub
                    byte[] result = this.MyServerRMI.processOwnOperation(requestLabel, requestData, request_number);
                    responses[final_i] = result;
                    continue;
                }

                Executors.newSingleThreadExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            responses[final_i] = stubs[final_i].processRequest(requestLabel, requestData, request_number);
                        } catch (NullPointerException npe){
                            System.out.println("Apanhou o primeiro");
                            System.out.println(npe);
                        } catch (Exception e) {
                            stubBind(final_i);
                            System.out.println("replica " + final_i_string + " is dead");
                        }
                    }
                });
            }

            System.out.println("waiting for (n-1/2) + 1 responses");

            long current_time = System.currentTimeMillis();
            while(true){
                int count = 0;
                for(int i = 0; i < responses.length ; i++){
                    if (responses[i] != null){
                        count++;
                    }
                }
                if(count > (responses.length / 2)){
                    break;
                }

                if(System.currentTimeMillis() > current_time + 10000){
                    System.out.println("TIMEOUT");
                    // return EOT
                    byte[] errorArray = new byte[1];
                    errorArray[0] = 4;
                    return errorArray;
                }
            }

            System.out.println("waiting done");

            for(int i = 0; i < responses.length ; i++){
                System.out.println(i + " " + responses[i]);
            }

            return responses[Integer.parseInt(this.id)];

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendVoteRequest(){

        try {
            CountDownLatch count = new CountDownLatch(line_cnt - 1);

            for (int i = 0; i < this.line_cnt; i++) {

                final int final_i = i;
                final String final_i_string = String.valueOf(i);
                final int my_term = this.MyServerRMI.getTerm();

                if(this.id.equals(final_i_string)){
                    nVotes[final_i] = this.MyServerRMI.answerVoteRequest(my_term);
                    System.out.println("STUB " + final_i + " : " + nVotes[final_i]);
                    continue;
                }

                Executors.newSingleThreadExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            // System.out.println("Stubs i: " + stubs[final_i]);
                            // Thread.sleep((long) (Math.random() * 1 + 50));
                            nVotes[final_i] = stubs[final_i].answerVoteRequest(my_term);
                            System.out.println("STUB " + final_i + " : " + nVotes[final_i]);
                        } catch (NullPointerException npe){
                            System.out.println(npe);
                        } catch (Exception e) {
                            stubBind(final_i);
                            System.out.println("replica " + final_i_string + " died");
                        }
                        count.countDown();
                    }
                });
            }

            count.await();

            int favorableVotes = 0;
            for (boolean nVote : nVotes) {
                if(nVote){
                    favorableVotes++;
                }
            }
            if(favorableVotes > line_cnt/2){
                isLeader = true;
                marosca = true;
                System.out.println("Replica " + this.id + " is the new leader");
            }else {
                System.out.println("im not the leader didnt receive 3 true votes");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] appendEntry(String requestLabel, byte[] byteArray, int request_number) throws NotBoundException, RemoteException {
        System.out.println("appendEntry");
        return quorumInvoke(requestLabel, byteArray, request_number);
    }

    // Codigo forte
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

    public boolean isLeader(){
        return isLeader;
    }

    public String getId() {
        return id;
    }

    // tries to bind to a server
    public void stubBind(int id){
        try {
            Registry registry = LocateRegistry.getRegistry(getPort(id));
            RegisterHandler stub = (RegisterHandler) registry.lookup(Integer.toString(id));
            stubs[id] = stub;
        }catch (Exception eee){
            // System.out.println("Tried to bind to dead replica id: " + id);
        }
    }


    class Thread_to_bind_stubs implements Runnable {

        @Override
        public void run() {
            boolean gringo = false;
            while(!gringo){
                for (int i = 0; i < line_cnt; i++) {
                    if(stubs[i] != null){
                        continue;
                    }
                    stubBind(i);
                    if(!verify_stubs()){
                        gringo = true;
                    }
                }
            }
            System.out.println("all stubs are bound");
        }

        public boolean verify_stubs(){
            boolean got_null = false;
            for (int i = 0; i < stubs.length; i++){
                if(stubs[i] == null){
                    got_null = true;
                }
            }
            return got_null;
        }
    }


    class Thread_to_send_HeartBeats implements Runnable {
        boolean oxi15 = false;
        @Override
        public void run() {
            while (true) {
                if (!marosca) {
                    isLeader = false;
                }
                if (!isLeader) {
                    oxi15 = false;
                }
                if (isLeader && !oxi15) {
                    oxi15 = true;
                    for (int i = 0; i < line_cnt; i++) {

                        final int final_i = i;
                        final String final_i_string = String.valueOf(i);

                        if(ClientRMI.this.id.equals(final_i_string)){
                            continue;
                        }

                        Executors.newSingleThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                boolean aux = true;
                                RegisterHandler stub = null;
                                while (true) {
                                    try {
                                        if (isLeader) {
                                            if (!aux) {
                                                //recover replica state after restart
                                                Registry registry = LocateRegistry.getRegistry(getPort(final_i));
                                                stub = (RegisterHandler) registry.lookup(final_i_string);
                                                stub.receiveHeartBeat(MyServerRMI.getTerm(), Integer.parseInt(id));
                                                stubs[final_i] = stub;

                                                int tmp = 0;
                                                for (Operation operation : MyServerRMI.getOperations()) {
                                                    invoke(final_i_string, "ADD", operation.getOperations(), operation.getRequest_number());
                                                    tmp = operation.getRequest_number();
                                                }
                                                MyServerRMI.request_number = tmp;

                                                aux = true;
                                            } else {
                                                //send heartbeat
                                                stubs[final_i].receiveHeartBeat(MyServerRMI.getTerm(), Integer.parseInt(id));
                                                Thread.sleep(1000);
                                            }
                                        }else {
                                            break;
                                        }
                                    } catch (Exception e) {
                                        aux = false;
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
    }
}