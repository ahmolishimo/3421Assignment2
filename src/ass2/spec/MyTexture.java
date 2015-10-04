package ass2.spec;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class MyTexture {

    private boolean mipMapEnabled = true;
    private int[] textureID = new int[1];
    
    //Create a texture from a file. Make sure the file has a width and height
    //that is a power of 2
    public MyTexture(GL2 gl, String fileName, String extension, boolean mipmaps) {
        mipMapEnabled = mipmaps;
        TextureData data = null;
        try {
             File file = new File(fileName);
             BufferedImage img = ImageIO.read(file); // read file into BufferedImage
             ImageUtil.flipImageVertically(img);
            //This library will result in different formats being upside down.
            //data = TextureIO.newTextureData(GLProfile.getDefault(), file, false,extension);
             
            //This library call flips all images the same way
            data = AWTTextureIO.newTextureData(GLProfile.getDefault(), img, false);
        } catch (IOException exc) {
            System.err.println(fileName);
            exc.printStackTrace();
        }
        gl.glGenTextures(1, textureID, 0);
        //The first time bind is called with the given id,
        //an openGL texture object is created and bound to the id
        //It also makes it the current texture.
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureID[0]);
        // Build texture initialised with image data.
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0,
                        data.getInternalFormat(),
                        data.getWidth(),
                        data.getHeight(),
                        0,
                        data.getPixelFormat(),
                        data.getPixelType(),
                        data.getBuffer());
        setFilters(gl);
    }
    
    private void setFilters(GL2 gl){
        // Build the texture from data.
        if (mipMapEnabled) {
            // Set texture parameters to enable automatic mipmap generation and bilinear/trilinear filtering
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
            //float fLargest[] = new float[1];
            //gl.glGetFloatv(GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, fLargest,0);
            //System.out.println(fLargest[0]);
            //gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, fLargest[0]);
            gl.glGenerateMipmap(GL2.GL_TEXTURE_2D);             
        } else {
            // Set texture parameters to enable bilinear filtering.
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);                            
        }
    }
    
    public MyTexture(GL2 gl, ByteBuffer buffer, int size,boolean mipmaps) {
        mipMapEnabled = mipmaps;
        gl.glGenTextures(1, textureID, 0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureID[0]);
        // Specify image data for currently active texture object.
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, size, size, 0, 
                GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE,buffer);
        setFilters(gl);
    }
    
    public MyTexture(GL2 gl, String textureFileName1, String textureExt1) {
        // TODO Auto-generated constructor stub
    }

    public MyTexture(GL2 gl, ByteBuffer chessImageBuf, int imageSize) {
        // TODO Auto-generated constructor stub
    }

    public int getTextureId() {
        return textureID[0];
    }
    
    public void release(GL2 gl) {
        if (textureID[0] > 0) {
            gl.glDeleteTextures(1, textureID, 0);
        }
    }
}
    