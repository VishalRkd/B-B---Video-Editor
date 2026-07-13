#extension GL_OES_EGL_image_external : require
precision medium float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform float uVintage;
void main() {
    vec4 color = texture2D(sTexture, vTextureCoord);
    vec3 rgb = color.rgb;
    vec3 sepia = vec3(
        dot(rgb, vec3(0.393, 0.769, 0.189)),
        dot(rgb, vec3(0.349, 0.686, 0.168)),
        dot(rgb, vec3(0.272, 0.534, 0.131))
    );
    gl_FragColor = vec4(mix(rgb, sepia, uVintage), color.a);
}
