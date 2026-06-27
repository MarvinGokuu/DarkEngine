#version 430 core
layout (location = 0) out vec4 gAlbedo;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gPBR;

in vec3 FragPos;
in vec2 TexCoords;
in vec3 Normal;
in mat3 TBN;

uniform sampler2D texture_diffuse1;
uniform sampler2D texture_normal1;
uniform sampler2D texture_pbr1; // (Roughness, Metallic, AO)
uniform vec4 colorMultiplier;

void main() {
    // 1. Albedo
    gAlbedo = texture(texture_diffuse1, TexCoords) * colorMultiplier;
    
    // 2. Normal (Transform normal map from tangent space to world space)
    vec3 normalMap = texture(texture_normal1, TexCoords).rgb;
    // Map normal from [0, 1] to [-1, 1]
    normalMap = normalize(normalMap * 2.0 - 1.0);
    vec3 N = normalize(TBN * normalMap);
    gNormal = vec4(N, 1.0);
    
    // 3. PBR data (R=Roughness, G=Metallic, B=AO)
    vec4 pbrData = texture(texture_pbr1, TexCoords);
    gPBR = pbrData;
}
