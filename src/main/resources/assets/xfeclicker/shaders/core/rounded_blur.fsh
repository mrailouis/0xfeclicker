#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

// Signed distance to a rounded box in local UV space (0..1).
float sdRoundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    // Approximate panel size in pixels from UV derivatives, then keep radius ~12px.
    vec2 panelPx = max(abs(1.0 / fwidth(texCoord0)), vec2(1.0));
    float radius = 12.0 / min(panelPx.x, panelPx.y);

    // Aspect-correct distance so corners stay circular on wide/tall panels.
    float aspect = panelPx.x / panelPx.y;
    vec2 p = (texCoord0 - 0.5) * vec2(aspect, 1.0);
    vec2 halfSize = vec2(aspect, 1.0) * 0.5;
    float dist = sdRoundedBox(p, halfSize, radius * aspect);

    float aa = max(fwidth(dist) * 0.75, 0.001);
    float mask = 1.0 - smoothstep(-aa, aa, dist);
    if (mask <= 0.001) {
        discard;
    }

    // Sample the fullscreen Kawase result at this fragment's screen position.
    vec2 blurUv = gl_FragCoord.xy / ScreenSize;
    vec4 blurred = texture(Sampler0, blurUv);
    vec4 tint = vertexColor * ColorModulator;

    // Frosted glass: blurred backdrop + translucent tint.
    vec3 rgb = mix(blurred.rgb, tint.rgb, tint.a);
    fragColor = vec4(rgb, mask);
}
