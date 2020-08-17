uniform mat4 u_ModelViewProjectionMatrix;
uniform mat4 u_ModelMatrix;

attribute vec4 a_Position;
attribute vec3 a_Normal;

varying vec4 v_WorldPosition;   // Position in world space.
varying vec4 v_WorldNormal;     // Surface normal in world space


void main()
{
    gl_Position = u_ModelViewProjectionMatrix * a_Position;
 
    v_WorldPosition = u_ModelMatrix * a_Position;
    v_WorldNormal = u_ModelMatrix * vec4(a_Normal, 0);
}
