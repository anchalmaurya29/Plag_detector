package com.plagiarism.detector.util;

import java.util.*;
import java.util.regex.*;

public class CodeTokenizer {

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract","assert","boolean","break","byte","case","catch","char","class",
            "const","continue","default","do","double","else","enum","extends","final",
            "finally","float","for","goto","if","implements","import","instanceof","int",
            "interface","long","native","new","null","package","private","protected",
            "public","return","short","static","strictfp","super","switch","synchronized",
            "this","throw","throws","transient","try","void","volatile","while","true","false"
    );

    public enum TokenType {
        KEYWORD, IDENTIFIER, LITERAL_STRING, LITERAL_NUMBER,
        OPERATOR, DELIMITER, COMMENT, WHITESPACE, UNKNOWN
    }

    public record Token(TokenType type, String value, int line) {}

    public List<Token> tokenize(String sourceCode) {
        String stripped = stripComments(sourceCode);
        List<Token> raw = lex(stripped);
        return normalize(raw);
    }

    private String stripComments(String code) {
        code = code.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
        code = code.replaceAll("//[^\n]*", " ");
        return code;
    }

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\"[^\"]*\""
                    + "|'[^']*'"
                    + "|\\d+\\.?\\d*"
                    + "|[a-zA-Z_][a-zA-Z0-9_]*"
                    + "|[+\\-*/%=<>!&|^~?:]{1,2}"
                    + "|[(){};,\\.\\[\\]]"
                    + "|\\s+"
    );

    private List<Token> lex(String code) {
        List<Token> tokens = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(code);
        int line = 1;
        while (m.find()) {
            String v = m.group();
            long newlines = v.chars().filter(c -> c == '\n').count();
            if (v.isBlank()) { line += (int) newlines; continue; }
            TokenType type = classify(v);
            tokens.add(new Token(type, v, line));
            line += (int) newlines;
        }
        return tokens;
    }

    private TokenType classify(String v) {
        if (JAVA_KEYWORDS.contains(v))              return TokenType.KEYWORD;
        if (v.matches("[a-zA-Z_][a-zA-Z0-9_]*"))   return TokenType.IDENTIFIER;
        if (v.matches("\".*\"|'.*'"))               return TokenType.LITERAL_STRING;
        if (v.matches("\\d+\\.?\\d*"))              return TokenType.LITERAL_NUMBER;
        if (v.matches("[(){};,\\.\\[\\]]"))         return TokenType.DELIMITER;
        return TokenType.OPERATOR;
    }

    private List<Token> normalize(List<Token> tokens) {
        Map<String, String> identifierMap = new LinkedHashMap<>();
        List<Token> normalised = new ArrayList<>();
        for (Token t : tokens) {
            switch (t.type()) {
                case IDENTIFIER -> {
                    String canonical = identifierMap.computeIfAbsent(
                            t.value(), k -> "VAR_" + identifierMap.size()
                    );
                    normalised.add(new Token(TokenType.IDENTIFIER, canonical, t.line()));
                }
                case LITERAL_STRING ->
                        normalised.add(new Token(TokenType.LITERAL_STRING, "STR_LIT", t.line()));
                case LITERAL_NUMBER ->
                        normalised.add(new Token(TokenType.LITERAL_NUMBER, "NUM_LIT", t.line()));
                default -> normalised.add(t);
            }
        }
        return normalised;
    }

    public List<String> tokenValues(String code) {
        return tokenize(code).stream().map(Token::value).toList();
    }
}