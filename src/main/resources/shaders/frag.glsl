/*#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D texture;
uniform float alphaModifier; // New uniform variable
varying vec2 vTexCoord;

void main() {
    vec4 color = texture2D(texture, vTexCoord);
    color.a  += alphaModifier;
    gl_FragColor = color;
}*/
#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D texture;
uniform float alphaModifier;
uniform vec3 targetColor;  // Either black (0, 0, 0) or white (1, 1, 1)

varying vec2 vTexCoord;

/*void main() {
    vec4 color = texture2D(texture, vTexCoord);
    color.rgb = mix(color.rgb, targetColor, 0.05);  // Blend towards target color
    color.a += alphaModifier;
    gl_FragColor = color;
}*/
/*
void main() {
    vec4 color = texture2D(texture, vTexCoord);
    color.rgb = mix(color.rgb, targetColor, 0.05);

    if (length(color.rgb - targetColor) < 0.01) {
        color.rgb = targetColor;
    }

    color.a += alphaModifier;

    if (color.a < 0.01) {
        color.a = 0.0;
        color.rgb = targetColor;
    }

    // Clamping
    color.a = clamp(color.a, 0.0, 1.0);
    gl_FragColor = color;
}
*/
/*void main() {
    vec4 color = texture2D(texture, vTexCoord);

    // Directly set left quarter to target
    if (vTexCoord.x < 0.25) {
        gl_FragColor = vec4(targetColor, 1.0);
        return;
    }

    color.rgb = mix(color.rgb, targetColor, 0.05);
    color.a += alphaModifier;

    if (color.a >= 1.0 && length(color.rgb - targetColor) < 0.1) {
        color.rgb = targetColor;
        color.a = 1.0;
    }

    gl_FragColor = color;
}*/
void main() {
    vec4 color = texture2D(texture, vTexCoord);

    // Fading logic
    color.rgb = mix(color.rgb, targetColor, 0.1);
    color.a += alphaModifier;

    // Snap to target if close enough
    if (length(color.rgb - targetColor) < 0.1) {
        color.rgb = targetColor;
    }

    // For the left quarter of the screen, directly set to target color
/*    if (vTexCoord.x < 0.15) {
        gl_FragColor = vec4(targetColor, 1.0);
        return;
    }*/

    color = vec4(color.r, color.g, color.b, color.a);

    gl_FragColor = color;
}
















