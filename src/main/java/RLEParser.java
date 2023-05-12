import processing.core.PApplet;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.IntBuffer;

public class RLEParser {
    final String METADATA_PREFIX = "#";

    final int MIN_BUFFER_SIZE = 64;

    final String HEADER_PATTERN = "^x = \\d+, y = \\d+, rule.*$";
    private  static final int MAX_BUFFER_SIZE = 1048576;

    private final LifeForm lifeForm;

    RLEParser(String text) throws NotLifeException {
        lifeForm = new LifeForm();

        handleMetaData(text);
        handleHeader(text);
        parseInstructions();
    }

    private  IntBuffer increaseBufSize(IntBuffer original) {
        int newLength = (int) (original.capacity() * 1.5);
        IntBuffer newBuffer = IntBuffer.allocate(newLength);
        original.rewind();
        newBuffer.put(original);
        return newBuffer;
    }

    void parseInstructions() throws NotLifeException {

        String instructions = lifeForm.instructions;
        if (instructions.length()==0) {
            throw new NotLifeException("no life was found in the details of the RLE");
        }

        int initialSize = MIN_BUFFER_SIZE;

        if (lifeForm.width > 0 && lifeForm.height > 0) {
            int size = lifeForm.width * lifeForm.height;

            if (size > 0) {
                float DENSITY_ESTIMATE = 1.5f;
                initialSize = (int) Math.max(initialSize, size * DENSITY_ESTIMATE);
                initialSize = Math.min(MAX_BUFFER_SIZE, initialSize);
            }
        }

        int count = 1, x = 0, y = 0, aliveCount = 0, len = instructions.length();
        boolean inNumber = false;
        char chr;
        IntBuffer fieldX = IntBuffer.allocate(initialSize);
        IntBuffer fieldY = IntBuffer.allocate(initialSize);

        for (int pos = 0; pos < len; pos++) {
            chr = instructions.charAt(pos);

            if (chr >= '0' && chr <= '9') {
                // stay in a number until you're not in a number anymore
                // once you leave the number you'll either be adding dead, alive or rows (y is the height - counts the rows)
                if (inNumber) {
                    // every position in a number multiplies it's place by 10 - clever
                    count *= 10;
                    count += chr - '0';
                } else {
                    count = chr - '0';
                    inNumber = true;
                }
            } else {
                if (chr == 'b') {
                    x += count;
                } else if ((chr >= 'A' && chr <= 'Z') || (chr >= 'a' && chr < 'z')) {
                    if (aliveCount + count > fieldX.capacity()) {
                        fieldX = increaseBufSize(fieldX);
                        fieldY = increaseBufSize(fieldY);
                    }

                    while (count-- > 0) {
                        fieldX.put(aliveCount, x++);
                        fieldY.put(aliveCount, y);
                        aliveCount++;
                    }
                } else if (chr == '$') {
                    // skipping rows
                    y += count;
                    x = 0;
                } else if (chr == '!') {
                    break;
                }

                count = 1;
                inNumber = false;
            }
        }

        lifeForm.field_x= fieldX.slice(0, aliveCount);
        lifeForm.field_y = fieldY.slice(0, aliveCount);
    }

    void handleHeader(String text) throws NotLifeException {

        Pattern compiledPattern = Pattern.compile(HEADER_PATTERN, Pattern.MULTILINE);
        Matcher matcher = compiledPattern.matcher(text);

        String line;
        if (matcher.find()) {
            line = matcher.group();
            lifeForm.instructions = text.substring(matcher.end());
        } else {
            throw new NotLifeException("can't find the header line");
        }

        String[] header = PApplet.split(line, ", ");

        for (String headerLine : header) {

            String[] components = PApplet.split(headerLine, ' ');

            String component = components[0];
            String value = components[2];

            switch (component) {
                case "x" -> lifeForm.width = PApplet.parseInt(value);
                case "y" -> lifeForm.height = PApplet.parseInt(value);
                case "rule" -> {
                    // parseRuleRLE ensures that the if there is a prefix of B or S then
                    // it puts them in the correct order each time and walks through parse to get the value
                    // that's what the true and false are for here
                    lifeForm.rule_s = parseRuleRLE(value, true);
                    lifeForm.rule_b = parseRuleRLE(value, false);

                    // add the rule used to the comments list and also to the result object
                    // in a consistent manner
                    String readable = rule2str(lifeForm.rule_s, lifeForm.rule_b);
                    lifeForm.comments.add("\nRule: " + readable + "\n");
                    lifeForm.rule = readable;
                }
                default ->
                    // if we got here we don't have something we know about
                        throw new NotLifeException("invalid header: " + line);
            }
        }
    }

