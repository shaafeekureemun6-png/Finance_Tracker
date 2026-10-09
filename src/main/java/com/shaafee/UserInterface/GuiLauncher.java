package com.shaafee.UserInterface;

import javafx.application.Application;

/**
 * Start the GUI from here (right-click > Run 'GuiLauncher.main()').
 *
 * This class deliberately does NOT extend Application. If the main class
 * extends Application, running from a classpath (IntelliJ / a fat jar) fails
 * with "JavaFX runtime components are missing".
 */
public class GuiLauncher {

    public static void main(String[] args) {
        Application.launch(FinanceApp.class, args);
    }
}
