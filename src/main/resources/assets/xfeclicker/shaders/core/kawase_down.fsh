#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

// Dual-Kawase downsample (one 2x-down pass).
void main() {
    vec2 texel = 1.0 / vec2(textureSize(InSampler, 0));
    vec2 o = texel * 0.5;

    vec4 sum = texture(InSampler, texCoord) * 4.0;
    sum += texture(InSampler, texCoord + vec2(-o.x, -o.y));
    sum += texture(InSampler, texCoord + vec2( o.x, -o.y));
    sum += texture(InSampler, texCoord + vec2(-o.x,  o.y));
    sum += texture(InSampler, texCoord + vec2( o.x,  o.y));

    fragColor = sum / 8.0;
}
