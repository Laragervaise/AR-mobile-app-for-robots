precision mediump float;

uniform vec4 u_MaterialEmissive;
uniform vec4 u_MaterialSpecular;
uniform float u_MaterialShininess;

uniform sampler2D u_DiffuseTexture;

uniform vec4 u_AmbientLight;    // Global ambient light contribution

uniform vec4 u_EyePos;          // Eye position in world space
uniform vec4 u_LightPos;        // Light's position in world space
uniform vec4 u_LightColor;      // Light's diffuse and specular contribution

varying vec4 v_WorldPosition;   // Position in world space
varying vec4 v_WorldNormal;     // Surface normal in world space
varying vec2 v_TexCoord;        // Texture coordinates


void main()
{
    // Retrieve the color from the texture
    vec4 Color = texture2D(u_DiffuseTexture, v_TexCoord);

    // Compute the emissive term
    vec4 Emissive = u_MaterialEmissive;

    // Compute the ambient term
    vec4 Ambient = vec4(u_AmbientLight.rgb * (Color.rgb * 0.2), Color.a);

    // Compute the diffuse term
    vec4 N = normalize(v_WorldNormal);
    vec4 L = normalize(u_LightPos - v_WorldPosition);
    float NdotL = max(dot(N, L), 0.0);
    vec4 LightDiffuse = vec4(u_LightColor.rgb * Color.rgb, Color.a);
    vec4 Diffuse = vec4(NdotL * LightDiffuse.rgb, LightDiffuse.a);

    // Compute the specular term.
    vec4 V = normalize(u_EyePos - v_WorldPosition);
    //vec4 H = normalize(L + V);
    vec4 R = reflect(-L, N);
    float RdotV = max(dot(R, V), 0.0);
    vec4 Specular = pow(RdotV, u_MaterialShininess) * u_LightColor * u_MaterialSpecular;

    gl_FragColor = Emissive + Ambient + Diffuse + Specular;
}
