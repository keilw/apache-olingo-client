package org.apache.olingo.odata2.client.fxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntitySetInfo;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.client.HttpException;
import org.apache.olingo.odata2.client.ODataClient;
import org.apache.olingo.odata2.client.StringHelper;

/**
 *
 * @author mibo
 */
public class BasicController implements Initializable {

  @FXML TextField inputArea;
  @FXML TextArea rawView;
  @FXML WebView webView;
  @FXML SplitPane edmPane;
  @FXML TextArea logArea;
  @FXML ListView entityListView;
  //
  private TableView tableView;

  @Override
  public void initialize(URL url, ResourceBundle rb) {
//    inputArea.setText("http://www.olingo.org");
//    inputArea.setText("http://localhost:8080/com.sap.core.odata.performance-web/ReferenceScenario.svc/");
    inputArea.setText("http://services.odata.org/Northwind/Northwind.svc/");

  }

  @FXML
  public void sendRequest(ActionEvent event) {

    String serviceUrl = inputArea.getText();

    if (isEmpty(serviceUrl)) {
      rawView.setText("No url given.");
    } else {
      StringBuilder b = new StringBuilder();
      b.append("--- ").append(DateFormat.getDateTimeInstance().format(new Date())).append("--------\n");
      try {
        InputStream is = ODataClient.getRawHttpEntity(serviceUrl, ODataClient.APPLICATION_JSON);
        String content = StringHelper.inputStreamToString(is);
        rawView.setText(content);
        webView.getEngine().loadContent(content);

        ODataClient client = new ODataClient(serviceUrl);
        createEdmView(client);
        writeToLogArea("All requests successfull processed");
      } catch (IOException | ODataException | HttpException ex) {
        writeToLogArea(ex);
      }
    }
  }

  private void writeToLogArea(Exception ex) {
    StringBuilder b = new StringBuilder();
    Writer out = new StringWriter();
    PrintWriter pw = new PrintWriter(out, true);
    ex.printStackTrace(pw);
    b.append("Exception occured: ").append(ex.getMessage()).append("\n").append(out.toString());
    writeToLogArea(b.toString());
  }

  private void writeToLogArea(String content) {
    StringBuilder b = new StringBuilder();
    b.append("--- ").append(DateFormat.getDateTimeInstance().format(new Date())).append("--------\n");
    b.append(content);
    b.append("\n--------------------------");
    logArea.setText(b.toString());
  }

  private void createEdmView(ODataClient client) throws ODataException, EdmException, IOException, HttpException {
    List<EdmEntitySetInfo> entitySets = client.getEntitySets();
    for (EdmEntitySetInfo edmEntitySetInfo : entitySets) {
      String containerName = edmEntitySetInfo.getEntityContainerName();
      String setName = edmEntitySetInfo.getEntitySetName();

      ODataFeed feed = client.readFeed(containerName, setName, ODataClient.APPLICATION_ATOM_XML);
      Edm edm = client.getEdm();
      EdmEntityType entityType = edm.getEntityContainer(containerName).getEntitySet(setName).getEntityType();

      ODataFeedItemHolder holder = new ODataFeedItemHolder(feed, entityType, setName);
      entityListView.getItems().add(holder);
    }
  }

  private class ODataFeedItemHolder {

    private final ODataFeed feed;
    private final EdmEntityType type;
    private final String name;

    public ODataFeedItemHolder(ODataFeed feed, EdmEntityType type, String name) {
      this.feed = feed;
      this.type = type;
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  @FXML
  public void updateTableView(MouseEvent event) {
    try {
      if (tableView != null) {
        edmPane.getItems().remove(tableView);
      }
      ODataFeedItemHolder feed = (ODataFeedItemHolder) entityListView.getSelectionModel().getSelectedItem();
      //      EdmEntityType entityType = edm.getEntityContainer(containerName).getEntitySet(setName).getEntityType();
      tableView = createTable(feed.feed, feed.type);
      edmPane.getItems().add(tableView);
    } catch (EdmException ex) {
      writeToLogArea(ex);
    }

  }

  private boolean isEmpty(String forValidation) {
    return forValidation == null || forValidation.length() == 0;
  }

  private TableView createTable(ODataFeed feed, EdmEntityType edmEntityType) throws EdmException {
    TableView table = new TableView();
    List<String> propertyNames = edmEntityType.getPropertyNames();

    for (String propertyName : propertyNames) {
      TableColumn tc = new TableColumn();
      tc.setText(propertyName);
      tc.setCellValueFactory(new ODataEntryFactory(propertyName));
      table.getColumns().add(tc);
    }

    ObservableList<ODataEntry> values = FXCollections.observableList(feed.getEntries());
    table.setItems(values);

    return table;
  }

  /**
   *
   */
  private class ODataEntryFactory implements Callback<TableColumn.CellDataFeatures<ODataEntry, String>, ObservableValue<String>> {

    private final String property;

    public ODataEntryFactory(String property) {
      this.property = property;
    }

    @Override
    public ObservableValue<String> call(TableColumn.CellDataFeatures<ODataEntry, String> p) {
      ODataEntry ode = p.getValue();
      Object object = ode.getProperties().get(this.property);
      if (object instanceof Calendar) {
        object = DateFormat.getDateTimeInstance().format(((Calendar) object).getTime());
      }
      return new SimpleStringProperty(String.valueOf(object));
    }
  };
}