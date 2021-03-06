package org.hit.internetprogramming.haim.matrix.client;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hit.internetprogramming.haim.matrix.client.action.MenuAction;
import org.hit.internetprogramming.haim.matrix.client.web.GraphWebService;
import org.hit.internetprogramming.haim.matrix.common.action.ActionType;
import org.hit.internetprogramming.haim.matrix.common.comms.HttpStatus;
import org.hit.internetprogramming.haim.matrix.common.comms.Request;
import org.hit.internetprogramming.haim.matrix.common.comms.Response;
import org.hit.internetprogramming.haim.matrix.common.graph.IGraph;
import org.hit.internetprogramming.haim.matrix.common.graph.MatrixGraphAdapter;
import org.hit.internetprogramming.haim.matrix.common.log.LoggingStream;
import org.hit.internetprogramming.haim.matrix.common.mat.CrossMatrix;
import org.hit.internetprogramming.haim.matrix.common.mat.IMatrix;
import org.hit.internetprogramming.haim.matrix.common.mat.Index;
import org.hit.internetprogramming.haim.matrix.common.mat.StandardMatrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

/**
 * @author Haim Adrian
 * @since 23-Apr-21
 */
public class MatrixClientMain {
    private static final String STDOUT_LOGGER_NAME = "stdout";
    private static final String STDERR_LOGGER_NAME = "stderr";

    private Logger log;

    public static void main(String[] args) {
        redirectStreamsToLog4j();

        new MatrixClientMain().run();
    }

    private static void redirectStreamsToLog4j() {
        System.setOut(new PrintStream(new LoggingStream(STDOUT_LOGGER_NAME), true));
        System.setErr(new PrintStream(new LoggingStream(STDERR_LOGGER_NAME), true));

        System.out.println(getJavaVersionString());
    }

    private static String getJavaVersionString() {
        return "java version \"" + System.getProperty("java.version") + "\"" + System.lineSeparator() + System.getProperty("java.runtime.name") +
            " (build " + System.getProperty("java.runtime.version") + ")" + System.lineSeparator() + System.getProperty("java.vm.name") +
            " (build " + System.getProperty("java.vm.version") + ", " + System.getProperty("java.vm.info") + ")";
    }

    private void run() {
        log = LogManager.getLogger(MatrixClientMain.class);

        log.info("Graph Client");
        try (Scanner scanner = new Scanner(System.in)) {
            boolean isRunning = true;
            do {
                MenuAction choice = showMenu(scanner);
                switch (choice) {
                    case CREATE_GRAPH: {
                        createNewGraph(scanner);
                        break;
                    }
                    case LOAD_GRAPH: {
                        loadGraphFromFile(scanner);
                        break;
                    }
                    case SAVE_GRAPH: {
                        saveGraphToFile(scanner);
                        break;
                    }
                    case GENERATE_RANDOM_GRAPH_STANDARD:
                    case GENERATE_RANDOM_GRAPH_CROSS: {
                        executeIndexedAction(scanner, "Please enter dimension in tuple format. e.g. (5, 5)", choice.getActionType(), true);
                        break;
                    }
                    case GET_NEIGHBORS:
                    case GET_REACHABLES: {
                        executeIndexedAction(scanner, "Please enter index in tuple format. e.g. (1, 1)", choice.getActionType(), false);
                        break;
                    }
                    case PRINT_GRAPH: {
                        executeRequest(new Request(ActionType.PRINT_GRAPH), true);
                        break;
                    }
                    default: {
                        isRunning = false;
                        GraphWebService.getInstance().disconnect();
                        log.info("Good bye!");
                    }
                }
            } while (isRunning);
        }
    }

    private void executeRequest(Request request, boolean resultAsMessage) {
        Response response = GraphWebService.getInstance().executeRequest(request);
        if (isOkResponse(response)) {
            log.info("Result: " + System.lineSeparator() + (resultAsMessage ? response.getMessage() : response.getValue()) + System.lineSeparator());
        }
    }

    private void executeIndexedAction(Scanner scanner, String instruction, ActionType actionType, boolean resultAsMessage) {
        Index index = readIndex(scanner, instruction);
        executeRequest(new Request(actionType, index), resultAsMessage);
    }

