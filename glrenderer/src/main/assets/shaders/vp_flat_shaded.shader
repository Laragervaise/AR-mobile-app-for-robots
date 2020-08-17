uniform mat4 u_MVPMatrix;
uniform vec4 u_Color;
uniform vec3 u_lightVector;
uniform mat3 u_NormMatrix;

attribute vec4 a_Position;
attribute vec3 a_Normal;

varying vec4 v_Color;


void main()
{
	vec3 modelViewNormal = normalize(u_NormMatrix * a_Normal);

	float diffuse = max(dot(modelViewNormal, u_lightVector), 0.4);
	v_Color = vec4(diffuse * u_Color.xyz, u_Color[3]);

	gl_PointSize = 3.0;
	gl_Position = u_MVPMatrix * a_Position;
}
