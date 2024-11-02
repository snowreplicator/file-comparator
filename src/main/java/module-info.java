module com.snowreplicator.filecomparator {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.snowreplicator.filecomparator to javafx.fxml;
    exports com.snowreplicator.filecomparator;
    exports com.snowreplicator.filecomparator.view_model;
}