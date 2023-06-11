package patterning

import processing.core.PApplet
import java.nio.IntBuffer
import java.util.*
import java.util.regex.Pattern

class RLEParser internal constructor(text: String) {
    val METADATA_PREFIX = "#"
    val MIN_BUFFER_SIZE = 64
    val HEADER_PATTERN = "^x = \\d+, y = \\d+, rule.*$"
    val result: LifeForm

    init {
        result = LifeForm()
        handleMetaData(text)
        handleHeader(text)
        parseInstructions()
    }

    private fun increaseBufSize(original: IntBuffer): IntBuffer {
        val newLength = (original.capacity() * 1.5).toInt()
        val newBuffer = IntBuffer.allocate(newLength)
        original.rewind()
        newBuffer.put(original)
        return newBuffer
    }

    @Throws(NotLifeException::class)
    fun parseInstructions() {
        val instructions = result.instructions
        if (instructions.length == 0) {
            throw NotLifeException("no life was found in the details of the RLE")
        }
        var initialSize = MIN_BUFFER_SIZE
        if (result.width > 0 && result.height > 0) {
            val size = result.width * result.height
            if (size > 0) {
                val DENSITY_ESTIMATE = 1.5f
                initialSize = Math.max(initialSize.toFloat(), size * DENSITY_ESTIMATE).toInt()
                initialSize = Math.min(MAX_BUFFER_SIZE, initialSize)
            }
        }
        var count = 1
        var x = 0
        var y = 0
        var aliveCount = 0
        val len = instructions.length
        var inNumber = false
        var chr: Char
        var fieldX = IntBuffer.allocate(initialSize)
        var fieldY = IntBuffer.allocate(initialSize)
        for (pos in 0 until len) {
            chr = instructions[pos]
            if (chr >= '0' && chr <= '9') {
                // stay in a number until you're not in a number anymore
                // once you leave the number you'll either be adding dead, alive or rows (y is the height - counts the rows)
                if (inNumber) {
                    // every position in a number multiplies its place by 10 - clever
                    count *= 10
                    count += chr.code - '0'.code
                } else {
                    count = chr.code - '0'.code
                    inNumber = true
                }
            } else {
                if (chr == 'b') {
                    x += count
                } else if (chr >= 'A' && chr <= 'Z' || chr >= 'a' && chr < 'z') {
                    if (aliveCount + count > fieldX.capacity()) {
                        fieldX = increaseBufSize(fieldX)
                        fieldY = increaseBufSize(fieldY)
                    }
                    while (count-- > 0) {
                        fieldX.put(aliveCount, x++)
                        fieldY.put(aliveCount, y)
                        aliveCount++
                    }
                } else if (chr == '$') {
                    // skipping rows
                    y += count
                    x = 0
                } else if (chr == '!') {
                    break
                }
                count = 1
                inNumber = false
            }
        }
        result.field_x = fieldX.slice(0, aliveCount)
        result.field_y = fieldY.slice(0, aliveCount)
    }

    @Throws(NotLifeException::class)
    fun handleHeader(text: String) {
        val compiledPattern = Pattern.compile(HEADER_PATTERN, Pattern.MULTILINE)
        val matcher = compiledPattern.matcher(text)
        val line: String
        if (matcher.find()) {
            line = matcher.group()
            result.instructions = text.substring(matcher.end())
        } else {
            throw NotLifeException("can't find the header line")
        }
        val header = PApplet.split(line, ", ")
        for (headerLine in header) {
            val components = PApplet.split(headerLine, ' ')
            val component = components[0]
            val value = components[2]
            when (component) {
                "x" -> result.width = PApplet.parseInt(value)
                "y" -> result.height = PApplet.parseInt(value)
                "rule" -> {
                    // parseRuleRLE ensures that the if there is a prefix of B or S then
                    // it puts them in the correct order each time and walks through parse to get the value
                    // that's what the true and false are for here
                    result.rule_s = parseRuleRLE(value, true)
                    result.rule_b = parseRuleRLE(value, false)

                    // add the rule used to the comments list and also to the result object
                    // in a consistent manner
                    val readable = rule2str(result.rule_s, result.rule_b)
                    result.comments.add("\nRule: $readable\n")
                    result.rule = readable
                }

                else ->  // if we got here we don't have something we know about
                    throw NotLifeException("invalid header: $line")
            }
        }
    }

    @Throws(NotLifeException::class)
    fun parseRuleRLE(ruleStr: String, survived: Boolean): Int {
        val rule = ruleStr.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (rule.size < 2 || rule[1].isEmpty()) {
            throw NotLifeException("invalid rule: $ruleStr")
        }
        if (isNumber(rule[0])) {
            return parseRule(java.lang.String.join("/", *rule), survived)
        }
        if (rule[0][0].lowercaseChar() == 'b') {
            Collections.reverse(Arrays.asList(*rule))
        }
        val parsedRuleStr = rule[0].substring(1) + "/" + rule[1].substring(1)
        return parseRule(parsedRuleStr, survived)
    }

    /* Why all the nonsense?  because the patterning.LifeUniverse uses the rules to form the eval bitmask
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
    @Throws(NotLifeException::class)
    fun parseRule(ruleStr: String, survived: Boolean): Int {
        var rule = 0
        val parsed = ruleStr.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[if (survived) 0 else 1]
        for (i in 0 until parsed.length) {
            val c = parsed[i]
            if (c < '0' || c > '9') {
                throw NotLifeException("not a valid rule - non digits in the rule field!: $ruleStr")
            }
            val n = c.code - '0'.code
            if (rule and (1 shl n) != 0) {
                throw NotLifeException("not a valid rule - you've got duplicates: $parsed")
            }
            rule = rule or (1 shl n)
        }
        return rule
    }

    /*    public String rule2strRle(int ruleS, int ruleB) {
        String rule = rule2str(ruleS, ruleB);
        String[] parts = rule.split("/");
        rule = "B" + parts[1] + "/S" + parts[0];
        return rule;
    }*/
    fun rule2str(ruleS: Int, ruleB: Int): String {
        var ruleS = ruleS
        var ruleB = ruleB
        val rule = StringBuilder()
        run {
            var i = 0
            while (ruleS != 0) {
                if (ruleS and 1 != 0) {
                    rule.append(i)
                }
                ruleS = ruleS shr 1
                i++
            }
        }
        rule.append("/")
        var i = 0
        while (ruleB != 0) {
            if (ruleB and 1 != 0) {
                rule.append(i)
            }
            ruleB = ruleB shr 1
            i++
        }
        return rule.toString()
    }

    fun isNumber(test: String): Boolean {
        return try {
            test.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    @Throws(NotLifeException::class)
    fun handleMetaData(text: String?) {
        val lines = PApplet.split(text, '\n')
        for (line in lines) {
            if (line.startsWith(METADATA_PREFIX) && line.length > 1) {
                val secondChar = line[1]
                val remainder = line.substring(2).trim { it <= ' ' }
                when (secondChar) {
                    'N' -> result.title = remainder
                    'O' -> result.author = remainder
                    'C', 'D' -> result.comments.add(remainder)
                    else -> throw NotLifeException("unknown metadata type: $line")
                }
            }
        }
    }

    companion object {
        private const val MAX_BUFFER_SIZE = 1048576
    }
}