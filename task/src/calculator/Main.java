package calculator;

import java.util.*;

public class Main {
    final static String commandRegex = "^/.*";
    final static String expressionRegex = "[-]?([a-zA-Z]+|\\d+)([ ]?([\\+-])[ ]?[-]?([a-zA-Z]+|\\d+))*";
    final static String assignmentRegEx = "^[a-zA-Z]+[ ]*=[ ]*.*";
    final static String invalidIDRegEx = "^([a-zA-Z]+[0-9]+|[0-9]+[a-zA-Z]+)[ ]?=.*";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Map<String, Integer> varMap = new HashMap<>();

        while (true) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            try {
                // clean up the input string
                input = cleanupInputString(input);

                if (input.matches(commandRegex)) {
                    boolean exit = processCommand(input);
                    if (exit == true) break;
                } else if (input.matches(assignmentRegEx)) {
                    processAssignment(input, varMap);
                } else if (input.matches(".*=.*")) {
                    throw new Exception("Invalid identifier");
                } else {
                    System.out.println(processExpression(input, varMap));
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static String cleanupInputString(String input) throws Exception {
        String cleanInput = "";

        if (input.matches(assignmentRegEx)) {
            // this is an assignment followed by expression
            // clean up what's before the '=' then
            // check what follows the '=' as an expression
            ArrayList<String> assignmentParts = new ArrayList<String>(Arrays.asList(input.split("=")));
            if (assignmentParts.size() != 2) {
                throw new Exception("Invalid assignment");
            } else {
                // check var
                String cleanVarName = assignmentParts.get(0).trim();
                if (!cleanVarName.matches("[a-zA-Z]+"))
                    throw new Exception("Invalid assignment");
                // process expression and reassemble
                cleanInput = cleanVarName + " = " + cleanupInputString(assignmentParts.get(1));
            }
        } else {
            cleanInput = input
                    // remove leading whitespace
                    .trim()

                    // replace '+-' with single '-'
                    .replaceAll("[ ]*\\+[ ]*-[ ]*", "-")

                    // replace '-+' with single '-'
                    .replaceAll("[ ]*-[ ]*\\+[ ]*", "-")

                    // replace odd number of '-' with single '-'
                    .replaceAll("-[ ]*(-[ ]*-)*[ ]*", "-")

                    // replace even number of '-' with single '+'
                    .replaceAll("[ ]*-[ ]*-[ ]*", "+")

                    // replace multiple '+' operator with just one
                    .replaceAll("\\+[ ]*(\\+)*", "+")

                    // remove leading '+'
                    .replaceAll("^[ ]*\\+[ ]*", "")
                    .replaceAll("\\([ ]*\\+[ ]*", "(")

                    // replace leading '-' with '0 -'
                    .replaceAll("^[ ]*-[ ]*", "0 -")
                    .replaceAll("\\([ ]*-[ ]*", "( 0 -")

                    // replace '*+' with single '*'
                    .replaceAll("[ ]*\\*[ ]*\\+[ ]*", "*")

                    // replace '/+' with single '/'
                    .replaceAll("[ ]*/[ ]*\\+[ ]*", "/")

                    // pad space around operators
                    .replaceAll("\\+", " + ")
                    .replaceAll("\\-", " - ")
                    .replaceAll("\\*", " * ")
                    .replaceAll("/", " / ")

                    // pad single space around parenthesis
                    .replaceAll("\\(", " ( ")
                    .replaceAll("\\)", " ) ")

                    // trim again and remove embedded whitespace
                    .trim()
                    .replaceAll("\\s+", " ");

            // multiple successive '*' or '/' is invalid in an expression
            if (cleanInput.matches(".*[\\*/]+[ ]*[\\*/]+.*"))
                throw new Exception("Invalid expression");

            checkParens(cleanInput);
        }
        return cleanInput;
    }

    private static void checkParens(String expression) throws Exception {
        String justParens = expression.replaceAll("[^\\(\\)]", "");

        Deque<Character> charStack = new ArrayDeque<>();

        if (justParens.length() % 2 != 0) {
            throw new Exception("Invalid expression");
        }
        for (int i = 0; i < justParens.length(); i++) {
            Character topOfStack;
            switch (justParens.charAt(i)) {
                case '{':
                case '[':
                case '(':
                    charStack.push(justParens.charAt(i));
                    break;
                case '}':
                    if (charStack.isEmpty() || !charStack.pop().equals('{')) {
                        throw new Exception("Invalid expression");
                    }
                    break;
                case ']':
                    if (charStack.isEmpty() || !charStack.pop().equals('[')) {
                        throw new Exception("Invalid expression");
                    }
                    break;
                case ')':
                    if (charStack.isEmpty() || !charStack.pop().equals('(')) {
                        throw new Exception("Invalid expression");
                    }
                    break;
            }
        }
        if (!charStack.isEmpty())
            throw new Exception("Invalid expression");
    }

    private static String replaceVars(String input, Map<String, Integer> varMap) throws Exception {
        ArrayList<String> values = new ArrayList<String>(Arrays.asList(input.split(" ")));
        StringBuilder newInput = new StringBuilder("");

        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).matches("[a-zA-Z]+")) {
                // first operand is a var value
                if (varMap.containsKey(values.get(i)))
                    values.set(i, String.valueOf(varMap.get(values.get(i))));
                else {
                    throw new Exception("Unknown variable");
                }
            }
            newInput.append(values.get(i) + " ");
        }
        if (newInput.toString().matches(".*[a-zA-Z].*")) {
            throw new Exception("Invalid assignment");
        }
        return newInput.toString().trim();
    }

    private static boolean processCommand(String input) throws Exception {
        boolean exitStatus = false;
        input = input.replaceAll(" ", "");
        if (input.matches("/exit")) {
            System.out.println("Bye!");
            exitStatus = true;
        } else if (input.equals("/help")) {
            System.out.println("The program does integer math\nAccepted operators are + - * / ()");
        } else {
            throw new Exception("Unknown command");
        }
        return exitStatus;
    }

    private static Integer processExpression(String input, Map<String, Integer> varMap) throws Exception {
        input = replaceVars(input, varMap);
        ArrayList<String> values = new ArrayList<String>(Arrays.asList(input.split(" ")));

        StringBuilder postfix = new StringBuilder("");
        Deque<String> stack = new ArrayDeque<>();

        // convert to postfix by pushing values onto stack
        for (int i = 0; i < values.size(); i++) {
            String thing = values.get(i).trim();
            if (thing.matches("\\d+")) {
                postfix.append(thing + " ");
            } else if (stack.isEmpty() || stack.peek().equals("(")) {
                stack.push(thing);
            } else if (precedence(thing) > precedence(stack.peek())) {
                stack.push(thing);
            } else if (thing.equals("(")) {
                stack.push("(");
            } else if (thing.equals(")")) {
                while (!stack.peek().equals("(")) {
                    postfix.append(stack.pop() + " ");
                }
                stack.pop();  // pop the opening paren
            } else if (precedence(thing) <= precedence(stack.peek())) {
                while (!stack.isEmpty() && precedence(thing) <= precedence(stack.peek())) {
                    postfix.append(stack.pop() + " ");
                }
                if (!stack.isEmpty() && (thing.equals(")") && stack.peek().equals("("))) {
                    stack.pop();
                }
                stack.push(thing);
            }
        }
        while (!stack.isEmpty()) {
            postfix.append(stack.pop() + " ");
        }

        // evaluate the postfix expression
//        System.out.println("postfix: " + postfix);
        values = new ArrayList<String>(Arrays.asList(postfix.toString().split(" ")));
        stack = new ArrayDeque<>();

        for (int i = 0; i < values.size(); i++) {
            String thing = values.get(i).trim();
            if (thing.trim().matches("\\d+")) {
                stack.push(thing);
            } else if (thing.matches("[\\+\\-\\*/]")) {
                int operand2 = Integer.valueOf(stack.pop());
                int operand1 = Integer.valueOf(stack.pop());
                switch (thing) {
                    case "+":
                        stack.push(Integer.toString(operand1 + operand2));
                        break;
                    case "-":
                        stack.push(Integer.toString(operand1 - operand2));
                        break;
                    case "*":
                        stack.push(Integer.toString(operand1 * operand2));
                        break;
                    case "/":
                        stack.push(Integer.toString(operand1 / operand2));
                        break;
                }
            }
        }
        return Integer.valueOf(stack.peek());
    }

    private static int precedence(String ch) {
        if (ch.equals("+") || ch.equals("-")) {
            return 1;              //Precedence of + or - is 1
        } else if (ch.equals("*") || ch.equals("/")) {
            return 2;            //Precedence of * or / is 2
        } else if (ch.equals("^")) {
            return 3;            //Precedence of ^ is 3
        } else {
            return 0;
        }
    }

    private static void processAssignment(String input, Map<String, Integer> varMap) throws Exception {
        ArrayList<String> values = new ArrayList<String>(Arrays.asList(input.split("=")));
        String varName = values.get(0).trim();
        varMap.put(varName, processExpression(values.get(1).trim(), varMap));
    }
}
