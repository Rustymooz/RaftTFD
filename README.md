# RaftTFD

# Implementation of Raft

# First of all, we need to start RMI registry. To do this we navigate to jdk\bin folder and run: start rmiregisrty.	
# Second, we need to verify that “replicas.txt” file is in the follow directory (from project root): RaftTFD\out\production\RaftTFD\replicas.txt. This file contains the IP and port of all replicas that will be used in the communication (more lines can be added to the file). After this, we need to position ourselves in the following directory (from the project’s root folder): RaftTFD\out\production\RaftTFD. Once we are here, we can run each replica one by one writing the following command: java Raft.Replica ID. E.g: java Raft.Replica 0 (to start the replica in the first line of the file, which will be given the ID 0).
# We can run multiple clients by typing java Raft.Client. As soon as one leader is elected the client will connect to the leader.
#	These are the possible commands for the client:
# •	ADD operation: sends an operation to be executed and added to the log.
# •	GET operation: retrieves a the given operation from the log.
# •	STATE: returns the replicated log
