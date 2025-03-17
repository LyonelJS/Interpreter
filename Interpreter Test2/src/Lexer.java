import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Lexer {
    String input;
    int pos;
    char curr;
    int line = 1; // Track the current line number
    Stack<Integer> indentStack = new Stack<>();
    int listNesting = 0; // Track list nesting

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        // "else if" will be handled separately.
        KEYWORDS.put("function", TokenType.DEF);
        KEYWORDS.put("class", TokenType.CLASS);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("def", TokenType.DEF);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("in", TokenType.IN);
    }

    public Lexer(String input) {
        this.input = input;
        this.pos = 0;
        this.curr = input.length() > 0 ? input.charAt(pos) : '\0';
        indentStack.push(0); // Start with indent level 0
    }

    // Advances the current position and updates the line counter when a newline is encountered.
    public void advance() {
        if (curr == '\n') {
            line++;
        }
        pos++;
        curr = pos < input.length() ? input.charAt(pos) : '\0';
    }

    public void skipWhitespace() {
        while (curr == ' ' || curr == '\t') {
            advance();
        }
    }

    // Handles indentation by comparing the number of leading spaces/tabs.
    private void handleIndentation(List<Token> tokens) {
        int spaces = 0;
        while (curr == ' ' || curr == '\t') {
            spaces += (curr == ' ') ? 1 : 4; // Assume tab = 4 spaces
            advance();
        }
        if (curr == '\n' || curr == '\0') {
            return; // Ignore blank lines
        }
        int prevIndent = indentStack.peek();
        if (spaces > prevIndent) {
            indentStack.push(spaces);
            tokens.add(new Token(TokenType.INDENT, "INDENT", line, spaces));
        } else {
            while (!indentStack.isEmpty() && spaces < indentStack.peek()) {
                int poppedIndent = indentStack.pop();
                tokens.add(new Token(TokenType.DEDENT, "DEDENT", line, poppedIndent));
            }
        }
    }

    public Token number() {
        StringBuilder result = new StringBuilder();
        while (Character.isDigit(curr) || curr == '.') {
            result.append(curr);
            advance();
        }
        return new Token(TokenType.NUMBER, result.toString(), line);
    }

    public Token identifier() {
        StringBuilder result = new StringBuilder();
        while (Character.isLetterOrDigit(curr)) {
            result.append(curr);
            advance();
        }
        String word = result.toString();
        // Check for "else if" by looking ahead after "else"
        if (word.equals("else")) {
            int savedPos = pos;
            char savedCurr = curr;
            while ((curr == ' ' || curr == '\t') && curr != '\n' && curr != '\0') {
                advance();
            }
            StringBuilder nextWord = new StringBuilder();
            int tempPos = pos;
            char tempCurr = tempPos < input.length() ? input.charAt(tempPos) : '\0';
            while (Character.isLetterOrDigit(tempCurr)) {
                nextWord.append(tempCurr);
                tempPos++;
                tempCurr = tempPos < input.length() ? input.charAt(tempPos) : '\0';
            }
            if (nextWord.toString().equals("if")) {
                for (int i = 0; i < nextWord.length(); i++) {
                    advance();
                }
                return new Token(TokenType.ELSEIF, "else if", line);
            } else {
                pos = savedPos;
                curr = savedCurr;
            }
        }
        TokenType type = KEYWORDS.getOrDefault(word, TokenType.IDENTIFIER);
        return new Token(type, word, line);
    }

    public Token string() {
        StringBuilder result = new StringBuilder();
        advance(); // Skip opening quote
        while (curr != '"' && curr != '\0') {
            result.append(curr);
            advance();
        }
        if (curr == '"') {
            advance(); // Skip closing quote
        }
        return new Token(TokenType.STRING, result.toString(), line);
    }

    public Token comma() {
        advance();
        return new Token(TokenType.COMMA, ",", line);
    }

    public Token dot() {
        advance();
        return new Token(TokenType.DOT, ".", line);
    }

    public Token operator() {
        switch (curr) {
            case '+':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.PLUS_EQUAL, "+=", line);
                }
                return new Token(TokenType.PLUS, "+", line);
            case '-':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.MINUS_EQUAL, "-=", line);
                }
                return new Token(TokenType.MINUS, "-", line);
            case '*':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.MULTIPLY_EQUAL, "*=", line);
                }
                return new Token(TokenType.MULTIPLY, "*", line);
            case '/':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.DIVIDE_EQUAL, "/=", line);
                }
                return new Token(TokenType.DIVIDE, "/", line);
            case '=':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.EQUAL_EQUAL, "==", line);
                }
                return new Token(TokenType.EQUALS, "=", line);
            case '(':
                advance();
                return new Token(TokenType.LPAREN, "(", line);
            case ')':
                advance();
                return new Token(TokenType.RPAREN, ")", line);
            case '>':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.GREATER_EQUAL, ">=", line);
                }
                return new Token(TokenType.GREATER, ">", line);
            case '<':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.LESS_EQUAL, "<=", line);
                }
                return new Token(TokenType.LESS, "<", line);
            case '!':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.NOTEQUAL, "!=", line);
                }
                return new Token(TokenType.NOT, "!", line);
            case '[':
                listNesting++; // Entering a list
                advance();
                return new Token(TokenType.LBRACKET, "[", line);
            case ']':
                listNesting--; // Exiting a list
                advance();
                return new Token(TokenType.RBRACKET, "]", line);
            case ':':
                advance();
                return new Token(TokenType.COLON, ":", line);
            case '%':
                advance();
                return new Token(TokenType.MODULO, "%", line);
            default:
                char unknown = curr;
                advance();
                throw new RuntimeException("Error at line " + line + ": " + "Unexpected character: " + unknown);
        }
    }

    private char peek() {
        return pos + 1 < input.length() ? input.charAt(pos + 1) : '\0';
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (curr != '\0') {
            if (curr == '/' && peek() == '/') {
                advance();
                advance();
                while (curr != '\n' && curr != '\0') {
                    advance();
                }
                continue;
            }
            if (curr == ' ' || curr == '\t') {
                skipWhitespace();
                continue;
            }
            if (curr == ',') {
                tokens.add(comma());
                continue;
            }
            if (curr == '.') {
                tokens.add(dot());
                continue;
            }
            if (Character.isDigit(curr)) {
                tokens.add(number());
                continue;
            }
            if (Character.isLetter(curr)) {
                tokens.add(identifier());
                continue;
            }
            if (curr == '"') {
                tokens.add(string());
                continue;
            }
            if ("+-/*()=<>![]:%".indexOf(curr) != -1) {
                tokens.add(operator());
                continue;
            }
            if (curr == '\n') {
                advance(); // Move past newline
                // If inside a list, ignore newlines completely.
                if (listNesting > 0) {
                    continue;
                }
                if (tokens.isEmpty() || tokens.get(tokens.size() - 1).type != TokenType.NEWLINE) {
                    tokens.add(new Token(TokenType.NEWLINE, "\\n", line));
                }
                handleIndentation(tokens);
                continue;
            }
            throw new RuntimeException("Error at line " + line + ": " + "Unexpected character: " + curr);
        }

        // Handle final dedents by popping any remaining indent levels.
        while (indentStack.peek() > 0) {
            int poppedIndent = indentStack.pop();
            tokens.add(new Token(TokenType.DEDENT, "DEDENT", line, poppedIndent));
        }

        tokens.add(new Token(TokenType.EOF, "EOF", line));
        return tokens;
    }
}