    int parseRuleRLE(String ruleStr, boolean survived) throws NotLifeException {
        String[] rule = ruleStr.split("/");

        if (rule.length < 2 || rule[1].isEmpty()) {
            throw new NotLifeException("invalid rule: " + ruleStr);
        }

        if (isNumber(rule[0])) {
            return parseRule(String.join("/", rule), survived);
        }

        if (Character.toLowerCase(rule[0].charAt(0)) == 'b') {
            Collections.reverse(Arrays.asList(rule));
        }

        String parsedRuleStr = rule[0].substring(1) + "/" + rule[1].substring(1);
        return parseRule(parsedRuleStr, survived);
    }

  /* Why all the nonsense?  because the LifeUniverse uses the rules to form the eval bitmask
   necessary to calculate neighbors and aliveness. The rule string parked on the rule field
   is what comes out of the RLE file so just look at that if you want to debug
   
   Inside the function, the ruleStr string is split on the "/" character using the split() method,
   and the appropriate part of the resulting array is selected based on the value of the survived parameter.
   
   The function then loops through the characters of the parsed string,
   checks if each character is a digit, and parses it as an integer.
   If a non-digit character is encountered, the function returns -1 to indicate an error.
   
   The function then checks if the parsed integer has already been seen
   by using a bitwise AND operation with the rule variable. If the parsed
   integer has already been seen, the function throws an exception to indicate an error.
   
   Finally, the function updates the rule variable by setting the corresponding
   bit using a bitwise OR operation, and returns the resulting value of the rule variable.
   If all characters in the parsed string have been successfully parsed and no duplicates
   have been found, the rule variable contains a bit mask
   representing the unique integers in the string.*/

    public int parseRule(String ruleStr, boolean survived) throws NotLifeException {

        int rule = 0;
        String parsed = ruleStr.split("/")[survived ? 0 : 1];

        for (int i = 0; i < parsed.length(); i++) {
            char c = parsed.charAt(i);

            if (c < '0' || c > '9') {
                throw new NotLifeException("not a valid rule - non digits in the rule field!: " + ruleStr);
            }

            int n = c - '0';

            if ((rule & (1 << n)) != 0) {
                throw new NotLifeException("not a valid rule - you've got duplicates: " + parsed);
            }

            rule |= 1 << n;
        }

        return rule;
    }

    public String rule2strRle(int ruleS, int ruleB) {
        String rule = rule2str(ruleS, ruleB);
        String[] parts = rule.split("/");
        rule = "B" + parts[1] + "/S" + parts[0];
        return rule;
    }

    public String rule2str(int ruleS, int ruleB) {
        StringBuilder rule = new StringBuilder();

        for (int i = 0; ruleS != 0; ruleS >>= 1, i++) {
            if ((ruleS & 1) != 0) {
                rule.append(i);
            }
        }

        rule.append("/");

        for (int i = 0; ruleB != 0; ruleB >>= 1, i++) {
            if ((ruleB & 1) != 0) {
                rule.append(i);
            }
        }

        return rule.toString();
    }

    boolean isNumber(String test) {
        try {
            Integer.parseInt(test);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    void handleMetaData(String text) throws NotLifeException {
        String[] lines = PApplet.split(text, '\n');

        for (String line : lines) {
            if (line.startsWith(METADATA_PREFIX) && line.length()>1) {

                char secondChar = line.charAt(1);
                String remainder = line.substring(2).trim();

                switch (secondChar) {
                    case 'N' -> lifeForm.title = remainder;
                    case 'O' -> lifeForm.author = remainder;
                    case 'C', 'D' -> lifeForm.comments.add(remainder);
                    default -> throw new NotLifeException("unknown metadata type: " + line);
                }
            }
        }
    }

    LifeForm getResult() {
        return lifeForm;
    }
}