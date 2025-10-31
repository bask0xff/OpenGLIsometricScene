# OpenGL Isometric Scene Demo

This is a demo Android application that renders an orthographic isometric scene using OpenGL ES. It features a 5x5x1 grid of colored cubes with touch-based selection for individual cubes.

## Features
- Isometric orthographic projection for 3D scene rendering.
- 5x5 grid of colorful cubes.
- Touch interaction to select and highlight cubes.
- Progressive versions showing development improvements.

## Demo Video
For a full video walkthrough of an intermediate version:
![ezgif-22021c897f4310](https://github.com/user-attachments/assets/7876abdd-2bdc-4152-9f72-195389b5f6c4)

## Versions
### 1st Version (21.04.2025)
![1st Version](https://github.com/user-attachments/assets/80fc7c4c-67dc-465a-adbe-8182f366e704)

### 2nd Version (01.05.2025)
![2nd Version](https://github.com/user-attachments/assets/5e3feece-5894-4fba-af8a-983101c27def)

### 3rd Version (07.05.2025)
![3rd Version](https://github.com/user-attachments/assets/40dca81f-32ee-44cf-b98f-872b775e0186)

### 4th Version (16.06.2025)
https://github.com/user-attachments/assets/c3f51098-1167-4889-9b42-d852ae4adeec


## Installation and Building
1. Clone the repository:
```
git clone https://github.com/bask0xff/OpenGLIsometricScene.git
```
2. Open the project in Android Studio.
3. Ensure you have the Android SDK installed (target SDK likely 34 or higher, min SDK 21+ for OpenGL ES support).
4. Build the project (Gradle will handle dependencies).
5. Run on an emulator or physical device.

Alternatively, download the pre-built APK from `app/release/app-release.apk` and install it directly on your Android device (enable unknown sources if needed).

## Requirements
- Android Studio 2023+ (with Gradle 8+).
- Java 17+ (based on build files).
- Device/emulator supporting OpenGL ES 2.0+.

## Contributing
Feel free to fork and submit pull requests for improvements, such as adding camera controls or shaders.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. (Note: Add a LICENSE file to your repo if not present.)

## About
Developed as a personal demo for exploring OpenGL isometric rendering on Android. No external dependencies beyond standard Android libraries.



