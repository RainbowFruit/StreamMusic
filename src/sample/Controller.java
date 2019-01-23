package sample;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Controller {

    @FXML
    private ListView listView;

    @FXML
    private Button gotoBtn;

    @FXML
    private Button removeButton;

    @FXML
    private Button btnConnect;

    @FXML
    private TextField textField;

    @FXML
    private TextField textFieldip;

    @FXML
    private TextField textFieldTitle;

    @FXML
    public void initialize(){
        listView.setOnMouseClicked(event -> textField.setText(Integer.toString(listView.getSelectionModel().getSelectedIndex() + 1)));
    }

    @FXML
    private void btnNextAction(ActionEvent event){
        send((byte)10);
    }

    @FXML
    void btnPrevAction(ActionEvent event){
        send((byte)20);
    }

    @FXML
    void btnConnectClick(ActionEvent event){
        btnConnect.setDisable(true);
        ReceiverThread receiver = new ReceiverThread(textFieldip.getText());
        receiver.start();
    }

    @FXML
    void upBtn(ActionEvent event){
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        listView.getItems().add(selectedIndex - 1, listView.getSelectionModel().getSelectedItem().toString());
        listView.getItems().remove(selectedIndex + 1);
    }

    @FXML
    void downBtn(ActionEvent event){
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        listView.getItems().add(selectedIndex + 2, listView.getSelectionModel().getSelectedItem().toString());
        listView.getItems().remove(selectedIndex);
    }

    @FXML
    void getQueueVector(ActionEvent event) throws Exception {
        byte[] buffer = new byte[listView.getItems().size() + 2];
        buffer[0] = 50;
        buffer[buffer.length - 1] = 0;
        for(int i = 0; i < listView.getItems().size(); i++){
            buffer[i + 1] = (byte)Integer.parseInt(listView.getItems().get(i).toString().split(" ")[0]);
        }
        StaticFields.socket.getOutputStream().write(buffer);
    }

    @FXML
    void gotoMusic(ActionEvent event) throws Exception {
        byte[] buffer = new byte[2];
        buffer[0] = 30;
        buffer[1] = (byte)Integer.parseInt(textField.getText());
        StaticFields.socket.getOutputStream().write(buffer);
    }

    @FXML
    void removeMusic(ActionEvent event) throws Exception {
        byte[] buffer = new byte[2];
        buffer[0] = 40;
        buffer[1] = (byte)Integer.parseInt(textField.getText());
        StaticFields.socket.getOutputStream().write(buffer);
    }

    void send(byte command){
        try {
            StaticFields.socket.getOutputStream().write(command);
            System.out.println("After sending");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void updatelistView(List<String> musicNames){
        Platform.runLater(() -> {
            listView.getSelectionModel().clearSelection();
            listView.getItems().clear();
            for(int i = 0; i < musicNames.size(); i++){
                listView.getItems().add(i, (i+1) + " " + musicNames.get(i));
            }
        });
    }

    void updatetextView(String message){
        Platform.runLater(()-> textFieldTitle.setText(message));
    }

    @FXML
    void sendMusic(ActionEvent event){
        StaticFields.musicTitleToSend = textFieldTitle.getText();
        File file = new File(StaticFields.musicTitleToSend);
        //Thread SendingMusicThread = new ClientSendingMusicThread(StaticFields.socket, StaticFields.musicTitleToSend);
        //SendingMusicThread.start();
        if(file.exists())
            send((byte)100);
        else
            updatetextView("Failed to open file");
    }
}
