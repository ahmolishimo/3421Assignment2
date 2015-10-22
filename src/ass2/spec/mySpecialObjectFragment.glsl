#version 120

varying vec3 N;
varying vec4 v;
varying vec2 texCoord;

uniform sampler2D texUnit1;

void main(void) {
	/*ambient calculation*/
	vec4 ambient, globalAmbient;
	ambient = gl_LightSource[0].ambient * gl_FrontMaterial.ambient;
	globalAmbient = gl_LightModel.ambient * gl_FrontMaterial.ambient;
	
	/*diffuse calculation*/
	vec3 normal, lightDir; 
	vec4 diffuse;
	float NdotL;
	normal = normalize(N);
	lightDir = normalize(vec3(gl_LightSource[0].position.xyz - v.xyz));
    NdotL = max(dot(normal, lightDir), 0.0);
	diffuse = NdotL * gl_FrontMaterial.diffuse * gl_LightSource[0].diffuse;
	
	/*specular calculation*/
	vec4 specular = vec4(0.0,0.0,0.0,1);
    float NdotHV;
    float NdotR;
    vec3 dirToView = normalize(vec3(-v));
    vec3 R = normalize(reflect(-lightDir,normal)); 
    vec3 H = normalize(lightDir+dirToView); 
	
	if (NdotL > 0.0) {
		NdotR = max(dot(R,dirToView ),0.0);
		
		//Can use the halfVector instead of the reflection vector if you wish 
		NdotHV = max(dot(normal, H),0.0);
		
		specular = gl_FrontMaterial.specular * gl_LightSource[0].specular * pow(NdotHV,gl_FrontMaterial.shininess);
	    
	}
	
	specular = clamp(specular,0,1);
	//gl_FragColor = vec4(0, 0, 0, 1);
	//gl_FragColor = texture2D(texUnit1, texCoord);
    gl_FragColor = texture2D(texUnit1, texCoord) * (gl_FrontMaterial.emission + globalAmbient + ambient + diffuse) + specular;
}