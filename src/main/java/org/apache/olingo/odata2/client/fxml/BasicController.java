package org.apache.olingo.odata2.client.fxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.client.HttpException;
import org.apache.olingo.odata2.client.ODataClient;
import org.apache.olingo.odata2.client.StringHelper;
import quicktime.util.StringHandle;

public class BasicController implements Initializable {

  @FXML TextField inputArea;
  @FXML TextArea output;
  
  @Override
  public void initialize(URL url, ResourceBundle rb) {
    // for this basic starter we dont have something to initialize
    inputArea.setText("http://www.olingo.org");
  }

  @FXML
  public void sendRequest(ActionEvent event) {

    String name = inputArea.getText();

    if (isEmpty(name)) {
      output.setText("No url given.");
    } else {
      try {
        InputStream is = ODataClient.getRawHttpEntity(name, ODataClient.APPLICATION_JSON);
        String content = StringHelper.inputStreamToString(is);
        output.setText(content);
      } catch (IOException | HttpException ex) {
        Writer out = new StringWriter();
        PrintWriter pw = new PrintWriter(out, true);
        ex.printStackTrace(pw);
        output.setText("Exception occured: " + ex.getMessage() + "\n" + out.toString());
      }
    }
  }

  private boolean isEmpty(String firstName) {
    return firstName == null || firstName.length() == 0;
  }
}
