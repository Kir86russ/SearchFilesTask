package sample;

import java.io.*;
import java.util.*;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class Controller {

    @FXML
    private Button button_find;

    @FXML
    private TextField start_dir; // начальная директория

    @FXML
    private TextField file_format; // фомат файла (.log по умолчанию)

    @FXML
    private TextField search_text; // искомый текст в файлах

    @FXML
    private Tab tab; // "вкладочка" (название начальной директории)

    @FXML
    private VBox result_list; // разметка для вывода списка файлов результа

    @FXML
    private VBox data_currentFile; // разметка содержимого выбранного файла

    @FXML
    private TabPane tabPane;

    private TreeItem<String> root;  // корневой элемент дерева результата
    private TreeView<String> tree = new TreeView<>(); // дерево результата
    private ArrayList<File> res = new ArrayList<>(); // лист в котором будем хранить найденные файлы

    @FXML
    void initialize() {
        // слушатель для кнопки "find"
        button_find.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {

                    result_list.getChildren().clear();
                    find(start_dir.getText(), search_text.getText()); // запуск рекурсивного поиска файла
                    tab.setText(start_dir.getText());


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        tabPane.setStyle("-fx-border-color: rgb(210,210,210);");
    }

    // метод для распознования заданных параметров, запуска непосредственно рекурсивного поиска
    // и общего формирования визуального дерева результатов
    private void find(String startPath, String mask) throws Exception {
        //проверка параметров
        if (startPath.equals("") || mask.equals("")) {
            throw new Exception("No search parameters are specified!");
        }
        // проверка наличия указанной начальной директории
        File topDirectory = new File(startPath);
        if (!topDirectory.exists()) {
            throw new Exception("Указанный путь не существует!");
        }

        // задаем корень в наше дерево результатов
        root = new TreeItem<>(start_dir.getText());
        root.setExpanded(true);
        tree.setRoot(root);

        //вычинаем сам поиск
        search(topDirectory, res);

        // задаю слушатель, чтобы при выделении элемента (файла) из дерева, нам показало его содержимое
        tree.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEventHandle);


        result_list.getChildren().add(tree);
        result_list.setPrefHeight(210);
        result_list.setPrefWidth(400);
    }

    private EventHandler<MouseEvent> mouseEventHandle = this::handleMouseClicked;

    private void handleMouseClicked(MouseEvent event) {
        Node node = event.getPickResult().getIntersectedNode();

        if (node instanceof Text || (node instanceof TreeCell && ((TreeCell) node).getText() != null)) {
            String nameItem = (String) ((TreeItem) tree.getSelectionModel().getSelectedItem()).getValue();

            try {
                data_currentFile.getChildren().clear(); // очистка поля содержимого файла при клике на элемент дерева
                if (!nameItem.equals(start_dir.getText()))
                    readingFile(nameItem);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    // метод который читает, файл, на который пользователь нажмет, и выводит содержимое этого файла
    private void readingFile(String nameFile) throws IOException {
        File necessaryFile = null;
        // находим выбранный пользователем файл из списка всех найденных файлов
        for (int i = 0; i != res.size(); i++) {
            if (nameFile.equals(res.get(i).getName())) {
                necessaryFile = res.get(i);
                break;
            }
        }


        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(necessaryFile), "Cp1251"));
        int ch;

        // текст, который будет отображать содержимое выбранного файла
        TextArea textArea = new TextArea();
        textArea.setPrefHeight(210);
        textArea.setMinWidth(400);
        textArea.setEditable(false); // отключаем ввод текста в поле вывода содержимого файла

        // читаем и формируем содержимое файла
        while ((ch = br.read()) != -1) {
            textArea.appendText(String.valueOf((char) (ch)));
        }
        data_currentFile.getChildren().add(textArea); // задаем сформированный текст в поле вывода содержимого файла
    }


    private void search(File topDirectory, ArrayList res) throws IOException {

        //получаем список всех объектов в текущей директории
        File[] list = topDirectory.listFiles();

        //просматриваем все объекты по-очереди
        for (int i = 0; i < list.length; i++) {

            //если это директория
            if (list[i].isDirectory()) {

                //выполняем поиск во вложенных директориях
                search(list[i], res);

            }
            //если это файл
            else {
                // если маска не задана, значит ищем файлы по маске ".log"
                if (file_format.getText().equals("")) {
                    // проверяем файл на совпадение маски ".log"
                    if (isSatisfyByMask(list[i], ".log"))
                        // проверяем, есть ли в этом файле искомый текст, заданный пользователем
                        if (isSatisfyFile(list[i])) {
                            // добавляем файл в список результатов
                            TreeItem<String> item = new TreeItem<>(list[i].getName()); // новый эелемент для дерева
                            root.getChildren().add(item); // присоединяю элемент в визуальное дерево
                            res.add(list[i]); // добавляю найденный файл в список результатов
                        }
                } else {
                    // если маска задана пользователем, делаем аналогичные действия, что и выше
                    if (isSatisfyByMask(list[i], file_format.getText()))
                        if (isSatisfyFile(list[i])) {
                            // добавляем файл в список результатов,
                            //и обновляем значения счетчиков
                            TreeItem<String> item = new TreeItem<>(list[i].getName());
                            root.getChildren().add(item);
                            res.add(list[i]);
                        }
                }
            }
        }


    }

    // метод, который проверяет совпадение файла по заданой маске
    private boolean isSatisfyByMask(File currentFile, String mask) {
        return currentFile.getName().endsWith(mask);
    }

    // метод, который проверяет наличие заданного текста в файле
    private boolean isSatisfyFile(File currentFile) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(currentFile), "Cp1251"));
        String str;
        StringBuilder sb = new StringBuilder();

        // читаем файл построчно
        while ((str = br.readLine()) != null) {
            sb.append(str);
        }

        String s = String.valueOf(sb);

        // проверяем, входит ли заданный текст в содержимое файла
        return s.contains(search_text.getText());

    }
}