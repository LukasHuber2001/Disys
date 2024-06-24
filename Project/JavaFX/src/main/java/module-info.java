module com.example.javafx_disys {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires org.apache.pdfbox;



    opens com.example.javafx_disys to javafx.fxml;
    exports com.example.javafx_disys;
}