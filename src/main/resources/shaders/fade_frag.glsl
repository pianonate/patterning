#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D texture;
varying vec2 vTexCoord;

void main() {
    vec4 color = texture2D(texture, vTexCoord);
    color.a *= 0.99;  // Reduce the alpha by 1%
    gl_FragColor = color;
}
