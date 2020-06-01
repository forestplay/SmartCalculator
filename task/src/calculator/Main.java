package calculator;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Map<String, Integer> varMap = new HashMap<>();

        while (true) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            // clean up the input string
            input = cleanupInput(input);

            String cmdRegex = "^/.*";
            String expRegex = "[-]?([a-zA-Z]+|\\d+)([ ]?([\\+-])[ ]?[-]?([a-zA-Z]+|\\d+))*";
            String assRegEx = "[a-zA-Z]+[ ]? =[ ]?" + expRegex;
            String invalidIDRegEx = "^([a-zA-Z]+[0-9]+|[0-9]+[a-zA-Z]+)[ ]?=.*";

            try {
                ArrayList<String> values = new ArrayList<String>(Arrays.asList(input.split(" ")));
                if (input.matches(cmdRegex)) {
//                    System.out.println(" -- found command");
                    boolean exit = processCommand(input);
                    if (exit == true) break;
                } else if (input.matches(invalidIDRegEx)) {
//                    System.out.println(" -- found invalid ID");
                    throw new Exception("Invalid identifier");
                } else if (input.matches(assRegEx)) {
//                    System.out.println(" -- found assignment");
                    processAssignment(values, varMap);
                } else if (input.matches(expRegex)) {
//                    System.out.println(" -- found expression");
                    System.out.println(processExpression(values, varMap));
                } else {
                    if (values.contains("="))
                        throw new Exception("Invalid assignment");
                    else
                        throw new Exception("Invalid expression");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static String cleanupInput(String input) {
        String cleanInput = input
                // replace odd number of '-' with " + -", minus pushed to next operand
                .replaceAll("-[ ]*(-[ ]*-[ ]*)*", " + -")

                // replace even number of '-' with single '+'
                .replaceAll("-[ ]*-", " + ")

                // remove leading '+'
                .replaceAll("^[ ]*\\+", "")

                // replace multiple '+' operator with just one
                .replaceAll("\\+([ ]*\\+)*", " + ")

                // insert one space on either side of operators: + =
                .replaceAll("([ ]*[=\\+][ ]*)", " $1 ")

                // remove leading/lagging whitespace & embedded extra whitespace
                .trim()
                .replaceAll("[ \t]+", " ");
        return cleanInput;
    }

    private static boolean processCommand(String input) throws Exception {
        boolean exitStatus = false;
        if (input.equals("/exit")) {
            System.out.println("Bye!");
            exitStatus = true;
        } else if (input.equals("/help")) {
            System.out.println("The program calculates the sum of numbers");
        } else {
            throw new Exception("Unknown command");
        }
        return exitStatus;
    }

    private static Integer processExpression(List<String> values, Map<String, Integer> varMap) throws Exception {
        Integer sum = 0;
        boolean negativeNumber = false;

        if (values.size() == 0)
            return 0;

        if (values.get(0).matches("^-.*")) {
            negativeNumber = true;
            values.set(0, values.get(0).substring(1));
        }

        if (values.get(0).matches("[a-zA-Z]+")) {
            // first operand is a var value
            if (varMap.containsKey(values.get(0)))
                sum = varMap.get(values.get(0));
            else {
                throw new Exception("Unknown variable");
            }
        }

        if (values.get(0).matches("[+-]?\\d+")) {
            // first operand is an number
            sum = Integer.parseInt(values.get(0));
        }

        if (negativeNumber)
            sum = -sum;

        if (values.size() > 1) {
            sum += processExpression(values.subList(1, values.size()), varMap);
        }
        return sum;
    }

    private static void processAssignment(ArrayList<String> input, Map<String, Integer> varMap) throws
            Exception {
        String varName = input.get(0);

        // skip input[1] because it's the equal sign

        // replace all var with values
        for (int i = 2; i < input.size(); i++) {
            if (input.get(i).matches("[a-zA-Z]+")) {
                if (varMap.containsKey(input.get(i))) {
                    input.set(i, String.valueOf(varMap.get(input.get(i))));
                } else {
                    throw new Exception("Unknown variable");
                }
            }
        }
        varMap.put(varName, processExpression(input.subList(2, input.size()), varMap));
    }
}
