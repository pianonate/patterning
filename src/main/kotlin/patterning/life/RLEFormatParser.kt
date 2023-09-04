package patterning.life

import java.util.regex.Pattern

class RLEFormatParser internal constructor(text: String) {
    private val metaDataPrefix = "#"
    private val headerPattern = "^x = \\d+, y = \\d+, rule.*$"
    val result: LifeForm = LifeForm()

    init {
        handleMetaData(text)
        handleHeader(text)
        parseInstructions()

        result.title = if (result.title.isEmpty()) {
            if (result.comments.size == 0)
                "untitled"
            else
                result.comments[0].take(20)
        } else
            result.title
    }

    private fun parseInstructions() {
        val instructions = result.instructions
        if (instructions.isEmpty()) {
            throw NotLifeException("no life was found in the details of the RLE")
        }

        var count = 1
        var x = 0
        var y = 0
        val len = instructions.length
        var inNumber = false
        var chr: Char
        val fieldX = ArrayList<Int>()
        val fieldY = ArrayList<Int>()
        for (pos in 0 until len) {
            chr = instructions[pos]
            if (chr in '0'..'9') {
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
                } else if (chr in 'A'..'Z' || chr in 'a'..'y') {
                    while (count-- > 0) {
                        fieldX.add(x++)
                        fieldY.add(y)
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

        result.fieldX = fieldX
        result.fieldY = fieldY
    }

    private fun handleHeader(text: String) {
        val compiledPattern = Pattern.compile(headerPattern, Pattern.MULTILINE)
        val matcher = compiledPattern.matcher(text)
        val line: String
        if (matcher.find()) {
            line = matcher.group()
            result.instructions = text.substring(matcher.end())
        } else {
            throw NotLifeException("can't find the header line")
        }
        val header = line.split(", ")
        for (headerLine in header) {
            val components = headerLine.split(' ')
            val component = components[0]
            val value = components[2]
            when (component) {
                "x" -> result.width = value.toInt()
                "y" -> result.height = value.toInt()
                "rule" -> {
                    // parseRuleRLE ensures that the if there is a prefix of B or S then
                    // it puts them in the correct order each time and walks through parse to get the value
                    // that's what the true and false are for here
                    result.rulesS = parseRuleRLE(value, true)
                    result.ruleB = parseRuleRLE(value, false)

                    // add the rule used to the comments list and also to the result object
                    // in a consistent manner
                    val readable = rule2str(result.rulesS, result.ruleB)
                    result.comments.add("\nRule: $readable\n")
                    result.rule = readable
                }

                else ->  // if we got here we don't have something we know about
                    throw NotLifeException("invalid header: $line")
            }
        }
    }

    private fun parseRuleRLE(ruleStr: String, survived: Boolean): Int {
        val rule = ruleStr.split("/").filter { it.isNotEmpty() }
        if (rule.size < 2 || rule[1].isEmpty()) {
            throw NotLifeException("invalid rule: $ruleStr")
        }
        if (isNumber(rule[0])) {
            return parseRule(rule.joinToString("/"), survived)
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
    private fun parseRule(ruleStr: String, survived: Boolean): Int {
        var rule = 0
        val parsed = ruleStr.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[if (survived) 0 else 1]
        for (element in parsed) {
            if (element < '0' || element > '9') {
                throw NotLifeException("not a valid rule - non digits in the rule field!: $ruleStr")
            }
            val n = element.code - '0'.code
            if (rule and (1 shl n) != 0) {
                throw NotLifeException("not a valid rule - you've got duplicates: $parsed")
            }
            rule = rule or (1 shl n)
        }
        return rule
    }

    private fun rule2str(ruleS: Int, ruleB: Int): String {
        var _ruleS = ruleS
        var _ruleB = ruleB
        val rule = StringBuilder()
        run {
            var i = 0
            while (_ruleS != 0) {
                if (_ruleS and 1 != 0) {
                    rule.append(i)
                }
                _ruleS = _ruleS shr 1
                i++
            }
        }
        rule.append("/")
        var i = 0
        while (_ruleB != 0) {
            if (_ruleB and 1 != 0) {
                rule.append(i)
            }
            _ruleB = _ruleB shr 1
            i++
        }
        return rule.toString()
    }

    private fun isNumber(test: String): Boolean {
        return test.toIntOrNull() != null

    }

    private fun handleMetaData(text: String) {
        val lines = text.split('\n')
        for (line in lines) {
            if (line.startsWith(metaDataPrefix) && line.length > 1) {
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
}