package ru.itmo.ordermanagement.security;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XmlLoginModule implements LoginModule {
    private Subject subject;
    private CallbackHandler callbackHandler;
    private boolean loginSucceeded = false;
    private String username;
    private List<String> roles;


    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> map, Map<String, ?> map1) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback nameCallback = new NameCallback("username: ");
        PasswordCallback passwordCallback = new PasswordCallback("password: ", false);

        try {
            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
            this.username = nameCallback.getName();
            String password = new String(passwordCallback.getPassword());

            if (authenticateAndLoadRoles(username, password)) {
                this.loginSucceeded = true;
                return true;
            } else {
                this.loginSucceeded = false;
                throw new LoginException("Login failed cause of invalid credentials");
            }
        } catch (Exception e) {
            throw new LoginException("Login failed: " + e.getMessage());
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (!loginSucceeded) {
            return false;
        }
        subject.getPrincipals().add(new UserPrincipal(username));
        for (String role : roles) {
            subject.getPrincipals().add(new RolePrincipal(role));
        }
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        loginSucceeded = false;
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals()
                .removeIf(p ->
                        p instanceof UserPrincipal || p instanceof RolePrincipal
                );
        return false;
    }

    private boolean authenticateAndLoadRoles(String username, String password) {
        try {
            InputStream is = getClass().getResourceAsStream("/users.xml");
            if (is == null) {
                throw new RuntimeException("Файл users.xml не найден");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(is);

            NodeList userNodes = document.getElementsByTagName("user");

            for (int i = 0; i < userNodes.getLength(); i++) {
                Element userElement = (Element) userNodes.item(i);
                String xmlUsername = userElement.getAttribute("username");
                String xmlPassword = userElement.getAttribute("password");

                if (username.equals(xmlUsername) && password.equals(xmlPassword)) {
                    this.roles = new ArrayList<>();
                    NodeList roleNodes = userElement.getElementsByTagName("role");
                    for (int j = 0; j < roleNodes.getLength(); j++) {
                        this.roles.add(roleNodes.item(j).getTextContent());
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
