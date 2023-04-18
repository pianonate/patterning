import processing.core.PApplet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.IntBuffer;



class Formats {

    Result parseRLE(String text) throws NotLifeException {
        return new RLEParser(text).getResult();
    }
}

public class RLEParser {
    final String METADATA_PREFIX = "#";
    final String HEADER_PATTERN = "^x = \\d+, y = \\d+, rule.*$";
    private final int MIN_BUFFER_SIZE = 64;
    private  static final int MAX_BUFFER_SIZE = 1048576;
    private final float DENSITY_ESTIMATE = 1.5f;

    private Result result;

    RLEParser(String text) throws NotLifeException {
        result = new Result();

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

        String instructions = result.instructions;
        if (instructions.length()==0) {
            throw new NotLifeException("no life was found in the details of the RLE");
        }

        int initialSize = MIN_BUFFER_SIZE;

        if (result.width > 0 && result.height > 0) {
            int size = result.width * result.height;

            if (size > 0) {
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

        result.field_x= fieldX.slice(0, aliveCount);
        result.field_y = fieldY.slice(0, aliveCount);
    }

    void handleHeader(String text) throws NotLifeException {

        Pattern compiledPattern = Pattern.compile(HEADER_PATTERN, Pattern.MULTILINE);
        Matcher matcher = compiledPattern.matcher(text);

        String line = "";
        if (matcher.find()) {
            line = matcher.group();
            result.instructions = text.substring(matcher.end());
        } else {
            throw new NotLifeException("can't find the header line");
        }

        String header[] = PApplet.split(line, ", ");

        for (String headerLine : header) {

            String components[] = PApplet.split(headerLine, ' ');

            String component = components[0];
            String value = components[2];

            switch (component) {
                case "x":
                    result.width = PApplet.parseInt(value);
                    break;

                case "y":
                    result.height = PApplet.parseInt(value);
                    break;

                case "rule":
                    // parseRuleRLE ensures that the if there is a prefix of B or S then
                    // it puts them in the correct order each time and walks through parse to get the value
                    // that's what the true and false are for here
                    result.rule_s = parseRuleRLE(value, true);
                    result.rule_b = parseRuleRLE(value, false);

                    // add the rule used to the comments list and also to the result object
                    // in a consistent manner
                    String readable = rule2str(result.rule_s, result.rule_b);
                    result.comments.add("\nRule: " + readable + "\n");
                    result.rule = readable;
                    break;

                default:
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
        String rule = "";

        for (int i = 0; ruleS != 0; ruleS >>= 1, i++) {
            if ((ruleS & 1) != 0) {
                rule += i;
            }
        }

        rule += "/";

        for (int i = 0; ruleB != 0; ruleB >>= 1, i++) {
            if ((ruleB & 1) != 0) {
                rule += i;
            }
        }

        return rule;
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
        String lines[] = PApplet.split(text, '\n');

        for (String line : lines) {
            if (line.startsWith("#") && line.length()>1) {

                char secondChar = line.charAt(1);
                String remainder="";

                if (line.length() > 1) {
                    remainder = line.substring(2).trim();
                }

                switch (secondChar) {
                    case 'N':
                        result.title = remainder;
                        break;

                    case 'O':
                        result.author = remainder;
                        break;
                    case 'C':
                    case 'D':
                        result.comments.add(remainder);
                        break;

                    default:
                        throw new NotLifeException("unknown metadata type: " + line);
                }
            }
        }
    }

    Result getResult() {
        return result;
    }
}

class Result {

    int width;
    int height;
    int rule_s;
    int rule_b;
    String title;
    String author;
    String rule;
    ArrayList<String> comments;
    String instructions;
    IntBuffer field_x;
    IntBuffer field_y;

    Result() {
        width=0;
        height=0;
        rule_s=0;
        rule_b=0;
        rule="";
        title="";
        author="";
        comments = new ArrayList<String>();
        instructions = "";
    }
}