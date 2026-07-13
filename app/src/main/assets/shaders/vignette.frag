#extension GL_OES_EGL_image_external : require
precision medium float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform float uVignette;
void main() {
    vec4 color = texture2D(sTexture, vTextureCoord);
    vec3 rgb = color.rgb;
    vec2 uv = vTextureCoord - 0.5;
    float dist = length(uv);
    float vfactor = smoothstep(0.8 - uVignette * 0.4, 0.4 - uVignette * 0.2, dist);
    gl_FragColor = vec4(rgb * vfactor, color.a);
}
