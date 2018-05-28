# YuvShot

Save raw [YUV][yuv] frames from the Android camera preview for analysis or
to collect data for automated tests.

## Running

Either import the project in [Android Studio][android] or use the
[Makefile][make].

## Getting the .yuv files

All captures are saved in `/sdcard/YuvShot`.
Use `adb` to get the folder:

	$ adb pull /sdcard/YuvShot

Since YUV is raw image data and doesn't contain any size information,
the width, height and rotation modifier of the camera frame are added to
file name like this:

	(timestamp)-1920x1080-90deg.yuv

## Converting a .yuv file to .jpg

Although the point of this tool is to work with raw YUV frames, you may
simply convert the .yuv files with [ffmpeg][ffmpeg]:

	$ ffmpeg -s 720x480 -pix_fmt nv21 -i in.yuv out.jpg

[android]: https://developer.android.com/sdk/
[yuv]: https://en.wikipedia.org/wiki/YUV
[make]: https://en.wikipedia.org/wiki/Make_(software)
[ffmpeg]: https://www.ffmpeg.org/