    private void saveGraphToFile(Scanner scanner) {
        Response response = GraphWebService.getInstance().executeRequest(new Request(ActionType.GET_GRAPH));
        if (isOkResponse(response)) {
            File file = readFileName(scanner, "Please enter file path to save graph to. e.g. graph1.json", false);

            try {
                Files.write(file.toPath(),
                    GraphWebService.getInstance().getObjectMapper().writeValueAsString(response.getGraph()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

                log.info("Graph stored to: " + file.getCanonicalPath() + System.lineSeparator());
            } catch (IOException e) {
                log.error("Error has occurred while saving graph to disk: " + e, e);
            }
        }
    }

    private void loadGraphFromFile(Scanner scanner) {
        File file = readFileName(scanner, "Please enter file path to load graph from. e.g. graph1.json", true);

        try {
            IGraph<Index> graph = GraphWebService.getInstance().getObjectMapper().readValue(new FileInputStream(file), new TypeReference<>() {});
            Response response = GraphWebService.getInstance().executeRequest(new Request(ActionType.PUT_GRAPH, graph, null, false));
            if (isOkResponse(response)) {
                log.info("Graph sent to server successfully. file=" + file.getCanonicalPath() + System.lineSeparator());
            }
        } catch (IOException e) {
            log.error("Error has occurred while loading graph from disk: " + e, e);
        }
    }

    private void createNewGraph(Scanner scanner) {
        Index dimension = readIndex(scanner, "Enter matrix dimension. e.g. (3, 3)");
        log.info("Enter matrix kind (1=Standard, 2=Cross)");
        int matrixKind = readChoice(scanner, 1, 2);

        IMatrix<Integer> matrix = matrixKind == 1 ? new StandardMatrix(dimension.getRow(), dimension.getColumn()) : new CrossMatrix(dimension.getRow(), dimension.getColumn());

        log.info("Enter matrix values, row by row. You have to enter 0 or 1 only, and you have chosen to create a matrix with " + (dimension.getRow() * dimension.getColumn()) + " elements");
        for (int i = 0; i < dimension.getRow(); i++) {
            for (int j = 0; j < dimension.getColumn(); j++) {
                matrix.setValue(new Index(i, j), readChoice(scanner, 0, 1));
            }
        }

        Index root;
        do {
            root = readIndex(scanner, "Enter root vertex location. e.g. (0, 0). (A root must have value 1 and not 0)");
        } while (!matrix.hasValue(root));

        IGraph<Index> graph = new MatrixGraphAdapter<>(matrix, root);

        Response response = GraphWebService.getInstance().executeRequest(new Request(ActionType.PUT_GRAPH, graph, null, false));
        if (isOkResponse(response)) {
            log.info("Graph sent to server successfully." + System.lineSeparator());
        }
    }

    private boolean isOkResponse(Response response) {
        boolean isOk = response.getStatus() == HttpStatus.OK.getCode();

        if (!isOk) {
            log.error("Something went wrong: " + response.getMessage() + System.lineSeparator());
        }

        return isOk;
    }

    private MenuAction showMenu(Scanner scanner) {
        StringBuilder menu = new StringBuilder("Please select an action: ").append(System.lineSeparator());

        // Get all except disconnect, which we show last.
        for (int i = 1; i < MenuAction.values().length; i++) {
            MenuAction menuAction = MenuAction.values()[i];
            menu.append(menuAction.ordinal()).append(". ").append(menuAction.getText()).append(System.lineSeparator());
        }

        menu.append(MenuAction.DISCONNECT.ordinal()).append(". ").append(MenuAction.DISCONNECT.getText());
        log.info(menu);

        int choice = readChoice(scanner, 0, MenuAction.values().length - 1);
        return MenuAction.values()[choice];
    }

    private int readChoice(Scanner scanner, int minValue, int maxValue) {
        int choice = minValue - 1;
        do {
            String input = scanner.nextLine().trim();
            try { choice = Integer.parseInt(input); } catch (Exception ignore) { }

            if ((choice < minValue) || (choice > maxValue)) {
                log.warn("Wrong input. Try again");
            }
        } while ((choice < minValue) || (choice > maxValue));

        return choice;
    }

    private Index readIndex(Scanner scanner, String instruction) {
        Index index = null;

        log.info(instruction);

        do {
            // Ignore all whitespace characters, to ease parsing
            String input = scanner.nextLine().replaceAll("\\s+", "");
            try {
                // Get rid of "(" and ")", then split by "," so we will have the two numbers.
                String[] nums = input.substring(1, input.length() - 1).split(",");
                index = new Index(Integer.parseInt(nums[0]), Integer.parseInt(nums[1]));
            } catch (Exception ignore) { }

            if (index == null) {
                log.warn("Wrong input. Format is: (0, 0). Try again");
            }
        } while (index == null);

        return index;
    }

    private File readFileName(Scanner scanner, String instruction, boolean validateExisting) {
        File file;

        log.info(instruction);

        do {
            String filePath = scanner.nextLine();
            try {
                file = new File(filePath);
                if (validateExisting && !file.exists()) {
                    file = null;
                }
            } catch (Exception ignore) {
                file = null;
            }

            if (file == null) {
                log.warn("Wrong input. Make sure file exists and we have R/W permissions. Try again");
            }
        } while (file == null);

        return file;
    }

}

