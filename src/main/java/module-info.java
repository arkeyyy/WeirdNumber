module org.example.weird_number {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.weird_number to javafx.fxml;
    exports org.example.weird_number;
}