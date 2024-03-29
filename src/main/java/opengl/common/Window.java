package opengl.common;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Window {
    // The window handle
    private long window;
    private int width = 300;
    private int height = 300;
    private static Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
    // Defining mouse:
    static float lastX = 400, lastY = 300;
    static float yaw = -90.0f, pitch = 0.0f;
    static float sensibility = 0.1f;

    public Window() {

    }

    public Window(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void run() throws Exception{
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(this.width, this.height, "Hello World!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() throws Exception {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        String vertexShaderSource = "#version 330 core\n" +
                "layout (location = 0) in vec3 aPos;\n" +
                "layout (location = 1) in vec3 aColor;\n" +
                "layout (location = 2) in vec2 aTexCoord;\n" +
                "uniform mat4 transform;\n" +
                "uniform mat4 view;\n" +
                "uniform mat4 projection;\n" +
                "out vec3 fColor;\n" +
                "out vec2 fTexCoord;\n" +
                "void main()\n" +
                "{\n" +
                "   gl_Position = projection * view *  transform * vec4(aPos, 1.0);\n" +
                "   fColor = aColor;\n" +
                "   fTexCoord = aTexCoord;\n" +
                "}\0";

        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexShaderSource);
        GL20.glCompileShader(vertexShader);

        if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling Shader code: " + GL20.glGetShaderInfoLog(vertexShader, 1024));
        }

        // Shader program 2
        // Fragment shader
        String fragmentShaderSource2 = "#version 330\n" +
                "\n" +
                "out vec4 fragColor;\n" +
                "in vec3 fColor;\n" +
                "in vec2 fTexCoord;\n" +
                "uniform vec3 changeValue;\n" +
                "uniform sampler2D texture;\n" +
                "uniform sampler2D background;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    fragColor = mix(texture(texture, fTexCoord), texture(background, fTexCoord), 0.3)  * vec4(fColor.x, fColor.y + changeValue.z, fColor.z + changeValue.z, 1.0);\n" +
                "}";

        int fragmentShader2 = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader2, fragmentShaderSource2);
        GL20.glCompileShader(fragmentShader2);
        int shader2 = GL20.glCreateProgram();
        GL20.glAttachShader(shader2, vertexShader);
        GL20.glAttachShader(shader2, fragmentShader2);
        GL20.glLinkProgram(shader2);

        // Clean up shader
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader2);

        float vertices2[] = {
                -0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f,  0.0f, 0.0f,
                0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f,  1.0f, 0.0f,
                0.5f,  0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f,  0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                -0.5f,  0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f,

                -0.5f, -0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f,
                0.5f, -0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
                0.5f,  0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f,  0.5f,  0.5f,1.0f, 1.0f, 1.0f,  1.0f, 1.0f,
                -0.5f,  0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f,

                -0.5f,  0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
                -0.5f,  0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f,
                -0.5f,  0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,

                0.5f,  0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
                0.5f,  0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                0.5f, -0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f,
                0.5f,  0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,

                -0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f, -0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
                0.5f, -0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
                -0.5f, -0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f,
                -0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,

                -0.5f,  0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                0.5f,  0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f,  0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
                0.5f,  0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
                -0.5f,  0.5f,  0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f,
                -0.5f,  0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f
        };

        int elementIndices[] = {
                0, 1, 2,
                2, 3, 1
        };

        // VAO 2
        int vao2 = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao2);

        int vbo2 = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo2);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices2, GL15.GL_STATIC_DRAW);

