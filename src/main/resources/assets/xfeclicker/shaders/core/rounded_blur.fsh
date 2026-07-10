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

uniform sampler2D Sampler0; // kawase-blurred scene
uniform sampler2D Sampler1; // grayscale water grid (dark lines, light fills)

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float sdRoundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    // Full quad size in pixels (panel + shadow padding).
    vec2 quadPx = max(abs(1.0 / fwidth(texCoord0)), vec2(1.0));
    float radiusPx = max(vertexColor.r * 255.0, 0.0);
    float shadowPadPx = max(vertexColor.g * 255.0, 0.0);
    float panelAlpha = vertexColor.a;

    vec2 localPx = (texCoord0 - 0.5) * quadPx;
    vec2 halfQuad = quadPx * 0.5;
    vec2 halfPanel = max(halfQuad - vec2(shadowPadPx), vec2(1.0));
    float panelRadius = min(radiusPx, min(halfPanel.x, halfPanel.y));

    float dist = sdRoundedBox(localPx, halfPanel, panelRadius);
    float aa = max(fwidth(dist) * 0.75, 0.001);
    float panelMask = 1.0 - smoothstep(-aa, aa, dist);

    // Soft outer shadow around the panel border.
    float shadow = (1.0 - panelMask) * (1.0 - smoothstep(0.0, shadowPadPx, dist));
    shadow *= shadow * panelAlpha * 0.55;

    if (panelMask <= 0.001 && shadow <= 0.001) {
        discard;
    }

    vec3 color = vec3(0.0);
    float outAlpha = shadow;

    if (panelMask > 0.001) {
        vec2 blurUv = gl_FragCoord.xy / ScreenSize;
        vec3 sampled = texture(Sampler0, blurUv).rgb;

        // Panel-local UVs for water (exclude shadow padding).
        vec2 panelUv = (localPx + halfPanel) / (2.0 * halfPanel);
        panelUv = clamp(panelUv, 0.0, 1.0);

        float t = GameTime * 500.0;
        vec2 flow = vec2(sin(t * 0.35) * 0.012, cos(t * 0.28) * 0.010);
        vec2 warp = vec2(
            sin(panelUv.y * 6.28318 + t * 0.4) * 0.015,
            cos(panelUv.x * 6.28318 + t * 0.35) * 0.015
        );
        vec2 waterUv = clamp(panelUv + flow + warp, 0.0, 1.0);
        float field = texture(Sampler1, waterUv).a;

        float shade = mix(0.94, 1.04, field);
        vec3 withWater = sampled * shade;
        color = mix(withWater, sampled, 0.55);
        outAlpha = max(shadow, panelMask * panelAlpha);
    }

    fragColor = vec4(color, outAlpha);
}
