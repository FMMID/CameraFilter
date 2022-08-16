#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES uTextureSampler;
varying vec2 vTextureCoord;
void main()
{
  vec4 vCameraColor = texture2D(uTextureSampler, vTextureCoord);
  float threshold = 0.5;
  float mean = (vCameraColor.r + vCameraColor.g + vCameraColor.b) / 3.0;
  vCameraColor.r = vCameraColor.g = vCameraColor.b = mean >= threshold ? 1.0 : 0.0;
  gl_FragColor = vec4(vCameraColor.r, vCameraColor.g, vCameraColor.b, 1.0);
}