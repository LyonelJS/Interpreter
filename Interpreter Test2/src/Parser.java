import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class Parser {
    private List<Token> tokens;
    private int pos = 0;
    private Token curr;

    // Symbol table support: a stack of scopes.
    // Each scope is a map from variable, function, or class names to Symbols.
    private List<HashMap<String, Symbol>> scopes = new ArrayList<>();

    // A symbol can be a variable, a function, or a class.
    private static class Symbol {
        String name;
        SymbolType type;
        int paramCount; // Only applicable for functions.

        Symbol(String name, SymbolType type, int paramCount) {
            this.name = name;
            this.type = type;
            this.paramCount = paramCount;
        }
    }

    // Added CLASS type.
    private enum SymbolType {
        VARIABLE,
        FUNCTION,
        CLASS
    }

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.curr = tokens.get(pos);
        // Initialize global scope.
        enterScope();
        // Define built-in functions in the global scope.
        defineBuiltInFunctions();
    }

    // Add built-in functions (like input and range) to the global symbol table.
    private void defineBuiltInFunctions() {
        // "input" is defined as a built-in function expecting 1 parameter.
        scopes.get(scopes.size() - 1).put("input", new Symbol("input", SymbolType.FUNCTION, 1));
        // "range" is defined as a built-in function expecting 2 parameters.
        scopes.get(scopes.size() - 1).put("range", new Symbol("range", SymbolType.FUNCTION, 2));
        // Add more built-in functions as needed.
    }

    // Push a new scope.
    private void enterScope() {
        scopes.add(new HashMap<>());
    }

    // Pop the current scope.
    private void exitScope() {
        scopes.remove(scopes.size() - 1);
    }

    // Look up a symbol by name (searching from innermost to outermost scope).
    private Symbol lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }
        return null;
    }

    // Define a variable in the current scope.
    private void defineVariable(String name) {
        HashMap<String, Symbol> currentScope = scopes.get(scopes.size() - 1);
        if (currentScope.containsKey(name)) {
            throw new RuntimeException("Semantic error: Variable '" + name + "' is already defined in this scope.");
        }
        currentScope.put(name, new Symbol(name, SymbolType.VARIABLE, 0));
    }

    // Define a function in the current scope.
    private void defineFunction(String name, int paramCount) {
        HashMap<String, Symbol> currentScope = scopes.get(scopes.size() - 1);
        if (currentScope.containsKey(name)) {
            throw new RuntimeException("Semantic error: Function '" + name + "' is already defined in this scope.");
        }
        currentScope.put(name, new Symbol(name, SymbolType.FUNCTION, paramCount));
    }

    // Define a class in the current scope.
    private void defineClass(String name) {
        HashMap<String, Symbol> currentScope = scopes.get(scopes.size() - 1);
        if (currentScope.containsKey(name)) {
            throw new RuntimeException("Semantic error: Class '" + name + "' is already defined in this scope.");
        }
        currentScope.put(name, new Symbol(name, SymbolType.CLASS, 0));
    }

    private void advance() {
        pos++;
        if (pos < tokens.size()) {
            curr = tokens.get(pos);
        }
    }

    private boolean match(TokenType type) {
        if (curr.type == type) {
            advance();
            return true;
        }
        return false;
    }

    private void expect(TokenType type, String errorMessage) {
        if (curr.type != type) {
            throw new RuntimeException(errorMessage + " Found: " + curr.value);
        }
        advance();
    }

    // Peek the next token without consuming it.
    private Token peekToken() {
        if (pos + 1 < tokens.size()) {
            return tokens.get(pos + 1);
        }
        return new Token(TokenType.EOF, "EOF");
    }

    // Skip extra newlines between statements.
    private void skipNewlines() {
        while (curr.type == TokenType.NEWLINE) {
            advance();
        }
    }

    // Entry point: returns a BlockNode containing all top-level statements.
    public ASTNode parse() {
        skipNewlines();
        List<ASTNode> statements = new ArrayList<>();
        while (curr.type != TokenType.EOF) {
            ASTNode stmt = statement();
            if (stmt != null) {
                statements.add(stmt);
            }
            skipNewlines();
        }
        return new BlockNode(statements);
    }

    // Parse a return statement.
    private ASTNode returnStatement() {
        advance(); // consume RETURN token
        ASTNode expr = expression();
        return new ReturnNode(expr);
    }

    // statement -> printStmt | ifStmt | whileStmt | forStmt | funcDef | classDef | returnStmt | assignment | expression
    private ASTNode statement() {
        if (curr.type == TokenType.PRINT) {
            return printStatement();
        } else if (curr.type == TokenType.IF) {
            return ifStatement();
        } else if (curr.type == TokenType.WHILE) {
            return whileStatement();
        } else if (curr.type == TokenType.FOR) {
            return forStatement();
        } else if (curr.type == TokenType.DEF) {
            return functionDefinition();
        } else if (curr.type == TokenType.CLASS) {
            return classDefinition();
        } else if (curr.type == TokenType.RETURN) {
            return returnStatement();
        } else {
            // Parse an expression as a candidate for assignment.
            ASTNode expr = expression();
            if (curr.type == TokenType.EQUALS) {
                // Left-hand side must be assignable.
                if (!(expr instanceof IdentifierNode || expr instanceof IndexNode || expr instanceof FieldAccessNode)) {
                    throw new RuntimeException("Invalid assignment target.");
                }
                advance(); // consume '='
                ASTNode right = expression();
                if (expr instanceof IdentifierNode) {
                    IdentifierNode idNode = (IdentifierNode) expr;
                    if (lookup(idNode.identifier.value) == null) {
                        defineVariable(idNode.identifier.value);
                    }
                    return new AssignmentNode(right, idNode.identifier);
                } else {
                    return new IndexAssignmentNode(expr, right);
                }
            }
            return expr;
        }
    }

    // Parse a print statement.
    private ASTNode printStatement() {
        advance(); // consume PRINT token
        ASTNode expr = expression();
        return new PrintNode(expr);
    }

    // Parse a List.
    private ASTNode parseList() {
        expect(TokenType.LBRACKET, "Expected '[' to start a list");
        List<ASTNode> elements = new ArrayList<>();
        if (curr.type != TokenType.RBRACKET) { // Handle empty list []
            elements.add(expression());
            while (match(TokenType.COMMA)) {
                elements.add(expression());
            }
        }
        expect(TokenType.RBRACKET, "Expected ']' to close the list");
        return new ListNode(elements);
    }

    // Parse an if-statement.
    private ASTNode ifStatement() {
        // Expect and consume IF.
        expect(TokenType.IF, "Expected 'if'"); // Now we require an IF token here.
        ASTNode condition = expression();
        expect(TokenType.NEWLINE, "Expected newline after if condition");
        expect(TokenType.INDENT, "Expected indent after if condition");
        enterScope();
        List<ASTNode> thenStatements = new ArrayList<>();
        skipNewlines();
        while (curr.type != TokenType.DEDENT && curr.type != TokenType.EOF) {
            thenStatements.add(statement());
            skipNewlines();
        }
        expect(TokenType.DEDENT, "Expected dedent after if block");
        exitScope();
        ASTNode thenBlock = new BlockNode(thenStatements);
        ASTNode elseBlock = null;
        if (curr.type == TokenType.ELSEIF || curr.type == TokenType.ELSE) {
            elseBlock = parseElseChain();
        }
        return new IfNode(condition, thenBlock, elseBlock);
    }

    // Helper method to parse the "else if"/"else" chain.
    private ASTNode parseElseChain() {
        if (curr.type == TokenType.ELSEIF) {
            advance(); // consume ELSEIF token (which represents "else if")
            ASTNode condition = expression();
            expect(TokenType.NEWLINE, "Expected newline after else if condition");
            expect(TokenType.INDENT, "Expected indent after else if condition");
            enterScope();
            List<ASTNode> elseifStatements = new ArrayList<>();
            skipNewlines();
            while (curr.type != TokenType.DEDENT && curr.type != TokenType.EOF) {
                elseifStatements.add(statement());
                skipNewlines();
            }
            expect(TokenType.DEDENT, "Expected dedent after else if block");
            exitScope();
            ASTNode elseifBlock = new BlockNode(elseifStatements);
            ASTNode subsequentElse = null;
            if (curr.type == TokenType.ELSEIF || curr.type == TokenType.ELSE) {
                subsequentElse = parseElseChain();
            }
            return new IfNode(condition, elseifBlock, subsequentElse);
        } else if (curr.type == TokenType.ELSE) {
            advance(); // consume ELSE token
            expect(TokenType.NEWLINE, "Expected newline after else");
            expect(TokenType.INDENT, "Expected indent after else");
            enterScope();
            List<ASTNode> elseStatements = new ArrayList<>();
            skipNewlines();
            while (curr.type != TokenType.DEDENT && curr.type != TokenType.EOF) {
                elseStatements.add(statement());
                skipNewlines();
            }
            expect(TokenType.DEDENT, "Expected dedent after else block");
            exitScope();
            return new BlockNode(elseStatements);
        } else {
            return null;
        }
    }

    // Parse a while-statement.
    private ASTNode whileStatement() {
        advance(); // consume WHILE token
        ASTNode condition = expression();
        expect(TokenType.NEWLINE, "Expected newline after while condition");
        expect(TokenType.INDENT, "Expected indent after while condition");
        enterScope();
        List<ASTNode> bodyStatements = new ArrayList<>();
        skipNewlines();
        while (curr.type != TokenType.DEDENT && curr.type != TokenType.EOF) {
            bodyStatements.add(statement());
            skipNewlines();
        }
        expect(TokenType.DEDENT, "Expected dedent after while block");
        exitScope();
        return new WhileNode(condition, new BlockNode(bodyStatements));
    }

    // Parse a for-statement.
    // Grammar: forStmt -> FOR IDENTIFIER IN expression NEWLINE INDENT statement* DEDENT
    private ASTNode forStatement() {
        advance(); // consume FOR token
        expect(TokenType.IDENTIFIER, "Expected loop variable in for statement");
        Token loopVar = tokens.get(pos - 1);
        if (match(TokenType.IN)) {
            // For-each loop: for i in x
            ASTNode listExpr = expression();
            expect(TokenType.NEWLINE, "Expected newline after for header");
            expect(TokenType.INDENT, "Expected indent after for header");
            enterScope();
            defineVariable(loopVar.value);
            List<ASTNode> bodyStatements = new ArrayList<>();
            skipNewlines();
            while (curr.type != TokenType.DEDENT && curr.type != TokenType.EOF) {
                bodyStatements.add(statement());
                skipNewlines();
            }
            expect(TokenType.DEDENT, "Expected dedent after for block");
            exitScope();
            return new ForEachNode(loopVar, listExpr, new BlockNode(bodyStatements));
        } else {
            // Numeric for loop: for i = start, end
            expect(TokenType.EQUALS, "Expected '=' after loop variable in for statement");
            ASTNode start = expression();
            expect(TokenType.COMMA, "Expected ',' after start value in for statement");
            ASTNode end = expression();
            expect(TokenType.NEWLINE, "Expected newline after for header");
            expect(TokenType.INDENT, "Expected indent after for header");
            enterScope();
            defineVariable(loopVar.value);
            List<ASTNode> bodyStatements = new ArrayList<>();
            skipNewlines();
            while (curr.type != TokenType.DEDENT && curr.type != TokenType.EOF) {
                bodyStatements.add(statement());
                skipNewlines();
            }
            expect(TokenType.DEDENT, "Expected dedent after for block");
            exitScope();
            return new ForNode(loopVar, start, end, new BlockNode(bodyStatements));
        }
    }

    // Parse a function definition.
    private ASTNode functionDefinition() {
        advance(); // consume DEF token
        expect(TokenType.IDENTIFIER, "Expected function name");
        Token functionName = tokens.get(pos - 1);
        expect(TokenType.LPAREN, "Expected '(' after function name");
        List<Token> parameters = new ArrayList<>();
        if (curr.type != TokenType.RPAREN) {
            expect(TokenType.IDENTIFIER, "Expected parameter name");
            parameters.add(tokens.get(pos - 1));
            while (match(TokenType.COMMA)) {
                if (curr.type == TokenType.RPAREN) break;
                expect(TokenType.IDENTIFIER, "Expected parameter name");
                parameters.add(tokens.get(pos - 1));
            }
        }
        expect(TokenType.RPAREN, "Expected ')' after parameters");
        defineFunction(functionName.value, parameters.size());
        expect(TokenType.NEWLINE, "Expected newline after function header");
        expect(TokenType.INDENT, "Expected indent in function body");
        enterScope();
        for (Token param : parameters) {
            defineVariable(param.value);
        }
        List<ASTNode> bodyStatements = new ArrayList<>();
        skipNewlines();
        while (curr.type != TokenType.DEDENT && curr.type != TokenType.EOF) {
            bodyStatements.add(statement());
            skipNewlines();
        }
        expect(TokenType.DEDENT, "Expected dedent after function body");
        exitScope();
        ASTNode body = new BlockNode(bodyStatements);
        return new FunctionDefinitionNode(functionName, parameters, body);
    }

    // Parse a class definition.
    private ASTNode classDefinition() {
        advance(); // consume CLASS token
        expect(TokenType.IDENTIFIER, "Expected class name");
        Token className = tokens.get(pos - 1);
        if (scopes.get(scopes.size() - 1).containsKey(className.value)) {
            throw new RuntimeException("Semantic error: Class '" + className.value + "' is already defined in this scope.");
        }
        defineClass(className.value);
        expect(TokenType.NEWLINE, "Expected newline after class name");
        expect(TokenType.INDENT, "Expected indent in class body");
        enterScope();
        List<ASTNode> members = new ArrayList<>();
        skipNewlines();
        while (curr.type != TokenType.DEDENT && curr.type != TokenType.EOF) {
            members.add(classMember());
            skipNewlines();
        }
        expect(TokenType.DEDENT, "Expected dedent after class body");
        exitScope();
        return new ClassDefinitionNode(className, members);
    }

    // Parse a class member: either a method (DEF) or an attribute assignment.
    private ASTNode classMember() {
        if (curr.type == TokenType.DEF) {
            return functionDefinition();
        } else if (curr.type == TokenType.IDENTIFIER) {
            Token temp = curr;
            int tempPos = pos;
            advance();
            if (curr.type == TokenType.EQUALS) {
                pos = tempPos;
                curr = temp;
                return statement();
            } else {
                pos = tempPos;
                curr = temp;
                return statement();
            }
        } else {
            return statement();
        }
    }

    // Parse object instantiation without using the keyword "new".
    // Grammar: objectCreationWithoutNew -> IDENTIFIER LPAREN [ argumentList ] RPAREN
    private ASTNode objectCreationWithoutNew(Token className) {
        expect(TokenType.LPAREN, "Expected '(' after class name in object instantiation");
        List<ASTNode> arguments = new ArrayList<>();
        if (curr.type != TokenType.RPAREN) {
            arguments.add(expression());
            while (curr.type == TokenType.COMMA) {
                advance();
                if (curr.type == TokenType.RPAREN) break;
                arguments.add(expression());
            }
        }
        expect(TokenType.RPAREN, "Expected ')' after arguments in object instantiation");
        return new ObjectCreationNode(className, arguments);
    }

    // Parse a function call.
    private ASTNode functionCall(Token nameToken) {
        expect(TokenType.LPAREN, "Expected '(' in function call");
        List<ASTNode> arguments = new ArrayList<>();
        if (curr.type != TokenType.RPAREN) {
            arguments.add(expression());
            while (curr.type == TokenType.COMMA) {
                advance();
                if (curr.type == TokenType.RPAREN) break;
                arguments.add(expression());
            }
        }
        expect(TokenType.RPAREN, "Expected ')' after arguments");
        return new FunctionCallNode(nameToken, arguments);
    }

    // === Expression Parsing ===
    // We update the expression grammar to support boolean logic.
    private ASTNode expression() {
        return logicOr();
    }

    // logicOr -> logicAnd ( OR logicAnd )*
    private ASTNode logicOr() {
        ASTNode node = logicAnd();
        while (curr.type == TokenType.OR) {
            Token op = curr;
            advance();
            ASTNode right = logicAnd();
            node = new BinaryOpNode(node, op, right);
        }
        return node;
    }

    // logicAnd -> equality ( AND equality )*
    private ASTNode logicAnd() {
        ASTNode node = equality();
        while (curr.type == TokenType.AND) {
            Token op = curr;
            advance();
            ASTNode right = equality();
            node = new BinaryOpNode(node, op, right);
        }
        return node;
    }

    // equality -> comparison ( (EQUAL_EQUAL | NOTEQUAL) comparison )*
    private ASTNode equality() {
        ASTNode node = comparison();
        while (curr.type == TokenType.EQUAL_EQUAL || curr.type == TokenType.NOTEQUAL) {
            Token op = curr;
            advance();
            ASTNode right = comparison();
            node = new BinaryOpNode(node, op, right);
        }
        return node;
    }

    // comparison -> term ( (GREATER | GREATER_EQUAL | LESS | LESS_EQUAL) term )*
    private ASTNode comparison() {
        ASTNode node = term();
        while (curr.type == TokenType.GREATER || curr.type == TokenType.GREATER_EQUAL ||
                curr.type == TokenType.LESS || curr.type == TokenType.LESS_EQUAL) {
            Token op = curr;
            advance();
            ASTNode right = term();
            node = new BinaryOpNode(node, op, right);
        }
        return node;
    }

    // term -> factor ( (PLUS | MINUS) factor )*
    private ASTNode term() {
        ASTNode node = factor();
        while (curr.type == TokenType.PLUS || curr.type == TokenType.MINUS) {
            Token op = curr;
            advance();
            ASTNode right = factor();
            node = new BinaryOpNode(node, op, right);
        }
        return node;
    }

    // factor -> unary ( (MULTIPLY | DIVIDE) unary )*
    private ASTNode factor() {
        ASTNode node = unary();
        while (curr.type == TokenType.MULTIPLY || curr.type == TokenType.DIVIDE) {
            Token op = curr;
            advance();
            ASTNode right = unary();
            node = new BinaryOpNode(node, op, right);
        }
        return node;
    }

    // unary -> (PLUS | MINUS | NOT) unary | primary
    private ASTNode unary() {
        if (curr.type == TokenType.PLUS || curr.type == TokenType.MINUS || curr.type == TokenType.NOT) {
            Token op = curr;
            advance();
            ASTNode operand = unary();
            return new UnaryOpNode(op, operand);
        }
        return primary();
    }

    // primary -> NUMBER | STRING | TRUE | FALSE | IDENTIFIER | LPAREN expression RPAREN [ and method calls/field access ]
    private ASTNode primary() {
        ASTNode node;
        if (curr.type == TokenType.NUMBER) {
            Token token = curr;
            advance();
            node = new NumberNode(token);
        } else if (curr.type == TokenType.STRING) {
            Token token = curr;
            advance();
            node = new StringNode(token);
        } else if (curr.type == TokenType.TRUE) {
            Token token = curr;
            advance();
            node = new BooleanNode(token);
        } else if (curr.type == TokenType.FALSE) {
            Token token = curr;
            advance();
            node = new BooleanNode(token);
        } else if (curr.type == TokenType.IDENTIFIER) {
            Token token = curr;
            advance();
            node = new IdentifierNode(token);
            // Check for function call or object creation.
            if (curr.type == TokenType.LPAREN) {
                Symbol sym = lookup(token.value);
                if (sym == null) {
                    throw new RuntimeException("Semantic error: Identifier '" + token.value + "' is not defined");
                }
                if (sym.type == SymbolType.CLASS) {
                    node = objectCreationWithoutNew(token);
                } else {
                    node = functionCall(token);
                }
            }
            // Handle field accesses or method calls.
            while (curr.type == TokenType.DOT) {
                advance(); // consume '.'
                expect(TokenType.IDENTIFIER, "Expected field or method name after '.'");
                Token nextId = tokens.get(pos - 1);
                if (curr.type == TokenType.LPAREN) {
                    advance(); // consume '('
                    List<ASTNode> arguments = new ArrayList<>();
                    if (curr.type != TokenType.RPAREN) {
                        arguments.add(expression());
                        while (curr.type == TokenType.COMMA) {
                            advance();
                            if (curr.type == TokenType.RPAREN) break;
                            arguments.add(expression());
                        }
                    }
                    expect(TokenType.RPAREN, "Expected ')' after method arguments");
                    node = new MethodCallNode(node, nextId, arguments);
                } else {
                    node = new FieldAccessNode(node, nextId);
                }
            }
        } else if (curr.type == TokenType.LPAREN) {
            advance();
            node = expression();
            expect(TokenType.RPAREN, "Expected ')'");
        } else if (curr.type == TokenType.LBRACKET) {
            // This branch handles list literals.
            node = parseList();
        } else {
            throw new RuntimeException("Unexpected token: " + curr.value);
        }

        // Now support chaining for field accesses, indexing, and slicing.
        while (curr.type == TokenType.DOT || curr.type == TokenType.LBRACKET) {
            if (curr.type == TokenType.DOT) {
                advance(); // consume '.'
                expect(TokenType.IDENTIFIER, "Expected field or method name after '.'");
                Token nextId = tokens.get(pos - 1);
                if (curr.type == TokenType.LPAREN) {
                    advance(); // consume '('
                    List<ASTNode> arguments = new ArrayList<>();
                    if (curr.type != TokenType.RPAREN) {
                        arguments.add(expression());
                        while (curr.type == TokenType.COMMA) {
                            advance();
                            if (curr.type == TokenType.RPAREN) break;
                            arguments.add(expression());
                        }
                    }
                    expect(TokenType.RPAREN, "Expected ')' after method arguments");
                    node = new MethodCallNode(node, nextId, arguments);
                } else {
                    node = new FieldAccessNode(node, nextId);
                }
            } else if (curr.type == TokenType.LBRACKET) {
                advance(); // consume '['
                if (curr.type == TokenType.COLON) {
                    advance(); // consume ':'
                    ASTNode end = null;
                    if (curr.type != TokenType.RBRACKET) {
                        end = expression();
                    }
                    ASTNode step = null;
                    if (curr.type == TokenType.COLON) {
                        advance(); // consume second ':'
                        if (curr.type != TokenType.RBRACKET) {
                            step = expression();
                        }
                    }
                    expect(TokenType.RBRACKET, "Expected ']' after slice expression");
                    node = new SliceNode(node, null, end, step);
                } else {
                    ASTNode start = expression();
                    if (curr.type == TokenType.COLON) {
                        advance(); // consume ':'
                        ASTNode end = null;
                        if (curr.type != TokenType.RBRACKET) {
                            end = expression();
                        }
                        ASTNode step = null;
                        if (curr.type == TokenType.COLON) {
                            advance(); // consume second ':'
                            if (curr.type != TokenType.RBRACKET) {
                                step = expression();
                            }
                        }
                        expect(TokenType.RBRACKET, "Expected ']' after slice expression");
                        node = new SliceNode(node, start, end, step);
                    } else {
                        expect(TokenType.RBRACKET, "Expected ']' after index expression");
                        node = new IndexNode(node, start);
                    }
                }
            }
        }
        return node;
    }
}
