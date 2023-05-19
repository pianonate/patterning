import java.nio.IntBuffer;
import java.util.ArrayList;


// contains the results of a parsed lifeform
// currently only RLE formats are parsed - either pasted in or loaded from a prior
// exit/load of the app - a long way to go here...
public class LifeForm {

    int width;
    int height;
    int rule_s;
    int rule_b;
    String title;
    String author;
    String rule;
    final ArrayList<String> comments;
    String instructions;
    IntBuffer field_x;
    IntBuffer field_y;

    LifeForm() {
        width=0;
        height=0;
        rule_s=0;
        rule_b=0;
        rule="";
        title="";
        author="";
        comments = new ArrayList<>();
        instructions = "";
    }
}