//        int veo2 = GL15.glGenBuffers();;
//        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, veo2);
//        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, elementIndices, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
        GL20.glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
        GL20.glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * Float.BYTES, 6 * Float.BYTES);
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        // Loading texture
        STBImage.stbi_set_flip_vertically_on_load(true);
        int textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        // set the texture wrapping/filtering options (on the currently bound texture object)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        ByteBuffer imageData = STBImage.stbi_load("./assets/images/hoanganh.jpg", width, height, channels, 0);

        if (imageData != null) {
            GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width.get(0), height.get(0), 0, GL_RGB, GL_UNSIGNED_BYTE, imageData);
            GL30.glGenerateMipmap(GL_TEXTURE_2D);
            STBImage.stbi_image_free(imageData);
        } else {
            System.out.println("Fail to load image");
        }

        // Loading texture
        int backgroundID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, backgroundID);
        // set the texture wrapping/filtering options (on the currently bound texture object)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);


        imageData = STBImage.stbi_load("./assets/images/TMA-logo.png", width, height, channels, 0);

        if (imageData != null) {
            GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
            GL30.glGenerateMipmap(GL_TEXTURE_2D);
            STBImage.stbi_image_free(imageData);
        } else {
            System.out.println("Fail to load image");
        }

        GL20.glUseProgram(shader2);
        int textUniform = GL20.glGetUniformLocation(shader2, "texture");
        int backgroundUniform = GL20.glGetUniformLocation(shader2, "background");
        GL20.glUniform1i(textUniform, 0);
        GL20.glUniform1i(backgroundUniform, 1);

        int changeValueUniform2 = GL20.glGetUniformLocation(shader2, "changeValue");
        int transformLocation = GL20.glGetUniformLocation(shader2, "transform");

        Matrix4f projection = new Matrix4f();
        projection.perspective((float)Math.toRadians(45), 800.0f / 600.0f, 0.1f, 100.0f);
        int projectionLocation = GL20.glGetUniformLocation(shader2, "projection");
        GL20.glUniformMatrix4fv(projectionLocation, false, extractMatrixToArray(projection));

        glEnable(GL_DEPTH_TEST);

        Vector3f[] cubePosition = new Vector3f[] {
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Vector3f(2.0f,  5.0f, -15.0f),
                new Vector3f(-1.5f, -2.2f, -2.5f),
                new Vector3f(-3.8f, -2.0f, -12.3f),
                new Vector3f(2.4f, -0.4f, -3.5f),
                new Vector3f(-1.7f,  3.0f, -7.5f),
                new Vector3f(1.3f, -2.0f, -2.5f),
                new Vector3f( 1.5f,  2.0f, -2.5f),
                new Vector3f(1.5f,  0.2f, -1.5f),
                new Vector3f(-1.3f,  1.0f, -1.5f),
        };

        Vector3f cameraPosition = new Vector3f(0.0f, 0.0f, 3.0f);
        Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glfwSetCursorPosCallback(window, (window, xPos, yPos) -> {
            float offsetX =(float) xPos - lastX;
            float offsetY = lastY - (float)yPos;

            lastX = (float) xPos;
            lastY = (float) yPos;

            offsetX *= sensibility;
            offsetY *= sensibility;

            pitch += offsetY;
            yaw += offsetX;

            if(pitch > 89.0f)
                pitch = 89.0f;
            else if(pitch < -89.0f)
                pitch = -89.0f;

            float x = (float) ( Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) );
            float y = (float) ( Math.sin(Math.toRadians(pitch)) );
            float z = (float) ( Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) );
            Vector3f direction = new Vector3f(x,y,z);
            cameraFront =  direction.normalize();
        });

        while ( !glfwWindowShouldClose(window) ) {
            float timeValue = (float) glfwGetTime();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // Generate transformation matrix
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            GL20.glUseProgram(shader2);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureID);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, backgroundID);

            GL30.glBindVertexArray(vao2);
            GL20.glUniform3f(changeValueUniform2, 0, 0, 0);

            Matrix4f view = new Matrix4f();

            processInput(window, cameraPosition);

            Vector3f center = new Vector3f(cameraFront);
            center.add(cameraPosition);
            view.lookAt(
                    cameraPosition,
                    center,
                    cameraUp
            );

            int viewLocation = GL20.glGetUniformLocation(shader2, "view");
            GL20.glUniformMatrix4fv(viewLocation, false, extractMatrixToArray(view));


            for(int i = 0; i < cubePosition.length; i++) {
                Matrix4f transformation = new Matrix4f();
                transformation.translate(cubePosition[i]);
                float angle = 20.0f * i;
                transformation.rotate(angle, new Vector3f(1.0f, 0.3f, 0.5f));
                GL20.glUniformMatrix4fv(transformLocation, false, extractMatrixToArray(transformation));
                glDrawArrays(GL_TRIANGLES, 0, 36);
            }


            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    public float[] extractMatrixToArray(Matrix4f m) {
        return new float[]{m.m00(), m.m01(), m.m02(), m.m03(),
                m.m10(), m.m11(), m.m12(), m.m13(),
                m.m20(), m.m21(), m.m22(), m.m23(),
                m.m30(), m.m31(), m.m32(), m.m33()};
    }

    public void processInput(long windowID, Vector3f cameraPosition) {
        final float cameraSpeed = 0.5f;

        Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f movement = new Vector3f(0,0,0);
        if(glfwGetKey(windowID, GLFW_KEY_W) == GLFW_PRESS) {
            movement.add(cameraFront).mul(cameraSpeed);
        } else if(glfwGetKey(windowID, GLFW_KEY_S) == GLFW_PRESS) {
            movement.add(cameraFront).mul(cameraSpeed).negate();
        }else if(glfwGetKey(windowID, GLFW_KEY_A) == GLFW_PRESS) {
            movement.add(cameraFront).cross(cameraUp).mul(cameraSpeed).negate();
        }else if(glfwGetKey(windowID, GLFW_KEY_D) == GLFW_PRESS) {
            movement.add(cameraFront).cross(cameraUp).mul(cameraSpeed);
        }
        cameraPosition.add(movement);
    }
}
