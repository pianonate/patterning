uniform mat4 projection;
uniform mat4 modelview;

attribute vec4 vertex;
attribute vec2 texCoord;

varying vec2 vTexCoord;

void main() {
    vTexCoord = texCoord;
    gl_Position = projection * modelview * vertex;
}
