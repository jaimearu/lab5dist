package com.example.demo.service;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Stream;

public class RestNameService {
    //
    HashMap<Integer, Integer> replicationDatabase = new HashMap<>();
    // Key is filenameHash, value is nodeName hash
    public HashMap<Integer, Integer> dataBase = new HashMap<>();
    public HashMap<Integer, String> nodes = new HashMap<>();
    Integer highest = 0;
    Integer lowest = 10000000;
    InetAddress inetAddress = InetAddress.getLocalHost();
    String name = inetAddress.getHostName();
    String thisIp =inetAddress.getHostAddress();

    public RestNameService() throws IOException {
       System.out.println("Naming Server Has been booted successfully");
        clearDataBase();
    }

    public int hashfunction(String name, boolean node) {
        int hash=0;
        int temp = 0;
        int i;
        for (i = 0; i<name.length();i++) {
            hash = 3 * hash + name.charAt(i);
            temp = temp+ name.charAt(i);
        }
        hash = hash+temp;
        if (node) {
        }
        else
            hash = hash/53;
        return hash;
    }
    public void addNodeToMap(String name, String ip) throws IOException {
        //System.out.println("Ik run nu addNodeToMap, Variebelen name "+name+" ip "+ip);
        /*
        BufferedWriter writer = new BufferedWriter(
                new FileWriter("//home//pi//DSLab5//src//main//java//com//example//demo/backLogic//NodeMap.txt", true)  //Set true for append mode
                //new FileWriter("C:\\Users\\Arla\\Desktop\\School\\lab5distStef\\src\\main\\java\\com\\example\\NodeMap.txt", true)  //Set true for append mode
        );
        writer.newLine();   //Add new line
        writer.write(name);
        writer.newLine();
        writer.write(ip);
        writer.close();
        readNodeMap();
        generateReplicationBase();

         */

       System.out.println(name+" "+ip+" "+"added to nodemap");
        nodes.put(hashfunction(name,true),ip);
        if (hashfunction(name,true) > highest) {
            highest = hashfunction(name, true);
        }
        if (hashfunction(name,true) < lowest) {
            lowest = hashfunction(name, true);
        }
    }
    public int requestFile(String filename){
        Integer hash = hashfunction(filename, false);
        if(replicationDatabase.get(hash)!=null)
            return replicationDatabase.get(hash);
        else
            return -1;
    }
    public void removeNodeFromMap(Integer node) throws IOException, InterruptedException {
        nodes.clear();
        File file = new File("//home//pi//DSLab5//src//main//java//com//example//demo/backLogic//NodeMap.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        nodes.clear();
        ArrayList<String> nameToAdd = new ArrayList<>();
        ArrayList<String> ipToAdd = new ArrayList<>();
        while ((st = br.readLine()) != null){
            String ip = br.readLine();
            int hash = hashfunction(st,true);
            if (hash!= node) {
                nodes.put(hash, ip);
                nameToAdd.add(st);
                ipToAdd.add(ip);
            }else
                System.out.println();
        }
        int i = 0;
        BufferedWriter writer = new BufferedWriter(
                new FileWriter("//home//pi//DSLab5//src//main//java//com//example//demo/backLogic//NodeMap.txt", false)  //Set true for append mode
        );
        while (i<nameToAdd.size()){
            if (i>=1)
                writer.newLine();
            writer.write(nameToAdd.get(i));
            writer.newLine();
            writer.write(ipToAdd.get(i));
            i++;
        }
        writer.close();
        highest = 0;
        readNodeMap();
        generateReplicationBase();
    }
    //Dees ga read replicationBase moete worre
    public void generateReplicationBase() throws IOException, InterruptedException {
        File file2 = new File("/home/pi/lab5dist/src/main/java/com/example/DataBase.txt");
        BufferedReader br2 = new BufferedReader(new FileReader(file2));
        String st2;
        while ((st2 = br2.readLine()) != null) {
            String[] temporary = st2.split("::");
            String fileName = temporary[0];
            String nodeName = temporary[1];
            Integer tempfile = hashfunction(fileName, false);
            Integer temp = tempfile - 1;
            if (nodes.size() > 1) {

                while ((nodes.get(temp) == null || temp == hashfunction(nodeName, true)) && temp != 0) {
                    temp--;
                }
                if (temp == 0) {
                    if (replicationDatabase.get(tempfile) == null) {
                        if (highest != hashfunction(nodeName, true)) {
                            System.out.println("1");
                            replicationDatabase.put(tempfile, highest);
                            URL connection = new URL("http://" + nodes.get(dataBase.get(tempfile)) + ":9000/HostLocalFile?FileName=" + fileName);
                            connection.openConnection().getInputStream();
                            URL connection2 = new URL("http://" + nodes.get(highest) + ":9000/GetReplicationFile?name=" + fileName+"&ownerIP="+nodes.get(dataBase.get(tempfile)));
                            connection2.openConnection().getInputStream();
                            System.out.println(fileName+" should be replicated from "+nodes.get(dataBase.get(tempfile))+" to "+nodes.get(highest));
                            //HIER DUS NAAR HIGHEST REPLICATEN
                        }
                        else{
                            int i = highest-1;
                            while (nodes.get((i))==null){
                                i--;
                            }
                            replicationDatabase.put(tempfile,i);
                            URL connection = new URL("http://" + nodes.get(dataBase.get(tempfile)) + ":9000/HostLocalFile?FileName=" + fileName);
                            connection.openConnection().getInputStream();
                            URL connection2 = new URL("http://" + nodes.get(i) + ":9000/GetReplicationFile?name=" + fileName+"&ownerIP="+nodes.get(dataBase.get(tempfile)));
                            connection2.openConnection().getInputStream();
                            System.out.println(fileName+" should be replicated from "+nodes.get(dataBase.get(tempfile))+" to "+nodes.get(i));
                            //Hier naar i knallen
                        }
                    } else if (replicationDatabase.get(tempfile)<highest){
                        URL connection = new URL("http://" + nodes.get(replicationDatabase.get(tempfile)) + ":9000/TransferReplicatedFile?name=" + fileName);
                        connection.openConnection().getInputStream();
                        URL connection2 = new URL("http://" + nodes.get(highest) + ":9000/GetReplicationFile?name=" + fileName+"&ownerIP="+nodes.get(replicationDatabase.get(tempfile)));
                        connection2.openConnection().getInputStream();
                        System.out.println(fileName+" should be replicated from "+nodes.get(replicationDatabase.get(tempfile))+" to "+nodes.get(highest));
                        replicationDatabase.replace(tempfile,hashfunction(nodeName,true));
                    }
                } else {

                    if (replicationDatabase.get(tempfile) == null) {
                        if (!temp.equals(hashfunction(nodeName, true))){
                            replicationDatabase.put(tempfile, temp);
                            URL connection = new URL("http://" + nodes.get(dataBase.get(tempfile)) + ":9000/HostLocalFile?FileName=" + fileName);
                            connection.openConnection().getInputStream();
                            URL connection2 = new URL("http://" + nodes.get(temp) + ":9000/GetReplicationFile?name=" + fileName+"&ownerIP="+nodes.get(dataBase.get(tempfile)));
                            connection2.openConnection().getInputStream();
                            System.out.println(fileName+" should be replicated from "+nodes.get(dataBase.get(tempfile))+" to "+nodes.get(temp));
                        //Knallen naar temp
                    }/*else{
                            int i = temp-1;
                            while (nodes.get((i))==null){
                                i--;
                            }
                            if(i !=0)
                            replicationDatabase.put(tempfile,i);
                            //Knallen naar i
                            else
                                replicationDatabase.put(tempfile,highest);
                            //Knallen naar highest

                        }*/
                    } else if (temp > replicationDatabase.get(tempfile)) {
                        URL connection = new URL("http://" + nodes.get(replicationDatabase.get(tempfile)) + ":9000/TransferReplicatedFile?name=" + fileName);
                        connection.openConnection().getInputStream();
                        URL connection2 = new URL("http://" + nodes.get(temp)+ ":9000/GetReplicationFile?name=" + fileName+"&ownerIP="+nodes.get(replicationDatabase.get(tempfile)));
                        connection2.openConnection().getInputStream();
                        System.out.println(fileName+" should be replicated from "+nodes.get(replicationDatabase.get(tempfile))+" to "+nodes.get(temp));
                        replicationDatabase.replace(tempfile,hashfunction(nodeName,true));
                        replicationDatabase.replace(tempfile, replicationDatabase.get(tempfile), temp);
                    } else
                        System.out.println("");
                }

            }
            else
                System.out.println("No replication, Only one node is present");
            System.out.println(replicationDatabase.toString());
        }
    }
    public int addFileToDataBase(String name, String fileName) throws IOException, InterruptedException {
        System.out.println("Adding file to database, name "+name+" filename "+fileName);
        int nameHash = hashfunction(name,true);
        int fileHash = hashfunction(fileName,false);
        if(nodes.get(nameHash)!=null) {
            if (dataBase.get(fileHash) == null) {
                dataBase.put(fileHash, nameHash);
                BufferedWriter writer = new BufferedWriter(
                        new FileWriter("/home/pi/lab5dist/src/main/java/com/example/DataBase.txt", true)  //Set true for append mode
                        //new FileWriter("C:\\Users\\Arla\\Desktop\\School\\lab5distStef\\src\\main\\java\\com\\example\\NodeMap.txt", true)  //Set true for append mode
                );
                writer.write(fileName + "::" + name);
                writer.newLine();   //Add new line
                writer.close();
                generateReplicationBase();
                return 1;
            }
            return -1;
        }
        else
            return -1;
    }
    private void clearDataBase() throws IOException {
        File database = new File("/home/pi/lab5dist/src/main/java/com/example/DataBase.txt");
        if (database.exists() && database.isFile())
        {
            database.delete();
        }
        database.createNewFile();
    }
    public void readNodeMap() throws IOException {
        File file = new File("//home//pi//DSLab5//src//main//java//com//example//demo/backLogic///NodeMap.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        nodes.clear();
        while ((st = br.readLine()) != null){
            if(!st.isEmpty()) {
                String ip = br.readLine();
                int hash = hashfunction(st, true);
               // System.out.println("node " + st + " heeft hashwaarde " + hash);
                nodes.put(hash, ip);
                if (hash > highest)
                    highest = hash;
            }
        }
    }

    public int removeFileFromDatabase(String name, String file) throws IOException, InterruptedException {
        int nameHash = hashfunction(name,true);
        int fileHash = hashfunction(file,false);
        if (dataBase.get(fileHash) == nameHash){
            //Hier ook delete replicated file
            //
            //
            URL connection = new URL("http://" + nodes.get(replicationDatabase.get(fileHash)) + ":9000/RemoveReplicatedFile?File=" + file);
            connection.openConnection().getInputStream();
            System.out.println("file "+file+" van node "+name+" met filehash "+fileHash+" werd verwijderd");
            dataBase.remove(fileHash);
            String fileName = "/home/pi/lab5dist/src/main/java/com/example/DataBase.txt";
            String lineToRemove = file+"::"+name;
            ArrayList<String> temp = new ArrayList<>();
            try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
                stream.filter(line -> !line.trim().equals(lineToRemove)).forEach(temp::add);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(temp.toString());
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter("/home/pi/lab5dist/src/main/java/com/example/DataBase.txt", false)  //Set true for append mode
                    //new FileWriter("C:\\Users\\Arla\\Desktop\\School\\lab5distStef\\src\\main\\java\\com\\example\\NodeMap.txt", true)  //Set true for append mode
            );
            for (String s : temp) {
                writer.write(s);
                writer.newLine();   //Add new line
            }
            writer.close();
            generateReplicationBase();
            return 1;
        }
        return -1;
    }
}
