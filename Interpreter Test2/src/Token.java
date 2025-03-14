public class Token {
    TokenType type;
    String value;
    int indentLevel;  // Stores the indent level (if applicable)

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
        this.indentLevel = -1; // Default value when not used
    }

    public Token(TokenType type, String value, int indentLevel) {
        this.type = type;
        this.value = value;
        this.indentLevel = indentLevel;
    }

    @Override
    public String toString() {
        if (indentLevel >= 0) {
            return "Token(" + type + ", '" + value + "', indent=" + indentLevel + ")";
        }
        return "Token(" + type + ", '" + value + "')";
    }
}
