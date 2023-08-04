package patterning.life

// contains the results of a parsed lifeform
// currently only RLE formats are parsed - either pasted in or loaded from a prior
// exit/load of the app - a long way to go here...
class LifeForm {
    var width = 0
    var height = 0
    var rulesS = 0
    var ruleB = 0
    var title = ""
    var author = ""
    var rule = ""
    val comments: ArrayList<String> = ArrayList()
    var instructions = ""
    var fieldX: ArrayList<Int> = ArrayList()
    var fieldY: ArrayList<Int> = ArrayList()
}