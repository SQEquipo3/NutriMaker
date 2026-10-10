package com.javafx.nutrimaker;

import static com.javafx.nutrimaker.animations.AnimationPersonalized.setFadeAndScaleAnimation;
import static com.javafx.nutrimaker.animations.AnimationPersonalized.setFadeAnimation;
import com.javafx.nutrimaker.repository.UserRepository;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * FXML Controller class
 *
 * @author uh709
 */
public class SignUpFormController implements Initializable {
    static final String REG_EXP_VER_EMAIL= "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,63}$";
    static final String REG_EXP_VER_PASS= ".{8,16}";
    
    @FXML
    private TextField emailTextField;
    @FXML
    private PasswordField passwordTextField;
    @FXML
    private PasswordField confPasswordTextField;
    @FXML
    private Button signupButton;
    @FXML
    private Button loginButton;

    private boolean isEmpty(){
        return emailTextField.getText().isEmpty() && 
                passwordTextField.getText().isEmpty() && 
                confPasswordTextField.getText().isEmpty();
    }
    
    private boolean checkPassword(String pass){
        return pass.matches(REG_EXP_VER_PASS);
    }
    
    private boolean checkPassword() {
        return comparePasswords() && checkPassword(passwordTextField.getText());                  
    }
    
    private boolean checkEmail() {
        return emailTextField.getText().matches(REG_EXP_VER_EMAIL);
    }
    
    private boolean comparePasswords(){
        return passwordTextField.getText().equals(confPasswordTextField.getText());
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setFadeAndScaleAnimation(loginButton);
        setFadeAndScaleAnimation(signupButton);
        setFadeAnimation(emailTextField);
        setFadeAnimation(passwordTextField);
        setFadeAnimation(confPasswordTextField);
    }    
      
    @FXML
    private void signUp(ActionEvent event) throws IOException {
        if(isEmpty()){
            dialog("No se admite texto vacio");
            return;
        }
        if((!checkPassword() || !checkEmail())){
            invalidCredentials();
            return;
        }
        
        if(!(new UserRepository().insertUser(emailTextField.getText(),passwordTextField.getText()))){
            emailExists();
            return;
        }
        registerSuccessful();  
    }

    @FXML
    public void login(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginForm.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Login");
        stage.show();
    }
    
    public void dialog(String warningMessage) throws IOException {
        FXMLLoader loader =new FXMLLoader(getClass().getResource("DialogInputs.fxml"));
        Parent parent = loader.load();
        DialogController dialog = loader.getController();
        dialog.setText(warningMessage);
        Scene scene = new Scene(parent);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }
    
    public void invalidCredentials() throws IOException{
        Parent parent = FXMLLoader.load(getClass().getResource("CheckCredentials.fxml"));
        Scene scene = new Scene(parent);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }
    private void emailExists() throws IOException{
        Parent parent = FXMLLoader.load(getClass().getResource("EmailExists.fxml"));
        Scene scene = new Scene(parent);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }
    
    private void registerSuccessful() throws IOException{
        Parent parent = FXMLLoader.load(getClass().getResource("RegisterSuccessful.fxml"));
        Scene scene = new Scene(parent);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }
}
