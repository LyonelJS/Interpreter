import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Lexer {
    String input;
    int pos;
    char curr;
    Stack<Integer> indentStack = new Stack<>();
    int listNesting = 0; // New variable to track list nesting

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        // We'll handle "else if" specially, so no need to put it here.
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

    public void advance() {
        pos++;
        curr = pos < input.length() ? input.charAt(pos) : '\0';
    }

    public void skipWhitespace() {
        while (curr == ' ' || curr == '\t') {
            advance();
        }
    }

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
            tokens.add(new Token(TokenType.INDENT, "INDENT", spaces));
        } else {
            while (!indentStack.isEmpty() && spaces < indentStack.peek()) {
                indentStack.pop();
                tokens.add(new Token(TokenType.DEDENT, "DEDENT"));
            }
        }
    }

    public Token number() {
        StringBuilder result = new StringBuilder();
        while (Character.isDigit(curr) || curr == '.') {
            result.append(curr);
            advance();
        }
        return new Token(TokenType.NUMBER, result.toString());
    }

    public Token identifier() {
        StringBuilder result = new StringBuilder();
        while (Character.isLetterOrDigit(curr)) {
            result.append(curr);
            advance();
        }
        String word = result.toString();
        // Check for "else if" (combine "else" followed by "if" on the same line)
        if (word.equals("else")) {
            // Save current state for lookahead.
            int savedPos = pos;
            char savedCurr = curr;
            // Skip spaces/tabs but stop if a newline is encountered.
            while ((curr == ' ' || curr == '\t') && curr != '\n' && curr != '\0') {
                advance();
            }
            // Collect the next word (if any) without consuming it permanently.
            StringBuilder nextWord = new StringBuilder();
            int tempPos = pos;
            char tempCurr = tempPos < input.length() ? input.charAt(tempPos) : '\0';
            while (Character.isLetterOrDigit(tempCurr)) {
                nextWord.append(tempCurr);
                tempPos++;
                tempCurr = tempPos < input.length() ? input.charAt(tempPos) : '\0';
            }
            if (nextWord.toString().equals("if")) {
                // Consume the "if" by advancing the lexer.
                for (int i = 0; i < nextWord.length(); i++) {
                    advance();
                }
                return new Token(TokenType.ELSEIF, "else if");
            } else {
                // Not "else if": revert to saved state.
                pos = savedPos;
                curr = savedCurr;
            }
        }
        TokenType type = KEYWORDS.getOrDefault(word, TokenType.IDENTIFIER);
        return new Token(type, word);
    }

    public Token string() {
        StringBuilder result = new StringBuilder();
        advance(); // Skip opening quote
        while (curr != '"' && curr != '\0') { // Prevent infinite loop
            result.append(curr);
            advance();
        }
        if (curr == '"') {
            advance(); // Skip closing quote
        }
        return new Token(TokenType.STRING, result.toString());
    }

    public Token comma() {
        advance();
        return new Token(TokenType.COMMA, ",");
    }

    public Token dot() {
        advance();
        return new Token(TokenType.DOT, ".");
    }

    public Token operator() {
        switch (curr) {
            case '+':
                advance();
                return new Token(TokenType.PLUS, "+");
            case '-':
                advance();
                return new Token(TokenType.MINUS, "-");
            case '*':
                advance();
                return new Token(TokenType.MULTIPLY, "*");
            case '/':
                advance();
                return new Token(TokenType.DIVIDE, "/");
            case '=':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.EQUAL_EQUAL, "=="); // Equality operator
                }
                return new Token(TokenType.EQUALS, "=");
            case '(':
                advance();
                return new Token(TokenType.LPAREN, "(");
            case ')':
                advance();
                return new Token(TokenType.RPAREN, ")");
            case '>':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.GREATER_EQUAL, ">=");
                }
                return new Token(TokenType.GREATER, ">");
            case '<':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.LESS_EQUAL, "<=");
                }
                return new Token(TokenType.LESS, "<");
            case '!':
                advance();
                if (curr == '=') {
                    advance();
                    return new Token(TokenType.NOTEQUAL, "!=");
                }
                return new Token(TokenType.NOT, "!");
            case '[':
                listNesting++; // Entering a list
                advance();
                return new Token(TokenType.LBRACKET, "[");
            case ']':
                listNesting--; // Exiting a list
                advance();
                return new Token(TokenType.RBRACKET, "]");
            case ':':
                advance();
                return new Token(TokenType.COLON, ":");
            default:
                char unknown = curr;
                advance();
                throw new RuntimeException("Unexpected character: " + unknown);
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
            if ("+-/*()=<>![]:".indexOf(curr) != -1) {
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
                    tokens.add(new Token(TokenType.NEWLINE, "\\n"));
                }
                handleIndentation(tokens);
                continue;
            }
            throw new RuntimeException("Unexpected character: " + curr);
        }

        // Handle final dedents
        while (indentStack.peek() > 0) {
            indentStack.pop();
            tokens.add(new Token(TokenType.DEDENT, "DEDENT"));
        }

        tokens.add(new Token(TokenType.EOF, "EOF"));
        return tokens;
    }
}
