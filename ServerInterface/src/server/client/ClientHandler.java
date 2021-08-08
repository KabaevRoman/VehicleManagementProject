package server.client;

import javafx.application.Platform;
import server.DBConnect;
import table.SummaryTable;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    private Server server;
    private Connection connection;
    private ObjectOutputStream objectOutputStream;
    private Scanner inMessage;
    private Socket clientSocket;
    private static int clients_count = 0;
    private boolean running = true;
    private boolean saveToggled;
    private Integer key;

    public ClientHandler(Socket socket, Server server, Integer key, DBConnect dbConnect, boolean saveToggled) {
        try {
            this.saveToggled = saveToggled;
            clients_count++;
            Platform.runLater(() -> server.controller.numOfClientsLabel.setText(String.valueOf(clients_count)));
            this.key = key;
            this.server = server;
            this.clientSocket = socket;
            this.objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            this.inMessage = new Scanner(socket.getInputStream());
            this.connection = dbConnect.getConnection();
        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                if (inMessage.hasNext()) {
                    String clientMessage = inMessage.nextLine();
                    System.out.println(clientMessage);
                    switch (clientMessage) {
                        case "#INSERT" -> {
                            String name = inMessage.nextLine();
                            String note = inMessage.nextLine();
                            String time = inMessage.nextLine();
                            insertRecording(name, note, time);
                            server.sendTableToAllClients();
                            server.sendMsgToPDOServer("#INSERT");
                        }
                        case "#FREEAUTO" -> {
                            changeAutoState();
                            String date = inMessage.nextLine();
                            saveReturnTime(date);
                        }
                        case "#INITTABLE" -> sendTable(false);
                        case "##session##end##" -> this.close();
                    }
                }
            } catch (NullPointerException | IOException | SQLException ex) {
                ex.printStackTrace();
                System.out.println("Error in switch");
            }
        }
    }

    private void saveReturnTime(String date) {
        System.out.println("UPDATE summary SET return_time =" + date + " WHERE id=" + key);
        try {
            connection.createStatement().executeUpdate(
                    "UPDATE summary SET return_time =" + "'" + date + "'" + " WHERE id=" + key);
            if (saveToggled) {
                connection.createStatement().executeUpdate(
                        "INSERT INTO archieve(old_id,fio,departure_time,car_status,return_time,pdo, note,gos_num)" +
                                "SELECT id,fio,departure_time,car_status,return_time,pdo, note,gos_num from summary WHERE id=" + key);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void close() throws IOException {
        running = false;
        clientSocket.close();
        server.removeClient(key);
        clients_count--;
        Platform.runLater(() -> server.controller.numOfClientsLabel.setText(String.valueOf(clients_count)));
        objectOutputStream.close();
    }

    public void insertRecording(String name, String note, String time) throws SQLException {
        connection.createStatement().executeUpdate("INSERT INTO summary(id,fio,departure_time,pdo,note) VALUES(" +
                "'" + key + "'," +
                "'" + name + "'" + "," + "'" + time + "'" + "," + "'" + "On approval" + "','" + note + "')");
    }

    public void changeAutoState() throws SQLException {
        ResultSet rs = connection.createStatement().executeQuery(
                "select gos_num from summary where id = " + "'" + key + "'");
        String gos_num = null;
        while (rs.next()) {
            gos_num = rs.getString("gos_num");
        }
        System.out.println(gos_num);
        connection.createStatement().executeUpdate("UPDATE car_list SET car_state = 1 WHERE reg_num='" + gos_num + "'");
    }

    public void sendTable(boolean lock) throws IOException {
        objectOutputStream.writeBoolean(lock);
        objectOutputStream.flush();

        int numOfAvailableCars = 0;
        try {
            ResultSet rs = connection.createStatement().executeQuery("select count(*) from car_list where car_state = 1");
            while (rs.next()) {
                numOfAvailableCars = rs.getInt("count");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println(numOfAvailableCars);
        objectOutputStream.writeInt(numOfAvailableCars);
        objectOutputStream.flush();

        ArrayList<SummaryTable> arrayList = new ArrayList<>();
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT*FROM summary");
            while (rs.next()) {
                arrayList.add(new SummaryTable(
                        rs.getString("id"),
                        rs.getString("fio"),
                        rs.getString("departure_time"),
                        rs.getString("pdo"),
                        rs.getString("note"),
                        rs.getString("gos_num"),
                        rs.getString("return_time")));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        objectOutputStream.writeObject(arrayList);
        objectOutputStream.flush();
    }
}