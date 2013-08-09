package org.apache.olingo.odata2.client.fxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import org.apache.olingo.odata2.client.HttpException;
import org.apache.olingo.odata2.client.ODataClient;
import org.apache.olingo.odata2.client.StringHelper;

public class BasicController implements Initializable {

  @FXML TextField inputArea;
  @FXML TextArea rawView;
  @FXML WebView webView;
  @FXML TableView outTable;
  
  @Override
  public void initialize(URL url, ResourceBundle rb) {
    // for this basic starter we dont have something to initialize
    inputArea.setText("http://www.olingo.org");
  }

  @FXML
  public void sendRequest(ActionEvent event) {

    String name = inputArea.getText();

    if (isEmpty(name)) {
      rawView.setText("No url given.");
    } else {
      try {
        InputStream is = ODataClient.getRawHttpEntity(name, ODataClient.APPLICATION_JSON);
        String content = StringHelper.inputStreamToString(is);
        rawView.setText(content);
        webView.getEngine().loadContent(content);
      } catch (IOException | HttpException ex) {
        Writer out = new StringWriter();
        PrintWriter pw = new PrintWriter(out, true);
        ex.printStackTrace(pw);
        rawView.setText("Exception occured: " + ex.getMessage() + "\n" + out.toString());
      }
    }
  }

  private boolean isEmpty(String firstName) {
    return firstName == null || firstName.length() == 0;
  }
}
