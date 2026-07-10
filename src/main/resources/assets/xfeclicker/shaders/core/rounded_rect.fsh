#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float sdRoundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    // Capsule-friendly radius: half the shorter axis in local UV space.
    vec2 panelPx = max(abs(1.0 / fwidth(texCoord0)), vec2(1.0));
    float aspect = panelPx.x / panelPx.y;
    float radius = min(aspect, 1.0) * 0.5;

    vec2 p = (texCoord0 - 0.5) * vec2(aspect, 1.0);
    vec2 halfSize = vec2(aspect, 1.0) * 0.5;
    float dist = sdRoundedBox(p, halfSize, radius);

    float aa = max(fwidth(dist) * 0.75, 0.001);
    float mask = 1.0 - smoothstep(-aa, aa, dist);
    if (mask <= 0.001) {
        discard;
    }

    vec4 color = vertexColor * ColorModulator;
    fragColor = vec4(color.rgb, color.a * mask);
}
