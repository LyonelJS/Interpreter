public class Token {
    TokenType type;
    String value;
    int indentLevel;
    int line;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
        this.indentLevel = -1;
    }

    public Token(TokenType type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }
    public Token(TokenType type, String value, int line, int indentLevel) {
        this.type = type;
        this.value = value;
        this.indentLevel = indentLevel;
        this.line = line;
    }

    @Override
    public String toString() {
        if (indentLevel >= 0) {
            return "Token(" + type + ", '" + value + "', line=" + line + "', indent=" + indentLevel + ")";
        }
        return "Token(" + type + ", '" + value + "', line=" + line +  "')";
    }
}
