package Main;

import MainPage.View.MainPage;
import MainPage.Controller.MainPageController;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            MainPage view = new MainPage();

            MainPageController controller =
                    new MainPageController(view);

            controller.init();

            view.setVisible(true);
        });
    }
}