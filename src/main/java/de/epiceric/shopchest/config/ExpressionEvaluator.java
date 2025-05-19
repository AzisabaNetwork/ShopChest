package de.epiceric.shopchest.config;

public class ExpressionEvaluator {
    public static String evaluateScript(String script) {
        // 数値演算を処理
        if (script.contains("+")) {
            String[] parts = script.split("\\+");
            return String.valueOf(parseValue(parts[0].trim()) + parseValue(parts[1].trim()));
        } else if (script.contains("-")) {
            String[] parts = script.split("-");
            return String.valueOf(parseValue(parts[0].trim()) - parseValue(parts[1].trim()));
        } else if (script.contains("*")) {
            String[] parts = script.split("\\*");
            return String.valueOf(parseValue(parts[0].trim()) * parseValue(parts[1].trim()));
        } else if (script.contains("/")) {
            String[] parts = script.split("/");
            double divisor = parseValue(parts[1].trim());
            if (divisor == 0) return "0";
            return String.valueOf(parseValue(parts[0].trim()) / divisor);
        }
        // 単一の値の場合はそのまま返す
        return script.replaceAll("\"", "");
    }

    public static boolean evaluate(String expression) {
        // 論理演算子を処理
        if (expression.contains("&&")) {
            String[] parts = expression.split("&&");
            return evaluate(parts[0].trim()) && evaluate(parts[1].trim());
        } else if (expression.contains("||")) {
            String[] parts = expression.split("\\|\\|");
            return evaluate(parts[0].trim()) || evaluate(parts[1].trim());
        }

        // 単純な比較演算子を処理
        if (expression.contains(">=")) {
            String[] parts = expression.split(">=");
            return parseValue(parts[0].trim()) >= parseValue(parts[1].trim());
        } else if (expression.contains("<=")) {
            String[] parts = expression.split("<=");
            return parseValue(parts[0].trim()) <= parseValue(parts[1].trim());
        } else if (expression.contains(">")) {
            String[] parts = expression.split(">");
            return parseValue(parts[0].trim()) > parseValue(parts[1].trim());
        } else if (expression.contains("<")) {
            String[] parts = expression.split("<");
            return parseValue(parts[0].trim()) < parseValue(parts[1].trim());
        } else if (expression.contains("==")) {
            String[] parts = expression.split("==");
            return compareValues(parts[0].trim(), parts[1].trim());
        } else if (expression.contains("!=")) {
            String[] parts = expression.split("!=");
            return !compareValues(parts[0].trim(), parts[1].trim());
        }
        return false;
    }

    private static double parseValue(String value) {
        value = value.replaceAll("\"", "");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static boolean compareValues(String val1, String val2) {
        val1 = val1.replaceAll("\"", "");
        val2 = val2.replaceAll("\"", "");

        try {
            double num1 = Double.parseDouble(val1);
            double num2 = Double.parseDouble(val2);
            return num1 == num2;
        } catch (NumberFormatException e) {
            return val1.equals(val2);
        }
    }
}
