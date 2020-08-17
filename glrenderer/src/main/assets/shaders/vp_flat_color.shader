uniform mat4 u_ModelViewProjectionMatrix;

attribute vec4 a_Position;


void main()
{
	gl_PointSize = 3.0;
	gl_Position = u_ModelViewProjectionMatrix * a_Position;
}
