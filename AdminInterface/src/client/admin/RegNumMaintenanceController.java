package client.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import table.SummaryTable;
import table.VehicleTable;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Scanner;

public class RegNumMaintenanceController implements Initializable {
    enum btnType {
        SUBMIT,
        DELETE
    }

    @FXML
    public TableView<VehicleTable> vehicleTable;
    @FXML
    public TableColumn<VehicleTable, String> regNum;
    @FXML
    public TableColumn<VehicleTable, String> vehicleState;
    @FXML
    public TableColumn<VehicleTable, String> buttonsCol;
    @FXML
    public TextField regNumTextField;
    @FXML
    public ComboBox<String> stateBox;
    @FXML
    public Button addVehicleBtn;
    @FXML
    public TableColumn<VehicleTable, String> delButtonCol;

    private String regNumCellData;
    private String vehicleStateCellData;


    private Socket clientSocket;
    private PrintWriter outMessage;
    private ObjectInputStream objectInputStream;
    private String serverHost;
    private int serverPort;
    private ArrayList<VehicleTable> arrayList;
    private final ObservableList<String> optionsList = FXCollections.observableArrayList("Free", "Busy", "On maintenance");

    public void addVehicle() {
        sendMsg("#ADDVEHICLE");
        sendMsg(regNumTextField.getText());
        String state = stateBox.getValue();
        switch (state) {
            case "Free" -> sendMsg("1");
            case "Busy" -> sendMsg("0");
            case "On maintenance" -> sendMsg("2");
        }
        sendMsg(state);
    }

    public Callback<TableColumn<VehicleTable, String>, TableCell<VehicleTable, String>> formCellFactoryBtn(String btnName, btnType type) {
        return new Callback<>() {
            @Override
            public TableCell<VehicleTable, String> call(final TableColumn<VehicleTable, String> param) {
                final Button btn = new Button(btnName);
                TableCell<VehicleTable, String> t = new TableCell<>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                        setText(null);
                    }
                };
                switch (type) {
                    case SUBMIT -> btn.setOnAction(e -> {
                        int cellIndex = t.getTableRow().getIndex();
                        regNumCellData = regNum.getCellData(cellIndex);
                        vehicleStateCellData = vehicleState.getCellData(cellIndex);
                        sendMsg("#VEHICLESTATECHANGED");
                        sendMsg(regNumCellData);
                        switch (vehicleStateCellData) {
                            case "Free" -> sendMsg("1");
                            case "Busy" -> sendMsg("0");
                            case "On maintenance" -> sendMsg("2");
                        }
                        updateTable();
                    });
                    case DELETE -> btn.setOnAction(e -> {
                        int cellIndex = t.getTableRow().getIndex();
                        regNumCellData = regNum.getCellData(cellIndex);
                        sendMsg("#DEELETEVEHICLE");
                        sendMsg(regNumCellData);
                        updateTable();
                    });
                }
                return t;
            }
        };
    }

    public void updateTable() {
        regNum.setCellValueFactory(new PropertyValueFactory<>("regNum"));
        vehicleState.setCellValueFactory(new PropertyValueFactory<>("vehicleState"));

        vehicleState.setCellFactory(ComboBoxTableCell.forTableColumn(optionsList));

        vehicleState.setOnEditCommit(event -> {
            VehicleTable table = event.getRowValue();
            table.setVehicleState(event.getNewValue());
        });

        stateBox.getItems().setAll(optionsList);
        buttonsCol.setCellFactory(formCellFactoryBtn("Submit", btnType.SUBMIT));
        delButtonCol.setCellFactory(formCellFactoryBtn("Delete", btnType.DELETE));
        vehicleTable.setEditable(true);
        vehicleTable.setItems(FXCollections.observableArrayList(arrayList));
    }

    public void sendMsg(String msg) {
        outMessage.println(msg);
        outMessage.flush();
    }

    public void initClient() throws IOException {
        String[] serverParams = SummaryController.getSettings();
        serverHost = serverParams[0];
        serverPort = Integer.parseInt(serverParams[1]);
        clientSocket = new Socket(serverHost, serverPort);
        outMessage = new PrintWriter(clientSocket.getOutputStream());
        objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
    }

    public void shutdown() throws IOException, InterruptedException {
        Thread.sleep(100);
        outMessage.println("##session##end##");
        outMessage.flush();
        outMessage.close();
        objectInputStream.close();
        clientSocket.close();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            initClient();
            sendMsg("#REGNUMMAINTENANCE");
            arrayList = (ArrayList<VehicleTable>) objectInputStream.readObject();
            System.out.println(arrayList);
        } catch (IOException | ClassNotFoundException | NullPointerException e) {
            e.printStackTrace();
        }
        updateTable();
    }
}