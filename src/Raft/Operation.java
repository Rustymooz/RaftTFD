package Raft;

import java.util.Arrays;

public class Operation {

    public final byte[] operations;
    public final int request_number;

    public Operation(byte[] operations, int request_number) {
        this.operations = operations;
        this.request_number = request_number;
    }

    public byte[] getOperations() {
        return operations;
    }

    public int getRequest_number() {
        return request_number;
    }

    public boolean compareRequest(byte[] request){
        return Arrays.equals(request, operations);
    }
}