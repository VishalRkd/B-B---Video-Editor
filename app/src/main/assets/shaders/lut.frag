#extension GL_OES_EGL_image_external : require
precision medium float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform float uLut;
uniform sampler2D uLutTexture;
void main() {
    vec4 color = texture2D(sTexture, vTextureCoord);
    vec3 rgb = color.rgb;
    
    float blueColor = rgb.b * 63.0;
    
    vec2 quad1;
    quad1.y = floor(floor(blueColor) / 8.0);
    quad1.x = floor(blueColor) - (quad1.y * 8.0);
    
    vec2 quad2;
    quad2.y = floor(ceil(blueColor) / 8.0);
    quad2.x = ceil(blueColor) - (quad2.y * 8.0);
    
    vec2 texPos1;
    texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * rgb.r);
    texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * rgb.g);
    
    vec2 texPos2;
    texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * rgb.r);
    texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * rgb.g);
    
    vec3 newColor1 = texture2D(uLutTexture, texPos1).rgb;
    vec3 newColor2 = texture2D(uLutTexture, texPos2).rgb;
    
    vec3 lutColor = mix(newColor1, newColor2, fract(blueColor));
    gl_FragColor = vec4(mix(rgb, lutColor, uLut), color.a);
}
